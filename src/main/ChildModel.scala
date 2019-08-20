package org.nlogo.ls

import java.awt.GraphicsEnvironment
import javax.swing.{JFrame, SwingUtilities}

import org.nlogo.api.{CommandRunnable, Workspace}
import org.nlogo.workspace.AbstractWorkspaceScala

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class ChildModel(val parentWorkspace: Workspace, val modelID: Int)  {
  lazy val evaluator = new Evaluator(modelID, name, workspace, parentWorkspace.asInstanceOf[AbstractWorkspaceScala])

  private var _name: Option[String] = None
  def name_= (newName: String): Unit = {
    _name = Some(newName)
    updateFrameTitle()
  }
  def name: String = _name.getOrElse(workspace.getModelFileName)

  def ask(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef], rng: RNG): Notifying[Unit] =
    evaluator.command(code, lets, args, rng, parallel = true) // parallel is safe, so it's the default

  def of(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef], rng: RNG): Notifying[AnyRef] =
    evaluator.report(code, lets, args, rng, parallel = true) // parallel is safe, so it's the default

  def kill(): Unit = {
    Future {
      workspace.dispose
    }
    val disposeRunnable = new CommandRunnable { def run(): Unit = frame.foreach(_.dispose) }
    if (java.awt.EventQueue.isDispatchThread) {
      disposeRunnable.run()
    } else {
      parentWorkspace.waitFor(disposeRunnable)
    }
  }

  def halt(): Unit = workspace.halt()

  def path: String = workspace.getModelPath

  protected def updateFrameTitle(): Unit = frame.foreach(_.setTitle(frameTitle))
  def frameTitle = s"$name (LevelSpace model #$modelID)"
  def frame: Option[JFrame]

  def setSpeed(d: Double)
  def workspace: AbstractWorkspaceScala

  // can't change once model is loaded
  lazy val usesLevelSpace: Boolean =
    workspace.getExtensionManager.loadedExtensions.asScala.exists(
      _.getClass.toString equals classOf[LevelSpace].toString
    )

  def show(): Unit = frame.foreach { f => onEDT { f.setVisible(true) } }
  def hide(): Unit = frame.foreach { f => onEDT { f.setVisible(false)} }
  def showAll(): Unit = {
    show()
    if (usesLevelSpace) {
      ask("ls:show-all ls:models", Seq(), Seq(), AuxRNG).waitFor
    }
  }
  def hideAll(): Unit = {
    hide()
    if (usesLevelSpace) {
      ask("ls:hide-all ls:models", Seq(), Seq(), AuxRNG).waitFor
    }
  }

  def seedRNG(rng: RNG, seed: Long): Unit = if (usesLevelSpace) {
    ask("ls:random-seed seed", Seq("seed" -> Double.box(seed)), Seq(), rng).waitFor
  } else {
    rng(workspace).setSeed(seed)
  }

  /**
   * If on EDT already, runs the given function, otherwise, invokes it async on the EDT.
   * NoOps in headless.
   **/
  def onEDT(f: => Unit): Unit =
    if (!GraphicsEnvironment.isHeadless) {
      if (SwingUtilities.isEventDispatchThread)
        f
      else
        SwingUtilities.invokeLater(() => f)
    }
}


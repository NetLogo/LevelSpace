package org.nlogo.ls

import org.nlogo.api.{CommandRunnable, Workspace, World}
import org.nlogo.workspace.AbstractWorkspaceScala

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import javax.swing.{JFrame, SwingUtilities}
import java.awt.GraphicsEnvironment

abstract class ChildModel(val parentWorkspace: Workspace, val modelID: Int)  {
  lazy val evaluator = new Evaluator(modelID, name, workspace, parentWorkspace.world)

  private var _name: Option[String] = None
  def name_= (newName: String) = {
    _name = Some(newName)
    updateFrameTitle()
  }
  def name = _name.getOrElse(workspace.getModelFileName)

  def ask(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): Notifying[Unit] =
    evaluator.command(code, lets, args)

  def of(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): Notifying[AnyRef] =
    evaluator.report(code, lets, args)

  def kill() = {
    Future {
      workspace.dispose
    }
    val disposeRunnable = new CommandRunnable { def run() = frame.foreach(_.dispose) }
    if (java.awt.EventQueue.isDispatchThread) {
      disposeRunnable.run()
    } else {
      parentWorkspace.waitFor(disposeRunnable)
    }
  }

  def halt() = workspace.halt()

  def path = workspace.getModelPath

  protected def updateFrameTitle() = frame.foreach(_.setTitle(frameTitle))
  def frameTitle = s"$name (LevelSpace model #$modelID)"
  def frame: Option[JFrame]

  def setSpeed(d: Double)
  def workspace: AbstractWorkspaceScala

  def usesLevelSpace = {
    workspace.getExtensionManager.loadedExtensions.asScala.exists(
      _.getClass.toString equals classOf[LevelSpace].toString
    )
  }

  def show() = frame.foreach { f => onEDT { f.setVisible(true) } }
  def hide() = frame.foreach { f => onEDT { f.setVisible(false)} }
  def showAll(): Unit = {
    show()
    if (usesLevelSpace) {
      ask("ls:show-all ls:models", Seq(), Seq()).waitFor
    }
  }
  def hideAll(): Unit = {
    hide()
    if (usesLevelSpace) {
      ask("ls:hide-all ls:models", Seq(), Seq()).waitFor
    }
  }

  /**
   * If on EDT already, runs the given function, otherwise, invokes it async on the EDT.
   * NoOps in headless.
   **/
  def onEDT(f: => Unit) =
    if (!GraphicsEnvironment.isHeadless) {
      if (SwingUtilities.isEventDispatchThread)
        f
      else
        SwingUtilities.invokeLater(() => f)
    }

}


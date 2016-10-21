package org.nlogo.ls

import org.nlogo.api.{CommandRunnable, Workspace}
import org.nlogo.workspace.{AbstractWorkspaceScala, InMemoryExtensionLoader}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import javax.swing.{JFrame, SwingUtilities}
import java.awt.GraphicsEnvironment

abstract class ChildModel(val parentWorkspace: Workspace, val modelID: Int)  {
  lazy val evaluator = new Evaluator(name, workspace)

  private var _name: Option[String] = None
  def name_= (newName: String) = {
    _name = Some(newName)
    updateFrameTitle
  }
  def name = _name.getOrElse(workspace.getModelFileName)

  def ask(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): FutureJob[Unit] = ErrorUtils.handle(this) {
    ErrorUtils.handle(this, evaluator.command(code, lets, args))
  }

  def of(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): FutureJob[AnyRef] = ErrorUtils.handle(this) {
    ErrorUtils.handle(this, evaluator.report(code, lets, args))
  }

  def kill = {
    Future {
      workspace.dispose
    }
    val disposeRunnable = new CommandRunnable { def run() = frame.foreach(_.dispose) }
    if (java.awt.EventQueue.isDispatchThread()) {
      disposeRunnable.run
    } else {
      parentWorkspace.waitFor(disposeRunnable)
    }
  }

  def halt = workspace.halt

  def path = workspace.getModelPath

  protected def updateFrameTitle = frame.foreach(_.setTitle(frameTitle))
  def frameTitle = s"$name (LevelSpace model #$modelID)"
  def frame: Option[JFrame]

  def setSpeed(d: Double)
  def workspace: AbstractWorkspaceScala

  def usesLevelSpace = {
    workspace.getExtensionManager.loadedExtensions.asScala.find {
      _.getClass.toString equals classOf[LevelSpace].toString
    }.isDefined
  }

  def show = onEDT { frame.foreach(_.setVisible(true)) }
  def hide = onEDT { frame.foreach(_.setVisible(false)) }
  def showAll = {
    show
    if (usesLevelSpace) {
      ask("ls:show-all ls:models", Seq(), Seq())(parentWorkspace.world)
    }
  }
  def hideAll = {
    hide
    if (usesLevelSpace) {
      ask("ls:hide-all ls:models", Seq(), Seq())(parentWorkspace.world)
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
        SwingUtilities.invokeLater(new Runnable { def run = f })
    }

  def injectProcedures = {
    val extName= "ls"
    val ext = new InjectedExtension(this)
    val loader = new InMemoryExtensionLoader(extName, ext)
    workspace.extensionManager.addLoader(loader)
    workspace.extensionManager.importExtension(loader, loader.locateExtension(extName).get, extName, null)
  }

}

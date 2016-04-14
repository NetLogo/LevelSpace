package org.nlogo.ls

import org.nlogo.api.{CommandRunnable, Workspace}
import org.nlogo.workspace.AbstractWorkspaceScala
import scala.collection.JavaConverters._
import javax.swing.{JFrame, SwingUtilities}

abstract class ChildModel(val parentWorkspace: Workspace, val modelID: Int)  {
  lazy val evaluator = new Evaluator(name, workspace)

  private var _name: Option[String] = None
  def name_= (newName: String) = {
    _name = Some(newName)
    frame.foreach(_.setTitle(frameTitle))
  }
  def name = _name.getOrElse(workspace.getModelFileName)

  def ask(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): FutureJob[Unit] = ErrorUtils.handle(this) {
    ErrorUtils.handle(this, evaluator.command(code, lets, args))
  }

  def of(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): FutureJob[AnyRef] = ErrorUtils.handle(this) {
    ErrorUtils.handle(this, evaluator.report(code, lets, args))
  }

  def kill = {
    UnlockAndBlock(parentWorkspace.world) {
      workspace.dispose
    }
    parentWorkspace waitFor new CommandRunnable {
      def run() = {
        frame.foreach(_.dispose)
      }
    }
  }

  def halt = workspace.halt

  def path = workspace.getModelPath

  def frameTitle = s"$name (LevelSpace model #$modelID)"
  def frame: Option[JFrame]

  def setSpeed(d: Double)
  def workspace: AbstractWorkspaceScala

  def usesLevelSpace = workspace.getExtensionManager.loadedExtensions.asScala.find {
    _.getClass.toString equals "class LevelSpace"
  }.isDefined

  def show = onEDT { frame.foreach(_.setVisible(true)) }
  def hide = onEDT { frame.foreach(_.setVisible(false)) }

  def onEDT(f: => Unit) = SwingUtilities.invokeLater(new Runnable { def run = f })
}


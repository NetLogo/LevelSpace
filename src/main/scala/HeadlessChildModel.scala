package org.nlogo.ls

import java.io.IOException

import org.nlogo.agent
import org.nlogo.api._
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.ls.gui.ViewFrame
import org.nlogo.nvm.HaltException
import org.nlogo.workspace.AbstractWorkspaceScala

@throws(classOf[InterruptedException])
@throws(classOf[ExtensionException])
@throws(classOf[HaltException])
@throws(classOf[IOException])
class HeadlessChildModel
(parentWorkspace: AbstractWorkspaceScala, path: String, modelID: Int)
  extends ChildModel(parentWorkspace, modelID) {

  val world: agent.World = if(Version.is3D) new org.nlogo.agent.World3D() else new org.nlogo.agent.World

  val workspace = new HeadlessWorkspace(
      world,
      new org.nlogo.compile.Compiler(if (Version.is3D) NetLogoThreeDDialect else NetLogoLegacyDialect),
      new org.nlogo.render.Renderer(world),
      new org.nlogo.sdm.AggregateManagerLite,
      null) {
    override def sendOutput(oo: org.nlogo.agent.OutputObject, toOutputArea: Boolean) = {
      frame.foreach { f => onEDT {
        new org.nlogo.window.Events.OutputEvent(false, oo, false, !toOutputArea).raise(f)
      }}
    }
  }

  try {
    workspace.open(path)
  } catch {
    case e: IllegalStateException =>
      throw new ExtensionException(s"$path is from an incompatible version of NetLogo. Try opening it in NetLogo to convert it.", e)
  }

  var frame: Option[ViewFrame] = None

  override def show() = onEDT {
    val f = frame.getOrElse { new ViewFrame(workspace) }
    frame = Some(f)
    updateFrameTitle()
    super.show()
    updateView()
  }

  def updateView() = frame.foreach { f => if (f.isVisible) onEDT{ f.repaint() } }

  def setSpeed(d: Double) = {}

  override def ask(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): NotifyingJob =
    super.ask(code, lets, args)
}

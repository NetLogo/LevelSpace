package org.nlogo.ls

import java.io.IOException

import org.nlogo.agent.{CompilationManagement, OutputObject, World, World2D, World3D}
import org.nlogo.api._
import org.nlogo.nvm.{Context, HaltException, Instruction}
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.ls.gui.ViewFrame
import org.nlogo.workspace.{AbstractWorkspace, AbstractWorkspaceScala}

@throws(classOf[InterruptedException])
@throws(classOf[ExtensionException])
@throws(classOf[HaltException])
@throws(classOf[IOException])
class HeadlessChildModel (parentWorkspace: AbstractWorkspace, path: String, modelID: Int)
  extends ChildModel(parentWorkspace, modelID) {

  val world: World with CompilationManagement = if(Version.is3D) new World3D() else new World2D()

  var frame: Option[ViewFrame] = None

  val workspace: HeadlessWorkspace = new HeadlessWorkspace(
      world,
      new org.nlogo.compile.Compiler(if (Version.is3D) NetLogoThreeDDialect else NetLogoLegacyDialect),
      new org.nlogo.render.Renderer(world),
      new org.nlogo.sdm.AggregateManagerLite,
      null) {

    override def sendOutput(oo: OutputObject, toOutputArea: Boolean): Unit = {
      frame.foreach { f => onEDT {
        new org.nlogo.window.Events.OutputEvent(false, oo, false, !toOutputArea).raise(f)
      }}
    }

    override def runtimeError(owner: JobOwner, context: Context, instruction: Instruction, ex: Exception): Unit = {

    }

    override def requestDisplayUpdate(force: Boolean): Unit = {
      super.requestDisplayUpdate(force)
      updateDisplay(false)
    }

    override def updateDisplay(ignored: Boolean): Unit =
      frame.foreach { f => if (f.isVisible) onEDT{ f.repaint() } }
  }

  try {
    workspace.open(path)
  } catch {
    case e: IllegalStateException =>
      throw new ExtensionException(
        s"$path is from an incompatible version of NetLogo. Try opening it in NetLogo to convert it.", e
      )
  }

  override def show(): Unit = onEDT {
    val f = frame.getOrElse { new ViewFrame(workspace) }
    frame = Some(f)
    updateFrameTitle()
    super.show()
    workspace.requestDisplayUpdate(false)
  }

  def isVisible: Boolean = frame.exists(_.isVisible)

  def setSpeed(d: Double): Unit = {}

  def tryEagerAsk(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef], rng: RNG): Notifying[Unit] =
    evaluator.command(code, lets, args, rng, parallel = usesLevelSpace || isVisible)

  def tryEagerOf(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef], rng: RNG): Notifying[AnyRef] =
    evaluator.report(code, lets, args, rng, parallel = usesLevelSpace || isVisible)
}

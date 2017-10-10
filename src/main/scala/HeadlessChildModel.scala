package org.nlogo.ls

import java.io.IOException

import org.nlogo.agent.{CompilationManagement, OutputObject, World, World2D, World3D}
import org.nlogo.api._
import org.nlogo.nvm.{CompilerFlags, Context => NvmContext, HaltException,
  Instruction, JobManagerInterface, JobManagerOwner, Optimizations}
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.workspace.{AbstractWorkspace, DefaultAbstractWorkspace, HeadlessJobManagerOwner}
import org.nlogo.ls.gui.ViewFrame

@throws(classOf[InterruptedException])
@throws(classOf[ExtensionException])
@throws(classOf[HaltException])
@throws(classOf[IOException])
class HeadlessChildModel(_parentWorkspace: AbstractWorkspace, path: String, modelID: Int)
  extends ChildModel(_parentWorkspace, modelID) {

  def is3D: Boolean = _parentWorkspace.compiler.dialect.is3D

  val world: World with CompilationManagement = if (is3D) new World3D() else new World2D()

  private val renderer = new org.nlogo.render.Renderer(world)
  private val aggManager = new org.nlogo.sdm.AggregateManagerLite
  private val deps = {
    new DefaultAbstractWorkspace.DefaultDependencies(
      world,
      new org.nlogo.compile.Compiler(if (is3D) NetLogoThreeDDialect else NetLogoLegacyDialect),
      null,
      Seq(aggManager),
      CompilerFlags(optimizations = Optimizations.standardOptimizations)) {
        override lazy val owner: JobManagerOwner = new HeadlessJobManagerOwner(messageCenter) {
          override def runtimeError(
            owner:       JobOwner,
            manager:     JobManagerInterface,
            context:     NvmContext,
            instruction: Instruction,
            ex:          Exception): Unit = {
          }
        }
      }
  }

  val workspace: HeadlessWorkspace = new HeadlessWorkspace(deps, world, renderer, aggManager) {

    override def sendOutput(oo: OutputObject, toOutputArea: Boolean): Unit = {
      frame.foreach { f => onEDT {
        new org.nlogo.window.Events.OutputEvent(false, oo, false, !toOutputArea).raise(f)
      }}
    }

    override def requestDisplayUpdate(force: Boolean): Unit = {
      super.requestDisplayUpdate(force)
      frame.foreach { f => onEDT {
        new org.nlogo.window.Events.PeriodicUpdateEvent().raise(f)
      } }
    }
  }

  try {
    workspace.open(path)
  } catch {
    case e: IllegalStateException =>
      throw new ExtensionException(s"$path is from an incompatible version of NetLogo. Try opening it in NetLogo to convert it.", e)
  }

  var frame: Option[ViewFrame] = None

  override def show(): Unit = onEDT {
    val f = frame.getOrElse { new ViewFrame(workspace) }
    frame = Some(f)
    updateFrameTitle()
    super.show()
    updateView()
  }

  def isVisible: Boolean = frame.exists(_.isVisible)

  def updateView(): Unit = frame.foreach { f => if (f.isVisible) onEDT{ f.repaint() } }

  def setSpeed(d: Double): Unit = {}

  override def ask(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): Notifying[Unit] =
    super.ask(code, lets, args).map {r => updateView(); r}

  def tryEagerAsk(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): Notifying[Unit] =
    evaluator.command(code, lets, args, parallel = usesLevelSpace || isVisible)

  def tryEagerOf(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): Notifying[AnyRef] =
    evaluator.report(code, lets, args, parallel = usesLevelSpace || isVisible)

}

package org.nlogo.ls.gui

import java.awt.EventQueue.isDispatchThread
import java.awt.image.BufferedImage
import java.nio.file.Paths

import org.nlogo.agent.{Agent, CompilationManagement, World, World2D, World3D}
import org.nlogo.api.{ControlSet, ModelType, NetLogoLegacyDialect, NetLogoThreeDDialect, Version}
import org.nlogo.app.codetab.ExternalFileManager
import org.nlogo.app.tools.AgentMonitorManager
import org.nlogo.awt.EventQueue
import org.nlogo.core.{AgentKind, Model}
import org.nlogo.lite.ProceduresLite
import org.nlogo.window.Events.{CompiledEvent, LoadModelEvent}
import org.nlogo.window.{CompilerManager, DefaultEditorFactory, Event, FileController, GUIWorkspace, InterfacePanelLite, LinkRoot, NetLogoListenerManager, OutputWidget, ReconfigureWorkspaceUI, UpdateManager, WorkspaceConfig }
import org.nlogo.workspace.OpenModelFromURI
import org.nlogo.{api, fileformat}

import scala.concurrent.{Future, Promise}
import scala.util.Try

abstract class InterfaceComponent(frame: javax.swing.JFrame, is3D: Boolean) extends javax.swing.JPanel
with Event.LinkParent
with LinkRoot
with ControlSet {
  val listenerManager = new NetLogoListenerManager

  val world: World with CompilationManagement =
    if (is3D) new World3D() else new World2D()

  val compiler = new org.nlogo.compile.Compiler(if (is3D) NetLogoThreeDDialect else NetLogoLegacyDialect)

  val monitorManager = new AgentMonitorManager(frame)

  val aggregateManager = new org.nlogo.sdm.AggregateManagerLite

  val config = WorkspaceConfig
    .default
    .withWorld(world)
    .withCompiler(compiler)
    .withUpdateManager(new UpdateManager(world.tickCounter))
    .withExternalFileManager(new ExternalFileManager)
    .withFrame(frame)
    .withLinkParent(frame)
    .withListenerManager(listenerManager)
    .withMonitorManager(monitorManager)
    .withControlSet(this)
    .withSourceOwner(aggregateManager)


  // KioskLevel.NONE - We want a 3d button
  val workspace: GUIWorkspace = new GUIWorkspace(config) {
    override def newRenderer = new org.nlogo.render.Renderer(world)
    override def updateModel(m: Model): Model = m
  }

  addLinkComponent(monitorManager)

  val viewManager = {
    val glViewFactory =
      if (is3D) new org.nlogo.gl.view.ThreeDGLViewFactory()
      else      new org.nlogo.gl.view.TwoDGLViewFactory()
    new org.nlogo.gl.view.ViewManager(workspace, frame, glViewFactory)
  }
  workspace.init(viewManager)
  addLinkComponent(viewManager)

  val procedures = new ProceduresLite(workspace, workspace)
  val liteEditorFactory = new DefaultEditorFactory(workspace.compiler, workspace.getExtensionManager)
  val interfacePanel: InterfacePanelLite = createInterfacePanel(workspace)

  addLinkComponent(aggregateManager)
  addLinkComponent(workspace)
  addLinkComponent(procedures)
  addLinkComponent(new CompilerManager(workspace, workspace.world.asInstanceOf[World with CompilationManagement], procedures, Seq(aggregateManager)))
  addLinkComponent(new CompiledEvent.Handler {
    override def handle(e: CompiledEvent) {
      if (e.error != null)
        throw e.error
  }})
  addLinkComponent(new LoadModelEvent.Handler {
    override def handle(e: LoadModelEvent) {
      aggregateManager.load(e.model, workspace)
  }})
  addLinkComponent(listenerManager)

  workspace.setWidgetContainer(interfacePanel)
  setBackground(java.awt.Color.WHITE)
  add(interfacePanel)

  def open(path: String) = {
    EventQueue.mustBeEventDispatchThread()
    val uri = Paths.get(path).toUri
    interfacePanel.reset()
    val controller = new FileController(this, workspace.modelTracker)
    val loader = fileformat.basicLoader
    val modelOpt = OpenModelFromURI(uri, controller, loader, fileformat.defaultConverter, Version)
    modelOpt.foreach(model => ReconfigureWorkspaceUI(this, uri, ModelType.Library, model, workspace.compilerServices, Version))
  }

  protected def createInterfacePanel(workspace: GUIWorkspace): InterfacePanelLite

  def userInterface: Future[BufferedImage] = {
    if (isDispatchThread)
      Promise.fromTry(Try(interfacePanel.interfaceImage)).future
    else {
      val promise = Promise[BufferedImage]()
      EventQueue.invokeLater { () =>
        promise.complete(Try(interfacePanel.interfaceImage))
        ()
      }
      promise.future
    }
  }

  def userOutput: Future[String] = {
    def findOutput(ipl: InterfacePanelLite): String =
      ipl.getComponents.collect {
        case ow: OutputWidget => ow.valueText
      }.headOption.getOrElse("")
    if (isDispatchThread)
      Promise.fromTry(Try(findOutput(interfacePanel))).future
    else {
      val promise = Promise[String]()
      EventQueue.invokeLater { () =>
        promise.complete(Try(findOutput(interfacePanel)))
        ()
      }
      promise.future
    }
  }
}

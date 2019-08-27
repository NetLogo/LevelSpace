package org.nlogo.ls.gui

import java.awt.event.ActionEvent
import java.awt.{Dimension, Graphics, Graphics2D}
import javax.swing.{BoxLayout, JFrame, JPanel, Timer}

import org.nlogo.core.CompilerException
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.window.Events.{AddJobEvent, CompileMoreSourceEvent, CompiledEvent, PeriodicUpdateEvent}
import org.nlogo.window.JobWidget
import org.nlogo.render.Renderer

class ViewFrame(ws: HeadlessWorkspace) extends JFrame with CompileMoreSourceEvent.Handler with AddJobEvent.Handler {
  private val viewPanel = new JPanel() {
    override def paintComponent(g: Graphics): Unit = ws.renderer.paint(g.asInstanceOf[Graphics2D], ws)
  }

  val viewContainer = new JPanel
  viewContainer.setLayout(new BoxLayout(viewContainer, BoxLayout.Y_AXIS))

  viewPanel.setPreferredSize(new Dimension((ws.viewWidth * ws.patchSize).toInt, (ws.viewHeight * ws.patchSize).toInt))
  viewPanel.setMinimumSize(viewPanel.getPreferredSize)
  viewPanel.setMaximumSize(viewPanel.getPreferredSize)
  viewContainer.add(viewPanel)

  val panel = new HeadlessPanel(ws, viewContainer)
  getContentPane.add(panel)
  pack()

  def handle(e: CompileMoreSourceEvent): Unit = {
    val owner = e.owner
    try {
      val displayName = Some.apply(owner.classDisplayName)
      val results =
        ws.compiler.compileMoreCode(owner.source,
          displayName, ws.world.program,
          ws.procedures, ws.getExtensionManager,
          ws.getLibraryManager, ws.getCompilationEnvironment)
      results.head.init(ws)
      results.head.owner = owner
      new CompiledEvent(owner, ws.world.program, results.head, null).raise(this)
    } catch {
      case error: CompilerException =>
        new CompiledEvent(owner, ws.world.program, null, error).raise(this)
    }
  }

  def handle(e: AddJobEvent): Unit = {
    val agents = if (e.agents == null ) ws.world.agentSetOfKind(e.owner.asInstanceOf[JobWidget].kind) else e.agents
    ws.jobManager.addJob(e.owner, agents, ws, e.procedure)
  }

}



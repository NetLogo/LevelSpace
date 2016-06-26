package org.nlogo.ls.gui

import javax.swing.{JFrame, JPanel, SwingUtilities, Timer}
import java.awt.{Graphics, Graphics2D, Dimension}
import java.awt.event.{ActionListener, ActionEvent}

import org.nlogo.api.{ViewSettings, RendererInterface}
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.window.JobWidget
import org.nlogo.window.Events.{CompileMoreSourceEvent, CompiledEvent, AddJobEvent}
import org.nlogo.core.CompilerException

class ViewFrame(ws: HeadlessWorkspace) extends JFrame with CompileMoreSourceEvent.Handler with AddJobEvent.Handler {
  val viewPanel = new JPanel() {
    override def paintComponent(g: Graphics) = {
      ws.renderer.exportView(g.asInstanceOf[Graphics2D], ws)
    }
  }

  viewPanel.setPreferredSize(new Dimension((ws.viewWidth * ws.patchSize).toInt, (ws.viewHeight * ws.patchSize).toInt))
  val guiPanel = new GUIPanel(ws, viewPanel)
  getContentPane.add(guiPanel)
  pack()

  new Timer(1000 / 30, new ActionListener() {
    override def actionPerformed(e: ActionEvent) = viewPanel.repaint()
  }).start

  def handle(e: CompileMoreSourceEvent): Unit = {
    val owner = e.owner
    try {
      val displayName = Some.apply(owner.classDisplayName)
      val results =
        ws.compiler.compileMoreCode(owner.source,
          displayName, ws.world.program,
          ws.getProcedures, ws.getExtensionManager,
          ws.getCompilationEnvironment);
      results.head.init(ws)
      results.head.owner = owner
      new CompiledEvent(owner, ws.world.program, results.head, null).raise(this)
    } catch {
      case error: CompilerException =>
        new CompiledEvent(owner, ws.world.program, null, error).raise(this)
    }
  }

  def handle(e: AddJobEvent): Unit = {
    val agents = if (e.agents == null ) ws.world.agentKindToAgentSet(e.owner.asInstanceOf[JobWidget].kind) else e.agents
    ws.jobManager.addJob(e.owner, agents, ws, e.procedure)
  }

}



package org.nlogo.ls.gui

import java.awt._
import java.awt.event.ActionEvent
import javax.swing._

import org.nlogo.app.CommandCenter
import org.nlogo.window.{Events, TickCounterLabel}
import org.nlogo.workspace.AbstractWorkspaceScala

class GUIPanel(ws: AbstractWorkspaceScala, panel: JPanel, fullView: Boolean) extends JPanel with Events.OutputEvent.Handler {
  setLayout(new BorderLayout)

  val controlStrip = new JPanel
  controlStrip.setLayout(new BorderLayout)
  controlStrip.add(new TickCounterLabel(ws.world), BorderLayout.WEST)

  add(controlStrip, BorderLayout.NORTH)
  var cc = new CommandCenter(ws, new AbstractAction() {
    def actionPerformed(actionEvent: ActionEvent) {
    }
  })
  val scrollPane: JScrollPane = new JScrollPane(panel,
    if (fullView) ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
    else          ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
  val splitPane: JSplitPane   = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, scrollPane, cc)
  splitPane.setOneTouchExpandable(true)
  splitPane.setResizeWeight(if (fullView) 1 else 0)
  add(splitPane, BorderLayout.CENTER)


  def handle(outputEvent: Events.OutputEvent): Unit = {
    if (outputEvent.clear)
      cc.output.clear()
    else
      cc.output.append(outputEvent.outputObject, outputEvent.wrapLines)
  }
}

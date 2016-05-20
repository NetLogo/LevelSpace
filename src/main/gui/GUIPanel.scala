package org.nlogo.ls.gui

import java.awt._
import java.awt.event.ActionEvent
import javax.swing._

import org.nlogo.app.CommandCenter
import org.nlogo.lite.InterfaceComponent
import org.nlogo.window.Events

class GUIPanel(ws: InterfaceComponent) extends JPanel with Events.OutputEvent.Handler {
  setLayout(new BorderLayout)
  var cc = new CommandCenter(ws.workspace, new AbstractAction() {
    def actionPerformed(actionEvent: ActionEvent) {
    }
  })
  val scrollPane: JScrollPane = new JScrollPane(ws, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
  val splitPane: JSplitPane   = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, scrollPane, cc)
  splitPane.setOneTouchExpandable(true)
  splitPane.setResizeWeight(1)
  add(splitPane)

  def handle(outputEvent: Events.OutputEvent): Unit = {
    if (outputEvent.clear)
      cc.output.clear()
    else
      cc.output.append(outputEvent.outputObject, outputEvent.wrapLines)
  }
}

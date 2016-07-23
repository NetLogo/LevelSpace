package org.nlogo.ls.gui

import java.awt._
import java.awt.event.ActionEvent
import javax.swing._
import javax.swing.event

import org.nlogo.app.interfacetab.CommandCenter
import org.nlogo.window.{Events, TickCounterLabel, GUIWorkspace}
import org.nlogo.workspace.AbstractWorkspaceScala

class GUIPanel(ws: AbstractWorkspaceScala, panel: JPanel, fullView: Boolean) extends JPanel with Events.OutputEvent.Handler {
  setLayout(new BorderLayout)

  val controlStrip = new JPanel
  controlStrip.setLayout(new BorderLayout)
  controlStrip.add(new TickCounterLabel(ws.world), BorderLayout.WEST)

  ws match {
    case gws: GUIWorkspace =>
      val speedSliderPanel = new JPanel
      speedSliderPanel.add(new JLabel("speed: "))
      val speedSlider = new JSlider(-110, 112, gws.speedSliderPosition().toInt)
      speedSlider.addChangeListener(new event.ChangeListener() {
        override def stateChanged(e: event.ChangeEvent): Unit = {
          gws.speedSliderPosition(speedSlider.getValue / 2)
          gws.updateManager.nudgeSleeper
        }
      })
      speedSliderPanel.add(speedSlider)
      controlStrip.add(speedSliderPanel, BorderLayout.CENTER)
    case _ =>
  }

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

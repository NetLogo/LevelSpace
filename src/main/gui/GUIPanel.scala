package org.nlogo.ls.gui

import java.awt._
import java.awt.event.ActionEvent
import javax.swing._
import javax.swing.event

import org.nlogo.app.interfacetab.CommandCenter
import org.nlogo.window.{Events, TickCounterLabel, GUIWorkspace}
import org.nlogo.workspace.AbstractWorkspaceScala

abstract class ModelPanel(ws: AbstractWorkspaceScala, panel: JPanel, verticalScroll: Int, resizeWeight: Int)
extends JPanel with Events.OutputEvent.Handler {
  setLayout(new BorderLayout)

  val controlStrip = new JPanel
  controlStrip.setLayout(new BorderLayout)
  controlStrip.add(new TickCounterLabel(ws.world), BorderLayout.WEST)

  add(controlStrip, BorderLayout.NORTH)
  var cc = new CommandCenter(ws)

  val scrollPane: JScrollPane = new JScrollPane(panel, verticalScroll, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
  val splitPane: JSplitPane   = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, scrollPane, cc)
  splitPane.setOneTouchExpandable(true)
  splitPane.setResizeWeight(resizeWeight)
  add(splitPane, BorderLayout.CENTER)


  def handle(outputEvent: Events.OutputEvent): Unit = {
    if (outputEvent.clear)
      cc.output.clear()
    else
      cc.output.append(outputEvent.outputObject, outputEvent.wrapLines)
  }
}

class GUIPanel(ws: GUIWorkspace, panel: JPanel)
extends ModelPanel(ws, panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, 1) {
  val speedSliderPanel = new JPanel
  speedSliderPanel.add(new JLabel("speed: "))
  val speedSlider = new JSlider(-110, 112, ws.speedSliderPosition().toInt)
  speedSlider.addChangeListener((e: event.ChangeEvent) => {
    ws.speedSliderPosition(speedSlider.getValue / 2)
    ws.updateManager().nudgeSleeper
  })
  speedSliderPanel.add(speedSlider)
  controlStrip.add(speedSliderPanel, BorderLayout.CENTER)
}

class HeadlessPanel(ws: AbstractWorkspaceScala, panel: JPanel)
extends ModelPanel(ws, panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, 0)

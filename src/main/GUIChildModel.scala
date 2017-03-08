package org.nlogo.ls

import java.awt._
import java.awt.event.{WindowAdapter, WindowEvent}
import javax.swing.{JFrame, JMenuBar, WindowConstants}

import org.nlogo.api._
import org.nlogo.app.App
import org.nlogo.ls.gui.{GUIPanel, InterfaceComponent, ZoomableInterfaceComponent}
import org.nlogo.nvm.HaltException
import org.nlogo.window.GUIWorkspace

import scala.util.{Failure, Try}

@throws(classOf[InterruptedException]) @throws(classOf[ExtensionException]) @throws(classOf[HaltException])
class GUIChildModel (ls: LevelSpace, parentWorkspace: Workspace, path: String, modelID: Int)
  extends ChildModel(parentWorkspace, modelID) {

  val (component, panel, frame) = UnlockAndBlock.onEDT(parentWorkspace.world) {
    val f = new JFrame()
    val component: InterfaceComponent = new ZoomableInterfaceComponent(f)
    val panel = new GUIPanel(component.workspace, component)
    (component, panel, Some(f))
  }

  UnlockAndBlock.onEDT(parentWorkspace.world) (Try {
    val f = frame.get
    f.add(panel)
    val currentlyFocused: Window =
      Option(KeyboardFocusManager.getCurrentKeyboardFocusManager.getActiveWindow).getOrElse(App.app.frame)
    f.setLocationRelativeTo(currentlyFocused)
    f.setLocationByPlatform(true)
    f.setVisible(true)
    currentlyFocused.toFront()
    component.open(path)
    f.pack()
    f.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
    f.addWindowListener(new GUIWindowAdapter)
    val newMenuBar = new JMenuBar()
    val zoomMenuClass = Class.forName("org.nlogo.app.ZoomMenu")
    newMenuBar.add(zoomMenuClass.newInstance().asInstanceOf[org.nlogo.swing.Menu])
    f.setJMenuBar(newMenuBar)
    (component, panel)
  }) match {
    case Failure(_) => kill
    case _ =>
  }
  updateFrameTitle

  class GUIWindowAdapter extends WindowAdapter {
    override def windowClosing(windowEvent: WindowEvent): Unit = frame.foreach { f =>
      hide
    }
  }

  def setSpeed(d: Double): Unit = {
    workspace.updateManager.speed = d
    javax.swing.SwingUtilities.invokeLater(new Runnable {
      def run = {
        panel.speedSlider.setValue((d * 2).intValue)
        // Wakes up the workspace if the speed slider is super low.
        // Makes it so there's not a long pause after increasing
        // the speed slider from a low position. BCH 6/18/2016
        workspace.updateManager.nudgeSleeper
      }
    })
  }

  def workspace: GUIWorkspace = component.workspace
}

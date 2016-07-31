package org.nlogo.ls

import java.awt._
import java.awt.event.{ WindowAdapter, WindowEvent }
import java.util.concurrent.Callable
import javax.swing.{ FocusManager, JFrame, JMenuBar, JOptionPane, WindowConstants }
import gui.{ ZoomableInterfaceComponent, GUIPanel, InterfaceComponent }
import org.nlogo.api._
import org.nlogo.app.App
import org.nlogo.nvm.HaltException
import org.nlogo.window.{SpeedSliderPanel, RuntimeErrorDialog, GUIWorkspace}
import org.nlogo.window.Events.ZoomedEvent
import org.nlogo.window.Widget.LoadHelper
import org.nlogo.workspace.AbstractWorkspaceScala


class GUIChildModel @throws(classOf[InterruptedException]) @throws(classOf[ExtensionException]) @throws(classOf[HaltException]) (parentWorkspace: Workspace, path: String, modelID: Int)
  extends ChildModel(parentWorkspace, modelID) {

  var frame: Option[JFrame] = None

  val (component, panel) = UnlockAndBlock.onEDT(parentWorkspace.world) {
    val f = new JFrame
    frame = Some(f)
    val component: InterfaceComponent = new ZoomableInterfaceComponent(f)
    val panel = new GUIPanel(component.workspace, component)
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
  }
  updateFrameTitle

  class GUIWindowAdapter extends WindowAdapter {
    override def windowClosing(windowEvent: WindowEvent): Unit = frame.foreach { f =>
      val options: Array[AnyRef] = Array("Close Model", "Run in Background", "Cancel")
      val n: Int = JOptionPane.showOptionDialog(f, "Close the model, run it in the background, or do nothing?", null, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options(2))
      n match {
        case 0 =>
          try {
            LevelSpace.closeModel(GUIChildModel.this)
          } catch {
            case e: ExtensionException => throw new RuntimeException(e)
            case e: HaltException =>
          }
        case 1 => hide
        case 2 =>
      }
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

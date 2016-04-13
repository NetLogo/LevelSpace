package org.nlogo.ls

import java.awt._
import java.awt.event.{ WindowAdapter, WindowEvent }
import java.util.concurrent.Callable
import javax.swing.{ FocusManager, JFrame, JMenuBar, JOptionPane, WindowConstants }
import gui.{ ZoomableInterfaceComponent, GUIPanel }
import org.nlogo.api._
import org.nlogo.app.App
import org.nlogo.lite.{LiteWorkspace, InterfaceComponent}
import org.nlogo.nvm.HaltException
import org.nlogo.window.SpeedSliderPanel
import org.nlogo.window.Events.ZoomedEvent
import org.nlogo.window.Widget.LoadHelper
import org.nlogo.workspace.AbstractWorkspaceScala

class GUIChildModel @throws(classOf[InterruptedException]) @throws(classOf[ExtensionException]) @throws(classOf[HaltException]) (parentWorld: World, path: String, modelID: Int)
  extends ChildModel(parentWorld, modelID) {

  final val frame: JFrame = new JFrame

  var panel: GUIPanel = null
  val component = runUISafely(RunGUIChildModel)

  init()

  object RunGUIChildModel extends Callable[InterfaceComponent] {
    @throws(classOf[Exception])
    def call: InterfaceComponent = {
      val component: InterfaceComponent = new ZoomableInterfaceComponent(frame)
      panel = new GUIPanel(component)
      frame.add(panel)
      val currentlyFocused: Window =
        Option(KeyboardFocusManager.getCurrentKeyboardFocusManager.getActiveWindow).getOrElse(App.app.frame)
      frame.setLocationRelativeTo(currentlyFocused)
      frame.setLocationByPlatform(true)
      frame.setVisible(true)
      currentlyFocused.toFront()
      component.open(path)
      val c: Array[Component] = component.workspace.viewWidget.controlStrip.getComponents
      c.foreach {
        case ssp: SpeedSliderPanel =>
          ssp.setVisible(false)
          ssp.setValue(0)
        case _ =>
      }
      frame.pack()
      frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
      frame.addWindowListener(new GUIWindowAdapter)
      val newMenuBar = new JMenuBar()
      val zoomMenuClass = Class.forName("org.nlogo.app.ZoomMenu")
      newMenuBar.add(zoomMenuClass.newInstance().asInstanceOf[org.nlogo.swing.Menu])
      frame.setJMenuBar(newMenuBar)
      component
    }
  }

  class GUIWindowAdapter extends WindowAdapter {
    override def windowClosing(windowEvent: WindowEvent): Unit = {
      val options: Array[AnyRef] = Array("Close Model", "Run in Background", "Cancel")
      val n: Int = JOptionPane.showOptionDialog(frame, "Close the model, run it in the background, or do nothing?", null, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options(2))
      n match {
        case 0 =>
          try {
            LevelSpace.closeModel(GUIChildModel.this)
          } catch {
            case e: ExtensionException => throw new RuntimeException(e)
            case e: HaltException =>
          }
        case 1 => hide()
        case 2 =>
      }
    }
  }

  def setSpeed(d: Double): Unit = {
    val c: Array[Component] = component.workspace.viewWidget.controlStrip.getComponents
    for (co <- c) {
      if (co.isInstanceOf[SpeedSliderPanel]) {
        (co.asInstanceOf[SpeedSliderPanel]).setValue(d.toInt)
      }
    }
  }

  def workspace: AbstractWorkspaceScala = component.workspace
}

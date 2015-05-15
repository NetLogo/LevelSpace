import java.awt._
import java.awt.event.{ WindowAdapter, WindowEvent }
import java.util.concurrent.Callable
import javax.swing.{ FocusManager, JFrame, JMenuBar, JOptionPane, WindowConstants }
import org.nlogo.api._
import org.nlogo.lite.{LiteWorkspace, InterfaceComponent}
import org.nlogo.nvm.HaltException
import org.nlogo.window.Events.ZoomedEvent
import org.nlogo.window.Widget.LoadHelper
import org.nlogo.window._
import org.nlogo.workspace.AbstractWorkspace

class GUIChildModel @throws(classOf[InterruptedException]) @throws(classOf[ExtensionException]) @throws(classOf[HaltException]) (parentWorld: World, path: String, levelsSpaceNumber: Int)
  extends ChildModel(parentWorld, levelsSpaceNumber) {

  final val _frame: JFrame = new JFrame

  var panel: GUIPanel = null

  val component: InterfaceComponent =
    runUISafely(new Callable[InterfaceComponent]() {
      @throws(classOf[Exception])
      def call: InterfaceComponent = {
        val component: InterfaceComponent = new ZoomableInterfaceComponent(frame())
        panel = new GUIPanel(component)
        frame().add(panel)
        val currentlyFocused: Window = FocusManager.getCurrentManager.getActiveWindow
        frame().setLocationRelativeTo(currentlyFocused)
        frame().setLocationByPlatform(true)
        frame().setVisible(true)
        currentlyFocused.toFront()
        component.open(path)
        val c: Array[Component] = component.workspace.viewWidget.controlStrip.getComponents
        for (co <- c) {
          if (co.isInstanceOf[SpeedSliderPanel]) {
            co.setVisible(false)
            (co.asInstanceOf[SpeedSliderPanel]).setValue(0)
          }
        }
        frame.pack()
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
        frame.addWindowListener(new WindowAdapter() {
          override def windowClosing(windowEvent: WindowEvent) {
            val options: Array[AnyRef] = Array("Close Model", "Run in Background", "Cancel")
            val n: Int = JOptionPane.showOptionDialog(frame, "Close the model, run it in the background, or do nothing?", null, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options(2))
            n match {
              case 0 =>
                try {
                  LevelsSpace.closeModel(levelsSpaceNumber)
                } catch {
                  case e: ExtensionException => throw new RuntimeException(e)
                  case e: HaltException =>
                }
              case 1 => hide()
              case 2 =>
            }
          }
        })
        val newMenuBar = new JMenuBar()
        val zoomMenuClass = Class.forName("org.nlogo.app.ZoomMenu")
        newMenuBar.add(zoomMenuClass.newInstance().asInstanceOf[org.nlogo.swing.Menu])
        frame.setJMenuBar(newMenuBar)
        component
      }
    })
  init()

  def setSpeed(d: Double): Unit = {
    val c: Array[Component] = component.workspace.viewWidget.controlStrip.getComponents
    for (co <- c) {
      if (co.isInstanceOf[SpeedSliderPanel]) {
        (co.asInstanceOf[SpeedSliderPanel]).setValue(d.toInt)
      }
    }
  }

  def workspace: AbstractWorkspace = component.workspace

  def frame(): JFrame = _frame
}

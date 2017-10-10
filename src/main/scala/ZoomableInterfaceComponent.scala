package org.nlogo.ls.gui

import java.awt._
import java.awt.event.{ComponentEvent, ComponentListener, ContainerEvent, ContainerListener}
import javax.swing.{ JFrame, JLayeredPane }

import org.nlogo.core.{ Widget => CoreWidget }
import org.nlogo.api.{CompilerServices, RandomServices}
import org.nlogo.window.Events.ZoomedEvent
import org.nlogo.window._

import scala.collection.mutable.{HashMap => MMap, MutableList => MList}

import scala.language.implicitConversions

trait ZoomableContainer
  extends ComponentListener
  with ContainerListener {

  import AwtScalable._

  private var _zoomFactor = 1.0

  val zoomMin             = 0.1

  val zoomableComponents: MList[Component] = MList.empty[Component]
  val unitAttributes: MMap[Component, ScalableAttributes] = MMap.empty[Component, ScalableAttributes]

  def zoomFactor          = _zoomFactor

  def zoomByStep(z: Int): Unit = {
    modifyZoomSteps(z)
    zoomChildWidgets()
  }

  def zoomReset(): Unit = {
    _zoomFactor = 1.0
    zoomChildWidgets()
  }

  def unitScaleFactor: Double = 1.0 / zoomFactor

  def registerZoomableComponent(c: Component) = {
    zoomableComponents += c
    recursively(c, registerScalableAttributes)
  }

  private def modifyZoomSteps(step: Int): Unit =
    _zoomFactor = (_zoomFactor + (step * 0.1)) max zoomMin

  private def registerScalableAttributes(c: Component): Unit = {
    unitAttributes += c -> (c.scalableAttributes scale unitScaleFactor)
    c.addComponentListener(this)
    c match {
      case container: Container => container.addContainerListener(this)
      case _ =>
    }
  }

  private def deregisterScalableAttributes(c: Component): Unit = {
    unitAttributes -= c
    c.removeComponentListener(this)
    c match {
      case container: Container => container.removeContainerListener(this)
      case _ =>
    }
  }

  private def zoomChildWidgets(): Unit = {
    zoomableComponents.foreach(_.removeComponentListener(this))
    zoomableComponents.foreach(zoomComponent)
    zoomableComponents.foreach(_.addComponentListener(this))
  }

  def zoomComponent(c: Component): Unit =
    recursively(c, { (com: Component) =>
      com.removeComponentListener(this)
      com.scaleTo(unitAttributes(c) scale zoomFactor)
      com.invalidate()
      com.validate()
      com.addComponentListener(this)
    })

  private def recursively(c: Component, f: Component => Unit): Unit = {
    f(c)
    c match {
      case container: Container =>
        container.getComponents.foreach(recursively(_, f))
      case _                    =>
    }
  }

  override def componentShown(componentEvent: ComponentEvent): Unit = ()

  override def componentHidden(componentEvent: ComponentEvent): Unit = ()

  override def componentMoved(componentEvent: ComponentEvent): Unit = {
    val component = componentEvent.getComponent
    unitAttributes(component) = component.scalableAttributes scale unitScaleFactor
  }

  override def componentResized(componentEvent: ComponentEvent): Unit = {
    val component = componentEvent.getComponent
    unitAttributes(component) = component.scalableAttributes scale unitScaleFactor
  }

  override def componentAdded(containerEvent: ContainerEvent): Unit =
    recursively(containerEvent.getChild, registerScalableAttributes)

  override def componentRemoved(containerEvent: ContainerEvent): Unit =
    recursively(containerEvent.getChild, deregisterScalableAttributes)
}

object AwtScalable {
  implicit def toScalableComponent(c: Component): ScalableComponent =
    new ScalableComponent(c)

  class ScalableComponent(c: Component) {
    def scaleTo(scalableAttributes: ScalableAttributes): Unit = {
      import scalableAttributes._
      c.setLocation(location)
      c.setSize(size)
      c.setFont(font)
    }

    def scalableAttributes: ScalableAttributes =
      ScalableAttributes(c.getLocation, c.getSize, c.getFont)
  }

  // the reason to track all of these as doubles is that otherwise rounding errors
  // accumulate and gradually skew positions
  class ScalableAttributes(x: Double,
                           y: Double,
                           width: Double,
                           height: Double,
                           fontSize: Double,
                           baseFont: Font) {
    def scale(d: Double): ScalableAttributes =
      new ScalableAttributes(x * d, y * d, width * d, height * d, fontSize * d, baseFont)

    val font: Font      = baseFont.deriveFont(fontSize.toFloat)

    val location: Point = new Point(x.ceil.toInt, y.ceil.toInt)

    val size: Dimension = new Dimension(width.ceil.toInt, height.ceil.toInt)
  }

  object ScalableAttributes {
    def apply(location: Point, size: Dimension, font: Font): ScalableAttributes =
      new ScalableAttributes(location.getX, location.getY, size.getWidth, size.getHeight, font.getSize, font)
  }
}

class ZoomableInterfacePanel(viewWidget: ViewWidgetInterface,
                             compiler: CompilerServices,
                             random: RandomServices,
                             plotManager: org.nlogo.plot.PlotManager,
                             editorFactory: EditorFactory)
  extends InterfacePanelLite(viewWidget, compiler, random, plotManager, editorFactory)
  with Events.ZoomedEvent.Handler
  with ZoomableContainer {

  registerZoomableComponent(viewWidget.asInstanceOf[Widget])

  override def loadWidget(coreWidget: CoreWidget): Widget = {
    val widget = super.loadWidget(coreWidget)
    registerZoomableComponent(widget)
    widget
  }

  override def isZoomed: Boolean = zoomFactor != 1.0

  override def handle(zoomedEvent: ZoomedEvent): Unit =
    if (zoomedEvent.action == 0)
      zoomReset()
    else
      zoomByStep(zoomedEvent.action)
}

class ZoomableInterfaceComponent(frame: JFrame, is3D: Boolean) extends InterfaceComponent(frame, is3D) {
  override protected def createInterfacePanel(workspace: GUIWorkspace) = {
    new ZoomableInterfacePanel(workspace.viewWidget, workspace.compilerServices, workspace,
      workspace.plotManager, liteEditorFactory)
  }
}

package org.nlogo.ls

import java.awt.image.BufferedImage
import java.io.IOException
import java.util.concurrent.Callable
import javax.swing.JFrame

import org.nlogo.workspace.AbstractWorkspaceScala
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.api.{ReporterRunnable, ExtensionException}
import org.nlogo.nvm.HaltException


class HeadlessChildModel @throws(classOf[InterruptedException]) @throws(classOf[ExtensionException]) @throws(classOf[HaltException]) @throws(classOf[IOException]) (parentWorkspace: AbstractWorkspaceScala, path: String, modelID: Int)
extends ChildModel(parentWorkspace, modelID) {
  val workspace = HeadlessWorkspace.newInstance
  workspace.open(path)

  var frame: Option[ImageFrame] = None

  override def show = {
    val f = frame.getOrElse {
      parentWorkspace waitForResult new ReporterRunnable[ImageFrame] {
        def run: ImageFrame = {
          val image = workspace.exportView
          new ImageFrame(image, frameTitle)
        }
      }
    }
    frame = Some(f)
    super.show
    updateView
  }

  def updateView = onEDT{ frame.foreach { f =>
    if (f.isVisible) f.updateImage(workspace.exportView)
  }}

  def setSpeed(d: Double) = {}

  override def ask(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): FutureJob[Unit] = super.ask(code, lets, args).andThen(r => {updateView; r})
}

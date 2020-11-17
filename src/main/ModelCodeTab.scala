package org.nlogo.ls.gui

import java.awt.event.ActionEvent
import java.io.{FileReader, FileWriter}
import java.net.URI
import javax.swing.{ AbstractAction, JButton, JOptionPane }

import org.nlogo.api.{ ExtensionException, ModelReader, ModelSection, ModelType, Version }
import org.nlogo.app.{ Tabs, App }
import org.nlogo.app.codetab.{ ProceduresMenu, CodeTab }
import org.nlogo.awt.UserCancelException
import org.nlogo.core.{ I18N, Model }
import org.nlogo.fileformat
import org.nlogo.swing.{ ToolBar, ToolBarActionButton, OptionDialog }, ToolBar.Separator
import org.nlogo.util.Utils
import org.nlogo.window.Events.ModelSavedEvent
import org.nlogo.workspace.{ AbstractWorkspaceScala, ModelTracker, OpenModel, OpenModelFromURI, SaveModel, ModelsLibrary }

import java.nio.file.Paths

class ModelCodeTab(workspace: AbstractWorkspaceScala, tabs: Tabs, modelManager: ModelManager)
extends CodeTab(workspace, tabs)
with ModelSavedEvent.Handler {
  val tabName            = workspace.getModelFileName
  val filePath           = workspace.getModelPath
  var modelSource        = ""
  var currentModel       = Option.empty[Model]

  setIndenter(true)

  locally {
    val loader = fileformat.basicLoader
    val controller = new OpenModel.Controller {
      def errorOpeningURI(uri: URI, exception: Exception): Unit = {
        throw new ExtensionException("Levelspace encountered an error while opening: " + Paths.get(uri).toString + ". " + exception.toString)
      }
      def invalidModel(uri: URI): Unit = {
        throw new ExtensionException("Levelspace couldn't open: " + Paths.get(uri).toString)
      }
      def invalidModelVersion(uri: URI, version: String): Unit = {
        throw new ExtensionException("Levelspace couldn't open invalid NetLogo model: " + Paths.get(uri).toString)
      }
      def errorAutoconvertingModel(failure: fileformat.FailedConversionResult): Option[Model] = None
      def shouldOpenModelOfDifferingArity(arity: Int, version: String): Boolean = false
      def shouldOpenModelOfLegacyVersion(version: String): Boolean = true
      def shouldOpenModelOfUnknownVersion(version: String): Boolean = true
    }
    OpenModelFromURI(Paths.get(filePath).toUri, controller, loader, fileformat.defaultConverter, Version).foreach { model =>
      currentModel = Some(model)
      innerSource = model.code
    }

    // All paths will be absolute, so this is okay
    if (ModelsLibrary.getModelPaths contains filePath) {
      text.setEditable(false)
      JOptionPane.showMessageDialog(App.app.frame,
        "<html><p style='width: 400px;'>" +
        "Because this is a models library model, you will not be able to make any changes to the code. If you wish " +
        "to make changes, copy the file to a different location, update your parent model with the new location, and " +
        "reopen the model." +
        "</p></html>",
        "Warning",
        JOptionPane.WARNING_MESSAGE)
    }
  }


  val tabManager   = tabs.getTabManager
  protected var isDirty  = false

  override def getAdditionalToolBarComponents = Seq(new ToolBarActionButton(FileCloseAction))

  def close(): Unit = {
    try {
      if (dirty && userWantsToSaveFile())
        save()
      tabManager.removeTab(this)
      modelManager.removeTab(this)
    } catch {
      case e: UserCancelException =>
    }
  }

  private def userWantsToSaveFile(): Boolean = {
    val options = Array[AnyRef](
      I18N.gui.get("common.buttons.save"),
      "Discard",
      I18N.gui.get("common.buttons.cancel"))
    val message = "Do you want to save the changes you made to " + filePath + "?"
    OptionDialog.showMessage(this, I18N.gui.get("common.messages.warning"), message, options) match {
      case 0 => true
      case 1 => false
      case _ => throw new UserCancelException
    }
  }

  private object FileCloseAction extends AbstractAction("Close") {
    override def actionPerformed(actionEvent: ActionEvent): Unit = close()
  }

  val originalModelSource = modelSource

  override def handle(modelSavedEvent: ModelSavedEvent): Unit =
    save()

  def save(): Unit = {
    val loader = fileformat.basicLoader
    val controller = new SaveModel.Controller {
      def chooseFilePath(modelType: ModelType): Option[URI] = {
        Some(Paths.get(workspace.getModelPath).toUri)
      }
      def shouldSaveModelOfDifferingVersion(version: String): Boolean = false
      // shouldn't see invalid file format
      def warnInvalidFileFormat(format: String): Unit = {
        throw new ExtensionException("Internal LevelSpace error: invalid file format: " + format)
      }
    }
    currentModel = currentModel.map(_.copy(code = innerSource)) orElse Some(Model(code = innerSource))
    currentModel.foreach { model =>
      SaveModel(model, loader, controller, workspace, Version).foreach {
        _.apply().foreach { _ =>
          changedSourceWarning()
          dirty = false
        }
      }
    }
  }

  def changedSourceWarning(): Unit =
    if (modelSource != originalModelSource) {
      errorLabel.setVisible(true)
      errorLabel.setText("Warning: Changes made to this code will not affect models until they are reloaded")
    } else {
      errorLabel.setVisible(false)
    }
}

package org.nlogo.ls.gui

import java.awt.event.ActionEvent
import java.io.{FileReader, FileWriter}
import javax.swing.JButton

import org.nlogo.api.{ExtensionException, ModelReader, ModelSection, Version}
import org.nlogo.core.{I18N}
import org.nlogo.app
import org.nlogo.app.{ProceduresMenu, CodeTab, Tabs}
import org.nlogo.awt.UserCancelException
import org.nlogo.fileformat
import org.nlogo.swing.ToolBar
import org.nlogo.swing.ToolBar.Separator
import org.nlogo.swing.ToolBarActionButton
import org.nlogo.util.Utils
import org.nlogo.window.Events.ModelSavedEvent
import org.nlogo.workspace.{ AbstractWorkspaceScala, ModelTracker, OpenModel, SaveModel }

import java.nio.file.Paths

class ModelCodeTab(workspace: AbstractWorkspaceScala,
                   tabs: Tabs,
                   modelManager: ModelManager)
  extends CodeTab(workspace)
  with ModelSavedEvent.Handler {

  val tabName            = workspace.getModelFileName
  val filePath           = workspace.getModelPath
  var modelSource        = ""

  setIndenter(true)

  locally {
    val loader =
      fileformat.standardLoader(workspace.compiler.compilerUtilities, workspace.autoConvert _)
    val controller = new OpenModel.Controller {
      def errorOpeningURI(uri: java.net.URI,exception: Exception): Unit = {
        throw new ExtensionException("Levelspace encountered an error while opening: " + Paths.get(uri).toString + ". " + exception.toString)
      }
      def invalidModel(uri: java.net.URI): Unit = {
        throw new ExtensionException("Levelspace couldn't open: " + Paths.get(uri).toString)
      }
      def invalidModelVersion(uri: java.net.URI,version: String): Unit = {
        throw new ExtensionException("Levelspace couldn't open invalid NetLogo model: " + Paths.get(uri).toString)
      }
      def shouldOpenModelOfDifferingArity(arity: Int,version: String): Boolean = false
      def shouldOpenModelOfLegacyVersion(version: String): Boolean = true
      def shouldOpenModelOfUnknownVersion(version: String): Boolean = true
    }
    OpenModel(Paths.get(filePath).toUri, controller, loader, Version).foreach { model =>
      innerSource = model.code
    }
  }

  protected var isDirty  = false

  override def getToolBar: ToolBar = {
    new ToolBar {
      override def addControls(): Unit = {
        add(new ToolBarActionButton(org.nlogo.app.FindDialog.FIND_ACTION))
        add(new ToolBarActionButton(compileAction))
        add(new Separator)
        add(new JButton(new FileCloseAction))
        add(new Separator)
        add(new ProceduresMenu(ModelCodeTab.this))
      }
    }
  }

  def close(): Unit = {
    try {
      if (isDirty && userWantsToSaveFile)
        save()
      tabs.remove(this)
      modelManager.removeTab(this)
    } catch {
      case e: UserCancelException =>
    }
  }

  override def dirty(): Unit = {
    isDirty = true
    super.dirty()
  }

  private def userWantsToSaveFile: Boolean = {
    val options = Array[AnyRef](
      I18N.gui.get("common.buttons.save"),
      "Discard",
      I18N.gui.get("common.buttons.cancel"))
    val message = "Do you want to save the changes you made to " + filePath + "?"
    org.nlogo.swing.OptionDialog.show(this, I18N.gui.get("common.messages.warning"), message, options) match {
      case 0 => true
      case 1 => false
      case _ => throw new UserCancelException
    }
  }

  class FileCloseAction extends javax.swing.AbstractAction("Close") {
    override def actionPerformed(actionEvent: ActionEvent): Unit = {
      close()
    }
  }

  val originalModelSource = modelSource

  override def handle(modelSavedEvent: ModelSavedEvent): Unit =
    save()

  def save(): Unit = {
    println("saving code tab to: " + workspace.getModelPath)
    println("model type: " + workspace.getModelType)
    println("contents: " + innerSource)
    val loader = fileformat.standardLoader(workspace.compiler.compilerUtilities, workspace.autoConvert _)
    val controller = new SaveModel.Controller {
      def chooseFilePath(modelType: org.nlogo.api.ModelType): Option[java.net.URI] = {
        Some(Paths.get(workspace.getModelPath).toUri)
      }
      def shouldSaveModelOfDifferingVersion(version: String): Boolean = false
      // shouldn't see invalid file format
      def warnInvalidFileFormat(format: String): Unit = {
        throw new ExtensionException("Internal LevelSpace error: invalid file format: " + format)
      }
    }
    SaveModel(org.nlogo.core.Model(code = innerSource),
      loader, controller, workspace, Version).foreach {
        _.apply().foreach { _ =>
          println("file saved")
          changedSourceWarning()
          isDirty = false
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

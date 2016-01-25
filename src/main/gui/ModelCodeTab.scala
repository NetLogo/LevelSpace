package gui

import java.awt.event.ActionEvent
import java.io.{FileReader, FileWriter}
import javax.swing.JButton

import org.nlogo.api.{I18N, ModelReader, ModelSection}
import org.nlogo.app
import org.nlogo.app.{ProceduresMenu, CodeTab, Tabs}
import org.nlogo.awt.UserCancelException
import org.nlogo.swing.ToolBar
import org.nlogo.swing.ToolBar.Separator
import org.nlogo.util.Utils
import org.nlogo.window.Events.ModelSavedEvent
import org.nlogo.workspace.AbstractWorkspace

class ModelCodeTab(workspace: AbstractWorkspace,
                         tabs: Tabs,
                         modelManager: ModelManager)
  extends CodeTab(workspace)
  with ModelSavedEvent.Handler {

  val tabName            = workspace.getModelFileName
  val filePath           = workspace.getModelPath
  var modelSource        = ""
  private val fileReader = new FileReader(filePath)

  setIndenter(true)

  try {
    modelSource = Utils.reader2String(fileReader)
    val modelMap    = ModelReader.parseModel(modelSource)
    innerSource(modelMap.get(ModelSection.Code).mkString("\n"))
  } finally {
    fileReader.close()
  }

  protected var isDirty  = false

  override def getToolBar: ToolBar = {
    new ToolBar {
      override def addControls(): Unit = {
        add(new JButton(org.nlogo.app.FindDialog.FIND_ACTION))
        add(new JButton(compileAction))
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
    // so we're just replacing the old source with the new, which is sort of cheating
    // in some future version of NetLogo, where ModelSaver doesn't use an App for saving
    // this code should make use of whatever mechanism is used there
    val nonCodeSource = modelSource.lines.dropWhile(_ != ModelReader.SEPARATOR)
    modelSource = innerSource + nonCodeSource.mkString("\n", "\n", "\n")
    val fileWriter = new FileWriter(filePath)
    try {
      fileWriter.write(modelSource)
      changedSourceWarning()
      isDirty = false
    } finally {
      fileWriter.close()
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

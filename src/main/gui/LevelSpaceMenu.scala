package gui

import java.awt.FileDialog.{LOAD => LOADFILE, SAVE => SAVEFILE}
import java.awt.event.ActionEvent
import java.io.{File, FileWriter, IOException}
import javax.swing._

import org.nlogo.api.ModelSections.{BufSaveable, Saveable}
import org.nlogo.api.{CompilerException, ExtensionException, ModelReader, ModelSections, Shape, Version}
import org.nlogo.app.{CodeTab, ModelSaver, App, Tabs}
import org.nlogo.awt.UserCancelException
import org.nlogo.shape.{VectorShape, LinkShape}
import org.nlogo.swing.FileDialog
import org.nlogo.util.Exceptions
import org.nlogo.workspace.{AbstractWorkspace, ModelsLibrary}

import scala.collection.JavaConversions._

trait ModelManager {
  def removeTab(tab: ModelCodeTab): Unit
  def existingTab(filePath: String): Option[CodeTab]
  def registerTab(filePath: String)
                 (f: AbstractWorkspace => ModelCodeTab): Option[ModelCodeTab]
}

class LevelSpaceMenu(tabs: Tabs, val backingModelManager: ModelManager)
  extends JMenu("LevelSpace") {

  import LevelSpaceMenu._

  val selectModel  = new SelectModelAction("Open Model in Code Tab", backingModelManager)
  val openModels   = new JMenu("Edit Open Models...")
  val newModel     = new NewModelAction("Create new LevelSpace Model", backingModelManager)

  add(selectModel)
  add(openModels)
  add(newModel)

  def addMenuItemsForOpenModels(modelPaths: Seq[String]) = {
    openModels.removeAll()
    modelPaths.foreach(addModelAction(openModels, _))
    if (openModels.getMenuComponentCount == 0)
      openModels.setEnabled(false)
    else
      openModels.setEnabled(true)
  }

  def replaceTab(oldTab: ModelCodeTab): Unit =
    newHeadlessBackedTab(oldTab.filePath).foreach {
      newTab => replaceSwingTab(oldTab, newTab)
    }

  private def addModelAction(menu: JMenu, filePath: String): Unit = {
    menu.add(new OpenModelAction(filePath, backingModelManager))
  }

  private def newHeadlessBackedTab(filePath: String): Option[ModelCodeTab] =
    backingModelManager.registerTab(filePath) { workspace =>
      new ModelCodeTab(workspace, tabs, backingModelManager)
    }

  private def replaceSwingTab(oldTab: ModelCodeTab, newTab: ModelCodeTab): Unit = {
    val i = tabs.getIndexOfComponent(oldTab)
    tabs.setComponentAt(i, newTab)
  }
}

object LevelSpaceMenu {
  abstract class NewTabAction(name: String, modelManager: ModelManager) extends AbstractAction(name) {
    val tabs = App.app.tabs

    def filePath: Option[String]

    def actingTab: Option[CodeTab] =
      filePath.flatMap(path => locateExistingTab(path) orElse createNewTab(path))

    private def locateExistingTab(path: String): Option[CodeTab] =
      modelManager.existingTab(path)

    private def createNewTab(path: String): Option[CodeTab] = {
      modelManager.registerTab(path) { workspace =>
        val tab = new ModelCodeTab(workspace, tabs, modelManager)
        tabs.addTab(tab.tabName, tab)
        tab
      }
    }

    override def actionPerformed(actionEvent: ActionEvent): Unit =
      actingTab.foreach(tabs.setSelectedComponent)
  }

  class OpenModelAction(fileName: String, modelManager: ModelManager)
    extends NewTabAction(fileName, modelManager) {
    override def filePath: Option[String] = Some(fileName)
  }

  class SelectModelAction(name: String, modelManager: ModelManager)
    extends NewTabAction(name, modelManager) {

    override def filePath: Option[String] = selectFile

    override def actingTab: Option[CodeTab] =
      try {
        super.actingTab
      } catch {
        case e: CompilerException =>
          // we shouldn't have to raise an exception here, we should just be able to open it, but
          // in order to do that, we'll need to change child models not to compile in their constructors
          throw new ExtensionException(filePath + " did not compile properly. There is probably something wrong " +
            "with its code. Exception said" + e.getMessage);
        case e: IOException =>
          throw new ExtensionException("There was no .nlogo file at the path: \"" + filePath + "\"")
      }


    private def selectFile: Option[String] =
      showLoadSelection.flatMap(path =>
        if (ModelsLibrary.getModelPaths.contains(path)) {
          showLibraryModelErrorMessage()
          None
        } else
          Some(path))

    private def showLoadSelection: Option[String] =
      try {
        Some(FileDialog.show(App.app.frame, "Load a LevelSpace Model...", LOADFILE))
      } catch {
        case e: UserCancelException =>
          Exceptions.ignore(e)
          None
      }

    private def showLibraryModelErrorMessage(): Unit =
      JOptionPane.showMessageDialog(
        App.app.frame,
        """|The model you selected is a library model, which cannot be opened in a LevelSpace code tab.
           |Please save the model elsewhere and try re-opening""".stripMargin)
  }

  class NewModelAction(name: String, modelManager: ModelManager)
    extends NewTabAction(name, modelManager) {

    override def filePath: Option[String] =
      try {
        FileDialog.setDirectory(App.app.workspace.getModelDir)
        val userEntry = FileDialog.show(App.app.frame, "Select a path for new Model...", SAVEFILE)
        // we basically need to write an empty NetLogo model in before we read...
        val fileName =
          if (userEntry.endsWith(".nlogo")) userEntry else userEntry + ".nlogo"
        if (new File(fileName).exists) {
          val fileAlreadyExists = "The file " + fileName + " already exists. Please choose a different name"
          throw new ExtensionException(fileAlreadyExists)
        }
        writeEmptyNetLogoFile(fileName)
      } catch {
        case e: UserCancelException =>
          Exceptions.ignore(e)
          None
      }

    private def writeEmptyNetLogoFile(fileName: String): Option[String] = {
      val modelString = new ModelSaver(EmptyNetLogoFile).save
      val fw = new FileWriter(fileName)
      try {
        fw.write(modelString)
        Some(fileName)
      } catch {
        case i: IOException => None
      } finally {
        fw.flush()
        fw.close()
      }
    }

    object EmptyNetLogoFile extends ModelSections {
      object EmptySaveable extends Saveable with BufSaveable {
        override def save: String = ""
        override def save(buf: StringBuilder): Unit = ()
      }

      override def procedureSource: String = ""
      override def aggregateManager: Saveable = EmptySaveable
      override def snapOn: Boolean = false
      override def previewCommands: String = ""
      override def linkShapes: Seq[Shape] =
        LinkShape.parseShapes(ModelReader.defaultLinkShapes, Version.version)
      override def hubnetManager: BufSaveable = EmptySaveable
      override def turtleShapes: Seq[Shape] =
        VectorShape.parseShapes(ModelReader.defaultShapes, Version.version)
      override def labManager: Saveable = EmptySaveable
      override def info: String = ""
      override def widgets: Seq[Saveable] = Seq()
      override def version: String = Version.version
    }
  }
}

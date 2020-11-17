package org.nlogo.ls.gui

import java.awt.FileDialog.{LOAD => LOADFILE, SAVE => SAVEFILE}
import java.awt.event.ActionEvent
import java.nio.file.{ Paths, Files }
import java.io.{File, FileWriter, IOException}
import javax.swing._

import org.nlogo.api.ModelSections.ModelSaveable
import org.nlogo.api.{ExtensionException, ModelSections, Version, Exceptions}
import org.nlogo.core.{CompilerException, Shape, ShapeParser}
import org.nlogo.app.{ModelSaver, App, Tabs}
import org.nlogo.app.codetab.CodeTab
import org.nlogo.awt.UserCancelException
import org.nlogo.fileformat
import org.nlogo.swing.FileDialog
import org.nlogo.workspace.{AbstractWorkspaceScala, ModelsLibrary, ModelTracker, SaveModel}

import scala.collection.JavaConversions._

trait ModelManager {
  def removeTab(tab: ModelCodeTab): Unit
  def existingTab(filePath: String): Option[CodeTab]
  def registerTab(filePath: String)
                 (f: AbstractWorkspaceScala => ModelCodeTab): Option[ModelCodeTab]
}

class LevelSpaceMenu(tabs: Tabs, val backingModelManager: ModelManager)
  extends JMenu("LevelSpace") {

  import LevelSpaceMenu._

  val tabManager   = tabs.getTabManager
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
    tabManager.replaceTab(oldTab,  newTab)
  }
}

  object LevelSpaceMenu {
    abstract class NewTabAction(name: String, modelManager: ModelManager) extends AbstractAction(name) {
      val tabs = App.app.tabs
      val tabManager   = tabs.getTabManager

      def filePath: Option[String]

      def actingTab: Option[CodeTab] =
        filePath.flatMap(path => locateExistingTab(path) orElse createNewTab(path))

      private def locateExistingTab(path: String): Option[CodeTab] =
        modelManager.existingTab(path)

      private def createNewTab(path: String): Option[CodeTab] = {
        modelManager.registerTab(path) { workspace =>
          val tab = new ModelCodeTab(workspace, tabs, modelManager)
          tabManager.addNewTab(tab, tab.tabName)
          tab
        }
      }

      override def actionPerformed(actionEvent: ActionEvent): Unit =
        actingTab.foreach(tabManager.setPanelsSelectedComponent)
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
            Some(FileDialog.showFiles(App.app.frame, "Load a LevelSpace Model...", LOADFILE))
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

      override def filePath: Option[String] = {
        FileDialog.setDirectory(App.app.workspace.getModelDir)

        val ws = App.app.workspace
        val loader = fileformat.basicLoader
        val controller = new SaveModel.Controller {
          def chooseFilePath(modelType: org.nlogo.api.ModelType): Option[java.net.URI] = {
            try {
              val userEntry = FileDialog.showFiles(App.app.frame, "Select a path for new Model...", SAVEFILE)
              // we basically need to write an empty NetLogo model in before we read...
              val fileName =
                if (userEntry.endsWith(".nlogo")) userEntry else userEntry + ".nlogo"
              val path = Paths.get(fileName)
              if (Files.exists(path)) {
                val fileAlreadyExists = s"The file $fileName already exists. Please choose a different name"
                throw new ExtensionException(fileAlreadyExists)
              }
              Some(path.toUri)
            } catch {
              case e: UserCancelException =>
                Exceptions.ignore(e)
                None
            }
          }
          def shouldSaveModelOfDifferingVersion(version: String): Boolean = true
          def warnInvalidFileFormat(format: String): Unit = {
            // we force users to save in NetLogo, so this doesn't happen
          }
        }
        val modelTracker = new ModelTracker {
          def compiler = App.app.workspace.compiler
          def getExtensionManager() = App.app.workspace.getExtensionManager
        }
        SaveModel(org.nlogo.core.Model(), loader, controller, modelTracker, Version).flatMap(_.apply().toOption.map(uri => Paths.get(uri).toString))
      }
    }
  }

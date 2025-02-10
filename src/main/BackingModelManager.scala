package org.nlogo.ls

import org.nlogo.ls.gui.{ModelCodeTab, LevelSpaceMenu, ModelManager}

import java.util.{ Map => JMap }

import org.nlogo.app.App
import org.nlogo.app.codetab.CodeTab
import org.nlogo.theme.ThemeSync
import org.nlogo.workspace.AbstractWorkspaceScala

import scala.collection.Map
import scala.collection.parallel.mutable.ParHashMap

trait LSModelManager extends ModelManager with ThemeSync {
  def updateChildModels(map: Map[Integer, ChildModel]): Unit = {}
  def guiComponent: LevelSpaceMenu = null
}

class BackingModelManager extends LSModelManager {
  override val guiComponent = new LevelSpaceMenu(App.app.tabManager, this)
  private val backingModels = ParHashMap.empty[String, (ChildModel, ModelCodeTab)]
  private var openModels    = Map.empty[String, ChildModel]
  private var models  = Seq[ChildModel]()

  override def updateChildModels(indexedModels: Map[Integer, ChildModel]): Unit = {
    models                = indexedModels.values.toSeq

    // toSeq.distinct preserves ordering, whereas toSet does not
    val modelPaths        = models.map(_.workspace.getModelPath).distinct

    val closedModelsPaths = (openModels.values.toSet &~ models.toSet).map(_.workspace.getModelPath)
    val newlyOpenedPaths  = (models.toSet &~ openModels.values.toSet).map(_.workspace.getModelPath)
    openModels            = (modelPaths zip models).toMap
    (closedModelsPaths intersect openModelPaths).foreach(replaceTabAtPath)
    (newlyOpenedPaths  intersect openModelPaths).foreach(replaceTabAtPath)
    guiComponent.addMenuItemsForOpenModels(modelPaths)
  }

  private def replaceTabAtPath(filePath: String): Unit =
    guiComponent.replaceTab(backingModels(filePath)._2)

  def openModelPaths: collection.Set[String] = backingModels.seq.keySet

  def existingTab(filePath: String): Option[CodeTab] =
    if (filePath == App.app.workspace.getModelPath)
      Some(App.app.tabManager.mainCodeTab)
    else
      backingModels.get(filePath).map(_._2)

  def removeTab(tab: ModelCodeTab): Unit = {
    if (! openModelPaths(tab.filePath))
      backingModels.get(tab.filePath).foreach(_._1.kill())
    backingModels -= tab.filePath
  }

  def registerTab(filePath: String, model: ChildModel)
                 (f: AbstractWorkspaceScala => ModelCodeTab): Option[ModelCodeTab] = {
    if (backingModels.get(filePath).isDefined) {
      None
    } else {
      val tab = f(model.workspace)
      backingModels += filePath ->(model, tab)
      Some(tab)
    }
  }

  def registerTab(filePath: String)
                 (f: AbstractWorkspaceScala => ModelCodeTab): Option[ModelCodeTab] = {
    if (backingModels.get(filePath).isDefined) {
      None
    } else {
      val newModel =
        openModels.getOrElse(filePath, new HeadlessChildModel(App.app.workspace, filePath, -1))
      registerTab(filePath, newModel)(f)
    }
  }

  def syncTheme() {
    guiComponent.syncTheme()
    models.foreach(_.syncTheme())
    backingModels.values.foreach(_._2.syncTheme())
  }
}

// this is an example of the
class HeadlessBackingModelManager extends LSModelManager {
  def removeTab(tab: ModelCodeTab): Unit = {}
  def existingTab(filePath: String): Option[ModelCodeTab] = None
  def registerTab(filePath: String)
                 (f: AbstractWorkspaceScala => ModelCodeTab): Option[ModelCodeTab] = None
  def syncTheme() {}
}

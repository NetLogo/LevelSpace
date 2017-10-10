package org.nlogo.ls

import org.nlogo.ls.gui.{ModelCodeTab, LevelSpaceMenu, ModelManager}

import java.util.{ Map => JMap }

import org.nlogo.app.App
import org.nlogo.app.codetab.CodeTab
import org.nlogo.workspace.AbstractWorkspace

import scala.collection.JavaConverters._
import scala.collection.parallel.mutable.ParHashMap

trait LSModelManager extends ModelManager {
  def updateChildModels(map: JMap[java.lang.Integer, ChildModel]): Unit = {}
  def guiComponent: LevelSpaceMenu = null
}

class BackingModelManager extends LSModelManager {
  override val guiComponent  = new LevelSpaceMenu(App.app.tabs, this)
  private val backingModels = ParHashMap.empty[String, (ChildModel, ModelCodeTab)]
  private var openModels    = Map.empty[String, ChildModel]

  override def updateChildModels(indexedModels: JMap[java.lang.Integer, ChildModel]): Unit = {
    val models            = indexedModels.values.asScala

    // toSeq.distinct preserves ordering, whereas toSet does not
    val modelPaths        = models.map(_.workspace.getModelPath).toSeq.distinct

    val closedModelsPaths = (openModels.values.toSet &~ models.toSet).map(_.workspace.getModelPath)
    val newlyOpenedPaths  = (models.toSet &~ openModels.values.toSet).map(_.workspace.getModelPath)
    openModels            = (modelPaths zip models).toMap
    (closedModelsPaths intersect openModelPaths).foreach(replaceTabAtPath)
    (newlyOpenedPaths  intersect openModelPaths).foreach(replaceTabAtPath)
    guiComponent.addMenuItemsForOpenModels(modelPaths)
  }

  private def replaceTabAtPath(filePath: String) =
    guiComponent.replaceTab(backingModels(filePath)._2)

  def openModelPaths = backingModels.seq.keySet

  def existingTab(filePath: String): Option[CodeTab] =
    if (filePath == App.app.workspace.getModelPath)
      Some(App.app.tabs.codeTab)
    else
      backingModels.get(filePath).map(_._2)

  def removeTab(tab: ModelCodeTab): Unit = {
    if (! openModelPaths(tab.filePath))
      backingModels.get(tab.filePath).foreach(_._1.kill())
    backingModels -= tab.filePath
  }

  def registerTab(filePath: String, model: ChildModel)
                 (f: AbstractWorkspace => ModelCodeTab): Option[ModelCodeTab] = {
    if (backingModels.get(filePath).isDefined) {
      None
    } else {
      val tab = f(model.workspace)
      backingModels += filePath ->(model, tab)
      Some(tab)
    }
  }

  def registerTab(filePath: String)
                 (f: AbstractWorkspace => ModelCodeTab): Option[ModelCodeTab] = {
    if (backingModels.get(filePath).isDefined) {
      None
    } else {
      val newModel =
        openModels.getOrElse(filePath, new HeadlessChildModel(App.app.workspace, filePath, -1))
      registerTab(filePath, newModel)(f)
    }
  }
}

// this is an example of the
class HeadlessBackingModelManager extends LSModelManager {
  def removeTab(tab: ModelCodeTab): Unit = {}
  def existingTab(filePath: String): Option[ModelCodeTab] = None
  def registerTab(filePath: String)
                 (f: AbstractWorkspace => ModelCodeTab): Option[ModelCodeTab] = None
}

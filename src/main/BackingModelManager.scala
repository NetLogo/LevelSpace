import gui.{ModelProceduresTab, LevelSpaceMenu}

import org.nlogo.app.App
import org.nlogo.workspace.AbstractWorkspace

import scala.collection.JavaConversions._
import scala.collection.parallel.mutable.ParHashMap

class BackingModelManager extends gui.ModelManager {
  val guiComponent  = new LevelSpaceMenu(App.app.tabs, this)
  val backingModels = ParHashMap.empty[String, (ChildModel, ModelProceduresTab)]
  var openModels    = Map.empty[String, ChildModel]

  def updateChildModels(indexedModels: java.util.HashMap[java.lang.Integer, ChildModel]): Unit = {
    val models                  = indexedModels.values
    val modelPaths: Seq[String] = models.map(_.workspace().getModelPath).toSeq
    val closedModelsPaths       = (openModels.values.toSet &~ models.toSet).map(_.workspace().getModelPath)
    val newlyOpenedPaths        = (models.toSet &~ openModels.values.toSet).map(_.workspace().getModelPath)
    openModels                  = (modelPaths zip models).toMap
    (closedModelsPaths & openModelPaths).foreach(replaceTabAtPath)
    (newlyOpenedPaths & openModelPaths).foreach(replaceTabAtPath)
    guiComponent.addMenuItemsForOpenModels(
      models.map(_.workspace().getModelPath).toSeq)
  }

  private def replaceTabAtPath(filePath: String) =
    guiComponent.replaceTab(backingModels(filePath)._2)

  def openModelPaths = backingModels.seq.keySet

  def existingTab(filePath: String): Option[ModelProceduresTab] =
    backingModels.get(filePath).map(_._2)

  def removeTab(tab: ModelProceduresTab): Unit = {
    if (! openModelPaths(tab.filePath))
      backingModels.get(tab.filePath).foreach(_._1.kill())
    backingModels -= tab.filePath
  }

  def registerTab(filePath: String, model: ChildModel)
                 (f: AbstractWorkspace => ModelProceduresTab): Option[ModelProceduresTab] = {
    if (backingModels.get(filePath).isDefined) {
      None
    } else {
      val tab = f(model.workspace())
      backingModels += filePath ->(model, tab)
      Some(tab)
    }
  }

  def registerTab(filePath: String)
                 (f: AbstractWorkspace => ModelProceduresTab): Option[ModelProceduresTab] = {
    if (backingModels.get(filePath).isDefined) {
      None
    } else {
      val newModel =
        openModels.getOrElse(filePath, new HeadlessChildModel(App.app.workspace.world(), filePath, -1))
      registerTab(filePath, newModel)(f)
    }
  }
}


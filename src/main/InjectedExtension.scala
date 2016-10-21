package org.nlogo.ls

import org.nlogo.workspace.{AbstractWorkspaceScala, InMemoryExtensionLoader}
import org.nlogo.api.{DefaultClassManager, PrimitiveManager, Command, Reporter, Argument, ExtensionException, Context}
import org.nlogo.core.Syntax

class InjectedExtension(model: ChildModel) extends DefaultClassManager {
  def load(primManager: PrimitiveManager) = {
    primManager.addPrimitive("name", new Name)
    primManager.addPrimitive("set-name", new SetName)
    primManager.addPrimitive("show", new ModelCommand(model.show _))
    primManager.addPrimitive("hide", new ModelCommand(model.hide _))
    //primManager.addPrimitive("die",
  }

  class ModelCommand(cmd: () => Unit) extends Command {
    override def getSyntax = Syntax.commandSyntax()
    override def perform(args: Array[Argument], ctx: Context): Unit = cmd()
  }

  class Name extends Reporter {
    override def getSyntax = Syntax.reporterSyntax(ret = Syntax.NumberType)
    override def report(args: Array[Argument], ctx: Context): AnyRef = model.name
  }

  class SetName extends Command {
    override def getSyntax = Syntax.commandSyntax(List(Syntax.StringType))
    override def perform(args: Array[Argument], ctx: Context) = model.name = args(0).getString
  }
}




package org.nlogo.ls

import org.nlogo.nvm
import org.nlogo.api.{CommandTask, Context, Task}
import org.nlogo.core.Syntax

class ReportFromCommand extends CommandTask {
  @volatile var result: AnyRef = null

  override def syntax = Syntax.commandSyntax(List(Syntax.WildcardType))
  override def perform(ctx: Context, args: Array[AnyRef]) = this.result = args(0)
  override def toString = "report-from-command"
}

case class TaskReporter(task: Task) extends nvm.Reporter {
  override def report(ctx: nvm.Context) = task
}


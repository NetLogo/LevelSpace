package org.nlogo.ls

import org.nlogo.nvm.{CommandTask, Context, Task, Reporter}
import org.nlogo.api.Syntax

class ReportFromCommand extends CommandTask(null, new Array(1), List(), Array()) {
  @volatile var result: AnyRef = null

  override def perform(ctx: Context, args: Array[AnyRef]) = this.result = args(0)
  override def toString = "report-from-command"
}

case class TaskReporter(task: Task) extends Reporter {
  override def syntax = Syntax.reporterSyntax(Syntax.CommandTaskType)
  override def report(ctx: Context) = task
}


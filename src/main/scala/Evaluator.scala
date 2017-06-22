package org.nlogo.ls

import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.nlogo.api.{SimpleJobOwner, World}
import org.nlogo.core.{AgentKind, LogoList, Nobody}
import org.nlogo.nvm.{Procedure, Reporter}
import org.nlogo.prim.{_constboolean, _constdouble, _constlist, _conststring, _nobody}
import org.nlogo.workspace.AbstractWorkspaceScala

class Evaluator(modelID: Int, name: String, ws: AbstractWorkspaceScala, parentWorld: World) {

  val owner = new SimpleJobOwner(name, ws.world.mainRNG, AgentKind.Observer)

  private val commandRunnerProc = ws.compileCommands("run []")
  private val reporterRunnerProc = ws.compileReporter("runresult [ 0 ]")

  private val lambdaCache = CacheBuilder.newBuilder.build(new CacheLoader[String, Reporter] {
    override def load(code: String) = compileProcedureReporter(code)
  })

  def command(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): NotifyingJob =
    run(code, lets, args, "__apply", commandRunner)

  def report(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): NotifyingJob =
    run(code, lets, args, "__apply-result", reporterRunner)

  def run(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef], apply: String, runner: (Reporter, Seq[AnyRef]) => Procedure): NotifyingJob = {
    val fullCode = s"[ [ __levelspace-argument-list ${lets.map(_._1).mkString(" ")} ] -> $apply [ $code ] __levelspace-argument-list]"

    val fullArgs = LogoList.fromVector(args.toVector) +: lets.map(_._2)

    val proc = ErrorUtils.handle(modelID, name) {
      runner(getLambda(fullCode), fullArgs)
    }
    val job = new NotifyingJob(parentWorld, modelID, name, ws, owner, ws.world.observers, proc)
    ws.jobManager.addJob(job, waitForCompletion = false)

    job
  }

  private def getLambda(code: String): Reporter = try {
    lambdaCache.get(code)
  } catch {
    // strip google's exception wrapping
    case ex: Exception => throw ex.getCause
  }

  private def compileProcedureReporter(code: String): Reporter =
    ws.compileReporter(code).code(0).args(0)

  private def makeConstantReporter(value:Object): Reporter =
    value match {
      case b:java.lang.Boolean  => new _constboolean(b)
      case d:java.lang.Double   => new _constdouble(d)
      case l:LogoList           => new _constlist(l)
      case s:String             => new _conststring(s)
      case Nobody               => new _nobody
      case _                    => throw new IllegalArgumentException(value.getClass.getName)
    }

  private def makeArgumentArray(task: Reporter, args: Seq[AnyRef]): Array[Reporter] =
    (task +: args.map(makeConstantReporter)).toArray

  private def reporterRunner(task: Reporter, args: Seq[AnyRef]): Procedure = {
    reporterRunnerProc.code(0).args(0).args = makeArgumentArray(task, args)
    reporterRunnerProc
  }

  private def commandRunner(task: Reporter, args: Seq[AnyRef]): Procedure = {
    commandRunnerProc.code(0).args = makeArgumentArray(task, args)
    commandRunnerProc
  }
}

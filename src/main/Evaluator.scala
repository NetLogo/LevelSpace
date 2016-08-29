package org.nlogo.ls

import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.nlogo.api.{ExtensionException, SimpleJobOwner, World, AnonymousCommand, AnonymousProcedure, Context}
import org.nlogo.core.{AgentKind, LogoList, Nobody, Syntax}
import org.nlogo.nvm
import org.nlogo.nvm.{Job, Procedure, Reporter}
import org.nlogo.prim.{_constboolean, _constdouble, _constlist, _conststring, _nobody}
import org.nlogo.workspace.AbstractWorkspaceScala



class Evaluator(name: String, ws: AbstractWorkspaceScala) {

  val owner = new SimpleJobOwner(name, ws.world.mainRNG, AgentKind.Observer);

  private val commandRunner = ws.compileCommands("run []")
  private val reporterRunner = ws.compileReporter("runresult [ 0 ]")

  private val lambdaCache = CacheBuilder.newBuilder.build(new CacheLoader[String, Reporter] {
    override def load(code: String) = compileProcedureReporter(code)
  })

  def command(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): FutureJob[Unit] =
    run(code, lets, args, "__apply", commandRunner _, _ => Unit)

  def report(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): FutureJob[AnyRef] =
    run(code, lets, args, "__apply-result", reporterRunner _, checkResult)

  def run[T](code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef], apply: String, runner: (Reporter, Seq[AnyRef]) => Procedure, handleResult: AnyRef => T): FutureJob[T] = {
    val fullCode = s"[ [ __levelspace-argument-list ${lets.map(_._1).mkString(" ")} ] -> $apply [ $code ] __levelspace-argument-list]"

    val fullArgs = LogoList.fromVector(args.toVector) +: lets.map(_._2)

    val proc = runner(getLambda(fullCode), fullArgs)
    val job = ws.jobManager.makeConcurrentJob(owner, ws.world.observers, ws, proc)
    ws.jobManager.addJob(job, waitForCompletion = false)
    (w: World) => UnlockAndBlock(w) {
      while (job.state != Job.REMOVED) {
        job.synchronized { job.wait(50) }
      }
      job.result match {
        case e: Exception => throw e
        case x => handleResult(x)
      }
    }
  }

  private def getLambda(code: String): Reporter = try {
    lambdaCache.get(code)
  } catch {
    // strip google's exception wrapping
    case ex: Exception => throw ex.getCause
  }

  private def checkResult(result: AnyRef): AnyRef = result match {
    case (_: org.nlogo.agent.Agent | _: org.nlogo.agent.AgentSet) =>
      throw new ExtensionException("You cannot report agents or agentsets from LevelSpace models.")
    case l: LogoList => {
      l.foreach(checkResult _)
      l
    }
    case x => x
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
    (task +: args.map(makeConstantReporter _)).toArray

  private def reporterRunner(task: Reporter, args: Seq[AnyRef]): Procedure = {
    reporterRunner.code(0).args(0).args = makeArgumentArray(task, args)
    reporterRunner
  }

  private def commandRunner(task: Reporter, args: Seq[AnyRef]): Procedure = {
    commandRunner.code(0).args = makeArgumentArray(task, args)
    commandRunner
  }
}

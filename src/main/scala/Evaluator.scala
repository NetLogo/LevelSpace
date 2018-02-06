package org.nlogo.ls

import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.nlogo.api.SimpleJobOwner
import org.nlogo.core.{AgentKind, LogoList, Nobody}
import org.nlogo.nvm.{ExclusiveJob, Procedure, Reporter}
import org.nlogo.prim.{_constboolean, _constdouble, _constlist, _conststring, _nobody}
import org.nlogo.workspace.AbstractWorkspace

class Evaluator(modelID: Int, name: String, ws: AbstractWorkspace, parentWS: AbstractWorkspace) {

  val owner = new SimpleJobOwner(name, ws.world.mainRNG, AgentKind.Observer)

  private val commandRunnerProc = ws.compileCommands("run []")
  private val reporterRunnerProc = ws.compileReporter("runresult [ 0 ]")

  private val lambdaCache = CacheBuilder.newBuilder.build(new CacheLoader[String, Reporter] {
    override def load(code: String): Reporter = compileProcedureReporter(code)
  })

  def command(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef], parallel: Boolean): Notifying[Unit] =
    run(code, lets, args, "__apply", parallel, commandRunner).map(ignore)

  def report(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef], parallel: Boolean): Notifying[AnyRef] =
    run(code, lets, args, "__apply-result", parallel, reporterRunner).map(checkResult)


  private val fullCodePrefix = "[[__lsargs "
  private val fullCodeMap = "]-> "
  private val fullCodeSuffix1 = "["
  private val fullCodeSuffix2 = "]__lsargs]"
  private val sbInitCap = fullCodePrefix.length +
                         fullCodeMap.length +
                         fullCodeSuffix1.length +
                         fullCodeSuffix2.length

  def run(code: String, lets: Seq[(String, AnyRef)],
          args: Seq[AnyRef],
          apply: String,
          parallel: Boolean,
          runner: (Reporter, LogoList, Seq[(String, AnyRef)]) => Procedure): Notifying[AnyRef] =
    ErrorUtils.handle(modelID, name) {
      val fullCodeBuilder = new StringBuilder(sbInitCap + code.length, fullCodePrefix)
      lets.foreach(l => fullCodeBuilder.append(l._1).append(' '))
      fullCodeBuilder
        .append(fullCodeMap)
        .append(apply)
        .append(fullCodeSuffix1)
        .append(code)
        .append(fullCodeSuffix2)

      val fullCode = fullCodeBuilder.toString

      val proc = runner(getLambda(fullCode), LogoList.fromVector(args.toVector), lets)

      val job: Notifying[AnyRef] = if (parallel) {
        val job = new NotifyingJob(parentWS.world, owner, ws.world.observers, proc)
        ws.jobManager.addJob(job, waitForCompletion = false)
        job
      } else {
        val j = new ExclusiveJob(owner, ws.world.observers, proc, 0, null, owner.random, ExclusiveJob.initialComeUpForAir)
        j.run()
        new FakeNotifier[AnyRef](j.result)
      }

      job.map {
        case ex: Exception => throw ErrorUtils.wrap(modelID, name, ex)
        case r => r
      }
    }

  @inline
  private def ignore(result: AnyRef): Unit = {}

  private def checkResult(result: AnyRef): AnyRef = result match {
    case (_: org.nlogo.agent.Agent | _: org.nlogo.agent.AgentSet) =>
      throw ErrorUtils.wrap(modelID, name, "You cannot report agents or agentsets from LevelSpace models.")
    case l: LogoList =>
      l.foreach(checkResult)
      l
    case x => x
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

  private def makeArgumentArray(task: Reporter, lsArgs: LogoList, lets: Seq[(String, AnyRef)]): Array[Reporter] = {
    val a = Array.ofDim[Reporter](2 + lets.size)
    a(0) = task
    a(1) = makeConstantReporter(lsArgs)
    var i = 2
    lets.foreach { l =>
      a(i) = makeConstantReporter(l._2)
      i += 1
    }
    a
  }

  private def reporterRunner(task: Reporter, lsArgs: LogoList, lets: Seq[(String, AnyRef)]): Procedure = {
    reporterRunnerProc.code(0).args(0).args = makeArgumentArray(task, lsArgs, lets)
    reporterRunnerProc
  }

  private def commandRunner(task: Reporter, lsArgs: LogoList, lets: Seq[(String, AnyRef)]): Procedure = {
    commandRunnerProc.code(0).args = makeArgumentArray(task, lsArgs, lets)
    commandRunnerProc
  }
}

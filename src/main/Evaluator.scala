package org.nlogo.ls

import org.nlogo.workspace.AbstractWorkspaceScala
import org.nlogo.api.{World, SimpleJobOwner, ExtensionException}
import org.nlogo.core.{LogoList, Nobody, AgentKind, CompilerException}
import org.nlogo.nvm.{Job, Reporter, Procedure}
import org.nlogo.prim.{ _constboolean, _constdouble, _constlist, _conststring, _nobody }
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}


class Evaluator(name: String, ws: AbstractWorkspaceScala) {
  type FutureJob[R] = Function1[World, R]

  val owner = new SimpleJobOwner(name, ws.world.mainRNG, AgentKind.Observer);

  private val commandRunner = ws.compileCommands("run task []")

  private val taskCache = CacheBuilder.newBuilder.build(new CacheLoader[String, Reporter] {
    override def load(code: String) = compileTaskReporter(code)
  })

  def command(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): FutureJob[Unit] = {
    val letString = lets.zipWithIndex.map {
      case ((name, v), i) => s"let ${name} ?${1 + args.length + i}"
    }.mkString("\n")

    val fullArgs = args ++ lets.map(_._2)
    val fullCode = s"$letString\n$code"

    val proc = getCommandRunner(getTask(fullCode), fullArgs)
    val job = ws.jobManager.makeConcurrentJob(owner, ws.world.observers, proc)
    ws.jobManager.addJob(job, waitForCompletion = false)
    (w: World) => UnlockAndBlock(w) {
      while (job.state != Job.REMOVED) {
        job.synchronized { job.wait(50) }
      }
      job.result match {
        case e: Exception => throw e
        case _ => // fine
      }
    }
  }

  def report(code: String, lets: Seq[(String, AnyRef)], args: Seq[AnyRef]): FutureJob[AnyRef] = {
    val resultReporter = new ReportFromCommand
    command(s"(run ?${args.length + 1} ($code))", lets, args :+ resultReporter).andThen {
      _ =>
        checkResult(resultReporter.result)
    }
  }

  private def getTask(code: String): Reporter = try {
    taskCache.get(code)
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

  private def compileTaskReporter(code: String): Reporter =
    ws.compileReporter("task [ " + code + " ]").code(0).args(0)

  private def makeConstantReporter(value:Object): Reporter =
    value match {
      case b:java.lang.Boolean => new _constboolean(b)
      case d:java.lang.Double  => new _constdouble(d)
      case l:LogoList          => new _constlist(l)
      case s:String            => new _conststring(s)
      case Nobody              => new _nobody
      case r:ReportFromCommand => TaskReporter(r)
      case _                   => throw new IllegalArgumentException(value.getClass.getName)
    }

  private def makeArgumentArray(task: Reporter, args: Seq[AnyRef]): Array[Reporter] =
    (task +: args.map(makeConstantReporter _)).toArray

  private def getCommandRunner(task: Reporter, args: Seq[AnyRef]): Procedure = {
    commandRunner.code(0).args = makeArgumentArray(task, args)
    commandRunner
  }
}

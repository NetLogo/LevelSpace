package org.nlogo.ls

import org.nlogo.agent.AgentSet
import org.nlogo.api.{ExtensionException, JobOwner}
import org.nlogo.core.LogoList
import org.nlogo.nvm.{ConcurrentJob, Job, Procedure}
import org.nlogo.workspace.AbstractWorkspaceScala

class NotifyingJob(notifyObject: AnyRef,
                   modelID: Int,
                   name: String,
                   workspace: AbstractWorkspaceScala,
                   owner: JobOwner,
                   agentSet: AgentSet,
                   procedure: Procedure)
extends ConcurrentJob(owner, agentSet, procedure, 0, null, workspace, owner.random) {
  override def finish = {
    super.finish
    notifyObject.synchronized {
      notifyObject.notify()
    }
  }

  def waitFor: AnyRef = {
    try {
      while (state != Job.REMOVED && state != Job.DONE) {
        notifyObject.synchronized {
          notifyObject.wait(200)
        }
      }
      ErrorUtils.handle(modelID, name) {
        val ex = workspace.lastLogoException // Used by headless; in GUI, result contains the exception
        if (ex != null) {
          workspace.clearLastLogoException
          throw ex
        }
        checkResult(result)
      }
    } catch {
      case _: InterruptedException => null// just fall through
    }
  }

  private def checkResult(result: AnyRef): AnyRef = result match {
    case (_: org.nlogo.agent.Agent | _: org.nlogo.agent.AgentSet) =>
      throw new ExtensionException("You cannot report agents or agentsets from LevelSpace models.")
    case l: LogoList =>
      l.foreach(checkResult)
      l
    case ex: Exception => throw ex
    case x => x
  }

}

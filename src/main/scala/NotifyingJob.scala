package org.nlogo.ls

import org.nlogo.agent.AgentSet
import org.nlogo.api.{ExtensionException, JobOwner}
import org.nlogo.core.LogoList
import org.nlogo.nvm.{ConcurrentJob, Job, Procedure}
import org.nlogo.workspace.AbstractWorkspaceScala

class NotifyingJob(override val notifyObject: AnyRef,
                   workspace: AbstractWorkspaceScala,
                   owner: JobOwner,
                   agentSet: AgentSet,
                   procedure: Procedure)
extends ConcurrentJob(owner, agentSet, procedure, 0, null, workspace, owner.random)
with Notifying[AnyRef] {

  override def finish() = {
    super[ConcurrentJob].finish()
    super[Notifying].finish()
  }

  override def isFinished = state == Job.REMOVED

  override def waitFor: AnyRef = {
    waitForFinish()
    result
  }
}

trait Notifying[R] {
  def notifyObject: AnyRef

  def finish() = {
    notifyObject.synchronized(notifyObject.notify())
  }

  def isFinished: Boolean

  final def waitForFinish() = {
    while (!isFinished) {
      notifyObject.synchronized(notifyObject.wait(200))
    }
  }

  def waitFor: R

  def map[S](fn: R => S): Notifying[S] = MappedNotifying(this, fn)
}

case class MappedNotifying[A,B](base: Notifying[A], fn: A => B) extends Notifying[B] {
  override def notifyObject: AnyRef = base.notifyObject
  override def finish() = base.finish()
  override def isFinished: Boolean =  base.isFinished
  override def waitFor = fn(base.waitFor)
}

package org.nlogo.ls

import org.nlogo.api.Workspace
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Failure}

object UnlockAndBlock {
  /**
   * Runs `fn` in a Future and waits for it to return while keeping `unlock`
   * unlocked via `unlock.wait`.
   * The primary purpose of this is to allow you to block on code in other
   * threads that may need to lock on this model's world. This includes
   * anything that may end up blocking on the EDT doing something.
   **/
  def apply[R](unlock: AnyRef)(fn: => R): R = {
    val future = Future {
      try {
        fn
      } finally {
        unlock.synchronized {
          unlock.notifyAll
        }
      }
    }
    try {
      while (!future.isCompleted) {
        unlock.synchronized {
          unlock.wait(50)
        }
        if (Thread.currentThread.isInterrupted)
          throw new InterruptedException()
      }
      future.value.map {
        case Success(v) => v
        case Failure(e) => throw e
      }.get
    } catch {
      case ex: InterruptedException =>
        Thread.currentThread.interrupt()
        throw new org.nlogo.nvm.HaltException(false)
    }
  }
}


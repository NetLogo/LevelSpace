package org.nlogo.ls

import org.nlogo.api.ExtensionException
import org.nlogo.nvm.HaltException

object ErrorUtils {
  def wrap(modelID: Int, name: String, e: Exception): ExtensionException = e match {
    case h: HaltException => throw h
    case e: Exception => new ExtensionException(s"Model $modelID ($name) encountered an error: ${e.getMessage}", e)
  }

  def wrap(modelID: Int, name: String, msg: String): ExtensionException =
    new ExtensionException(s"Model $modelID ($name) encountered an error: $msg")

  def handle[R](modelID: Int, name: String)(body: => R): R = try {
    body
  } catch {
    case e: HaltException => throw e
    case e: Exception => throw ErrorUtils.wrap(modelID, name, e)
  }
}

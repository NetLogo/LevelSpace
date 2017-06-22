package org.nlogo.ls

import org.nlogo.api.ExtensionException
import org.nlogo.nvm.HaltException

object ErrorUtils {
  def wrap(modelID: Int, name: String, e: Exception) =
    throw new ExtensionException(s"Model $modelID ($name) encountered an error: ${e.getMessage}", e)

  def handle[R](modelID: Int, name: String)(body: => R): R = try {
    body
  } catch {
    case e: HaltException => throw e
    case e: Exception => ErrorUtils.wrap(modelID, name, e)
  }
}

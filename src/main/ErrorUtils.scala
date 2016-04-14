package org.nlogo.ls

import org.nlogo.api.ExtensionException
import org.nlogo.nvm.HaltException

object ErrorUtils {
  def wrap(model: ChildModel, e: Exception) =
    throw new ExtensionException(s"Model ${model.modelID} (${model.name}) encountered an error: ${e.getMessage}", e)

  def handle[R](model: ChildModel)(fn: =>R) = try {
    fn
  } catch {
    case e: HaltException => throw e
    case e: Exception => ErrorUtils.wrap(model, e)
  }

  // This needs to be usable from Java for now, which doesn't seem to like
  // Scala's currying syntax.
  def handle[A,B](model: ChildModel, fn: A => B): A=>B = (x: A) =>
    try fn(x) catch {
      case e: HaltException => throw e
      case e: Exception => ErrorUtils.wrap(model, e)
    }
}

package actors.execution

import akka.actor.{PoisonPill, Actor}

/**
 * @author rbuckland
 */
trait ScriptExecutor  extends Actor {
  def stop() = self ! PoisonPill
  def executeScript(name: String, filename: String)
}

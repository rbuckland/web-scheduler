package actors.execution

import akka.actor.{Props, Actor}
import akka.actor.Actor.Receive
import org.codehaus.groovy.ast.ClassHelper
import org.apache.commons.logging.LogFactory
import akka.event.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Each message that is sent to this Actor will spin up a new Actor and
 * send the execution task to it
 *
 * @author rbuckland
 */
class ExecutionDispatcher extends Actor {

  val log = LoggerFactory.getLogger(classOf[ExecutionDispatcher])
  override def receive: Receive = {
    case m: GroovyExecute => context.actorOf(Props[GroovyExecutor]) ! m
    case m: ScalaExecute => context.actorOf(Props[ScalaExecutor]) ! m
    case u: Any => log.error(s"Rec'd a message not expected: $u")
  }
}

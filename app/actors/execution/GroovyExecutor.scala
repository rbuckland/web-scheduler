package actors.execution

import akka.actor.Actor
import groovy.lang.{GroovyShell, Binding, GroovyObject, GroovyClassLoader}
import java.io.File
import org.codehaus.groovy.control.CompilerConfiguration
import akka.event.Logging
import org.slf4j.LoggerFactory

case class GroovyExecute(name: String, filename: String)

/**
 * @author rbuckland
 */
class GroovyExecutor extends ScriptExecutor {

  val log = LoggerFactory.getLogger(classOf[GroovyExecutor])

  override def receive: Receive = {
    // this will do for right now
    case GroovyExecute(name, filename) => {
      executeScript(name, filename)
      stop()
    }
  }

  override def executeScript(name: String, filename: String) {

    log.debug(s"Executing [$name] --> $filename")
    def configuration = new CompilerConfiguration()
    val parent = getClass.getClassLoader
    val loader = new GroovyClassLoader(parent)
    def shell = new GroovyShell(parent, new Binding(), configuration)

    try {
      shell.evaluate(new File(filename))
    } catch {
      case e: Throwable => {
        log.error("Failed to run: " + filename, e)
      }
    }

  }

}

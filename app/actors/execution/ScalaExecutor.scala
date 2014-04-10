package actors.execution

import akka.event.Logging
import org.codehaus.groovy.control.CompilerConfiguration
import groovy.lang.{Binding, GroovyShell, GroovyClassLoader}
import com.twitter.util.Eval
import java.io.File
import org.slf4j.LoggerFactory

case class ScalaExecute(name: String, filename: String)

/**
 * @author rbuckland
 */
class ScalaExecutor extends ScriptExecutor {

  val log = LoggerFactory.getLogger(classOf[ScalaExecutor])

  override def receive: Receive = {
    // this will do for right now
    case ScalaExecute(name, filename) => {
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

      new Eval()(new File(filename))

    } catch {

      case e: Throwable => {
        log.error("Failed to run: " + filename, e)
      }
    }

  }

}
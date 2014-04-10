package controllers

import play.api._
import play.api.mvc._
import actors.execution.ExecutionDispatcher
import play.libs.{Json, Akka}
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import actors.quartz._
import actors.execution.GroovyExecute
import actors.execution.ScalaExecute
import actors.quartz.AddCronSchedule
import actors.quartz.AddCronScheduleSuccess
import actors.quartz.AddCronScheduleFailure

/**
 * @author rbuckland
 */
object ScheduleManager extends Controller {

  val log = LoggerFactory.getLogger(this.getClass)
  implicit val timeout = Timeout(3 seconds)
  import ExecutionContext.Implicits.global
  val dispatcherRef = Akka.system().actorOf(Props[ExecutionDispatcher], name="executionDispatcher")
  val quartzActorRef = Akka.system().actorOf(Props[QuartzActor], name="quartzActor")

  // @BodyParser.Of(classOf[play.mvc.BodyParser.Json])
  def addSchedule(name: String, schedule: String, language: String, filename: String) = Action {


    val executeMessage = language.toLowerCase match {
      case "groovy" => GroovyExecute(name, filename)
      case "scala" => ScalaExecute(name, filename)
    }

    log.debug(s"$executeMessage - adding new schedule: $schedule $filename")
    val response:Future[Any] = quartzActorRef ? AddCronSchedule(name+"/"+ language, dispatcherRef, schedule, executeMessage, true)

    Async { response.map {
        case AddCronScheduleFailure(ex) => Ok("failed to add: " + ex.getLocalizedMessage)
        case x: AddCronScheduleSuccess => Ok(s"Added Cron $name - $filename - $schedule as ${x}")
      }
    }
  }

  /**
   * get a list of the jobs that the Akka Scheduler knows about
   * @return
   */
  def listJobs() = Action {
    Async {
      val result = quartzActorRef.ask(QueryScheduledJobs()).mapTo[Iterable[ScheduledJobDetails]]
      result.map {
        case x: Iterable[ScheduledJobDetails] => Ok(x.toString)
      }
    }
  }
}

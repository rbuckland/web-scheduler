package actors.quartz

/*
Copyright 2012 Yann Ramin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import akka.actor.{Cancellable, ActorRef, Actor}
import akka.event.Logging
import org.quartz.impl.StdSchedulerFactory
import java.util.{Date, Properties}
import org.quartz._
import utils.Key
import org.quartz.impl.matchers.GroupMatcher
import scala.collection.JavaConversions._

/**
 * Message to add a cron scheduler. Send this to the QuartzActor
 * @param name we will use this for the key
 * @param to The ActorRef describing the desination actor
 * @param cron A string Cron expression
 * @param message Any message
 * @param reply Whether to give a reply to this message indicating success or failure (optional)
 */
case class AddCronSchedule(name: String, to: ActorRef, cron: String, message: Any, reply: Boolean = false, spigot: Spigot = OpenSpigot)

trait AddCronScheduleResult

/**
 * Indicates success for a scheduler add action.
 * @param cancel The cancellable allows the job to be removed later. Can be invoked directly -
 *               canceling will send an internal RemoveJob message
 */
case class AddCronScheduleSuccess(cancel: Cancellable) extends AddCronScheduleResult

/**
 * Indicates the job couldn't be added. Usually due to a bad cron expression.
 * @param reason The reason
 */
case class AddCronScheduleFailure(reason: Throwable) extends AddCronScheduleResult

/**
 * Remove a job based upon the Cancellable returned from a success call.
 * @param cancel
 */
case class RemoveJob(cancel: Cancellable)

/**
 * As it's name suggests, it asks the internal Scheduler for a list of jobs
 * it knows about
 */
case class QueryScheduledJobs()
case class ScheduledJobDetails(groupName: String, jobKey: String, nextFireTime: Option[Date], to: ActorRef, message: Any)


/**
 * Internal class to make Quartz work.
 * This should be in QuartzActor, but for some reason Quartz
 * ends up with a construction error when it is.
 */
private class QuartzActorExecutor() extends Job {

  def execute(ctx: JobExecutionContext) {

    val jdm = ctx.getJobDetail.getJobDataMap // Really?
    val spigot = jdm.get(QuartzActor.Spigot).asInstanceOf[Spigot]
    if (spigot.open) {
      val msg = jdm.get(QuartzActor.Message)
      val actor = jdm.get(QuartzActor.ToActor).asInstanceOf[ActorRef]
      actor ! msg
    }

  }

}

trait Spigot {
  def open: Boolean
}

object OpenSpigot extends Spigot {
  val open = true
}

/**
 * The base quartz scheduling actor. Handles a single quartz scheduler
 * and processes Add and Remove messages.
 */
class QuartzActor extends Actor {

  val log = Logging(context.system, this)

  // Create a sane default quartz scheduler
  private[this] val props = new Properties()
  props.setProperty("org.quartz.scheduler.instanceName", context.self.path.name)
  props.setProperty("org.quartz.threadPool.threadCount", "1")
  props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
  props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true")
  // Whoever thought this was smart shall be shot

  val scheduler = new StdSchedulerFactory(props).getScheduler

  /**
   * Cancellable to later kill the job. Yes this is mutable, I'm sorry.
   * @param job
   */
  class CancelSchedule(val job: JobKey, val trig: TriggerKey) extends Cancellable {
    var cancelled = false

    def isCancelled: Boolean = cancelled

    def cancel() = {
      context.self ! RemoveJob(this)
      cancelled = true
      true
    }

  }

  override def preStart() {
    scheduler.start()
    log.info("Scheduler started")
  }

  override def postStop() {
    scheduler.shutdown()
  }

  // Largely imperative glue code to make quartz work :)
  def receive = {

    case QueryScheduledJobs() => context.sender ! jobDetails()

    case RemoveJob(cancel) => cancel match {
      case cs: CancelSchedule => scheduler.deleteJob(cs.job); cs.cancelled = true
      case _ => log.error("Incorrect cancelable sent")
    }
    case AddCronSchedule(name, to, cron, message, reply, spigot) =>
      // use UUID as the jobkey
      val jobkey = new JobKey(s"${name}_" + java.util.UUID.randomUUID().toString)
      // reuse the name
      val trigkey = new TriggerKey(jobkey.getName + "_trigger")
      // We use JobDataMaps to pass data to the newly created job runner class
      val jd = org.quartz.JobBuilder.newJob(classOf[QuartzActorExecutor])
      val jdm = new JobDataMap()
      jdm.put(QuartzActor.Spigot, spigot)
      jdm.put(QuartzActor.Message, message)
      jdm.put(QuartzActor.ToActor, to)
      val job = jd.usingJobData(jdm).withIdentity(jobkey).build()

      try {
        scheduler.scheduleJob(job, org.quartz.TriggerBuilder.newTrigger().startNow()
          .withIdentity(trigkey).forJob(job)
          .withSchedule(org.quartz.CronScheduleBuilder.cronSchedule(cron)).build())

        if (reply)
          context.sender ! AddCronScheduleSuccess(new CancelSchedule(jobkey, trigkey))

      } catch {
        // Quartz will drop a throwable if you give it an invalid cron expression - pass that info on
        case e: Throwable =>
          log.error("Quartz failed to add a task: ", e)
          if (reply)
            context.sender ! AddCronScheduleFailure(e)

      }
    // I'm relatively unhappy with the two message replies, but it works

    case _ => //
  }

  /**
   * Extract out from the Quartz Scheduler what jobs it has on
   * @return
   */
  private def jobDetails() = (
        for (
          groupName <- scheduler.getJobGroupNames;
          ourJobKey <- scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
          dateOfTrigger = scheduler.getTriggersOfJob(ourJobKey).to[List].headOption.map(_.getNextFireTime);
          toActor = QuartzActor.toActor(scheduler.getJobDetail(ourJobKey));
          message = scheduler.getJobDetail(ourJobKey).getJobDataMap.get(QuartzActor.Message)
        ) yield ScheduledJobDetails(groupName, ourJobKey.getName,dateOfTrigger, toActor, message)
    ).to[Iterable]

}

object QuartzActor {
  val Spigot = "spigot"
  val Message = "message"
  val ToActor = "actor"

  // get the to actorRef out of the jobdetail
  def toActor(jobDetail: JobDetail):ActorRef = jobDetail.getJobDataMap.get(QuartzActor.ToActor).asInstanceOf[ActorRef]
}
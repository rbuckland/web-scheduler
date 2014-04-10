Scala and Groovy Script - Quartz Web Scheduler
=====================================

? Why .. Good Exercise

### Another Cron - Kind Of

 - 1. I could run a groovy script at X time
 - 2. I could run a Scala Script at X time
 - 3. I could have lots of these scheduled tasks (many . any schedules)
 - 4. I could have a web interface to see the current scheduled tasks
 - 5. I could stop and start various ones
 - 6. I could add and remove jobs
 - 7. It dynamically reads the script files

 I have implemented 1, 2, 3 & 7

### Adding a Cron
```
// execute scripts/test.scala ever 2nd minute at the 1st second
01 0/2 * * * ?

http://localhost:9000/api/cron/add/Test/01%200%2F2%20*%20*%20*%20%3F/scala/scripts%2Ftest.scala

// execute scripts/test.groovy ever 2nd minute at the 15th second
15 0/2 * * * ?

http://localhost:9000/api/cron/add/Test/15%200%2F2%20*%20*%20*%20%3F/groovy/scripts%2Ftest.groovy
```

### Show the Current Crons
```
http://localhost:9000/api/cron/jobs

Vector(
ScheduledJobDetails(DEFAULT,Test/groovy_e93621bb-3422-4766-966a-152da05c44e3,Some(Fri Apr 11 00:34:15 BST 2014),Actor[akka://application/user/executionDispatcher#-152758938],GroovyExecute(Test,scripts/test.groovy)), 
ScheduledJobDetails(DEFAULT,Test/scala_1a505bc0-7901-42c0-b083-4b1a4962336b,Some(Fri Apr 11 00:34:01 BST 2014),Actor[akka://application/user/executionDispatcher#-152758938],ScalaExecute(Test,scripts/test.scala))
)

```

### What Can I do With this ?
Well .. because it just executes your groory of Scala - Practically anything.

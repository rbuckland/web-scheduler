Scala and Groovy Script - Quartz Web Scheduler
=====================================

? Why .. Good Exercise

### I wanted a Cron Web Application
 where
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
http://localhost:9000/api/cron/add/Test/01%200%2F2%20*%20*%20*%20%3F/scala/scripts%2Ftest.scala
http://localhost:9000/api/cron/add/Test/15%200%2F2%20*%20*%20*%20%3F/groovy/scripts%2Ftest.groovy
```

### Show the Current Crons
http://localhost:9000/api/cron/jobs
# JobSchedulerExample

The job service will work on below cycle
onCreate -> onStartJob

You need only move all tasks doing at onCreate of background service to onStartJob of jobIntentService
It will be working well without Background Limitation.

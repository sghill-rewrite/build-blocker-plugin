build-blocker-plugin
====================

Jenkins build blocker plugin

This plugin uses a QueueTaskDispatcher to block scheduled jobs from starting as long as configured other jobs are running.

These other jobs can be configured in a textarea where each line represents a regular expression of the job names that should block this job from starting.

The blocking behaviour can be configured to either block builds

* from running on the same node
* from running at all

Additionally, the blocking behaviour can be configured to consider planned, but not yet running builds in the decision to block a build.
Either

* buildable builds can stop another build from running (for instance builds that are waiting for an available executor)
* all planned builds can stop another build from running (blocked builds, pending builds waiting builds and buildable builds)
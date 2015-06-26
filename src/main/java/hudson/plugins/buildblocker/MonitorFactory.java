package hudson.plugins.buildblocker;

public interface MonitorFactory {
    BlockingJobsMonitor build(String blockingJobs);
}

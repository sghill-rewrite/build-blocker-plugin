package hudson.plugins.buildblocker;

public class DefaultMonitorFactory implements MonitorFactory {
    @Override
    public BlockingJobsMonitor build(String blockingJobs) {
        return new BlockingJobsMonitor(blockingJobs);
    }
}

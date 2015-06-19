package hudson.plugins.buildblocker;

public class BuildBlockerPropertyBuilder {
    private boolean useBuildBlocker = false;
    private boolean blockOnNodeLevel = false;
    private boolean blockOnGlobalLevel = false;
    private boolean scanAllQueueItemStates = false;
    private String blockingJobs = null;

    public BuildBlockerPropertyBuilder setUseBuildBlocker(boolean useBuildBlocker) {
        this.useBuildBlocker = useBuildBlocker;
        return this;
    }

    public BuildBlockerPropertyBuilder setBlockOnNodeLevel(boolean blockOnNodeLevel) {
        this.blockOnNodeLevel = blockOnNodeLevel;
        return this;
    }

    public BuildBlockerPropertyBuilder setBlockOnGlobalLevel(boolean blockOnGlobalLevel) {
        this.blockOnGlobalLevel = blockOnGlobalLevel;
        return this;
    }

    public BuildBlockerPropertyBuilder setScanAllQueueItemStates(boolean scanAllQueueItemStates) {
        this.scanAllQueueItemStates = scanAllQueueItemStates;
        return this;
    }

    public BuildBlockerPropertyBuilder setBlockingJobs(String blockingJobs) {
        this.blockingJobs = blockingJobs;
        return this;
    }

    public BuildBlockerProperty createBuildBlockerProperty() {
        return new BuildBlockerProperty(useBuildBlocker, blockOnNodeLevel, blockOnGlobalLevel, scanAllQueueItemStates, blockingJobs);
    }
}
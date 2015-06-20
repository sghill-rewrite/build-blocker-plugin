package hudson.plugins.buildblocker;

import hudson.plugins.buildblocker.BuildBlockerProperty.BlockLevel;
import hudson.plugins.buildblocker.BuildBlockerProperty.QueueScanScope;

public class BuildBlockerPropertyBuilder {
    private boolean useBuildBlocker = false;
    private String blockLevel = "";
    private String scanQueueFor = "";
    private String blockingJobs = "";

    public BuildBlockerPropertyBuilder setUseBuildBlocker(boolean useBuildBlocker) {
        this.useBuildBlocker = useBuildBlocker;
        return this;
    }

    public BuildBlockerPropertyBuilder setBlockOnNodeLevel(boolean blockOnNodeLevel) {
        if (!blockLevel.equals("global")) {
            this.blockLevel = "node";
        }
        return this;
    }

    public BuildBlockerPropertyBuilder setBlockOnGlobalLevel(boolean blockOnGlobalLevel) {
        this.blockLevel = "global";
        return this;
    }

    public BuildBlockerPropertyBuilder setScanAllQueueItemStates(boolean scanAllQueueItemStates) {
        this.scanQueueFor = "all";
        return this;
    }

    public BuildBlockerPropertyBuilder setScanBuildableQueueItemStates(boolean scanBuildableQueueItemStates) {
        if (!scanQueueFor.equals("all")) {
            this.scanQueueFor = "buildable";
        }
        return this;
    }

    public BuildBlockerPropertyBuilder setBlockingJobs(String blockingJobs) {
        this.blockingJobs = blockingJobs;
        return this;
    }

    public BuildBlockerProperty createBuildBlockerProperty() {
        return new BuildBlockerProperty(useBuildBlocker, new BlockLevel(blockLevel), new QueueScanScope(scanQueueFor), blockingJobs);
    }
}
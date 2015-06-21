package hudson.plugins.buildblocker;

public class BuildBlockerPropertyBuilder {
    private boolean useBuildBlocker = false;
    private String blockLevel = "";
    private String scanQueueFor = "";
    private String blockingJobs = "";

    public BuildBlockerPropertyBuilder setUseBuildBlocker() {
        this.useBuildBlocker = true;
        return this;
    }

    public BuildBlockerPropertyBuilder setBlockOnNodeLevel() {
        if (!blockLevel.equals("global")) {
            this.blockLevel = "node";
        }
        return this;
    }

    public BuildBlockerPropertyBuilder setBlockOnGlobalLevel() {
        this.blockLevel = "global";
        return this;
    }

    public BuildBlockerPropertyBuilder setScanAllQueueItemStates() {
        this.scanQueueFor = "all";
        return this;
    }

    public BuildBlockerPropertyBuilder setScanBuildableQueueItemStates() {
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
        return new BuildBlockerProperty(useBuildBlocker, blockLevel, scanQueueFor, blockingJobs);
    }
}
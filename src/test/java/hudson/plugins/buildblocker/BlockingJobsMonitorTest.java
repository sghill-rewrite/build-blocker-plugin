/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Frederik Fromm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.buildblocker;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.slaves.SlaveComputer;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests
 */
public class BlockingJobsMonitorTest extends HudsonTestCase {

    private String blockingJobName;
    private Future<FreeStyleBuild> future;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        blockingJobName = "blockingJob";


        // clear queue from preceding tests
        Jenkins.getInstance().getQueue().clear();

        // init slave
        LabelAtom label = new LabelAtom("label");
        DumbSlave slave = this.createSlave(label);
        SlaveComputer c = slave.getComputer();
        c.connect(false).get(); // wait until it's connected
        if (c.isOffline()) {
            fail("Slave failed to go online: " + c.getLog());
        }


        FreeStyleProject blockingProject = this.createFreeStyleProject(blockingJobName);
        blockingProject.setAssignedLabel(label);

        Shell shell = new Shell("sleep 1");
        blockingProject.getBuildersList().add(shell);

        future = blockingProject.scheduleBuild2(0);

        // wait until blocking job started
        while (!slave.getComputer().getExecutors().get(0).isBusy()) {
            TimeUnit.SECONDS.sleep(1);
        }

    }

    @Override
    protected void tearDown() throws Exception {
        // wait until blocking job stopped
        while (!future.isDone()) {
            TimeUnit.SECONDS.sleep(1);
        }
        BlockingJobsMonitor blockingJobsMonitorUsingFullName = new BlockingJobsMonitor(blockingJobName);

        assertNull(blockingJobsMonitorUsingFullName.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingFullName.checkForBuildableQueueEntries(null));


        super.tearDown();
    }

    public void testNullMonitorDoesNotBlock() throws Exception {


        BlockingJobsMonitor blockingJobsMonitorUsingNull = new BlockingJobsMonitor(null);
        assertNull(blockingJobsMonitorUsingNull.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingNull.checkForBuildableQueueEntries(null));

    }

    public void testNonMatchingMonitorDoesNotBlock() {
        BlockingJobsMonitor blockingJobsMonitorNotMatching = new BlockingJobsMonitor("xxx");
        assertNull(blockingJobsMonitorNotMatching.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorNotMatching.checkForBuildableQueueEntries(null));

    }

    public void testMatchingMonitorReturnsBlockingJobsDisplayName() {
        BlockingJobsMonitor blockingJobsMonitorUsingFullName = new BlockingJobsMonitor(blockingJobName);
        assertEquals(blockingJobName, blockingJobsMonitorUsingFullName.checkAllNodesForRunningBuilds().getDisplayName
                ());
        assertNull(blockingJobsMonitorUsingFullName.checkForBuildableQueueEntries(null));

    }

    public void testMonitorBlocksBasedOnRegEx() {
        BlockingJobsMonitor blockingJobsMonitorUsingRegex = new BlockingJobsMonitor("block.*");
        assertEquals(blockingJobName, blockingJobsMonitorUsingRegex.checkAllNodesForRunningBuilds().getDisplayName());
        assertNull(blockingJobsMonitorUsingRegex.checkForBuildableQueueEntries(null));
    }

    public void testMonitorBlocksIfConfiguredWithSeveralProjectnames() {
        BlockingJobsMonitor blockingJobsMonitorUsingMoreLines = new BlockingJobsMonitor("xxx\nblock.*\nyyy");
        assertEquals(blockingJobName, blockingJobsMonitorUsingMoreLines.checkAllNodesForRunningBuilds()
                .getDisplayName());
        assertNull(blockingJobsMonitorUsingMoreLines.checkForBuildableQueueEntries(null));

    }

    public void testMonitorDoesNotBlockIfRegexDoesNotMatch() {
        BlockingJobsMonitor blockingJobsMonitorUsingWrongRegex = new BlockingJobsMonitor("*BW2S.*QRT.");
        assertNull(blockingJobsMonitorUsingWrongRegex.checkAllNodesForRunningBuilds());
        assertNull(blockingJobsMonitorUsingWrongRegex.checkForBuildableQueueEntries(null));
    }
}

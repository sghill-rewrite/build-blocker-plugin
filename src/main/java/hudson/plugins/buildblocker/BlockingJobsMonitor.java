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

import hudson.matrix.MatrixConfiguration;
import hudson.model.*;
import hudson.model.queue.SubTask;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * This class represents a monitor that checks all running jobs if
 * one of their names matches with one of the given blocking job's
 * regular expressions.
 * <p/>
 * The first hit returns the blocking job's name.
 */
public class BlockingJobsMonitor {

    /**
     * the list of regular expressions from the job configuration
     */
    private List<String> blockingJobs = emptyList();

    private static final Logger LOG = Logger.getLogger(BlockingJobsMonitor.class.getName());


    /**
     * Constructor using the job configuration entry for blocking jobs
     *
     * @param blockingJobs line feed separated list og blocking jobs
     */
    public BlockingJobsMonitor(String blockingJobs) {
        if (StringUtils.isNotBlank(blockingJobs)) {
            this.blockingJobs = asList(blockingJobs.split("\n"));
            LOG.fine("blocking jobs: " + blockingJobs);
        }
    }

    public SubTask checkForBuildableQueueEntries(Queue.Item item) {
        List<Queue.BuildableItem> buildableItems = Jenkins.getInstance().getQueue().getBuildableItems();

        SubTask buildableItem = checkForPlannedBuilds(item, buildableItems);
        if (buildableItem != null) {
            LOG.fine("build " + item + " blocked by queued build " + buildableItem);
            return buildableItem;
        }
        return null;
    }

    public SubTask checkForQueueEntries(Queue.Item item) {
        List<Queue.Item> buildableItems = asList(Jenkins.getInstance().getQueue().getItems());

        SubTask buildableItem = checkForPlannedBuilds(item, buildableItems);
        if (buildableItem != null) {
            LOG.fine("build " + item + " blocked by queued build " + buildableItem);
            return buildableItem;
        }
        return null;
    }

    public SubTask checkNodeForBuildableQueueEntries(Queue.Item item, Node node) {
        List<? extends Queue.Item> buildableItems = Jenkins.getInstance().getQueue().getBuildableItems(node
                .toComputer());

        SubTask buildableItem = checkForPlannedBuilds(item, buildableItems);
        if (buildableItem != null) {
            LOG.fine("build " + item + " blocked by queued build " + buildableItem);
            return buildableItem;
        }
        return null;
    }

    public SubTask checkNodeForQueueEntries(Queue.Item item, Node node) {
        List<Queue.Item> buildableItemsOnNode = new ArrayList<Queue.Item>();
        for (Queue.Item buildableItem : Jenkins.getInstance().getQueue().getItems()) {
            if (buildableItem.getAssignedLabel().contains(node)) {
                buildableItemsOnNode.add(buildableItem);
            }
        }

        SubTask buildableItem = checkForPlannedBuilds(item, buildableItemsOnNode);
        if (buildableItem != null) {
            LOG.fine("build " + item + " blocked by queued build " + buildableItem);
            return buildableItem;
        }
        return null;
    }

    public SubTask checkAllNodesForRunningBuilds() {
        Computer[] computers = Jenkins.getInstance().getComputers();

        for (Computer computer : computers) {
            SubTask task = checkComputerForRunningBuilds(computer);
            if (task != null) {
                return task;
            }
        }
        return null;
    }

    private SubTask checkComputerForRunningBuilds(Computer computer) {
        List<Executor> executors = computer.getExecutors();

        executors.addAll(computer.getOneOffExecutors());

        for (Executor executor : executors) {
            SubTask task = checkForRunningBuilds(executor);
            if (task != null) {
                LOG.fine("build blocked by running build " + task);
                return task;
            }
        }
        return null;
    }

    public SubTask checkNodeForRunningBuilds(Node node) {
        if (node == null) {
            return null;
        }
        return checkComputerForRunningBuilds(node.toComputer());
    }

    private SubTask checkForPlannedBuilds(Queue.Item item, List<? extends Queue.Item> buildableItems) {
        for (Queue.Item buildableItem : buildableItems) {
            if (item != buildableItem) {
                for (String blockingJob : this.blockingJobs) {
                    AbstractProject project = (AbstractProject) buildableItem.task;
                    if (project.getFullName().matches(blockingJob)) {
                        return project;
                    }
                }
            }
        }
        return null;
    }

    private SubTask checkForRunningBuilds(Executor executor) {
        if (executor.isBusy()) {
            Queue.Executable currentExecutable = executor.getCurrentExecutable();

            SubTask subTask = currentExecutable.getParent();
            Queue.Task task = subTask.getOwnerTask();

            if (task instanceof MatrixConfiguration) {
                task = ((MatrixConfiguration) task).getParent();
            }

            AbstractProject project = (AbstractProject) task;

            for (String blockingJob : this.blockingJobs) {
                try {
                    if (project.getFullName().matches(blockingJob)) {
                        return subTask;
                    }
                } catch (java.util.regex.PatternSyntaxException pse) {
                    return null;
                }
            }
        }
        return null;
    }

}

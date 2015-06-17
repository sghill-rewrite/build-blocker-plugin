package hudson.plugins.buildblocker;

import hudson.model.*;
import hudson.model.Queue.BuildableItem;
import hudson.model.queue.SubTask;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.support.membermodification.MemberMatcher.field;

@PrepareForTest({Jenkins.class, BuildableItem.class, Project.class})
@RunWith(PowerMockRunner.class)
public class BlockingJobsMonitorUnitTests {

    @Mock
    private Node node;
    @Mock
    private Computer computer;
    @Mock
    private Queue queue;
    @Mock
    private Executor idleExecutor;
    @Mock
    private Executor executor;
    @Mock
    private OneOffExecutor idleOneOffExecutor;

    private Project project;
    private Project nonBlockingProject;
    private BuildableItem item;
    private BuildableItem nonBlockingItem;
    private Jenkins jenkins;

    private BlockingJobsMonitor monitor;

    @Before
    public void setup() throws IllegalAccessException {
        monitor = new BlockingJobsMonitor("blockingProject");

        project = mock(Project.class);
        nonBlockingProject = mock(Project.class);
        when(project.getFullName()).thenReturn("blockingProject");
        when(nonBlockingProject.getFullName()).thenReturn("harmlessProject");

        item = mock(BuildableItem.class);
        nonBlockingItem = mock(BuildableItem.class);
        field(BuildableItem.class, "task").set(item, project);
        field(BuildableItem.class, "task").set(nonBlockingItem, nonBlockingProject);

        mockStatic(Jenkins.class);
        jenkins = mock(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(jenkins.getQueue()).thenReturn(queue);

        when(node.toComputer()).thenReturn(computer);

        when(idleExecutor.isBusy()).thenReturn(false);
        when(executor.isBusy()).thenReturn(true);

        when(idleOneOffExecutor.isBusy()).thenReturn(false);
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesNeedsNode() {
        assertThat(monitor.checkNodeForBuildableQueueEntries(item, null), is(nullValue()));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesNeedsItem() {
        assertThat(monitor.checkNodeForBuildableQueueEntries(null, node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getBuildableItems(any(Computer.class))).thenReturn(Collections.<BuildableItem>emptyList());

        assertThat(monitor.checkNodeForBuildableQueueEntries(item, node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsTaskThatIsQueued() {
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingItem, item));

        assertThat((Project) monitor.checkNodeForBuildableQueueEntries(mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsNullForDifferentNode() {
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingItem, item));

        assertThat(monitor.checkNodeForBuildableQueueEntries(mock(BuildableItem.class), mock(Node.class)), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildNeedsNode() {
        assertThat(monitor.checkNodeForRunningBuilds(null), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForNonBusyExecutor() {
        when(computer.getExecutors()).thenReturn(singletonList(idleExecutor));
        when(computer.getOneOffExecutors()).thenReturn(Collections.<OneOffExecutor>emptyList());
        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForNonBusyOneOffExecutor() {
        when(computer.getExecutors()).thenReturn(Collections.<Executor>emptyList());
        when(computer.getOneOffExecutors()).thenReturn(singletonList(idleOneOffExecutor));
        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForDifferentRunningProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        Queue.Executable executable = Mockito.mock(Queue.Executable.class);
        when(executor.getCurrentExecutable()).thenReturn(executable);
        SubTask subTask = Mockito.mock(SubTask.class);
        when(executable.getParent()).thenReturn(subTask);
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsBlockedProjectIfItIsRunning() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        Queue.Executable executable = Mockito.mock(Queue.Executable.class);
        when(executor.getCurrentExecutable()).thenReturn(executable);
        SubTask subTask = Mockito.mock(SubTask.class);
        when(executable.getParent()).thenReturn(subTask);
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkNodeForRunningBuilds(node), is(equalTo(project)));
    }

    //TODO cases for one off executor, matrix project

    //TODO flag check all states implemented

}

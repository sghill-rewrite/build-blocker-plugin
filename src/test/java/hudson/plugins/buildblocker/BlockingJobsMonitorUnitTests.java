package hudson.plugins.buildblocker;

import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.*;
import hudson.model.Queue.BuildableItem;
import hudson.model.queue.SubTask;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
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
    @Mock
    private OneOffExecutor oneOffExecutor;

    private Project project;
    private Project nonBlockingProject;
    private MatrixProject nonBlockingMatrixProject;
    private MatrixProject matrixProject;
    private BuildableItem item;
    private BuildableItem nonBlockingItem;

    private BlockingJobsMonitor monitor;

    @Before
    public void setup() throws IllegalAccessException {
        monitor = new BlockingJobsMonitor("blockingProject\nblockingMatrixProject");

        project = PowerMockito.mock(Project.class);
        nonBlockingProject = PowerMockito.mock(Project.class);
        matrixProject = PowerMockito.mock(MatrixProject.class);
        nonBlockingMatrixProject = PowerMockito.mock(MatrixProject.class);
        when(project.getFullName()).thenReturn("blockingProject");
        when(nonBlockingProject.getFullName()).thenReturn("harmlessProject");
        when(matrixProject.getFullName()).thenReturn("blockingMatrixProject");
        when(nonBlockingMatrixProject.getFullName()).thenReturn("harmlessMatrixProject");


        item = PowerMockito.mock(BuildableItem.class);
        nonBlockingItem = PowerMockito.mock(BuildableItem.class);
        field(BuildableItem.class, "task").set(item, project);
        field(BuildableItem.class, "task").set(nonBlockingItem, nonBlockingProject);

        PowerMockito.mockStatic(Jenkins.class);
        Jenkins jenkins = PowerMockito.mock(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(jenkins.getQueue()).thenReturn(queue);

        when(node.toComputer()).thenReturn(computer);

        when(idleExecutor.isBusy()).thenReturn(false);
        when(executor.isBusy()).thenReturn(true);

        when(idleOneOffExecutor.isBusy()).thenReturn(false);
        when(oneOffExecutor.isBusy()).thenReturn(true);
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

        assertThat((Project) monitor.checkNodeForBuildableQueueEntries(Mockito.mock(BuildableItem.class), node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsNullForDifferentNode() {
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingItem, item));

        assertThat(monitor.checkNodeForBuildableQueueEntries(Mockito.mock(BuildableItem.class), Mockito.mock(Node.class)), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildNeedsNode() {
        assertThat(monitor.checkNodeForRunningBuilds(null), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForNonBusyExecutor() {
        when(computer.getExecutors()).thenReturn(singletonList(idleExecutor));
        when(computer.getOneOffExecutors()).thenReturn(new ArrayList<OneOffExecutor>());
        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForNonBusyOneOffExecutor() {
        when(computer.getExecutors()).thenReturn(new ArrayList<Executor>());
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

        assertThat(monitor.checkNodeForRunningBuilds(node), is(equalTo(subTask)));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForDifferentRunningProjectOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        Queue.Executable executable = Mockito.mock(Queue.Executable.class);
        when(oneOffExecutor.getCurrentExecutable()).thenReturn(executable);
        SubTask subTask = Mockito.mock(SubTask.class);
        when(executable.getParent()).thenReturn(subTask);
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsBlockedProjectIfItIsRunningOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        Queue.Executable executable = Mockito.mock(Queue.Executable.class);
        when(oneOffExecutor.getCurrentExecutable()).thenReturn(executable);
        SubTask subTask = Mockito.mock(SubTask.class);
        when(executable.getParent()).thenReturn(subTask);
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(equalTo(subTask)));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForDifferentRunningMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        Queue.Executable executable = Mockito.mock(Queue.Executable.class);
        when(executor.getCurrentExecutable()).thenReturn(executable);
        SubTask subTask = Mockito.mock(SubTask.class);
        when(executable.getParent()).thenReturn(subTask);
        MatrixConfiguration configuration = Mockito.mock(MatrixConfiguration.class);
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(nonBlockingMatrixProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsBlockedMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        Queue.Executable executable = Mockito.mock(Queue.Executable.class);
        when(executor.getCurrentExecutable()).thenReturn(executable);
        SubTask subTask = Mockito.mock(SubTask.class);
        when(executable.getParent()).thenReturn(subTask);
        MatrixConfiguration configuration = Mockito.mock(MatrixConfiguration.class);
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(matrixProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(equalTo(subTask)));
    }

    //TODO flag check all states implemented

}

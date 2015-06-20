package hudson.plugins.buildblocker;

import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.OneOffExecutor;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.queue.SubTask;
import hudson.model.queue.WorkUnit;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.field;

@PrepareForTest({Jenkins.class, BuildableItem.class, Queue.BlockedItem.class, Queue.WaitingItem.class, Project.class, WorkUnit.class})
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
    @Mock
    private Label blockingLabel;
    @Mock
    private Label nonBlockingLabel;
    @Mock
    private SubTask subTask;
    @Mock
    private MatrixConfiguration configuration;

    private Project project;
    private Project nonBlockingProject;
    private MatrixProject nonBlockingMatrixProject;
    private MatrixProject matrixProject;
    private BuildableItem buildableItem;
    private BuildableItem buildableItemOnDifferentNode;
    private BuildableItem nonBlockingBuildableItem;
    private Queue.BlockedItem blockedItem;
    private Queue.BlockedItem blockedItemOnDifferentNode;
    private Queue.BlockedItem nonBlockingBlockedItem;
    private Queue.WaitingItem waitingItem;
    private Queue.WaitingItem waitingItemOnDifferentNode;
    private Queue.WaitingItem nonBlockingWaitingItem;
    private WorkUnit workUnit;

    private BlockingJobsMonitor monitor;

    @Before
    public void setup() throws IllegalAccessException, ClassNotFoundException, InvocationTargetException,
            InstantiationException {
        monitor = new BlockingJobsMonitor("blockingProject\nblockingMatrixProject");

        trainProjects();
        trainBuildableItems();
        trainBlockedItems();
        trainWaitingItems();
        trainLabels();
        trainJenkins();
        trainNodes();
        trainWorkUnit();
        trainExecutors();
    }

    private void trainWorkUnit() throws IllegalAccessException {
        workUnit = PowerMockito.mock(WorkUnit.class);
        field(WorkUnit.class, "work").set(workUnit, subTask);
    }

    private void trainLabels() {
        when(blockingLabel.contains(eq(node))).thenReturn(true);
        when(nonBlockingLabel.contains(eq(node))).thenReturn(false);
    }

    private void trainWaitingItems() throws IllegalAccessException {
        waitingItem = PowerMockito.mock(Queue.WaitingItem.class);
        nonBlockingWaitingItem = PowerMockito.mock(Queue.WaitingItem.class);
        waitingItemOnDifferentNode = PowerMockito.mock(Queue.WaitingItem.class);
        field(Queue.BlockedItem.class, "task").set(waitingItem, project);
        field(Queue.BlockedItem.class, "task").set(waitingItemOnDifferentNode, project);
        field(Queue.BlockedItem.class, "task").set(nonBlockingWaitingItem, nonBlockingProject);
        when(waitingItem.getAssignedLabel()).thenReturn(blockingLabel);
        when(waitingItemOnDifferentNode.getAssignedLabel()).thenReturn(nonBlockingLabel);
        when(nonBlockingWaitingItem.getAssignedLabel()).thenReturn(blockingLabel);
    }

    private void trainBlockedItems() throws IllegalAccessException {
        blockedItem = PowerMockito.mock(Queue.BlockedItem.class);
        nonBlockingBlockedItem = PowerMockito.mock(Queue.BlockedItem.class);
        blockedItemOnDifferentNode = PowerMockito.mock(Queue.BlockedItem.class);
        field(Queue.BlockedItem.class, "task").set(blockedItem, project);
        field(Queue.BlockedItem.class, "task").set(nonBlockingBlockedItem, nonBlockingProject);
        field(Queue.BlockedItem.class, "task").set(blockedItemOnDifferentNode, project);
        when(blockedItem.getAssignedLabel()).thenReturn(blockingLabel);
        when(blockedItemOnDifferentNode.getAssignedLabel()).thenReturn(nonBlockingLabel);
        when(nonBlockingBlockedItem.getAssignedLabel()).thenReturn(blockingLabel);
    }

    private void trainBuildableItems() throws IllegalAccessException {
        buildableItem = PowerMockito.mock(BuildableItem.class);
        buildableItemOnDifferentNode = PowerMockito.mock(BuildableItem.class);
        nonBlockingBuildableItem = PowerMockito.mock(BuildableItem.class);
        field(BuildableItem.class, "task").set(buildableItem, project);
        field(BuildableItem.class, "task").set(buildableItemOnDifferentNode, project);
        field(BuildableItem.class, "task").set(nonBlockingBuildableItem, nonBlockingProject);
        when(buildableItem.getAssignedLabel()).thenReturn(blockingLabel);
        when(buildableItemOnDifferentNode.getAssignedLabel()).thenReturn(blockingLabel);
        when(nonBlockingBuildableItem.getAssignedLabel()).thenReturn(blockingLabel);
    }

    private void trainExecutors() {
        when(idleExecutor.isBusy()).thenReturn(false);
        when(executor.isBusy()).thenReturn(true);
        when(executor.getCurrentWorkUnit()).thenReturn(workUnit);

        when(idleOneOffExecutor.isBusy()).thenReturn(false);
        when(oneOffExecutor.isBusy()).thenReturn(true);
        when(oneOffExecutor.getCurrentWorkUnit()).thenReturn(workUnit);

    }

    private void trainNodes() {
        when(node.toComputer()).thenReturn(computer);
    }

    private void trainJenkins() {
        PowerMockito.mockStatic(Jenkins.class);
        Jenkins jenkins = PowerMockito.mock(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(jenkins.getQueue()).thenReturn(queue);
        when(jenkins.getComputers()).thenReturn(new Computer[]{computer});
    }

    private void trainProjects() {
        project = PowerMockito.mock(Project.class);
        nonBlockingProject = PowerMockito.mock(Project.class);
        matrixProject = PowerMockito.mock(MatrixProject.class);
        nonBlockingMatrixProject = PowerMockito.mock(MatrixProject.class);
        when(project.getFullName()).thenReturn("blockingProject");
        when(nonBlockingProject.getFullName()).thenReturn("harmlessProject");
        when(matrixProject.getFullName()).thenReturn("blockingMatrixProject");
        when(nonBlockingMatrixProject.getFullName()).thenReturn("harmlessMatrixProject");
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesItemDoesNotSelfBlock() {
        when(queue.getBuildableItems(Mockito.any(Computer.class))).thenReturn(singletonList(buildableItem));
        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getBuildableItems(Mockito.any(Computer.class))).thenReturn(Collections.<BuildableItem>emptyList());

        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat((Project) monitor.checkNodeForBuildableQueueEntries(Mockito.mock(BuildableItem.class), node), is
                (equalTo(project)));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsNullForDifferentNode() {
        when(queue.getBuildableItems(eq(computer))).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat(monitor.checkNodeForBuildableQueueEntries(Mockito.mock(BuildableItem.class), Mockito.mock(Node
                .class)), is(nullValue()));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{});

        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItem});

        assertThat((Project) monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), node), is(equalTo
                (project)));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsNullForDifferentNode() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItemOnDifferentNode});

        assertThat(monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), Mockito.mock(Node.class)), is
                (nullValue()));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsBlockedTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItem});

        assertThat((Project) monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), node), is(equalTo
                (project)));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsNullForDifferentNodeCaseBlocked() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItemOnDifferentNode});

        assertThat(monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), Mockito.mock(Node.class)), is
                (nullValue()));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsWaitingTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItem});

        assertThat((Project) monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), node), is(equalTo
                (project)));
    }

    @Test
    public void testCheckNodeForQueueEntriesReturnsNullForDifferentNodeCaseWaiting() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItemOnDifferentNode});

        assertThat(monitor.checkNodeForQueueEntries(Mockito.mock(BuildableItem.class), Mockito.mock(Node.class)), is
                (nullValue()));
    }

    @Test
    public void testCheckForBuildableQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getBuildableItems()).thenReturn(Collections.<BuildableItem>emptyList());

        assertThat(monitor.checkForBuildableQueueEntries(buildableItem), is(nullValue()));
    }

    @Test
    public void testCheckForBuildableQueueEntriesItemDoesNotSelfBlock() {
        when(queue.getBuildableItems()).thenReturn(singletonList(buildableItem));
        assertThat(monitor.checkNodeForBuildableQueueEntries(buildableItem, node), is(nullValue()));
    }

    @Test
    public void testCheckForBuildableQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getBuildableItems()).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat((Project) monitor.checkForBuildableQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo
                (project)));
    }

    @Test
    public void testCheckForBuildableQueueEntriesReturnsProjectForDifferentNode() {
        when(queue.getBuildableItems()).thenReturn(asList(nonBlockingBuildableItem, buildableItem));

        assertThat((Project) monitor.checkForBuildableQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo
                (project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{});

        assertThat(monitor.checkForBuildableQueueEntries(buildableItem), is(nullValue()));
    }

    @Test
    public void testCheckForQueueEntriesReturnsBuildableTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItem});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsProjectForDifferentNode() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBuildableItem, buildableItemOnDifferentNode});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsBlockedTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItem});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsProjectForDifferentNodeCaseBlocked() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingBlockedItem, blockedItemOnDifferentNode});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsWaitingTaskThatIsQueued() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItem});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsProjectForDifferentNodeCaseWaiting() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, waitingItemOnDifferentNode});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(equalTo(project)));
    }

    @Test
    public void testCheckForQueueEntriesReturnsNullForNonBlockingItems() {
        when(queue.getItems()).thenReturn(new Queue.Item[]{nonBlockingWaitingItem, nonBlockingBuildableItem,
                nonBlockingBlockedItem});

        assertThat((Project) monitor.checkForQueueEntries(Mockito.mock(BuildableItem.class)), is(nullValue()));
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
    public void testCheckNodeForRunningBuildReturnsNullForDifferentRunningProject() throws IllegalAccessException {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsBlockedProjectIfItIsRunning() throws IllegalAccessException {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkNodeForRunningBuilds(node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForDifferentRunningProjectOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsBlockedProjectIfItIsRunningOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkNodeForRunningBuilds(node), is(equalTo(project)));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsNullForDifferentRunningMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(nonBlockingMatrixProject);

        assertThat(monitor.checkNodeForRunningBuilds(node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForRunningBuildReturnsBlockedMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(matrixProject);

        assertThat((MatrixProject) monitor.checkNodeForRunningBuilds(node), is(equalTo(matrixProject)));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForNonBusyExecutor() {
        when(computer.getExecutors()).thenReturn(singletonList(idleExecutor));
        when(computer.getOneOffExecutors()).thenReturn(new ArrayList<OneOffExecutor>());
        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForNonBusyOneOffExecutor() {
        when(computer.getExecutors()).thenReturn(new ArrayList<Executor>());
        when(computer.getOneOffExecutors()).thenReturn(singletonList(idleOneOffExecutor));
        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForDifferentRunningProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsBlockedProjectIfItIsRunning() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkAllNodesForRunningBuilds(), is(equalTo(project)));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForDifferentRunningProjectOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(nonBlockingProject);

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsBlockedProjectIfItIsRunningOnOneOffExecutor() {
        when(computer.getOneOffExecutors()).thenReturn(singletonList(oneOffExecutor));
        when(subTask.getOwnerTask()).thenReturn(project);

        assertThat((Project) monitor.checkAllNodesForRunningBuilds(), is(equalTo(project)));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsNullForDifferentRunningMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(nonBlockingMatrixProject);

        assertThat(monitor.checkAllNodesForRunningBuilds(), is(nullValue()));
    }

    @Test
    public void testCheckAllNodesForRunningBuildReturnsBlockedMatrixProject() {
        when(computer.getExecutors()).thenReturn(singletonList(executor));
        when(subTask.getOwnerTask()).thenReturn(configuration);
        when(configuration.getParent()).thenReturn(matrixProject);

        assertThat((MatrixProject) monitor.checkAllNodesForRunningBuilds(), is(equalTo(matrixProject)));
    }

}

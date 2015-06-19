package hudson.plugins.buildblocker;

import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Queue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.powermock.api.support.membermodification.MemberMatcher.field;

@PrepareForTest({Queue.BuildableItem.class, Project.class})
@RunWith(PowerMockRunner.class)
public class BuildBlockerQueueTaskDispatcherUnitTest {

    @Mock
    private BlockingJobsMonitor monitor;
    @Mock
    private Node node;

    private AbstractProject project;
    private Queue.BuildableItem item;

    private MonitorFactory monitorFactory;
    private BuildBlockerQueueTaskDispatcher dispatcher;

    @Before
    public void setup() throws IllegalAccessException {
        monitorFactory = new FieldReturningMonitorFactory(monitor);
        dispatcher = new BuildBlockerQueueTaskDispatcher(monitorFactory);

        project = PowerMockito.mock(AbstractProject.class);
        item = PowerMockito.mock(Queue.BuildableItem.class);
        field(Queue.Item.class, "task").set(item, project);
    }

    @Test
    public void testCanRunWithBuildBlockerDisabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyZeroInteractions(monitor);
    }

    @Test
    public void testCanRunWithBuildBlockerEnabledAndNullBlockingJobsDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyZeroInteractions(monitor);
    }

    @Test
    public void testCanRunWithGlobalEnabledAndCheckAllDisabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnGlobalLevel(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForBuildableQueueEntries(Mockito.eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    public void testCanRunWithGlobalEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnGlobalLevel(true)
                                .setScanAllQueueItemStates(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForQueueEntries(Mockito.eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    public void testCanRunWithNodeEnabledAndCheckAllDisabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnNodeLevel(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyZeroInteractions(monitor);
    }

    @Test
    public void testCanRunWithNodeEnabledAndCheckAllEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnNodeLevel(true)
                                .setScanAllQueueItemStates(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verifyZeroInteractions(monitor);
    }

    @Test
    public void testCanRunWithGlobalEnabledAndNodeEnabledAndCheckAllDisabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnGlobalLevel(true)
                                .setBlockOnNodeLevel(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForBuildableQueueEntries(Mockito.eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    public void testCanRunWithGlobalEnabledAndNodeEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnGlobalLevel(true)
                                .setBlockOnNodeLevel(true)
                                .setScanAllQueueItemStates(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canRun(item);

        verify(monitor, times(1)).checkAllNodesForRunningBuilds();
        verify(monitor, times(1)).checkForQueueEntries(Mockito.eq(item));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    public void testCanTakeWithBuildBlockerDisabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyZeroInteractions(monitor);
    }

    @Test
    public void testCanTakeWithBuildBlockerEnabledAndNullBlockingJobsDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyZeroInteractions(monitor);
    }

    @Test
    public void testCanTakeWithGlobalEnabledAndCheckAllDisabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnGlobalLevel(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyZeroInteractions(monitor);
    }

    @Test
    public void testCanTakeWithGlobalEnabledAndCheckAllEnabledDoesNothing() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnGlobalLevel(true)
                                .setScanAllQueueItemStates(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyZeroInteractions(monitor);
    }

    @Test
    public void testCanTakeWithNodeEnabledAndCheckAllDisabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnNodeLevel(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verify(monitor, times(1)).checkNodeForRunningBuilds(eq(node));
        verify(monitor, times(1)).checkNodeForBuildableQueueEntries(eq(item), eq(node));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    public void testCanTakeWithNodeEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnNodeLevel(true)
                                .setScanAllQueueItemStates(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verify(monitor, times(1)).checkNodeForRunningBuilds(eq(node));
        verify(monitor, times(1)).checkNodeForQueueEntries(eq(item), eq(node));
        verifyNoMoreInteractions(monitor);
    }

    @Test
    public void testCanTakeWithGlobalEnabledAndNodeEnabledAndCheckAllDisabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(
                        new BuildBlockerPropertyBuilder()
                                .setUseBuildBlocker(true)
                                .setBlockOnGlobalLevel(true)
                                .setBlockOnNodeLevel(true)
                                .setBlockingJobs("someJob")
                                .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyZeroInteractions(monitor);
    }

    @Test
    public void testCanTakeWithGlobalEnabledAndNodeEnabledAndCheckAllEnabledCallsCorrectMethods() {
        when(project.getProperty(eq(BuildBlockerProperty.class)))
                .thenReturn(new BuildBlockerPropertyBuilder()
                        .setUseBuildBlocker(true)
                        .setBlockOnGlobalLevel(true)
                        .setBlockOnNodeLevel(true)
                        .setScanAllQueueItemStates(true)
                        .setBlockingJobs("someJob")
                        .createBuildBlockerProperty());

        dispatcher.canTake(node, item);

        verifyZeroInteractions(monitor);
    }

    private class FieldReturningMonitorFactory implements MonitorFactory {

        private BlockingJobsMonitor monitor;

        @Override
        public BlockingJobsMonitor build(String blockingJobs) {
            return monitor;
        }

        public FieldReturningMonitorFactory(BlockingJobsMonitor monitor) {
            this.monitor = monitor;
        }
    }
}

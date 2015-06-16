package hudson.plugins.buildblocker;

import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckNodeForBuildableQueueEntriesNeedsNode() {
        monitor.checkNodeForBuildableQueueEntries(null, null);
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsNullIfNothingIsQueued() {
        when(queue.getBuildableItems(any(Computer.class))).thenReturn(Collections.<BuildableItem>emptyList());

        assertThat(monitor.checkNodeForBuildableQueueEntries(item, node), is(nullValue()));
    }

    @Test
    public void testCheckNodeForBuildableQueueEntriesReturnsTaskThatIsQueued() {
        when(queue.getBuildableItems(any(Computer.class))).thenReturn(asList(nonBlockingItem, item));

        assertThat((Project) monitor.checkNodeForBuildableQueueEntries(mock(BuildableItem.class), node), is(equalTo(project)));
    }

}

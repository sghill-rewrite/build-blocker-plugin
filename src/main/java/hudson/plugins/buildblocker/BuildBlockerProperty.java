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

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.util.logging.Level.FINE;

/**
 * Job property that stores the line feed separated list of
 * regular expressions that define the blocking jobs.
 */
public class BuildBlockerProperty extends JobProperty<Job<?, ?>> {
    /**
     * the logger
     */
    private static final Logger LOG = Logger.getLogger(BuildBlockerProperty.class.getName());

    /**
     * the enable checkbox in the job's config
     */
    public static final String USE_BUILD_BLOCKER = "useBuildBlocker";

    /**
     * blocking jobs form field name
     */
    public static final String BLOCKING_JOBS_KEY = "blockingJobs";

    /**
     * flag if build blocker should be used
     */
    private boolean useBuildBlocker;

    private BlockLevel blockLevel;
    private QueueScanScope scanQueueFor;
    /**
     * the job names that block the build if running
     */
    private String blockingJobs;

    public BlockLevel getBlockLevel() {
        return blockLevel;
    }

    public QueueScanScope getScanQueueFor() {
        return scanQueueFor;
    }

    /**
     * Returns true if the build blocker is enabled.
     *
     * @return true if the build blocker is enabled
     */
    public boolean isUseBuildBlocker() {
        return useBuildBlocker;
    }

    /**
     * Sets the build blocker flag.
     *
     * @param useBuildBlocker the build blocker flag
     */
    public void setUseBuildBlocker(boolean useBuildBlocker) {
        this.useBuildBlocker = useBuildBlocker;
    }


    /**
     * Returns the text of the blocking jobs field.
     *
     * @return the text of the blocking jobs field
     */
    public String getBlockingJobs() {
        return blockingJobs;
    }

    /**
     * Sets the blocking jobs field
     *
     * @param blockingJobs the blocking jobs entry
     */
    public void setBlockingJobs(String blockingJobs) {
        this.blockingJobs = blockingJobs;
    }

    @DataBoundConstructor
    public BuildBlockerProperty(boolean useBuildBlocker, BlockLevel blockLevel, QueueScanScope scanQueueFor, String blockingJobs) {
        LOG.logp(FINE, getClass().getName(), "BuildBlockerProperty", "useBuildBlocker: " + useBuildBlocker + " blockLevel: " + blockLevel + " scanQueueFor: " +
                scanQueueFor + " blockingJobs: " + blockingJobs);
        this.useBuildBlocker = useBuildBlocker;
        this.scanQueueFor = scanQueueFor;
        this.blockLevel = blockLevel;
        this.blockingJobs = blockingJobs;
    }

//    public BuildBlockerProperty() {
//    }

    /**
     * Descriptor
     */
    @Extension
    public static final class BuildBlockerDescriptor extends JobPropertyDescriptor {

        /**
         * Returns the name to be shown on the website
         *
         * @return the name to be shown on the website.
         */
        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }


        /**
         * Chcek the regular expression entered by the user
         */
        public FormValidation doCheckRegex(@QueryParameter final String blockingJobs) {
            List<String> listJobs = null;
            if (StringUtils.isNotBlank(blockingJobs)) {
                listJobs = Arrays.asList(blockingJobs.split("\n"));
            }
            if (listJobs != null) {
                for (String blockingJob : listJobs) {
                    try {
                        Pattern.compile(blockingJob);
                    } catch (PatternSyntaxException pse) {
                        return FormValidation.error("Invalid regular expression [" +
                                blockingJob + "] exception: " +
                                pse.getDescription());
                    }
                }
                return FormValidation.ok();
            } else {
                return FormValidation.ok();
            }
        }

        /**
         * Returns always true as it can be used in all types of jobs.
         *
         * @param jobType the job type to be checked if this property is applicable.
         * @return true
         */
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }
    }

    public static class BlockLevel {
        private static final String GLOBAL = "global";
        private static final String NODE = "node";

        //default is global
        private String value = GLOBAL;

        @DataBoundConstructor
        public BlockLevel(String value) {
            this.value = value;
        }

        public boolean isGlobal() {
            return value.equals(GLOBAL);
        }

        public boolean isNode() {
            return value.equals(NODE);
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "BlockLevel{" + "value='" + value + '\'' + '}';
        }
    }

    public static class QueueScanScope {
        private static final String ALL = "all";
        private static final String BUILDABLE = "buildable";
        public static final String DISABLED = "disabled";

        //default is disabled
        private String value = DISABLED;

        @DataBoundConstructor
        public QueueScanScope(String value) {
            this.value = value;
        }

        public boolean isAll() {
            return value.equals(ALL);
        }

        public boolean isBuildable() {
            return value.equals(BUILDABLE);
        }

        public boolean isDisabled() {
            return !isAll() && !isBuildable();
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "QueueScanScope{" + "value='" + value + '\'' + '}';
        }
    }

}

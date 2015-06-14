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
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

    private boolean blockOnNodeLevel;
    private boolean blockOnGlobalLevel;
    private boolean scanAllQueueItemStates;

    /**
     * the job names that block the build if running
     */
    private String blockingJobs;

    /**
     * Returns true if the build blocker is enabled.
     *
     * @return true if the build blocker is enabled
     */
    @SuppressWarnings("unused")
    public boolean isUseBuildBlocker() {
        return useBuildBlocker;
    }

    /**
     * Sets the build blocker flag.
     *
     * @param useBuildBlocker the build blocker flag
     */
    public void setUseBuildBlocker(boolean useBuildBlocker) {
        LOG.fine("use build blocker: " + useBuildBlocker);
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
        LOG.fine("blocking jobs: " + blockingJobs);
        this.blockingJobs = blockingJobs;
    }

    public boolean isBlockOnNodeLevel() {
        return blockOnNodeLevel;
    }

    public void setBlockOnNodeLevel(boolean blockOnNodeLevel) {
        LOG.fine("block on node level: " + blockOnNodeLevel);
        this.blockOnNodeLevel = blockOnNodeLevel;
    }

    public boolean isBlockOnGlobalLevel() {
        return blockOnGlobalLevel;
    }

    public void setBlockOnGlobalLevel(boolean blockOnGlobalLevel) {
        LOG.fine("block on global level: " + blockOnGlobalLevel);
        this.blockOnGlobalLevel = blockOnGlobalLevel;
    }

    public boolean isScanAllQueueItemStates() {
        return scanAllQueueItemStates;
    }

    public void setScanAllQueueItemStates(boolean scanAllQueueItemStates) {
        LOG.fine("scan all queue item states: " + scanAllQueueItemStates);
        this.scanAllQueueItemStates = scanAllQueueItemStates;
    }

    /**
     * Descriptor
     */
    @SuppressWarnings("unused")
    @Extension
    public static final class BuildBlockerDescriptor extends JobPropertyDescriptor {

        /**
         * Constructor loading the data from the config file
         */
        public BuildBlockerDescriptor() {
            load();
        }

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
         * Returns a new instance of the build blocker property
         * when job config page is saved.
         *
         * @param req      stapler request
         * @param formData the form data
         * @return a new instance of the build blocker property
         * @throws FormException
         */
        @Override
        public BuildBlockerProperty newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            BuildBlockerProperty buildBlockerProperty = new BuildBlockerProperty();

            if (formData.containsKey(USE_BUILD_BLOCKER)) {
                try {
                    buildBlockerProperty.setUseBuildBlocker(true);
                    buildBlockerProperty.setBlockingJobs(formData.getJSONObject(USE_BUILD_BLOCKER).getString(BLOCKING_JOBS_KEY));

                } catch (JSONException e) {
                    buildBlockerProperty.setUseBuildBlocker(false);
                    LOG.log(Level.WARNING, "could not get blocking jobs from " + formData.toString());
                }
            }

            return buildBlockerProperty;
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

}

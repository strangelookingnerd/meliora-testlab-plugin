package fi.meliora.testlab.ext.jenkins;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * A post build action to publish test results to Meliora Testlab.
 *
 * @author Marko Kanala, Meliora Ltd
 */
public class TestlabNotifier extends Notifier {

    /*
        BuildSteps that run after the build is completed.

        Notifier is a kind of Publisher that sends out the outcome of the builds
        to other systems and humans. This marking ensures that notifiers are run
        after the build result is set to its final value by other Recorders. To run
        even after the build is marked as complete, override Publisher.needsToRunAfterFinalized()
        to return true.
     */

    // project key which to publish the results to
    private String projectKey;

    public String getProjectKey() {
        return projectKey;
    }

    // name of the test run to create or update at Testlab side
    private String testRunTitle;

    public String getTestRunTitle() {
        return testRunTitle;
    }

    // title of the version the results are bound to in Testlab
    private String testTargetTitle;

    public String getTestTargetTitle() {
        return testTargetTitle;
    }

    // title of the environment the results are bound to in Testlab
    private String testEnvironmentTitle;

    public String getTestEnvironmentTitle() {
        return testEnvironmentTitle;
    }

    // holder for optional issues settings
    private IssuesSettings issuesSettings;

    public IssuesSettings getIssuesSettings() {
        return issuesSettings;
    }

    // if true added issues are merged and added as a single issue
    private boolean mergeAsSingleIssue;

    public boolean isMergeAsSingleIssue() {
        return mergeAsSingleIssue;
    }

    // if set issues are automatically assigned to this user
    private String assignToUser;

    public String getAssignToUser() {
        return assignToUser;
    }

    // if set we try to reopen existing matching issues on push
    private boolean reopenExisting;

    public boolean isReopenExisting() {
        return reopenExisting;
    }

    // holder for optional advanced settings
    private AdvancedSettings advancedSettings;

    public AdvancedSettings getAdvancedSettings() {
        return advancedSettings;
    }

    // job specific company ID of target testlab, optional
    private String companyId;

    public String getCompanyId() {
        return companyId;
    }

    // job specific apikey of target testlab, optional
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    // title of the Testlab custom field to use to map the unit tests to Testlab's test cases, optional
    private String testCaseMappingField;

    public String getTestCaseMappingField() {
        return testCaseMappingField;
    }

    /**
     * This annotation tells Hudson to call this constructor, with
     * values from the configuration form page with matching parameter names.
     */
    @DataBoundConstructor
    public TestlabNotifier(String projectKey, String testRunTitle, String testTargetTitle, String testEnvironmentTitle, IssuesSettings issuesSettings, AdvancedSettings advancedSettings) {
        this.projectKey = projectKey;
        this.testRunTitle = testRunTitle;
        this.testTargetTitle = testTargetTitle;
        this.testEnvironmentTitle = testEnvironmentTitle;

        this.issuesSettings = issuesSettings;
        if(issuesSettings != null) {
            this.mergeAsSingleIssue = issuesSettings.isMergeAsSingleIssue();
            this.assignToUser = issuesSettings.getAssignToUser();
            this.reopenExisting = issuesSettings.isReopenExisting();
        }

        this.advancedSettings = advancedSettings;
        if(advancedSettings != null) {
            this.companyId = advancedSettings.getCompanyId();
            this.apiKey = advancedSettings.getApiKey();
            this.testCaseMappingField = advancedSettings.getTestCaseMappingField();
        }
    }

    /**
     * Return true if this {@link hudson.tasks.Publisher} needs to run after the build result is
     * fully finalized.
     * <p/>
     * <p/>
     * The execution of normal {@link hudson.tasks.Publisher}s are considered within a part
     * of the build. This allows publishers to mark the build as a failure, or
     * to include their execution time in the total build time.
     * <p/>
     * <p/>
     * So normally, that is the preferrable behavior, but in a few cases
     * this is problematic. One of such cases is when a publisher needs to
     * trigger other builds, which in turn need to see this build as a
     * completed build. Those plugins that need to do this can return true
     * from this method, so that the {@link #perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)}
     * method is called after the build is marked as completed.
     * <p/>
     * <p/>
     * When {@link hudson.tasks.Publisher} behaves this way, note that they can no longer
     * change the build status anymore.
     *
     * @author Kohsuke Kawaguchi
     * @since 1.153
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    /**
     * Declares the scope of the synchronization monitor this {@link hudson.tasks.BuildStep} expects from outside.
     *
     * @since 1.319
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    /**
     * Runs the step over the given build and reports the progress to the listener.
     *
     * A plugin can contribute the action object to Actionable.getActions() so that a 'report'
     * becomes a part of the persisted data of Build. This is how JUnit plugin attaches the
     * test report to a build page, for example.
     *
     * Using the return value to indicate success/failure should be considered deprecated,
     * and implementations are encouraged to throw AbortException to indicate a failure.
     *
     * @param build
     * @param launcher
     * @param listener
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Publishing test results to Testlab project: " + projectKey);

        DescriptorImpl d = getDescriptor();

        // get job specific settings if any and fallback to global configuration
        String runCompanyId = isBlank(companyId) ? d.getCompanyId() : companyId;
        String runApiKey = isBlank(apiKey) ? d.getApiKey() : apiKey;
        String runTestCaseMappingField = isBlank(testCaseMappingField) ? d.getTestCaseMappingField() : testCaseMappingField;

        String abortError = null;
        if(isBlank(runCompanyId)) {
            abortError = "Could not publish results to Testlab: Company ID is not set. Configure it for your job or globally in Jenkins' configuration.";
        }

        if(isBlank(runApiKey)) {
            abortError = "Could not publish results to Testlab: Api Key is not set. Configure it for your job or globally in Jenkins' configuration.";
        }

        if(isBlank(runTestCaseMappingField)) {
            abortError = "Could not publish results to Testlab: Test case mapping field is not set. Configure it for your job or globally in Jenkins' configuration.";
        }

        if(isBlank(projectKey)) {
            abortError = "Could not publish results to Testlab: Project key is not set. Configure it for your job.";
        }

        if(isBlank(testRunTitle)) {
            abortError = "Could not publish results to Testlab: Test run title is not set. Configure it for your job.";
        }

        if(abortError != null) {
            listener.error(abortError);
            throw new AbortException(abortError);
        }

        Sender.sendResults(
                runCompanyId,
                runApiKey,
                projectKey,
                testRunTitle,
                testTargetTitle,
                testEnvironmentTitle,
                issuesSettings != null,
                mergeAsSingleIssue,
                reopenExisting,
                !isBlank(assignToUser) ? assignToUser : null,
                runTestCaseMappingField,
                build
        );

        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return (DescriptorImpl)super.getDescriptor();
    }

    // this annotation tells Hudson that this is the implementation of an extension point
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        // company id of the testlab which to publish to
        private String companyId;
        // testlab api key
        private String apiKey;
        // custom field name to map the test ids against with
        private String testCaseMappingField;

        public DescriptorImpl() {
            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Publish test results to Testlab";
        }

        /**
         * Applicable to any kind of project.
         */
        @Override
        public boolean isApplicable(Class type) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws Descriptor.FormException {
            // persist configuration
            companyId = json.getString("companyId");
            apiKey = json.getString("apiKey");
            testCaseMappingField = json.getString("testCaseMappingField");
            save();
            return true; // indicate that everything is good so far
        }

        public String getCompanyId() {
            return companyId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getTestCaseMappingField() {
            return testCaseMappingField;
        }
    }

    /**
     * Optional job config block for advanced settings.
     */
    public static final class AdvancedSettings {
        // job specific company ID of target testlab, optional
        private String companyId;

        public String getCompanyId() {
            return companyId;
        }

        // job specific apikey of target testlab, optional
        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        // title of the Testlab custom field to use to map the unit tests to Testlab's test cases, optional
        private String testCaseMappingField;

        public String getTestCaseMappingField() {
            return testCaseMappingField;
        }

        @DataBoundConstructor
        public AdvancedSettings(String companyId, String apiKey, String testCaseMappingField) {
            this.companyId = companyId;
            this.apiKey = apiKey;
            this.testCaseMappingField = testCaseMappingField;
        }

    }

    /**
     * Optional job config block for issues.
     *
     * If set implicitly implies that issues should be added on push.
     */
    public static final class IssuesSettings {
        // if true added issues are merged and added as a single issue
        private boolean mergeAsSingleIssue;

        public boolean isMergeAsSingleIssue() {
            return mergeAsSingleIssue;
        }

        // if set issues are automatically assigned to this user
        private String assignToUser;

        public String getAssignToUser() {
            return assignToUser;
        }

        // if set we try to reopen existing matching issues on push
        private boolean reopenExisting;

        public boolean isReopenExisting() {
            return reopenExisting;
        }

        @DataBoundConstructor
        public IssuesSettings(boolean mergeAsSingleIssue, String assignToUser, boolean reopenExisting) {
            this.mergeAsSingleIssue = mergeAsSingleIssue;
            this.assignToUser = assignToUser;
            this.reopenExisting = reopenExisting;
        }
    }

    /**
     * @return true if trimmed String is empty
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

}

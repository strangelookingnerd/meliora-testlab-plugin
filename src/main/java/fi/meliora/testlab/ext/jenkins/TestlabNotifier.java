package fi.meliora.testlab.ext.jenkins;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.PluginServletFilter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A post build action to publish test results to Meliora Testlab.
 *
 * @author Marko Kanala, Meliora Ltd
 */
public class TestlabNotifier extends Notifier {
    private final static Logger log = Logger.getLogger(TestlabNotifier.class.getName());

    /*
        BuildSteps that run after the build is completed.

        Notifier is a kind of Publisher that sends out the outcome of the builds
        to other systems and humans. This marking ensures that notifiers are run
        after the build result is set to its final value by other Recorders. To run
        even after the build is marked as complete, override Publisher.needsToRunAfterFinalized()
        to return true.
     */

    public static final String DEFAULT_COMMENT_TEMPLATE
            = "Jenkins build: ${BUILD_FULL_DISPLAY_NAME} ${BUILD_RESULT}, ${BUILD_URL} - ${BUILD_STATUS}";

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

    // comment of the test run to create or update at Testlab side
    private String comment;

    public String getComment() {
        return comment;
    }

    // identifier or a title of a milestone the results are bound to in Testlab
    private String milestone;

    public String getMilestone() {
        return milestone;
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

    // tags for the test run
    private String tags;

    public String getTags() {
        return tags;
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

    // if set, on-premise variant of Testlab is used and Testlab URL should be set and honored
    private Usingonpremise usingonpremise;

    public Usingonpremise getUsingonpremise() {
        return usingonpremise;
    }

    /**
     * This annotation tells Hudson to call this constructor, with
     * values from the configuration form page with matching parameter names.
     */
    @DataBoundConstructor
    public TestlabNotifier(String projectKey, String testRunTitle, String comment, String milestone, String testTargetTitle, String testEnvironmentTitle, String tags, IssuesSettings issuesSettings, AdvancedSettings advancedSettings) {
        this.projectKey = projectKey;
        this.testRunTitle = testRunTitle;
        this.comment = comment;
        this.milestone = milestone;
        this.testTargetTitle = testTargetTitle;
        this.testEnvironmentTitle = testEnvironmentTitle;
        this.tags = tags;

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
            this.usingonpremise = advancedSettings.getUsingonpremise();
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

        log.fine("perform(): " + this + ", descriptor: " + d);

        // get job specific settings if any and fallback to global configuration
        String runApiKey = isBlank(apiKey) ? d.getApiKey() : apiKey;
        String runTestCaseMappingField = isBlank(testCaseMappingField) ? d.getTestCaseMappingField() : testCaseMappingField;

        Usingonpremise uop = advancedSettings != null && advancedSettings.getUsingonpremise() != null
                ? advancedSettings.getUsingonpremise() : d.getUsingonpremise();

        String runCompanyId = null, runOnpremiseurl = null;
        boolean runUsingonpremise = false;

        if(uop != null && !isBlank(uop.getOnpremiseurl())) {
            //
            // we apply onpremise settings only if they are complete
            //
            runCompanyId = null;
            runUsingonpremise = true;
            runOnpremiseurl = uop.getOnpremiseurl();

            log.fine("using on-premise with url: " + runOnpremiseurl);

        } else {
            //
            // otherwise we use companyId if present
            //
            runCompanyId = !isBlank(companyId) ? companyId : d.getCompanyId();

            log.fine("using hosted with company id: " + runCompanyId);
        }

        // replace env vars for applicable fields

        EnvVars envVars = build.getEnvironment(listener);

        Map<String, String> additionalKeys = new HashMap<String, String>();
        additionalKeys.put("BUILD_FULL_DISPLAY_NAME", build.getFullDisplayName());
        Run.Summary summary = build.getBuildStatusSummary();
        additionalKeys.put("BUILD_STATUS", summary != null ? summary.message : "[No build status available]");
        Result result = build.getResult();
        additionalKeys.put("BUILD_RESULT", result != null ? result.toString() : "[No build result available]");

        VariableReplacer vr = new VariableReplacer(envVars, additionalKeys);

        if(log.isLoggable(Level.FINE)) {
            log.fine("Environment variables:");
            for(String key : vr.getVars().keySet()) {
                log.fine(" " + key + "=" + vr.getVars().get(key));
            }
        }

        String runProjectKey = vr.replace(projectKey);
        String runMilestone = vr.replace(milestone);
        String runTestRunTitle = vr.replace(testRunTitle);
        String runComment = vr.replace(isBlank(comment) ? DEFAULT_COMMENT_TEMPLATE : comment);
        String runTestTargetTitle = vr.replace(testTargetTitle);
        String runTestEnvironmentTitle = vr.replace(testEnvironmentTitle);
        String runTags = vr.replace(tags);
        String runAssignToUser = vr.replace(assignToUser);
        runTestCaseMappingField = vr.replace(runTestCaseMappingField);

        String abortError = null;
        if(!runUsingonpremise && isBlank(runCompanyId)) {
            abortError = "Could not publish results to Testlab: Company ID is not set. Configure it for your job or globally in Jenkins' configuration.";
        }

        if(runUsingonpremise && isBlank(runOnpremiseurl)) {
            abortError = "Could not publish results to Testlab: Testlab URL for on-premise Testlab is not set. Configure it for your job or globally in Jenkins' configuration.";
        }

        if(isBlank(runApiKey)) {
            abortError = "Could not publish results to Testlab: Api Key is not set. Configure it for your job or globally in Jenkins' configuration.";
        }

        if(isBlank(runTestCaseMappingField)) {
            abortError = "Could not publish results to Testlab: Test case mapping field is not set. Configure it for your job or globally in Jenkins' configuration or, if the value contains variable tags make sure they have values.";
        }

        if(isBlank(runProjectKey)) {
            abortError = "Could not publish results to Testlab: Project key is not set. Configure it for your job or, if the value contains variable tags make sure they have values.";
        }

        if(isBlank(runTestRunTitle)) {
            abortError = "Could not publish results to Testlab: Test run title is not set. Configure it for your job or, if the value contains variable tags make sure they have values.";
        }

        if(abortError != null) {
            listener.error(abortError);
            throw new AbortException(abortError);
        }

        Sender.sendResults(
                runCompanyId,
                runUsingonpremise,
                runOnpremiseurl,
                runApiKey,
                runProjectKey,
                runMilestone,
                runTestRunTitle,
                runComment,
                runTestTargetTitle,
                runTestEnvironmentTitle,
                runTags,
                issuesSettings != null,
                mergeAsSingleIssue,
                reopenExisting,
                !isBlank(runAssignToUser) ? runAssignToUser : null,
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
        // if set, on-premise variant of Testlab is used and Testlab URL should be set and honored
        private Usingonpremise usingonpremise;
        // defines CORS settings for calls from Testlab -> Jenkins API
        private Cors cors;

        private CORSFilter CORSFilter;

        public DescriptorImpl() {
            load();

            log.fine("load: " + companyId + ", api key hidden, " + testCaseMappingField + ", " + usingonpremise + ", " + usingonpremise + ", " + cors);

            // let's inject our CORSFilter as we're at it
            try {
                CORSFilter = new CORSFilter();
                PluginServletFilter.addFilter(CORSFilter);
                log.info("CORSFilter injected.");
            } catch (ServletException se) {
                log.warning("Could not inject CORSFilter.");
                se.printStackTrace();
            }

            configureCORS();
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

            JSONObject uop = json.getJSONObject("usingonpremise");
            if(uop != null && !uop.isNullObject() && !uop.isEmpty()) {
                usingonpremise = new Usingonpremise(uop.getString("onpremiseurl"));
            } else {
                usingonpremise = null;
            }

            JSONObject c = json.getJSONObject("cors");
            if(c != null && !c.isNullObject() && !c.isEmpty()) {
                cors = new Cors(c.getString("origin"));
            } else {
                cors = null;
            }

            log.fine("configure: " + companyId + ", api key hidden, " + testCaseMappingField + ", " + usingonpremise + ", " + cors);

            save();

            configureCORS();

            return true; // indicate that everything is good so far
        }

        protected void configureCORS() {
            CORSFilter.setEnabled(cors != null && !isBlank(cors.getOrigin()));
            if(cors != null && cors.getOrigin() != null) {
                //
                // parse a comma separated list to a list of allowed origins
                //
                String[] spl = cors.getOrigin().split(",");
                List<String> origins = new ArrayList<String>();
                for(String o : spl) {
                    origins.add(o.trim());
                }
                CORSFilter.setOrigins(origins);
            }
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

        public Usingonpremise getUsingonpremise() {
            return usingonpremise;
        }

        public Cors getCors() {
            return cors;
        }

        public String getDefaultCommentTemplate() {
            return DEFAULT_COMMENT_TEMPLATE;
        }

        @Override
        public String toString() {
            return "DescriptorImpl{" +
                    "companyId='" + companyId + '\'' +
                    ", apiKey='hidden'" +
                    ", testCaseMappingField='" + testCaseMappingField + '\'' +
                    ", usingonpremise=" + usingonpremise +
                    ", cors=" + cors +
                    '}';
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

        // if set, on-premise variant of Testlab is used and Testlab URL should be set and honored
        private Usingonpremise usingonpremise;

        public Usingonpremise getUsingonpremise() {
            return usingonpremise;
        }

        @DataBoundConstructor
        public AdvancedSettings(String companyId, String apiKey, String testCaseMappingField, Usingonpremise usingonpremise) {
            this.companyId = companyId;
            this.apiKey = apiKey;
            this.testCaseMappingField = testCaseMappingField;
            this.usingonpremise = usingonpremise;
        }

        @Override
        public String toString() {
            return "AdvancedSettings{" +
                    "companyId='" + companyId + '\'' +
                    ", apiKey='hidden'" +
                    ", testCaseMappingField='" + testCaseMappingField + '\'' +
                    ", usingonpremise=" + usingonpremise +
                    '}';
        }
    }

    /**
     * Optional job config block for on-premise settings.
     */
    public static final class Usingonpremise {
        // full url address of on-premise Testlab
        private String onpremiseurl;

        public String getOnpremiseurl() {
            return onpremiseurl;
        }

        @DataBoundConstructor
        public Usingonpremise(String onpremiseurl) {
            this.onpremiseurl = onpremiseurl;
        }

        @Override
        public String toString() {
            return "Usingonpremise{" +
                    "onpremiseurl='" + onpremiseurl + '\'' +
                    '}';
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

        @Override
        public String toString() {
            return "IssuesSettings{" +
                    "mergeAsSingleIssue=" + mergeAsSingleIssue +
                    ", assignToUser='" + assignToUser + '\'' +
                    ", reopenExisting=" + reopenExisting +
                    '}';
        }
    }

    /**
     * Optional config block for CORS settings.
     */
    public static final class Cors {
        // allow origin
        private String origin;

        public String getOrigin() {
            return origin;
        }

        @DataBoundConstructor
        public Cors(String origin) {
            this.origin = origin;
        }

        @Override
        public String toString() {
            return "Cors{" +
                    "origin='" + origin + '\'' +
                    '}';
        }
    }

    /**
     * @return true if trimmed String is empty
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

    @Override
    public String toString() {
        return "TestlabNotifier{" +
                "projectKey='" + projectKey + '\'' +
                ", testRunTitle='" + testRunTitle + '\'' +
                ", comment='" + comment + '\'' +
                ", milestone='" + milestone + '\'' +
                ", testTargetTitle='" + testTargetTitle + '\'' +
                ", testEnvironmentTitle='" + testEnvironmentTitle + '\'' +
                ", issuesSettings=" + issuesSettings +
                ", mergeAsSingleIssue=" + mergeAsSingleIssue +
                ", assignToUser='" + assignToUser + '\'' +
                ", reopenExisting=" + reopenExisting +
                ", advancedSettings=" + advancedSettings +
                ", companyId='" + companyId + '\'' +
                ", apiKey='hidden'" +
                ", testCaseMappingField='" + testCaseMappingField + '\'' +
                ", usingonpremise=" + usingonpremise +
                '}';
    }
}

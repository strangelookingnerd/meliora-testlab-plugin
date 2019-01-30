package fi.meliora.testlab.ext.jenkins;

import hudson.*;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.PluginServletFilter;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
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
 * @author Meliora Ltd
 */
public class TestlabNotifier extends Notifier implements SimpleBuildStep {
    private final static Logger log = Logger.getLogger(TestlabNotifier.class.getName());

    /*
        BuildSteps that run after the build is completed.

        Notifier is a kind of Publisher that sends out the outcome of the builds
        to other systems and humans. This marking ensures that notifiers are run
        after the build result is set to its final value by other Recorders. To run
        even after the build is marked as complete, override Publisher.needsToRunAfterFinalized()
        to return true.

        This notifier can also be used as a Pipeline step:

        post {
            always {
                junit '** /build/test-results/** /*.xml'
                melioraTestlab(
                    projectKey: 'PRJX',
                    testRunTitle: 'Automated tests',
                    comment: 'Jenkins build: ${BUILD_FULL_DISPLAY_NAME} ${BUILD_RESULT}, ${BUILD_URL}',
                    milestone: 'M1',
                    testTargetTitle: 'Version 1.0',
                    testEnvironmentTitle: 'integration-env',
                    tags: 'jenkins nightly',
                    parameters: 'BROWSER, USERNAME',
                    issuesSettings: [
                        mergeAsSingleIssue: true,
                        reopenExisting: true,
                        assignToUser: 'agentsmith'
                    ],
                    importTestCases: [
                        importTestCasesRootCategory: 'Imported/Jenkins'
                    ],
                    publishTap: [
                        tapTestsAsSteps: true,
                        tapFileNameInIdentifier: true,
                        tapTestNumberInIdentifier: false,
                        tapMappingPrefix: 'tap-'
                    ],
                    publishRobot: [
                        robotOutput: '** /output.xml',
                        robotCatenateParentKeywords: true
                    ],
                    advancedSettings: [
                        companyId: 'testcompany',
                        apiKey: hudson.util.Secret.fromString('verysecretapikey'),
                        testCaseMappingField: 'Test class',
                        usingonpremise: [
                            onpremiseurl: 'http://testcompany:8080/'
                        ]
                    ]
                )
            }
        }
    */

    public static final String DEFAULT_COMMENT_TEMPLATE
            = "Jenkins build: ${BUILD_FULL_DISPLAY_NAME} ${BUILD_RESULT}, ${BUILD_URL} - ${BUILD_STATUS}";

    /**
     * Note: All optional parameters need a setter annotated with @DataBoundSetter
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

    // comment of the test run to create or update at Testlab side
    private String comment;

    public String getComment() {
        return comment;
    }

    @DataBoundSetter
    public void setComment(String comment) {
        this.comment = comment;
    }

    // identifier or a title of a milestone the results are bound to in Testlab
    private String milestone;

    public String getMilestone() {
        return milestone;
    }

    @DataBoundSetter
    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    // title of the version the results are bound to in Testlab
    private String testTargetTitle;

    public String getTestTargetTitle() {
        return testTargetTitle;
    }

    @DataBoundSetter
    public void setTestTargetTitle(String testTargetTitle) {
        this.testTargetTitle = testTargetTitle;
    }

    // title of the environment the results are bound to in Testlab
    private String testEnvironmentTitle;

    public String getTestEnvironmentTitle() {
        return testEnvironmentTitle;
    }

    @DataBoundSetter
    public void setTestEnvironmentTitle(String testEnvironmentTitle) {
        this.testEnvironmentTitle = testEnvironmentTitle;
    }

    // tags for the test run
    private String tags;

    public String getTags() {
        return tags;
    }

    @DataBoundSetter
    public void setTags(String tags) {
        this.tags = tags;
    }

    // test case parameters to send from environmental variables
    private String parameters;

    public String getParameters() {
        return parameters;
    }

    @DataBoundSetter
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    // holder for optional issues settings
    private IssuesSettings issuesSettings;

    public IssuesSettings getIssuesSettings() {
        return issuesSettings;
    }

    @DataBoundSetter
    public void setIssuesSettings(IssuesSettings issuesSettings) {
        this.issuesSettings = issuesSettings;
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

    @DataBoundSetter
    public void setAdvancedSettings(AdvancedSettings advancedSettings) {
        this.advancedSettings = advancedSettings;
    }

    // job specific company ID of target testlab, optional
    private String companyId;

    public String getCompanyId() {
        return companyId;
    }

    // job specific apikey of target testlab, optional
    private Secret apiKey;

    public Secret getApiKey() {
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

    // if set, publish TAP results
    private PublishTap publishTap;

    public PublishTap getPublishTap() {
        return publishTap;
    }

    @DataBoundSetter
    public void setPublishTap(PublishTap publishTap) {
        this.publishTap = publishTap;
    }

    // If set, each TAP file will be mapped to a single test case in Testlab and the steps of the test case will be overwritten and matched to sent lines in TAP file
    private boolean tapTestsAsSteps;

    public boolean isTapTestsAsSteps() {
        return tapTestsAsSteps;
    }

    // If set, the name of the TAP file containing the tests is included in the mapping identifier as a prefix
    private boolean tapFileNameInIdentifier;

    public boolean isTapFileNameInIdentifier() {
        return tapFileNameInIdentifier;
    }

    // If set, the mapping identifier will not include the test number of the TAP test
    private boolean tapTestNumberInIdentifier;

    public boolean isTapTestNumberInIdentifier() {
        return tapTestNumberInIdentifier;
    }

    // If set, mapping identifiers sent will be prefixed with this value
    private String tapMappingPrefix;

    public String getTapMappingPrefix() {
        return tapMappingPrefix;
    }

    // if set, automatically creates test cases when publishing
    public ImportTestCases importTestCases;

    public ImportTestCases getImportTestCases() {
        return importTestCases;
    }

    @DataBoundSetter
    public void setImportTestCases(ImportTestCases importTestCases) {
        this.importTestCases = importTestCases;
    }

    // If set, sets the root category path where the test cases are created. By default, "Import".
    private String importTestCasesRootCategory;

    public String getImportTestCasesRootCategory() {
        return importTestCasesRootCategory;
    }

    // If set, publish Robot Framework results
    private PublishRobot publishRobot;

    public PublishRobot getPublishRobot() {
        return publishRobot;
    }

    @DataBoundSetter
    public void setPublishRobot(PublishRobot publishRobot) {
        this.publishRobot = publishRobot;
    }

    // Robot output.xml file path
    private String robotOutput;

    public String getRobotOutput() {
        return robotOutput;
    }

    // If set, catenates all sub keywords of a keyword as a single step in result
    private boolean robotCatenateParentKeywords;

    public boolean isRobotCatenateParentKeywords() {
        return robotCatenateParentKeywords;
    }

    /**
     * This annotation tells Hudson to call this constructor, with
     * values from the configuration form page with matching parameter names.
     */
    @DataBoundConstructor
    public TestlabNotifier(String projectKey, String testRunTitle, String comment, String milestone, String testTargetTitle, String testEnvironmentTitle, String tags, String parameters, IssuesSettings issuesSettings, AdvancedSettings advancedSettings, PublishRobot publishRobot, PublishTap publishTap, ImportTestCases importTestCases) {
        this.projectKey = projectKey;
        this.testRunTitle = testRunTitle;
        this.comment = comment;
        this.milestone = milestone;
        this.testTargetTitle = testTargetTitle;
        this.testEnvironmentTitle = testEnvironmentTitle;
        this.tags = tags;
        this.parameters = parameters;

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

        this.publishRobot = publishRobot;
        if(publishRobot != null) {
            this.robotOutput = publishRobot.getRobotOutput();
            this.robotCatenateParentKeywords = publishRobot.isRobotCatenateParentKeywords();
        }

        this.publishTap = publishTap;
        if(publishTap != null) {
            this.tapFileNameInIdentifier = publishTap.isTapFileNameInIdentifier();
            this.tapTestNumberInIdentifier = publishTap.isTapTestNumberInIdentifier();
            this.tapTestsAsSteps = publishTap.isTapTestsAsSteps();
            this.tapMappingPrefix = publishTap.getTapMappingPrefix();
        }

        this.importTestCases = importTestCases;
        if(importTestCases != null) {
            this.importTestCasesRootCategory = importTestCases.getImportTestCasesRootCategory();
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
        // from the contract of SimpleBuildStep
        return BuildStepMonitor.NONE;
    }

    /**
     * SimpleBuildStep perform().
     *
     * @param run a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param launcher a way to start processes
     * @param listener a place to send output
     * @throws InterruptedException if the step is interrupted
     * @throws IOException if something goes wrong; use AbortException for a polite error
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        doPerform(run, workspace, listener);
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
     * @return boolean deprecated
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return doPerform(build, build.getWorkspace(), listener);
    }

    /**
     * Execute our logic.
     *
     * Should throw AbortException for graceful and polite errors.
     */
    protected boolean doPerform(Run<?, ?> build, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Publishing test results to Testlab project: " + projectKey);

        DescriptorImpl d = getDescriptor();

        log.fine("perform(): " + this + ", descriptor: " + d);

        // get job specific settings if any and fallback to global configuration
        Secret secretKey = apiKey;
        if(secretKey == null || "".equals(secretKey.getPlainText())) {
            // prefer key from global settings if the job has none
            secretKey = d.getApiKey();
        }
        String runApiKey = secretKey != null ? secretKey.getPlainText() : null;
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
        additionalKeys.put("BUILD_STATUS", summary.message != null ? summary.message : "[No build status available]");
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

        String runParameterVariables = vr.replace(parameters);
        Map<String, String> runParameters = null;
        if(runParameterVariables != null && runParameterVariables.trim().length() > 0) {
            String[] pars = runParameterVariables.split(",");
            Map<String, String> vars = vr.getVars();
            for(String par : pars) {
                par = par.trim();
                String value = vars.get(par);
                if(value == null)
                    value = vars.get(par.toUpperCase());
                if(value != null) {
                    if(runParameters == null)
                        runParameters = new HashMap<String, String>();
                    runParameters.put(par, value);
                }
            }
        }

        String runTapMappingPrefix = vr.replace(tapMappingPrefix);

        String abortError = null;
        if(workspace == null) {
            abortError = "The provided build has no workspace.";
        }

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
                workspace,
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
                runParameters,
                issuesSettings != null,
                mergeAsSingleIssue,
                reopenExisting,
                !isBlank(runAssignToUser) ? runAssignToUser : null,
                publishTap != null,
                tapTestsAsSteps,
                tapFileNameInIdentifier,
                tapTestNumberInIdentifier,
                runTapMappingPrefix,
                importTestCases != null,
                importTestCasesRootCategory,
                runTestCaseMappingField,
                publishRobot != null,
                robotOutput,
                robotCatenateParentKeywords,
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
    @Symbol("melioraTestlab")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        // company id of the testlab which to publish to
        private String companyId;
        // testlab api key
        private Secret apiKey;
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
            apiKey = Secret.fromString(json.getString("apiKey"));
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

        public Secret getApiKey() {
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
        private Secret apiKey;

        public Secret getApiKey() {
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

        @DataBoundSetter
        public void setCompanyId(String companyId) {
            this.companyId = companyId;
        }

        @DataBoundSetter
        public void setApiKey(Secret apiKey) {
            this.apiKey = apiKey;
        }

        @DataBoundSetter
        public void setTestCaseMappingField(String testCaseMappingField) {
            this.testCaseMappingField = testCaseMappingField;
        }

        @DataBoundSetter
        public void setUsingonpremise(Usingonpremise usingonpremise) {
            this.usingonpremise = usingonpremise;
        }

        @DataBoundConstructor
        public AdvancedSettings(String companyId, Secret apiKey, String testCaseMappingField, Usingonpremise usingonpremise) {
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

        @DataBoundSetter
        public void setOnpremiseurl(String onpremiseurl) {
            this.onpremiseurl = onpremiseurl;
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

        @DataBoundSetter
        public void setMergeAsSingleIssue(boolean mergeAsSingleIssue) {
            this.mergeAsSingleIssue = mergeAsSingleIssue;
        }

        // if set issues are automatically assigned to this user
        private String assignToUser;

        public String getAssignToUser() {
            return assignToUser;
        }

        @DataBoundSetter
        public void setAssignToUser(String assignToUser) {
            this.assignToUser = assignToUser;
        }

        // if set we try to reopen existing matching issues on push
        private boolean reopenExisting;

        @DataBoundSetter
        public void setReopenExisting(boolean reopenExisting) {
            this.reopenExisting = reopenExisting;
        }

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
     * Optional job config block for TAP support.
     *
     * If set implicitly implies that TAP results should be published to Testlab.
     */
    public static final class PublishTap {
        // If set, each TAP file will be mapped to a single test case in Testlab and the steps of the test case will be overwritten and matched to sent lines in TAP file
        private boolean tapTestsAsSteps;

        // If set, the name of the TAP file containing the tests is included in the mapping identifier as a prefix
        private boolean tapFileNameInIdentifier;

        // If set, the mapping identifier will not include the test number of the TAP test
        private boolean tapTestNumberInIdentifier;

        // If set, mapping identifiers sent will be prefixed with this value
        private String tapMappingPrefix;

        public boolean isTapFileNameInIdentifier() {
            return tapFileNameInIdentifier;
        }

        public boolean isTapTestNumberInIdentifier() {
            return tapTestNumberInIdentifier;
        }

        public boolean isTapTestsAsSteps() {
            return tapTestsAsSteps;
        }

        public String getTapMappingPrefix() {
            return tapMappingPrefix;
        }

        @DataBoundSetter
        public void setTapTestsAsSteps(boolean tapTestsAsSteps) {
            this.tapTestsAsSteps = tapTestsAsSteps;
        }

        @DataBoundSetter
        public void setTapFileNameInIdentifier(boolean tapFileNameInIdentifier) {
            this.tapFileNameInIdentifier = tapFileNameInIdentifier;
        }

        @DataBoundSetter
        public void setTapTestNumberInIdentifier(boolean tapTestNumberInIdentifier) {
            this.tapTestNumberInIdentifier = tapTestNumberInIdentifier;
        }

        @DataBoundSetter
        public void setTapMappingPrefix(String tapMappingPrefix) {
            this.tapMappingPrefix = tapMappingPrefix;
        }

        @DataBoundConstructor
        public PublishTap(boolean tapFileNameInIdentifier, boolean tapTestNumberInIdentifier, boolean tapTestsAsSteps, String tapMappingPrefix) {
            this.tapFileNameInIdentifier = tapFileNameInIdentifier;
            this.tapTestNumberInIdentifier = tapTestNumberInIdentifier;
            this.tapTestsAsSteps = tapTestsAsSteps;
            this.tapMappingPrefix = tapMappingPrefix;
        }

        @Override
        public String toString() {
            return "PublishTap{" +
                    "tapFileNameInIdentifier=" + tapFileNameInIdentifier +
                    ", tapTestsAsSteps=" + tapTestsAsSteps +
                    ", tapTestNumberInIdentifier=" + tapTestNumberInIdentifier +
                    ", tapMappingPrefix=" + tapMappingPrefix +
                    '}';
        }
    }

    /**
     * Optional job config block for Robot Framework support.
     *
     * If set implicitly implies that Robot results should be published to Testlab.
     */
    public static final class PublishRobot {
        // Robot output.xml file path
        private String robotOutput;

        // If set, catenates all sub keywords of a keyword as a single step in result
        private boolean robotCatenateParentKeywords;

        public String getRobotOutput() {
            return robotOutput;
        }

        public boolean isRobotCatenateParentKeywords() {
            return robotCatenateParentKeywords;
        }

        @DataBoundSetter
        public void setRobotOutput(String robotOutput) {
            this.robotOutput = robotOutput;
        }

        @DataBoundSetter
        public void setRobotCatenateParentKeywords(boolean robotCatenateParentKeywords) {
            this.robotCatenateParentKeywords = robotCatenateParentKeywords;
        }

        @DataBoundConstructor
        public PublishRobot(String robotOutput, boolean robotCatenateParentKeywords) {
            this.robotOutput = robotOutput;
            this.robotCatenateParentKeywords = robotCatenateParentKeywords;
        }

        @Override
        public String toString() {
            return "PublishRobot{" +
                    "robotOutput='" + robotOutput + '\'' +
                    ", robotCatenateParentKeywords=" + robotCatenateParentKeywords +
                    '}';
        }
    }

    /**
     * Optional job config block for auto-creating test cases.
     *
     * If set implicitly implies that test cases should be automatically created during the push.
     */
    public static final class ImportTestCases {
        // If set, sets the root category path where the test cases are created. By default, "Import".
        private String importTestCasesRootCategory;

        public String getImportTestCasesRootCategory() {
            return importTestCasesRootCategory;
        }

        @DataBoundSetter
        public void setImportTestCasesRootCategory(String importTestCasesRootCategory) {
            this.importTestCasesRootCategory = importTestCasesRootCategory;
        }

        @DataBoundConstructor
        public ImportTestCases(String importTestCasesRootCategory) {
            this.importTestCasesRootCategory = importTestCasesRootCategory;
        }

        @Override
        public String toString() {
            return "ImportTestCases{" +
                    "importTestCasesRootCategory='" + importTestCasesRootCategory + '\'' +
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
                ", tags='" + tags + '\'' +
                ", parameters='" + parameters + '\'' +
                ", issuesSettings=" + issuesSettings +
                ", mergeAsSingleIssue=" + mergeAsSingleIssue +
                ", assignToUser='" + assignToUser + '\'' +
                ", reopenExisting=" + reopenExisting +
                ", advancedSettings=" + advancedSettings +
                ", companyId='" + companyId + '\'' +
                ", apiKey='hidden'" +
                ", testCaseMappingField='" + testCaseMappingField + '\'' +
                ", usingonpremise=" + usingonpremise +
                ", publishTap=" + publishTap +
                ", importTestCases=" + importTestCases +
                ", publishRobot=" + publishRobot +
                '}';
    }
}

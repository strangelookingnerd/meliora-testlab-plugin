package fi.meliora.testlab.ext.jenkins.test;

import hudson.model.Result;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Jenkins tests for testing meliora testlab plugin as a Pipeline step.
 *
 * mvn -Dtest=PipelineTest -Djava.util.logging.config.file=src/test/resources/logging.properties clean test
 *
 * @author Marko Kanala
 */
public class PipelineTest extends TestBase {
    public static final boolean SANDBOX = false;

    // see https://jenkins.io/doc/developer/testing/

    /**
     * Tests that step fails if (all) required parameters are missing.
     */
    @Test
    public void testPipelineRequiredSettings() throws Exception {
        WorkflowJob pipelineJob = j.jenkins.createProject(WorkflowJob.class, "test-pipeline");
        pipelineJob.setConcurrentBuild(false);

        String testProjectPath = new File(
                getClass().getClassLoader().getResource("pipelinetestproject").toURI()
        ).getAbsolutePath();

        String script = "" +
                "pipeline {\n" +
                "    agent any\n" +
                "    stages {\n" +
                "        stage('Copy testproject assets') {\n" +
                "            steps {\n" +
                "                sh 'cd \"" + testProjectPath + "\" && cp -r * \"$WORKSPACE\"'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    post {\n" +
                "        always {\n" +
                "            junit '**/surefire-reports/*.xml'\n" +
                "            melioraTestlab(\n" +
                "            )\n" +
                "        }\n" +
                "    }\n" +
                "}";
        if(!SANDBOX) {
            ScriptApproval.get().preapprove(script, GroovyLanguage.get());
        }
        pipelineJob.setDefinition(new CpsFlowDefinition(script, SANDBOX));

        WorkflowRun run = pipelineJob.scheduleBuild2(0).get();
        String log = FileUtils.readFileToString(run.getLogFile());

        l(log);

        // should fail with errors for all missing required paameters
        j.assertBuildStatus(Result.FAILURE, run);
        assertContains(log, "Missing required parameter: \"projectKey\"");
    }

    /**
     * Tests that step fails if api key cannot be resolved.
     */
    @Test
    public void testPipelineNoApiKey() throws Exception {
        WorkflowJob pipelineJob = j.jenkins.createProject(WorkflowJob.class, "test-pipeline");
        pipelineJob.setConcurrentBuild(false);

        String testProjectPath = new File(
                getClass().getClassLoader().getResource("pipelinetestproject").toURI()
        ).getAbsolutePath();

        String script = "" +
                "pipeline {\n" +
                "    agent any\n" +
                "    stages {\n" +
                "        stage('Copy testproject assets') {\n" +
                "            steps {\n" +
                "                sh 'cd \"" + testProjectPath + "\" && cp -r * \"$WORKSPACE\"'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    post {\n" +
                "        always {\n" +
                "            junit '**/surefire-reports/*.xml'\n" +
                "            melioraTestlab(\n" +
                "                projectKey: 'TLABDEMO',\n" +
                "                rulesetSettings: [\n" +
                "                    testRunTitle: 'pipelined integration tests'\n" +
                "                ],\n" +
                "                advancedSettings: [" +
                "                    companyId: 'testcompany'," +
//                "                    apiKey: hudson.util.Secret.fromString('reallysecretapikey')," +
                "                    usingonpremise: [" +
                "                        onpremiseurl: 'http://testcompany:8080/'" +
                "                    ]" +
                "                ]" +
                "            )\n" +
                "        }\n" +
                "    }\n" +
                "}";
        if(!SANDBOX) {
            ScriptApproval.get().preapprove(script, GroovyLanguage.get());
        }
        pipelineJob.setDefinition(new CpsFlowDefinition(script, SANDBOX));

        WorkflowRun run = pipelineJob.scheduleBuild2(0).get();
        String log = FileUtils.readFileToString(run.getLogFile());

        l(log);

        j.assertBuildStatus(Result.FAILURE, run);
        assertContains(log, "ERROR: Could not publish results to Testlab: Api Key is not set.");
    }

    /**
     * Tests that step fails if caller identity (companyId) cannot be resolved.
     */
    @Test
    public void testPipelineNoCompanyId() throws Exception {
        WorkflowJob pipelineJob = j.jenkins.createProject(WorkflowJob.class, "test-pipeline");
        pipelineJob.setConcurrentBuild(false);

        String testProjectPath = new File(
                getClass().getClassLoader().getResource("pipelinetestproject").toURI()
        ).getAbsolutePath();

        String script = "" +
                "pipeline {\n" +
                "    agent any\n" +
                "    stages {\n" +
                "        stage('Copy testproject assets') {\n" +
                "            steps {\n" +
                "                sh 'cd \"" + testProjectPath + "\" && cp -r * \"$WORKSPACE\"'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    post {\n" +
                "        always {\n" +
                "            junit '**/surefire-reports/*.xml'\n" +
                "            melioraTestlab(\n" +
                "                projectKey: 'TLABDEMO',\n" +
                "                rulesetSettings: [\n" +
                "                    testRunTitle: 'pipelined integration tests'\n" +
                "                ],\n" +
                "                advancedSettings: [" +
//                "                    companyId: 'testcompany'," +
                "                    apiKey: hudson.util.Secret.fromString('reallysecretapikey')" +
//                "                    usingonpremise: [" +
//                "                        onpremiseurl: 'http://testcompany:8080/'" +
//                "                    ]" +
                "                ]" +
                "            )\n" +
                "        }\n" +
                "    }\n" +
                "}";
        if(!SANDBOX) {
            ScriptApproval.get().preapprove(script, GroovyLanguage.get());
        }
        pipelineJob.setDefinition(new CpsFlowDefinition(script, SANDBOX));

        WorkflowRun run = pipelineJob.scheduleBuild2(0).get();
        String log = FileUtils.readFileToString(run.getLogFile());

        l(log);

        j.assertBuildStatus(Result.FAILURE, run);
        assertContains(log, "ERROR: Could not publish results to Testlab: Company ID is not set.");
    }

    /**
     * Tests that step succeeds without companyId if onpremise url address is set.
     */
    @Test
    public void testPipelineWithOnpremiseUrl() throws Exception {
        WorkflowJob pipelineJob = j.jenkins.createProject(WorkflowJob.class, "test-pipeline");
        pipelineJob.setConcurrentBuild(false);

        String testProjectPath = new File(
                getClass().getClassLoader().getResource("pipelinetestproject").toURI()
        ).getAbsolutePath();

        String script = "" +
                "pipeline {\n" +
                "    agent any\n" +
                "    stages {\n" +
                "        stage('Copy testproject assets') {\n" +
                "            steps {\n" +
                "                sh 'cd \"" + testProjectPath + "\" && cp -r * \"$WORKSPACE\"'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    post {\n" +
                "        always {\n" +
                "            junit '**/surefire-reports/*.xml'\n" +
                "            melioraTestlab(\n" +
                "                projectKey: 'TLABDEMO',\n" +
                "                rulesetSettings: [\n" +
                "                    testRunTitle: 'pipelined integration tests'\n" +
                "                ],\n" +
                "                advancedSettings: [" +
//                "                    companyId: 'testcompany'," +
                "                    apiKey: hudson.util.Secret.fromString('reallysecretapikey')," +
                "                    usingonpremise: [" +
                "                        onpremiseurl: 'http://testcompany:8080/'" +
                "                    ]" +
                "                ]" +
                "            )\n" +
                "        }\n" +
                "    }\n" +
                "}";
        if(!SANDBOX) {
            ScriptApproval.get().preapprove(script, GroovyLanguage.get());
        }
        pipelineJob.setDefinition(new CpsFlowDefinition(script, SANDBOX));

        WorkflowRun run = pipelineJob.scheduleBuild2(0).get();
        String log = FileUtils.readFileToString(run.getLogFile());

        l(log);

        j.assertBuildStatus(Result.UNSTABLE, run);
        assertContains(log, "Publishing test results to Testlab project: TLABDEMO");
    }

    /**
     * Tests that step succeeds with saas-hosted configuration (companyId set, onpremise url not set).
     */
    @Test
    public void testPipelineHosted() throws Exception {
        WorkflowJob pipelineJob = j.jenkins.createProject(WorkflowJob.class, "test-pipeline");
        pipelineJob.setConcurrentBuild(false);

        String testProjectPath = new File(
                getClass().getClassLoader().getResource("pipelinetestproject").toURI()
        ).getAbsolutePath();

        String script = "" +
                "pipeline {\n" +
                "    agent any\n" +
                "    stages {\n" +
                "        stage('Copy testproject assets') {\n" +
                "            steps {\n" +
                "                sh 'cd \"" + testProjectPath + "\" && cp -r * \"$WORKSPACE\"'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    post {\n" +
                "        always {\n" +
                "            junit '**/surefire-reports/*.xml'\n" +
                "            melioraTestlab(\n" +
                "                projectKey: 'TLABDEMO',\n" +
                "                rulesetSettings: [\n" +
                "                    testRunTitle: 'pipelined integration tests'\n" +
                "                ],\n" +
                "                advancedSettings: [" +
                "                    companyId: 'testcompany'," +
                "                    apiKey: hudson.util.Secret.fromString('reallysecretapikey')" +
                "                ]" +
                "            )\n" +
                "        }\n" +
                "    }\n" +
                "}";
        if(!SANDBOX) {
            ScriptApproval.get().preapprove(script, GroovyLanguage.get());
        }
        pipelineJob.setDefinition(new CpsFlowDefinition(script, SANDBOX));

        WorkflowRun run = pipelineJob.scheduleBuild2(0).get();
        String log = FileUtils.readFileToString(run.getLogFile());

        l(log);

        j.assertBuildStatus(Result.UNSTABLE, run);
        assertContains(log, "Publishing test results to Testlab project: TLABDEMO");
    }

    /**
     * Tests that step succeeds with all parameters set.
     */
    @Test
    public void testPipelineMaximalSettings() throws Exception {
        WorkflowJob pipelineJob = j.jenkins.createProject(WorkflowJob.class, "test-pipeline");
        pipelineJob.setConcurrentBuild(false);

        String testProjectPath = new File(
                getClass().getClassLoader().getResource("pipelinetestproject").toURI()
        ).getAbsolutePath();

        String script = "" +
                "pipeline {\n" +
                "    agent any\n" +
                "    stages {\n" +
                "        stage('Copy testproject assets') {\n" +
                "            steps {\n" +
                "                sh 'cd \"" + testProjectPath + "\" && cp -r * \"$WORKSPACE\"'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    post {\n" +
                "        always {\n" +
                "            junit '**/surefire-reports/*.xml'\n" +
                "            melioraTestlab(" +
                "                projectKey: 'TLABDEMO'," +
                "                ruleset: 'default'," +
                "                automationSource: 'pipelinetestsource'," +
                "                rulesetSettings: [" +
                "                    testRunTitle: 'pipelined integration tests'," +
                "                    milestone: 'C'," +
                "                    testTargetTitle: '1.0'," +
                "                    testEnvironmentTitle: 'jenkins-node'," +
                "                    addIssueStrategy: 'ADDPERTESTRUN'," +
                "                    tags: 'jenkins pipeline'," +
                "                    assignToUser: 'agentsmith'," +
                "                    reopenExisting: true," +
                "                    robotCatenateParentKeywords: true" +
                "                ],\n" +
                "                description: 'Jenkins build: ${BUILD_FULL_DISPLAY_NAME} ${BUILD_RESULT}, ${BUILD_URL}'," +
                "                parameters: 'BROWSER'," +
                "                publishTap: [" +
                "                    tapTestsAsSteps: true," +
                "                    tapFileNameInIdentifier: true," +
                "                    tapTestNumberInIdentifier: true," +
                "                    tapMappingPrefix: 'tap'" +
                "                ]," +
                "                publishRobot: [" +
                "                    robotOutput: 'output.xml'" +
                "                ]," +
                "                advancedSettings: [" +
                "                    companyId: 'testcompany'," +
                "                    apiKey: hudson.util.Secret.fromString('reallysecretapikey')" +
                "                ]" +
                "            )\n" +
                "        }\n" +
                "    }\n" +
                "}";
        if(!SANDBOX) {
            ScriptApproval.get().preapprove(script, GroovyLanguage.get());
        }
        pipelineJob.setDefinition(new CpsFlowDefinition(script, SANDBOX));

        WorkflowRun run = pipelineJob.scheduleBuild2(0).get();
        String log = FileUtils.readFileToString(run.getLogFile());

        l(log);

        j.assertBuildStatus(Result.UNSTABLE, run);
        assertContains(log, "Publishing test results to Testlab project: TLABDEMO");
    }

    @Override
    public void setup() throws IOException {
        super.setup();
        //
        // note: skip all actual rest calls to Testlab in these tests
        //
        System.setProperty("TESTLAB_SENDER_SKIP_SEND", "true");
    }

}
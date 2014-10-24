package fi.meliora.testlab.ext.jenkins;

import fi.meliora.testlab.ext.crest.CrestEndpointFactory;
import fi.meliora.testlab.ext.crest.TestResultResource;
import fi.meliora.testlab.ext.rest.model.AddTestResultResponse;
import fi.meliora.testlab.ext.rest.model.TestCaseResult;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends jenkins test results to Testlab.
 *
 * @author Marko Kanala, Meliora Ltd
 */
public class Sender {
    /**
     * Jenkins uses java.util.logging for loggers. Configure Testlab plugin specific
     * log to your Jenkins from Manage Jenkins > System Log and add a new logger
     * for class fi.meliora.testlab.ext.jenkins.Sender with ALL level.
     */
    private final static Logger log = Logger.getLogger(Sender.class.getName());

    static {
        //
        // set crest to prefer slf4j, see http://crest.codegist.org/deeper/logging.html
        //
        System.getProperties().setProperty(
                "org.codegist.common.log.class",
                "org.codegist.common.log.Slf4jLogger"
        );
    }

    /**
     * Does the actual sending of results to Testlab. Called from appropriate Jenkins extension point.
     *
     * @param companyId
     * @param usingonpremise
     * @param onpremiseurl
     * @param apiKey
     * @param projectKey
     * @param milestone
     * @param testRunTitle
     * @param testTargetTitle
     * @param testEnvironmentTitle
     * @param addIssues
     * @param mergeAsSingleIssue
     * @param reopenExisting
     * @param assignToUser
     * @param testCaseMappingField
     * @param build
     */
    public static void sendResults(String companyId, boolean usingonpremise, String onpremiseurl, String apiKey, String projectKey, String milestone, String testRunTitle, String testTargetTitle, String testEnvironmentTitle, boolean addIssues, boolean mergeAsSingleIssue, boolean reopenExisting, String assignToUser, String testCaseMappingField, AbstractBuild<?, ?> build) {
        // no need to validate params here, extension ensures we have some values set

        if(log.isLoggable(Level.FINE))
            log.fine("Running Sender - " + companyId + ", " + usingonpremise + ", " + onpremiseurl + ", api key hidden, " + projectKey + ", " + milestone + ", " + testRunTitle + ", " + testTargetTitle + ", " + testEnvironmentTitle + ", " + addIssues + ", " + mergeAsSingleIssue + ", " + reopenExisting + ", " + assignToUser + ", " + testCaseMappingField);

        // parse test results
        AbstractTestResultAction ra = build.getTestResultAction();

        if(log.isLoggable(Level.FINE))
            log.fine("Have results: " + ra);

        if(ra == null) {
            log.warning("We have no results to publish. Please make sure your job is configured to publish some test results to make them available to this plugin.");
        } else {
            List<TestCaseResult> results = new ArrayList<TestCaseResult>();

            String user = "Jenkins job: " + build.getProject().getDisplayName();

            fi.meliora.testlab.ext.rest.model.TestResult data = new fi.meliora.testlab.ext.rest.model.TestResult();
            data.setStatus(fi.meliora.testlab.ext.rest.model.TestResult.STATUS_FINISHED);
            data.setProjectKey(projectKey);
            data.setTestRunTitle(testRunTitle);
            // note: we send the set milestone in both fields as backend logic tries first with identifier and fallbacks to title
            data.setMilestoneIdentifier(milestone);
            data.setMilestoneTitle(milestone);
            data.setAddIssues(addIssues);
            data.setMergeAsSingleIssue(mergeAsSingleIssue);
            data.setReopenExistingIssues(reopenExisting);
            data.setAssignIssuesToUser(assignToUser);
            data.setTestCaseMappingField(testCaseMappingField);
            data.setUser(user);

            StringBuilder comment = new StringBuilder();
            comment.append("Automated tests from Jenkins, build: ");
            comment.append(build.getFullDisplayName());
            comment.append(", ");
            String jenkinsUrl = Jenkins.getInstance().getRootUrl();
            if(TestlabNotifier.isBlank(jenkinsUrl)) {
                try {
                    jenkinsUrl = Jenkins.getInstance().getRootUrlFromRequest();
                } catch (Exception e) {
                    // note: this fails when run in unit test with JenkinsRule - ignore
                }
            }
            if(TestlabNotifier.isBlank(jenkinsUrl)) {
                // fallback to deprecated absolute url of build
                try {
                    comment.append(build.getAbsoluteUrl());
                } catch (Exception e) {
                    // note: this fails when run in unit test with JenkinsRule - ignore
                }
            } else {
                comment.append(jenkinsUrl);
                comment.append(build.getUrl());
            }

            Run.Summary summary = build.getBuildStatusSummary();
            if(summary != null && !TestlabNotifier.isBlank(summary.message)) {
                if(comment.length() > 0)
                    comment.append("\n\n");
                comment.append(summary.message);
            }
            data.setComment(comment.toString());

            if(!TestlabNotifier.isBlank(testTargetTitle))
                data.setTestTargetTitle(testTargetTitle);

            if(!TestlabNotifier.isBlank(testEnvironmentTitle))
                data.setTestEnvironmentTitle(testEnvironmentTitle);

            if(ra.getResult() instanceof hudson.tasks.test.TestResult) {
                TestResult result = (TestResult)ra.getResult();
                if(log.isLoggable(Level.FINE))
                    log.fine("Result object: " + result + ", " + result.getClass().getName());

                // parse results
                if(result instanceof hudson.tasks.junit.TestResult) {

                    //// junit results

                    if(log.isLoggable(Level.FINE))
                        log.fine("Detected junit compatible result object.");

                    hudson.tasks.junit.TestResult junitResult = (hudson.tasks.junit.TestResult)result;

                    for(SuiteResult sr : junitResult.getSuites()) {
                        for(CaseResult cr : sr.getCases()) {
                            String id = cr.getClassName() + "." + cr.getName();
                            if(log.isLoggable(Level.FINE))
                                log.fine("Status for " + id + " is " + cr.getStatus());
                            int res;
                            if(cr.isPassed())
                                res = TestCaseResult.RESULT_PASS;
                            else if(cr.isSkipped())
                                res = TestCaseResult.RESULT_SKIP;
                            else
                                res = TestCaseResult.RESULT_FAIL;

                            String msg = cr.getErrorDetails();
                            String stacktrace = cr.getErrorStackTrace();

                            results.add(getTestCaseResult(build, id, res, msg, stacktrace, user, cr.getDuration()));
                        }
                    }
                } else {

                    //// a generic test result, try to parse it
                    //
                    // this should work for example with testng harness
                    // see https://github.com/jenkinsci/testng-plugin-plugin/blob/master/src/main/java/hudson/plugins/testng/results/MethodResult.java

                    if(log.isLoggable(Level.FINE))
                        log.fine("Detected generic result object.");

                    for(TestResult tr : result.getPassedTests()) {
                        String id = tr.getParent() != null ? tr.getParent().getName() + "." + tr.getName() : tr.getName();
                        results.add(getTestCaseResult(build, id, TestCaseResult.RESULT_PASS, tr.getErrorDetails(), tr.getErrorStackTrace(), user, tr.getDuration()));
                    }
                    for(TestResult tr : result.getFailedTests()) {
                        String id = tr.getParent() != null ? tr.getParent().getName() + "." + tr.getName() : tr.getName();
                        results.add(getTestCaseResult(build, id, TestCaseResult.RESULT_FAIL, tr.getErrorDetails(), tr.getErrorStackTrace(), user, tr.getDuration()));
                    }
                    for(TestResult tr : result.getSkippedTests()) {
                        String id = tr.getParent() != null ? tr.getParent().getName() + "." + tr.getName() : tr.getName();
                        results.add(getTestCaseResult(build, id, TestCaseResult.RESULT_SKIP, tr.getErrorDetails(), tr.getErrorStackTrace(), user, tr.getDuration()));
                    }
                }
            }

            if(results.size() > 0) {
                if(log.isLoggable(Level.FINE))
                    log.fine("Sending " + results.size() + " test results to Testlab.");
                data.setResults(results);

                // send results to testlab
                String onpremiseUrl = usingonpremise ? onpremiseurl : null;
                AddTestResultResponse response = CrestEndpointFactory.getInstance().getTestlabEndpoint(
                        companyId, onpremiseUrl, apiKey, TestResultResource.class
                ).addTestResult(data);

                if(log.isLoggable(Level.INFO))
                    log.info("Posted results successfully to testlab test run: " + response.getTestRunId());
            } else {
                if(log.isLoggable(Level.INFO))
                    log.info("No test results to send to Testlab. Skipping.");
            }
        }
    }

    protected static TestCaseResult getTestCaseResult(AbstractBuild<?, ?> build, String id, int result, String msg, String stacktrace, String user, float duration) {
        TestCaseResult r = new TestCaseResult();
        r.setMappingId(id);
        r.setResult(result);
        long started = build.getTimeInMillis();
        r.setStarted(started);
        r.setRun(started + (long)(duration * 1000));        // duration as float in seconds
        r.setRunBy(user);
        if(msg != null || stacktrace != null) {
            StringBuilder comment = new StringBuilder();
            if(!TestlabNotifier.isBlank(msg)) {
                comment.append(msg);
            }
            if(!TestlabNotifier.isBlank(stacktrace)) {
                if(comment.length() > 0)
                    comment.append("\n\n");
                comment.append(stacktrace);
            }
            r.setComment(comment.toString());
        }
        return r;
    }

}

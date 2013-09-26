package fi.meliora.testlab.ext.jenkins.test;

import fi.meliora.testlab.ext.jenkins.TestlabNotifier;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Maven;
import hudson.tasks.Shell;
import hudson.tasks.junit.JUnitResultArchiver;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Jenkins tests for running a dummy plugin run for a test project.
 *
 * @author Marko Kanala, Meliora Ltd
 */
public class SenderTest extends TestBase {

    public static final String TESTPROJECT_INSTALL_SCRIPT =
            "cd {0}\n" +
            "cp -r * $WORKSPACE";

    /**
     * <ul>
     *     <li>creates a new job</li>
     *     <li>copies testproject/ codebase to job workspace</li>
     *     <li>configures a maven build, junit publish and testlab publishes to the job</li>
     *     <li>builds the build which fails with results and publishes the results to testlab specified with parameters (see below)</li>
     *     <li>asserts that build console log contains a note about successful publish</li>
     * </ul>
     *
     * This test is skipped if COMPANYID, APIKEY or TESTLABAPI environment parameters are missing.
     *
     * @throws Exception
     */
    @Test
    public void testSender() throws Exception {
        // check if we have proper vars to run this test
        String TEST_COMPANYID = System.getProperty("COMPANYID");
        String TEST_APIKEY = System.getProperty("APIKEY");
        String TEST_TESTLABAPI = System.getProperty("TESTLABAPI");
        if(TEST_COMPANYID == null || TEST_APIKEY == null || TEST_TESTLABAPI == null) {
            System.out.println("Skipping test " + getClass() + " as we have no enough params to run this test.");
            return;
        }

        System.setProperty("TESTLAB_" + TEST_COMPANYID.toUpperCase(), TEST_TESTLABAPI);

        //// setup a new project and build it

        FreeStyleProject p = j.createFreeStyleProject("test");

        // add a Shell builder which copies the testproject to the workspace
        // TODO: fix platform dependency in this test
        String testProjectPath = new File(getClass().getClassLoader().getResource("testproject").toURI()).getAbsolutePath();
        String script = TESTPROJECT_INSTALL_SCRIPT.replace("{0}", testProjectPath);
        p.getBuildersList().add(new Shell(script));

        // add a maven build for our test project
        Maven maven = new Maven("clean test", "maven");
        p.getBuildersList().add(maven);

        // register publish junit tests post build action
        p.getPublishersList().add(new JUnitResultArchiver("**/surefire-reports/*.xml", false, null));

        // register our plugin
        p.getPublishersList().add(
                new TestlabNotifier(
                        "ATM",
                        "Jenkins plugin unit test run",
                        "unit test version",
                        "unit test jenkins",
                        null,
                        new TestlabNotifier.AdvancedSettings(TEST_COMPANYID, TEST_APIKEY, "Test class")
                )
        );

        // launch build

        FreeStyleBuild build = p.scheduleBuild2(0).get();

        // build completed

        String log = FileUtils.readFileToString(build.getLogFile());

        System.out.println("log is:\n\n" + log + "\n\n");

        assertTrue(
                "log file did not report a successful publish.",
                log.contains("Publishing test results to Testlab project: ATM\nFinished: FAILURE")
        );
    }

}

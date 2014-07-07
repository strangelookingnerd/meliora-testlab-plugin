package fi.meliora.testlab.ext.jenkins.test;

import com.gargoylesoftware.htmlunit.html.*;
import fi.meliora.testlab.ext.jenkins.TestlabNotifier;
import hudson.model.FreeStyleProject;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Jenkins tests for testing meliora testlab plugin's settings.
 *
 * @author Marko Kanala, Meliora Ltd
 */
public class SettingsTest extends TestBase {

    /**
     * <ul>
     *     <li>Loads up the /configure page</li>
     *     <li>asserts that we have our configuration fields available and they are empty</li>
     *     <li>sets some values and saves the configuration</li>
     *     <li>loads up the /configure page</li>
     *     <li>asserts that the values have been saved</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testGlobalSettings() throws Exception {
        JenkinsRule.WebClient client = getWebClient();
        client.login("admin", "admin");

        HtmlPage configureSystemPage = client.goTo("configure");
        HtmlForm form = configureSystemPage.getFormByName("config");

        HtmlTextInput companyIdInput = form.getInputByName(FIELD_COMPANYID);
        HtmlCheckBoxInput usingonpremiseInput = form.getInputByName(FIELD_USINGONPREMISE);
        HtmlTextInput onpremiseurlInput = form.getInputByName(FIELD_ONPREMISEURL);
        HtmlTextInput apiKeyInput = form.getInputByName(FIELD_APIKEY);
        HtmlTextInput testCaseMappingFieldInput = form.getInputByName(FIELD_TESTCASEMAPPINGFIELD);
        HtmlCheckBoxInput corsInput = form.getInputByName(FIELD_CORS);
        HtmlTextInput originsInput = form.getInputByName(FIELD_ORIGIN);

        assertEmpty(companyIdInput);
        assertChecked(usingonpremiseInput, false);
        assertEmpty(onpremiseurlInput);
        assertEmpty(apiKeyInput);
        assertEmpty(testCaseMappingFieldInput);
        assertChecked(corsInput, false);
        assertHasValue(originsInput, "*");

        companyIdInput.setValueAttribute("unittestcompany");
        usingonpremiseInput.setChecked(true);
        onpremiseurlInput.setValueAttribute("https://unittesthost:8080");
        apiKeyInput.setValueAttribute("1010101010202020");
        testCaseMappingFieldInput.setValueAttribute("Some field");
        corsInput.setChecked(true);
        originsInput.setValueAttribute("http://somehost, http://anotherhost");

        j.submit(form);

        configureSystemPage = client.goTo("configure");
        form = configureSystemPage.getFormByName("config");

        companyIdInput = form.getInputByName(FIELD_COMPANYID);
        usingonpremiseInput = form.getInputByName(FIELD_USINGONPREMISE);
        onpremiseurlInput = form.getInputByName(FIELD_ONPREMISEURL);
        apiKeyInput = form.getInputByName(FIELD_APIKEY);
        testCaseMappingFieldInput = form.getInputByName(FIELD_TESTCASEMAPPINGFIELD);
        corsInput = form.getInputByName(FIELD_CORS);
        originsInput = form.getInputByName(FIELD_ORIGIN);

        assertHasValue(companyIdInput, "unittestcompany");
        assertChecked(usingonpremiseInput, true);
        assertHasValue(onpremiseurlInput, "https://unittesthost:8080");
        assertHasValue(apiKeyInput, "1010101010202020");
        assertHasValue(testCaseMappingFieldInput, "Some field");
        assertChecked(corsInput, true);
        assertHasValue(originsInput, "http://somehost, http://anotherhost");
    }

    /**
     * <ul>
     *     <li>adds a new free style project</li>
     *     <li>adds our plugin to the project as post-build action</li>
     *     <li>loads up the job configuration page</li>
     *     <li>asserts that we have our configuration fields available for the job and they are empty</li>
     *     <li>sets only required fields</li>
     *     <li>loads up the job configuration page</li>
     *     <li>asserts that the values have been saved</li>
     *     <li>sets add issues block fields</li>
     *     <li>loads up the job configuration page</li>
     *     <li>asserts that the values have been saved</li>
     *     <li>sets advanced settings block fields</li>
     *     <li>loads up the job configuration page</li>
     *     <li>asserts that the values have been saved</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testJobSettings() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("test");
        p.getPublishersList().add(new TestlabNotifier(null, null, null, null, null, null));

        JenkinsRule.WebClient client = getWebClient();
        client.login("admin", "admin");

        HtmlPage configurePage = client.goTo("job/test/configure");

        HtmlForm form = configurePage.getFormByName("config");

        HtmlTextInput projectkeyInput = form.getInputByName(FIELD_PROJECTKEY);
        HtmlTextInput testRunTitleInput = form.getInputByName(FIELD_TESTRUNTITLE);
        HtmlTextInput testTargetTitleInput = form.getInputByName(FIELD_TESTTARGETTITLE);
        HtmlTextInput testEnvironmentTitleInput = form.getInputByName(FIELD_TESTENVIRONMENTTITLE);
        HtmlCheckBoxInput issuesSettingsInput = form.getInputByName(FIELD_BLOCK_ISSUESSETTINGS);
        HtmlCheckBoxInput mergeAsSingleIssueInput = form.getInputByName(FIELD_MERGEASSINGLEISSUE);
        HtmlCheckBoxInput reopenExistingInput = form.getInputByName(FIELD_REOPENEXISTING);
        HtmlTextInput assignToUserInput = form.getInputByName(FIELD_ASSIGNTOUSER);
        HtmlCheckBoxInput advancedSettingsInput = form.getInputByName(FIELD_BLOCK_ADVANCEDSETTINGS);
        HtmlTextInput companyIdInput = form.getInputByName(FIELD_COMPANYID);
        HtmlCheckBoxInput usingonpremiseInput = form.getInputByName(FIELD_USINGONPREMISE);
        HtmlTextInput onpremiseurlInput = form.getInputByName(FIELD_ONPREMISEURL);
        HtmlTextInput apiKeyInput = form.getInputByName(FIELD_APIKEY);
        HtmlTextInput testCaseMappingFieldInput = form.getInputByName(FIELD_TESTCASEMAPPINGFIELD);

        assertEmpty(projectkeyInput);
        assertEmpty(testRunTitleInput);
        assertEmpty(testTargetTitleInput);
        assertEmpty(testEnvironmentTitleInput);

        assertChecked(issuesSettingsInput, false);
        assertChecked(mergeAsSingleIssueInput, true);       // defaults to true
        assertChecked(reopenExistingInput, false);
        assertEmpty(assignToUserInput);

        assertChecked(advancedSettingsInput, false);
        assertEmpty(companyIdInput);
        assertChecked(usingonpremiseInput, false);
        assertEmpty(onpremiseurlInput);
        assertEmpty(apiKeyInput);
        assertEmpty(testCaseMappingFieldInput);

        //// set only required fields and assert save

        projectkeyInput.setValueAttribute("PROJ");
        testRunTitleInput.setValueAttribute("Test run");
        j.submit(form);
        configurePage = client.goTo("job/test/configure");
        form = configurePage.getFormByName("config");

        projectkeyInput = form.getInputByName(FIELD_PROJECTKEY);
        testRunTitleInput = form.getInputByName(FIELD_TESTRUNTITLE);
        testTargetTitleInput = form.getInputByName(FIELD_TESTTARGETTITLE);
        testEnvironmentTitleInput = form.getInputByName(FIELD_TESTENVIRONMENTTITLE);
        issuesSettingsInput = form.getInputByName(FIELD_BLOCK_ISSUESSETTINGS);
        mergeAsSingleIssueInput = form.getInputByName(FIELD_MERGEASSINGLEISSUE);
        reopenExistingInput = form.getInputByName(FIELD_REOPENEXISTING);
        assignToUserInput = form.getInputByName(FIELD_ASSIGNTOUSER);
        advancedSettingsInput = form.getInputByName(FIELD_BLOCK_ADVANCEDSETTINGS);
        companyIdInput = form.getInputByName(FIELD_COMPANYID);
        usingonpremiseInput = form.getInputByName(FIELD_USINGONPREMISE);
        onpremiseurlInput = form.getInputByName(FIELD_ONPREMISEURL);
        apiKeyInput = form.getInputByName(FIELD_APIKEY);
        testCaseMappingFieldInput = form.getInputByName(FIELD_TESTCASEMAPPINGFIELD);

        assertHasValue(projectkeyInput, "PROJ");
        assertHasValue(testRunTitleInput, "Test run");
        assertEmpty(testTargetTitleInput);
        assertEmpty(testEnvironmentTitleInput);

        assertChecked(issuesSettingsInput, false);
        assertChecked(mergeAsSingleIssueInput, true);       // defaults to true
        assertChecked(reopenExistingInput, false);
        assertEmpty(assignToUserInput);

        assertChecked(advancedSettingsInput, false);
        assertEmpty(companyIdInput);
        assertChecked(usingonpremiseInput, false);
        assertEmpty(onpremiseurlInput);
        assertEmpty(apiKeyInput);
        assertEmpty(testCaseMappingFieldInput);

        //// set other optional fields and issues block and assert save

        testTargetTitleInput.setValueAttribute("Some version");
        testEnvironmentTitleInput.setValueAttribute("Some env");
        issuesSettingsInput.setChecked(true);
        mergeAsSingleIssueInput.setChecked(false);
        reopenExistingInput.setChecked(true);
        assignToUserInput.setValueAttribute("someuser");
        j.submit(form);
        configurePage = client.goTo("job/test/configure");
        form = configurePage.getFormByName("config");

        projectkeyInput = form.getInputByName(FIELD_PROJECTKEY);
        testRunTitleInput = form.getInputByName(FIELD_TESTRUNTITLE);
        testTargetTitleInput = form.getInputByName(FIELD_TESTTARGETTITLE);
        testEnvironmentTitleInput = form.getInputByName(FIELD_TESTENVIRONMENTTITLE);
        issuesSettingsInput = form.getInputByName(FIELD_BLOCK_ISSUESSETTINGS);
        mergeAsSingleIssueInput = form.getInputByName(FIELD_MERGEASSINGLEISSUE);
        reopenExistingInput = form.getInputByName(FIELD_REOPENEXISTING);
        assignToUserInput = form.getInputByName(FIELD_ASSIGNTOUSER);
        advancedSettingsInput = form.getInputByName(FIELD_BLOCK_ADVANCEDSETTINGS);
        companyIdInput = form.getInputByName(FIELD_COMPANYID);
        usingonpremiseInput = form.getInputByName(FIELD_USINGONPREMISE);
        onpremiseurlInput = form.getInputByName(FIELD_ONPREMISEURL);
        apiKeyInput = form.getInputByName(FIELD_APIKEY);
        testCaseMappingFieldInput = form.getInputByName(FIELD_TESTCASEMAPPINGFIELD);

        assertHasValue(projectkeyInput, "PROJ");
        assertHasValue(testRunTitleInput, "Test run");
        assertHasValue(testTargetTitleInput, "Some version");
        assertHasValue(testEnvironmentTitleInput, "Some env");

        assertChecked(issuesSettingsInput, true);
        assertChecked(mergeAsSingleIssueInput, false);
        assertChecked(reopenExistingInput, true);
        assertHasValue(assignToUserInput, "someuser");

        assertChecked(advancedSettingsInput, false);
        assertEmpty(companyIdInput);
        assertEmpty(apiKeyInput);
        assertEmpty(testCaseMappingFieldInput);

        //// set advanced setting fields and assert save

        advancedSettingsInput.setChecked(true);
        companyIdInput.setValueAttribute("unittestcompanyjob");
        usingonpremiseInput.setChecked(true);
        onpremiseurlInput.setValueAttribute("https://unittesthost:8080");
        apiKeyInput.setValueAttribute("1010101010303030");
        testCaseMappingFieldInput.setValueAttribute("Other field");
        j.submit(form);
        configurePage = client.goTo("job/test/configure");
        form = configurePage.getFormByName("config");

        projectkeyInput = form.getInputByName(FIELD_PROJECTKEY);
        testRunTitleInput = form.getInputByName(FIELD_TESTRUNTITLE);
        testTargetTitleInput = form.getInputByName(FIELD_TESTTARGETTITLE);
        testEnvironmentTitleInput = form.getInputByName(FIELD_TESTENVIRONMENTTITLE);
        issuesSettingsInput = form.getInputByName(FIELD_BLOCK_ISSUESSETTINGS);
        mergeAsSingleIssueInput = form.getInputByName(FIELD_MERGEASSINGLEISSUE);
        reopenExistingInput = form.getInputByName(FIELD_REOPENEXISTING);
        assignToUserInput = form.getInputByName(FIELD_ASSIGNTOUSER);
        advancedSettingsInput = form.getInputByName(FIELD_BLOCK_ADVANCEDSETTINGS);
        companyIdInput = form.getInputByName(FIELD_COMPANYID);
        usingonpremiseInput = form.getInputByName(FIELD_USINGONPREMISE);
        onpremiseurlInput = form.getInputByName(FIELD_ONPREMISEURL);
        apiKeyInput = form.getInputByName(FIELD_APIKEY);
        testCaseMappingFieldInput = form.getInputByName(FIELD_TESTCASEMAPPINGFIELD);

        assertHasValue(projectkeyInput, "PROJ");
        assertHasValue(testRunTitleInput, "Test run");
        assertHasValue(testTargetTitleInput, "Some version");
        assertHasValue(testEnvironmentTitleInput, "Some env");

        assertChecked(issuesSettingsInput, true);
        assertChecked(mergeAsSingleIssueInput, false);
        assertChecked(reopenExistingInput, true);
        assertHasValue(assignToUserInput, "someuser");

        assertChecked(advancedSettingsInput, true);
        assertHasValue(companyIdInput, "unittestcompanyjob");
        assertChecked(usingonpremiseInput, true);
        assertHasValue(onpremiseurlInput, "https://unittesthost:8080");
        assertHasValue(apiKeyInput, "1010101010303030");
        assertHasValue(testCaseMappingFieldInput, "Other field");
    }

}

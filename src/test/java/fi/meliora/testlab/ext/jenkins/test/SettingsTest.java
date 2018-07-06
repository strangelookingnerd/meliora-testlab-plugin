package fi.meliora.testlab.ext.jenkins.test;

import com.gargoylesoftware.htmlunit.html.*;
import fi.meliora.testlab.ext.jenkins.TestlabNotifier;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Jenkins tests for testing meliora testlab plugin's settings.
 *
 * @author Meliora Ltd
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
        HtmlPasswordInput apiKeyInput = form.getInputByName(FIELD_APIKEY);
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
        assertHasValue(apiKeyInput, Secret.fromString("1010101010202020").getEncryptedValue());
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
        p.getPublishersList().add(new TestlabNotifier(null, null, null, null, null, null, null, null, null, null, null, null, null));

        JenkinsRule.WebClient client = getWebClient();
        client.login("admin", "admin");

        HtmlPage configurePage = client.goTo("job/test/configure");

        HtmlForm form = configurePage.getFormByName("config");

        HtmlTextInput projectkeyInput = form.getInputByName(FIELD_PROJECTKEY);
        HtmlTextInput testRunTitleInput = form.getInputByName(FIELD_TESTRUNTITLE);
        HtmlTextInput milestoneInput = form.getInputByName(FIELD_MILESTONE);
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
        HtmlPasswordInput apiKeyInput = form.getInputByName(FIELD_APIKEY);
        HtmlTextInput testCaseMappingFieldInput = form.getInputByName(FIELD_TESTCASEMAPPINGFIELD);
        HtmlTextArea commentInput = form.getTextAreaByName(FIELD_COMMENT);
        HtmlTextInput tagsInput = form.getInputByName(FIELD_TAGS);
        HtmlTextInput parametersInput = form.getInputByName(FIELD_PARAMETERS);
        HtmlCheckBoxInput publishTapInput = form.getInputByName(FIELD_BLOCK_PUBLISHTAP);
        HtmlCheckBoxInput tapTestsAsStepsInput = form.getInputByName(FIELD_TAPTESTSASSTEPS);
        HtmlCheckBoxInput tapFileNameInIdentifier = form.getInputByName(FIELD_TAPFILENAMEINIDENTIFIER);
        HtmlCheckBoxInput tapTestNumberInIdentifier = form.getInputByName(FIELD_TAPTESTNUMBERINIDENTIFIER);
        HtmlTextInput tapMappingPrefixInput = form.getInputByName(FIELD_TAPMAPPINGPREFIX);
        HtmlCheckBoxInput importTestCasesInput = form.getInputByName(FIELD_BLOCK_IMPORTTESTCASES);
        HtmlTextInput importTestCasesRootCategoryInput = form.getInputByName(FIELD_IMPORTTESTCASESROOTCATEGORY);
        HtmlCheckBoxInput publishRobotInput = form.getInputByName(FIELD_BLOCK_PUBLISHROBOT);
        HtmlTextInput robotOutputInput = form.getInputByName(FIELD_ROBOTOUTPUT);
        HtmlCheckBoxInput robotCatenateParentKeywordsInput = form.getInputByName(FIELD_ROBOTCATENATEPARENTKEYWORDS);

        assertEmpty(projectkeyInput);
        assertEmpty(testRunTitleInput);
        assertEmpty(milestoneInput);
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

        assertHasValue(commentInput, TestlabNotifier.DEFAULT_COMMENT_TEMPLATE);
        assertEmpty(tagsInput);

        assertEmpty(parametersInput);

        assertChecked(publishTapInput, false);
        assertChecked(tapTestsAsStepsInput, false);
        assertChecked(tapFileNameInIdentifier, true);       // defaults to true
        assertChecked(tapTestNumberInIdentifier, false);
        assertEmpty(tapMappingPrefixInput);
        assertChecked(importTestCasesInput, false);
        assertEmpty(importTestCasesRootCategoryInput);

        assertChecked(publishRobotInput, false);
        assertHasValue(robotOutputInput, "**/output.xml");
        assertChecked(robotCatenateParentKeywordsInput, true);

        //// set only required fields and assert save

        projectkeyInput.setValueAttribute("PROJ");
        testRunTitleInput.setValueAttribute("Test run");
        j.submit(form);
        configurePage = client.goTo("job/test/configure");
        form = configurePage.getFormByName("config");

        projectkeyInput = form.getInputByName(FIELD_PROJECTKEY);
        testRunTitleInput = form.getInputByName(FIELD_TESTRUNTITLE);
        milestoneInput = form.getInputByName(FIELD_MILESTONE);
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
        commentInput = form.getTextAreaByName(FIELD_COMMENT);
        tagsInput = form.getInputByName(FIELD_TAGS);
        parametersInput = form.getInputByName(FIELD_PARAMETERS);
        publishTapInput = form.getInputByName(FIELD_BLOCK_PUBLISHTAP);
        tapTestsAsStepsInput = form.getInputByName(FIELD_TAPTESTSASSTEPS);
        tapFileNameInIdentifier = form.getInputByName(FIELD_TAPFILENAMEINIDENTIFIER);
        tapTestNumberInIdentifier = form.getInputByName(FIELD_TAPTESTNUMBERINIDENTIFIER);
        tapMappingPrefixInput = form.getInputByName(FIELD_TAPMAPPINGPREFIX);
        importTestCasesInput = form.getInputByName(FIELD_BLOCK_IMPORTTESTCASES);
        importTestCasesRootCategoryInput = form.getInputByName(FIELD_IMPORTTESTCASESROOTCATEGORY);
        publishRobotInput = form.getInputByName(FIELD_BLOCK_PUBLISHROBOT);
        robotOutputInput = form.getInputByName(FIELD_ROBOTOUTPUT);
        robotCatenateParentKeywordsInput = form.getInputByName(FIELD_ROBOTCATENATEPARENTKEYWORDS);

        assertHasValue(projectkeyInput, "PROJ");
        assertHasValue(testRunTitleInput, "Test run");
        assertEmpty(milestoneInput);
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

        assertHasValue(commentInput, TestlabNotifier.DEFAULT_COMMENT_TEMPLATE);
        assertEmpty(tagsInput);

        assertEmpty(parametersInput);

        assertChecked(publishTapInput, false);
        assertChecked(tapTestsAsStepsInput, false);
        assertChecked(tapFileNameInIdentifier, true);       // defaults to true
        assertChecked(tapTestNumberInIdentifier, false);
        assertEmpty(tapMappingPrefixInput);
        assertChecked(importTestCasesInput, false);
        assertEmpty(importTestCasesRootCategoryInput);

        assertChecked(publishRobotInput, false);
        assertHasValue(robotOutputInput, "**/output.xml");
        assertChecked(robotCatenateParentKeywordsInput, true);

        //// set other optional fields and issues block and assert save

        milestoneInput.setValueAttribute("Milestone 1");
        testTargetTitleInput.setValueAttribute("Some version");
        testEnvironmentTitleInput.setValueAttribute("Some env");
        issuesSettingsInput.setChecked(true);
        mergeAsSingleIssueInput.setChecked(false);
        reopenExistingInput.setChecked(true);
        assignToUserInput.setValueAttribute("someuser");
        commentInput.setText("comment text");
        tagsInput.setValueAttribute("jenkins tags");
        parametersInput.setValueAttribute("var1, var2");
        publishTapInput.setChecked(true);
        tapTestsAsStepsInput.setChecked(true);
        tapFileNameInIdentifier.setChecked(false);
        tapTestNumberInIdentifier.setChecked(true);
        tapMappingPrefixInput.setValueAttribute("PREF");
        importTestCasesInput.setChecked(true);
        importTestCasesRootCategoryInput.setValueAttribute("RC");
        publishRobotInput.setChecked(true);
        robotOutputInput.setValueAttribute("results/output2.xml");
        robotCatenateParentKeywordsInput.setChecked(false);

        j.submit(form);
        configurePage = client.goTo("job/test/configure");
        form = configurePage.getFormByName("config");

        projectkeyInput = form.getInputByName(FIELD_PROJECTKEY);
        testRunTitleInput = form.getInputByName(FIELD_TESTRUNTITLE);
        milestoneInput = form.getInputByName(FIELD_MILESTONE);
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
        commentInput = form.getTextAreaByName(FIELD_COMMENT);
        tagsInput = form.getInputByName(FIELD_TAGS);
        parametersInput = form.getInputByName(FIELD_PARAMETERS);
        publishTapInput = form.getInputByName(FIELD_BLOCK_PUBLISHTAP);
        tapTestsAsStepsInput = form.getInputByName(FIELD_TAPTESTSASSTEPS);
        tapFileNameInIdentifier = form.getInputByName(FIELD_TAPFILENAMEINIDENTIFIER);
        tapTestNumberInIdentifier = form.getInputByName(FIELD_TAPTESTNUMBERINIDENTIFIER);
        tapMappingPrefixInput = form.getInputByName(FIELD_TAPMAPPINGPREFIX);
        importTestCasesInput = form.getInputByName(FIELD_BLOCK_IMPORTTESTCASES);
        importTestCasesRootCategoryInput = form.getInputByName(FIELD_IMPORTTESTCASESROOTCATEGORY);
        publishRobotInput = form.getInputByName(FIELD_BLOCK_PUBLISHROBOT);
        robotOutputInput = form.getInputByName(FIELD_ROBOTOUTPUT);
        robotCatenateParentKeywordsInput = form.getInputByName(FIELD_ROBOTCATENATEPARENTKEYWORDS);

        assertHasValue(projectkeyInput, "PROJ");
        assertHasValue(testRunTitleInput, "Test run");
        assertHasValue(milestoneInput, "Milestone 1");
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

        assertHasValue(commentInput, "comment text");
        assertHasValue(tagsInput, "jenkins tags");

        assertHasValue(parametersInput, "var1, var2");

        assertChecked(publishTapInput, true);
        assertChecked(tapTestsAsStepsInput, true);
        assertChecked(tapFileNameInIdentifier, false);
        assertChecked(tapTestNumberInIdentifier, true);
        assertHasValue(tapMappingPrefixInput, "PREF");
        assertChecked(importTestCasesInput, true);
        assertHasValue(importTestCasesRootCategoryInput, "RC");

        assertChecked(publishRobotInput, true);
        assertHasValue(robotOutputInput, "results/output2.xml");
        assertChecked(robotCatenateParentKeywordsInput, false);

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
        milestoneInput = form.getInputByName(FIELD_MILESTONE);
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
        commentInput = form.getTextAreaByName(FIELD_COMMENT);
        tagsInput = form.getInputByName(FIELD_TAGS);
        parametersInput = form.getInputByName(FIELD_PARAMETERS);
        publishTapInput = form.getInputByName(FIELD_BLOCK_PUBLISHTAP);
        tapTestsAsStepsInput = form.getInputByName(FIELD_TAPTESTSASSTEPS);
        tapFileNameInIdentifier = form.getInputByName(FIELD_TAPFILENAMEINIDENTIFIER);
        tapTestNumberInIdentifier = form.getInputByName(FIELD_TAPTESTNUMBERINIDENTIFIER);
        tapMappingPrefixInput = form.getInputByName(FIELD_TAPMAPPINGPREFIX);
        importTestCasesInput = form.getInputByName(FIELD_BLOCK_IMPORTTESTCASES);
        importTestCasesRootCategoryInput = form.getInputByName(FIELD_IMPORTTESTCASESROOTCATEGORY);
        publishRobotInput = form.getInputByName(FIELD_BLOCK_PUBLISHROBOT);
        robotOutputInput = form.getInputByName(FIELD_ROBOTOUTPUT);
        robotCatenateParentKeywordsInput = form.getInputByName(FIELD_ROBOTCATENATEPARENTKEYWORDS);

        assertHasValue(projectkeyInput, "PROJ");
        assertHasValue(testRunTitleInput, "Test run");
        assertHasValue(milestoneInput, "Milestone 1");
        assertHasValue(testTargetTitleInput, "Some version");
        assertHasValue(testEnvironmentTitleInput, "Some env");

        assertChecked(issuesSettingsInput, true);
        assertChecked(mergeAsSingleIssueInput, false);
        assertChecked(reopenExistingInput, true);
        assertHasValue(assignToUserInput, "someuser");

        assertHasValue(commentInput, "comment text");
        assertHasValue(tagsInput, "jenkins tags");

        assertHasValue(parametersInput, "var1, var2");

        assertChecked(advancedSettingsInput, true);
        assertHasValue(companyIdInput, "unittestcompanyjob");
        assertChecked(usingonpremiseInput, true);
        assertHasValue(onpremiseurlInput, "https://unittesthost:8080");
        assertHasValue(apiKeyInput, Secret.fromString("1010101010303030").getEncryptedValue());
        assertHasValue(testCaseMappingFieldInput, "Other field");

        assertChecked(publishTapInput, true);
        assertChecked(tapTestsAsStepsInput, true);
        assertChecked(tapFileNameInIdentifier, false);
        assertChecked(tapTestNumberInIdentifier, true);
        assertHasValue(tapMappingPrefixInput, "PREF");
        assertChecked(importTestCasesInput, true);
        assertHasValue(importTestCasesRootCategoryInput, "RC");

        assertChecked(publishRobotInput, true);
        assertHasValue(robotOutputInput, "results/output2.xml");
        assertChecked(robotCatenateParentKeywordsInput, false);
    }

}

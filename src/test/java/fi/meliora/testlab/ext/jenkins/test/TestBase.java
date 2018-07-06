package fi.meliora.testlab.ext.jenkins.test;

import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Base class for all unit tests.
 *
 * @author Meliora Ltd
 */
public class TestBase {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    public static final String FIELD_PROJECTKEY = "_.projectKey";
    public static final String FIELD_TESTRUNTITLE = "_.testRunTitle";
    public static final String FIELD_MILESTONE = "_.milestone";
    public static final String FIELD_TESTTARGETTITLE = "_.testTargetTitle";
    public static final String FIELD_TESTENVIRONMENTTITLE = "_.testEnvironmentTitle";

    public static final String FIELD_BLOCK_ISSUESSETTINGS = "_.issuesSettings";
    public static final String FIELD_REOPENEXISTING = "_.reopenExisting";
    public static final String FIELD_MERGEASSINGLEISSUE = "_.mergeAsSingleIssue";
    public static final String FIELD_ASSIGNTOUSER = "_.assignToUser";

    public static final String FIELD_BLOCK_ADVANCEDSETTINGS = "_.advancedSettings";
    public static final String FIELD_COMPANYID = "_.companyId";
    public static final String FIELD_USINGONPREMISE = "_.usingonpremise";
    public static final String FIELD_ONPREMISEURL = "_.onpremiseurl";
    public static final String FIELD_APIKEY = "_.apiKey";
    public static final String FIELD_TESTCASEMAPPINGFIELD = "_.testCaseMappingField";

    public static final String FIELD_BLOCK_PUBLISHTAP = "_.publishTap";
    public static final String FIELD_TAPTESTSASSTEPS = "_.tapTestsAsSteps";
    public static final String FIELD_TAPFILENAMEINIDENTIFIER = "_.tapFileNameInIdentifier";
    public static final String FIELD_TAPTESTNUMBERINIDENTIFIER = "_.tapTestNumberInIdentifier";
    public static final String FIELD_TAPMAPPINGPREFIX = "_.tapMappingPrefix";

    public static final String FIELD_BLOCK_IMPORTTESTCASES = "_.importTestCases";
    public static final String FIELD_IMPORTTESTCASESROOTCATEGORY = "_.importTestCasesRootCategory";

    public static final String FIELD_BLOCK_PUBLISHROBOT = "_.publishRobot";
    public static final String FIELD_ROBOTOUTPUT = "_.robotOutput";
    public static final String FIELD_ROBOTCATENATEPARENTKEYWORDS = "_.robotCatenateParentKeywords";

    public static final String FIELD_CORS = "_.cors";
    public static final String FIELD_ORIGIN = "_.origin";

    public static final String FIELD_COMMENT = "_.comment";
    public static final String FIELD_TAGS = "_.tags";

    public static final String FIELD_PARAMETERS = "_.parameters";

    /**
     * Asserts that a htmlinput is empty.
     *
     * @param input
     */
    protected void assertEmpty(HtmlInput input) {
        assertTrue(input.getNameAttribute() + " was not empty, was: " + input.getValueAttribute(), StringUtils.isEmpty(input.getValueAttribute()));
    }

    /**
     * Asserts that htmlinput has a certain value.
     *
     * @param input
     * @param value
     */
    protected void assertHasValue(HtmlInput input, String value) {
        assertTrue(input.getNameAttribute() + " value was not " + value + ": was " + input.getValueAttribute(), value.equals(input.getValueAttribute()));
    }

    /**
     * Asserts that test area has a certain value.
     *
     * @param textArea
     * @param value
     */
    protected void assertHasValue(HtmlTextArea textArea, String value) {
        assertTrue(textArea.getNameAttribute() + " value was not " + value + ": was " + textArea.getText(), value.equals(textArea.getText()));
    }

    /**
     * Asserts that htmlinput is checked.
     *
     * @param input
     * @param checked
     */
    protected void assertChecked(HtmlInput input, boolean checked) {
        assertTrue(input.getNameAttribute() + " checked is not " + checked, checked ? input.isChecked() : !input.isChecked());
    }

    /**
     * Setups the JenkinsRule for a test.
     */
    @Before
    public void setup() throws IOException {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
    }

    /**
     * @return JenkinsRule WebClient
     */
    protected JenkinsRule.WebClient getWebClient() {
        JenkinsRule.WebClient webClient = j.createWebClient();
//        webClient.setThrowExceptionOnFailingStatusCode(false);
        webClient.setCssErrorHandler(new ErrorHandler() {
            @Override
            public void warning(CSSParseException e) {
            }
            @Override
            public void error(CSSParseException e) {
            }
            @Override
            public void fatalError(CSSParseException e) {
            }
        });
//        webClient.setPrintContentOnFailingStatusCode(false);
        return webClient;
    }

}

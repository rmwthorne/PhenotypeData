/**
 * Copyright © 2014 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mousephenotype.cda.seleniumtests.tests;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mousephenotype.cda.db.dao.PhenotypePipelineDAO;
import org.mousephenotype.cda.seleniumtests.support.GenePage;
import org.mousephenotype.cda.seleniumtests.support.PageStatus;
import org.mousephenotype.cda.seleniumtests.support.SeleniumWrapper;
import org.mousephenotype.cda.seleniumtests.support.TestUtils;
import org.mousephenotype.cda.solr.service.PostQcService;
import org.mousephenotype.cda.utilities.CommonUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author mrelac
 *
 * These are selenium-based JUnit web tests that are configured (via the pom.xml) not to
 * run with the default profile because they take too long to complete. To run them,
 * use the 'web-tests' profile.
 *
 * These selenium tests use selenium's WebDriver protocol and thus need a hub
 * against which to run. The url for the hub is defined in the Test Packages
 * /src/test/resources/testConfig.properties file (driven by /src/test/resources/test-config.xml).
 *
 * To run these tests, edit /src/test/resources/testConfig.properties, making sure
 * that the properties 'seleniumUrl' and 'desiredCapabilities' are defined. Consult
 * /src/test/resources/test-config.xml for valid desiredCapabilities bean ids.
 *
 * Examples:
 *      seleniumUrl=http://mi-selenium-win.windows.ebi.ac.uk:4444/wd/hub
 *      desiredCapabilities=firefoxDesiredCapabilities
 *
 * testAkt2() - @author Gautier Koscielny
 * Selenium test for graph query coverage ensuring each graph display works for
 * any given gene accession/parameter/zygosity from the Solr core
 *
 * IMPORTANT NOTE: In order to run the tests, you must specify the "platform", a directory under the /configfiles
 * resource directory, which must contain an application.properties file.
 *
 * Examples: /Users/mrelac/configfiles/beta/application.properties,
 *           /Users/mrelac/configfiles/dev/application.properties,
 *           /net/isilonP/public/rw/homes/tc_mi01/configfiles/beta/application.properties
 *           /net/isilonP/public/rw/homes/tc_mi01/configfiles/dev/application.properties
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("file:${user.home}/configfiles/${platform}/applicationTest.properties")
@SpringApplicationConfiguration(classes = TestConfig.class)
public class PhenotypeAssociationsTest {

    private CommonUtils commonUtils = new CommonUtils();
    private WebDriver driver;
    private List<String> successList = new ArrayList<>();
    protected TestUtils testUtils = new TestUtils();
    private WebDriverWait wait;

    private final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
    private final int TIMEOUT_IN_SECONDS = 120;         // Increased timeout from 4 to 120 secs as some of the graphs take a long time to load.
    private final int THREAD_WAIT_IN_MILLISECONDS = 20;

    private int timeoutInSeconds = TIMEOUT_IN_SECONDS;
    private int threadWaitInMilliseconds = THREAD_WAIT_IN_MILLISECONDS;

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    Environment env;

    @Autowired
	@Qualifier("postqcService")
    protected PostQcService genotypePhenotypeService;

    @Autowired
    protected PhenotypePipelineDAO phenotypePipelineDAO;

    @Autowired
    protected SeleniumWrapper wrapper;

    @NotNull
    @Value("${baseUrl}")
    protected String baseUrl;


    @PostConstruct
    public void initialise() throws Exception {
        driver = wrapper.getDriver();
    }

    @Before
    public void setup() {
        if (commonUtils.tryParseInt(System.getProperty("TIMEOUT_IN_SECONDS")) != null)
            timeoutInSeconds = commonUtils.tryParseInt(System.getProperty("TIMEOUT_IN_SECONDS"));
        if (commonUtils.tryParseInt(System.getProperty("THREAD_WAIT_IN_MILLISECONDS")) != null)
            threadWaitInMilliseconds = commonUtils.tryParseInt(System.getProperty("THREAD_WAIT_IN_MILLISECONDS"));

        testUtils.printTestEnvironment(driver, wrapper.getSeleniumUrl());
        wait = new WebDriverWait(driver, timeoutInSeconds);

        driver.navigate().refresh();
        commonUtils.sleep(threadWaitInMilliseconds);
    }

    @After
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }


    // PRIVATE METHODS


    private PageStatus processRow(WebDriverWait wait, String geneId, int index) {
        PageStatus status = new PageStatus();
        String message;
        String target = baseUrl + "/genes/" + geneId;
        System.out.println("gene[" + index + "] URL: " + target);

        int sumOfPhenotypeCounts = 0;
        int expectedMinimumResultCount = -1;
        try {
            GenePage genePage = new GenePage(driver, wait, target, geneId, phenotypePipelineDAO, baseUrl);
            genePage.selectGenesLength(100);
            // Make sure this page has phenotype associations.
            List<WebElement> phenotypeAssociationElements = driver.findElements(By.cssSelector("div.inner ul li a.filterTrigger"));
            if ((phenotypeAssociationElements == null) || (phenotypeAssociationElements.isEmpty())) {
                status.addError("ERROR: Expected phenotype association but none was found");
                return status;         // This gene page has no phenotype associations.
            }

            // Get the expected result count.
            int expectedResultsCount = genePage.getResultsCount();
            int actualResultsCount =  driver.findElements(By.xpath("//img[@alt = 'Female' or @alt = 'Male']")).size();

            if (expectedResultsCount != actualResultsCount) {
                status.addError("ERROR: Expected minimum result count of " + expectedMinimumResultCount + " but actual sum of phenotype counts was " + sumOfPhenotypeCounts + " for " + driver.getCurrentUrl());
            }
        } catch (NoSuchElementException | TimeoutException te) {
            message = "Expected page for MGI_ACCESSION_ID " + geneId + "(" + target + ") but found none.";
            status.addError(message);
        }  catch (Exception e) {
            message = "EXCEPTION processing target URL " + target + ": " + e.getLocalizedMessage();
            status.addError(message);
        }

        return status;
    }


    // TESTS


    /**
     * Fetches all gene IDs (MARKER_ACCESSION_ID) from the genotype-phenotype
     * core and tests to make sure:
     * <ul><li>this page has phenotype associations</li>
     * <li>the expected result count is less than or equal to the sum of the
     * phenotype link counts</li></ul>
     *
     * <p><em>Limit the number of test iterations by adding an entry to
     * testIterations.properties with this test's name as the lvalue and the
     * number of iterations as the rvalue. -1 means run all iterations.</em></p>
     *
     * @throws SolrServerException
     */
    @Test
//@Ignore
    public void testTotalsCount() throws SolrServerException {
        PageStatus status = new PageStatus();
        String testName = "testTotalsCount";
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        List<String> geneIds = new ArrayList(genotypePhenotypeService.getAllGenesWithPhenotypeAssociations());

        Date start = new Date();

        int targetCount = testUtils.getTargetCount(env, testName, geneIds, 10);
        System.out.println(dateFormat.format(start) + ": " + testName + " started. Expecting to process " + targetCount + " of a total of " + geneIds.size() + " records.");

        // Loop through all genes, testing each one for valid page load.
        WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
        int i = 0;
        for (String geneId : geneIds) {
// if (i == 0) geneId = "MGI:1922257";
// if (i == 1) geneId = "MGI:1933966";
            if (i >= targetCount) {
                break;
            }

            status.add(processRow(wait, geneId, i));
            i++;

            commonUtils.sleep(threadWaitInMilliseconds);
        }

        if ( ! status.hasErrors()) {
            successList.add("Success");
        }

        testUtils.printEpilogue(testName, start, status, successList.size(), targetCount, geneIds.size());
    }

}

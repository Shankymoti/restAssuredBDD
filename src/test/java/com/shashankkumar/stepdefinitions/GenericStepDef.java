package com.shashankkumar.stepdefinitions;



import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericStepDef - a single-file, commented version of a typical Cucumber step-definition
 * that generates request payloads, invokes APIs and validates response codes.
 *
 * NOTE: This file contains line-by-line inline comments explaining the purpose of each
 * statement. Some helper classes/methods referenced here (ScenarioContext, ApiInfo, ExcelUtils,
 * PropertiesUtil, RestHelper, Constants) are assumed to exist in your project. Where necessary
 * I've shown the *intended* contract for these helpers in comments so you can map them to
 * your actual implementations.
 */
public class GenericStepDef extends BaseRunner {

    // Logger instance for printing useful information during test execution
    private static final Logger log = LoggerFactory.getLogger(GenericStepDef.class);

    // testID is stored locally and also in ScenarioContext for later use by other steps
    private String testID = "";

    /**
     * Step: User generates service request for <apiName> with payload <exampleId>
     *
     * We support two ways to provide payload data:
     *  1) exampleId references a row in an Excel sheet (and test data contains a "Payload" column)
     *  2) exampleId starts with "$" meaning the remainder is treated as a RAW payload string
     *
     * The method prepares the RequestSpecification, resolves base URL and endpoint, loads payload,
     * performs placeholder replacements (using values from ScenarioContext or generated UUIDs)
     * and stores the final payload in ScenarioContext so subsequent steps can call the API.
     */
    @Given("^User generates service request for (.+) with payload (.+)$")
    public void userGeneratesServiceRequestForApiNameWithPayloadPayLoadData(String apiName, String exampleId) throws Exception {
        // reset any previous request specification (headers, query params, body) so tests don't leak state
        ScenarioContext.getRestHelper().resetRequestSpec();

        // If exampleId contains a "$" we treat the provided exampleId as an inline/raw string marker
        // (this mirrors what your screenshots hinted at). We only use 'contains' so both "$abc"
        // and "abc$" are handled, but you can change to startsWith if needed.
        boolean takeRawStringAsPayload = exampleId != null && exampleId.trim().contains("$");

        // If raw payload marker present remove the marker and trim whitespace so real payload can be processed
        if (takeRawStringAsPayload) {
            exampleId = exampleId.replace("$", "").trim();
        }

        // Save the current apiName in scenario context for later use
        ScenarioContext.setApiName(apiName);

        // Logging the API base information so it's easy to debug if wrong base URL or endpoint is used
        log.info("Preparing request for API: {}", apiName);
        log.info("API base: {}", ApiInfo.getAPIDetail(apiName).getDomainName());
        log.info("API endpoint: {}", ApiInfo.getAPIDetail(apiName).getEndpoint());

        // Build base URI from properties and API metadata and set on the request spec
        // (PropertiesUtil looks up domain -> actual base URL from e.g. environment properties)
        String baseDomainProperty = PropertiesUtil.getProperty(ApiInfo.getAPIDetail(apiName).getDomainName());
        String endpoint = ApiInfo.getAPIDetail(apiName).getEndpoint();

        // Set the base URI + endpoint on the RestHelper's request spec so later when invoking
        // the request we don't have to keep passing it around.
        ScenarioContext.getRestHelper().setBaseUri(baseDomainProperty);
        ScenarioContext.getRestHelper().setEndpoint(endpoint);

        // Initialize payload variable
        String payload = null;

        // If exampleId is non-empty we try to fetch test data from Excel using the ApiInfo configuration
        if (exampleId != null && !exampleId.trim().isEmpty()) {
            // Compose path to the Excel file. Constants.EXCEL_PATH should point to the folder holding test data.
            String excelPath = Constants.EXCEL_PATH + ApiInfo.getAPIDetail(apiName).getExcelFileName().trim();

            // ExcelUtils.getDataFromExcelAsMap is expected to return a Map where columnName -> cellValue for the given exampleId.
            Map<String, String> testData = ExcelUtils.getDataFromExcelAsMap(excelPath,
                    ApiInfo.getAPIDetail(apiName).getSheetName(), exampleId);

            // Read the 'Payload' column from excel. This is the JSON/body used to call the API.
            payload = testData.get("Payload");

            // For visibility in logs, print the payload we retrieved from the test data
            log.info("Payload from excel (exampleId={}): {}", exampleId, payload);

            // Put payload into request spec (RestHelper should attach it when invoking)
            ScenarioContext.getRestHelper().setRequestBody(payload);

        } else {
            // If no exampleId provided, fall back to ApiInfo request object (if any)
            if (ApiInfo.getAPIDetail(apiName).getRequestObj() != null &&
                    !ApiInfo.getAPIDetail(apiName).getRequestObj().trim().isEmpty()) {
                payload = ApiInfo.getAPIDetail(apiName).getRequestObj().trim();
                log.info("Using request object from ApiInfo for api {}", apiName);
                ScenarioContext.getRestHelper().setRequestBody(payload);
            }
        }

        // Ensure payload is not null to avoid NullPointerException later when replacing placeholders
        if (payload == null) {
            payload = ""; // default to empty string if not provided
        }

        // Replace tokens/placeholders inside the payload. Examples of placeholders: {groupInvoiceId}, {policyNo} etc.
        // This helper will replace tokens using ScenarioContext values where available, and auto-generate values when needed.
        payload = replacePlaceholdersInPayload(payload);

        // Save final payload in scenario context so other steps (invoke API) can reuse it
        ScenarioContext.setContextVariable("payload", payload);

        // Example: for file upload APIs we might need to add a query param from context variable 'newFileName'
        // If the ApiName is one of the special cases, add the needed query param
        if (apiName.equalsIgnoreCase("UPLOADSYSENGFILES")) {
            // ScenarioContext.getContextVariable("newFileName") is expected to be set by a previous step which uploaded a file
            Object newFileNameObj = ScenarioContext.getContextVariable("newFileName");
            if (newFileNameObj != null) {
                ScenarioContext.getRestHelper().addQueryParam("name", newFileNameObj.toString());
            }
        }

        // Store the test/example id for downstream use
        this.testID = exampleId;
        ScenarioContext.setContextVariable("testID", this.testID);

        log.info("Final payload stored in context for api {}: {}", apiName, payload);
    }


    /**
     * Generic method to invoke API with the request prepared in ScenarioContext
     */
    @When("^Invokes API (.+) with payload$")
    public void userInvokesApiWithPayload(String apiName) {
        // Retrieve the RestAssured RequestSpecification prepared earlier
        RequestSpecification spec = ScenarioContext.getRestHelper().getRequestSpec();

        // Retrieve payload from context
        Object payloadObj = ScenarioContext.getContextVariable("payload");
        String payload = (payloadObj == null) ? "" : payloadObj.toString();

        // Determine HTTP method from API metadata
        String method = ApiInfo.getAPIDetail(apiName).getMethod(); // e.g. GET, POST, PUT, DELETE
        String endpoint = ApiInfo.getAPIDetail(apiName).getEndpoint();

        log.info("Invoking API: {} {}", method, endpoint);

        Response response;
        switch (method.toUpperCase()) {
            case "GET":
                // GET usually doesn't have a body, but spec may already contain query params or headers
                response = RestAssured.given().spec(spec).get(endpoint);
                break;
            case "POST":
                // Pass the payload as body for POST
                response = RestAssured.given().spec(spec).body(payload).post(endpoint);
                break;
            case "PUT":
                response = RestAssured.given().spec(spec).body(payload).put(endpoint);
                break;
            case "DELETE":
                // Some DELETE calls support body, others do not; we include body to be general
                response = RestAssured.given().spec(spec).body(payload).delete(endpoint);
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        // Save response in ScenarioContext so validation steps can access it
        ScenarioContext.setResponse(response);

        log.info("API Response status: {}", response.getStatusCode());
    }

    /**
     * Validates HTTP response status code
     */
    @Then("^Validates the ResponseCode with expected (\\d+)$")
    public void validatesResponseCodeWithExpected(int expectedStatusCode) {
        // Retrieve response that was stored after invocation
        Response response = ScenarioContext.getResponse();

        // Assert that the actual HTTP status code matches expected
        Assert.assertNotNull(response, "Response object is null - did you call the API before validating?");
        int actualCode = response.getStatusCode();
        log.info("Validating response code. Expected: {}, Actual: {}", expectedStatusCode, actualCode);
        Assert.assertEquals(actualCode, expectedStatusCode, "Unexpected HTTP status code returned from API");
    }


    // ----------------------------- Helper methods -----------------------------

    /**
     * Replace placeholders inside payload using context variables or generated values.
     * Supported behaviour:
     *  - If placeholder name exists as a context variable -> replace with its value
     *  - If placeholder is known special token (like groupInvoiceId) -> generate UUID
     *  - Otherwise leave token as-is (or you may choose to throw an error)
     */
    private String replacePlaceholdersInPayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            return payload; // nothing to replace
        }

        // Regex to find tokens like {someToken}
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(payload);

        // StringBuffer is used by Matcher.appendReplacement/appendTail mechanism
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String tokenName = matcher.group(1); // e.g. "groupInvoiceId"

            // First try to resolve from ScenarioContext
            Object ctxValue = ScenarioContext.getContextVariable(tokenName);
            String replacement;

            if (ctxValue != null) {
                // If context value exists, use it
                replacement = ctxValue.toString();
            } else {
                // No context value - handle some well-known tokens by generating values
                switch (tokenName) {
                    case "groupInvoiceId":
                    case "invoiceId":
                    case "randomId":
                        // Create a short random id from UUID
                        replacement = UUID.randomUUID().toString().replace("-", "");
                        break;
                    default:
                        // If you prefer to fail the test when token can't be resolved, throw exception here.
                        // For now we will leave the token unchanged so debugging is easier.
                        replacement = matcher.group(0); // includes the braces
                        break;
                }
            }

            // Escape any backslashes in replacement so appendReplacement works properly
            replacement = Matcher.quoteReplacement(replacement);
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

}


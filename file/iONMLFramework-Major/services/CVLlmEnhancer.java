package com.tcsion.ml.Services;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tcsion.ml.Beans.RinxBean;
import com.tcsion.ml.Utils.ExceptionLogUtility;
import com.tcsion.ml.Utils.MlFrameworkUtils;

// Framework utility imports
import com.tcs.genai.engine.service.EngineInvocationService;
import com.tcs.genai.prompt.service.Impl.PayloadGeneratorService;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

/**
 * Single utility class containing ALL LLM enhancement logic for CV parsing.
 *
 * Pipeline: extractText -> preprocessText -> callLlm -> validateAndNormalize -> writeOutputFiles
 * Uses dependencies: PDFBox, Apache POI, Jackson, GptIntegration, MlFrameworkUtils.
 * 
 * userJson:        optional custom extraction schema { "fieldName": "description", ... }
 *                      if present -> LLM extracts only those keys, output contains only those keys
 *                      if null    -> full default iON schema used (original behavior)
 *
 * callLlm()  uses the GenAI framework utilities:
 *   PayloadGeneratorService  -> fetches prompt from DB
 *   EngineInvocationService  -> invokes Azure LLM, handles token audit
 *
 */
@SuppressWarnings("unchecked")
public class CVLlmEnhancer {

    private static final Log logger = LogFactory.getLog(CVLlmEnhancer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_CV_CHARS    = 15000;
    private static final int MIN_TEXT_LENGTH = 30;

    // -----------------------------------------------------------------------
    // Default schema 
    // -----------------------------------------------------------------------
    private static final String[] STRING_FIELDS = {
        "presentCity", "presentCountry", "presentPincode", "presentState",
        "permanentCity", "permanentCountry", "permanentPincode", "permanentState",
        "fathersName", "passportNumber", "nationality", "maritalStatus", "gender",
        "currentCTC", "expectedCTC", "languagesKnown", "panNumber", "hobbies",
        "communicationSkill", "name", "firstName", "lastName", "middleName",
        "dateOfBirth", "emailaddress", "phonenumber", "presentaddress", "permanentaddress"
    };

    private static final String[] ARRAY_FIELDS = {
        "careerProfile", "projectProfile", "educationalProfile",
        "skills", "certifications", "publicationDetails"
    };

    private static final String[] OBJECT_FIELDS = {
        "trainings", "awardsAndAchievements"
    };

    private static final String[] CAREER_KEYS     = {"companyName","designation","startDate","endDate","responsibilities","location"};
    private static final String[] PROJECT_KEYS     = {"projectName","role","description","technologiesUsed","duration","client"};
    private static final String[] EDUCATION_KEYS   = {"degree","institution","university","startYear","endYear","percentage","specialization"};
    private static final String[] PUBLICATION_KEYS = {"title","journal","year","authors","url"};
    private static final String[] TRAINING_KEYS    = {"trainingName","institute","duration","year"};
    private static final String[] AWARD_KEYS       = {"awardName","year","description"};

    // -----------------------------------------------------------------------
    // Section detection patterns
    // -----------------------------------------------------------------------
    private static final Map<String, List<Pattern>> SECTION_PATTERNS = new LinkedHashMap<>();
    static {
        addSection("PERSONAL_INFO",
            "personal\\s*(info|information|details|data|profile)",
            "contact\\s*(info|information|details)", "about\\s*me", "bio[\\s-]*data");
        addSection("CAREER_EXPERIENCE",
            "(work|professional|employment)\\s*(experience|history)",
            "career\\s*(profile|summary|history|objective)", "experience",
            "work\\s*details", "employment\\s*record", "positions?\\s*held");
        addSection("EDUCATION",
            "education(al)?\\s*(qualifications?|background|details|profile)?",
            "academic\\s*(qualifications?|background|details|profile|record)",
            "qualifications?", "schooling", "degrees?");
        addSection("SKILLS",
            "(technical\\s*)?skills?\\s*(set|summary)?", "competenc(ies|e)",
            "technologies?\\s*(known|used)?", "tools?\\s*(and|&)\\s*technologies?",
            "proficiency", "expertise");
        addSection("PROJECTS",
            "project(s)?\\s*(profile|details|experience|summary)?",
            "key\\s*projects?", "notable\\s*projects?");
        addSection("CERTIFICATIONS",
            "certifications?\\s*(and\\s*licenses?)?",
            "licenses?\\s*(and\\s*certifications?)?",
            "professional\\s*certifications?", "accreditations?");
        addSection("PUBLICATIONS",
            "publications?\\s*(and\\s*papers?)?",
            "research\\s*(papers?|publications?)", "papers?\\s*published");
        addSection("TRAININGS",
            "trainings?\\s*(and\\s*workshops?)?",
            "workshops?\\s*(and\\s*trainings?)?",
            "courses?\\s*(completed|attended)", "professional\\s*development");
        addSection("AWARDS",
            "awards?\\s*(and\\s*)?(achievements?|recognitions?|honours?)?",
            "achievements?\\s*(and\\s*awards?)?",
            "honours?\\s*(and\\s*awards?)?", "recognitions?");
        addSection("HOBBIES",
            "hobbies?\\s*(and\\s*interests?)?", "interests?\\s*(and\\s*hobbies?)?",
            "extra[\\s-]*curricular", "leisure\\s*activities");
        addSection("LANGUAGES",
            "languages?\\s*(known|spoken|proficiency)?", "linguistic\\s*(skills?|abilities?)");
        addSection("REFERENCES", "references?", "referees?");
        addSection("DECLARATION", "declaration", "i\\s*hereby\\s*declare");
    }

    private static void addSection(String name, String... patterns) {
        List<Pattern> list = new ArrayList<>();
        for (String p : patterns) {
            list.add(Pattern.compile("^" + p + "$", Pattern.CASE_INSENSITIVE));
        }
        SECTION_PATTERNS.put(name, list);
    }

    // -----------------------------------------------------------------------
    // Entity tagging patterns
    // -----------------------------------------------------------------------
    private static final List<String[]> ENTITY_PATTERNS = new ArrayList<>();
    static {
        ENTITY_PATTERNS.add(new String[]{"\\b([\\w.+\\-]+@[\\w\\-]+\\.[\\w.\\-]+)\\b",                                           "[EMAIL: $1]"});
        ENTITY_PATTERNS.add(new String[]{"\\b(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})\\b",                                         "[DATE: $1]"});
        ENTITY_PATTERNS.add(new String[]{"\\b((?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[\\s,]+\\d{4})\\b",        "[DATE: $1]"});
        ENTITY_PATTERNS.add(new String[]{"\\b(\\d{4})\\s*[-\u2013\u2014to]+\\s*(\\d{4}|present|current|till\\s*date|ongoing)\\b", "[PERIOD: $0]"});
        ENTITY_PATTERNS.add(new String[]{"\\b(\\d+\\.?\\d*)\\s*(cgpa|gpa|%|percentage)\\b",                                       "[SCORE: $0]"});
        ENTITY_PATTERNS.add(new String[]{"\\b(cgpa|gpa)\\s*[:\\-]?\\s*(\\d+\\.?\\d*)\\b",                                         "[SCORE: $0]"});
        ENTITY_PATTERNS.add(new String[]{"\\b(B\\.?Tech|M\\.?Tech|B\\.?E|M\\.?E|B\\.?Sc|M\\.?Sc|B\\.?A|M\\.?A|MBA|Ph\\.?D|BCA|MCA|B\\.?Com|M\\.?Com|Diploma)\\b", "[DEGREE: $1]"});
        ENTITY_PATTERNS.add(new String[]{"\\b([A-Z]{5}\\d{4}[A-Z])\\b",                                                           "[PAN: $1]"});
        ENTITY_PATTERNS.add(new String[]{"\\b([A-Z]\\d{7})\\b",                                                                    "[PASSPORT: $1]"});
        ENTITY_PATTERNS.add(new String[]{"\\b(\\d+\\.?\\d*)\\s*(lpa|lakh|lac|lakhs?|crore|cr)\\b",                                 "[CTC: $0]"});
    }

    private static final List<Pattern> FILLER_PATTERNS = new ArrayList<>();
    static {
        String[] fillers = {
            "i (hereby )?(would like to |wish to )?(declare|state|confirm) that",
            "the above (mentioned )?(information|details|particulars) (is|are) (true|correct)",
            "to the best of my knowledge( and belief)?",
            "i am (a |an )?(highly )?(motivated|dedicated|passionate|enthusiastic|hardworking|self[\\s\\-]?motivated) (individual|professional|person|candidate)",
            "seeking (a )?(challenging|rewarding|growth[\\s\\-]?oriented) (opportunity|position|role)",
            "with (a )?(proven|strong|demonstrated) track record",
            "(good|excellent|strong) (communication|interpersonal|analytical) skills?",
            "ability to work (in a team|independently|under pressure)",
            "quick learner (with|and)"
        };
        for (String f : fillers) {
            FILLER_PATTERNS.add(Pattern.compile(f, Pattern.CASE_INSENSITIVE));
        }
    }

    // -----------------------------------------------------------------------
    // userJson schema injection suffix
    // Appended to the last user message when caller provides a custom schema
    // -----------------------------------------------------------------------
    private static final String USER_JSON_INJECTION_PREFIX =
        "\n\nIMPORTANT OVERRIDE: Extract ONLY the following fields. " +
        "Return a JSON object with EXACTLY these keys and nothing else:\n";

    // -----------------------------------------------------------------------
    // PUBLIC API: tryLlmPipeline
    // -----------------------------------------------------------------------
    public static RinxBean tryLlmPipeline(
            String inputFilePath,
            String outputFolderPath,
            org.json.JSONObject userJson,
            JsonNode vendorCredMap,
            String useCaseId,
            String appId,
            String apiID,
            String orgId,
            HashMap<String, Object> usageDetailsMap) {

        try {
            // Guard: vendorCredMap must be present
            if (vendorCredMap == null) {
                logger.error("[LLM ENHANCER] vendorCredMap is null. Skipping LLM.");
                return null;
            }

            // Guard: same path/security validations as Rinx � never looser
            MlFrameworkUtils utilityObj = new MlFrameworkUtils();
            if (!utilityObj.validateImagePath(inputFilePath)) {
                logger.error("[LLM ENHANCER] validateImagePath failed for: " + inputFilePath);
                return null;
            }
            if (!utilityObj.validateFileExtensionPDFDocxDoc(inputFilePath)) {
                logger.error("[LLM ENHANCER] validateFileExtension failed for: " + inputFilePath);
                return null;
            }
            if (utilityObj.checkDirectoryTraversalAttempt(outputFolderPath)) {
                logger.error("[LLM ENHANCER] Directory traversal attempt detected in: " + outputFolderPath);
                return null;
            }
            if (!utilityObj.validateFolderPath(outputFolderPath)) {
                logger.error("[LLM ENHANCER] validateFolderPath failed for: " + outputFolderPath);
                return null;
            }
            if (!utilityObj.validateDirectory(outputFolderPath)) {
                logger.error("[LLM ENHANCER] validateDirectory failed for: " + outputFolderPath);
                return null;
            }

            // Step 1: Resolve file
            File cvFile = new File(inputFilePath);
            if (!cvFile.isFile()) {
                logger.error("[LLM ENHANCER] Input path is not a file: " + inputFilePath);
                return null;
            }

            // Step 2: Extract text
            String rawText = extractText(cvFile);
            if (rawText == null || rawText.trim().length() < MIN_TEXT_LENGTH) {
                logger.error("[LLM ENHANCER] Text extraction returned insufficient content.");
                return null;
            }
            logger.error("[LLM ENHANCER] Extracted " + rawText.length() + " chars from " + cvFile.getName());

            // Step 3: Pre-process
            String processed = preprocessText(rawText);
            if (processed == null || processed.trim().length() < MIN_TEXT_LENGTH) {
                logger.error("[LLM ENHANCER] Pre-processing insufficient, using raw text.");
                processed = rawText;
            } else {
                logger.error("[LLM ENHANCER] Pre-processed: " + rawText.length()
                        + " -> " + processed.length() + " chars");
            }

            // Step 4: Call LLM via framework utilities
            String llmJson = callLlm(processed, userJson, vendorCredMap,
                    useCaseId, appId, apiID, orgId, usageDetailsMap);
            if (llmJson == null || llmJson.trim().isEmpty()) {
                logger.error("[LLM ENHANCER] LLM returned null/empty.");
                return null;
            }

            // Step 5: Validate and normalize
            String validatedJson = (userJson != null && userJson.length() > 0)
                    ? validateAndNormalizeCustom(llmJson, userJson)
                    : validateAndNormalize(llmJson);

            if (validatedJson == null) {
                logger.error("[LLM ENHANCER] Validation/normalization failed.");
                return null;
            }

            // Step 6: Write output files
            String uniqueId = UUID.randomUUID().toString();
            String[] paths = writeOutputFiles(validatedJson, outputFolderPath, uniqueId);
            if (paths == null) {
                logger.error("[LLM ENHANCER] File write failed.");
                return null;
            }

            // Step 7: Build RinxBean
            RinxBean rb = new RinxBean();
            rb.setStatus("CVPARSE_200");
            rb.setMessage("CVParsing Execution Sucessfull");
            rb.setJsonOutput(paths[0]);
            rb.setXmlOutput(paths[1]);

            logger.error("[LLM ENHANCER] LLM pipeline succeeded. json=" + paths[0]
                    + " xml=" + paths[1]);
            return rb;

        } catch (Exception e) {
            logger.error("[LLM ENHANCER] Pipeline exception: " + e.getMessage());
            ExceptionLogUtility.logException("[LLM ENHANCER] Pipeline exception:", e, logger);
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // LLM CALL � framework utility flow
    //
    // 1. Extract vendor details from vendorCredMap
    // 2. Build parameterMap for PayloadGeneratorService + EngineInvocationService
    // 3. PayloadGeneratorService fetches prompt from DB
    // 4. Extract messages array from payload
    // 5. Inject processedText (and optional userJson schema) into last user message
    // 6. Build invocationContext for token audit
    // 7. EngineInvocationService invokes Azure LLM
    // 8. Extract vendor_response string from result
    // -----------------------------------------------------------------------
    public static String callLlm(
            String processedText,
            org.json.JSONObject userJson,
            JsonNode vendorCredMap,
            String useCaseId,
            String appId,
            String apiID,
            String orgId,
            HashMap<String, Object> usageDetailsMap) {

        try {
            // Truncate cleanly on newline boundary
            if (processedText.length() > MAX_CV_CHARS) {
                int cutAt = processedText.lastIndexOf('\n', MAX_CV_CHARS);
                processedText = processedText.substring(0, cutAt > 0 ? cutAt : MAX_CV_CHARS);
                logger.error("[LLM ENHANCER] Text truncated to " + processedText.length() + " chars for LLM.");
            }

            // ------------------------------------------------------------------
            // Step 1: Extract vendor details from vendorCredMap
            // vendorCredMap structure: { "OPENAI_GPT4o": { "OPENAI_API_URL": ...,
            //   "OPENAI_API_KEY": ..., "OPENAI_API_VERSION": ..., "serviceId": ... } }
            // ------------------------------------------------------------------
            String vendorName  = null;
            String serviceId   = null;
            JsonNode vendorNode = null;

            Iterator<Map.Entry<String, JsonNode>> vendorFields = vendorCredMap.fields();
            if (vendorFields.hasNext()) {
                Map.Entry<String, JsonNode> entry = vendorFields.next();
                vendorName = entry.getKey();
                vendorNode = entry.getValue();
            }

            if (vendorName == null || vendorNode == null) {
                logger.error("[LLM ENHANCER] vendorCredMap is empty � cannot determine vendor.");
                return null;
            }

            serviceId = vendorNode.path("serviceId").textValue();
            if (serviceId == null || serviceId.isEmpty()) {
                logger.error("[LLM ENHANCER] serviceId missing from vendorCredMap for vendor: " + vendorName);
                return null;
            }

            logger.error("[LLM ENHANCER] vendorName=" + vendorName + " | serviceId=" + serviceId);

            // Build vendorProps map for EngineInvocationService
            HashMap<String, String> vendorPropsInner = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> credFields = vendorNode.fields();
            while (credFields.hasNext()) {
                Map.Entry<String, JsonNode> credEntry = credFields.next();
                if (credEntry.getValue().isTextual()) {
                    vendorPropsInner.put(credEntry.getKey(), credEntry.getValue().textValue());
                }
            }

            Map<String, HashMap<String, String>> vendorProps = new HashMap<>();
            vendorProps.put(vendorName, vendorPropsInner);

            // ------------------------------------------------------------------
            // Step 2: Build parameterMap for PayloadGeneratorService
            // ------------------------------------------------------------------
            Map<String, String> parameterMap = new HashMap<>();
            parameterMap.put(GenAIFrameworkConstants.SERVICE_NAME, vendorName);
            parameterMap.put(GenAIFrameworkConstants.SERVICE_ID,   serviceId);

            // ------------------------------------------------------------------
            // Step 3: Fetch prompt from DB via PayloadGeneratorService
            // ------------------------------------------------------------------
            long   useCaseIdL = Long.parseLong(useCaseId);
            int    apiIdI     = Integer.parseInt(apiID);
            long   orgIdL     = Long.parseLong(orgId);
            long   appIdL     = Long.parseLong(appId);

            PayloadGeneratorService payloadGeneratorService = new PayloadGeneratorService();
            JSONObject payloadFromDb = payloadGeneratorService.generatePayload(
                    useCaseIdL, parameterMap, apiIdI, orgIdL, appIdL);

            logger.error("[LLM ENHANCER] Payload from DB: " + payloadFromDb);

            // Check for framework error codes
            if (payloadFromDb == null || payloadFromDb.isEmpty()) {
                logger.error("[LLM ENHANCER] PayloadGeneratorService returned null/empty.");
                return null;
            }

            Object statusCode = payloadFromDb.get(GenAIFrameworkConstants.STATUS_CODE);
            if (statusCode == null || !GenAIFrameworkConstants.GEN_200.equals(statusCode.toString())) {
                logger.error("[LLM ENHANCER] PayloadGeneratorService returned error status: "
                        + statusCode + " | message: " + payloadFromDb.get(GenAIFrameworkConstants.STATUS_MESSAGE));
                return null;
            }

            // ------------------------------------------------------------------
            // Step 4: Extract messages array from payload
            // ------------------------------------------------------------------
            JsonNode payloadNode   = MAPPER.readTree(payloadFromDb.toString()).get("payload");
            JsonNode messagesNode  = payloadNode != null ? payloadNode.get("messages") : null;

            if (messagesNode == null || !messagesNode.isArray() || messagesNode.size() == 0) {
                logger.error("[LLM ENHANCER] messages array missing or empty in DB payload.");
                return null;
            }

            // Convert to org.json.simple.JSONArray for EngineInvocationService
            org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
            JSONArray dbMessages = (JSONArray) parser.parse(messagesNode.toString());

            // ------------------------------------------------------------------
            // Step 5: Inject processedText into last user message
            // Also inject userJson custom schema suffix if present
            // ------------------------------------------------------------------
            JSONArray finalMessages = injectCvTextIntoMessages(
                    dbMessages, processedText, userJson);

            if (finalMessages == null || finalMessages.isEmpty()) {
                logger.error("[LLM ENHANCER] Message injection failed.");
                return null;
            }

            // ------------------------------------------------------------------
            // Step 6: Build invocationContext for token audit
            // ------------------------------------------------------------------
            Map<String, Object> invocationContext = new HashMap<String, Object>();
            invocationContext.put("appId",     appIdL);
            invocationContext.put("orgId",     orgIdL);
            invocationContext.put("apiId",     apiIdI);
            invocationContext.put("usecaseId", useCaseIdL);
            invocationContext.put("serviceId", serviceId);

            // ------------------------------------------------------------------
            // Step 7: Build final payload JSONObject for EngineInvocationService
            // Mirrors the structure EngineInvocationService.invokeAdvanceAiEngine expects
            // ------------------------------------------------------------------
            JSONObject enginePayload = new JSONObject();
            enginePayload.put("messages", finalMessages);

            // Copy any other top-level keys from DB payload (temperature, max_tokens etc)
            if (payloadNode != null) {
                Iterator<Map.Entry<String, JsonNode>> payloadFields = payloadNode.fields();
                while (payloadFields.hasNext()) {
                    Map.Entry<String, JsonNode> pf = payloadFields.next();
                    if (!"messages".equals(pf.getKey()) && pf.getValue().isValueNode()) {
                        enginePayload.put(pf.getKey(), pf.getValue().asText());
                    }
                }
            }
            
            // metadata handling should be done in utility framework
            enginePayload = addMetadataToPayload(vendorName, vendorPropsInner, enginePayload);

            // ------------------------------------------------------------------
            // Step 8: Invoke engine via EngineInvocationService
            // tokenAuditNeeded=true � audit is always written on LLM path
            // ------------------------------------------------------------------
            EngineInvocationService eis = new EngineInvocationService(
                    vendorProps, invocationContext, true);

            JSONObject finalResult = eis.invokeAdvanceAiEngine(enginePayload, parameterMap);
            logger.error("[LLM ENHANCER] EngineInvocationService result: " + finalResult);

            if (finalResult == null) {
                logger.error("[LLM ENHANCER] EngineInvocationService returned null.");
                return null;
            }
            
	         // Check engine status � log it but don't block on it
	         Object engineStatus = finalResult.get(GenAIFrameworkConstants.STATUS_CODE);
	         logger.error("[LLM ENHANCER] Engine status code: " + engineStatus);
   
            // ------------------------------------------------------------------
            // Step 9: Extract token counts into usageDetailsMap for response audit
            // ------------------------------------------------------------------
            if (usageDetailsMap != null && finalResult.containsKey("tokens")) {
                try {
                    JSONObject tokens = (JSONObject) finalResult.get("tokens");
                    if (tokens != null) {
                        Object inputTok  = tokens.get("inputTokens");
                        Object outputTok = tokens.get("outputTokens");
                        long input  = inputTok  instanceof Number ? ((Number) inputTok).longValue()  : 0L;
                        long output = outputTok instanceof Number ? ((Number) outputTok).longValue() : 0L;
                        usageDetailsMap.put("promptTokens",     input);
                        usageDetailsMap.put("completionTokens", output);
                        usageDetailsMap.put("totalTokens",      input + output);
                        usageDetailsMap.put("engineId",           vendorName);
                        logger.error("[LLM ENHANCER] Token audit � input=" + input
                                + " output=" + output + " engine=" + vendorName);
                    }
                } catch (Exception tokenEx) {
                    logger.error("[LLM ENHANCER] Token extraction failed | error=" + tokenEx.getMessage());
                    // Non-fatal � pipeline continues
                }
            }

            // ------------------------------------------------------------------
            // Step 10: Extract vendor_response string
            // ------------------------------------------------------------------
            Object vendorResponse = finalResult.get(GenAIFrameworkConstants.VENDOR_RESPONSE);
            if (vendorResponse == null || vendorResponse.toString().trim().isEmpty()) {
                logger.error("[LLM ENHANCER] vendor_response is null or empty in engine result.");
                return null;
            }

            String output = vendorResponse.toString().trim();

            // Strip markdown fences if model returns them despite instructions
            if (output.startsWith("```")) {
                output = output.replaceAll("```(?:json)?", "").trim();
                if (output.endsWith("```")) output = output.substring(0, output.length() - 3).trim();
            }

            return output;

        } catch (Exception e) {
            logger.error("[LLM ENHANCER] LLM call exception: " + e.getMessage());
            ExceptionLogUtility.logException("[LLM ENHANCER] LLM call exception:", e, logger);
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // MESSAGE INJECTION
    //
    // Finds the last message with role="user" in the DB-fetched messages array
    // and appends the processed CV text to its content.
    // If userJson is present, also appends the custom schema instruction.
    //
    // This keeps the system prompt (guardrails, few-shots) from DB intact
    // and only adds the dynamic CV content at the end.
    // -----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static JSONArray injectCvTextIntoMessages(
            JSONArray dbMessages,
            String processedText,
            org.json.JSONObject userJson) {

        try {
            JSONArray result = new JSONArray();

            int lastUserIndex = -1;
            for (int i = 0; i < dbMessages.size(); i++) {
                JSONObject msg = (JSONObject) dbMessages.get(i);
                if ("user".equals(msg.get("role"))) {
                    lastUserIndex = i;
                }
            }

            if (lastUserIndex == -1) {
                // No user message found � create one with just the CV text
                logger.error("[LLM ENHANCER] No user message in DB prompt � creating one.");
                result.addAll(dbMessages);
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", buildInjectionSuffix(processedText, userJson));
                result.add(userMsg);
                return result;
            }

            // Append CV text (and optional schema) to the last user message content
            for (int i = 0; i < dbMessages.size(); i++) {
                JSONObject msg = (JSONObject) dbMessages.get(i);
                if (i == lastUserIndex) {
                    JSONObject injected = new JSONObject();
                    injected.put("role", msg.get("role"));
                    String existingContent = msg.get("content") != null
                            ? msg.get("content").toString() : "";
                    injected.put("content", existingContent
                            + "\n\n" + buildInjectionSuffix(processedText, userJson));
                    result.add(injected);
                } else {
                    result.add(msg);
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("[LLM ENHANCER] injectCvTextIntoMessages error: " + e.getMessage());
            return null;
        }
    }

    // Builds the text to append to the last user message
    private static String buildInjectionSuffix(String processedText, org.json.JSONObject userJson) {
    	   StringBuilder sb = new StringBuilder();
    	   if (userJson != null && userJson.length() > 0) {
    	       // Custom schema mode � inject caller-defined schema
    	       sb.append(USER_JSON_INJECTION_PREFIX);
    	       sb.append(buildCustomSchemaString(userJson));
    	       sb.append("\n\nExtract ONLY these fields from the CV below.");
    	   } else {
    	       // Default mode � inject full TCSION schema built from Java constants
    	       sb.append("SCHEMA:\n");
    	       sb.append(buildSchemaString());
    	   }
    	   sb.append("\n\nPRE-PROCESSED CV TEXT:\n").append(processedText);
    	   sb.append("\n\nReturn ONLY valid JSON. No explanation. No markdown fences.");
    	   return sb.toString();
    	}
    // -----------------------------------------------------------------------
    // Metadata Adder(should be done in genai utility)
    // -----------------------------------------------------------------------

    
    private static JSONObject addMetadataToPayload(
    	       String serviceName, HashMap<String, String> credMap, JSONObject payload) {
    	   try {
    	       HashMap<String, String> customKeys = new HashMap<>();
    	       if (GenAIFrameworkConstants.OPENAI_GPT4_1.equals(serviceName)
    	               || GenAIFrameworkConstants.OPENAI_GPT4O.equals(serviceName)
    	               || GenAIFrameworkConstants.OPENAI_GPT.equals(serviceName)) {
    	           customKeys.put(GenAIFrameworkConstants.OPENAI_MODEL_TYPE,  "model");
    	           customKeys.put(GenAIFrameworkConstants.OPENAI_TEMPERATURE, "temperature");
    	           customKeys.put(GenAIFrameworkConstants.OPENAI_MAX_TOKENS,  "max_tokens");
    	           customKeys.put(GenAIFrameworkConstants.OPENAI_TOP_P,       "top_p");
    	       } else if (GenAIFrameworkConstants.AZURE_MISTRAL_SMALL_2503.equals(serviceName)) {
    	           customKeys.put("AZMISTRAL_MODEL_TYPE",  "model");
    	           customKeys.put("AZMISTRAL_TEMPERATURE", "temperature");
    	           customKeys.put("AZMISTRAL_MAX_TOKENS",  "max_tokens");
    	           customKeys.put("AZMISTRAL_TOP_P",       "top_p");
    	       } else {
    	           logger.error("[LLM ENHANCER] addMetadataToPayload: unsupported service=" + serviceName);
    	           return payload;
    	       }
    	       for (Map.Entry<String, String> entry : customKeys.entrySet()) {
    	           String key   = entry.getKey();
    	           String field = entry.getValue();
    	           if (!credMap.containsKey(key) || credMap.get(key) == null) continue;
    	           if ("model".equals(field)) {
    	               payload.put(field, credMap.get(key));
    	           } else if ("max_tokens".equals(field)) {
    	               payload.put(field, Integer.parseInt(credMap.get(key)));
    	           } else {
    	               payload.put(field, Double.parseDouble(credMap.get(key)));
    	           }
    	       }
    	       logger.error("[LLM ENHANCER] addMetadataToPayload done | service=" + serviceName
    	               + " | keys added=" + customKeys.size());
    	       // Enable JSON Mode � LLM to always return valid parseable JSON
    	       org.json.simple.JSONObject responseFormat = new org.json.simple.JSONObject();
    	       responseFormat.put("type", "json_object");
    	       payload.put("response_format", responseFormat);
    	       logger.error("[LLM ENHANCER] JSON Mode enabled for service=" + serviceName);
    	   } catch (Exception e) {
    	       logger.error("[LLM ENHANCER] addMetadataToPayload exception | service="
    	               + serviceName + " | error=" + e.getMessage());
    	   }
    	   return payload;
    	}
    	   
    // -----------------------------------------------------------------------
    // Schema builders
    // -----------------------------------------------------------------------

    private static String buildCustomSchemaString(org.json.JSONObject userJson) {
        try {
            ObjectNode schema = JsonNodeFactory.instance.objectNode();
            java.util.Iterator<String> keys = userJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                schema.put(key, userJson.optString(key, ""));
            }
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        } catch (Exception e) {
            logger.error("[LLM ENHANCER] buildCustomSchemaString error: " + e.getMessage());
            return "{}";
        }
    }
    
    private static String buildSchemaString() {
    	   try {
    	       ObjectNode schema = JsonNodeFactory.instance.objectNode();
    	       for (String f : STRING_FIELDS) schema.put(f, "");
    	       for (String f : ARRAY_FIELDS)  schema.putArray(f);
    	       for (String f : OBJECT_FIELDS) schema.putObject(f);
    	       return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
    	   } catch (Exception e) {
    	       return "{}";
    	   }
    	}

    // -----------------------------------------------------------------------
    // TEXT EXTRACTION 
    // -----------------------------------------------------------------------

    public static String extractText(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".pdf"))  return extractPdf(file);
            if (name.endsWith(".docx")) return extractDocx(file);
            if (name.endsWith(".doc"))  return extractDoc(file);
        } catch (Exception e) {
            logger.error("[LLM ENHANCER] Text extraction error: " + e.getMessage());
        }
        return "";
    }

    private static String extractPdf(File file) throws Exception {
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    private static String extractDocx(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        sb.append(cell.getText()).append("\n");
                    }
                }
            }
            return sb.toString();
        }
    }

    private static String extractDoc(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument doc = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    // -----------------------------------------------------------------------
    // TEXT PRE-PROCESSING 
    // -----------------------------------------------------------------------

    public static String preprocessText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return "";
        String text = cleanNoise(rawText);
        List<String[]> sections = detectSections(text);
        List<String[]> tagged   = tagEntities(sections);
        return assembleAndCompress(tagged, 10000);
    }

    private static String cleanNoise(String text) {
        text = text.replaceAll("(?im)^\\s*Page\\s*\\d+\\s*(of\\s*\\d+)?\\s*$", "");
        text = text.replaceAll("(?m)^\\s*\\d{1,2}\\s*$", "");
        text = text.replaceAll("(?im)^\\s*(curriculum\\s*vitae|resume|cv)\\s*$", "");
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = text.replaceAll("[ \\t]{2,}", " ");
        String[] lines = text.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) sb.append(line.trim()).append("\n");
        return sb.toString().trim();
    }

    private static List<String[]> detectSections(String text) {
        String[] lines = text.split("\\n");
        List<String[]> sections = new ArrayList<>();
        String currentLabel = "PERSONAL_INFO";
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            String detected = matchSectionHeader(line);
            if (detected != null) {
                if (currentContent.length() > 0) {
                    sections.add(new String[]{currentLabel, currentContent.toString().trim()});
                }
                currentLabel = detected;
                currentContent = new StringBuilder();
            } else {
                currentContent.append(line).append("\n");
            }
        }
        if (currentContent.length() > 0) {
            sections.add(new String[]{currentLabel, currentContent.toString().trim()});
        }

        if (sections.size() <= 1 && text.trim().length() > 0) {
            sections = inferSections(text);
        }
        return sections;
    }

    private static String matchSectionHeader(String line) {
        String clean = line.trim().replaceAll(":$", "").trim();
        if (clean.isEmpty() || clean.length() > 60) return null;
        for (Map.Entry<String, List<Pattern>> entry : SECTION_PATTERNS.entrySet()) {
            for (Pattern p : entry.getValue()) {
                if (p.matcher(clean).matches()) return entry.getKey();
            }
        }
        return null;
    }

    private static List<String[]> inferSections(String text) {
        List<String[]> sections = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");
        for (String para : paragraphs) {
            if (para.trim().isEmpty()) continue;
            sections.add(new String[]{inferParagraphSection(para), para.trim()});
        }
        if (sections.isEmpty()) sections.add(new String[]{"UNKNOWN", text});
        return sections;
    }

    private static String inferParagraphSection(String para) {
        String lower = para.toLowerCase();

        String[] eduPatterns = {
            "\\b(b\\.?tech|m\\.?tech|b\\.?e|m\\.?e|b\\.?sc|m\\.?sc|mba|phd|doctorate)\\b",
            "\\b(university|college|institute|school|iit|nit|iiit)\\b",
            "\\b(cgpa|gpa|percentage|marks|grade|distinction|first\\s*class)\\b",
            "\\b(graduated|graduation|bachelor|master|diploma|degree)\\b",
            "\\b(10th|12th|hsc|ssc|cbse|icse|board)\\b"
        };
        if (countMatches(lower, eduPatterns) >= 2) return "EDUCATION";

        String[] expPatterns = {
            "\\b(worked|working|employed|joined|resigned|currently)\\b",
            "\\b(company|organization|firm|employer|client)\\b",
            "\\b(responsibilities|role|designation|position|manager|engineer|developer|analyst)\\b",
            "\\b(years?\\s*of\\s*experience|yrs?\\s*exp)\\b",
            "\\b(team\\s*lead|senior|junior|associate|intern)\\b"
        };
        if (countMatches(lower, expPatterns) >= 2) return "CAREER_EXPERIENCE";

        String[] skillPatterns = {
            "\\b(java|python|javascript|react|angular|sql|aws|azure|docker)\\b",
            "\\b(proficient|expertise|skilled|knowledge|familiar)\\b"
        };
        if (countMatches(lower, skillPatterns) >= 2) return "SKILLS";

        String[] personalPatterns = {
            "\\b[\\w.+\\-]+@[\\w\\-]+\\.[\\w.\\-]+\\b",
            "[\\+]?\\d[\\d\\s\\-()]{8,}",
            "\\b(father|mother|spouse|marital|gender|nationality|passport|dob|date\\s*of\\s*birth)\\b"
        };
        if (countMatches(lower, personalPatterns) >= 1) return "PERSONAL_INFO";

        return "UNKNOWN";
    }

    private static int countMatches(String text, String[] patterns) {
        int count = 0;
        for (String p : patterns) {
            if (Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(text).find()) count++;
        }
        return count;
    }

    private static List<String[]> tagEntities(List<String[]> sections) {
        List<String[]> tagged = new ArrayList<>();
        for (String[] sec : sections) {
            tagged.add(new String[]{sec[0], applyEntityTags(sec[1])});
        }
        return tagged;
    }

    private static String applyEntityTags(String text) {
        for (String[] ep : ENTITY_PATTERNS) {
            try {
                text = Pattern.compile(ep[0], Pattern.CASE_INSENSITIVE).matcher(text).replaceAll(ep[1]);
            } catch (Exception e) { /* skip bad pattern */ }
        }
        text = text.replaceAll("\\[([A-Z_]+): ([^\\[\\]]*?)\\[\\1: ([^\\]]+)\\]([^\\]]*)\\]", "[$1: $2$3$4]");
        text = text.replaceAll("\\[([A-Z_]+): \\1: ", "[$1: ");
        text = text.replaceAll("\\[\\[", "[");
        text = text.replaceAll("\\]\\]", "]");
        return text;
    }

    private static String assembleAndCompress(List<String[]> sections, int maxChars) {
        StringBuilder sb = new StringBuilder();
        for (String[] sec : sections) {
            String content = compressSection(sec[1].trim());
            if (content.isEmpty()) continue;
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("--- ").append(sec[0]).append(" ---\n").append(content);
        }
        String result = sb.toString();
        if (result.length() > maxChars) {
            int cutAt = result.lastIndexOf('\n', maxChars);
            result = result.substring(0, cutAt > 0 ? cutAt : maxChars);
            logger.error("[LLM ENHANCER] Text compressed to " + result.length() + " chars.");
        }
        return result;
    }

    private static String compressSection(String text) {
        String[] lines = text.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.trim().matches("^[-_=*]{3,}$")) continue;
            String compressed = line;
            for (Pattern fp : FILLER_PATTERNS) {
                compressed = fp.matcher(compressed).replaceAll("");
            }
            compressed = compressed.trim();
            if (compressed.length() > 2) sb.append(compressed).append("\n");
        }
        return sb.toString().trim();
    }

    // -----------------------------------------------------------------------
    // VALIDATION & NORMALIZATION 
    // -----------------------------------------------------------------------

    public static String validateAndNormalize(String jsonString) {
        try {
            JsonNode data = MAPPER.readTree(jsonString);
            if (data == null || !data.isObject()) return null;

            ObjectNode result = JsonNodeFactory.instance.objectNode();

            for (String field : STRING_FIELDS) {
                result.put(field, coerceString(data.get(field)));
            }
            for (String field : ARRAY_FIELDS) {
                JsonNode val = data.get(field);
                result.set(field, (val != null && val.isArray())
                        ? val : JsonNodeFactory.instance.arrayNode());
            }
            for (String field : OBJECT_FIELDS) {
                JsonNode val = data.get(field);
                result.set(field, (val != null && val.isObject())
                        ? val : JsonNodeFactory.instance.objectNode());
            }

            result.set("skills",             normalizeStringArray(result.get("skills")));
            result.set("certifications",     normalizeStringArray(result.get("certifications")));
            result.set("careerProfile",      normalizeObjectArray(result.get("careerProfile"),      CAREER_KEYS));
            result.set("projectProfile",     normalizeObjectArray(result.get("projectProfile"),     PROJECT_KEYS));
            result.set("educationalProfile", normalizeObjectArray(result.get("educationalProfile"), EDUCATION_KEYS));
            result.set("publicationDetails", normalizeObjectArray(result.get("publicationDetails"), PUBLICATION_KEYS));
            result.set("trainings",             normalizeObjectListContainer(result.get("trainings"),             "trainingList", TRAINING_KEYS));
            result.set("awardsAndAchievements", normalizeObjectListContainer(result.get("awardsAndAchievements"), "awardList",    AWARD_KEYS));

            result.put("emailaddress", normalizeEmail(result.get("emailaddress").asText("")));
            result.put("phonenumber",  normalizePhone(result.get("phonenumber").asText("")));
            result.put("dateOfBirth",  normalizeDate(result.get("dateOfBirth").asText("")));
            result.put("name",         normalizeName(result));

            if (!isSufficientlyValid(result)) {
                logger.error("[LLM ENHANCER] Validation: output has insufficient data to be useful.");
                return null;
            }

            return MAPPER.writeValueAsString(result);

        } catch (Exception e) {
            logger.error("[LLM ENHANCER] Validation error: " + e.getMessage());
            return null;
        }
    }

    public static String validateAndNormalizeCustom(
            String jsonString, org.json.JSONObject userJson) {
        try {
            JsonNode data = MAPPER.readTree(jsonString);
            if (data == null || !data.isObject()) return null;

            ObjectNode result  = JsonNodeFactory.instance.objectNode();
            boolean hasAnyData = false;

            java.util.Iterator<String> keys = userJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JsonNode val = data.get(key);

                if (val == null || val.isNull()) {
                    result.put(key, "");
                } else if (val.isTextual() || val.isNumber()) {
                    String strVal = val.asText("").trim();
                    result.put(key, strVal);
                    if (!strVal.isEmpty()) hasAnyData = true;
                } else if (val.isArray()) {
                    result.set(key, val);
                    if (val.size() > 0) hasAnyData = true;
                } else if (val.isObject()) {
                    result.set(key, val);
                    if (val.size() > 0) hasAnyData = true;
                } else {
                    result.put(key, val.asText(""));
                }
            }

            if (!hasAnyData) {
                logger.error("[LLM ENHANCER] Custom validation: no data extracted for any requested key.");
                return null;
            }

            logger.error("[LLM ENHANCER] Custom validation passed with " + result.size() + " keys.");
            return MAPPER.writeValueAsString(result);

        } catch (Exception e) {
            logger.error("[LLM ENHANCER] Custom validation error: " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Normalization helpers 
    // -----------------------------------------------------------------------

    private static String coerceString(JsonNode val) {
        if (val == null || val.isNull()) return "";
        if (val.isTextual()) return val.asText().trim();
        if (val.isNumber())  return val.asText();
        return "";
    }

    private static ArrayNode normalizeStringArray(JsonNode val) {
        ArrayNode arr = JsonNodeFactory.instance.arrayNode();
        if (val != null && val.isArray()) {
            for (JsonNode item : val) {
                String s = coerceString(item);
                if (!s.isEmpty()) arr.add(s);
            }
        }
        return arr;
    }

    private static ArrayNode normalizeObjectArray(JsonNode val, String[] keys) {
        ArrayNode arr = JsonNodeFactory.instance.arrayNode();
        if (val != null && val.isArray()) {
            for (JsonNode item : val) {
                if (!item.isObject()) continue;
                ObjectNode norm = JsonNodeFactory.instance.objectNode();
                boolean hasData = false;
                for (String k : keys) {
                    String v = coerceString(item.get(k));
                    norm.put(k, v);
                    if (!v.isEmpty()) hasData = true;
                }
                if (hasData) arr.add(norm);
            }
        }
        return arr;
    }

    private static ObjectNode normalizeObjectListContainer(JsonNode val, String listKey, String[] itemKeys) {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        if (val != null && val.isObject()) {
            ArrayNode normalized = normalizeObjectArray(val.get(listKey), itemKeys);
            if (normalized.size() > 0) obj.set(listKey, normalized);
        }
        return obj;
    }

    private static String normalizeEmail(String email) {
        email = email.trim().toLowerCase(Locale.ROOT);
        return email.matches("[^@]+@[^@]+\\.[^@]+") ? email : "";
    }

    private static String normalizePhone(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        return phone.replaceAll("[^\\d+\\-\\s()]", "").trim();
    }

    private static String normalizeDate(String date) {
        if (date == null || date.isEmpty()) return "";
        if (date.matches("\\d{2}/\\d{2}/\\d{4}")) return date;
        Matcher m = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(date);
        if (m.matches()) return m.group(3) + "/" + m.group(2) + "/" + m.group(1);
        m = Pattern.compile("(\\d{2})-(\\d{2})-(\\d{4})").matcher(date);
        if (m.matches()) return m.group(1) + "/" + m.group(2) + "/" + m.group(3);
        return date;
    }

    private static String normalizeName(ObjectNode result) {
        String name = result.get("name").asText("");
        if (!name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder();
        for (String k : new String[]{"firstName", "middleName", "lastName"}) {
            String v = result.get(k).asText("");
            if (!v.isEmpty()) { if (sb.length() > 0) sb.append(" "); sb.append(v); }
        }
        return sb.toString().trim();
    }

    private static boolean isSufficientlyValid(ObjectNode result) {
        if (!result.get("name").asText("").isEmpty())         return true;
        if (!result.get("emailaddress").asText("").isEmpty()) return true;
        if (!result.get("phonenumber").asText("").isEmpty())  return true;
        if (result.get("careerProfile").size() > 0)           return true;
        if (result.get("educationalProfile").size() > 0)      return true;
        return false;
    }

    // -----------------------------------------------------------------------
    // OUTPUT FILE WRITING 
    // -----------------------------------------------------------------------

    public static String[] writeOutputFiles(String validatedJson, String outputFolderPath, String uniqueId) {
        try {
            File outDir = new File(outputFolderPath);
            if (!outDir.exists()) outDir.mkdirs();

            String jsonPath = new File(outDir, "cv_output_" + uniqueId + ".json").getAbsolutePath();
            String xmlPath  = new File(outDir, "cv_output_" + uniqueId + ".xml").getAbsolutePath();

            try (Writer w = new OutputStreamWriter(new FileOutputStream(jsonPath), StandardCharsets.UTF_8)) {
                w.write(validatedJson);
            }
            String xmlStr = toXmlString(validatedJson);
            try (Writer w = new OutputStreamWriter(new FileOutputStream(xmlPath), StandardCharsets.UTF_8)) {
                w.write(xmlStr);
            }

            File jf = new File(jsonPath);
            File xf = new File(xmlPath);
            if (!jf.exists() || jf.length() < 2 || !xf.exists() || xf.length() < 2) {
                logger.error("[LLM ENHANCER] Output file verification failed.");
                return null;
            }

            return new String[]{jsonPath, xmlPath};

        } catch (Exception e) {
            logger.error("[LLM ENHANCER] File write error: " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // XML CONVERSION 
    // -----------------------------------------------------------------------

    public static String toXmlString(String jsonString) {
        try {
            JsonNode root = MAPPER.readTree(jsonString);
//            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try{
	            // Disable XXE
	            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
	            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
	            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
	            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	            
	            // Secure processing
	            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            }catch (Exception fe) {
                logger.error("[LLM ENHANCER] XXE feature set error: " + fe.getMessage());
            }
            
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document doc = factory.newDocumentBuilder().newDocument();
            
            Element rootEl = doc.createElement("CVData");
            doc.appendChild(rootEl);
            buildXml(doc, rootEl, root);

            javax.xml.transform.Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter sw = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();

        } catch (Exception e) {
            logger.error("[LLM ENHANCER] XML conversion error: " + e.getMessage());
            return "<CVData/>";
        }
    }

    private static void buildXml(Document doc, Element parent, JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String tag = safeTag(entry.getKey());
                Element child = doc.createElement(tag);
                parent.appendChild(child);
                buildXml(doc, child, entry.getValue());
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                Element itemEl = doc.createElement("item");
                parent.appendChild(itemEl);
                buildXml(doc, itemEl, item);
            }
        } else {
            parent.setTextContent(node.asText(""));
        }
    }

    private static String safeTag(String tag) {
        tag = tag.replace(" ", "_");
        if (!tag.isEmpty() && Character.isDigit(tag.charAt(0))) tag = "_" + tag;
        return tag;
    }
}

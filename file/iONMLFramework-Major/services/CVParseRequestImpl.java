package com.tcsion.ml.Services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.Beans.RinxBean;
import com.tcsion.ml.Utils.ExceptionLogUtility;
import com.tcsion.ml.Utils.MlFrameworkConstants;

public class CVParseRequestImpl {

    private final Log logger = LogFactory.getLog(CVParseRequestImpl.class);

    private String jobId = "";
    String output = null;
    private RinxBean rinxBean = null;
    public static final int IONML_CVPARSING_GPT4_1_SERVICE_ID = 10;

    public String processApiByUseCase(final JSONObject inputDetailsJson, JSONObject outputJson) throws Exception {

        logger.error("CVParseRequestImpl -> processApiByUseCase invoked ");
        String jsonExecutionOutput = MlFrameworkConstants.COULD_NOT_EXECUTE;
        String xmlExecutionOutput  = MlFrameworkConstants.COULD_NOT_EXECUTE;

        // Usage map for token audit logging 
        HashMap<String, Object> usageDetailsMap = new HashMap<>();

        this.logger.error("InputDetailsJson received | keys=" + inputDetailsJson.length());
        this.logger.error("OutputJson received | keys=" + outputJson.length());

        if (inputDetailsJson != null) {
            try {
                logger.error("Inside Rinx use Case {}");

                String useCaseId = inputDetailsJson.getString("useCaseID");
                logger.error("useCaseId : " + useCaseId);

                String appId = inputDetailsJson.getString("appId");
                logger.error("appId: " + appId);

                String apiID = inputDetailsJson.getString("apiID");
                logger.error("apiID: " + apiID);

                int port = inputDetailsJson.getInt("port");
                logger.error("portnumber: " + port);

                jobId = inputDetailsJson.getString("jobId");
                logger.error("jobId: " + jobId);

                String orgId = inputDetailsJson.getString("orgId");
                logger.error("orgId: " + orgId);

                // Initialise usage map - tokens populated later inside CVLlmEnhancer after LLM call
                usageDetailsMap = setUsageDetailsMap(useCaseId, appId, apiID, orgId);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readValue(inputDetailsJson.getString("requestInput"), JsonNode.class);
                logger.error("RequestInput: " + node);

                String status  = null;
                String message = null;

                String inputFilePath    = node.get("inputFilePath").textValue();
                String outputFolderPath = node.get("outputFolderPath").textValue();

                
                // vendorCredMap - built by ML Framework from tcs_ion_ai_framework_configurations
                // Structure: { "VENDOR_SERVICE_NAME": { "serviceId": "N", ...decrypted creds... } }
                // The key (vendor service name) is service_name from gbl_ai_commercial_vendor_service_master
                // We never hardcode vendor names - we resolve them by matching serviceId below
                JsonNode vendorCredMap = null;
                JsonNode root = mapper.readTree(inputDetailsJson.toString());
                if (root.has("vendorCredMap") && !root.get("vendorCredMap").isNull()) {
                    vendorCredMap = root.get("vendorCredMap");
                    logger.error("[CVParseRequestImpl] vendorCredMap present: "
                    	    + (vendorCredMap != null) 
                    	    + " | vendors=" 
                    	    + (vendorCredMap != null ? vendorCredMap.size() : 0));
                } else {
                    logger.error("[CVParseRequestImpl] vendorCredMap absent - LLM pipeline will be skipped if enabled");
                }
                
                // -----------------------------------------------------------------------
                // userRegisteredServiceId - sent model selection 
                // Arrives in requestInput just like userJson
                // Maps to id in gbl_ai_commercial_vendor_service_master
                // Absent = no explicit model chosen, takes first by default , flag governs as normal
                // -----------------------------------------------------------------------

                int userRegisteredServiceId = -1; // -1 = not provided by caller
                if (node.has("userRegisteredServiceId") && !node.get("userRegisteredServiceId").isNull()) {
                   String svcIdStr = node.get("userRegisteredServiceId").asText("").trim();
                   if (!svcIdStr.isEmpty() && !svcIdStr.equals("null")) {
                       userRegisteredServiceId = node.get("userRegisteredServiceId").asInt(-1);
                   }
                }
                // If caller did not send serviceId � resolve from vendorCredMap
                if (userRegisteredServiceId == -1) {
                   if (vendorCredMap != null && vendorCredMap.fields().hasNext()) {
                       Map.Entry<String, JsonNode> firstVendor = vendorCredMap.fields().next();
                       JsonNode firstVendorNode = firstVendor.getValue();
                       if (firstVendorNode != null && !firstVendorNode.isNull()
                && firstVendorNode.has(GenAIFrameworkConstants.SERVICE_ID)) {
                           userRegisteredServiceId = firstVendorNode
                                   .get(GenAIFrameworkConstants.SERVICE_ID).asInt(-1);
                           logger.error("[CVParseRequestImpl] userRegisteredServiceId resolved from vendorCredMap"
                                   + " | firstVendor=" + firstVendor.getKey()
                                   + " | resolvedServiceId=" + userRegisteredServiceId);
                       }
                   } else {
                       logger.error("[CVParseRequestImpl] userRegisteredServiceId null and vendorCredMap null/empty"
                               + " � will fall to legacy RINX");
                       userRegisteredServiceId = -1;
                   }
                }
                logger.error("[CVParseRequestImpl] userRegisteredServiceId=" + userRegisteredServiceId);

                // Optional: custom extraction schema { "fieldName": "description", ... }
                // null -> full default TCSiON schema 
                JSONObject userJson = null;
                JsonNode userJsonNode = node.get("userJson");
                
                if (userJsonNode != null && !userJsonNode.isNull()) {

                    try {
                        String userJsonText = node.get("userJson").asText();

                        Object parsed = new org.json.JSONTokener(userJsonText).nextValue();

                        if (parsed instanceof org.json.JSONObject) {
                            // Case 1: JSON Object
                            userJson = (org.json.JSONObject) parsed;

                        } else if (parsed instanceof org.json.JSONArray) {
                            // Case 2: JSON Array - take first object
                            org.json.JSONArray arr = (org.json.JSONArray) parsed;

                            if (arr.length() > 0 && arr.get(0) instanceof org.json.JSONObject) {
                                userJson = arr.getJSONObject(0);
                            } else {
                                logger.error("[CVParseRequestImpl] userJson array is empty or not valid JSONObjects");
                            }
                        }

                        if (userJson != null) {
                            logger.error("[CVParseRequestImpl] userJson received with " + userJson.length() + " keys");
                        }

                    } catch (Exception uje) {
                        logger.error("[CVParseRequestImpl] Failed to parse userJson, ignoring | error=" + uje.getMessage());
                        userJson = null;
                    }
                }

                

                // -----------------------------------------------------------------------
                // Resolve selectedVendorName from vendorCredMap
                //
                // vendorCredMap already carries serviceId inside each vendor node.
                // The key of the matching entry IS the vendor name - set by the framework.
                // No hardcoding needed. If a new vendor is onboarded, zero code change here.
                //
                // Returns null if:
                //   - vendorCredMap is null
                //   - no vendor node carries a matching serviceId
                // Null is handled safely in Rinx - falls through to RINX legacy call.
                // -----------------------------------------------------------------------
                String selectedVendorName = null;
                if (vendorCredMap != null) {
                    Iterator<Map.Entry<String, JsonNode>> vendors = vendorCredMap.fields();
                    while (vendors.hasNext()) {
                        Map.Entry<String, JsonNode> entry = vendors.next();
                        JsonNode vendorNode = entry.getValue();
                        if (vendorNode != null && !vendorNode.isNull()) {
                            JsonNode svcId = vendorNode.get(GenAIFrameworkConstants.SERVICE_ID);
                            if (svcId != null && !svcId.isNull()
                                    && svcId.asInt(0) == userRegisteredServiceId) {
                                selectedVendorName = entry.getKey();
                                break;
                            }
                        }
                    }
                }
                logger.error("[CVParseRequestImpl] selectedVendorName=" + selectedVendorName
                        + " | userRegisteredServiceId=" + userRegisteredServiceId);

                if ((inputFilePath == null) || inputFilePath.equals("")) {
                    status  = "-1";
                    message = "Provide valid values/nonempty params";
                }

                if ((outputFolderPath == null) || outputFolderPath.equals("")) {
                    status  = "-1";
                    message = "Provide valid values/nonempty params";
                } else {
                    logger.error(" Folder Path:" + inputFilePath);

                    // -----------------------------------------------------------------------
                    // LLM enabled decision
                    //
                    // GPT 4.1 (userRegisteredServiceId == IONML_CVPARSING_GPT4_1_SERVICE_ID):
                    //   User explicitly chose GPT — llmEnabled = 1 always, flag not checked.
                    //   IONML_CVPARSING_GPT4_1_SERVICE_ID is declared in MlFrameworkConstants.
                    //   It identifies which service ID bypasses the flag. 
                    //   Vendor name itself comes from vendorCredMap above.
                    //
                    // Mistral (id=11) or default (id=11/absent):
                    //   Read IONML_CVPARSING_LLM_FLAG from textLimitMap.
                    //   flag=1 -> LLM runs with selectedVendorName (or falls to RINX if null)
                    //   flag=0 -> RINX runs directly
                    // -----------------------------------------------------------------------
//                    int llmEnabled = 0;
//
//                    if (userRegisteredServiceId == IONML_CVPARSING_GPT4_1_SERVICE_ID) {
//                        llmEnabled = 1;
//                        logger.error("[CVParseRequestImpl] GPT 4.1 selected — llmEnabled=1, flag bypassed"
//                                + " | vendor=" + selectedVendorName);
//                    } else {
//                        final JSONObject limitsMap = inputDetailsJson.getJSONObject("textLimitMap");
//                        final Map<String, String> textLimitsMap =
//                                mapper.readValue(limitsMap.toString(),
//                                        new TypeReference<HashMap<String, String>>() {});
//
//                        if (textLimitsMap.containsKey(MlFrameworkConstants.IONML_CVPARSING_LLM_FLAG)) {
//                            llmEnabled = Integer.parseInt(
//                                    textLimitsMap.get(MlFrameworkConstants.IONML_CVPARSING_LLM_FLAG));
//                        }
//                        logger.error("[CVParseRequestImpl] Mistral/default path — llmEnabled=" + llmEnabled
//                                + " | vendor=" + selectedVendorName
//                                + " | userRegisteredServiceId=" + userRegisteredServiceId);
//                    }
                    int llmEnabled = 0;
                    if (userRegisteredServiceId == -1) {
                       // vendorCredMap was null/empty � no credentials available
                       llmEnabled = 0;
                       logger.error("[CVParseRequestImpl] No serviceId resolved and vendorCredMap empty"
                               + " � llmEnabled=0, falling to legacy RINX");
                    } else if (userRegisteredServiceId == IONML_CVPARSING_GPT4_1_SERVICE_ID) {
                       llmEnabled = 1;
                       logger.error("[CVParseRequestImpl] GPT 4.1 selected � llmEnabled=1, flag bypassed"
                               + " | vendor=" + selectedVendorName);
                    } else {

                        final JSONObject limitsMap = inputDetailsJson.getJSONObject("textLimitMap");
                        final Map<String, String> textLimitsMap =
                                mapper.readValue(limitsMap.toString(),
                                        new TypeReference<HashMap<String, String>>() {});

                        // Check org-specific flag first e.g. "ionml.CVParseUC.llmFlag.726"
                        // then fall back to global flag "ionml.CVParseUC.llmFlag"
                        
                        String orgSpecificFlagKey = MlFrameworkConstants.IONML_CVPARSING_LLM_FLAG + "." + orgId;

                        if (textLimitsMap.containsKey(orgSpecificFlagKey)) {
                            llmEnabled = Integer.parseInt(textLimitsMap.get(orgSpecificFlagKey));
                            logger.error("[CVParseRequestImpl] LLM flag read from org-specific key="
                                            + orgSpecificFlagKey
                                            + " | llmEnabled="
                                            + llmEnabled);

                        } else if (textLimitsMap.containsKey(MlFrameworkConstants.IONML_CVPARSING_LLM_FLAG)) {
                            llmEnabled = Integer.parseInt(textLimitsMap.get(MlFrameworkConstants.IONML_CVPARSING_LLM_FLAG));

                            logger.error("[CVParseRequestImpl] LLM flag read from global key="
                                            + MlFrameworkConstants.IONML_CVPARSING_LLM_FLAG
                                            + " | llmEnabled="
                                            + llmEnabled);
                            
                        } else {
                        	logger.error("[CVParseRequestImpl] LLM flag not found in textLimitMap "
                                            + "� llmEnabled stays 0, falling to legacy RINX");
                        }

                        logger.error("[CVParseRequestImpl] Non-GPT path � llmEnabled="
                                        + llmEnabled
                                        + " | vendor="
                                        + selectedVendorName
                                        + " | userRegisteredServiceId="
                                        + userRegisteredServiceId);
                    }             

                    Rinx rinxObj = new Rinx();
                    rinxBean = rinxObj.rinxAsyncFlask(
                            inputFilePath, outputFolderPath, port,
                            llmEnabled, userJson, vendorCredMap,
                            useCaseId, appId, apiID, orgId,
                            selectedVendorName, usageDetailsMap);

                    logger.error("Rinx async api called : {}");
                    status  = rinxBean.getStatus();
                    message = rinxBean.getMessage();
                    jsonExecutionOutput = rinxBean.getJsonOutput();
                    xmlExecutionOutput  = rinxBean.getXmlOutput();
                }

                logger.error("Rinx async api called Status: {} " + status);
                logger.error("Rinx async api called Called executionOutput: {} " + jsonExecutionOutput + " " + xmlExecutionOutput);
                logger.error("Rinx async api called Messege: {} " + message);

                if (jsonExecutionOutput != null && xmlExecutionOutput != null) {
                    output = this.prepareResponse(status, message,
                            jsonExecutionOutput, xmlExecutionOutput, outputJson, usageDetailsMap);
                } else {
                    output = this.prepareResponseWithOldMethod(message, apiID);
                }

                this.logger.error("**THE OUTPUT IS**: " + output);

            } catch (Exception e) {

                JSONObject finalJson = new JSONObject();
                finalJson.put("RequestStatus", "Failed");
                finalJson.put("jsonResultFilePath", jsonExecutionOutput);
                finalJson.put("xmlResultFilePath", xmlExecutionOutput);
                finalJson.put("ReturnCode", 100);

//                JSONArray usageAuditDetailsArray = new JSONArray();
//                usageAuditDetailsArray.put(new JSONObject(usageDetailsMap));
//                finalJson.put("aimlAuditUsageDetails", usageAuditDetailsArray);

                logger.error(this.jobId + " ***** Error Occured while Rinx method Call ***** ");
                ExceptionLogUtility.logException(
                        this.jobId + " ***** Error Occured while Rinx method Call ***** ", e, logger);

                return finalJson.toString();
            }
        }

        return output;
    }

    // -----------------------------------------------------------------------
    // Usage map initialiser
    // Tokens populated later inside CVLlmEnhancer after LLM call
    // -----------------------------------------------------------------------
    private HashMap<String, Object> setUsageDetailsMap(
            String useCaseId, String appId, String apiID, String orgId) {

        HashMap<String, Object> map = new HashMap<>();
        map.put("usecaseId", useCaseId);
        map.put("appId",     appId);
        map.put("apidId",    apiID);
        map.put("orgId",     orgId);
        map.put("promptTokens",     0);
        map.put("completionTokens", 0);
        map.put("totalTokens",      0);
        map.put("engineId",           "");
        return map;
    }

    private String prepareResponseWithOldMethod(final String executionOutput, final String apiID)
            throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Map<Object, Object> hs1 = new HashMap();
        final Map<Object, Object> hs2 = new HashMap();
        hs2.put("RequestStatus", executionOutput);
        hs1.put("apiResultData", hs2);
        hs1.put("apiID", apiID);
        return mapper.writeValueAsString(hs1);
    }

    private String prepareResponse(final String status_code, final String messege,
            final String jsonExecutionOutput, final String xmlExecutionOutput,
            final JSONObject outputJson,
            final HashMap<String, Object> usageDetailsMap)
            throws IOException, JSONException {

        this.logger.error("Inside Prepare Response. Execution Output: {} " + jsonExecutionOutput + " Output JSON: {} " + outputJson);
        this.logger.error("Inside Prepare Response. Execution Output: {} " + xmlExecutionOutput  + " Output JSON: {} " + outputJson);
        this.logger.error("output json " + outputJson.toString());

        if (outputJson.has("RequestStatus")) {
            logger.error("Output json has RequestStatus");
            String fieldName        = outputJson.getString("RequestStatus");
            String replaceString    = fieldName.replace("<", "");
            String replaceStringNew = replaceString.replace(">", "");
            outputJson.remove("RequestStatus");
            outputJson.put(replaceStringNew, messege);
        } else {
            logger.error("Missing RequestStatus from Output json");
        }

        if (outputJson.has("xmlResultFilePath")) {
            logger.error("Output json has xmlResultFilePath");
            String fieldName        = outputJson.getString("xmlResultFilePath");
            String replaceString    = fieldName.replace("<", "");
            String replaceStringNew = replaceString.replace(">", "");
            outputJson.remove("xmlResultFilePath");
            outputJson.put(replaceStringNew, xmlExecutionOutput);
        } else {
            logger.error("Missing xmlResultFilePath from Output json");
        }

        if (outputJson.has("jsonResultFilePath")) {
            logger.error("Output json has jsonResultFilePath");
            String fieldName        = outputJson.getString("jsonResultFilePath");
            String replaceString    = fieldName.replace("<", "");
            String replaceStringNew = replaceString.replace(">", "");
            outputJson.remove("jsonResultFilePath");
            outputJson.put(replaceStringNew, jsonExecutionOutput);
        } else {
            logger.error("Missing jsonResultFilePath from Output json");
        }

        if (outputJson.has("StatusCode")) {
            logger.error("Output json has StatusCode");
            String fieldName        = outputJson.getString("StatusCode");
            String replaceString    = fieldName.replace("<", "");
            String replaceStringNew = replaceString.replace(">", "");
            outputJson.remove("StatusCode");
            outputJson.put(replaceStringNew, status_code);
        } else {
            logger.error("Missing StatusCode from Output json");
        }

        if (outputJson.has("Return_Message")) {
            logger.error("Output json has Return_Message");
            String fieldName        = outputJson.getString("Return_Message");
            String replaceString    = fieldName.replace("<", "");
            String replaceStringNew = replaceString.replace(">", "");
            outputJson.remove("Return_Message");
            outputJson.put(replaceStringNew, messege);
        } else {
            logger.error("Missing Return_Message from Output json");
        }

//        // Append token audit usage
//        JSONArray usageAuditDetailsArray = new JSONArray();
//        usageAuditDetailsArray.put(new JSONObject(usageDetailsMap));
//        outputJson.put("aimlAuditUsageDetails", usageAuditDetailsArray);
//        logger.error("aimlAuditUsageDetails appended: " + usageAuditDetailsArray.toString());

        return outputJson.toString();
    }
}

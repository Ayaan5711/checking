package com.tcs.genai.engine.adapter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.genai.engine.utils.EngineConstants;
import com.tcs.genai.engine.utils.GeminiErrorResponseHandler;
import com.tcs.genai.engine.utils.GeminiHTTPCall;
import com.tcs.genai.gemini.model.Content;
import com.tcs.genai.gemini.model.GenerateContentRequest;
import com.tcs.genai.gemini.model.GenerateContentResponse;
import com.tcs.genai.gemini.model.GenerationConfig;
import com.tcs.genai.gemini.model.Part;
import com.tcs.genai.gemini.model.SafetySetting;
import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapterFactory;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

/**
 * Adapter that invokes the Vertex AI Gemini generateContent endpoint.
 *
 * <p>Follows the exact same flow as {@link AzureAIEngineInvoker}:
 * <ol>
 *   <li>Extract vendor name and connection properties from {@code vendorProps}.</li>
 *   <li>Build the {@link GenerateContentRequest} from the incoming payload.</li>
 *   <li>POST via {@link GeminiHTTPCall} (Bearer-token auth, uses {@code GcpCredentialsProvider}).</li>
 *   <li>Retry with exponential back-off on non-200 responses.</li>
 *   <li>Map HTTP status to engine codes via {@link GeminiErrorResponseHandler}.</li>
 *   <li>Parse {@code candidates[0].content.parts[0].text} from the response.</li>
 *   <li>Extract {@code usageMetadata} token counts.</li>
 *   <li>Return a standardised {@link JSONObject} with the same keys as Azure.</li>
 * </ol>
 *
 * <p>Vendor properties expected in the inner map under key {@code GEMINI_VERTEX_AI}:
 * <ul>
 *   <li>{@code GEMINI_PROJECT_ID}   – GCP project ID</li>
 *   <li>{@code GEMINI_LOCATION}     – Vertex AI location (default: {@code global})</li>
 *   <li>{@code GEMINI_MODEL_ID}     – Model ID (e.g. {@code gemini-2.0-flash-001})</li>
 *   <li>{@code GEMINI_CREDENTIALS}  – Absolute path to the GCP service-account JSON key file</li>
 *   <li>{@code GEMINI_TEMPERATURE}  – (optional) sampling temperature; default 0.9</li>
 *   <li>{@code GEMINI_MAX_TOKENS}   – (optional) max output tokens; default 2048</li>
 *   <li>{@code GEMINI_TOP_P}        – (optional) top-P; default 0.95</li>
 * </ul>
 *
 * <p>Payload JSON keys:
 * <ul>
 *   <li>{@code "prompt"}       – user message (required)</li>
 *   <li>{@code "systemPrompt"} – system instruction (optional)</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
public class GeminiEngineInvoker implements EngineInvokerAdapter {

    private static final Log logger = LogFactory.getLog(VendorPayloadAdapterFactory.class);

    private static final double DEFAULT_TEMPERATURE = 0.9;
    private static final int    DEFAULT_MAX_TOKENS  = 2048;
    private static final double DEFAULT_TOP_P       = 0.95;

    // Configurable retry parameters — matches AzureAIEngineInvoker defaults
    private static final int MAX_RETRIES    = 3;
    private static final int BASE_DELAY_MS  = 10000; // 10-second base delay

    

    @Override
    public JSONObject invokeAiEngine(JSONObject payload,
            Map<String, HashMap<String, String>> vendorProps) throws JSONException, NullPointerException {

        JSONObject finalVendorResultMap = new JSONObject();
        String engineStatusCode    = null;
        String engineStatusMessage = null;
        String vendorName          = null;
        String vendorActualMessage = null;
        Map<String, String> propertiesValueMap = new HashMap<>();

        // --- 1. Extract vendor name and properties ---
        for (Map.Entry<String, HashMap<String, String>> entry : vendorProps.entrySet()) {
            vendorName = entry.getKey();
            logger.info("GeminiEngineInvoker [vendorName]: " + vendorName);
        }
        propertiesValueMap = vendorProps.get(vendorName);

        String projectId   = propertiesValueMap.get(EngineConstants.GEMINI_PROJECT_ID);
        String location    = propertiesValueMap.getOrDefault(EngineConstants.GEMINI_LOCATION, "global");
        String modelId     = propertiesValueMap.get(EngineConstants.GEMINI_MODEL_ID);
        String credentials = propertiesValueMap.get(EngineConstants.GEMINI_CREDENTIALS);
        double temperature = parseDouble(propertiesValueMap.get(EngineConstants.GEMINI_TEMPERATURE), DEFAULT_TEMPERATURE);
        int    maxTokens   = parseInt(propertiesValueMap.get(EngineConstants.GEMINI_MAX_TOKENS),    DEFAULT_MAX_TOKENS);
        double topP        = parseDouble(propertiesValueMap.get(EngineConstants.GEMINI_TOP_P),       DEFAULT_TOP_P);

        // --- 2. Build Gemini request JSON ---
        String requestJson;
        try {
            String userPromptText   = (String) payload.get("prompt");
            String systemPromptText = payload.get("systemPrompt") != null
                    ? (String) payload.get("systemPrompt") : "";

            Content userContent    = new Content("user", Arrays.asList(new Part(userPromptText)));
            Content systemContent  = new Content(Arrays.asList(new Part(systemPromptText)));
            GenerationConfig genConfig = new GenerationConfig(temperature, maxTokens, topP);
            GenerateContentRequest request = new GenerateContentRequest(
                    Arrays.asList(userContent), systemContent, genConfig, defaultSafetySettings());

            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            requestJson = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            logger.error(GenAIFrameworkConstants.LOGGER_ERROR
                    + " [GeminiEngineInvoker] Failed to build request JSON: " + e);
            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, "");
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, "Failed to build Gemini request: " + e.getMessage());
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, "ENGINE_800");
            return finalVendorResultMap;
        }

        // --- 3. Build endpoint URL and invoke via GeminiHTTPCall ---
        String endpointUrl = String.format(
                "https://aiplatform.googleapis.com/v1/projects/%s/locations/%s"
                + "/publishers/google/models/%s:generateContent",
                projectId, location, modelId);

        logger.info(GenAIFrameworkConstants.LOGGER_INFO
                + " [GeminiEngineInvoker] Calling Vertex AI endpoint: " + endpointUrl);

        GeminiHTTPCall geminiHit = new GeminiHTTPCall(endpointUrl, credentials, requestJson);
        JSONObject vendorResponse = geminiHit.makeCall();
        int responseCode = (int) vendorResponse.get(GenAIFrameworkConstants.STATUS_CODE);

        logger.error(GenAIFrameworkConstants.LOGGER_DEBUG
                + " [GeminiEngineInvoker] First-time hit to Vertex AI. Response code: " + responseCode);

        // --- 4. Retry on non-200 ---
        if (responseCode != HttpStatus.SC_OK) {
            try {
                vendorResponse = geminiHit.retryWithBackoff(MAX_RETRIES, BASE_DELAY_MS);
            } catch (InterruptedException e) {
                logger.error(GenAIFrameworkConstants.LOGGER_ERROR
                        + " [GeminiEngineInvoker] Exception during retry backoff: " + e);
            }
        }

        responseCode = (int) vendorResponse.get(GenAIFrameworkConstants.STATUS_CODE);
        String response = (String) vendorResponse.get(GenAIFrameworkConstants.VENDOR_RESPONSE);

        // --- 5. Map HTTP status to engine codes ---
        GeminiErrorResponseHandler geminiErrorHandler = new GeminiErrorResponseHandler();
        Map<String, String> engineStatus = geminiErrorHandler.errorMessageForStatus(responseCode);
        engineStatusCode    = engineStatus.get(EngineConstants.ENGINE_RETURN_CODE);
        engineStatusMessage = engineStatus.get(EngineConstants.ENGINE_RETURN_MESSAGE);
        vendorActualMessage = (String) vendorResponse.get(GenAIFrameworkConstants.STATUS_MESSAGE);

        // --- 6. Parse response ---
        if (response == null || response.isEmpty()) {
            logger.error(GenAIFrameworkConstants.LOGGER_ERROR
                    + " [GeminiEngineInvoker] Received response is null or empty!");
            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, response);
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, engineStatusMessage);
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, engineStatusCode);
            finalVendorResultMap.put("geminiResponse", response);
            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_ACTUAL_MESSAGE, vendorActualMessage);
        } else {
            try {
                ObjectMapper objectMapper = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                GenerateContentResponse geminiResponse =
                        objectMapper.readValue(response, GenerateContentResponse.class);

                // --- Check promptFeedback for Model Armor prompt-level blocking ---
                // When Model Armor or Gemini safety blocks the PROMPT itself (before any
                // candidate is generated), the response has no candidates but contains
                // promptFeedback.blockReason (e.g. "MODEL_ARMOR", "SAFETY").
                if (geminiResponse.getPromptFeedback() != null
                        && geminiResponse.getPromptFeedback().getBlockReason() != null) {
                    String blockReason        = geminiResponse.getPromptFeedback().getBlockReason();
                    String blockReasonMessage = geminiResponse.getPromptFeedback().getBlockReasonMessage();
                    Map<String, String> blockStatus = geminiErrorHandler.errorMessageForPromptBlockReason(blockReason, blockReasonMessage);
                    engineStatusCode    = blockStatus.get(EngineConstants.ENGINE_RETURN_CODE);
                    engineStatusMessage = blockStatus.get(EngineConstants.ENGINE_RETURN_MESSAGE);
                    logger.error(GenAIFrameworkConstants.LOGGER_ERROR
                            + " [GeminiEngineInvoker] Prompt blocked. blockReason=" + blockReason
                            + ", message=" + blockReasonMessage
                            + ", engineCode=" + engineStatusCode);
                }
                // --- Check finishReason for Model Armor / safety output blocking ---
                // A HTTP 200 response can still be a blocked response when Model Armor or
                // Gemini safety filters act on the generated output. In that case the
                // candidate content is empty and finishReason identifies the block type.
                if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                    String finishReason = geminiResponse.getCandidates().get(0).getFinishReason();
                    boolean isBlocked = finishReason != null && (
                            finishReason.equals(EngineConstants.GEMINI_FINISH_REASON_BLOCKLIST)
                         || finishReason.equals(EngineConstants.GEMINI_FINISH_REASON_PROHIBITED_CONTENT)
                         || finishReason.equals(EngineConstants.GEMINI_FINISH_REASON_SPII)
                         || finishReason.equals(EngineConstants.GEMINI_FINISH_REASON_SAFETY)
                         || finishReason.equals(EngineConstants.GEMINI_FINISH_REASON_RECITATION));

                    if (isBlocked) {
                        Map<String, String> blockStatus = geminiErrorHandler.errorMessageForFinishReason(finishReason);
                        engineStatusCode    = blockStatus.get(EngineConstants.ENGINE_RETURN_CODE);
                        engineStatusMessage = blockStatus.get(EngineConstants.ENGINE_RETURN_MESSAGE);
                        logger.error(GenAIFrameworkConstants.LOGGER_ERROR
                                + " [GeminiEngineInvoker] Response blocked. finishReason=" + finishReason
                                + ", engineCode=" + engineStatusCode);
                    } else {
                        // For non-blocking finish reasons, still log for diagnostics
                        logger.info(GenAIFrameworkConstants.LOGGER_INFO
                                + " [GeminiEngineInvoker] Gemini finishReason: " + finishReason);
                    }
                }

                String vendor_response = geminiResponse.getFirstCandidateText();

                logger.info(GenAIFrameworkConstants.LOGGER_INFO
                        + " [GeminiEngineInvoker] Gemini response parsed successfully.");

                finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, vendor_response);
                finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, engineStatusMessage);
                finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, engineStatusCode);
                finalVendorResultMap.put("geminiResponse", response);
                finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_ACTUAL_MESSAGE, vendorActualMessage);

            } catch (Exception e) {
                logger.error(GenAIFrameworkConstants.LOGGER_ERROR
                        + " [GeminiEngineInvoker] Exception parsing vendor response: " + e);
                finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, "");
                finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, engineStatusMessage);
                finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, engineStatusCode);
                finalVendorResultMap.put("geminiResponse", response);
                finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_ACTUAL_MESSAGE, vendorActualMessage);
            }

            // --- 7. Extract token usage from usageMetadata ---
            try {
                JSONObject tokenJson = extractTokenUsage(response);
                if (tokenJson != null) {
                    finalVendorResultMap.putAll(tokenJson);
                } else {
                    logger.error(GenAIFrameworkConstants.LOGGER_ERROR
                            + " [GeminiEngineInvoker] Token result is empty.");
                }
            } catch (Exception e) {
                logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION
                        + " [GeminiEngineInvoker] Exception during token extraction: " + e);
            }
        }

        return finalVendorResultMap;
    }

    /**
     * Extracts token usage counts from the Gemini {@code usageMetadata} in the raw response JSON.
     * Returns a {@link JSONObject} with a {@code "tokens"} sub-object containing
     * {@code inputTokens}, {@code outputTokens}, and {@code totalTokens} —
     * matching the structure produced by {@link AzureAIEngineInvoker#extractTokenUsage}.
     */
    public JSONObject extractTokenUsage(String rawResponseJson) {
        long inputTokens  = 0L;
        long outputTokens = 0L;
        long totalTokens  = 0L;

        try {
            if (rawResponseJson != null && !rawResponseJson.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                GenerateContentResponse geminiResponse =
                        mapper.readValue(rawResponseJson, GenerateContentResponse.class);

                if (geminiResponse.getUsageMetadata() != null) {
                    inputTokens  = geminiResponse.getUsageMetadata().getPromptTokenCount();
                    outputTokens = geminiResponse.getUsageMetadata().getCandidatesTokenCount();
                    totalTokens  = geminiResponse.getUsageMetadata().getTotalTokenCount();
                }
            }
        } catch (Exception e) {
            logger.error("[GeminiEngineInvoker][Token Usage] Failed to extract token usage: " + e);
        }

        JSONObject tokensJson = new JSONObject();
        tokensJson.put("inputTokens",  inputTokens);
        tokensJson.put("outputTokens", outputTokens);
        tokensJson.put("totalTokens",  totalTokens);

        JSONObject result = new JSONObject();
        result.put("tokens", tokensJson);
        return result;
    }

    @Override
    public JSONObject invokeEngine(String sourceDoc, String extractionType,
            Map<String, HashMap<String, String>> vendorProps)
            throws JSONException, NullPointerException {
        return null;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static List<SafetySetting> defaultSafetySettings() {
        return Arrays.asList(
            SafetySetting.of(SafetySetting.HarmCategory.HARM_CATEGORY_HATE_SPEECH,
                             SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH),
            SafetySetting.of(SafetySetting.HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
                             SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH),
            SafetySetting.of(SafetySetting.HarmCategory.HARM_CATEGORY_HARASSMENT,
                             SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH),
            SafetySetting.of(SafetySetting.HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT,
                             SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
        );
    }

    private static double parseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return defaultValue; }
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return defaultValue; }
    }
}

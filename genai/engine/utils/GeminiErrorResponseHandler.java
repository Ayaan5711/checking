package com.tcs.genai.engine.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

/**
 * Maps Vertex AI Gemini HTTP status codes to engine-level return codes and messages.
 *
 * <p>Follows the same pattern as {@link OpenAIErrorResponseHandler} so that
 * {@code GeminiEngineInvoker} can process errors identically to {@code AzureAIEngineInvoker}.
 */
public class GeminiErrorResponseHandler {

    private static final Log logger = LogFactory.getLog(GeminiErrorResponseHandler.class);
    static final String ENGINE_RETURN_MESSAGE = EngineConstants.ENGINE_RETURN_MESSAGE;
    static final String ENGINE_RETURN_CODE    = EngineConstants.ENGINE_RETURN_CODE;

    public Map<String, String> errorMessageForStatus(int statusCode) {
        logger.error("Handling Gemini non-200 response: " + statusCode);
        Map<String, String> errorMap = new HashMap<>();
        switch (statusCode) {
        case HttpStatus.SC_OK: // 200
            errorMap.put(ENGINE_RETURN_MESSAGE, "Gemini engine successfully returned the response.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_001");
            break;
        case HttpStatus.SC_BAD_REQUEST: // 400
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Bad Request. Please retry with correct input format.");
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_400);
            break;
        case HttpStatus.SC_UNAUTHORIZED: // 401
            errorMap.put(ENGINE_RETURN_MESSAGE, "Unauthorized. GCP credentials are wrong or missing.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_401");
            break;
        case HttpStatus.SC_FORBIDDEN: // 403
            errorMap.put(ENGINE_RETURN_MESSAGE, "Forbidden. Service account may not have Vertex AI access.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_403");
            break;
        case HttpStatus.SC_NOT_FOUND: // 404
            errorMap.put(ENGINE_RETURN_MESSAGE, "Not Found. Check project ID, location, and model ID.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_404");
            break;
        case 429: // Too Many Requests
            errorMap.put(ENGINE_RETURN_MESSAGE, "Too Many Requests. Vertex AI quota exceeded. Retry after some time.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_429");
            break;
        case HttpStatus.SC_INTERNAL_SERVER_ERROR: // 500
            errorMap.put(ENGINE_RETURN_MESSAGE, "Internal Server Error. Vertex AI engine is down. Retry after delay.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_500");
            break;
        case HttpStatus.SC_BAD_GATEWAY: // 502
            errorMap.put(ENGINE_RETURN_MESSAGE, "Bad Gateway. Vertex AI backend issue. Retry after delay.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_502");
            break;
        case HttpStatus.SC_SERVICE_UNAVAILABLE: // 503
            errorMap.put(ENGINE_RETURN_MESSAGE, "Service Unavailable. Vertex AI is overloaded or under maintenance. Retry after delay.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_503");
            break;
        case HttpStatus.SC_GATEWAY_TIMEOUT: // 504
            errorMap.put(ENGINE_RETURN_MESSAGE, "Gateway Timeout. Vertex AI backend timed out. Retry after delay.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_504");
            break;
        default:
            errorMap.put(ENGINE_RETURN_MESSAGE, "Gemini engine service error during processing.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_999");
            break;
        }
        return errorMap;
    }

    /**
     * Maps a Gemini {@code finishReason} value to an engine return code and message.
     *
     * <p>Used when the Vertex AI call returns HTTP 200 but the candidate was blocked:
     * <ul>
     *   <li>{@code BLOCKLIST}          — Model Armor blocklist template matched the response.</li>
     *   <li>{@code PROHIBITED_CONTENT} — Model Armor prohibited-content template matched.</li>
     *   <li>{@code SPII}               — Model Armor SPII (Sensitive PII) template matched.</li>
     *   <li>{@code SAFETY}             — Gemini built-in safety filter blocked the response.</li>
     *   <li>{@code RECITATION}         — Gemini recitation policy blocked the response.</li>
     *   <li>{@code MAX_TOKENS}         — Response truncated at the configured token limit.</li>
     *   <li>{@code STOP}               — Normal completion; no block.</li>
     * </ul>
     *
     * @param finishReason the {@code finishReason} string from {@code candidates[0]}
     * @return map with {@code engine_return_code} and {@code engine_return_message}
     */
    /**
     * Maps a {@code promptFeedback.blockReason} value to an engine return code and message.
     *
     * <p>Used when the Vertex AI call returns HTTP 200 but the prompt itself was blocked
     * before any candidate was generated (i.e. {@code candidates} is absent):
     * <ul>
     *   <li>{@code MODEL_ARMOR} — Model Armor floor setting blocked the prompt
     *       (e.g. prompt injection / jailbreak filter).</li>
     *   <li>{@code SAFETY}      — Gemini built-in safety filter blocked the prompt.</li>
     *   <li>{@code OTHER}       — Blocked for an unspecified reason.</li>
     * </ul>
     *
     * @param blockReason        the {@code promptFeedback.blockReason} string
     * @param blockReasonMessage the {@code promptFeedback.blockReasonMessage} detail (may be null)
     * @return map with {@code engine_return_code} and {@code engine_return_message}
     */
    public Map<String, String> errorMessageForPromptBlockReason(String blockReason, String blockReasonMessage) {
        logger.error("[GeminiErrorResponseHandler] Prompt blocked. blockReason: " + blockReason
                + ", message: " + blockReasonMessage);
        Map<String, String> errorMap = new HashMap<>();
        String detail = (blockReasonMessage != null && !blockReasonMessage.isEmpty())
                ? " Detail: " + blockReasonMessage : "";
        if (blockReason == null) {
            errorMap.put(ENGINE_RETURN_MESSAGE, "Prompt blocked for unknown reason." + detail);
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_MA_PROMPT_BLOCK);
            return errorMap;
        }
        switch (blockReason) {
        case EngineConstants.GEMINI_BLOCK_REASON_MODEL_ARMOR:
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Prompt blocked by Model Armor floor setting." + detail);
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_MA_PROMPT_BLOCK);
            break;
        case EngineConstants.GEMINI_BLOCK_REASON_SAFETY:
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Prompt blocked by Gemini built-in safety filter." + detail);
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_SAFETY_BLOCK);
            break;
        case EngineConstants.GEMINI_BLOCK_REASON_OTHER:
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Prompt blocked for unspecified reason." + detail);
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_MA_PROMPT_BLOCK);
            break;
        default:
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Prompt blocked with unrecognised blockReason: " + blockReason + "." + detail);
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_999");
            break;
        }
        return errorMap;
    }

    public Map<String, String> errorMessageForFinishReason(String finishReason) {
        logger.error("[GeminiErrorResponseHandler] Handling finishReason: " + finishReason);
        Map<String, String> errorMap = new HashMap<>();
        if (finishReason == null) {
            errorMap.put(ENGINE_RETURN_MESSAGE, "Gemini returned a null finishReason.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_999");
            return errorMap;
        }
        switch (finishReason) {
        case EngineConstants.GEMINI_FINISH_REASON_STOP:
            errorMap.put(ENGINE_RETURN_MESSAGE, "Gemini generation completed normally.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_001");
            break;
        case EngineConstants.GEMINI_FINISH_REASON_MAX_TOKENS:
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Response truncated: maximum output token limit reached.");
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_MAX_TOKENS");
            break;
        case EngineConstants.GEMINI_FINISH_REASON_SAFETY:
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Response blocked by Gemini built-in safety filter.");
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_SAFETY_BLOCK);
            break;
        case EngineConstants.GEMINI_FINISH_REASON_RECITATION:
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Response blocked by Gemini recitation policy (potential copyright issue).");
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_RECITATION_BLOCK);
            break;
        case EngineConstants.GEMINI_FINISH_REASON_BLOCKLIST:
            // Model Armor: blocklist template matched the generated response
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Response blocked by Model Armor: content matched a configured blocklist.");
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_MA_BLOCKLIST);
            break;
        case EngineConstants.GEMINI_FINISH_REASON_PROHIBITED_CONTENT:
            // Model Armor: prohibited-content template matched
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Response blocked by Model Armor: prohibited content detected.");
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_MA_PROHIBITED);
            break;
        case EngineConstants.GEMINI_FINISH_REASON_SPII:
            // Model Armor: SPII (Sensitive Personally Identifiable Information) template matched
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Response blocked by Model Armor: sensitive personally identifiable information (SPII) detected.");
            errorMap.put(ENGINE_RETURN_CODE, EngineConstants.ENGINE_MA_SPII);
            break;
        default:
            errorMap.put(ENGINE_RETURN_MESSAGE,
                    "Gemini generation stopped with unrecognised finishReason: " + finishReason);
            errorMap.put(ENGINE_RETURN_CODE, "ENGINE_999");
            break;
        }
        return errorMap;
    }
}

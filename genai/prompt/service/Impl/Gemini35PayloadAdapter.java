package com.tcs.genai.prompt.service.Impl;

import java.util.List;

import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapter;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Builds a Gemini-compatible generateContent payload.
 *
 * Gemini payload structure:
 * <pre>
 * {
 *   "contents": [
 *     { "role": "user",  "parts": [{ "text": "..." }] },
 *     { "role": "model", "parts": [{ "text": "..." }] }
 *   ],
 *   "systemInstruction": {
 *     "parts": [{ "text": "..." }]
 *   }
 * }
 * </pre>
 *
 * Key differences from OpenAI / Mistral adapters:
 * <ul>
 *   <li>Top-level array key is {@code "contents"}, not {@code "messages"}.</li>
 *   <li>Each entry wraps text inside {@code "parts": [{ "text": "..." }]}.</li>
 *   <li>Role {@code "assistant"} is mapped to {@code "model"} (Gemini terminology).</li>
 *   <li>Role {@code "system"} is extracted into a separate {@code "systemInstruction"}
 *       block at the top level (no {@code "role"} key inside it).</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
public class Gemini35PayloadAdapter implements VendorPayloadAdapter {

    private static final Log logger = LogFactory.getLog(PayloadGeneratorService.class);

   

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    /** Derives the conversation role from the dot-separated prompt name. */
    private String extractRoleFromPromptName(String promptName) {
        String[] parts = promptName.split("\\.");
        if (parts.length < 2) {
            return GenAIFrameworkConstants.GEMINI_ROLE_USER;
        }
        return parts[parts.length - 2].toLowerCase();
    }

    /**
     * Maps generic role names to Gemini roles.
     * "system" stays as-is so the caller can route it to systemInstruction.
     * "assistant" becomes "model".
     * Everything else (incl. "user") passes through unchanged.
     */
    private String toGeminiRole(String role) {
        if (GenAIFrameworkConstants.ASSISTANT.equalsIgnoreCase(role)) {
            return GenAIFrameworkConstants.GEMINI_ROLE_MODEL;
        }
        return role;
    }

    /** Collapses internal whitespace / newlines into a single space. */
    private String formatSingleLine(String input) {
        return input.replaceAll("\\n", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Builds a Gemini content entry:
     * {@code { "role": "user"|"model", "parts": [{ "text": "..." }] }}
     */
    private JSONObject buildContentEntry(String role, String text) {
        JSONObject textPart = new JSONObject();
        textPart.put(GenAIFrameworkConstants.GEMINI_TEXT_KEY, text);

        JSONArray partsArray = new JSONArray();
        partsArray.add(textPart);

        JSONObject entry = new JSONObject();
        entry.put(GenAIFrameworkConstants.ROLES, role);
        entry.put(GenAIFrameworkConstants.GEMINI_PARTS_KEY, partsArray);
        return entry;
    }

    /**
     * Builds a Gemini system-instruction block (no "role" key):
     * {@code { "parts": [{ "text": "..." }] }}
     */
    private JSONObject buildSystemInstruction(String text) {
        JSONObject textPart = new JSONObject();
        textPart.put(GenAIFrameworkConstants.GEMINI_TEXT_KEY, text);

        JSONArray partsArray = new JSONArray();
        partsArray.add(textPart);

        JSONObject instruction = new JSONObject();
        instruction.put(GenAIFrameworkConstants.GEMINI_PARTS_KEY, partsArray);
        return instruction;
    }

    // ------------------------------------------------------------------ //
    //  Core payload builder                                               //
    // ------------------------------------------------------------------ //

    @Override
    public JSONObject getPayload(JSONObject commonPayload) {

        JSONArray promptList = (JSONArray) commonPayload.get(GenAIFrameworkConstants.PROMPTIDLIST);
        JSONArray contents   = new JSONArray();
        JSONObject systemInstruction = null;

        try {
            for (int i = 0; i < promptList.size(); i++) {
                JSONObject prompt = (JSONObject) promptList.get(i);

                // 1. Main prompt block
                if (prompt.containsKey(GenAIFrameworkConstants.PROMPT_NAME)
                        && prompt.containsKey(GenAIFrameworkConstants.PROMPT_TEMPLATE)) {

                    String rawRole = extractRoleFromPromptName(
                            (String) prompt.get(GenAIFrameworkConstants.PROMPT_NAME));
                    String text    = (String) prompt.get(GenAIFrameworkConstants.PROMPT_TEMPLATE);

                    if (GenAIFrameworkConstants.GEMINI_ROLE_SYSTEM.equals(rawRole)) {
                        // System prompts → systemInstruction (last one wins)
                        systemInstruction = buildSystemInstruction(text);
                    } else {
                        contents.add(buildContentEntry(toGeminiRole(rawRole), text));
                    }
                }

                // 2. Few-shot examples
                if (prompt.containsKey(GenAIFrameworkConstants.FEWSHOT_ID)) {
                    JSONArray fewshotsArray = (JSONArray) prompt.get(GenAIFrameworkConstants.FEWSHOT_ID);
                    for (int j = 0; j < fewshotsArray.size(); j++) {
                        JSONObject fewshot = (JSONObject) fewshotsArray.get(j);
                        contents = processUserMessage(fewshot, contents);
                        contents = processAssistantMessage(fewshot, contents);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred while building Gemini 3.5 payload: " + e.getMessage(), e);
        }

        // Assemble final Gemini-compatible request body
        JSONObject finalPayload = new JSONObject();
        finalPayload.put(GenAIFrameworkConstants.GEMINI_CONTENTS_KEY, contents);
        if (systemInstruction != null) {
            finalPayload.put(GenAIFrameworkConstants.GEMINI_SYSTEM_INSTRUCTION_KEY, systemInstruction);
        }
        return finalPayload;
    }

    // ------------------------------------------------------------------ //
    //  Few-shot helpers                                                    //
    // ------------------------------------------------------------------ //

    /** Appends a "user" content entry from a few-shot example. */
    public JSONArray processUserMessage(JSONObject fewshot, JSONArray contents) {
        if (fewshot.containsKey(GenAIFrameworkConstants.FEWSHOT_USER_MESSAGE)) {
            String text = formatSingleLine(
                    (String) fewshot.get(GenAIFrameworkConstants.FEWSHOT_USER_MESSAGE));
            contents.add(buildContentEntry(GenAIFrameworkConstants.GEMINI_ROLE_USER, text));
        }
        return contents;
    }

    /** Appends a "model" content entry from a few-shot example. */
    public JSONArray processAssistantMessage(JSONObject fewshot, JSONArray contents) {
        if (fewshot.containsKey(GenAIFrameworkConstants.FEWSHOT_ASSISTANCE_MESSAGE)) {
            String text = formatSingleLine(
                    (String) fewshot.get(GenAIFrameworkConstants.FEWSHOT_ASSISTANCE_MESSAGE));
            contents.add(buildContentEntry(GenAIFrameworkConstants.GEMINI_ROLE_MODEL, text));
        }
        return contents;
    }

    // ------------------------------------------------------------------ //
    //  VendorPayloadAdapter interface stubs                               //
    // ------------------------------------------------------------------ //

    @Override
    public JSONArray addSystemPersonaPrompt(String systemPersona, String contentrole) throws JSONException {
        return null;
    }

    @Override
    public JSONArray buildGuardRailPrompt(List<String> guardrailList) throws JSONException {
        return null;
    }

    @Override
    public JSONArray buildUserQueryMessage(Object inputUserMessage, String inputType) throws JSONException {
        return null;
    }

    @Override
    public JSONArray buildAssistantQueryMessage(String inputAssistantMessage) throws JSONException {
        return null;
    }

    @Override
    public JSONArray addToExistingPromptMessages(JSONArray messages, JSONArray contentToAppend, String role) {
        return null;
    }
}

package com.tcs.genai.prompt.service.Impl;

import java.util.List;

import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapter;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class MistralPayloadAdapter implements VendorPayloadAdapter {
	
	private static final Log logger = LogFactory
			.getLog(PayloadGeneratorService.class);
	
	private String extractRoleFromPromptName(String promptName) {
		String[] parts = promptName.split("\\.");
	    if (parts.length < 2) {
	        return "";
	    }
	    return parts[parts.length - 2].toLowerCase();
    }
	
	private String formatSingleLine(String input) {
        return input.replaceAll("\n", " ").replaceAll("\\s+", " ").trim();
    }
	
	@SuppressWarnings("unchecked")
	public JSONObject getPayload(JSONObject commonPayload) {
		JSONArray promptList = (JSONArray) commonPayload.get(GenAIFrameworkConstants.PROMPTIDLIST);
        JSONArray messages = new JSONArray();
		
        try {
			for (int i = 0; i < promptList.size(); i++) {
			    JSONObject prompt = (JSONObject) promptList.get(i);
			    
//            logger.error("[MISTRAL] prompt: " + prompt);

			    // 1. Process outer prompt block
			    if (prompt.containsKey(GenAIFrameworkConstants.PROMPT_NAME) && prompt.containsKey(GenAIFrameworkConstants.PROMPT_TEMPLATE)) {
			        String role = extractRoleFromPromptName((String) prompt.get(GenAIFrameworkConstants.PROMPT_NAME));
//                logger.error("[MISTRAL] role: " + role);
			        String content = (String) prompt.get(GenAIFrameworkConstants.PROMPT_TEMPLATE);
//                logger.error("[MISTRAL] content: " + content);

			        JSONObject message = new JSONObject();
			        message.put(GenAIFrameworkConstants.ROLES, role);
			        message.put(GenAIFrameworkConstants.CONTENTS, content);
			        messages.add(message);
			    }

			    // 2. Process fewshots block (if present)
			    if (prompt.containsKey(GenAIFrameworkConstants.FEWSHOT_ID)) {
			        JSONArray fewshotsArray = (JSONArray) prompt.get(GenAIFrameworkConstants.FEWSHOT_ID);
			        for (int j = 0; j < fewshotsArray.size(); j++) {
			            JSONObject fewshot = (JSONObject) fewshotsArray.get(j);

			            // user message block
			            messages = processUserMessage(fewshot, messages);

			            // assistant message block
			            messages = processAssistantMessage(fewshot, messages);
			        }
			    }
			}
		} catch (Exception e) {
			logger.error("Error occured while getting Mistral Payload: " + e.getMessage(), e);
		}

        // Wrap into final structure
        JSONObject finalMessage = new JSONObject();
        finalMessage.put("messages", messages);
        
//        logger.error("[MISTRAL] finalMessage: " + finalMessage);
        return finalMessage;
	}


	
//	Process User Message Block
	public JSONArray processUserMessage(JSONObject fewshot, JSONArray messages) {
		if (fewshot.containsKey(GenAIFrameworkConstants.FEWSHOT_USER_MESSAGE)) {
            JSONObject userMessage = new JSONObject();
            userMessage.put(GenAIFrameworkConstants.ROLES, GenAIFrameworkConstants.GPT4O_DEFAULT_CONTENT_ROLE);
            userMessage.put(GenAIFrameworkConstants.CONTENTS, formatSingleLine((String) fewshot.get(GenAIFrameworkConstants.FEWSHOT_USER_MESSAGE)));
            
//            logger.error("[MISTRAL] userMessage: " + userMessage);
            messages.add(userMessage);
        }
		return messages;
	}
	
//	Process Assistant Message Block
	public JSONArray processAssistantMessage(JSONObject fewshot, JSONArray messages) {
		if (fewshot.containsKey(GenAIFrameworkConstants.FEWSHOT_ASSISTANCE_MESSAGE)) {
            JSONObject assistantMessage = new JSONObject();
            assistantMessage.put(GenAIFrameworkConstants.ROLES, GenAIFrameworkConstants.ASSISTANT);
            assistantMessage.put(GenAIFrameworkConstants.CONTENTS, formatSingleLine((String) fewshot.get(GenAIFrameworkConstants.FEWSHOT_ASSISTANCE_MESSAGE)));
            
//            logger.error("[MISTRAL] assistantMessage: " + assistantMessage);
            messages.add(assistantMessage);
        }
		return messages;
	}
	
	
	@Override
	public JSONArray addSystemPersonaPrompt(String systemPersona,
			String contentrole) throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public JSONArray buildGuardRailPrompt(List<String> guardrailList)
			throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public JSONArray buildUserQueryMessage(Object inputUserMessage,
			String inputType) throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public JSONArray buildAssistantQueryMessage(String inputAssistantMessage)
			throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public JSONArray addToExistingPromptMessages(JSONArray messages,
			JSONArray contentToAppend, String role) {
		// TODO Auto-generated method stub
		return null;
	}

}

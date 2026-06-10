package com.tcs.genai.prompt.service.Impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapter;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
//import com.tcs.genai.prompt.utils.GenAIFrameworkConstants.AzureGPT4oParams;

//import com.tcs.genai.prompt.utils.GenAIFrameworkConstants.VENDOR_CONSTANTS;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class AzureOpenAIServicePayloadAdapter implements VendorPayloadAdapter {
	private static final Log logger = LogFactory
			.getLog(PayloadGeneratorService.class);
	
	
	private JSONObject createContentMap(Object inputMessage, String inputType)
	{
		JSONObject contentObject = new JSONObject();
		if (inputType.equals(GenAIFrameworkConstants.TEXT_CONTENT_TYPE)) {
			contentObject.put(GenAIFrameworkConstants.TYPE, (String) inputType);
			contentObject.put(GenAIFrameworkConstants.TEXT_CONTENT_TYPE, (String) inputMessage);
		}

		return contentObject;
	}
	
	
	public JSONArray addSystemPersonaPrompt(String systemPersona, String contentrole, String promptType) throws JSONException {
		
		JSONArray messages = new JSONArray();
		Map<String, Object> systemMessage = new HashMap<String, Object>();
		JSONArray contentArray = new JSONArray();
		
		
		contentArray.add(this.createContentMap(systemPersona, promptType));
		systemMessage.put(GenAIFrameworkConstants.CONTENT_ROLE, contentrole);
		systemMessage.put(GenAIFrameworkConstants.CONTENT, contentArray);
		messages.add(systemMessage);

		return messages;
	}
	
	
	public JSONArray buildGuardRailPrompt(List<String> guardrailList, String promptType) throws JSONException {
		JSONArray contentArray = new JSONArray();
		for (String guardrailText : guardrailList) {
			contentArray.add(this.createContentMap(guardrailText, promptType));
		}
		return contentArray;
	}
	
	public JSONObject buildQueryForUserAssistanceMessage(String inputUserMessage, String inputType) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put(inputType, inputUserMessage);
		obj.put(GenAIFrameworkConstants.TYPE, inputType);
		return obj;
	}
	
	public JSONArray buildUserQueryAssistanceMessage(String inputUserMessage, String inputType) throws JSONException {
		JSONObject obj = new JSONObject();
		JSONArray message = new JSONArray();
		
		obj.put(inputType, inputUserMessage);
		obj.put(GenAIFrameworkConstants.TYPE, inputType);
		message.add(obj);
		
		return message;
	}
	
	
	
	public JSONArray addToExistingPromptMessages(JSONArray messages, JSONArray contentToAppend, String role) {
		JSONObject messageMap = new JSONObject();
			
		messageMap.put(GenAIFrameworkConstants.CONTENT_ROLE, (String) role);
		messageMap.put(GenAIFrameworkConstants.CONTENT, contentToAppend);
		messages.add(messageMap);
		
//		logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "  messages inside addToExistingPromptMessages {} : " + messages);
		
		return messages;
	}
	
	// Fetching the Prompt Persona USER/SYSTEM from the prompt template
	private String extractContentRole(String promptName) {
		String role = GenAIFrameworkConstants.GPT4O_DEFAULT_CONTENT_ROLE;
		if(promptName!=null){
			try{
				String[] splittedText = promptName.split("\\.");
				role = splittedText[splittedText.length - 2];
				role = role.toLowerCase(Locale.ROOT);
			}catch(Exception e){
				logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception occured in extracting prompt role. {} " + e.getMessage());
			}
			
		}else{
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + "  not prompt name is present.");
		}
		return role;
		
	}
	
	// Fetching the content type TEXT/IMAGE_URL from the prompt template
	private String extractContentType(String promptName) {
		String role = GenAIFrameworkConstants.GPT4O_DEFAULT_CONTENT_TYPE;
		if(promptName!=null){
			try{
				String[] splittedText = promptName.split("\\.");
				role = splittedText[splittedText.length - 1];
				role = role.toLowerCase();
			}catch(Exception e){
				logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception occured in extracting prompt type. {} " + e.getMessage());
			}
			
		}else{
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + "  not prompt name is present.");
		}
		return role;
		
	}
	
	private List<String> extractGuardrails(JSONArray guardRails) {
		List<String> guardrails = new ArrayList<>();

		if (guardRails == null || guardRails.size() == 0) {
		    logger.error("Guardrail array is null or empty.");
		    return guardrails;
		}

		for (int j = 0; j < guardRails.size(); j++) {
		    JSONObject guardrail = (JSONObject) guardRails.get(j);

			if (guardrail != null && guardrail.containsKey("guardrail_instruction")) {
				guardrails.add((String) guardrail.get("guardrail_instruction"));
			} else {
			    logger.warn("Missing or null guardrail_instruction at index " + j);
			}
		}

	    return guardrails;
	}
	
	private JSONArray processGuardrails(JSONArray messages, List<String> guardrails, String guardrailRole, String promptType) {
		try {
			if (!guardrails.isEmpty()) {
				JSONArray guardrailMessages = buildGuardRailPrompt(guardrails, promptType);
				messages = addToExistingPromptMessages(messages, guardrailMessages, guardrailRole);
			}	
		} catch (JSONException e) {
			logger.error("Exception Occured During Fewshot example processing ");
		}
	
		return messages;
	}
	
	private JSONArray processFewshotExamples(JSONArray messages, JSONObject finalFewShotUserAssistantQueries) {
		
		logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "  Inside processFewshotExamples {} ");
		
		JSONArray userQueries = new JSONArray();
	    JSONArray assistantQueries = new JSONArray();
	    
	    
	    String userRole = null;
	    String assistantRole = null;

	    if (finalFewShotUserAssistantQueries.containsKey(GenAIFrameworkConstants.GPT4O_DEFAULT_CONTENT_ROLE)) {
	    	
	    	JSONObject userContent = (JSONObject) finalFewShotUserAssistantQueries.get(GenAIFrameworkConstants.GPT4O_DEFAULT_CONTENT_ROLE);
		    userQueries = (JSONArray) userContent.get(GenAIFrameworkConstants.CONTENT);
		    userRole = (String) userContent.get(GenAIFrameworkConstants.CONTENT_ROLE);
		    
		}
		if (finalFewShotUserAssistantQueries.containsKey(GenAIFrameworkConstants.ASSISTANT)) {
			
			JSONObject assistantContent = (JSONObject) finalFewShotUserAssistantQueries.get(GenAIFrameworkConstants.ASSISTANT);
		    assistantQueries = (JSONArray) assistantContent.get(GenAIFrameworkConstants.CONTENT);
		    assistantRole = (String) assistantContent.get(GenAIFrameworkConstants.CONTENT_ROLE);
		    
		}

	    try {
	        if (userQueries.size() > 0) {
	            messages = addToExistingPromptMessages(messages, userQueries, userRole);
	        }
	        if (assistantQueries.size() > 0) {
	        	messages = addToExistingPromptMessages(messages, assistantQueries, assistantRole);
	        }
	    } catch (Exception e) {
	        logger.error("Error in adding few-shot examples to messages: " + e.getMessage(), e);
	    }

	    return messages;
	}
	
	private JSONObject extractFewshotExamples(JSONArray fewshotExamples, String promptType) {
		
		JSONObject fewShotsUserMessage = new JSONObject();
		JSONObject fewShotsAssistanceMessage = new JSONObject();
		JSONArray extractedFewShotsUser = new JSONArray();
		JSONArray extractedFewShotsAssistance = new JSONArray();
		JSONObject finalFewShot = new JSONObject();

		for (int j = 0; j < fewshotExamples.size(); j++) {
			
		    JSONObject fewShotExample = (JSONObject) fewshotExamples.get(j);
			if (fewShotExample.containsKey(GenAIFrameworkConstants.FEWSHOT_USER_MESSAGE)) {				
			    try {
			    	String fewShotUserMessage = (String) fewShotExample.get(GenAIFrameworkConstants.FEWSHOT_USER_MESSAGE);
			    	extractedFewShotsUser.add(buildQueryForUserAssistanceMessage(fewShotUserMessage, promptType));
			    } catch (JSONException e) {
			        logger.error("Error parsing user_message: " + e.getMessage(), e);
			    }
			    
			    
			    if (fewShotExample.containsKey(GenAIFrameworkConstants.FEWSHOT_ASSISTANCE_MESSAGE)) {
			    	
				    try {
				    	String fewShotAssistanceMessage = (String) fewShotExample.get(GenAIFrameworkConstants.FEWSHOT_ASSISTANCE_MESSAGE);
				    	extractedFewShotsAssistance.add(buildQueryForUserAssistanceMessage(fewShotAssistanceMessage, promptType));
				    } catch (JSONException e) {
				        logger.error("Error parsing assistant_message: " + e.getMessage(), e);
				    }
				}
			    
			}
		}
		fewShotsUserMessage.put(GenAIFrameworkConstants.CONTENT_ROLE, GenAIFrameworkConstants.GPT4O_DEFAULT_CONTENT_ROLE);
		fewShotsUserMessage.put(GenAIFrameworkConstants.CONTENT, extractedFewShotsUser);
		
		fewShotsAssistanceMessage.put(GenAIFrameworkConstants.CONTENT_ROLE, GenAIFrameworkConstants.ASSISTANT);
		fewShotsAssistanceMessage.put(GenAIFrameworkConstants.CONTENT, extractedFewShotsAssistance);
		
		finalFewShot.put(GenAIFrameworkConstants.GPT4O_DEFAULT_CONTENT_ROLE, fewShotsUserMessage);
		finalFewShot.put(GenAIFrameworkConstants.ASSISTANT, fewShotsAssistanceMessage);
		
	    return finalFewShot;
	}
	
	
//	private JSONObject extractPromptParams(JSONObject finalMessage,JSONObject finalPayload) {
//	        
//			JSONArray promptParamsArray = (JSONArray) finalPayload.get("promptparams");
//
//			JSONObject paramMap = new JSONObject();
//			paramMap.put(GenAIFrameworkConstants.OPENAI_MAX_TOKENS, "max_tokens");
//			paramMap.put(GenAIFrameworkConstants.OPENAI_TEMPERATURE, "temperature");
//			paramMap.put(GenAIFrameworkConstants.OPENAI_TOP_P, "top_p");
//			paramMap.put(GenAIFrameworkConstants.OPENAI_MODEL_TYPE, "model");
//	
//	        if (promptParamsArray != null) {
//	            for (int i = 0; i < promptParamsArray.size(); i++) {
//	                JSONObject paramObject = (JSONObject) promptParamsArray.get(i);
//					String paramName = (String) paramObject.get("parameter_name");
//					String paramValue = (String) paramObject.get("parameter_value");
//
//					if (!paramName.isEmpty() && !paramValue.isEmpty()) {
//					    finalMessage.put(paramMap.get(paramName), paramValue);
//					}
//	            }
//	        }
//	        
//	        return finalMessage;
//		}
	
	private JSONArray extractGuardrail(JSONArray guardRail, String contentRole, JSONArray messages, String promptType) {
		List<String> guardrails = extractGuardrails(guardRail);
		messages = processGuardrails(messages, guardrails, contentRole, promptType);
		return messages;
	}

	private JSONArray formpayloadForEachPrompt(JSONObject childPrompt, JSONArray messages) {
		JSONArray userQueriesPromptTemplate = new JSONArray();
		
		String promptName = (String) childPrompt.get("prompt_name");
		String promptTemplate = (String) childPrompt.get("prompt_template");
		String promptRole = extractContentRole(promptName);
		String promptType = extractContentType(promptName);
		
		try {
			if (childPrompt.containsKey("prompt_template") && promptRole.equals(GenAIFrameworkConstants.SYSTEM)) {
				messages = addSystemPersonaPrompt(promptTemplate, promptRole, promptType);
			}
			// Processing GuardRails
			JSONArray guardRail = (JSONArray) childPrompt.get("guardrail_id");
			if(guardRail != null && !guardRail.isEmpty()){
				messages = extractGuardrail(guardRail, promptRole, messages, promptType);
			}

			// Processing Fewshots
			JSONArray fewShot = (JSONArray) childPrompt.get("fewshots_id");
			if(fewShot != null && !fewShot.isEmpty()){
				JSONObject finalFewShotPrompts = extractFewshotExamples(fewShot, promptType);
				messages = processFewshotExamples(messages, finalFewShotPrompts);
			}
			
			if (promptRole.equals("user") || promptRole.equals("USER")) {
				userQueriesPromptTemplate = buildUserQueryAssistanceMessage(promptTemplate, promptType);
				messages = addToExistingPromptMessages(messages,userQueriesPromptTemplate, promptRole);

			}
		} catch (JSONException e) {
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + "Exception Occured while formpayloadForEachPrompt {}: " + e);
		}
		
		return messages;
	}
	
	
	
	/*
	 * Iterating through the persona objects in the common payLoad and restructuring it to form the final vendor payLoad 
	 */
	
	public JSONObject getPayload(JSONObject commonPayload)
	{
		JSONObject finalPayload = commonPayload;
		JSONObject finalMessage = new JSONObject();
		JSONArray messages = new JSONArray();
		
		JSONArray promptList = (JSONArray) finalPayload.get(GenAIFrameworkConstants.PROMPTIDLIST);
		for (int i = 0; i < promptList.size(); i++) {
			JSONObject childPrompt = (JSONObject) promptList.get(i);
			messages = (JSONArray) formpayloadForEachPrompt(childPrompt, messages);
			
		}
		finalMessage.put("messages", messages);
        
        return finalMessage;
	}


	@Override
	public JSONArray buildUserQueryMessage(Object inputUserMessage,
			String inputType) throws JSONException {
		return null;
	}


	@Override
	public JSONArray buildAssistantQueryMessage(String inputAssistantMessage)
			throws JSONException {
		return null;
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




	
}
	


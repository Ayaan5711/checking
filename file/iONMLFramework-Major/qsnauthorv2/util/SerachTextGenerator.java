package com.tcsion.ml.qsnauthorv2.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.tcs.genai.engine.service.EngineInvocationService;
import com.tcs.genai.prompt.service.Impl.PayloadGeneratorService;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;
import com.tcsion.ml.qsnauthorv2.inputtypes.KnowledgeBasedAuthoring;

public class SerachTextGenerator {
	private static final Log logger = LogFactory.getLog(SerachTextGenerator.class);
	
	
	public String QueryGenerator(InputBean inputBean) throws IOException, JSONException{
		StringBuilder searchTextBuilder = new StringBuilder();		
		
		if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
		    org.json.simple.JSONObject simpleDirectives = inputBean.getDirectives();
		    org.json.JSONObject directivesJson = new org.json.JSONObject(simpleDirectives.toJSONString());

		    Iterator<String> keys = directivesJson.keys();
		    while (keys.hasNext()) {
		        String key = keys.next();
		        org.json.JSONObject directive = directivesJson.getJSONObject(key);
		        if (directive.optBoolean("mandatory", false)) {
		            org.json.JSONArray values = directive.optJSONArray("value");
		            if (values != null) {
		                for (int i = 0; i < values.length(); i++) {
		                    if (searchTextBuilder.length() > 0) 
		                    	searchTextBuilder.append(", ");
		                    searchTextBuilder.append(values.getString(i));
		                }
		            }
		        }
		    }
		}
		
		
		
	    String serviceName = inputBean.getAIvendorCredNodeMap().keySet().iterator().next();
		Boolean tokenAuditNeeded = true;
		Map<String, Object> invocationContext = new HashMap<String, Object>(); 
		
		String searchQuery = searchTextBuilder.toString(); 
		
		if (!searchTextBuilder.toString().trim().isEmpty()) {
			
			invocationContext.put("appId", inputBean.getAppId()); 
			invocationContext.put("orgId", inputBean.getOrgId()); 
			invocationContext.put("apiId", inputBean.getApiID()); 
			invocationContext.put("useCaseId", inputBean.getUsecaseId()); 
			invocationContext.put("serviceId", inputBean.getUserRegisteredServiceId()); 
			EngineInvocationService eis =new EngineInvocationService(inputBean.getAIvendorCredNodeMap(),invocationContext,tokenAuditNeeded);
			
			
			JSONObject prompt_messages = getQueryGenPrompt(inputBean);
		    logger.info("Final prompt print for queryGenerator : " + prompt_messages);
		    

	    	logger.info("[[FLAG]] The Parameter Map looks like: " + inputBean.getParameterMap().toString());

		    JSONObject frameworkResult = eis.invokeAdvanceAiEngine(prompt_messages, inputBean.getParameterMap());
			logger.info("[AzureAIEngineHandler] Result for " +serviceName + " is: " + frameworkResult.toString());
			
			String llmQuery = processResponse(frameworkResult);

		    if (llmQuery != null && !llmQuery.trim().isEmpty()) {
		        searchQuery = llmQuery;
		    }
		
		}	
		
		
		return searchQuery;
	}
	
	
	
	public JSONObject getQueryGenPrompt(InputBean inputBean)throws IOException {

		logger.error("-----Inside getQueryGenPrompt----");

		logger.info("vendorDetailsMap {} " + inputBean.getAIvendorCredNodeMap());
		logger.error("orgid " + inputBean.getOrgId());
		logger.error("appid " + inputBean.getAppId());
		logger.error("apiid " + inputBean.getApiID());

		JSONArray promptMessages = new JSONArray();
		JSONObject payloadFromGenAI = null;

		try {
			// Step 1: Call GenAI framework to get the raw payload with placeholders
			payloadFromGenAI = callGenAIUtility(inputBean,
					inputBean.getAIvendorCredNodeMap(),
					Long.parseLong(inputBean.getOrgId()),
					Long.parseLong(inputBean.getAppId()),
					Integer.parseInt(inputBean.getApiID()), "queryGen");

			logger.info("Payload received from GenAI framework: "+ payloadFromGenAI);

			// Step 2: Extract the 'messages' array from the JSON
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");

			JsonNode messagesNode = payloadNode.get("messages");

			logger.info("Extract the messages array from the JSON :"+ messagesNode);

			// Convert Jackson JsonNode to org.json.simple.JSONArray
			JSONParser parser = new JSONParser();
			JSONArray dbMessages = (JSONArray) parser.parse(messagesNode.toString());

			// Step 3: Process placeholders, add image or context as needed
			promptMessages = preparePromptFromDBPayload(dbMessages, inputBean);
			// Replace messages inside original payload
			((JSONObject) payloadFromGenAI.get("payload")).put("messages",promptMessages);

			logger.info("Final prompt messages after replacement (SearchQueryGen): "+ promptMessages);

		} catch (Exception e) {
			logger.error("Error while generating MCQ prompt", e);
			throw new IOException("Failed to generate prompt", e);
		}

		return (JSONObject) payloadFromGenAI.get("payload");
	}

	public JSONObject callGenAIUtility(final InputBean inputBean,final Map<String, HashMap<String, String>> vendorDetailsMap,final long orgId, final long appId,final int apiId,String questionType){
		logger.info("[[FLAG]] Sending request to GenAI Framework!!");
		logger.info(" vendor details recieved for Question gen{} " + vendorDetailsMap);
		PayloadGeneratorService pgs = new PayloadGeneratorService(questionType);
    	logger.info("[[FLAG]] The Parameter Map looks like in promptbuilder: " + inputBean.getParameterMap().toString());
		
		JSONObject payloadFromGenAi = (JSONObject) pgs.generatePayload(Long.valueOf(0),inputBean.getParameterMap() , apiId, orgId, appId);
		logger.error("[[FLAG]] Payload received from GenAI Framework for Question Generation {} : " + payloadFromGenAi.toString());
		return payloadFromGenAi;
	}
	
	
	
	public JSONArray preparePromptFromDBPayload(JSONArray dbMessages,InputBean inputBean) {

		JSONArray updatedMessages = new JSONArray();

		for (int i = 0; i < dbMessages.size(); i++) {
			JSONObject message = (JSONObject) dbMessages.get(i);
			String role = (String) message.get("role");
			JSONArray contentArray = (JSONArray) message.get("content");

			JSONArray updatedContentArray = new JSONArray();

			for (int j = 0; j < contentArray.size(); j++) {
				JSONObject contentItem = (JSONObject) contentArray.get(j);

				if ("text".equals(contentItem.get("type"))) {

					String text = (String) contentItem.get("text");

					
					// ---------------- User Directives ----------------
					JSONObject directives = inputBean.getDirectives();

					if (directives != null && !directives.isEmpty()) {

						text = text.replace("{User Directives}", directives.toString());
					} else {
						text = text.replace("{User Directives}", "");
					}


					// ---------------- User Instruction ----------------
					if (!Strings.isNullOrEmpty(inputBean.getUserInput())) {
						text = text.replace("{UserInstruction}","User instruction: "+ inputBean.getUserInput());
					} else {
						text = text.replace("{UserInstruction}", "");
					}

					

					JSONObject updatedText = new JSONObject();
					updatedText.put("type", "text");
					updatedText.put("text", text);
					updatedContentArray.add(updatedText);

				} else {
					updatedContentArray.add(contentItem);
				}
			}

			JSONObject updatedMessage = new JSONObject();
			updatedMessage.put("role", role);
			updatedMessage.put("content", updatedContentArray);
			updatedMessages.add(updatedMessage);
		}

		return updatedMessages;
	}
	
	
	
	private String processResponse(JSONObject finalResult) {

	    if (finalResult == null) {
	        logger.error("Final result is null in processResponse");
	        return "";
	    }

	    String statusCode = (String) finalResult.get("statuscode");
	    logger.error("Query generator status code: " + statusCode);

	    try {

	        // Only successful execution path
	        if ("ENGINE_001".equals(statusCode)) {

	            String output = (String) finalResult.get("extractedText");

	            if (output == null || output.trim().isEmpty()) {
	                logger.error("Empty extractedText from LLM response");
	                return "";
	            }

	            output = output.trim();

	            if (output.startsWith("Sorry")) {
	                logger.error("Output starts with Sorry : " + output);
	                return "";
	            }

	            return output;
	        }

	        //  Merge all failure cases into single handling
	        if ("ENGINE_429".equals(statusCode)|| "ENGINE_400".equals(statusCode)) {

	            logger.error("Query generator failed with status code: " + statusCode);
	            return "";
	        }

	        logger.error("Unexpected status code from query generator: " + statusCode);
	        return "";

	    } catch (Exception e) {
	        logger.error("Error while processing response for query generator", e);
	        return "";
	    }
	}

}

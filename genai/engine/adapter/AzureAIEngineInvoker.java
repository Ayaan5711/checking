package com.tcs.genai.engine.adapter;


import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;






import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.genai.engine.utils.EngineConstants;
import com.tcs.genai.engine.utils.GenericHTTPCall;
import com.tcs.genai.engine.utils.OpenAIErrorResponseHandler;
import com.tcs.genai.engine.utils.OpenAIErrorResponseHandler.RetryableStatus;
import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapterFactory;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

@SuppressWarnings("unchecked")
public class AzureAIEngineInvoker implements EngineInvokerAdapter {
	
	private static final Log logger = LogFactory
			.getLog(VendorPayloadAdapterFactory.class);
	
	@Override
	public JSONObject invokeAiEngine(JSONObject payload,
			Map<String, HashMap<String, String>> vendorProps) {
		
		
		String prompt = payload.toJSONString();
		JSONObject finalVendorResultMap = new JSONObject();
		String engineStatusCode = null;
		String engineStatusMessage = null;
		Map<String, String> propertiesValueMap = new HashMap<String, String>();
		String API_URL = null;
		String Engine_Key = null;
		String API_VERSION = null;
		String vendorName = null;
		String vendorActualMessage = null;

		// Configurable retry parameters
		int maxRetries = 3;
		int baseDelayMs = 10000; // 10 second base delay
		
		
		for (Map.Entry<String, HashMap<String, String>> entry : vendorProps.entrySet()) {
			vendorName = entry.getKey();
			logger.info("AzureAIEngineInvoker [vendorName]: " + vendorName);
		}
		
		propertiesValueMap = vendorProps.get(vendorName);
		
		switch (vendorName) {
		case GenAIFrameworkConstants.OPENAI_GPT:
		case GenAIFrameworkConstants.OPENAI_GPT4O:
		case GenAIFrameworkConstants.OPENAI_GPT4_1:
		case GenAIFrameworkConstants.OPENAI_GPT5_4:
			API_URL = propertiesValueMap.get(EngineConstants.OPENAI_API_URL);
			Engine_Key = propertiesValueMap.get(EngineConstants.OPENAI_API_KEY);
			API_VERSION = propertiesValueMap.get(EngineConstants.OPENAI_API_VERSION);			
			break;
		case GenAIFrameworkConstants.AZURE_MISTRAL_SMALL_2503:
		case GenAIFrameworkConstants.QUESTION_GENERATION_OPENSOURCE:
		case GenAIFrameworkConstants.TCS_ION_AI_ML:
			API_URL = propertiesValueMap.get(EngineConstants.AZMISTRAL_API_URL);
			Engine_Key = propertiesValueMap.get(EngineConstants.AZMISTRAL_API_KEY);
			API_VERSION = propertiesValueMap.get(EngineConstants.AZMISTRAL_API_VERSION);
			break;

		default:
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " Vendor specific implementation not present for this vendor: "+ vendorName);
			finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, "NA");
			finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, "Vendor specific implementation not present for this vendor: "+ vendorName);
			finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, "ENGINE_800");
			return finalVendorResultMap;
		}
		
		
		GenericHTTPCall newhit = new GenericHTTPCall(API_URL,API_VERSION,Engine_Key,prompt);
		JSONObject vendorResponse = newhit.makeCall();
		int responseCode = (int) vendorResponse.get(GenAIFrameworkConstants.STATUS_CODE);
		logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + " First time hit to third party API. "
				+ ". Response code : " + responseCode);
		
		if(RetryableStatus.isRetryable(responseCode)){
			try {
				vendorResponse = newhit.retryWithBackoff(maxRetries, baseDelayMs);
			} catch (InterruptedException e) {
				logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " Exception is parsing vendor response.Exception {} " + e);
			}
		}
		responseCode = (int) vendorResponse.get(GenAIFrameworkConstants.STATUS_CODE);
		String response = (String) vendorResponse.get(GenAIFrameworkConstants.VENDOR_RESPONSE);
		
		OpenAIErrorResponseHandler openAIErrorHandler = new OpenAIErrorResponseHandler();
	    Map<String, String> engineStatus = openAIErrorHandler.errorMessageForStatus(responseCode);
	    engineStatusCode = engineStatus.get(EngineConstants.ENGINE_RETURN_CODE);
	    engineStatusMessage = engineStatus.get(EngineConstants.ENGINE_RETURN_MESSAGE);
	    vendorActualMessage =  (String) vendorResponse.get(GenAIFrameworkConstants.STATUS_MESSAGE);

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode choicesArray = null;
		JsonNode jsonArray = null;
		String vendor_response = "";
		
		if(response == null || response.isEmpty()){
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " Recieved response is null or Empty! ");
			finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, response);
	        finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, engineStatusMessage);
	        finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, engineStatusCode);
	        finalVendorResultMap.put(GenAIFrameworkConstants.AZURE_RESPONSE, response);
            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_ACTUAL_MESSAGE,vendorActualMessage);
		}else{
			try{
				JsonNode response_node = objectMapper.readTree(response);
				if(response_node.has("choices")){
					choicesArray = response_node.get("choices");
			
			    	if	(choicesArray == null){
				    	logger.error(GenAIFrameworkConstants.LOGGER_ERROR + ": Response from engine: "+ vendorActualMessage);
				        logger.error(GenAIFrameworkConstants.LOGGER_ERROR + ": Response received : " + response);
			
				        finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, vendor_response);
				        finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, engineStatusMessage);
				        finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, engineStatusCode);
				        finalVendorResultMap.put(GenAIFrameworkConstants.AZURE_RESPONSE, response);
			            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_ACTUAL_MESSAGE,vendorActualMessage);
			    	}else{
			    		jsonArray = objectMapper.readTree(choicesArray.toString());
			    		logger.error(GenAIFrameworkConstants.LOGGER_ERROR + "Response jsonArray {} " + jsonArray);
		
			    		for (JsonNode jsonNode : jsonArray) {
				            vendor_response = jsonNode.get(GenAIFrameworkConstants.MESSAGE)
				                                      .get(GenAIFrameworkConstants.CONTENT)
				                                      .asText();
			
				            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, vendor_response);
				            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, engineStatusMessage);
				            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, engineStatusCode);
				            finalVendorResultMap.put(GenAIFrameworkConstants.AZURE_RESPONSE, response);
				            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_ACTUAL_MESSAGE,vendorActualMessage);
			    		}
				    }
				} else {
					logger.error(GenAIFrameworkConstants.LOGGER_ERROR + ": Response from engine: "+ vendorActualMessage);
			        logger.error(GenAIFrameworkConstants.LOGGER_ERROR + ": Response received : " + response);
		
			        finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, vendor_response);
			        finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, engineStatusMessage);
			        finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, engineStatusCode);
			        finalVendorResultMap.put(GenAIFrameworkConstants.AZURE_RESPONSE, response);
		            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_ACTUAL_MESSAGE,vendorActualMessage);
				}
		    	
		} catch (Exception e) {
		    logger.error(GenAIFrameworkConstants.LOGGER_ERROR + 
		        " Exception in parsing vendor response.Exception {} " + e);
		    finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, vendor_response);
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, engineStatusMessage);
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, engineStatusCode);
            finalVendorResultMap.put(GenAIFrameworkConstants.AZURE_RESPONSE, response);
            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_ACTUAL_MESSAGE,vendorActualMessage);
		}
	    
			
	  	try{
	  		JSONObject tokenJson = extractTokenUsage(vendorResponse);
			finalVendorResultMap.putAll(tokenJson);

	  	}catch(Exception e){
	  		logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + ": Exception occurred while tokenJson extration: ",e);
	  	}
		
		}
		return finalVendorResultMap;
	}

	//extracted tokens 
	public JSONObject extractTokenUsage(JSONObject vendorResponse) {

	    long inputTokens = 0L;
	    long outputTokens = 0L;
	    long reasoningTokens = 0L;

	    try {

		if (vendorResponse != null && vendorResponse.containsKey("extractedText")) {
		
		            String extractedTextObj = (String) vendorResponse.get("extractedText");
		
		            JSONObject extractedTextJson;

		            JSONParser parser = new JSONParser();
	                extractedTextJson =(JSONObject) parser.parse((String) extractedTextObj);
	                
		            if (extractedTextJson != null && !(extractedTextJson.isEmpty())  && extractedTextJson.containsKey("usage")) {
		
		                JSONObject usage = (JSONObject) extractedTextJson.get("usage");
		
		                if (usage != null) {
		
		                    Object promptTokensObj = usage.get("prompt_tokens");
		                    Object completionTokensObj = usage.get("completion_tokens");
		
		                    if (promptTokensObj instanceof Number) {
		                        inputTokens = ((Number) promptTokensObj).longValue();
		                    }
		
		                    if (completionTokensObj instanceof Number) {
		                        outputTokens = ((Number) completionTokensObj).longValue();
		                    }
		
		                    if (usage.containsKey("completion_tokens_details")) {
		
		                        JSONObject completionDetails =
		                                (JSONObject) usage.get("completion_tokens_details");
		
		                        if (completionDetails != null) {
		                            Object reasoningObj =
		                                    completionDetails.get("reasoning_tokens");
		
		                            if (reasoningObj instanceof Number) {
		                                reasoningTokens =
		                                        ((Number) reasoningObj).longValue();
		                            }
		                        }
		                    }
		                }
		            }
		        }

	    } catch (Exception e) {
	        logger.error("[Token Usage] Failed to extract token usage.Exception is {} ", e);
	    }

	    JSONObject tokensJson = new JSONObject();
	    tokensJson.put("inputTokens", inputTokens);
	    tokensJson.put("outputTokens", outputTokens);
	    tokensJson.put("reasoningTokens", reasoningTokens);

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

}



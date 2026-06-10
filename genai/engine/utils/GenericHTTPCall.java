package com.tcs.genai.engine.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

public class GenericHTTPCall {
	private static final Log logger = LogFactory
			.getLog(GenericHTTPCall.class);
	
	String API_URL;
	String API_VERSION;
	String Engine_Key;
	String prompt;
	
	public GenericHTTPCall(String aPI_URL, String aPI_VERSION,
			String engine_Key, String prompt) {
		this.API_URL = aPI_URL;
		this.API_VERSION = aPI_VERSION;
		this.Engine_Key = engine_Key;
		this.prompt = prompt;		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
//	private String getThirdPartyResponse(HttpURLConnection con) {
//		StringBuilder response = new StringBuilder();
//		String responseStr="";
//		try (BufferedReader br = new BufferedReader(new InputStreamReader(
//				con.getInputStream(), GenAIFrameworkConstants.UTF_8))) {
//			String responseLine = null;
//			while ((responseLine = br.readLine()) != null) {
//				response.append(responseLine.trim());
//			}
//			responseStr = response.toString();
//		}catch(Exception e){
//			InputStream es = con.getErrorStream();
//		    if (es != null) {
//		    	responseStr = new BufferedReader(new InputStreamReader(es))
//		            .lines().collect(Collectors.joining("\n"));
//		        logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + "[GenAIEngineInvoker] Error response: " + responseStr);
//		    }
//		}
//		return responseStr;
//	}
	
	private Map<String,String> getThirdPartyResponseMap(HttpURLConnection con) {
		StringBuilder response = new StringBuilder();
		Map<String, String> thirdPartyResponseMap = new HashMap<String, String>();
		String responseStr = "";
		String vendorStatusCode  = "999";
		String vendorStatusMessage = "";
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				con.getInputStream(), GenAIFrameworkConstants.UTF_8))) {
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			responseStr = response.toString();
			vendorStatusCode = String.valueOf(con.getResponseCode());
			vendorStatusMessage = con.getResponseMessage();
			thirdPartyResponseMap.put("responseStr",responseStr);
					
			
		}catch(Exception e){
			InputStream es = con.getErrorStream();
			Map<String, String> errorMap = new HashMap<String, String>();
		    if (es != null) {
		    	responseStr = new BufferedReader(new InputStreamReader(es))
		            .lines().collect(Collectors.joining("\n"));
		        logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + "[GenAIEngineInvoker] Error response: " + responseStr);
		        errorMap = parseErrorResponse(responseStr);
		        
		        vendorStatusCode = errorMap.get("errorCode");
		        vendorStatusMessage = errorMap.get("errorMessage");
		    }
		    thirdPartyResponseMap.put("responseStr","");
		}
		
		thirdPartyResponseMap.put("vendorStatusCode",vendorStatusCode);
		thirdPartyResponseMap.put("vendorStatusMessage",vendorStatusMessage);
		logger.info("[GENAI][getThirdPartyResponseMap]thirdPartyResponseMap {} "+thirdPartyResponseMap);
		return thirdPartyResponseMap;
	}

	
	
	

	public JSONObject makeCall() {
		Map<String, String> headers = new HashMap<>();
		headers.put(EngineConstants.CONTENT_TYPE, "application/json");
		headers.put(EngineConstants.API_KEY, Engine_Key);
		JSONObject finalVendorResultMap = new JSONObject();
		URL url;
		HttpURLConnection con = null;
		try {
			url = new URL(API_URL + "?api-version=" + API_VERSION);
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				con.setRequestProperty(entry.getKey(), entry.getValue());
			}
			
			OutputStream os = con.getOutputStream();
			byte[] input = prompt.getBytes(GenAIFrameworkConstants.UTF_8);
			os.write(input, 0, input.length);
			
			Map<String,String> thirdPartyResponseMap = getThirdPartyResponseMap(con);
			
			String response = thirdPartyResponseMap.get("responseStr");
			int responsCode = Integer.valueOf(thirdPartyResponseMap.get("vendorStatusCode"));
			String responseMessage = thirdPartyResponseMap.get("vendorStatusMessage");

			finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, response);
			finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, responseMessage);
			finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, responsCode);
			
		} catch (Exception e){
			logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception occured to create connection {} " +  e);
			finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, 999);
			finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, "");
			finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, "Engine invocation failed.");
		}finally{
			if(con != null)
				con.disconnect();
		}
		logger.error("[GENAI][MAKECALL] finalVendorResultMap"+finalVendorResultMap);
		return finalVendorResultMap;
	}

	public JSONObject retry(int maxretry, int sleeptime) throws InterruptedException {
		JSONObject vendorResponse = null;
		int retrycount = 0;
		while(maxretry > 0){
			Thread.sleep(maxretry * sleeptime);
			maxretry--;
			retrycount++;
			vendorResponse = makeCall();
			int status_code = (int) vendorResponse.get(GenAIFrameworkConstants.STATUS_CODE);
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " retry : " + retrycount + " , response got : " + vendorResponse);
			
			if(status_code == HttpStatus.SC_OK ){
				break;
			}
		}
		return vendorResponse;
		
	}
	
	public static Map<String, String> parseErrorResponse(String responseStr) {
	    Map<String, String> errorMap = new HashMap<>();

	    if (responseStr == null || responseStr.isEmpty()) {
	    	logger.error(" Response str is null or empty in error response case {} " + responseStr);
	    	errorMap.put(GenAIFrameworkConstants.ERROR_CODE, GenAIFrameworkConstants.ERROR_CODE_VALUE);
	        errorMap.put(GenAIFrameworkConstants.ERROR_MESSAGE, GenAIFrameworkConstants.ERROR_MESSAGE_VALUE);
	        return errorMap;
	    }

	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        JsonNode root = mapper.readTree(responseStr);
	        
	        JsonNode errorNode = root.path("error");
	        String errorCode = errorNode.path("code").asText();
	        String errorMessage = errorNode.path("message").asText();

	        if ("content_filter".equalsIgnoreCase(errorCode)) {

	            String statusCode = errorNode.path("status").asText("400");
	
	            JsonNode innerError = errorNode.path("innererror");
	            String violationType = innerError.path("code").asText();
	
	            JsonNode filterResults = innerError.path("content_filter_result");
	
	            List<String> triggeredReasons = new ArrayList<>();
	
	            if (filterResults != null && filterResults.isObject()) {
	                Iterator<Map.Entry<String, JsonNode>> fields = filterResults.fields();
	
	                while (fields.hasNext()) {
	                    Map.Entry<String, JsonNode> entry = fields.next();
	                    String category = entry.getKey();
	                    JsonNode value = entry.getValue();
	
	                    boolean filtered = value.path("filtered").asBoolean(false);
	                    boolean detected = value.path("detected").asBoolean(false);
	
	                    if (filtered || detected) {
	                        triggeredReasons.add(category);
	                    }
	                }
	            }
	
	            String reasonStr = String.join(", ", triggeredReasons);
	
	            String customMessage = "Content Filter: " + errorMessage
	                    + " Content Filter Type - " + violationType
	                    + ", Reason - " + reasonStr;
		        errorMap.put(GenAIFrameworkConstants.ERROR_CODE, statusCode);
		        errorMap.put(GenAIFrameworkConstants.ERROR_MESSAGE, customMessage);
	        } else {
	            errorMap.put(GenAIFrameworkConstants.ERROR_CODE, errorCode);
	            errorMap.put(GenAIFrameworkConstants.ERROR_MESSAGE, errorMessage);
	        }
	    } catch (Exception e) {
	    	logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception occured during error response parsing {} " +  e);
	        errorMap.put(GenAIFrameworkConstants.ERROR_CODE, GenAIFrameworkConstants.ERROR_CODE_VALUE);
	        errorMap.put(GenAIFrameworkConstants.ERROR_MESSAGE, GenAIFrameworkConstants.ERROR_MESSAGE_VALUE);
	    }

	    return errorMap;
	}
	
//	public static Map<String, String> parseErrorResponse(String responseStr) {
//
//	    Map<String, String> errorMap = new HashMap<>();
//
//	    String errorCode = "";
//	    String errorMessage = "";
//
//	    try {
//	        // Extract error code
//	        String codeKey = "\"code\":\"";
//	        int codeStart = responseStr.indexOf(codeKey);
//
//	        if (codeStart != -1) {
//	            codeStart += codeKey.length();
//	            int codeEnd = responseStr.indexOf("\"", codeStart);
//	            errorCode = responseStr.substring(codeStart, codeEnd);
//	        }
//
//	        // Extract error message
//	        String msgKey = "\"message\":\"";
//	        int msgStart = responseStr.indexOf(msgKey);
//
//	        if (msgStart != -1) {
//	            msgStart += msgKey.length();
//	            int msgEnd = responseStr.indexOf("\"", msgStart);
//	            errorMessage = responseStr.substring(msgStart, msgEnd);
//	        }
//
//	    } catch (Exception e) {
//	        // fallback safety
//	        errorCode = "999";
//	        errorMessage = "Failed to parse Exception error response";
//	    }
//
//	    errorMap.put("errorCode", errorCode);
//	    errorMap.put("errorMessage", errorMessage);
//
//	    return errorMap;
//	}

	/*
	 * Exponential backoff retry mechanism
	 */
	public JSONObject retryWithBackoff(int maxRetries, int baseDelayMs) throws InterruptedException {
		JSONObject vendorResponse = null;
		int retryCount = 1;
		while (retryCount < maxRetries) {
			if (retryCount > 0) {
				
					long delay = (long) (baseDelayMs * Math.pow(2, retryCount - 1));
					Thread.sleep(delay);
				
			}
			vendorResponse = makeCall();
			int statusCode = (int) vendorResponse.get(GenAIFrameworkConstants.STATUS_CODE);
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " exponential backoff retry : " + (retryCount + 1) + " , response got : " + vendorResponse);

			if (statusCode == HttpStatus.SC_OK) {
				break;
			}
			retryCount++;
		}
		return vendorResponse;
	}

}

package com.tcs.genai.engine.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.genai.engine.adapter.EngineInvokerAdapter;
import com.tcs.genai.engine.adapter.EngineInvokerFactory;
import com.tcs.genai.engine.audit.Audit;
import com.tcsion.ml.metadatahandler.audit.AimlAuditAndUsageDetails;
import com.tcsion.ml.metadatahandler.audit.kafka.*;
import com.tcsion.ml.metadatahandler.util.ExceptionLogUtility;
import com.tcs.genai.prompt.service.Impl.PayloadGeneratorService;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

@SuppressWarnings("unchecked")
public class EngineInvocationService {
	private static final Log logger = LogFactory
			.getLog(EngineInvocationService.class);
	
	Map<String, HashMap<String, String>> vendorProps = null;
	private Map<String, Object> invocationContext;
    boolean tokenAuditNeeded = false;

	
	public EngineInvocationService() {
		this.vendorProps = new HashMap<>();
		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + " Engine Invocation Service Constructor.");
	}
	
	public EngineInvocationService(Map<String, HashMap<String, String>> vendorProps) {
		this.vendorProps = vendorProps;
		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + " Engine Invocation Service Constructor with vendorProps.");
	}


	public EngineInvocationService(
	        Map<String, HashMap<String, String>> vendorProps,
	        Map<String, Object> invocationContext,
	        boolean tokenAuditNeeded) {

	    this.vendorProps = vendorProps;
	    this.tokenAuditNeeded = tokenAuditNeeded;
	    this.invocationContext = invocationContext;


	    logger.error(GenAIFrameworkConstants.LOGGER_DEBUG
	            + " Engine Invocation Service Constructor with context "
	            + "invocationContext" + invocationContext);
	}

	
	//Entry Point
	public JSONObject invokeAdvanceAiEngine(JSONObject payload, Map<String, String> parameterMap) {
		JSONObject finalResult = new JSONObject();

		if (parameterMap == null || parameterMap.isEmpty()) {
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " : " + GenAIFrameworkConstants.GEN_400_MSG);
			finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_400);
			finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_400_MSG);
			return finalResult;
		}
		
		String vendorName = parameterMap.get(GenAIFrameworkConstants.SERVICE_NAME);
    	
    	if (vendorName == null || vendorName.isEmpty()) {
            logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " : " + GenAIFrameworkConstants.GEN_412_MSG);
            finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_412);
            finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_412_MSG);
            return finalResult;
        }
    	
    	EngineInvokerAdapter adapter = EngineInvokerFactory.getAdapter(vendorName);
        
        if (adapter == null) {
            logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " : " + GenAIFrameworkConstants.GEN_404_MSG + vendorName);
            finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_404);
            finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_404_MSG + vendorName);
            return finalResult;
        }

        try {
            if (GenAIFrameworkConstants.GEMINI_3_5_FLASH.equals(vendorName)) {
                finalResult = adapter.invokeAiEngine(payload, vendorProps);
            } else {
                finalResult = adapter.invokeAiEngine(payload, vendorProps);
            }
            logger.error(GenAIFrameworkConstants.LOGGER_INFO+ " : Final Response from engine  - " + finalResult.toJSONString());         
            //return finalResult;

        } catch (Exception e) { 

        	logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception Occured while invoking third-party AI service : {} " + e);
            finalResult.put(GenAIFrameworkConstants.VENDOR_RESPONSE, null);
            finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_410);
			finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_410_MSG);
            return finalResult;
        } 
       
         
        try{
        	if (tokenAuditNeeded && finalResult.containsKey(GenAIFrameworkConstants.TOKENS) && finalResult.get(GenAIFrameworkConstants.TOKENS) != null) {
                
                JSONObject tokens = (JSONObject) finalResult.get("tokens");
                

                final Map<String, Long> tokensMap = new HashMap<>();

                for (Object key : tokens.keySet()) {
                	tokensMap.put((String) key, ((Number) tokens.get(key)).longValue());
                }
                
                //ExecutorService executorService = Executors.newSingleThreadExecutor();
                logger.info(GenAIFrameworkConstants.LOGGER_INFO
                        + " Going to log the token consumption detail : " + tokensMap.toString());

				// Old executor code - only audit, no Kafka publish
				/*
				executorService.submit(new Runnable() {
				    @Override
				    public void run() {
				        Audit audit = new Audit();
				        audit.auditTable(invocationContext, tokensMap);
				    }
				});
				executorService.shutdown();
				*/
                logger.error("invocationContext is {} "+ invocationContext);
                try {
                		logger.error("Inside try Extracting audit details from invocationContext for Kafka publish: " + invocationContext);
		            Long appId = ((Number) invocationContext.get(GenAIFrameworkConstants.APPID)).longValue();
		            Long orgId = ((Number) invocationContext.get(GenAIFrameworkConstants.ORGID)).longValue();
		            long useCaseId = ((Number) invocationContext.get(GenAIFrameworkConstants.USECASEID)).longValue();
		            int apidId = ((Number) invocationContext.get(GenAIFrameworkConstants.APIID)).intValue();
		            String engineId = String.valueOf(invocationContext.get(GenAIFrameworkConstants.SERVICEID));

		            long usecaseBucketId = invocationContext.get(GenAIFrameworkConstants.USECASEBUCKETID) != null
		                    ? ((Number) invocationContext.get(GenAIFrameworkConstants.USECASEBUCKETID)).longValue() : 0L;
		            String inputKey1 = invocationContext.get(GenAIFrameworkConstants.INPUTKEY1) != null
		                    ? invocationContext.get(GenAIFrameworkConstants.INPUTKEY1).toString() : "";
		            String inputKey2 = invocationContext.get(GenAIFrameworkConstants.INPUTKEY2) != null
		                    ? invocationContext.get(GenAIFrameworkConstants.INPUTKEY2).toString() : "";
		            String inputKey3 = invocationContext.get(GenAIFrameworkConstants.INPUTKEY3) != null
		                    ? invocationContext.get(GenAIFrameworkConstants.INPUTKEY3).toString() : "";
		            String inputKey4 = invocationContext.get(GenAIFrameworkConstants.INPUTKEY4) != null
		                    ? invocationContext.get(GenAIFrameworkConstants.INPUTKEY4).toString() : "";
		            long key1 = invocationContext.get(GenAIFrameworkConstants.KEY1) != null
		                    ? ((Number) invocationContext.get(GenAIFrameworkConstants.KEY1)).longValue() : 0L;
		            long key2 = invocationContext.get(GenAIFrameworkConstants.KEY2) != null
		                    ? ((Number) invocationContext.get(GenAIFrameworkConstants.KEY2)).longValue() : 0L;
		            long key3 = invocationContext.get(GenAIFrameworkConstants.KEY3) != null
		                    ? ((Number) invocationContext.get(GenAIFrameworkConstants.KEY3)).longValue() : 0L;
		            long key4 = invocationContext.get(GenAIFrameworkConstants.KEY4) != null
		                    ? ((Number) invocationContext.get(GenAIFrameworkConstants.KEY4)).longValue() : 0L;
		            long loggedBy = invocationContext.get(GenAIFrameworkConstants.LOGGEDBY) != null
		                    ? ((Number) invocationContext.get(GenAIFrameworkConstants.LOGGEDBY)).longValue() : 0L;

		            long inputTokens = tokensMap.get(GenAIFrameworkConstants.INPUTTOKEN) != null
		                    ? tokensMap.get(GenAIFrameworkConstants.INPUTTOKEN) : 0L;
		            long outputTokens = tokensMap.get(GenAIFrameworkConstants.OUTPUTTOKEN) != null
		                    ? tokensMap.get(GenAIFrameworkConstants.OUTPUTTOKEN) : 0L;
		            long reasoningTokens = tokensMap.get(GenAIFrameworkConstants.REASONINGTOKEN) != null
		                    ? tokensMap.get(GenAIFrameworkConstants.REASONINGTOKEN) : 0L;

		            // Build payload matching aimlAuditUsageDetails structure
		            HashMap<String, String> auditMessage = new HashMap<>();
		            auditMessage.put("usecaseId", String.valueOf(useCaseId));
		            auditMessage.put("appId", String.valueOf(appId));
		            auditMessage.put("orgId", String.valueOf(orgId));
		            auditMessage.put("apidId", String.valueOf(apidId));
		            auditMessage.put("engineId", engineId);
		            auditMessage.put("usecaseBucketId", String.valueOf(usecaseBucketId));
		            auditMessage.put("inputKey1", inputKey1);
		            auditMessage.put("inputKey2", inputKey2);
		            auditMessage.put("inputKey3", inputKey3);
		            auditMessage.put("inputKey4", inputKey4);
		            auditMessage.put("key1", String.valueOf(key1));
		            auditMessage.put("key2", String.valueOf(key2));
		            auditMessage.put("key3", String.valueOf(key3));
		            auditMessage.put("key4", String.valueOf(key4));
		            auditMessage.put("inputTokens", String.valueOf(inputTokens));
		            auditMessage.put("outputTokens", String.valueOf(outputTokens));
		            auditMessage.put("reasoningTokens", String.valueOf(reasoningTokens));
		            auditMessage.put("loggedBy", String.valueOf(loggedBy));
		            org.json.JSONObject jsonobj= new org.json.JSONObject(auditMessage);
//		            ObjectMapper objmap = new ObjectMapper();
//		            String json = objmap.writeValueAsString(auditMessage);
		            
		            logger.error("The value of jsonobj is:{} "+jsonobj);
		            JSONArray jArray = new JSONArray();
		            jArray.add(jsonobj);
		            org.json.JSONObject aiml_jobj = new org.json.JSONObject();
		            aiml_jobj.put("aimlAuditUsageDetails",jArray);
		            logger.error("The value of aiml_jobj is:{} "+aiml_jobj);
		           
		            try{
		            	AimlAuditAndUsageDetails obj = new AimlAuditAndUsageDetails();
		            	obj.executeAIMLUsageAudit(aiml_jobj,apidId);
		            }catch(Exception e){
		            		ExceptionLogUtility.logException("exception while auditing aimlusagedetails to kafka", e, logger);
		            	}
//		            boolean published = KafkaProducerEngineInvocationService.publishToKafka("aimlAuditUsageDetails", appId, orgId, auditMessage.toString());
//		            logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " : aimlAuditUsageDetails Kafka publish status - " + published);
		        } catch (Exception kafkaEx) {
		            logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " : Exception while publishing aimlAuditUsageDetails to Kafka: " + kafkaEx);
		        }

            }
        }catch (Exception e) {
			logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + ": Exception occurred while token Audit thread: ",e);
		}
        
        return finalResult;
	}
	

	public JSONObject invokeAdvanceAiEngine(String sourceDoc, String extractionType) {
		JSONObject finalResult = new JSONObject();
		logger.info("Inside invokeAdvanceAiEngine");
		try { 
        	
			String vendorName = vendorProps.keySet().iterator().next();
			
        	if (vendorName == null || vendorName.isEmpty()) {
                logger.error("[GENAI] Vendor name is missing.");
                finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_412);
                finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_412_MSG);
                return finalResult;
            } else {
            	logger.error("[[FLAG]] vendorName: " + vendorName);
            }
        	
            EngineInvokerAdapter adapter = EngineInvokerFactory.getAdapter(vendorName); 
            
            if (adapter == null) {
                logger.error("[GENAI] No adapter found for vendor: " + vendorName);
                finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_404);
                finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_404_MSG + vendorName);
                return finalResult;
            }
            
            finalResult = adapter.invokeEngine(sourceDoc, extractionType, vendorProps);
            
            return finalResult;

        } catch (Exception e) { 

        	logger.error("[GENAI] Error in fetching data from engine!", e);
            finalResult.put("extractedText", null);
            finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_410);
			finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_410_MSG);
            return finalResult;
        }
		
	}
}
















//package com.tcs.genai.engine.service;

//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.json.simple.JSONObject;
//
//import com.tcs.genai.engine.adapter.EngineInvokerAdapter;
//import com.tcs.genai.engine.adapter.EngineInvokerFactory;
//import com.tcs.genai.engine.audit.Audit;
//import com.tcs.genai.prompt.service.Impl.PayloadGeneratorService;
//import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
//
//@SuppressWarnings("unchecked")
//public class EngineInvocationService {
//	private static final Log logger = LogFactory
//			.getLog(EngineInvocationService.class);
//	
//	Map<String, HashMap<String, String>> vendorProps = null;
//	private Map<String, Object> invocationContext;
//    boolean tokenAuditNeeded = false;
//
//	
//	public EngineInvocationService() {
//		this.vendorProps = new HashMap<>();
//		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + " Engine Invocation Service Constructor.");
//	}
//	
//	public EngineInvocationService(Map<String, HashMap<String, String>> vendorProps) {
//		this.vendorProps = vendorProps;
//		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + " Engine Invocation Service Constructor with vendorProps.");
//	}
//
//
//	public EngineInvocationService(
//	        Map<String, HashMap<String, String>> vendorProps,
//	        Map<String, Object> invocationContext,
//	        boolean tokenAuditNeeded) {
//
//	    this.vendorProps = vendorProps;
//	    this.tokenAuditNeeded = tokenAuditNeeded;
//	    this.invocationContext = invocationContext;
//
//
//	    logger.error(GenAIFrameworkConstants.LOGGER_DEBUG
//	            + " Engine Invocation Service Constructor with context "
//	            + "invocationContext" + invocationContext);
//	}
//
//	
//	//Entry Point
//	public JSONObject invokeAdvanceAiEngine(JSONObject payload, Map<String, String> parameterMap) {
//		JSONObject finalResult = new JSONObject();
//
//		if (parameterMap == null || parameterMap.isEmpty()) {
//			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " : " + GenAIFrameworkConstants.GEN_400_MSG);
//			finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_400);
//			finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_400_MSG);
//			return finalResult;
//		}
//		
//		String vendorName = parameterMap.get(GenAIFrameworkConstants.SERVICE_NAME);
//    	
//    	if (vendorName == null || vendorName.isEmpty()) {
//            logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " : " + GenAIFrameworkConstants.GEN_412_MSG);
//            finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_412);
//            finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_412_MSG);
//            return finalResult;
//        }
//    	
//    	EngineInvokerAdapter adapter = EngineInvokerFactory.getAdapter(vendorName);
//        
//        if (adapter == null) {
//            logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " : " + GenAIFrameworkConstants.GEN_404_MSG + vendorName);
//            finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_404);
//            finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_404_MSG + vendorName);
//            return finalResult;
//        }
//
//        try { 
//            finalResult = adapter.invokeAzureAiEngine(payload, vendorProps);
//            logger.error(GenAIFrameworkConstants.LOGGER_INFO+ " : Final Response from engine  - " + finalResult.toJSONString());         
//            //return finalResult;
//
//        } catch (Exception e) { 
//
//        	logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception Occured while invoking third-party AI service : {} " + e);
//            finalResult.put(GenAIFrameworkConstants.VENDOR_RESPONSE, null);
//            finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_410);
//			finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_410_MSG);
//            return finalResult;
//        } 
//        
//        try{
//        	if (tokenAuditNeeded && finalResult.containsKey(GenAIFrameworkConstants.TOKENS) && finalResult.get(GenAIFrameworkConstants.TOKENS) != null) {
//                
//                JSONObject tokens = (JSONObject) finalResult.get("tokens");
//                
//
//                final Map<String, Long> tokensMap = new HashMap<>();
//
//                for (Object key : tokens.keySet()) {
//                	tokensMap.put((String) key, ((Number) tokens.get(key)).longValue());
//                }
//                
//                ExecutorService executorService = Executors.newSingleThreadExecutor();
//                logger.info(GenAIFrameworkConstants.LOGGER_INFO
//                        + " Going to log the token consumption detail : " + tokensMap.toString());
//
//				executorService.submit(new Runnable() {
//				    @Override
//				    public void run() {
//				        Audit audit = new Audit();
//				        audit.auditTable(invocationContext, tokensMap);
//				    }
//				});
//				executorService.shutdown();
//
//            }
//        }catch (Exception e) {
//			logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + ": Exception occurred while token Audit thread: ",e);
//		}
//        
//        return finalResult;
//	}
//	
//
//	public JSONObject invokeAdvanceAiEngine(String sourceDoc, String extractionType) {
//		JSONObject finalResult = new JSONObject();
//		logger.info("Inside invokeAdvanceAiEngine");
//		try { 
//        	
//			String vendorName = vendorProps.keySet().iterator().next();
//			
//        	if (vendorName == null || vendorName.isEmpty()) {
//                logger.error("[GENAI] Vendor name is missing.");
//                finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_412);
//                finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_412_MSG);
//                return finalResult;
//            } else {
//            	logger.error("[[FLAG]] vendorName: " + vendorName);
//            }
//        	
//            EngineInvokerAdapter adapter = EngineInvokerFactory.getAdapter(vendorName); 
//            
//            if (adapter == null) {
//                logger.error("[GENAI] No adapter found for vendor: " + vendorName);
//                finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_404);
//                finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_404_MSG + vendorName);
//                return finalResult;
//            }
//            
//            finalResult = adapter.invokeEngine(sourceDoc, extractionType, vendorProps);
//            
//            return finalResult;
//
//        } catch (Exception e) { 
//
//        	logger.error("[GENAI] Error in fetching data from engine!", e);
//            finalResult.put("extractedText", null);
//            finalResult.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_410);
//			finalResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_410_MSG);
//            return finalResult;
//        }
//		
//	}
//}

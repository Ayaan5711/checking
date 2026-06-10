package com.tcs.genai.engine.audit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import com.tcsion.ml.metadatahandler.audit.AimlAuditAndUsageDetails;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

import java.util.Map;



public class Audit {
	
	private static final Log logger = LogFactory.getLog(Audit.class);

		public void auditTable(Map<String, Object> invocationContext,Map<String, Long> tokens) {
		
		    try {
		        long appId = (long) invocationContext.get(GenAIFrameworkConstants.APPID);
		
		        long orgId = (long) invocationContext.get(GenAIFrameworkConstants.ORGID);
		
		        int apiId = (int) invocationContext.get(GenAIFrameworkConstants.APIID) ;
		
		        long useCaseId = (long) invocationContext.get(GenAIFrameworkConstants.USECASEID) ;
		
		        String serviceId = (String) invocationContext.get(GenAIFrameworkConstants.SERVICEID) ;
		        
		        long usecaseBucketId = invocationContext.get(GenAIFrameworkConstants.USECASEBUCKETID) != null
		                ? ((Number) invocationContext.get(GenAIFrameworkConstants.USECASEBUCKETID)).longValue()
		                : 0L;

		        String inputKey1 = invocationContext.get(GenAIFrameworkConstants.INPUTKEY1) != null
		                ? invocationContext.get(GenAIFrameworkConstants.INPUTKEY1).toString()
		                : "";

		        String inputKey2 = invocationContext.get(GenAIFrameworkConstants.INPUTKEY2) != null
		                ? invocationContext.get(GenAIFrameworkConstants.INPUTKEY2).toString()
		                : "";

		        String inputKey3 = invocationContext.get(GenAIFrameworkConstants.INPUTKEY3) != null
		                ? invocationContext.get(GenAIFrameworkConstants.INPUTKEY3).toString()
		                : "";

		        String inputKey4 = invocationContext.get(GenAIFrameworkConstants.INPUTKEY4) != null
		                ? invocationContext.get(GenAIFrameworkConstants.INPUTKEY4).toString()
		                : "";

		        long key1 = invocationContext.get(GenAIFrameworkConstants.KEY1) != null
		                ? ((Number) invocationContext.get(GenAIFrameworkConstants.KEY1)).longValue()
		                : 0L;

		        long key2 = invocationContext.get(GenAIFrameworkConstants.KEY2) != null
		                ? ((Number) invocationContext.get(GenAIFrameworkConstants.KEY2)).longValue()
		                : 0L;

		        long key3 = invocationContext.get(GenAIFrameworkConstants.KEY3) != null
		                ? ((Number) invocationContext.get(GenAIFrameworkConstants.KEY3)).longValue()
		                : 0L;

		        long key4 = invocationContext.get(GenAIFrameworkConstants.KEY4) != null
		                ? ((Number) invocationContext.get(GenAIFrameworkConstants.KEY4)).longValue()
		                : 0L;
		        
				long loggedBy = invocationContext.get(GenAIFrameworkConstants.LOGGEDBY) != null
				        ? ((Number) invocationContext.get(GenAIFrameworkConstants.LOGGEDBY)).longValue()
				        : 0L;


		
		        JSONObject auditDetails = new JSONObject();
		
		        auditDetails.put("usecaseId", useCaseId);
		        auditDetails.put("appId", appId);
		        auditDetails.put("apidId", apiId);
		        auditDetails.put("orgId", orgId);
		
		        auditDetails.put("engineId", serviceId);
		        auditDetails.put("usecaseBucketId", usecaseBucketId);

		        auditDetails.put("inputKey1", inputKey1);
		        auditDetails.put("inputKey2", inputKey2);
		        auditDetails.put("inputKey3", inputKey3);
		        auditDetails.put("inputKey4", inputKey4);

		        auditDetails.put("key1", key1);
		        auditDetails.put("key2", key2);
		        auditDetails.put("key3", key3);
		        auditDetails.put("key4", key4);
		
		        auditDetails.put("inputTokens", tokens.get(GenAIFrameworkConstants.INPUTTOKEN));
		        auditDetails.put("outputTokens", tokens.get(GenAIFrameworkConstants.OUTPUTTOKEN));
		        auditDetails.put("reasoningTokens", tokens.get(GenAIFrameworkConstants.REASONINGTOKEN));
		
		        auditDetails.put("loggedBy", loggedBy);
		
		        JSONArray auditArray = new JSONArray();
		        auditArray.put(auditDetails);
		
		        JSONObject auditJson = new JSONObject();
		        auditJson.put("aimlAuditUsageDetails", auditArray);
		        logger.info("[Flag] Audit auditTable : " + auditJson.toString());
		
		        AimlAuditAndUsageDetails ads = new AimlAuditAndUsageDetails();
		        ads.executeAIMLUsageAudit(auditJson, apiId);
		
		    } catch (Exception e) {
		        logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + ": Exception occurred while adding audit details: ",e);
		    }
		}
}
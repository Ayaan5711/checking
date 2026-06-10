package com.tcs.genai.prompt.service.Impl;

//import java.util.HashMap;
//import java.util.Iterator;
import java.util.Map;
//import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tcs.genai.prompt.bean.ResponseMaster;
import com.tcs.genai.prompt.dao.UseCasePromptMappingDAO;
import com.tcs.genai.prompt.service.Impl.AssembleCommonPayload;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.metadatahandler.util.DBConnectionUtility;

public class PayloadGeneratorService {	
	private static final Log logger = LogFactory
			.getLog(PayloadGeneratorService.class);	
	public String recognizer_entity = "NA";
	
	public DBConnectionUtility dbConnection = new DBConnectionUtility();
	ResponseMaster responseMasterObject = new ResponseMaster();
	
	public PayloadGeneratorService() {
		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + " Payload Generator service Constructor without recognizer_entity.");
	}
	
	public PayloadGeneratorService(String recognizer_entity) {
		this.recognizer_entity = recognizer_entity;
		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + " Payload Generator service Constructor with recognizer_entity as: ." + recognizer_entity);
	}
	
	/**
	 * This method is the entry point for integration AI FW API.
	 * functionality:
	 * 1. fetch the propmt templates from DB based on usecase id, App id, API id, org id.
	 * 2. forming a common generic json which will be used to create payload specific to engine. 
	 * 3. The final vendor-specific payload is returned as a 'JSONObject'.
	 * 
	 * @param usecaseId The ID of the use case for which the payload is generated.
	 * @param vendorServiceId The id of the vendor for which the payload is specific.
	 * @param apiId The api id requiring this dynamic prompt generation.
	 * @param appId The called FW app id.
	 * @param orgId organization id.
	 * @return A `JSONArray` representing the final payload for the specified use case, vendor, and API.
	 */
	@SuppressWarnings("unchecked")
	public JSONObject generatePayload(final long useCaseId, final Map<String, String> parameterMap, final int apiId,final long orgId,long appId) {
		
		
		JSONObject finalPayloadForVendor = new JSONObject();

		if (parameterMap == null || parameterMap.isEmpty()) {
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_400);
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_400_MSG);
			return finalPayloadForVendor;
		}
		
		/*
		 * Fetching prompt mapping for the API of the usecase. It will check cache first, if not found then 
		 * will fetch from table "tcs_ion_gen_ai_usecase_prompt_sequence_mapping" in Global schema.
		 * If no entry present in table, null value will be received.
		 */
		long vendorserviceid = Long.valueOf(parameterMap.get(GenAIFrameworkConstants.SERVICE_ID));

		UseCasePromptMappingDAO promptMappingDAO  = new UseCasePromptMappingDAO(responseMasterObject);
		String promptList  = promptMappingDAO.getPromptMapping(apiId, useCaseId, orgId, vendorserviceid, recognizer_entity);

//		logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "vendorserviceid {} "+vendorserviceid);

		
		if (responseMasterObject.getStatus().equals(GenAIFrameworkConstants.GEN_401) || responseMasterObject.getStatus().equals(GenAIFrameworkConstants.GEN_408) || responseMasterObject.getStatus().equals(GenAIFrameworkConstants.GEN_409)) {
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_CODE, responseMasterObject.getStatus());
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_MESSAGE, responseMasterObject.getMsg());
			return finalPayloadForVendor;
		}
		
		
		/*
		 * Fetching prompt details like GuardRails and FewShots and adding that to a common payLoad.
		 */		
		JSONObject commonPayload = null;
		AssembleCommonPayload payLoadCreator = new AssembleCommonPayload(this.dbConnection, responseMasterObject);
		try {
			commonPayload = payLoadCreator.commonPayloadCreator(promptList);
		} catch (Exception e) {
			logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception Occured during Common Payload Creation {}: " + e);
		}
		
		if (commonPayload == null || commonPayload.isEmpty()) {
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_402);
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_402_MSG);
			return finalPayloadForVendor;
		} else if (responseMasterObject.getStatus().equals(GenAIFrameworkConstants.GEN_405) || responseMasterObject.getStatus().equals(GenAIFrameworkConstants.GEN_406) || responseMasterObject.getStatus().equals(GenAIFrameworkConstants.GEN_407)) {
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_CODE, responseMasterObject.getStatus());
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_MESSAGE, responseMasterObject.getMsg());
			return finalPayloadForVendor;
		} else {
			logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + "Success in creating Common Payload!");
		}
		
		/*
		 * Sending the common payLoad to the adapter and getting the Vendor specific final payLoad.
		 */	
		JSONObject vendorPayload = (JSONObject) payLoadCreator.getFinalVendorSpecificPayload(commonPayload, parameterMap);
		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + " Final Vendor Specific Payload Generated!");
		
		if(vendorPayload != null && !vendorPayload.isEmpty()) {
			finalPayloadForVendor.put("payload", vendorPayload);
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_200);
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_200_MSG);
		} else {
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_403);
			finalPayloadForVendor.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_403_MSG);
		}
		
		
		return finalPayloadForVendor;
	}

	public ResponseMaster getRm() {
		return responseMasterObject;
	}

	public void setRm(ResponseMaster rm) {
		this.responseMasterObject = rm;
	}
	
	
}

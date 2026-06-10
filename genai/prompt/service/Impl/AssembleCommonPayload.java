package com.tcs.genai.prompt.service.Impl;
//import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tcs.genai.prompt.bean.ResponseMaster;
import com.tcs.genai.prompt.dao.PromptMasterDAO;
import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapter;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.metadatahandler.util.DBConnectionUtility;
public class AssembleCommonPayload {

	private static final Log logger = LogFactory.getLog(AssembleCommonPayload.class);
    
    DBConnectionUtility dbConnectionUtility = null;
    ResponseMaster responseMasterObject = new ResponseMaster();
	public AssembleCommonPayload(DBConnectionUtility dbConnectionUtility, ResponseMaster responseMasterObject) {
		super();
		this.dbConnectionUtility = dbConnectionUtility;
		this.responseMasterObject = responseMasterObject;
	}
	/*
	 * Iterating through the prompt Id list and fetching data from DB adding it to the common payload
	 */
	@SuppressWarnings("unchecked")
	public JSONObject commonPayloadCreator(String promptLisForUseCase){
		logger.info("[[FLAG]] Inside Common Payload Creator {}: ");
		JSONArray promptListSeqPayload = new JSONArray();
	    JSONObject comonPayload = new JSONObject();
	    PromptMasterDAO masterPromptDaoObj = null;
	    masterPromptDaoObj =  new PromptMasterDAO(this.dbConnectionUtility, responseMasterObject);
		try {
			String[] stringArray = promptLisForUseCase.split(",");
	        for (String promptId : stringArray) {
	        	JSONObject coomonPayloadForpromptId = masterPromptDaoObj.fetchPromptDetailsFromMasterPromptTable(Long.parseLong(promptId));
	        	if(coomonPayloadForpromptId != null) {
	        		responseMasterObject.setStatus(GenAIFrameworkConstants.GEN_202);
					responseMasterObject.setMsg(GenAIFrameworkConstants.GEN_202_MSG);
	        		promptListSeqPayload.add(coomonPayloadForpromptId);
	        	} else {
	        		logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " Failure while creating common payload! ");
	        		return null;
	        	}
	        }
			comonPayload.put("promptidlist", promptListSeqPayload);
		} catch (Exception e) {
			logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception occured inside commonPayloadCreator {} " + e.getMessage(), e);
		}
	    return comonPayload;
	}

	public JSONObject getFinalVendorSpecificPayload(JSONObject commonPayload,
			Map<String, String> parameterMap) {
		
		JSONObject finalPayload = new JSONObject();
		String vendorName = parameterMap.get(GenAIFrameworkConstants.SERVICE_NAME);
		VendorPayloadService service = new VendorPayloadService();		
		VendorPayloadAdapter vendorAdaptor = service.getVendorAdaptor(vendorName);
		if(vendorAdaptor == null){
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " No adaptor present for the service : " + vendorName);
		}else{
			try {
				finalPayload = (JSONObject) vendorAdaptor.getPayload(commonPayload);
			} catch (Exception e) {
				logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " Exception occured while fetching Vendor Specific Payload : {} " + e);
			}
		}
		return finalPayload;
	}
}



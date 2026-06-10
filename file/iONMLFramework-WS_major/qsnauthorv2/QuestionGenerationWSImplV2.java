package com.tcs.ion.ml.ws.qsnauthorv2;

import java.sql.SQLException;
//import java.util.ArrayList;


//import java.util.ArrayList;



import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcs.ion.ml.ws.bean.TransactionRequestBean;
import com.tcs.ion.ml.ws.bean.TransactionResponseBean;
import com.tcs.ion.ml.ws.bean.WebServiceParams;
import com.tcs.ion.ml.ws.service.MergediONMLServlet;
import com.tcs.ion.ml.ws.util.MLWSConstants;
import com.tcsion.ml.metadatahandler.service.DbBasedLimitGuavaCacheLoaderService;
import com.tcsion.ml.metadatahandler.service.UserLevelEngineSelectionService;
import com.tcsion.ml.metadatahandler.util.CommercialVendorDetailsCacheLoader;
//import com.tcsion.ml.Utils.MlFrameworkConstants;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.util.MetadataUtility;

@SuppressWarnings("all")
public class QuestionGenerationWSImplV2 implements QuestionGenerationWSV2 {
	private final Log logger=LogFactory.getLog(QuestionGenerationWSImplV2.class);
	private QuestionGenerationV2 questionGenerationV2;
	
	
	private final DbBasedLimitGuavaCacheLoaderService cacheLoader = DbBasedLimitGuavaCacheLoaderService.getInstance();
	private MetadataUtility metadataUtility = new MetadataUtility();
	CommercialVendorDetailsCacheLoader commercialCacheLoader =  MergediONMLServlet.commercialCacheLoader;
	UserLevelEngineSelectionService userLevelEngineSelectionService = new UserLevelEngineSelectionService();
	Map<String, HashMap<String, String>> vendorDetailsMap = null;
	
	public QuestionGenerationWSImplV2(final QuestionGenerationV2 questionGenerationV2){
		this.questionGenerationV2=questionGenerationV2;
	}
	@Override
	public TransactionResponseBean questionGenerationWebServiceImplement(TransactionRequestBean requestBean, 
			WebServiceParams wsParams) throws IllegalArgumentException, SQLException, Exception 
	{
		InputBean inputBean = new InputBean();
		TransactionResponseBean responsebean=new TransactionResponseBean();

		long startTimeDBLimits= System.currentTimeMillis();
		Map<String, String> textLimitsMap = cacheLoader.getTextLimits("ionml.QuestionGenerationSync");
		org.json.JSONObject textLimitsJson = new org.json.JSONObject(textLimitsMap);
		
		final int userRegisteredServiceId = requestBean.getQuestionGenerationWSBeanV2().getUserRegisteredServiceId();
		
		logger.info("ws params {} "+ wsParams.toString());
		
		inputBean.setOrgId(String.valueOf(wsParams.getOrgId()));
		inputBean.setAppId(String.valueOf(wsParams.getAppId()));
		inputBean.setUserId(String.valueOf(wsParams.getUserId()));
		inputBean.setPort(MergediONMLServlet.gunicornDetails.getPortno());
		
		inputBean.setEntity(requestBean.getQuestionGenerationWSBeanV2().getEntity());
		inputBean.setUserInput(requestBean.getQuestionGenerationWSBeanV2().getUserInput());
		inputBean.setFilePaths(requestBean.getQuestionGenerationWSBeanV2().getFilePaths());
		inputBean.setFileType(requestBean.getQuestionGenerationWSBeanV2().getFileType());
		inputBean.setQuestionTypeConfigMap(requestBean.getQuestionGenerationWSBeanV2().getQuestionTypeConfigMap());
		inputBean.setDirectives(requestBean.getQuestionGenerationWSBeanV2().getDirectives()); 
		inputBean.setUserRegisteredServiceId(userRegisteredServiceId);

		inputBean.setMetadataBean(metadataUtility.fetchSyncMetadataFromCache(textLimitsJson, inputBean.getOrgId()));

		int apiId = commercialCacheLoader.getApiId("com.tcs.ion.ml.ws.bean.QuestionGenerationThread");
		logger.error("ApiId {} : "+apiId);
		inputBean.setApiID(String.valueOf(apiId));
		

		if(apiId != -1){
			if(userRegisteredServiceId != 0) {
				vendorDetailsMap = userLevelEngineSelectionService.getUserRegisteredServicesCredMap(wsParams.getOrgId(),
						Long.valueOf(wsParams.getAppId()), "0", apiId, String.valueOf(userRegisteredServiceId),
						wsParams.getUserId());
			} else {
				vendorDetailsMap = commercialCacheLoader.getCommercialPropertiesValue(apiId, wsParams.getOrgId(), wsParams.getCallerAppId(), "0",wsParams.getUserId());
			}
			inputBean.setAIvendorCredNodeMap(vendorDetailsMap);
		}
		Map<String, HashMap<String, String>> vendorMap =inputBean.getAIvendorCredNodeMap();
		if (vendorMap != null && !vendorMap.isEmpty()) {
		    String vendorName = vendorMap.keySet().iterator().next();
		    inputBean.setVendorName(vendorName);
		} else {
		    logger.error("Vendor details map is null/empty for apiId={}"+apiId +"orgId={} "+ wsParams.getOrgId()+ "appid={} "+inputBean.getAppId());
		}
		
		Map<String, String> parameterMap = new HashMap<>();
		parameterMap.put(GenAIFrameworkConstants.SERVICE_ID, String.valueOf(inputBean.getUserRegisteredServiceId()));
    	parameterMap.put("serviceName", inputBean.getVendorName());
		inputBean.setParameterMap(parameterMap);
		
		logger.error("inputBean:" + inputBean);
		

		if(!inputBean.getQuestionTypeConfigMap().isEmpty()) {
			logger.info("none of the keys in the requestbean are null or empty");
			final JSONObject jObject = questionGenerationV2.generateQuestions(inputBean, wsParams);
			logger.info("The final response is : " + jObject.toString());
			if( jObject.isEmpty()) {	
				  responsebean.setResponseMessage(MLWSConstants.FAILED);
				  responsebean.setResponseCode(999);
			}
			else {
				 responsebean.setJsonobject(jObject);
				 responsebean.setResponseMessage(MLWSConstants.SUCCESS_MSG);
				 responsebean.setResponseCode(200);
			}	
		}
		else {
			logger.error("Provide valid values/nonempty params");
			responsebean.setResponseMessage(MLWSConstants.INCORRECT_PARAMS);
			responsebean.setResponseCode(999);
		}
		return responsebean;
	}
}



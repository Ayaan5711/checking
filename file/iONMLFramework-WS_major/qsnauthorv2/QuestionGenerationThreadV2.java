package com.tcs.ion.ml.ws.qsnauthorv2;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

import com.tcs.ion.ml.ws.bean.WebServiceParams;
import com.tcs.ion.ml.ws.service.AuditLogService;
import com.tcs.ion.ml.ws.service.ServiceLocator;
import com.tcs.ion.ml.ws.util.ExceptionLogUtility;
import com.tcs.ion.ml.ws.util.MLWSConstants;
import com.tcsion.ml.metadatahandler.audit.VendorAuditImpl;
import com.tcsion.ml.metadatahandler.util.PythonLayerRequest;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.OutputBean;
import com.tcsion.ml.qsnauthorv2.inputtypes.KnowledgeBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.inputtypes.SmallFileBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.inputtypes.TextBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.util.QsnAuthConstants;

@SuppressWarnings("all")
public class QuestionGenerationThreadV2 implements PythonLayerRequest{
	ArrayList<JSONObject> jsonarray;
	InputBean inputBean;
	private OutputBean outputBean = new OutputBean();
		
	private final Log logger = LogFactory.getLog(QuestionGenerationThreadV2.class);
	private AuditLogService auditLogService=ServiceLocator.instance().getAuditLogService();
	WebServiceParams wsParams;
	
	public QuestionGenerationThreadV2(ArrayList<JSONObject> jsonarray, InputBean inputBean, WebServiceParams wsParams) {
		this.jsonarray = jsonarray;	
		this.inputBean = inputBean;
		this.wsParams = wsParams;
	}
	
	//@SuppressWarnings("unchecked")
	@Override
	public void run() {
		JSONObject jsonobject=new JSONObject();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		
		String request_id =null;
		
		try {
			wsParams.setCoreAIProcessStartDate(new Timestamp(System.currentTimeMillis())); 
			
			long uuid = UUID.randomUUID().getMostSignificantBits();
			long unsigned_uuid = Math.abs(uuid);
			request_id = "QSNGEN_"+unsigned_uuid;
			
			if ("kb".equalsIgnoreCase(inputBean.getFileType())){
				KnowledgeBasedAuthoring authoringKB = new KnowledgeBasedAuthoring();
				outputBean = authoringKB.questionAuthoring(this.inputBean);
			} 
			else if ("file".equalsIgnoreCase(inputBean.getFileType())){
				SmallFileBasedAuthoring authoringSmallFile = new SmallFileBasedAuthoring();
				outputBean = authoringSmallFile.questionAuthoring(this.inputBean);
			}
			else {
				TextBasedAuthoring authoringText = new TextBasedAuthoring();
				outputBean = authoringText.questionAuthoring(this.inputBean);
			} 

			logger.error("outputBean:" + outputBean);
			wsParams.setCoreAIProcessEndDate(new Timestamp(System.currentTimeMillis()));
			
			if (outputBean.getOutput()!=null){
				ArrayList<Map<String, String>> vendorAuditDetails = outputBean.getVendorAuditDetails();
				logger.error("Question Generation Sync vendorAuditDetails {} " + vendorAuditDetails);
				
				VendorAuditImpl vImplBean = new VendorAuditImpl();
				if(vendorAuditDetails != null && !vendorAuditDetails.isEmpty()){

					for (Map<String, String> auditMap : vendorAuditDetails) {
						auditMap.put("request_id", request_id);
					}

					vImplBean.initateVendorAuditThread(vendorAuditDetails);
					logger.error("Vendor Audit Completed for Question Generation sync.");
				}
			}
		}
		catch(Exception e) {
			ExceptionLogUtility.logException("Exception Occurred in QuestionGenerationThread: {}", e, logger);
			outputBean.setMessage(QsnAuthConstants.SOMETHING_IS_WRONG_IN_SCRIPT_EXECUTION);
			outputBean.setReturnCode(QsnAuthConstants.QSNGEN_999);
		}
		finally{
			LinkedHashMap<String,Object> dataMap=new LinkedHashMap<String, Object>();					
			dataMap.put("orgid", wsParams.getOrgId());
			dataMap.put("dcnid", wsParams.getDcnId());
			dataMap.put("appid", wsParams.getCallerAppId());
			dataMap.put("api_used", MLWSConstants.QUESTION_GENERATION);
			dataMap.put("request_params", inputBean.getUserInput().length() < 200 ? 
					inputBean.getUserInput() : inputBean.getUserInput().substring(0, 200)
					+ " ::: " +inputBean.getFilePaths()+" :::  "+inputBean.getQuestionTypeConfigMap());
			dataMap.put("key1",outputBean.getMessage());
			dataMap.put("key2", outputBean.getOutput());

			if(wsParams.getRequestReceivedDate()!=null){
				dataMap.put("Start",formatter.format(wsParams.getRequestReceivedDate()));
			}
			if(wsParams.getBizValidationStartDate()!=null){
				dataMap.put("KV Start",formatter.format(wsParams.getBizValidationStartDate()));
			}
			if(wsParams.getBizValidationEndDate()!=null){
				dataMap.put("KV End",formatter.format(wsParams.getBizValidationEndDate()));
			}
			if(wsParams.getUserDetailsFetchStartDate()!=null){
				dataMap.put("UDF Start",formatter.format(wsParams.getUserDetailsFetchStartDate()));
			}
			if(wsParams.getUserDetailsFetchEndDate()!=null){
				dataMap.put("UDF End",formatter.format(wsParams.getUserDetailsFetchEndDate()));
			}
			if(wsParams.getCoreAIProcessStartDate()!=null){
				dataMap.put("AI Start",formatter.format(wsParams.getCoreAIProcessStartDate()));
			}
			if(wsParams.getCoreAIProcessEndDate()!=null){
				dataMap.put("AI End",formatter.format(wsParams.getCoreAIProcessEndDate()));
			}
			dataMap.put("End",formatter.format(new Date()));
			
			auditLogService.logAuditEntry(wsParams.getAppId(), wsParams.getDcnId(), wsParams.getOrgId(), dataMap);

			jsonobject.put("Output", outputBean.getOutput());
			jsonobject.put("Message", outputBean.getMessage());
			jsonobject.put("ReturnCode", outputBean.getReturnCode());
			jsonobject.put("ReferenceID",request_id);
			jsonobject.put("ThirdPartyDetails", outputBean.getThirdPartyDetails());
			jsonarray.add(jsonobject);
		}
	}
}



package com.tcsion.ml.qsnauthorv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.Utils.ExceptionLogUtility;
import com.tcsion.ml.Utils.MlFrameworkConstants;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.OutputBean;
import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;
import com.tcsion.ml.qsnauthorv2.inputtypes.KnowledgeBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.inputtypes.SmallFileBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.inputtypes.TextBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.util.MetadataUtility;
import com.tcsion.ml.qsnauthorv2.util.Utility;


public class QuestionAuthoringRequestImpl {
	private static final Log logger = LogFactory.getLog(QuestionAuthoringRequestImpl.class);
	private ObjectMapper mapper = new ObjectMapper();
	private InputBean inputBean = new InputBean();
	private OutputBean outputBean = new OutputBean();
	private MetadataUtility metadataUtility = new MetadataUtility();
	private JSONParser parser = new JSONParser();
	
	public String processApiByUseCase(final JSONObject inputDetailsJson, JSONObject outputJson) throws Exception {
		String response="";
		if(inputDetailsJson!=null){
			logger.info("inputDetailsJson: "+inputDetailsJson);
			try{
				// Processing input in a separate file
				processInput(inputDetailsJson);

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
				
				response = this.prepareResponse(inputBean.getUserInput(),  
						outputBean.getReturnCode(), outputBean.getMessage(), outputBean.getOutput(), 
						outputJson, outputBean.getThirdPartyDetails(), outputBean.getVendorAuditDetails());
				logger.error("The final response is : "+ outputBean.getOutput());
			}catch(final Exception e){
				ExceptionLogUtility.logException(this.inputBean.getJobId()+" ** Error Occured in Question Generation: ", e, logger);
//				throw e;
			}
		}
		logger.error("Exiting QuestionAuthoringRequestImpl Class jobID: " + inputBean.getJobId() + " Request ID: "+ inputBean.getRequestId());
		return response;
	}
	
	
	
	private String prepareResponse(final String userInput, final String returnCode, 
			final String messege, final org.json.simple.JSONObject executionOutput, 
			final JSONObject outputJson, final org.json.simple.JSONObject thirdPartyDetails,
			ArrayList<Map<String, String>> vendorAuditDetailsArray) throws IOException, JSONException 
	{
		try
		{
			JSONObject executionOutputJson = new JSONObject(executionOutput);
	        logger.error("Inside Prepare Response. Execution Output: {} "+executionOutput+" Output JSON: {} "+outputJson);
	        if(outputJson.has(MlFrameworkConstants.RequestStatus)){
	        	String fieldName = outputJson.get(MlFrameworkConstants.RequestStatus).toString();
	        	String replaceString = fieldName.replace("<", "");
	            String replaceStringNew = replaceString.replace(">","");
	        	outputJson.remove(MlFrameworkConstants.RequestStatus);
	        	outputJson.put(replaceStringNew,messege);
	        	logger.info("messege : {} " + messege);
	        }else{
	        	logger.error("Missing RequestStatus from Output json");
	        }
	        if(outputJson.has(MlFrameworkConstants.GeneratedOutput)){
	        	String fieldName = outputJson.get(MlFrameworkConstants.GeneratedOutput).toString();
	        	String replaceString = fieldName.replace("<", "");
	            String replaceStringNew = replaceString.replace(">","");
	        	outputJson.remove(MlFrameworkConstants.GeneratedOutput);
				outputJson.put(replaceStringNew,executionOutputJson);
	        	logger.info("executionOutputJson : {} "+executionOutputJson);
	        }else{
	        	logger.error("Missing GeneratedOutput from Output json");
	        }
	        if(outputJson.has(MlFrameworkConstants.userInput)){
	        	String fieldName = outputJson.get(MlFrameworkConstants.userInput).toString();
	        	String replaceString = fieldName.replace("<", "");
	            String replaceStringNew = replaceString.replace(">","");
	        	outputJson.remove(MlFrameworkConstants.userInput);
				outputJson.put(replaceStringNew,userInput);
	        	logger.info("userInput : {} "+userInput);
	        }else{
	        	logger.error("Missing userInput from Output json");
	        }
	        if(outputJson.has(MlFrameworkConstants.ReturnCode)){
	        	String fieldName = outputJson.get(MlFrameworkConstants.ReturnCode).toString();
	        	String replaceString = fieldName.replace("<", "");
	            String replaceStringNew = replaceString.replace(">","");
	        	outputJson.remove(MlFrameworkConstants.ReturnCode);
				outputJson.put(replaceStringNew,returnCode);
	        	logger.info("returnCode : {} "+returnCode);
	        }else{
	        	logger.error("Missing GeneratedOutput from Output json");
	        }
	        if(outputJson.has(MlFrameworkConstants.THIRD_PARTY_DETAILS)){
	        	String fieldName = outputJson.get(MlFrameworkConstants.THIRD_PARTY_DETAILS).toString();
	        	String replaceString = fieldName.replace("<", "");
	            String replaceStringNew = replaceString.replace(">","");
	        	outputJson.remove(MlFrameworkConstants.THIRD_PARTY_DETAILS);
				outputJson.put(replaceStringNew,thirdPartyDetails);
	        	logger.info("thirdPartyDetails : {} "+thirdPartyDetails);
	        }else{
	        	logger.error("Missing GeneratedOutput from Output json");
	        }
		}
		catch(Exception e) {
			logger.error("Exception occurred in prepareResponse function : "+e);
		}
		
		outputJson.put(MlFrameworkConstants.VENDOR_AUDIT_DETAILS, vendorAuditDetailsArray);
        return outputJson.toString();
    }
	
	void processInput(final JSONObject inputDetailsJson) throws JSONException, JsonParseException, JsonMappingException, IOException, ParseException{
		inputBean.setApiID(inputDetailsJson.getString("apiID"));
		inputBean.setPort(inputDetailsJson.getInt("port"));
		inputBean.setJobId(inputDetailsJson.getString("jobId"));
		inputBean.setOrgId(inputDetailsJson.getString("orgId"));
		inputBean.setAppId(inputDetailsJson.getString("appId"));
		inputBean.setRequestId(inputDetailsJson.getString("requestId"));
		inputBean.setUserId(inputDetailsJson.getString("logged_by"));
		inputBean.setUsecaseId(Long.parseLong(inputDetailsJson.getString("useCaseID")));
		
		
		final JsonNode node = mapper.readValue(inputDetailsJson.getString("requestInput"),JsonNode.class);
		logger.error("node: "+node);
		
		inputBean.setEntity(node.get("entity").textValue());
		
		if (!Strings.isNullOrEmpty(node.get("userInput").textValue()) && 
				!node.get("userInput").textValue().equalsIgnoreCase("null")){
			inputBean.setUserInput(node.get("userInput").textValue());
		}
		
		inputBean.setFileType(node.get("fileType").textValue());
		
		String filePathsStr = node.get("filePaths").asText();
		logger.error("filePathsStr: "+filePathsStr);
		
		if (!Strings.isNullOrEmpty(filePathsStr) && !filePathsStr.equals("null")) {
			
			filePathsStr = filePathsStr.substring(1,filePathsStr.length()-1);
			logger.error("filePathsStr substring within if: "+filePathsStr);
	        String regex = Utility.ALLOWED_EXTENSIONS.stream()
	                .map(ext -> "\\" + ext + ",")  // Include comma in the extension pattern
	                .collect(Collectors.joining("|"));
	            
	        String[] filePaths = filePathsStr.split("(?<=" + regex + ")");
	        
	        // Process each element and update the array
	        for (int i = 0; i < filePaths.length; i++) {
	            filePaths[i] = filePaths[i].trim();
	            // Remove trailing comma if present
	            if (filePaths[i].endsWith(",")) {
	                filePaths[i] = filePaths[i].substring(0, filePaths[i].length() - 1);
	            }
	            if (!filePaths[i].isEmpty()) {
	                logger.error("filepath: " + filePaths[i]);
	            }
	        }
	        logger.error("filePaths: " + Arrays.toString(filePaths));
	        inputBean.setFilePaths(filePaths);
	    }
	     
		String questionTypeConfigStr  = node.get("questionTypeConfigMap").toString();
		logger.error("questionTypeConfigStr: "+questionTypeConfigStr);
		if (!Strings.isNullOrEmpty(questionTypeConfigStr) && questionTypeConfigStr!="null"){
			JSONObject questionTypeJson = new JSONObject(questionTypeConfigStr.substring(1, questionTypeConfigStr.length()-1));
			logger.error("questionTypeJson: "+questionTypeJson);
			Map<String, QuestionTypeConfig> questionTypeConfigMap = mapper.readValue(questionTypeJson.toString(), new TypeReference<Map<String, QuestionTypeConfig>>() {});
			logger.error("questionTypeConfigMap: "+questionTypeConfigMap);
			inputBean.setQuestionTypeConfigMap(questionTypeConfigMap);
		}
		
		final JSONObject textLimitJson = inputDetailsJson.getJSONObject("textLimitMap");
		inputBean.setMetadataBean(metadataUtility.fetchAsyncMetadataFromCache(textLimitJson, inputBean.getOrgId()));
		
		String directivesStr = node.get("directives").toString();
		logger.error("directivesStr:" + directivesStr);
		
		if(!Strings.isNullOrEmpty(directivesStr) && directivesStr!="null"){ 
			JSONObject directivesJson = new JSONObject(directivesStr.substring(1, directivesStr.length()-1));
//			System.out.println("directivesJson: "+directivesJson);
			org.json.simple.JSONObject directivesJsonSimple = (org.json.simple.JSONObject) parser.parse(directivesJson.toString());
//			System.out.println("directivesJsonSimple: "+directivesJsonSimple);
			inputBean.setDirectives(directivesJsonSimple);
		}
		
		int id = node.get("userRegisteredServiceId").asInt();
		inputBean.setUserRegisteredServiceId(id);
		
		String vendorName = "";
		
		// Fetch third-party/home-grown vendor details from Cache
		if(inputDetailsJson.has("vendorCredMap")){
			try{
				JSONObject AIvendorCredNode = inputDetailsJson.getJSONObject("vendorCredMap");
				inputBean.setAIvendorCredNodeMap(mapper.readValue(AIvendorCredNode.toString(), new TypeReference<Map<String, HashMap<String, String>>>(){}));
				Map<String, HashMap<String, String>> vendorMap =inputBean.getAIvendorCredNodeMap();
				if (vendorMap != null && !vendorMap.isEmpty()) {
				    vendorName = vendorMap.keySet().iterator().next();
				    inputBean.setVendorName(vendorName);
				} else {
				    logger.error("Vendor details map is null/empty for apiId={}"+inputBean.getApiID() +"orgId={} "+ inputBean.getOrgId()+ "appid={} "+inputBean.getAppId());
				}
				
			} catch(Exception e) {
				ExceptionLogUtility.logException("Exception occured during vendor details fetching:", e, logger);
			}
		}
		
		
		Map<String, String> parameterMap = new HashMap<>();
		parameterMap.put(GenAIFrameworkConstants.SERVICE_ID, String.valueOf(inputBean.getUserRegisteredServiceId()));
    	parameterMap.put("serviceName", vendorName);
		inputBean.setParameterMap(parameterMap);
		
		logger.error("Inputbean is : "+inputBean.toString());
	}
	
	public static void main(String args[]){
		QuestionAuthoringRequestImpl impl = new QuestionAuthoringRequestImpl();
		logger.error(impl.inputBean.getUserInput().toString());
	}
}

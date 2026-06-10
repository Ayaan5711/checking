package com.tcsion.ml.qsnauthorv2.beans;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.tcsion.ml.filereader.Chunk;

public class InputBean {
	private String entity; 
	private String userInput;
	private String[] filePaths;
	private String fileType; // kb or file
	private Map<String, QuestionTypeConfig> questionTypeConfigMap;
	private JSONObject directives;
	private List<Chunk> retrievedChunks;
	private Map<String, HashMap<String, String>> AIvendorCredNodeMap;
	private MetadataBean metadataBean;
	private String jobId = "";
	private String orgId = "";
	private String requestId = "";
	private String appId = "";
	private String apiID;
	private int port;
	private String userId;
	private int userRegisteredServiceId;
	private long usecaseId = 0L;
	private String vendorName = "";
	private Map<String, String> parameterMap = new HashMap<String, String>();
	
	public Map<String, String> getParameterMap() {
		return parameterMap;
	}
	public void setParameterMap(Map<String, String> parameterMap) {
		this.parameterMap = parameterMap;
	}
	public String getVendorName() {
		return vendorName;
	}
	public void setVendorName(String vendorName) {
		this.vendorName = vendorName;
	}
	public long getUsecaseId() {
		return usecaseId;
	}
	public void setUsecaseId(long usecaseId) {
		this.usecaseId = usecaseId;
	}
	public int getUserRegisteredServiceId() {
		return userRegisteredServiceId;
	}
	public void setUserRegisteredServiceId(int userRegisteredServiceId) {
		this.userRegisteredServiceId = userRegisteredServiceId;
	}
	public String getEntity() {
		return entity;
	}
	public void setEntity(String entity) {
		this.entity = entity;
	}
	public String getUserInput() {
		return userInput;
	}
	public void setUserInput(String userInput) {
		this.userInput = userInput;
	}
	public String[] getFilePaths() {
		return filePaths;
	}
	public void setFilePaths(String[] filePaths) {
		this.filePaths = filePaths;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public Map<String, QuestionTypeConfig> getQuestionTypeConfigMap() {
		return questionTypeConfigMap;
	}
	public void setQuestionTypeConfigMap(
			Map<String, QuestionTypeConfig> questionTypeConfigMap) {
		this.questionTypeConfigMap = questionTypeConfigMap;
	}
	public JSONObject getDirectives() {
		return directives;
	}
	public void setDirectives(JSONObject jsonObject) {
		this.directives = jsonObject;
	}
	public MetadataBean getMetadataBean() {
		return metadataBean;
	}
	public void setMetadataBean(MetadataBean metadataBean) {
		this.metadataBean = metadataBean;
	}

	public Map<String, HashMap<String, String>> getAIvendorCredNodeMap() {
		return AIvendorCredNodeMap;
	}
	public void setAIvendorCredNodeMap(
			Map<String, HashMap<String, String>> aIvendorCredNodeMap) {
		AIvendorCredNodeMap = aIvendorCredNodeMap;
	}
	public String getJobId() {
		return jobId;
	}
	
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	public String getOrgId() {
		return orgId;
	}
	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}
	public String getRequestId() {
		return requestId;
	}
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	public String getApiID() {
		return apiID;
	}
	public void setApiID(String apiID) {
		this.apiID = apiID;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public List<Chunk> getRetrievedChunks() {
		return retrievedChunks;
	}
	public void setRetrievedChunks(List<Chunk> retrievedChunks) {
		this.retrievedChunks = retrievedChunks;
	}
	
	@Override
	public String toString() {
	    return "InputBean [entity=" + entity 
	            + ", userInput=" + userInput
	            + ", filePaths=" + (filePaths != null ? Arrays.toString(filePaths) : null)
	            + ", fileType=" + fileType
	            + ", questionTypeConfigMap=" + questionTypeConfigMap
	            + ", directives=" + directives 
	            + ", retrievedChunks=" + retrievedChunks 
	            + ", jobId=" + jobId 
	            + ", orgId=" + orgId 
	            + ", requestId=" + requestId
	            + ", appId=" + appId 
	            + ", apiID=" + apiID
	            + ", port=" + port 
	            + ", userId=" + userId 
	            +", parameterMap=" + parameterMap + "]" ;
	}
	



}

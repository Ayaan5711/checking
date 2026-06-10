package com.tcs.ion.ml.ws.qsnauthorv2;

import java.util.Arrays;
import java.util.Map;

import org.json.simple.JSONObject;

import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;

@SuppressWarnings("all")
public class QuestionGenerationWSBeanV2 {

	private static final long serialVersionUID = 1L;
	private String entity; 
	private String userInput;
	private String[] filePaths;
	private String fileType; // kb or file
	private Map<String, QuestionTypeConfig> questionTypeConfigMap;
	private JSONObject directives;
	private int userRegisteredServiceId = 0;
	
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

	public void setDirectives(JSONObject directives) {
		this.directives = directives;
	}

	public int getUserRegisteredServiceId() {
		return userRegisteredServiceId;
	}

	public void setUserRegisteredServiceId(int userRegisteredServiceId) {
		this.userRegisteredServiceId = userRegisteredServiceId;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		return "QuestionGenerationWSBeanV2 [entity=" + entity + ", userInput="
				+ userInput + ", filePaths=" + Arrays.toString(filePaths)
				+ ", fileType=" + fileType + ", questionTypeConfigMap="
				+ questionTypeConfigMap + ", directives=" + directives
				+ ", userRegisteredServiceId=" + userRegisteredServiceId + "]";
	}
	
	

}

package com.tcsion.ml.qsnauthorv2.beans;

import org.json.simple.JSONObject;

public class QuestionTypeConfig {
	private int maxQuestions;
	private int maxOptions;
	private boolean enableHint;
	private boolean enableKnowMore;
	private JSONObject responseFormat;
	private boolean isMarkingSchemeEnabled;
	
	public JSONObject getResponseFormat() {
		return responseFormat;
	}
	public void setResponseFormat(JSONObject responseFormat) {
		this.responseFormat = responseFormat;
	}
	public int getMaxQuestions() {
		return maxQuestions;
	}
	public void setMaxQuestions(int maxQuestions) {
		this.maxQuestions = maxQuestions;
	}
	public int getMaxOptions() {
		return maxOptions;
	}
	public void setMaxOptions(int maxOptions) {
		this.maxOptions = maxOptions;
	}
	public boolean isEnableHint() {
		return enableHint;
	}
	public void setEnableHint(boolean enableHint) {
		this.enableHint = enableHint;
	}
	public boolean isEnableKnowMore() {
		return enableKnowMore;
	}
	public void setEnableKnowMore(
			boolean enableKnowMore) {
		this.enableKnowMore = enableKnowMore;
	}
	

	public boolean isMarkingEnabled() {
	    return isMarkingSchemeEnabled;
	}
	
	public void setMarkingEnabled(boolean isMarkingEnabled) {
	    this.isMarkingSchemeEnabled = isMarkingEnabled;
	}

	
	@Override
	public String toString() {
		return "QuestionTypeConfig [maxQuestions=" + maxQuestions
				+ ", maxOptions=" + maxOptions 
				+ ", enableHint=" + enableHint
				+ ", enableKnowMore=" + enableKnowMore 
				+ ", isMarkingEnabled = " + isMarkingSchemeEnabled  + " ] ";
	}

}

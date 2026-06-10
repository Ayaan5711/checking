package com.tcsion.ml.qsnauthorv2.beans;

import java.util.ArrayList;
import java.util.Map;
import org.json.simple.JSONObject;
import com.google.common.base.MoreObjects;

public class OutputBean {
	private JSONObject output;
	private String returnCode;
	private String message;
	private JSONObject thirdPartyDetails = new JSONObject();
	private ArrayList<Map<String, String>> vendorAuditDetails = new ArrayList<Map<String, String>>();

	public ArrayList<Map<String, String>> getVendorAuditDetails() {
		return vendorAuditDetails;
	}
	public void setVendorAuditDetails(
			ArrayList<Map<String, String>> vendorAuditDetails) {
		this.vendorAuditDetails = vendorAuditDetails;
	}

	public JSONObject getOutput() {
		return output;
	}
	public void setOutput(JSONObject output) {
		this.output = output;
	}
	public String getReturnCode() {
		return returnCode;
	}
	public void setReturnCode(String returnCode) {
		this.returnCode = returnCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public JSONObject getThirdPartyDetails() {
		return thirdPartyDetails;
	}
	public void setThirdPartyDetails(JSONObject thirdPartyDetails) {
		this.thirdPartyDetails = thirdPartyDetails;
	}
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this.getClass())
				.add("ReturnCode", this.getReturnCode())
				.add("Message", this.getMessage())
				.add("Output", this.getOutput())
				.add("thirdPartyDetails", this.getThirdPartyDetails())
				.toString();
	}
}

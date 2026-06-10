package com.tcs.genai.prompt.service.interfacehub;

//import java.util.HashMap;
import java.util.List;
//import java.util.Map;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public interface VendorPayloadAdapter {
	    
	
	     //Method to add system persona to a prompt
		JSONArray addSystemPersonaPrompt(String systemPersona,String contentrole) throws JSONException;
		
		//Method to build guardrail prompt from the given list of guardrails
		JSONArray buildGuardRailPrompt(List<String> guardrailList) throws JSONException;
		
		//Method to build user query message from the input user message content
		JSONArray buildUserQueryMessage(Object inputUserMessage,String inputType) throws JSONException;
		
		//Method to build assistant query message from the input assistant message content
		JSONArray buildAssistantQueryMessage(String inputAssistantMessage) throws JSONException;
		
		//Method to append message content arrays to existing prompt message , thereby building the overall prompt
		JSONArray addToExistingPromptMessages(JSONArray messages,JSONArray contentToAppend,String role);
	    
		//Method to build the final prompt by stitching together the various prompt parts
		JSONObject getPayload(JSONObject commonPayload) throws JSONException,NullPointerException;


		
		
}




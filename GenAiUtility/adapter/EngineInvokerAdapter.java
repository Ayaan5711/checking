package com.tcs.genai.engine.adapter;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.simple.JSONObject;

public interface EngineInvokerAdapter { 
    JSONObject invokeAiEngine(JSONObject payload, Map<String, HashMap<String, String>> vendorProps) throws JSONException,NullPointerException;

    
    JSONObject invokeEngine(String sourceDoc, String extractionType,Map<String, HashMap<String, String>> vendorProps) throws JSONException,NullPointerException;
}
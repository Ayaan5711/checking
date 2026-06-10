package com.tcs.genai.engine.utils;

public class EngineConstants {
	
	private EngineConstants() {
		throw new UnsupportedOperationException("Cannot instantiate utility class");
	}
	
	// GPT4o
	public static final String 	OPENAI_API_URL = "OPENAI_API_URL";
	public static final String 	OPENAI_API_KEY = "OPENAI_API_KEY";
	public static final String 	OPENAI_API_VERSION = "OPENAI_API_VERSION";
	
	public static final String 	AZMISTRAL_API_URL = "AZMISTRAL_API_URL";
	public static final String 	AZMISTRAL_API_KEY = "AZMISTRAL_API_KEY";
	public static final String 	AZMISTRAL_API_VERSION = "AZMISTRAL_API_VERSION";
	
	public static final String 	CONTENT_TYPE = "Content-Type";
	public static final String 	API_KEY = "api-key";
	
	public static final String ENGINE_RETURN_CODE = "engine_return_code";
	public static final String ENGINE_RETURN_MESSAGE = "engine_return_message";
	
	//AWS_TEXTRACT
	public static final String 	AWS_TEXTRACT = "AWS_TEXTRACT";
	public static final String 	AWS_TEXTRACT_DETECT_DOCUMENT = "awsTextractDetectDocument";
	public static final String 	AWS_TEXTRACT_ANALYZE_DOCUMENT = "awsTextractAnalyzeDocument";
	public static final String 	AWS_TEXTRACT_DETECT_DOCUMENT_RESULT = "detectDocTextResult";
	public static final String 	AWS_TEXTRACT_ANALYZE_DOCUMENT_RESULT = "analyzeDocumentResult";
	
	
	public static final String ACCESS_KEY = "ACCESS_KEY";
	public static final String SECRET_KEY = "ACCESS_SECRET";
	public static final String REGION = "ACCESS_REGION";
	
	public static final String GOOGLE_VISION_OCR = "GOOGLE_VISION_OCR";
	public static final String GOOGLE_VISION_URL = "GOOGLE_VISION_URL";
	public static final String GOOGLE_VISION_API_KEY = "GOOGLE_VISION_API_KEY";
	
	// GEMINI / Vertex AI
	public static final String GEMINI_PROJECT_ID   = "GEMINI_PROJECT_ID";
	public static final String GEMINI_LOCATION     = "GEMINI_LOCATION";
	public static final String GEMINI_MODEL_ID     = "GEMINI_MODEL_ID";
	public static final String GEMINI_CREDENTIALS  = "GEMINI_CREDENTIALS";
	public static final String GEMINI_TEMPERATURE  = "GEMINI_TEMPERATURE";
	public static final String GEMINI_MAX_TOKENS   = "GEMINI_MAX_TOKENS";
	public static final String GEMINI_TOP_P        = "GEMINI_TOP_P";

	// GEMINI finish reasons (output-side blocking)
	public static final String GEMINI_FINISH_REASON_STOP               = "STOP";
	public static final String GEMINI_FINISH_REASON_MAX_TOKENS         = "MAX_TOKENS";
	public static final String GEMINI_FINISH_REASON_SAFETY             = "SAFETY";
	public static final String GEMINI_FINISH_REASON_RECITATION         = "RECITATION";
	// Model Armor finish reasons
	public static final String GEMINI_FINISH_REASON_BLOCKLIST          = "BLOCKLIST";
	public static final String GEMINI_FINISH_REASON_PROHIBITED_CONTENT = "PROHIBITED_CONTENT";
	public static final String GEMINI_FINISH_REASON_SPII               = "SPII";

	// Model Armor prompt-level block reasons (promptFeedback.blockReason)
	public static final String GEMINI_BLOCK_REASON_MODEL_ARMOR         = "MODEL_ARMOR";
	public static final String GEMINI_BLOCK_REASON_SAFETY              = "SAFETY";
	public static final String GEMINI_BLOCK_REASON_OTHER               = "OTHER";

	// Engine return codes for Gemini / Model Armor blocking
	public static final String ENGINE_400             = "ENGINE_400";     // HTTP 400 bad request (any engine)
	public static final String ENGINE_MA_BLOCKLIST    = "ENGINE_MA_BL";   // response blocked: blocklist template
	public static final String ENGINE_MA_PROHIBITED   = "ENGINE_MA_PC";   // response blocked: prohibited content template
	public static final String ENGINE_MA_SPII         = "ENGINE_MA_SPII"; // response blocked: SPII template
	public static final String ENGINE_MA_PROMPT_BLOCK = "ENGINE_MA_PR";   // prompt blocked by Model Armor (promptFeedback)
	public static final String ENGINE_SAFETY_BLOCK    = "ENGINE_SAFETY";  // response blocked: Gemini safety filter
	public static final String ENGINE_RECITATION_BLOCK = "ENGINE_RECIT";  // response blocked: recitation policy

	public static final String VENDOR_RESPONSE_STATUS = "vendorResponseStatus";

}

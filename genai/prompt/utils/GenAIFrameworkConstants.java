package com.tcs.genai.prompt.utils;

public class GenAIFrameworkConstants {
	
	private GenAIFrameworkConstants(){
		// Prevent instantiation
		// Prevent instantiation with a private constructor 
		//– Ensures it remains a static utility class.
		throw new UnsupportedOperationException("Cannot instantiate utility class");
	}
	public static final String 	AZURE_OPENAI_GPT40 = "AzureOpenAIGPT4o";
	public static final String 	AZURE_OPENAI_GPT35 = "AzureOpenAIGPT35";
	public static final String SYSTEM = "system" ;
	public static final String PROMPTIDLIST = "promptidlist";
	public static final String CONTENT_ROLE = "role";
	public static final String CONTENT = "content";
	public static final String TEXT_CONTENT_TYPE  = "text";
	public static final String GPT4O_DEFAULT_CONTENT_ROLE  = "user";
	public static final String GPT4O_DEFAULT_CONTENT_TYPE  = "text";
	public static final String MESSAGE  = "message";
	
	
	
	public static final String PROMPT_NAME = "prompt_name";
	public static final String PROMPT_TEMPLATE = "prompt_template";
	public static final String GUARD_RAIL = "guard_rail";
	public static final String ROLES = "role";
	public static final String CONTENTS = "content";
	public static final String TYPE = "type";
	public static final String ASSISTANT= "assistant";
	public static final String FEWSHOT_ID= "fewshots_id";
	public static final String GUARDRAIL_INSTRUCTION = "guardrail_instruction";
	public static final String FEWSHOTS_EXAMPLE = "fewshots_example";
	public static final String FEWSHOT_USER_MESSAGE = "user_message";
	public static final String FEWSHOT_ASSISTANCE_MESSAGE = "assistant_message";
	public static final String STATUS_CODE = "statuscode";
	public static final String STATUS_MESSAGE = "statusmessage";
	public static final String VENDOR_RESPONSE  = "extractedText";
	public static final String AZURE_RESPONSE  = "azureResponse";
	
	public static final String GEN_200 = "GEN_200";
	public static final String GEN_201 = "GEN_201";
	public static final String GEN_202 = "GEN_202";
	public static final String GEN_203 = "GEN_203";
	public static final String GEN_400 = "GEN_400";
	public static final String GEN_401 = "GEN_401";
	public static final String GEN_402 = "GEN_402";
	public static final String GEN_403 = "GEN_403";
	public static final String GEN_404 = "GEN_404";
	public static final String GEN_405 = "GEN_405";
	public static final String GEN_406 = "GEN_406";
	public static final String GEN_407 = "GEN_407";
	public static final String GEN_408 = "GEN_408";
	public static final String GEN_409 = "GEN_409";
	public static final String GEN_410 = "GEN_410";
	public static final String GEN_411 = "GEN_411";
	public static final String GEN_412 = "GEN_412";
	
	public static final String GEN_200_MSG = "Successfully generated the payload!";
	public static final String GEN_201_MSG = "Retrieved the prompt ID list successfully.";
	public static final String GEN_202_MSG = "Common payload generated successfully.";
	public static final String GEN_203_MSG = "Successfully received response from vendor.";
	public static final String GEN_400_MSG = "Please provide the third-party AI invocation parameter.";
	public static final String GEN_401_MSG = "The received prompidlist was empty.";
	public static final String GEN_402_MSG = "Failed to generate the common payload.";
	public static final String GEN_403_MSG = "Unable to generate the payload.";
	public static final String GEN_404_MSG = "The provided third party vendor invocation implementation is not present right now.";
	public static final String GEN_405_MSG = "Failed to retrieve prompt details from the database.";
	public static final String GEN_406_MSG = "Unable to fetch Guardrail details.";
	public static final String GEN_407_MSG = "Failed to retrieve Fewshot details.";
	public static final String GEN_408_MSG = "No prompt ID list available for the given key.";
	public static final String GEN_409_MSG = "Database SQL error occurred while fetching prompt IDs.";
	public static final String GEN_410_MSG = "Exception occured while invoking third-party AI service.";
	public static final String GEN_411_MSG = "Exception Occured during invoking OpanAi Vendor.";
	public static final String GEN_412_MSG = "Please provide the third-party vendor invocation details.";
	
	
	public static final String LOGGER_ERROR = "[GenAI Error]";
	public static final String LOGGER_EXCEPTION = "[GenAI Exception]";
	public static final String LOGGER_INFO = "[GenAI Info]";
	public static final String LOGGER_DEBUG = "[GenAI Debug]";
	public static final String LOGGER_CACHE = "[GenAI Cache]";

	
	
	public static final String OPENAI_GPT4O = "OPENAI_GPT4o";
	public static final String OPENAI_GPT = "OPENAI_GPT";
	public static final String MISTRAL = "MISTRAL";
	public static final String OPENAI_GPT4_1 = "OPENAI_GPT4.1";
	public static final String OPENAI_GPT5_4 = "OPENAI_GPT5.4";
	public static final String TCS_ION_AI_ML = "TCS_iON_AI_ML";
	public static final String TCS_iON_AI_ML = "TCS_iON_AI_ML";
	
	public static final String AZURE_MISTRAL_SMALL_2503 = "MISTRAL_2503_SMALL";
	public static final String QUESTION_GENERATION_OPENSOURCE = "Question_Generation_Opensource";
	public static final String GOOGLE_VISION_OCR = "GOOGLE_VISION_OCR";
	public static final String GEMINI_3_5_FLASH = "GEMINI_3_5_FLASH";
	
	public static final String PARAMETER_NAME = "parameter_name";
	public static final String PARAMETER_VALUE = "parameter_value";
	public static final String PROMPTPARAMS = "promptparams";
	
	public static final String OPENAI_MAX_TOKENS = "OPENAI_MAX_TOKENS";
	public static final String OPENAI_TEMPERATURE = "OPENAI_TEMPERATURE";
	public static final String OPENAI_TOP_P = "OPENAI_TOP_P";
	public static final String OPENAI_MODEL_TYPE = "OPENAI_MODEL_TYPE";

	public static final String GENAI_CACHE_REFRESH_INTERVAL_PROPERTY = "GENAI_CACHE_REFRESH_INTERVAL_PROPERTY";
	public static final String EXCEPTION_MESSAGE = "Exception Occurred : {}";
    public static final String PREPARED_STATEMENT_EXCEPTION_MESSAGE = "Exception occured in closing preparedStatement {} ";
    public static final String RESULT_EXCEPTION_MESSAGE = "Exception occured in closing resultSet {} ";
    public static final String GLOBAL_CONNECTION_EXCEPTION_MESSAGE = "Exception occured in closing Global Connection {} ";
    public static final String CUSTOMER_CONNECTION_EXCEPTION_MESSAGE = "Exception occured in closing Customer Connection {} ";
	public static final String SERVICE_ID = "serviceId" ; 
	public static final String SERVICE_NAME = "serviceName" ; 
	public static final long DEFAULT_ORGID = 1;
	public static final String ENGINE_500 = "ENGINE_500";
	public static final String ENGINE_503 = "ENGINE_503";
	public static final String ENGINE_429 = "ENGINE_429";
	public static final String ENGINE_999 = "ENGINE_999";

	public static final String VENDOR_RESPONSE_STATUS = "vendorResponseStatus";
	public static final String VENDOR_ACTUAL_MESSAGE = "vendorActualMessage";
	
	public static final String INPUTTOKEN  = "inputTokens";
	public static final String OUTPUTTOKEN = "outputTokens";
	public static final String REASONINGTOKEN = "reasoningTokens";
	public static final String TOKENS = "tokens";
	
	public static final String USECASEID = "usecaseId";
	public static final String APPID = "appId";
	public static final String ORGID = "orgId";
	public static final String APIDID = "apidId";
	public static final String APIID = "apiId";
	public static final String SERVICEID = "serviceId";
	public static final String ENGINEID = "engineId";
	public static final String USECASEBUCKETID = "usecaseBucketId";
	
	public static final String INPUTKEY1 = "inputKey1";
	public static final String INPUTKEY2 = "inputKey2";
	public static final String INPUTKEY3 = "inputKey3";
	public static final String INPUTKEY4 = "inputKey4";
	
	public static final String KEY1 = "key1";
	public static final String KEY2 = "key2";
	public static final String KEY3 = "key3";
	public static final String KEY4 = "key4";
	
	public static final String LOGGEDBY = "loggedBy";
	
	public static final String UTF_8 = "utf-8";
	
	public static final String ERROR_CODE = "errorCode";
	public static final String ERROR_MESSAGE = "errorMessage";
	public static final String ERROR_CODE_VALUE = "999";
	public static final String ERROR_MESSAGE_VALUE = "Failed to parse AZURE GPT error response";
	
	public static final String GEMINI_ROLE_USER   = "user";
	public static final String GEMINI_ROLE_MODEL  = "model";
	public static final String GEMINI_ROLE_SYSTEM = "system";

	public static final String GEMINI_CONTENTS_KEY            = "contents";
	public static final String GEMINI_SYSTEM_INSTRUCTION_KEY  = "systemInstruction";
	public static final String GEMINI_PARTS_KEY               = "parts";
	public static final String GEMINI_TEXT_KEY                = "text";
	


	
//	public static enum VENDOR_CONSTANTS {
//		OPENAI_MAX_TOKENS,
//		OPENAI_TEMPERATURE,
//		OPENAI_TOP_P,
//		OPENAI_MODEL_TYPE
//	};

}

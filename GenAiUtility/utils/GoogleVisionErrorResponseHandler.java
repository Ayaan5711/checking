package com.tcs.genai.engine.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

public class GoogleVisionErrorResponseHandler {

	private static final Log logger = LogFactory.getLog(OpenAIErrorResponseHandler.class);
	static final String ENGINE_RETURN_MESSAGE = EngineConstants.ENGINE_RETURN_MESSAGE;
	static final String ENGINE_RETURN_CODE = EngineConstants.ENGINE_RETURN_CODE;
	public Map<String,String> errorMessageForStatus(int statusCode) {
		logger.error("Handling non-200 response: " + statusCode);
		Map<String,String> errorMap = new HashMap<>();
		switch (statusCode) {
		case HttpStatus.SC_OK: // 200
			errorMap.put(ENGINE_RETURN_MESSAGE,"The engine successfully returned the response ");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_001");
			break;
		case HttpStatus.SC_BAD_REQUEST: // 400
			errorMap.put(ENGINE_RETURN_MESSAGE,"Bad Request. Please retry with correct input format");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_400");
			break;

		case HttpStatus.SC_UNAUTHORIZED: // 401
			errorMap.put(ENGINE_RETURN_MESSAGE,"Unauthorized. Your API key is wrong or missing");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_401");
			break;

		case HttpStatus.SC_FORBIDDEN: // 403
			errorMap.put(ENGINE_RETURN_MESSAGE,"Forbidden. You may not have access to this resource");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_403");
			break;

		case HttpStatus.SC_NOT_FOUND: // 404
			errorMap.put(ENGINE_RETURN_MESSAGE,"Not Found, endpoint doesn't exist");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_404");
			break;
			
		case HttpStatus.SC_CONFLICT: // 409
			errorMap.put(ENGINE_RETURN_MESSAGE,"Conflict, The operation could not be completed due to a conflict with the current state of the resource.");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_409");
			break;

		case 429: // Too Many Requests
			errorMap.put(ENGINE_RETURN_MESSAGE,"Too Many Requests. You hit the rate limit. Retry after some time.");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_429");
			break;
			
		case 499: // Too Many Requests
			errorMap.put(ENGINE_RETURN_MESSAGE,"Client Closed Request, The operation was cancelled by the client.");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_499");
			break;

		case HttpStatus.SC_INTERNAL_SERVER_ERROR:
			errorMap.put(ENGINE_RETURN_MESSAGE,"Internal Server Error. OpenAI engine is down. Retry after delay.");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_500");
			break;
			
		case HttpStatus.SC_NOT_IMPLEMENTED:
			errorMap.put(ENGINE_RETURN_MESSAGE,"Not Implemented, The operation is not implemented or supported by the service.");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_501");
			break;

		case HttpStatus.SC_BAD_GATEWAY:
			errorMap.put(ENGINE_RETURN_MESSAGE,"Bad Gateway. OpenAI backend issue. Retry after delay.");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_502");
			break;

		case HttpStatus.SC_SERVICE_UNAVAILABLE:
			errorMap.put(ENGINE_RETURN_MESSAGE,"Service Unavailable. API is overloaded or under maintenance. Retry after delay.");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_503");
			break;

		case HttpStatus.SC_GATEWAY_TIMEOUT:
			errorMap.put(ENGINE_RETURN_MESSAGE,"Gateway Timeout. OpenAI engine backend timed out. Retry after delay.");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_504");
			break;

		default:
			errorMap.put(ENGINE_RETURN_MESSAGE,"AI engine service error during processing.");
			errorMap.put(ENGINE_RETURN_CODE,"ENGINE_999");
			break;
		}
		return errorMap;
	}
}

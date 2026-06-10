package com.tcs.genai.engine.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tcs.genai.engine.utils.EngineConstants;
import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapterFactory;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

public class EngineInvokerFactory {
	private static final Log logger = LogFactory
			.getLog(VendorPayloadAdapterFactory.class);
	
	private EngineInvokerFactory() {
		logger.info("[Advance AI] Inside EngineInvokerFactory");
	}
	
	public static EngineInvokerAdapter getAdapter(String vendorName) { 
		switch (vendorName) {
		case GenAIFrameworkConstants.OPENAI_GPT4O:
			return new AzureAIEngineInvoker();
		case EngineConstants.AWS_TEXTRACT:
			return new AWSTextractInvoker();
		case GenAIFrameworkConstants.OPENAI_GPT4_1:
			return new AzureAIEngineInvoker();
		case GenAIFrameworkConstants.AZURE_MISTRAL_SMALL_2503:
			return new AzureAIEngineInvoker();
		case GenAIFrameworkConstants.GOOGLE_VISION_OCR:
			return new GoogleVisionInvoker();
		case GenAIFrameworkConstants.GEMINI_VERTEX_AI:
			return new GeminiEngineInvoker();
		case GenAIFrameworkConstants.QUESTION_GENERATION_OPENSOURCE:
			return new AzureAIEngineInvoker();
		case GenAIFrameworkConstants.OPENAI_GPT5_4:
			return new AzureAIEngineInvoker();
		case GenAIFrameworkConstants.TCS_ION_AI_ML:
			return new AzureAIEngineInvoker();
		default:
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " Vendor specific implementation not present for this vendor: "
					+ vendorName);
			return null;
		}

    } 

}

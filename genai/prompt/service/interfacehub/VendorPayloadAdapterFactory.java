package com.tcs.genai.prompt.service.interfacehub;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


//import com.tcs.genai.prompt.service.Impl.AzureOpenAIGPT35PayloadAdapter;
import com.tcs.genai.prompt.service.Impl.AzureOpenAIServicePayloadAdapter;
import com.tcs.genai.prompt.service.Impl.Gemini35PayloadAdapter;
import com.tcs.genai.prompt.service.Impl.MistralPayloadAdapter;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

/**
 * Factory method to return a specific VendorPayloadAdapter based on the provided vendor name.
 * 
 * The purpose of this method is to provide a way to obtain the correct adapter for the 
 * given vendor name. The adapter is responsible for handling the payload processing logic
 * specific to each vendor.
 * @author 1949882
 */


public class VendorPayloadAdapterFactory {

	private static final Log logger = LogFactory
			.getLog(VendorPayloadAdapterFactory.class);

	public VendorPayloadAdapter getAdapter(String vendorName) {

		switch (vendorName) {
		case GenAIFrameworkConstants.OPENAI_GPT4O:
			return new AzureOpenAIServicePayloadAdapter();

		case GenAIFrameworkConstants.OPENAI_GPT:
			return new AzureOpenAIServicePayloadAdapter();
			
		case GenAIFrameworkConstants.MISTRAL:
			return new MistralPayloadAdapter();
		
		case GenAIFrameworkConstants.OPENAI_GPT4_1:
			return new AzureOpenAIServicePayloadAdapter();
		
		case GenAIFrameworkConstants.AZURE_MISTRAL_SMALL_2503:
			return new AzureOpenAIServicePayloadAdapter();
		
		case GenAIFrameworkConstants.QUESTION_GENERATION_OPENSOURCE:
			return new AzureOpenAIServicePayloadAdapter();
			
		case GenAIFrameworkConstants.OPENAI_GPT5_4:
			return new AzureOpenAIServicePayloadAdapter();
			
		case GenAIFrameworkConstants.TCS_ION_AI_ML:
			return new AzureOpenAIServicePayloadAdapter();

		case GenAIFrameworkConstants.GEMINI_3_5_FLASH:
			return new Gemini35PayloadAdapter();
			
		default:
			logger.error(GenAIFrameworkConstants.LOGGER_ERROR + " Vendor specific implementation not present for this vendor: "
					+ vendorName);
			return null;
		}

	}

}

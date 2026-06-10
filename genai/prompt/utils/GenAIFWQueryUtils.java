package com.tcs.genai.prompt.utils;

public class GenAIFWQueryUtils {
	
	private GenAIFWQueryUtils(){
		// Prevent instantiation
		// Prevent instantiation with a private constructor 
		//– Ensures it remains a static utility class.
		throw new UnsupportedOperationException("Cannot instantiate utility class");
	}
	
	public static final String PROMPT_ID_LIST_QUERY = "select prompidlist,orgid from global.tcs_ion_gen_ai_usecase_prompt_sequence_mapping where apiid in (?, 1) and usecaseid = ? and orgid in (?, 1) and vendorserviceid = ? and recognizer_entity = ? and rowstate >=0";
	
	public static final String PROMPT_DETAILS_QUERY = "select prompt_name, prompt_template, guardrail_id, fewshots_id from global.tcs_ion_gen_ai_prompt_master where id = (?) and rowstate >=0";

	public static final String GUARDRAIL_DETAILS_QUERY = "select guardrail_name, guardrail_instruction from global.tcs_ion_gen_ai_guardrail_master where id = (?) and rowstate >=0";
	
	public static final String FEWSHOT_DETAILS_QUERY = "select fewshot_name, user_message, assistant_message from global.tcs_ion_gen_ai_fewshot_master_table where id in (?) and rowstate >=0";
	
	public static final String PROMPT_PARAMS_QUERY = "select parameter_name, parameter_value from tcs_ion_gen_ai_usecase_prompt_param where apiID in (?) and UsecaseId in (?) and rowstate >=0";
	
	public static final String GENAI_CACHE_INTERVAL = "select propvalue from global.gblglobalproperties where propname = 'GENAI_CACHE_REFRESH_INTERVAL_PROPERTY' ";

	//master prompt constants//
	public static final String MASTER_PROMPT_NAME = "prompt_name";

	public static final String MASTER_PROMPT_TEMPLATE = "prompt_template";

	public static final String MASTER_PROMPT_ID = "prompt_id";

	public static final String MASTER_PROMPT_GUARDRAIL = "guardrail_id";

	public static final String MASTER_PROMPT_FEWSHOT = "fewshots_id";
	
	public static final String CACHE_MASTER_PROMPT_GUARDRAIL_ID = "cache_guardrail_id";

	public static final String CACHE_MASTER_PROMPT_FEWSHOT_ID = "cache_fewshots_id";

	public static final String MASTER_RESPONSE_TEMPLATE = "response_format_id";
	
	public static final String MASTER_PROMPT_PARAMS = "promptparams";
	public static final String GUARD_RAILS_NAME= "guardrail_name";
	public static final String GUARD_RAILS_INSTRUCTION = "guardrail_instruction";
}

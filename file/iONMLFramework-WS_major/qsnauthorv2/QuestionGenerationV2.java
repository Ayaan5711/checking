package com.tcs.ion.ml.ws.qsnauthorv2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcs.ion.ml.ws.bean.WebServiceParams;
import com.tcs.ion.ml.ws.service.ExecutorServiceSingleTon;
import com.tcsion.ml.Utils.ExceptionLogUtility;
import com.tcsion.ml.Utils.JobPriority;
import com.tcsion.ml.Utils.MlFrameworkConstants;
import com.tcsion.ml.metadatahandler.service.DbBasedLimitGuavaCacheLoaderService;
import com.tcsion.ml.metadatahandler.util.CustomJob;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;


@SuppressWarnings("all")
public class QuestionGenerationV2 {
	private final Log logger = LogFactory.getLog(QuestionGenerationV2.class);
	private ExecutorServiceSingleTon executorServiceSingleTon;
	final DbBasedLimitGuavaCacheLoaderService cacheLoader = DbBasedLimitGuavaCacheLoaderService.getInstance();

	public QuestionGenerationV2(final ExecutorServiceSingleTon executorServiceSingleTon) {
		this.executorServiceSingleTon = executorServiceSingleTon;
	}

	@SuppressWarnings("unchecked")
	public JSONObject generateQuestions(InputBean inputBean, WebServiceParams wsParams) throws InterruptedException, IOException {
		ArrayList<JSONObject> jsonarray = new ArrayList<JSONObject>();
		JSONObject jobj = new JSONObject();
		final ExecutorService executor = this.executorServiceSingleTon.getExecutorServiceInstance();

		if (inputBean.getAIvendorCredNodeMap() == null || inputBean.getAIvendorCredNodeMap().isEmpty() ||
				inputBean.getAIvendorCredNodeMap().containsKey(MlFrameworkConstants.TCS_iON_AI_ML) ||
				inputBean.getAIvendorCredNodeMap().containsKey(MlFrameworkConstants.QUESTION_GENERATION_OPENSOURCE)) {

			Future f = null;
			f = executor.submit(new CustomJob(new QuestionGenerationThreadV2(jsonarray, inputBean, wsParams),
					JobPriority.jobPriorityMap.get(JobPriority.IONML_DEFAULT_SYNC_JOB), "IONML_DEFAULT_SYNC_JOB"));
			try {
				if (f != null) f.get();
			} catch (final Exception e) {
				ExceptionLogUtility.logException("Exception Occured {}", e, logger);
			}

		} else if (inputBean.getAIvendorCredNodeMap().containsKey(GenAIFrameworkConstants.GEMINI_3_5_FLASH)) {

			final QuestionGenerationGemini gemini = new QuestionGenerationGemini();
			jsonarray = gemini.questionGenerationGeminiCall(inputBean, wsParams);

		} else {

			final QuestionGenerationChatGPT gpt = new QuestionGenerationChatGPT();
			jsonarray = gpt.questionGenerationGPTCall(inputBean, wsParams);
		}

		jobj.put("Result", jsonarray);
		logger.error("The resultArray is "+ jsonarray );
		return jobj;
	}
}

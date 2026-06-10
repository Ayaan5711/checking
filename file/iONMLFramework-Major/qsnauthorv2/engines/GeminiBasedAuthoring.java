package com.tcsion.ml.qsnauthorv2.engines;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.base.Strings;
import com.tcs.genai.engine.service.EngineInvocationService;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.filereader.Chunk;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.OutputBean;
import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;
import com.tcsion.ml.qsnauthorv2.prompts.GeminiPromptBuilder;
import com.tcsion.ml.qsnauthorv2.prompts.GPTResponseFormatBuilder;
import com.tcsion.ml.qsnauthorv2.util.QsnAuthConstants;
import com.tcsion.ml.qsnauthorv2.util.Utility;

public class GeminiBasedAuthoring {
	private static final Log logger = LogFactory.getLog(GeminiBasedAuthoring.class);
	private GeminiPromptBuilder promptBuilder = new GeminiPromptBuilder();
	private GPTResponseFormatBuilder responseFormatter = new GPTResponseFormatBuilder();
	private OutputBean outputBean = new OutputBean();
	private Utility utility = new Utility();

	public OutputBean authoringByGemini(InputBean inputBean) throws InterruptedException, IOException {

		Map<String, List<JSONArray>> questionListsByType = new ConcurrentHashMap<>();
		ExecutorService executor = Executors.newFixedThreadPool(inputBean.getQuestionTypeConfigMap().size());
		List<Future<Void>> futures = new ArrayList<>();

		Set<String> successTypes = ConcurrentHashMap.newKeySet();
		Set<String> failureTypes = ConcurrentHashMap.newKeySet();
		Map<String, Integer> errorCodeCounter = new ConcurrentHashMap<>();

		for (String questionType : inputBean.getQuestionTypeConfigMap().keySet()) {
			questionListsByType.put(questionType, new ArrayList<>());
		}

		for (Map.Entry<String, QuestionTypeConfig> entry : inputBean.getQuestionTypeConfigMap().entrySet()) {
			String questionType = entry.getKey();
			QuestionTypeConfig config = entry.getValue();

			Future<Void> future = executor.submit(() -> {
				try {
					processQuestionType(questionType, config, inputBean,
							questionListsByType, successTypes, failureTypes, errorCodeCounter);
				} catch (Exception e) {
					logger.error("Exception in processing question type: " + questionType, e);
					throw new RuntimeException(e);
				}
				return null;
			});
			futures.add(future);
		}

		boolean allTasksCompleted = true;
		Exception firstException = null;

		for (Future<Void> future : futures) {
			try {
				future.get();
			} catch (ExecutionException e) {
				allTasksCompleted = false;
				if (firstException == null) firstException = (Exception) e.getCause();
				logger.error("Task execution failed", e);
			} catch (InterruptedException e) {
				allTasksCompleted = false;
				if (firstException == null) firstException = e;
				logger.error("Task interrupted", e);
				Thread.currentThread().interrupt();
			}
		}

		executor.shutdown();

		if (!allTasksCompleted && firstException != null) {
			if (firstException instanceof RuntimeException) {
				RuntimeException rte = (RuntimeException) firstException;
				if (rte.getCause() instanceof InterruptedException) throw (InterruptedException) rte.getCause();
				if (rte.getCause() instanceof IOException) throw (IOException) rte.getCause();
			}
		}

		if (successTypes.size() == inputBean.getQuestionTypeConfigMap().size()) {
			outputBean.setReturnCode(QsnAuthConstants.QSNGEN_001);
			outputBean.setMessage(QsnAuthConstants.COMPLETED_SUCCESSFULLY);
		} else if (successTypes.size() > 0) {
			outputBean.setReturnCode(QsnAuthConstants.QSNGEN_003);
			outputBean.setMessage(QsnAuthConstants.COMPLETED_PARTIALLY);
		} else {
			String mostFrequent = errorCodeCounter.entrySet().stream()
					.max(Map.Entry.comparingByValue())
					.map(Map.Entry::getKey)
					.orElse("UNKNOWN");
			switch (mostFrequent) {
				case "429":
					outputBean.setReturnCode(QsnAuthConstants.QSNGEN_429);
					outputBean.setMessage(QsnAuthConstants.TOO_MANY_REQUESTS);
					break;
				case "400":
					outputBean.setReturnCode(QsnAuthConstants.QSNGEN_002);
					outputBean.setMessage(QsnAuthConstants.HARMFUL_OR_INAPPROPRIATE_CONTENT);
					break;
				case "503":
					outputBean.setReturnCode(QsnAuthConstants.QSNGEN_503);
					outputBean.setMessage(QsnAuthConstants.SERVICE_UNAVAILABLE);
					break;
				default:
					outputBean.setReturnCode(QsnAuthConstants.QSNGEN_987);
					outputBean.setMessage(QsnAuthConstants.ISSUE_IN_THIRD_PARTY_INTEGRATION);
					break;
			}
		}

		outputBean.setOutput(utility.consolidateQuestionsForMultipleTypes(
				questionListsByType, inputBean.getQuestionTypeConfigMap()));

		if (Strings.isNullOrEmpty(outputBean.getReturnCode())) {
			outputBean.setReturnCode(QsnAuthConstants.QSNGEN_001);
			outputBean.setMessage(QsnAuthConstants.COMPLETED_SUCCESSFULLY);
		}

		return outputBean;
	}

	
	// Per-question-type processing
	@SuppressWarnings("unchecked")
	private void processQuestionType(String questionType,
			QuestionTypeConfig config,
			InputBean inputBean,
			Map<String, List<JSONArray>> questionListsByType,
			Set<String> successTypes,
			Set<String> failureTypes,
			Map<String, Integer> errorCodeCounter)
			throws InterruptedException, IOException, JSONException {

		List<JSONArray> consolidatedQuestions = questionListsByType.get(questionType);
		int totalQuestionsGenerated = 0;

		JSONObject responseFormat = responseFormatter.buildResponseFormat(questionType,
				inputBean.getQuestionTypeConfigMap().get(questionType));
		logger.error("responseFormat for " + questionType + " : " + responseFormat);
		inputBean.getQuestionTypeConfigMap().get(questionType).setResponseFormat(responseFormat);

		List<Chunk> chunks = inputBean.getRetrievedChunks();
		logger.error("Final Chunk is : " + chunks);
		if (chunks == null || chunks.isEmpty()) {
			chunks = Collections.singletonList(null);
		} else {
			Collections.shuffle(chunks);
		}

		// Build parameterMap once — serviceName drives EngineInvokerFactory to GeminiEngineInvoker
		Map<String, String> parameterMap = new HashMap<>(inputBean.getParameterMap());
		parameterMap.put(GenAIFrameworkConstants.SERVICE_NAME,
				inputBean.getAIvendorCredNodeMap().keySet().iterator().next());

		int chunkIndex = 0;
		for (Chunk chunk : chunks) {
			if (totalQuestionsGenerated >= config.getMaxQuestions()) {
				logger.error("Reached the maximum number of questions to generate for type: " + questionType
						+ ". maxQuestions:" + config.getMaxQuestions()
						+ ", Number of questions generated:" + totalQuestionsGenerated);
				break;
			}
			logger.error("Generating " + questionType + " question for Chunk num : " + chunkIndex);

			JSONObject geminiPayload = buildPromptMessages(questionType, chunk, inputBean);
			logger.error("Final prompt for " + questionType + " : " + geminiPayload);

			// EngineInvocationService routes to GeminiEngineInvoker via EngineInvokerFactory
			EngineInvocationService engineService = new EngineInvocationService(
					inputBean.getAIvendorCredNodeMap());
			JSONObject geminiResult = engineService.invokeAdvanceAiEngine(geminiPayload, parameterMap);

			totalQuestionsGenerated = processGeminiResponse(geminiResult, questionType,
					consolidatedQuestions, totalQuestionsGenerated, config,
					successTypes, failureTypes, errorCodeCounter);
			chunkIndex++;
		}
	}

	private JSONObject buildPromptMessages(String questionType, Chunk chunk, InputBean inputBean) throws IOException {
		switch (questionType) {
			case "mcq":              return promptBuilder.getMCQPrompt(inputBean, chunk);
			case "faq":              return promptBuilder.getShortAnswerPrompt(inputBean, chunk);
			case "truefalse":        return promptBuilder.getTrueFalsePrompt(inputBean, chunk);
			case "msq":              return promptBuilder.getMSQPrompt(inputBean, chunk);
			case "fillblanks":       return promptBuilder.getFillInTheBlanksPrompt(inputBean, chunk);
			case "comprehensionmcq": return promptBuilder.getComprehensionPrompt(inputBean, chunk);
			default:                 return new JSONObject();
		}
	}

	
	// Process EngineInvocationService result
	// EngineInvocationService returns STATUS_CODE as an engine string 
	// Map to integer buckets to match error-handling 
	@SuppressWarnings("unchecked")
	private int processGeminiResponse(JSONObject geminiResult,
	        String questionType,
	        List<JSONArray> consolidatedQuestions,
	        int totalQuestionsGenerated,
	        QuestionTypeConfig config,
	        Set<String> successTypes,
	        Set<String> failureTypes,
	        Map<String, Integer> errorCodeCounter) {

	    String engineCode = String.valueOf(geminiResult.get(GenAIFrameworkConstants.STATUS_CODE));
	    String vendorResponse = (String) geminiResult.get(GenAIFrameworkConstants.VENDOR_RESPONSE);

	    logger.error("Gemini engine code for " + questionType + ": " + engineCode);

	    try {
	        switch (engineCode) {

	            case "ENGINE_001":
	                if (vendorResponse == null || vendorResponse.trim().toLowerCase().startsWith("sorry")) {
	                    logger.error("Output starts with Sorry for " + questionType + ": " + vendorResponse);
	                    failureTypes.add(questionType);
	                    errorCodeCounter.merge("500", 1, Integer::sum);
	                } else {
	                    JSONArray newQuestions = postProcessOutput(vendorResponse, questionType);

	                    synchronized (consolidatedQuestions) {
	                        consolidatedQuestions.add(newQuestions);
	                    }

	                    if ("comprehensionmcq".equalsIgnoreCase(questionType)) {
	                        if (!newQuestions.isEmpty() && newQuestions.get(0) instanceof JSONObject) {
	                            JSONObject wrapper = (JSONObject) newQuestions.get(0);
	                            Object innerObj = wrapper.get("questions");

	                            if (innerObj instanceof JSONArray) {
	                                JSONArray innerQuestions = (JSONArray) innerObj;
	                                totalQuestionsGenerated += innerQuestions.size();
	                            }
	                        }
	                    } else {
	                        totalQuestionsGenerated += newQuestions.size();
	                    }

	                    synchronized (outputBean) {
	                        JSONObject thirdParty = new JSONObject();
	                        thirdParty.put("geminiResponse", geminiResult.get("geminiResponse"));
	                        thirdParty.put("vendorActualMessage",
	                                geminiResult.get(GenAIFrameworkConstants.VENDOR_ACTUAL_MESSAGE));
	                        outputBean.setThirdPartyDetails(thirdParty);
	                    }

	                    successTypes.add(questionType);

	                    if (totalQuestionsGenerated >= config.getMaxQuestions()) {
	                        logger.error("Stopping iteration for " + questionType +
	                                ". Total Questions Generated: " + totalQuestionsGenerated);
	                    }
	                }
	                break;

	            case "ENGINE_429":
	                logger.error("Too many requests for " + questionType + ".");
	                failureTypes.add(questionType);
	                errorCodeCounter.merge("429", 1, Integer::sum);
	                break;

	            case "ENGINE_503":
	                logger.error("Service unavailable for " + questionType + ". engineCode: " + engineCode);
	                failureTypes.add(questionType);
	                errorCodeCounter.merge("503", 1, Integer::sum);
	                break;

	            case "ENGINE_400":
	            case "ENGINE_MA_BLOCKLIST":
	            case "ENGINE_MA_PROHIBITED":
	            case "ENGINE_MA_SPII":
	            case "ENGINE_MA_PROMPT_BLOCK":
	            case "ENGINE_SAFETY_BLOCK":
	            case "ENGINE_RECITATION_BLOCK":
	                logger.error("Content blocked for " + questionType + ". engineCode: " + engineCode);
	                failureTypes.add(questionType);
	                errorCodeCounter.merge("400", 1, Integer::sum);
	                break;

	            default:
	                logger.error("Unexpected engine code for " + questionType + ": " + engineCode);
	                failureTypes.add(questionType);
	                errorCodeCounter.merge("500", 1, Integer::sum);
	                break;
	        }

	    } catch (Exception e) {
	        logger.error("Error while processing Gemini response for " + questionType, e);
	        failureTypes.add(questionType);
	        errorCodeCounter.merge("500", 1, Integer::sum);
	        throw new RuntimeException(e);
	    }

	    return totalQuestionsGenerated;
	}
	
	// postProcessOutput
	public JSONArray postProcessOutput(String output, String qsnType) {
		JSONArray resultArray = new JSONArray();
		try {
			JSONParser parser = new JSONParser();
			JSONObject root = (JSONObject) parser.parse(output);

			if ("comprehensionmcq".equalsIgnoreCase(qsnType)) {
				JSONObject comprehensionObject = new JSONObject();
				comprehensionObject.put("passage", root.getOrDefault("passage", ""));
				Object questionsObj = root.get("questions");
				if (questionsObj instanceof JSONArray) {
					JSONArray questionsArray = (JSONArray) questionsObj;
					shuffleOptionsIfRequired(questionsArray, qsnType);
					comprehensionObject.put("questions", questionsArray);
				} else {
					comprehensionObject.put("questions", new JSONArray());
				}
				resultArray.add(comprehensionObject);
			} else {
				Object questionsObj = root.get("questions");
				if (questionsObj instanceof JSONArray) {
					JSONArray questionsArray = (JSONArray) questionsObj;
					shuffleOptionsIfRequired(questionsArray, qsnType);
					resultArray = questionsArray;
				}
			}
		} catch (Exception e) {
			logger.error("Error in postProcessOutput for " + qsnType, e);
		}
		return resultArray;
	}

	private void shuffleOptionsIfRequired(JSONArray questionsArray, String qsnType) {
		if (!"comprehensionmcq".equalsIgnoreCase(qsnType) &&
				!"mcq".equalsIgnoreCase(qsnType) &&
				!"msq".equalsIgnoreCase(qsnType)) {
			return;
		}
		for (Object obj : questionsArray) {
			if (obj instanceof JSONObject) {
				JSONObject question = (JSONObject) obj;
				Object optionsObj = question.get("options");
				if (optionsObj instanceof JSONArray) {
					JSONArray optionsArray = (JSONArray) optionsObj;
					List<Object> tempList = new ArrayList<>(optionsArray);
					Collections.shuffle(tempList);
					JSONArray shuffledArray = new JSONArray();
					shuffledArray.addAll(tempList);
					question.put("options", shuffledArray);
				}
			}
		}
	}
}

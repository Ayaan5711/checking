package com.tcsion.ml.qsnauthorv2.engines;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.base.Strings;
import com.tcsion.ml.Utils.ExceptionLogUtility;
import com.tcsion.ml.audit.AuditThirdPartyAPICall;
import com.tcsion.ml.azure.openai.GptIntegration;
import com.tcsion.ml.azure.openai.GptResponseBean;
import com.tcsion.ml.filereader.Chunk;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.OutputBean;
import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;
import com.tcsion.ml.qsnauthorv2.prompts.GPTPromptBuilder;
import com.tcsion.ml.qsnauthorv2.prompts.GPTResponseFormatBuilder;
import com.tcsion.ml.qsnauthorv2.util.QsnAuthConstants;
import com.tcsion.ml.qsnauthorv2.util.Utility;

public class GPTBasedAuthoring {
	private static final Log logger = LogFactory.getLog(GPTBasedAuthoring.class);
	private GPTPromptBuilder promptBuilder = new GPTPromptBuilder();
	private ArrayList<Map<String, String>> vendorAuditDetailsArray = new ArrayList<Map<String, String>>();
	private OutputBean outputBean = new OutputBean();
	private Utility utility = new Utility();
	private GPTResponseFormatBuilder responseFormatter = new GPTResponseFormatBuilder();
	GptIntegration gptIntegration = new GptIntegration();
	
	public OutputBean authoringByGPT(InputBean inputBean) throws InterruptedException, IOException {
	    HashMap<String, String> apiDetailsMap = inputBean.getAIvendorCredNodeMap().values().iterator().next();
	    String serviceName = inputBean.getAIvendorCredNodeMap().keySet().iterator().next();

	    Map<String, List<JSONArray>> questionListsByType = new ConcurrentHashMap<>();
	    ExecutorService executor = Executors.newFixedThreadPool(inputBean.getQuestionTypeConfigMap().size());
	    List<Future<Void>> futures = new ArrayList<>();

	    Set<String> successTypes = ConcurrentHashMap.newKeySet();
	    Set<String> failureTypes = ConcurrentHashMap.newKeySet();
	    Map<String, Integer> errorCodeCounter = new ConcurrentHashMap<>();

	    
	    // Initialize question lists for each type
	    for (String questionType : inputBean.getQuestionTypeConfigMap().keySet()) {
	        questionListsByType.put(questionType, new ArrayList<>());
	    }

	    // Create tasks for each question type
	    for (Map.Entry<String, QuestionTypeConfig> entry : inputBean.getQuestionTypeConfigMap().entrySet()) {
	        String questionType = entry.getKey();
	        QuestionTypeConfig config = entry.getValue();
	        
	        Future<Void> future = executor.submit(() -> {
	            try {
	                processQuestionType(questionType, config, inputBean, apiDetailsMap, 
	                					serviceName, questionListsByType,successTypes,failureTypes,errorCodeCounter);
	            } catch (Exception e) {
	                logger.error("Exception in processing question type: " + questionType, e);
	                throw new RuntimeException(e);
	            }
	            return null;
	        });
	        futures.add(future);
	    }

	    // Wait for all tasks to complete
	    boolean allTasksCompleted = true;
	    Exception firstException = null;
	    
	    for (Future<Void> future : futures) {
	        try {
	            future.get(); // This will throw an exception if the task failed
	        } catch (ExecutionException e) {
	            allTasksCompleted = false;
	            if (firstException == null) {
	                firstException = (Exception) e.getCause();
	            }
	            logger.error("Task execution failed", e);
	        } catch (InterruptedException e) {
	            allTasksCompleted = false;
	            if (firstException == null) {
	                firstException = e;
	            }
	            logger.error("Task interrupted", e);
	            Thread.currentThread().interrupt();
	        }
	    }
	    
	    executor.shutdown();
	    
	    // Handle any exceptions that occurred during parallel processing
	    if (!allTasksCompleted && firstException != null) {
	        if (firstException instanceof RuntimeException) {
	            RuntimeException runtimeException = (RuntimeException) firstException;
	            if (runtimeException.getCause() instanceof InterruptedException) {
	                throw (InterruptedException) runtimeException.getCause();
	            } else if (runtimeException.getCause() instanceof IOException) {
	                throw (IOException) runtimeException.getCause();
	            }
	        }
	        
	        
//	        outputBean.setMessage(QsnAuthConstants.ISSUE_IN_THIRD_PARTY_INTEGRATION);
//	        outputBean.setReturnCode(QsnAuthConstants.QSNGEN_987);
//	        return outputBean;
	    }
	    
	    
	    
	    if (successTypes.size() == inputBean.getQuestionTypeConfigMap().size()) {
	        // All succeeded
	        outputBean.setReturnCode(QsnAuthConstants.QSNGEN_001);
	        outputBean.setMessage(QsnAuthConstants.COMPLETED_SUCCESSFULLY);
	    } else if (successTypes.size() > 0) {
	        // Some succeeded, some failed
	        outputBean.setReturnCode(QsnAuthConstants.QSNGEN_003);  
	        outputBean.setMessage(QsnAuthConstants.COMPLETED_PARTIALLY);
	    } else {
	        // All failed - find the most frequent error code
	        String mostFrequentErrorCode = errorCodeCounter.entrySet()
	            .stream()
	            .max(Map.Entry.comparingByValue())
	            .map(Map.Entry::getKey)
	            .orElse("UNKNOWN");

	        switch (mostFrequentErrorCode) {
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


	    // Consolidate results
	    outputBean.setOutput(utility.consolidateQuestionsForMultipleTypes(questionListsByType, inputBean.getQuestionTypeConfigMap()));

	    if (Strings.isNullOrEmpty(outputBean.getReturnCode())) {
	        outputBean.setReturnCode(QsnAuthConstants.QSNGEN_001);
	        outputBean.setMessage(QsnAuthConstants.COMPLETED_SUCCESSFULLY);
	    }
	    
	    return outputBean;
	}

	@SuppressWarnings("unchecked")
	private void processQuestionType(String questionType,
            QuestionTypeConfig config,
            InputBean inputBean,
            HashMap<String, String> apiDetailsMap,
            String serviceName,
            Map<String, List<JSONArray>> questionListsByType,
            Set<String> successTypes,
            Set<String> failureTypes,
            Map<String, Integer> errorCodeCounter)
            throws InterruptedException, IOException, JSONException {

		List<JSONArray> consolidatedQuestions = questionListsByType.get(questionType);
		int totalQuestionsGenerated = 0;
		
		JSONObject responseFormat = responseFormatter.buildResponseFormat(questionType, 
				inputBean.getQuestionTypeConfigMap().get(questionType));
		
		if (inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT")) {
			responseFormat = new JSONObject();
		    responseFormat.put("type", "json_object"); 
		}
		logger.info("responseFormat for "+questionType+" : "+responseFormat);
		inputBean.getQuestionTypeConfigMap().get(questionType).setResponseFormat(responseFormat);
		
		List<Chunk> chunks = inputBean.getRetrievedChunks();
		
		logger.info("FInal Chunk is : "+chunks);
		if (chunks == null || chunks.isEmpty()) {
		    chunks = Collections.singletonList(null); // ensures loop runs once with null
		}else {
		    // Shuffle the list to randomize the order before looping
		    Collections.shuffle(chunks);
		}
		
		int chunkIndex = 0;
		for (Chunk chunk : chunks) {
		    if (totalQuestionsGenerated >= config.getMaxQuestions()) {
		        logger.error("Reached the maximum number of questions to generate for type: " + questionType + ". maxQuestions:" + config.getMaxQuestions() + ", Number of questions generated:" + totalQuestionsGenerated);
		        break;
		    }
		    logger.error("Generating "+ questionType +" question for Chunk num : "+chunkIndex );
		    
		    JSONArray prompt_messages = buildPromptMessages(questionType, chunk, inputBean);
		    logger.info("Final prompt print for "+questionType+" : " + prompt_messages);
		    
			Timestamp startTime = new Timestamp(System.currentTimeMillis());


		    GptResponseBean gptResponseBean = gptIntegration.invokeAPIwithResponseFormatFromGenAI(inputBean, apiDetailsMap, 
		    		                 prompt_messages,responseFormat);
		    
			Timestamp endTime = new Timestamp(System.currentTimeMillis());


		    totalQuestionsGenerated = processGptResponse(gptResponseBean, prompt_messages, questionType, 
		    						consolidatedQuestions, totalQuestionsGenerated, config,successTypes,failureTypes,errorCodeCounter,inputBean,startTime,endTime,serviceName);
		    chunkIndex++;
		}
	}
	
	private JSONArray buildPromptMessages(String questionType, Chunk chunk, InputBean inputBean) throws IOException {
	    switch (questionType) {
            case "mcq": return promptBuilder.getMCQPrompt(inputBean, chunk);
            case "faq": return promptBuilder.getShortAnswerPrompt(inputBean, chunk);
            case "truefalse": return promptBuilder.getTrueFalsePrompt(inputBean, chunk);
            case "msq": return promptBuilder.getMSQPrompt(inputBean, chunk);
            case "fillblanks": return promptBuilder.getFillInTheBlanksPrompt(inputBean, chunk);
            case "comprehensionmcq" : return promptBuilder.getComprehensionPrompt(inputBean, chunk);
            default: return new JSONArray();
        }
	    
	}
	
	/*private GptResponseBean invokeGPTWithRetry(HashMap<String, String> apiDetailsMap,
            JSONArray prompt_messages,
            JSONObject responseFormat,
            String questionType,
            String serviceName,
            InputBean inputBean) throws InterruptedException, JSONException {
		
		logger.error("Invoking GPT with prompt.");
		GptIntegration gptIntegration = new GptIntegration();
		Timestamp startTime = new Timestamp(System.currentTimeMillis());
		
		GptResponseBean gpt4oBean = gptIntegration.invokeAPIwithResponseFormat(apiDetailsMap, prompt_messages, responseFormat);
		Timestamp endTime = new Timestamp(System.currentTimeMillis());
		
		// Retry transient failures
		if (gpt4oBean.getStatusCode() == 429 ||
		gpt4oBean.getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE ||
		gpt4oBean.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR || gpt4oBean.getStatusCode() == 400) {

		
			logger.error("Retrying API call for " + questionType);
			Thread.sleep(5000);
			startTime = new Timestamp(System.currentTimeMillis());
			gpt4oBean = gptIntegration.invokeAPIwithResponseFormat(apiDetailsMap, prompt_messages, responseFormat);
			endTime = new Timestamp(System.currentTimeMillis());
		}
		
		// Thread-safe audit logging
		Map<String, String> auditdetailsMap = AuditThirdPartyAPICall.prepareAuditDetailsMap(
		inputBean.getOrgId(), inputBean.getAppId(), "QuestionGeneration_" + questionType, 
		serviceName, startTime, endTime, String.valueOf(gpt4oBean.getStatusCode()),
		gpt4oBean.getRawOutput(), prompt_messages.toString());
		
		synchronized (vendorAuditDetailsArray) {
			vendorAuditDetailsArray.add(auditdetailsMap);
			outputBean.setVendorAuditDetails(vendorAuditDetailsArray);
		}
		
		return gpt4oBean;
	}*/
	
	

	private int processGptResponse(GptResponseBean gptBean,
            JSONArray prompt_messages,
            String questionType,
            List<JSONArray> consolidatedQuestions,
            int totalQuestionsGenerated,
            QuestionTypeConfig config,
            Set<String> successTypes,
            Set<String> failureTypes,
            Map<String, Integer> errorCodeCounter,InputBean inputBean,Timestamp startTime,Timestamp endTime,String serviceName) {
		
		logger.error("Http status code for " + questionType + ": " + gptBean.getStatusCode());
		
			
		try {
			if (gptBean.getStatusCode() == HttpStatus.SC_OK) {
				if (gptBean.getOutput() != null && gptBean.getOutput().startsWith("Sorry")) {
					logger.error("Output starts with Sorry for " + questionType + ": " + gptBean.getOutput());
					
					failureTypes.add(questionType);
					errorCodeCounter.merge(String.valueOf(gptBean.getStatusCode()), 1, Integer::sum);
					return totalQuestionsGenerated;
				} else {
					JSONArray newQuestions = postProcessOutput(gptBean.getOutput(), questionType, inputBean);

					synchronized (consolidatedQuestions) {
					    consolidatedQuestions.add(newQuestions);
					}

					if ("comprehensionmcq".equalsIgnoreCase(questionType)) {
					    if (!newQuestions.isEmpty()) {
					        JSONObject wrapper = (JSONObject) newQuestions.get(0);
					        JSONArray innerQuestions = (JSONArray) wrapper.get("questions");
					        totalQuestionsGenerated += innerQuestions.size();
					    }
					} else {
					    totalQuestionsGenerated += newQuestions.size();
					}
					
					synchronized (outputBean) {
						outputBean.setThirdPartyDetails(gptBean.getThirdPartyDetails());
					}
					successTypes.add(questionType);
			
					if (totalQuestionsGenerated >= config.getMaxQuestions()) {
						logger.info("Stopping iteration for " + questionType + ". Total Questions Generated: " + totalQuestionsGenerated);
					}
				}
			} else if (gptBean.getStatusCode() == 429) {
				logger.error("Too many requests for " + questionType + ".");
				failureTypes.add(questionType);
				errorCodeCounter.merge(String.valueOf(gptBean.getStatusCode()), 1, Integer::sum);

			} else if (gptBean.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
				
				JSONObject jsonObject =(JSONObject) org.json.simple.JSONValue.parse(gptBean.getRawOutput());
				JSONObject error = (JSONObject) jsonObject.get("error");
				JSONObject innerError = (JSONObject) error.get("innererror");
				JSONObject contentFilterResult = (JSONObject) innerError.get("content_filter_result");
				StringBuilder blockedCategories = new StringBuilder("Blocked by Azure content filtering under category - ");
				boolean first = true;
				
				for (Object key : contentFilterResult.keySet()) {
					JSONObject category = (JSONObject) contentFilterResult.get(key);
					if ((boolean) category.get("filtered")) {
						 if (!first) blockedCategories.append(", ");
						 blockedCategories.append(key);
						 first = false;
					}
				}
				
				logger.error("Azure content filtering blocked request for " + questionType+ " HARMFUL_OR_INAPPROPRIATE_CONTENT"+blockedCategories );
				
				failureTypes.add(questionType);
				errorCodeCounter.merge(String.valueOf(gptBean.getStatusCode()), 1, Integer::sum);

			} else {
				logger.error("Service unavailable for " + questionType + ". Http status code: " + gptBean.getStatusCode());
				
				failureTypes.add(questionType);
				errorCodeCounter.merge(String.valueOf(gptBean.getStatusCode()), 1, Integer::sum);

			}
		} catch (Exception e) {
			logger.error("Error while processing GPT response for " + questionType, e);
			
			failureTypes.add(questionType);
			errorCodeCounter.merge(String.valueOf(gptBean.getStatusCode()), 1, Integer::sum);

			throw new RuntimeException(e);
		}
		
		
		
		Map<String, String> auditdetailsMap = AuditThirdPartyAPICall.prepareAuditDetailsMap(
				inputBean.getOrgId(), inputBean.getAppId(), "QuestionGeneration_" + questionType, 
				serviceName, startTime, endTime, String.valueOf(gptBean.getStatusCode()),
				gptBean.getRawOutput(), prompt_messages.toString());
				
		synchronized (vendorAuditDetailsArray) {
					vendorAuditDetailsArray.add(auditdetailsMap);
					outputBean.setVendorAuditDetails(vendorAuditDetailsArray);
			}
		
		return totalQuestionsGenerated;
	}

//	public JSONArray postProcessOutput(String output, String qsnType) {
//	    JSONArray questions = new JSONArray();
//	    try {
//	        // Parse root JSON
//	        JSONParser parser = new JSONParser();
//	        JSONObject root = (JSONObject) parser.parse(output);
//
//	        // Extract "questions" array
//	        questions = (JSONArray) root.get("questions");
//	        
//	    } catch (Exception e) {
//	        e.printStackTrace();
//	    }
//
//	    return questions;
//	}
	
	
	public JSONArray postProcessOutput(String output, String qsnType, InputBean inputBean) {
	    JSONArray resultArray = new JSONArray();

	    try {
	        JSONParser parser = new JSONParser();
	        JSONObject root = (JSONObject) parser.parse(output);

	        if ("comprehensionmcq".equalsIgnoreCase(qsnType)) {
	            JSONObject comprehensionObject = new JSONObject();
	            // Add passage
	            comprehensionObject.put("passage", root.getOrDefault("passage", ""));

	            // Add questions
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
	        e.printStackTrace();
	    }
	    
	    
	    
	    logger.error("ResultArray{} "+ qsnType+ " "+ resultArray);
	    


		if ("faq".equalsIgnoreCase(qsnType) &&
		    inputBean.getQuestionTypeConfigMap() != null &&
		    inputBean.getQuestionTypeConfigMap().containsKey("faq") &&
		    inputBean.getQuestionTypeConfigMap().get("faq").isMarkingEnabled())
		 {
			
			Utility util = new Utility();
			logger.error("Entering for process faq marking scheme");
		


		util.processFAQWithMarkingScheme(
		        resultArray,
		        inputBean.getAIvendorCredNodeMap(),
		        inputBean.getDirectives(),
		        Long.parseLong("313"),
		        Long.parseLong(inputBean.getOrgId()),
		        Integer.parseInt("128"),
		        inputBean.getUsecaseId(),
		        String.valueOf(inputBean.getUserRegisteredServiceId())
		    );
		
		logger.error("Exit faq marking scheme");


		}

	    return resultArray;
	}
	
	
	
	private void shuffleOptionsIfRequired(JSONArray questionsArray, String qsnType) {

	    if (!"comprehensionmcq".equalsIgnoreCase(qsnType) &&
	        !"mcq".equalsIgnoreCase(qsnType) &&
	        !"msq".equalsIgnoreCase(qsnType)) {
	        return; // No shuffle needed
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
	
	public static void main(String[] args){
		GPTBasedAuthoring auth = new GPTBasedAuthoring();
//		System.out.println(auth.postProcessOutput(
//				"{\"questions\":[{\"question\":\"Which of the following is NOT considered a renewable energy source?\",\"answer\":\"Coal\",\"knowMore\":\"Renewable energy sources are those that can be replenished naturally, such as solar, wind, and hydropower. Coal is a fossil fuel and is non-renewable.\",\"hint\":\"Think about energy sources that are naturally replenished.\",\"choices\":[\"Solar power\",\"Wind energy\",\"Hydropower\",\"Coal\"]},{\"question\":\"What is the primary advantage of using solar energy?\",\"answer\":\"It is environmentally friendly and does not produce greenhouse gases.\",\"knowMore\":\"Solar energy harnesses sunlight and transforms it into electricity with minimal environmental impact as it does not emit greenhouse gases during production.\",\"hint\":\"Consider the environmental benefits of solar energy.\",\"choices\":[\"It is available only at night\",\"It is environmentally friendly and does not produce greenhouse gases\",\"It requires burning fossil fuels\",\"It increases air pollution\"]},{\"question\":\"Which technology converts wind energy into electricity?\",\"answer\":\"Wind turbines\",\"knowMore\":\"Wind turbines capture the kinetic energy of wind and convert it into electrical energy.\",\"hint\":\"Look for the machine used for harvesting wind energy.\",\"choices\":[\"Hydroelectric dams\",\"Geothermal wells\",\"Wind turbines\",\"Solar panels\"]}]}", 
//				"mcq"));
	}

	
}

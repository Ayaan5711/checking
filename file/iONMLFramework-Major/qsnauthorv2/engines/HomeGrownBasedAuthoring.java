package com.tcsion.ml.qsnauthorv2.engines;

import java.io.IOException;
import java.net.InetAddress;
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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import com.google.common.base.Strings;
import com.tcsion.ml.Utils.ExceptionLogUtility;
import com.tcsion.ml.filereader.Chunk;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.OutputBean;
import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;
import com.tcsion.ml.qsnauthorv2.prompts.HomeGrownPromptBuilder;
import com.tcsion.ml.qsnauthorv2.util.QsnAuthConstants;
import com.tcsion.ml.qsnauthorv2.util.Utility;
import com.tcsion.ml.audit.AuditThirdPartyAPICall;
import com.tcsion.ml.azure.mistral.MistralIntegration;
//import com.tcsion.ml.azure.AzureResponseBean;
import com.tcsion.ml.azure.mistral.MistralResponseBean;
import com.tcsion.ml.azure.openai.GptResponseBean;

public class HomeGrownBasedAuthoring {
	private static final Log logger = LogFactory.getLog(HomeGrownBasedAuthoring.class);
	private OutputBean outputBean = new OutputBean();
	private JSONObject finalOutput = new JSONObject ();
	private Utility utility = new Utility();
	//private GPTPromptBuilder promptBuilder = new GPTPromptBuilder();
	private HomeGrownPromptBuilder promptBuilderHG = new HomeGrownPromptBuilder();
	private ArrayList<Map<String, String>> vendorAuditDetailsArray = new ArrayList<Map<String, String>>();
	MistralIntegration mistralIntegration = new MistralIntegration();
	//private GPTResponseFormatBuilder responseFormatter = new GPTResponseFormatBuilder();
	
	public OutputBean authoringByHGAI(InputBean inputBean) throws InterruptedException, IOException {
	    
	        /*if (inputBean.getMetadataBean().getShuffleChunks() == true) {
	            Collections.shuffle(allChunks);
	        }*/
		logger.error("---inside HomeGrownBasedAuthoring ---");
	    	HashMap<String, String> apiDetailsMap = inputBean.getAIvendorCredNodeMap().values().iterator().next();
		    logger.info("apiDetailsMap-----"+ apiDetailsMap);
	    	String serviceName = inputBean.getAIvendorCredNodeMap().keySet().iterator().next();
	       logger.error("serviceName---"+serviceName);
		    // Map<String, List<JSONArray>> questionListsByType = new HashMap<>();
	        
	        
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
	        
	        Timestamp invoke_start_time = new Timestamp(System.currentTimeMillis());
	        
	        // Process each question type sequentially
	        for (Map.Entry<String, QuestionTypeConfig> entry : inputBean.getQuestionTypeConfigMap().entrySet()) {
	            String questionType = entry.getKey();
	            QuestionTypeConfig config = entry.getValue();
	            
	            Future<Void> future = executor.submit(() -> {
		            try {
		            	processHGAIQuestionType(questionType, config, inputBean,apiDetailsMap, 
		    					serviceName, questionListsByType,successTypes,failureTypes,errorCodeCounter);
		            } catch (Exception e) {
		                logger.error("Exception in processing question type: " + questionType, e);
		                throw new RuntimeException(e);
		            }
		            return null;
		        });
	            futures.add(future);
	            logger.info("Processing question type: " + questionType + " with max questions: " + config.getMaxQuestions());
	            
	            
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
		            else{
		            	logger.info("To clear sonar");
		            }
		        }
		        
		        
		        outputBean.setMessage(QsnAuthConstants.ISSUE_IN_THIRD_PARTY_INTEGRATION);
		        outputBean.setReturnCode(QsnAuthConstants.QSNGEN_987);
		        return outputBean;
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

	        // old code
		    
	       /* Timestamp response_get_time = new Timestamp(System.currentTimeMillis());
	        long timeTakenMillis = response_get_time.getTime() - invoke_start_time.getTime();
	        
	        logger.info("Total time taken for Question generation: " + timeTakenMillis);
	        
	        // Check if any questions were generated
	        boolean hasQuestions = questionListsByType.values().stream()
	            .anyMatch(list -> !list.isEmpty());
	        
	        if (hasQuestions) {
	            outputBean.setOutput(utility.consolidateQuestionsForMultipleTypes(questionListsByType, inputBean.getQuestionTypeConfigMap()));
	            outputBean.setReturnCode(QsnAuthConstants.QSNGEN_001);
	            outputBean.setMessage(QsnAuthConstants.COMPLETED_SUCCESSFULLY);
	        } else {
	            outputBean.setOutput(null);
	            outputBean.setReturnCode(QsnAuthConstants.QSNGEN_994);
	            outputBean.setMessage(QsnAuthConstants.COMPLETED_BUT_QUESTION_NOT_GENERATED);
	        }
	        
	   
	    return outputBean;*/
	}

	private void processHGAIQuestionType(
			String questionType, 
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
		
		// check it
		List<Chunk> chunks = inputBean.getRetrievedChunks();
		
		logger.error("FInal Chunk is : "+chunks);
		if (chunks == null || chunks.isEmpty()) {
		    chunks = Collections.singletonList(null); // ensures loop runs once with null
		}else {
		    // Shuffle the list to randomize the order before looping
		    Collections.shuffle(chunks);
		}
		//
		
		int chunkIndex = 0;
	    for (Chunk chunk : chunks) {
	        logger.error("Processing context for " + questionType + ". Total questions generated so far: " + totalQuestionsGenerated);
	        
	        if (totalQuestionsGenerated >= config.getMaxQuestions()) {
	            logger.error("Reached the maximum number of questions to generate for " + questionType + 
	                       ". maxQuestions:" + config.getMaxQuestions() + 
	                       ", Number of questions generated:" + totalQuestionsGenerated);
	            break;
	        }
	        JSONArray prompt_messages = buildPromptMessages(questionType, chunk, inputBean);
		    logger.error("Final prompt print for "+questionType+" : " + prompt_messages);
		    
			Timestamp startTime = new Timestamp(System.currentTimeMillis());

		    
		    JSONObject responseFormat = null;
	        MistralResponseBean mistralResponseBean = mistralIntegration.invokeAPIwithResponseFormatFromGenAI(inputBean,apiDetailsMap, 
	        		                             prompt_messages, responseFormat);
			Timestamp endTime = new Timestamp(System.currentTimeMillis());

	        
	        totalQuestionsGenerated = processMistralResponse(mistralResponseBean, prompt_messages, questionType, 
					consolidatedQuestions, totalQuestionsGenerated, config,successTypes,failureTypes,errorCodeCounter,inputBean,startTime,endTime,serviceName);
	        
	        chunkIndex++;
	    }
	    
	    logger.info("Completed processing for question type: " + questionType + 
	               ". Total questions generated: " + totalQuestionsGenerated);
	}
	
	private JSONArray buildPromptMessages(String questionType, Chunk chunk, InputBean inputBean) throws IOException {
	    switch (questionType) {
            case "mcq": return promptBuilderHG.getMCQPrompt(inputBean, chunk);
            case "faq": return promptBuilderHG.getShortAnswerPrompt(inputBean, chunk);
            case "truefalse": return promptBuilderHG.getTrueFalsePrompt(inputBean, chunk);
            case "msq": return promptBuilderHG.getMSQPrompt(inputBean, chunk);
            case "fillblanks": return promptBuilderHG.getFillInTheBlanksPrompt(inputBean, chunk);
            case "comprehensionmcq" : return promptBuilderHG.getComprehensionPrompt(inputBean, chunk);
            default: return new JSONArray();
        }
	    
	}
	
	
	
	/*private MistralResponseBean invokeMistralWithRetry(HashMap<String, String> apiDetailsMap,
			JSONArray prompt_messages,
            String questionType,
            String serviceName,
            InputBean inputBean) throws InterruptedException, JSONException {
		
		logger.error("Invoking mistral with prompt.");
		MistralIntegration mistralIntegration = new MistralIntegration();
		Timestamp startTime = new Timestamp(System.currentTimeMillis());
		
		MistralResponseBean mistralBean = mistralIntegration.invokeAPI(apiDetailsMap, prompt_messages);
		Timestamp endTime = new Timestamp(System.currentTimeMillis());
		
		// Retry transient failures
		if (mistralBean.getStatusCode() == 429 ||
				mistralBean.getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE ||
						mistralBean.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR || mistralBean.getStatusCode() == 400) {

		
			logger.error("Retrying API call for " + questionType);
			Thread.sleep(5000);
			startTime = new Timestamp(System.currentTimeMillis());
			mistralBean = mistralIntegration.invokeAPI(apiDetailsMap, prompt_messages);
			endTime = new Timestamp(System.currentTimeMillis());
		}
		
		// Thread-safe audit logging
		Map<String, String> auditdetailsMap = AuditThirdPartyAPICall.prepareAuditDetailsMap(
		inputBean.getOrgId(), inputBean.getAppId(), "QuestionGeneration_" + questionType, 
		serviceName, startTime, endTime, String.valueOf(mistralBean.getStatusCode()),
		mistralBean.getRawOutput(), prompt_messages.toString());
		
		synchronized (vendorAuditDetailsArray) {
			vendorAuditDetailsArray.add(auditdetailsMap);
			outputBean.setVendorAuditDetails(vendorAuditDetailsArray);
		}
		
		return mistralBean;
	}*/
	
	private int processMistralResponse(MistralResponseBean mistralBean ,
            JSONArray prompt_messages,
            String questionType,
            List<JSONArray> consolidatedQuestions,
            int totalQuestionsGenerated,
            QuestionTypeConfig config,
            Set<String> successTypes,
            Set<String> failureTypes,
            Map<String, Integer> errorCodeCounter,InputBean inputBean,Timestamp startTime,Timestamp endTime,String serviceName) {
		
		logger.error("Http status code for " + questionType + ": " + mistralBean.getStatusCode());
			
		try {
			if (mistralBean.getStatusCode() == HttpStatus.SC_OK) {
				if (mistralBean.getOutput().startsWith("Sorry")) {
					logger.error("Output starts with Sorry for " + questionType + ": " + mistralBean.getOutput());
					
					failureTypes.add(questionType);
					errorCodeCounter.merge(String.valueOf(mistralBean.getStatusCode()), 1, Integer::sum);

					return totalQuestionsGenerated;
				} else {
					JSONArray newQuestions = postProcessOutput(mistralBean.getOutput(),questionType);
					synchronized (consolidatedQuestions) {
						consolidatedQuestions.add(newQuestions);
					}
					totalQuestionsGenerated += newQuestions.size();
			
					synchronized (outputBean) {
						outputBean.setThirdPartyDetails(mistralBean.getThirdPartyDetails());
					}
					successTypes.add(questionType);
			
					if (totalQuestionsGenerated >= config.getMaxQuestions()) {
						logger.info("Stopping iteration for " + questionType + ". Total Questions Generated: " + totalQuestionsGenerated);
					}
				}
			} else if (mistralBean.getStatusCode() == 429) {
				logger.error("Too many requests for " + questionType + ".");
				failureTypes.add(questionType);
				errorCodeCounter.merge(String.valueOf(mistralBean.getStatusCode()), 1, Integer::sum);

			} else if (mistralBean.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
				
				JSONObject jsonObject = (JSONObject) org.json.simple.JSONValue.parse(mistralBean.getOutput());
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
				errorCodeCounter.merge(String.valueOf(mistralBean.getStatusCode()), 1, Integer::sum);

			} else {
				logger.error("Service unavailable for " + questionType + ". Http status code: " + mistralBean.getStatusCode());
				
				failureTypes.add(questionType);
				errorCodeCounter.merge(String.valueOf(mistralBean.getStatusCode()), 1, Integer::sum);

			}
		} catch (Exception e) {
			logger.error("Error while processing mistral response for " + questionType, e);
			
			failureTypes.add(questionType);
			errorCodeCounter.merge(String.valueOf(mistralBean.getStatusCode()), 1, Integer::sum);

			throw new RuntimeException(e);
		}
		
		Map<String, String> auditdetailsMap = AuditThirdPartyAPICall.prepareAuditDetailsMap(
				inputBean.getOrgId(), inputBean.getAppId(), "QuestionGeneration_" + questionType, 
				serviceName, startTime, endTime, String.valueOf(mistralBean.getStatusCode()),
				mistralBean.getRawOutput(), prompt_messages.toString());
				
		synchronized (vendorAuditDetailsArray) {
					vendorAuditDetailsArray.add(auditdetailsMap);
					outputBean.setVendorAuditDetails(vendorAuditDetailsArray);
			}
		
		return totalQuestionsGenerated;
	}
	
	/*@SuppressWarnings("unchecked")
	public static String flaskAPICall(InputBean inputBean, List<Map<String, String>> messages) {
	    HttpPost post = null;
	    String responseString = null;
	    String text = null;
	    try {
	        RequestConfig requestConfig = RequestConfig.custom()
	        		.setConnectTimeout(4000 * 1000)
	        		.setSocketTimeout(4000 * 1000) 
	                .build();

	        CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

	        String addr = InetAddress.getLocalHost().getHostAddress();
	        post = new HttpPost("http://" + addr + ":" + inputBean.getPort() + "/tcsionHgAiInference");

	        JSONObject json = new JSONObject();
	        json.put("messages", messages);
	        json.put("jobId", inputBean.getJobId());
	        json.put("requestId", inputBean.getRequestId());
	        json.put("temperature", inputBean.getMetadataBean().getTemperature());
	        json.put("topk", inputBean.getMetadataBean().getTopK());
	        json.put("seqLength", inputBean.getMetadataBean().getSequenceLength());
	        
	        StringEntity params = new StringEntity(json.toString(), "UTF-8");
	        logger.info("----------THE HTTP REQUEST IS----------" + json.toString());

	        post.addHeader("content-type", "application/json");
	        post.setProtocolVersion(HttpVersion.HTTP_1_1);
	        post.setEntity(params);
	        logger.info("Invoking flask API - requestId " + inputBean.getRequestId() + " JobID : " + inputBean.getJobId());
	        HttpResponse response = httpclient.execute(post);
	        int response_code = response.getStatusLine().getStatusCode();
	        logger.info("Completed flask API request " + inputBean.getRequestId() + " JobID : " + inputBean.getJobId() + " Response_code: " + response_code);
	        if (response_code == HttpStatus.SC_OK) {
	            HttpEntity entity = response.getEntity();
	            responseString = EntityUtils.toString(entity, "UTF-8");
	            logger.info("---THE RESPONSE STRING IS: " + responseString);

	        } else {
	            logger.error("ERROR OCCURED IN SYNC SENDHTTP SCRIPT,STATUS CODE NOT 200");
	            logger.error("---THE RESPONSE STRING IS: " + responseString);
	        }
	    } catch (Exception e) {
	        responseString = null;
	        ExceptionLogUtility.logException("Exception in flaskAPICall : ", e, logger);
	    } finally {
	        if (post != null) {
	            text = responseString;
	            post.releaseConnection();
	        }
	    }
	    return text;
	}*/

	@SuppressWarnings("unchecked")
	public JSONArray outputPreparation(String output, String qsnType) {
	    JSONArray finalOutput = new JSONArray();

	    try {
	        String[] segments = output.split("Question:");
	        for (int i = 1; i < segments.length; i++) {
	            String[] parts = segments[i].split("Answer:");
	            String question = parts[0].replace("\n", " ").trim();
	            String answer = parts[1].replace("\n", " ").trim();

	            if ("mcq".equalsIgnoreCase(qsnType)) {
	                List<String> options = new ArrayList<>();
	                Matcher matcher = Pattern.compile("\\b[a-e]\\)\\s.*?(?=\\s[a-e]\\)|$)").matcher(question);
	                while (matcher.find()) {
	                    String option = matcher.group().substring(3).trim();
	                    if (option.endsWith(",")) {
	                        option = option.substring(0, option.length() - 1);
	                    }
	                    options.add(option);
	                }

	                answer = answer.replaceAll("^[a-e]\\)\\s*", "");
	                answer = utility.sentenceWithLowestDistance(answer, options);
	                question = question.split("Multiple choices:")[0].trim();

	                JSONObject qna = new JSONObject();
	                qna.put("question", question);
	                qna.put("answer", answer);
	                qna.put("options", options);
	                finalOutput.add(qna);
	            } else {
	                JSONObject qna = new JSONObject();
	                qna.put("question", question);
	                qna.put("answer", answer);
	                finalOutput.add(qna);
	            }
	        }
	    } catch (Exception e) {
	        logger.error("Exception during output processing: " + e.getMessage());
	    }

	    return finalOutput;
	}
	
//	public JSONArray postProcessOutput(String output) {
//	    JSONArray questions = new JSONArray();
//	    try {
//	        // Parse root JSON
//	    	logger.info("output in post process before processing "+ output);
//	    	output=output.replaceAll("```json", "")
//                    .replaceAll("```", "")
//                    .trim();
//	    	logger.info("output in post process"+ output);
//	    	logger.error(output.getClass().getSimpleName());
//	        JSONParser parser = new JSONParser();
//	         questions = (JSONArray) parser.parse(output);
//	         logger.error("First element" +questions.get(0));
//	         logger.error("size" + questions.size());
//	        
//	       
//	        
//	        
//	        /*String jsonString = simpleArray.toJSONString();
//	        JSONObject jsonObject = new JSONObject();
//	        jsonObject.put("questions", new org.json.JSONArray(jsonString)); */
//	        /*Object obj = parser.parse(output);
//	        //JSONArray array = new JSONArray();
//
//	        JSONObject root = (JSONObject) (obj);*/
//
//	        // Extract "questions" array
//	        //questions = (JSONArray) jsonObject.get("questions");
//	        
//	    } catch (Exception e) {
//	    	logger.error("Error while post processing the output", e);
//	       // e.printStackTrace();
//	    }
//
//	    return questions;
//	}
	
	
	
	public JSONArray postProcessOutput(String output, String qsnType) {
	    JSONArray resultArray = new JSONArray();

	    try {
	        output = output.replaceAll("```json", "")
	                       .replaceAll("```", "")
	                       .trim();

	        JSONParser parser = new JSONParser();
	        Object parsed = parser.parse(output);

	        JSONObject root;

	        // Ã°Å¸â€˜â€° Normalize: always work with JSONObject
	        if (parsed instanceof JSONArray) {
	            root = new JSONObject();
	            root.put("questions", parsed);
	        } else {
	            root = (JSONObject) parsed;
	        }

	        JSONArray questionsArray = (JSONArray) root.getOrDefault("questions", new JSONArray());
	        shuffleOptionsIfRequired(questionsArray, qsnType);

	        // Ã°Å¸â€˜â€° Only special handling for comprehension
	        if ("comprehensionmcq".equalsIgnoreCase(qsnType)) {
	            JSONObject comprehensionObject = new JSONObject();
	            comprehensionObject.put("passage", root.getOrDefault("passage", ""));
	            comprehensionObject.put("questions", questionsArray);
	            resultArray.add(comprehensionObject);
	        } else {
	            resultArray = questionsArray;
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
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
   
}

package com.tcsion.ml.qsnauthorv2.prompts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.tcs.genai.prompt.service.Impl.PayloadGeneratorService;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.filereader.Chunk;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;

public class GPTPromptBuilder {
	private static final Log logger = LogFactory.getLog(GPTPromptBuilder.class);
	Map<String, String> parameterMap =  new HashMap<String, String>();
		
    
    
    public JSONArray getMCQPrompt(InputBean inputBean, Chunk chunk) throws IOException {
    	
    	logger.error("-----Inside getMCQPrompt----");
        QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("mcq");

        logger.info("vendorDetailsMap {} " + inputBean.getAIvendorCredNodeMap());
        logger.info("orgid " + inputBean.getOrgId());
        logger.info("appid " + inputBean.getAppId());
        logger.info("apiid " + inputBean.getApiID());

        JSONArray promptMessages = new JSONArray();

        try {
            // Step 1: Call GenAI framework to get the raw payload with placeholders
            JSONObject payloadFromGenAI = callGenAIUtility(
                inputBean.getAIvendorCredNodeMap(),
                Long.parseLong(inputBean.getOrgId()),
                Long.parseLong(inputBean.getAppId()),
                Integer.parseInt(inputBean.getApiID()),
                "mcq"
            );

            logger.info("Payload received from GenAI framework: " + payloadFromGenAI);

            // Step 2: Extract the 'messages' array from the JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");

            JsonNode messagesNode = payloadNode.get("messages");
            
            logger.info("Extract the messages array from the JSON :"+messagesNode);

            // Convert Jackson JsonNode to org.json.simple.JSONArray
            JSONParser parser = new JSONParser();
            JSONArray dbMessages = (JSONArray) parser.parse(messagesNode.toString());

            // Step 3: Process placeholders, add image or context as needed
            promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig,"MCQ");

            logger.info("Final prompt messages after replacement (MCQ): " + promptMessages);

        } catch (Exception e) {
            logger.error("Error while generating MCQ prompt", e);
            throw new IOException("Failed to generate prompt", e);
        }

        return promptMessages;
    }

    	
    	@SuppressWarnings("unchecked")
    	public JSONArray getMSQPrompt(InputBean inputBean, Chunk chunk) throws IOException {
    	    logger.error("-----Inside getMSQPrompt----");

    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("msq");

    	    logger.info("vendorDetailsMap {} " + inputBean.getAIvendorCredNodeMap());
    	    logger.info("orgid " + inputBean.getOrgId());
    	    logger.info("appid " + inputBean.getAppId());
    	    logger.info("apiid " + inputBean.getApiID());

    	    JSONArray promptMessages = new JSONArray();

    	    try {
    	        // Step 1: Call GenAI framework to get the raw payload with placeholders
    	        JSONObject payloadFromGenAI = callGenAIUtility(
    	            inputBean.getAIvendorCredNodeMap(),
    	            Long.parseLong(inputBean.getOrgId()),
    	            Long.parseLong(inputBean.getAppId()),
    	            Integer.parseInt(inputBean.getApiID()),
    	            "msq"
    	        );

    	        logger.info("Payload received from GenAI framework: " + payloadFromGenAI);

    	        // Step 2: Extract the 'messages' array from the JSON
    	        ObjectMapper objectMapper = new ObjectMapper();
    	        JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");

    	        JsonNode messagesNode = payloadNode.get("messages");

    	        logger.info("Extracted the messages array from the JSON: " + messagesNode);

    	        // Convert Jackson JsonNode to org.json.simple.JSONArray
    	        JSONParser parser = new JSONParser();
    	        JSONArray dbMessages = (JSONArray) parser.parse(messagesNode.toString());

    	        // Step 3: Process placeholders, add image or context as needed
    	        promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "multi-select");

    	        logger.info("Final prompt messages after replacement (MSQ): " + promptMessages);

    	    } catch (Exception e) {
    	        logger.error("Error while generating MSQ prompt", e);
    	        throw new IOException("Failed to generate prompt", e);
    	    }

    	    return promptMessages;
    	}

   	
    	@SuppressWarnings("unchecked")
    	public JSONArray getShortAnswerPrompt(InputBean inputBean, Chunk chunk) throws IOException {
    	    logger.error("-----Inside getShortAnswerPrompt----");

    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("faq");

    	    logger.info("vendorDetailsMap {} " + inputBean.getAIvendorCredNodeMap());
    	    logger.info("orgid " + inputBean.getOrgId());
    	    logger.info("appid " + inputBean.getAppId());
    	    logger.info("apiid " + inputBean.getApiID());

    	    JSONArray promptMessages = new JSONArray();

    	    try {
    	        // Step 1: Call GenAI framework to get the raw payload with placeholders
    	        JSONObject payloadFromGenAI = callGenAIUtility(
    	            inputBean.getAIvendorCredNodeMap(),
    	            Long.parseLong(inputBean.getOrgId()),
    	            Long.parseLong(inputBean.getAppId()),
    	            Integer.parseInt(inputBean.getApiID()),
    	            "faq"
    	        );

    	        logger.info("Payload received from GenAI framework (FAQ): " + payloadFromGenAI);

    	        // Step 2: Extract the 'messages' array from the JSON
    	        ObjectMapper objectMapper = new ObjectMapper();
    	        JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");

    	        JsonNode messagesNode = payloadNode.get("messages");

    	        logger.info("Extracted messages array from payload: " + messagesNode);

    	        // Convert Jackson JsonNode to org.json.simple.JSONArray
    	        JSONParser parser = new JSONParser();
    	        JSONArray dbMessages = (JSONArray) parser.parse(messagesNode.toString());

    	        // Step 3: Process placeholders and prepare prompt
    	        promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "short-answer");

    	        logger.info("Final prompt messages after replacement (FAQ): " + promptMessages);

    	    } catch (Exception e) {
    	        logger.error("Error while generating Short Answer prompt", e);
    	        throw new IOException("Failed to generate prompt", e);
    	    }

    	    return promptMessages;
    	}

    	
    	
    	@SuppressWarnings("unchecked")
    	public JSONArray getTrueFalsePrompt(InputBean inputBean, Chunk chunk) throws IOException {
    	    logger.error("-----Inside getTrueFalsePrompt----");

    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("truefalse");

    	    logger.info("vendorDetailsMap {} " + inputBean.getAIvendorCredNodeMap());
    	    logger.info("orgid " + inputBean.getOrgId());
    	    logger.info("appid " + inputBean.getAppId());
    	    logger.info("apiid " + inputBean.getApiID());

    	    JSONArray promptMessages = new JSONArray();

    	    try {
    	        // Step 1: Call GenAI framework to get the raw prompt payload (template with placeholders)
    	        JSONObject payloadFromGenAI = callGenAIUtility(
    	            inputBean.getAIvendorCredNodeMap(),
    	            Long.parseLong(inputBean.getOrgId()),
    	            Long.parseLong(inputBean.getAppId()),
    	            Integer.parseInt(inputBean.getApiID()),
    	            "truefalse"
    	        );

    	        logger.info("Payload received from GenAI framework (True/False): " + payloadFromGenAI);

    	        // Step 2: Extract the 'messages' array from the payload
    	        ObjectMapper objectMapper = new ObjectMapper();
    	        JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");

    	        JsonNode messagesNode = payloadNode.get("messages");

    	        logger.info("Extracted messages array from payload: " + messagesNode);

    	        // Step 3: Convert to org.json.simple.JSONArray
    	        JSONParser parser = new JSONParser();
    	        JSONArray dbMessages = (JSONArray) parser.parse(messagesNode.toString());

    	        // Step 4: Process placeholders and prepare the final prompt
    	        promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "TrueFalse");

    	        logger.info("Final prompt messages after replacement (True/False): " + promptMessages);

    	    } catch (Exception e) {
    	        logger.error("Error while generating True/False prompt", e);
    	        throw new IOException("Failed to generate True/False prompt", e);
    	    }

    	    return promptMessages;
    	}
    	
  	
    	@SuppressWarnings("unchecked")
    	public JSONArray getFillInTheBlanksPrompt(InputBean inputBean, Chunk chunk) throws IOException {
    	    logger.error("-----Inside getFillInTheBlanksPrompt----");

    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("fillblanks");

    	    logger.info("vendorDetailsMap {} " + inputBean.getAIvendorCredNodeMap());
    	    logger.info("orgid " + inputBean.getOrgId());
    	    logger.info("appid " + inputBean.getAppId());
    	    logger.info("apiid " + inputBean.getApiID());

    	    JSONArray promptMessages = new JSONArray();

    	    try {
    	        // Step 1: Call GenAI framework to get the raw payload with placeholders
    	        JSONObject payloadFromGenAI = callGenAIUtility(
    	            inputBean.getAIvendorCredNodeMap(),
    	            Long.parseLong(inputBean.getOrgId()),
    	            Long.parseLong(inputBean.getAppId()),
    	            Integer.parseInt(inputBean.getApiID()),
    	            "fillintheblanks"
    	        );

    	        logger.info("Payload received from GenAI framework: " + payloadFromGenAI);

    	        // Step 2: Extract the 'messages' array from the JSON
    	        ObjectMapper objectMapper = new ObjectMapper();
    	        JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");

    	        JsonNode messagesNode = payloadNode.get("messages");

    	        logger.info("Extracted the messages array from the JSON: " + messagesNode);

    	        // Convert Jackson JsonNode to org.json.simple.JSONArray
    	        JSONParser parser = new JSONParser();
    	        JSONArray dbMessages = (JSONArray) parser.parse(messagesNode.toString());

    	        // Step 3: Process placeholders, add image or context as needed
    	        promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "fill-in-the-blank");

    	        logger.info("Final prompt messages after replacement (fill-in-the-blanks): " + promptMessages);

    	    } catch (Exception e) {
    	        logger.error("Error while generating Fill in the Blanks prompt", e);
    	        throw new IOException("Failed to generate Fill in the Blanks prompt", e);
    	    }

    	    return promptMessages;
    	}

    	public JSONArray getComprehensionPrompt(InputBean inputBean, Chunk chunk) throws IOException {
    		logger.error("-----Inside ComprehensionPrompt----");

    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("comprehensionmcq");

    	    logger.info("vendorDetailsMap {} " + inputBean.getAIvendorCredNodeMap());
    	    logger.info("orgid " + inputBean.getOrgId());
    	    logger.info("appid " + inputBean.getAppId());
    	    logger.info("apiid " + inputBean.getApiID());

    	    JSONArray promptMessages = new JSONArray();
    	    try {
    	        // Step 1: Call GenAI framework to get the raw payload with placeholders
    	        JSONObject payloadFromGenAI = callGenAIUtility(
    	            inputBean.getAIvendorCredNodeMap(),
    	            Long.parseLong(inputBean.getOrgId()),
    	            Long.parseLong(inputBean.getAppId()),
    	            Integer.parseInt(inputBean.getApiID()),
    	            "comprehension"
    	        );

    	        logger.info("Payload received from GenAI framework: " + payloadFromGenAI);

    	        // Step 2: Extract the 'messages' array from the JSON
    	        ObjectMapper objectMapper = new ObjectMapper();
    	        JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");

    	        JsonNode messagesNode = payloadNode.get("messages");

    	        logger.info("Extracted the messages array from the JSON: " + messagesNode);

    	        // Convert Jackson JsonNode to org.json.simple.JSONArray
    	        JSONParser parser = new JSONParser();
    	        JSONArray dbMessages = (JSONArray) parser.parse(messagesNode.toString());

    	        // Step 3: Process placeholders, add image or context as needed
    	        promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "comprehension");

    	        logger.info("Final prompt messages after replacement (comprehension): " + promptMessages);

    	    } catch (Exception e) {
    	        logger.error("Error while generating comprehension prompt", e);
    	        throw new IOException("Failed to generate comprehension prompt", e);
    	    }

    		return promptMessages;
    	}

    	public JSONArray preparePromptFromDBPayload(JSONArray dbMessages, InputBean inputBean, Chunk chunk, QuestionTypeConfig qTypeConfig,String QuestionType) {
    		JSONArray updatedMessages = new JSONArray();
    		boolean imageInserted = false;
    	
    		for (int i = 0; i < dbMessages.size(); i++) {
    			JSONObject message = (JSONObject) dbMessages.get(i);
    			String role = (String) message.get("role");
    			JSONArray contentArray = (JSONArray) message.get("content");
    	
    			JSONArray updatedContentArray = new JSONArray();
    	
    			for (int j = 0; j < contentArray.size(); j++) {
    				JSONObject contentItem = (JSONObject) contentArray.get(j);
    	
    				if ("text".equals(contentItem.get("type"))) {
    					String text = (String) contentItem.get("text");
    	
    					// Replace placeholders
    					text = replaceAllPlaceholders(text, inputBean, chunk, qTypeConfig,QuestionType);
    	
    					JSONObject updatedText = new JSONObject();
    					updatedText.put("type", "text");
    					updatedText.put("text", text);
    					updatedContentArray.add(updatedText);
    	
    					// Inject image_url after first text block of first user message
    					if (!imageInserted && "user".equals(role) &&
    						chunk != null && !Strings.isNullOrEmpty(chunk.getImagePath()) ) {

    						try {
    							String base64Image = encodeImageToBase64(chunk.getImagePath());
    							JSONObject imageBlock = buildImageContent(base64Image);
    							updatedContentArray.add(imageBlock);
    							imageInserted = true;
    						} catch (Exception e) {
    							logger.error("Image encoding failed, skipping image injection", e);
    						}
    					}

    	
    				} else {
    					// Keep non-text content unchanged
    					updatedContentArray.add(contentItem);
    				}
    			}
    	
    			// Rebuild the updated message
    			JSONObject updatedMessage = new JSONObject();
    			updatedMessage.put("role", role);
    			updatedMessage.put("content", updatedContentArray);
    			updatedMessages.add(updatedMessage);
    		}
    	
    		return updatedMessages;
    	}
    	



    	private String replaceAllPlaceholders(String text, InputBean inputBean, Chunk chunk, QuestionTypeConfig qTypeConfig, String QuestionType) {
    		logger.info("Inside replaceholders ------");
    		logger.info("qTypeConfig is "+ qTypeConfig.toString());
    		
    		
    		
    		String fileType = inputBean.getFileType();
    		boolean imageInserted = false;
    		String imagebased = "";

    		String maxQuestions = ("kb".equalsIgnoreCase(fileType) || "file".equalsIgnoreCase(fileType))
    			    ? String.valueOf(Math.min(20, qTypeConfig.getMaxQuestions()))
    			    : String.valueOf(qTypeConfig.getMaxQuestions());
    		
    		if (!imageInserted && chunk != null && !Strings.isNullOrEmpty(chunk.getImagePath()) ) {
    			imagebased = ("kb".equalsIgnoreCase(fileType) || "file".equalsIgnoreCase(fileType))
			            ? "from the given image": ""; 
    		}
    	    
    		
    		text = text
    				.replaceAll("\\{Imagebased}", imagebased)
    			    .replaceAll("\\{quesTypeConfig}", String.valueOf(qTypeConfig.getResponseFormat()))
    			    .replaceAll("\\{MaxQuestions}", maxQuestions)
    			    .replaceAll("\\{QuestionType}", QuestionType)
    			    .replaceAll("\\{MaxOptions}", String.valueOf(qTypeConfig.getMaxOptions()));

    	
    		// User Directives
//    		if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
//    			text = text.replace("{User Directives}", "**User Directives:** " + inputBean.getDirectives().toJSONString());
//    		} else {
//    			text = text.replace("{User Directives}", "");
//    		}
    		JSONObject directives = inputBean.getDirectives();

    		if (directives != null && !directives.isEmpty()) {

    		    // Special keys
    		    String retained = (String) directives.get("QuestionsToBeRetained");
    		    String discarded = (String) directives.get("QuestionsToBeDiscarded");

    		    // Prepare prompts using ternary operators
    		    String retainedPrompt = (retained != null) ? "\n Following Questions already exist. Do not generate similar questions : " + retained : "";
    		    String discardedPrompt = (discarded != null) ? "\n User did not like following questions : " + discarded : "";

    		    // Create a copy for general directives
    		    JSONObject filteredDirectives = new JSONObject();
    		    filteredDirectives.putAll(directives);

    		    // Remove special keys so they don't appear again
    		    filteredDirectives.remove("QuestionsToBeRetained");
    		    filteredDirectives.remove("QuestionsToBeDiscarded");

    		    String generalDirectives = (!filteredDirectives.isEmpty())
    		            ? "**User Directives:** " + filteredDirectives.toJSONString() : "";

    		    // Final replacement
    		    text = text.replace("{User Directives}", generalDirectives + retainedPrompt + discardedPrompt);

    		} else {
    		    text = text.replace("{User Directives}", "");
    		}
    		
    	
    		// Handle both {EnableHint} and {HintDescription} together
    		if (qTypeConfig.isEnableHint()) {
    		    text = text.replace("{EnableHint}", "**Enable Hint for each question:** Yes");
    		    text = text.replace("{HintDescription}",
                    "10. The hint must help the student **eliminate wrong choices or think critically**, but should **not paraphrase, echo, "
                    + "or closely resemble the correct answer**. Use logic, indirect context, and common scenarios as hint to guide learners - "
                    + "not keyword clues."
    		    );
    		} else {
    		    text = text.replace("{EnableHint}", "");
    		    text = text.replace("{HintDescription}", "");
    		}

    		// Know More
    		if (qTypeConfig.isEnableKnowMore()) {
    			text = text.replace("{EnableKnowMore}",  "**Enable Know More for each question:** Yes");
    		    text = text.replace("{KnowMoreDescription}",
    		        "11. The 'Know More' section must give a detailed conceptual justification explaining **why the "
    		        + "correct answer is right and why the other options are incorrect**"
    		    );
    		} else {
    		    text = text.replace("{EnableKnowMore}", "");
    		    text = text.replace("{KnowMoreDescription}", "");
    			
    		}
    		
    	
    		// User instruction
    		if (!Strings.isNullOrEmpty(inputBean.getUserInput())) {
    			text = text.replace("{UserInstruction}", "User instruction: " + inputBean.getUserInput());
    		} else {
    			text = text.replace("{UserInstruction}", "");
    		}
    	
    		// Input Context
    		if (chunk != null && Strings.isNullOrEmpty(chunk.getImagePath()) && !Strings.isNullOrEmpty(chunk.getText())) {
    			text = text.replace("{Input Context}", "Input Context: " + chunk.getText());
    		} else {
    			text = text.replace("{Input Context}", "");
    		}
    	
    		return text;
    	}
    	

    	
    	
    	
    	private JSONObject buildImageContent(String base64Image) {
    	    JSONObject imageContent = new JSONObject();
    	    imageContent.put("type", "image_url");
    	    
    	    JSONObject imageUrl = new JSONObject();
    	    imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
    	    
    	    imageContent.put("image_url", imageUrl);
    	    return imageContent;
    	}

   	
    	private static String encodeImageToBase64(String imagePath) throws IOException {
            byte[] fileContent = Files.readAllBytes(new File(imagePath).toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        }
    	
    	
    	
    	/*@SuppressWarnings("unchecked")
    	private static JSONArray prepareAzurePromptWithImage(List<String> base64Images, String system_message, String user_message) {
            logger.info("Creating prompt... ");
            JSONArray messages = new JSONArray();

            // System message
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            JSONArray systemContent = new JSONArray();

            JSONObject systemTextObj = new JSONObject();
            systemTextObj.put("type", "text");
            systemTextObj.put("text", system_message);
            systemContent.add(systemTextObj);

            systemMessage.put("content", systemContent);
            messages.add(systemMessage);

            // User message
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            JSONArray userContent = new JSONArray();

            for (String base64Img : base64Images) {
                JSONObject newlineText = new JSONObject();
                newlineText.put("type", "text");
                newlineText.put("text", "\n");
                userContent.add(newlineText);

                JSONObject imageUrl = new JSONObject();
                imageUrl.put("url", "data:image/jpeg;base64," + base64Img);

                JSONObject imageContent = new JSONObject();
                imageContent.put("type", "image_url");
                imageContent.put("image_url", imageUrl);
                userContent.add(imageContent);
            }

            JSONObject finalNewline = new JSONObject();
            finalNewline.put("type", "text");
            finalNewline.put("text", "\n");
            userContent.add(finalNewline);

            JSONObject instruction = new JSONObject();
            instruction.put("type", "text");
            instruction.put("text", user_message);
            userContent.add(instruction);

            userMessage.put("content", userContent);
            messages.add(userMessage);

            return messages;
        }
    	
    	@SuppressWarnings("unchecked")
    	private static JSONObject createContentMap(String text) {
            JSONObject textObject = new JSONObject();
            textObject.put("type", "text");
            textObject.put("text", text);
            return textObject;
        }
*/    	
    	
    	public JSONObject callGenAIUtility(final Map<String, HashMap<String, String>> vendorDetailsMap,final long orgId, final long appId,final int apiId,String questionType){
    		logger.info("[[FLAG]] Sending request to GenAI Framework!!");
    		logger.info(" vendor details recieved for Question gen{} " + vendorDetailsMap);
    		PayloadGeneratorService pgs = new PayloadGeneratorService(questionType);
    		long vendorserviceid = fetchServiceIdFromVendorCredMap(vendorDetailsMap);
    		String vendorName = vendorDetailsMap.keySet().iterator().next();
    		parameterMap.put(GenAIFrameworkConstants.SERVICE_ID, String.valueOf(vendorserviceid));
        	parameterMap.put("serviceName", vendorName);
        	logger.info("[[FLAG]] The Parameter Map looks like: " + parameterMap.toString());
    		
    		JSONObject payloadFromGenAi = (JSONObject) pgs.generatePayload(Long.valueOf(0),parameterMap , apiId, orgId, appId);
    		logger.info("[[FLAG]] Payload received from GenAI Framework for Question Generation {} : " + payloadFromGenAi.toString());
    		return payloadFromGenAi;
    	}
    	
    	public long fetchServiceIdFromVendorCredMap(Map<String, HashMap<String, String>> vendorCredMap){
    	    long vendorserviceid = -1 ;
    	    // Fetching the serviceId using an iterator
    	    Iterator<Entry<String, HashMap<String, String>>> iterator =  vendorCredMap.entrySet().iterator();
    	 
    	    while (iterator.hasNext()) {
    	        Entry<String, HashMap<String, String>> entry = iterator.next();
    	        Map<String,String> innerMap = entry.getValue();
//    	            logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "innermap: " + innerMap);
    	 
    	        // Checking if the "serviceid" key exists in the inner map
    	        if (innerMap.containsKey(GenAIFrameworkConstants.SERVICE_ID)) {
    	            String serviceId = innerMap.get(GenAIFrameworkConstants.SERVICE_ID);
    	            vendorserviceid = Long.parseLong(serviceId);
    	            logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + "serviceId: " + serviceId);
    	        }else{
    	            logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "inner map doesn't  contains serviceid key : ");            
    	        }
    	}
    	    return vendorserviceid ;
    	}
    	

    	
    	
    	
    	

//    	@SuppressWarnings("unchecked")
//    	public JSONArray getMCQPrompt(InputBean inputBean, Chunk chunk) throws IOException {
//    	    JSONArray prompt_messages = new JSONArray();
//    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("mcq");
//    	    
//    	    logger.error("vendorDetailsMap {} "+ inputBean.getAIvendorCredNodeMap());
//    	    logger.error("orgid " + inputBean.getOrgId());
//    	    logger.error("appid "+ inputBean.getAppId());
//    	    logger.error("apiid "+ inputBean.getApiID());
//    	    
//    	    
//    	    // -----Prompt Builder Implement ----//
//    	    try {  
//            	JSONObject payloadFromGenAI = callGenAIUtility(inputBean.getAIvendorCredNodeMap(),Long.parseLong(inputBean.getOrgId()),Long.parseLong(inputBean.getAppId()),Integer.parseInt(inputBean.getApiID()),"mcq");
//            	String jsonString = payloadFromGenAI.toString();
//                ObjectMapper objectMapper = new ObjectMapper();  
//                JsonNode jsonNode = objectMapper.readTree(jsonString);  
//                
//                logger.error("The prompt recieved : "+jsonNode.toString());
//                  
//                // Accessing specific fields in the JSON object  
//                String statusCode = jsonNode.get("statuscode").asText();  
//                JsonNode payloadNode = jsonNode.get("payload");  
//            
//                
//                String userInput = jsonNode.get("payload").get("messages").get(4).get("content").get(0).get("text").asText();
//                  
//                logger.error("Status Code: " + statusCode);  
//                logger.error("User Input: "+ userInput);
//                
////                String jobDescription = userInput  
////                        .replace("{jobRole}", jobRole)  
////                        .replace("{qualification}", qualification)  
////                        .replace("{experience}", experience)  
////                        .replace("{industry}", industry);  
////          
////            
////                
////                ((ObjectNode) payloadNode.get("messages").get(4).get("content").get(0)).put("text",jobDescription);
////                
////                String payloadMessage = objectMapper.writeValueAsString(payloadNode);
////                
////                logger.error("Message is "+ payloadMessage);
////        		logger.error("[[FLAG]] Payload received from GenAI Framework for Job Description after placeholder {} : " + payloadFromGenAI.toString());
////                
//    //
////                JSONParser parser = new JSONParser();
////                JSONObject jobj = (JSONObject)parser.parse(payloadMessage);
////                
////                logger.error("after parsing "+ jobj);
////                messages = (JSONArray) jobj.get("messages"); ;
////                
////                
////                
////                logger.error(messages.size());
//        		
//                
//                
//                
//            } catch (Exception e) {  
//                e.printStackTrace();  
//            }
//    	    
//    	    
//    	    
//    	 // -----Prompt Builder Implement ----//
//    	    
//    	    
//    	    
//    	    
//    	     
//    	    
//    	    logger.error("General prompt calling as before");
//    	    
//
//    	    // ===== Build System Persona Prompt =====
//    	    String system_message =
//    	        "You are an AI authoring assistant for education domain, designed to generate structured question-answer pairs. " +
//    	        "If images are provided, generate questions ONLY from the images. " +
//    	        "If no images but context is provided, generate questions ONLY from that context. " +
//    	        "If neither images nor context are provided, generate questions from user instructions and directives only. " +
//    	        "Ensure the questions are relevant to the directive and vary in complexity as appropriate. " +
//    	        "Responses should be concise yet detailed enough to fully address the question. " +
//    	        "\n\n**Instructions**\n1. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_1 +
//    	        "\n2. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_2 +
//    	        "\n3. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_3 +
//    	        "\n4. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_4 +
//    	        "\n5. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_5 +
//    	        "\n\n**You must return valid JSON that conforms to the schema provided by the system.** "+ 
//    	        "\nDo not include any extra text, explanations, or commentary outside the JSON." +
//    	        "\n JSON format: "+ qTypeConfig.getResponseFormat();
//    	    
////    	    if(inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT"))
////    	    	system_message += "\n JSON format: " + qTypeConfig.getResponseFormat();
//
//    	    // ===== Build User Prompt =====
//    	    StringBuilder userPromptBuilder = new StringBuilder();
//    	    userPromptBuilder.append("Generate ").append(qTypeConfig.getMaxQuestions()).append(" MCQ type questions and answers. ");
//    	    userPromptBuilder.append("Generated questions should adhere to Bloom's Taxonomy levels:\n")
//    	        .append(" - **Easy** - Recall/Remember type questions using 1-2 direct concepts.\n")
//    	        .append(" - **Medium** - Understand type questions using 2 or more combined concepts.\n")
//    	        .append(" - **Hard** - Create/Evaluate type questions applying 2 or more concepts to real-life scenarios.\n\n");
//
//    	    userPromptBuilder.append("**Strict Instructions (Must NOT be violated):**\n")
//    	        .append("1. DO NOT generate overlapping, ambiguous, or multiple correct answer options.\n")
//    	        .append("2. DO NOT use 'giveaway' correct options.\n")
//    	        .append("3. All distractors must be conceptually valid within the same concept.\n")
//    	        .append("4. DO NOT introduce calculation errors in numerical-type questions.\n")
//    	        .append("5. Response must be plain text only (no LaTeX, no backslashes).\n")
//    	        .append("6. Each MCQ must have ").append(qTypeConfig.getMaxOptions()).append(" comma-separated choices.\n")
//    	        .append("7. Only one choice is correct.\n")
//    	        .append("8. Output must include ONLY the formatted Q&A pairs (no commentary).\n")
//    	        .append("9. Strictly follow directives at all times.\n");
//
//    	    if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
//    	        userPromptBuilder.append("**User Directives:** ").append(inputBean.getDirectives()).append("\n");
//    	    }
//    	    if (qTypeConfig.isEnableHint()) {
//    	        userPromptBuilder.append("**Enable Hint for each question:** Yes\n");
//    	    }
//    	    if (qTypeConfig.isEnableKnowMore()) {
//    	        userPromptBuilder.append("**Enable Know More for each question:** Yes\n");
//    	    }
//    	    if (!Strings.isNullOrEmpty(inputBean.getUserInput())) {
//    	        userPromptBuilder.append("User instruction: ").append(inputBean.getUserInput()).append("\n");
//    	    }
//    	    final String user_message = userPromptBuilder.toString();
//    	    
//    	    // ===== If Image Provided =====
//    	    if (chunk!=null && !Strings.isNullOrEmpty(chunk.getImagePath())) {
//    	        List<String> base64Images = new ArrayList<>();
//    	        base64Images.add(encodeImageToBase64(chunk.getImagePath()));
//    	        JSONArray prompt_messages_with_image = prepareAzurePromptWithImage(base64Images, system_message, user_message);
//    	        logger.error("Final prompt (image-based): " + prompt_messages_with_image);
//    	        return prompt_messages_with_image;
//    	    }
//    	    else if (chunk!=null && !Strings.isNullOrEmpty(chunk.getText())) {
//    	        userPromptBuilder.append("Input Context: ").append(chunk.getText()).append("\n");
//    	    }
//
//    	    // ===== Otherwise, use normal text prompt =====
//    	    JSONArray contentArray = new JSONArray();
//    	    Map<String, Object> systemMessage = new HashMap<>();
//    	    Map<String, Object> userMessageActualPrompt = new HashMap<>();
//
//    	    // System message
//    	    contentArray.add(createContentMap(system_message));
//    	    systemMessage.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.SYSTEM);
//    	    systemMessage.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(systemMessage);
//
//    	    // User message
//    	    contentArray = new JSONArray();
//    	    contentArray.add(createContentMap(user_message));
//    	    userMessageActualPrompt.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.USER);
//    	    userMessageActualPrompt.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(userMessageActualPrompt);
//
//    	    logger.error("Final prompt (text-based): " + prompt_messages);
//    	    return prompt_messages;
//    	}
    	

    	
//    	@SuppressWarnings("unchecked")
//    	public JSONArray getMSQPrompt(InputBean inputBean, Chunk chunk) throws IOException {
//    	    JSONArray prompt_messages = new JSONArray();
//    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("msq");
//
//    	    // ===== Build System Persona Prompt =====
//    	    String system_message =
//    	        "You are an AI authoring assistant for education domain, designed to generate structured question-answer pairs. " +
//    	        "If images are provided, generate questions ONLY from the images. " +
//    	        "If no images but context is provided, generate questions ONLY from that context. " +
//    	        "If neither images nor context are provided, generate questions from user instructions and directives only. " +
//    	        "Ensure the questions are relevant to the directive and vary in complexity as appropriate. " +
//    	        "Responses should be concise yet detailed enough to fully address the question. " +
//    	        "\n\n**Instructions**\n1. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_1 +
//    	        "\n2. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_2 +
//    	        "\n3. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_3 +
//    	        "\n4. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_4 +
//    	        "\n5. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_5 +
//    	        "\n\n**You must return valid JSON that conforms to the schema provided by the system.** " + 
//    	        "\nDo not include any extra text, explanations, or commentary outside the JSON." +
//    	        "\n JSON format: " + qTypeConfig.getResponseFormat();
//
//    	    if (inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT")) {
//    	        system_message += "\n JSON format: " + qTypeConfig.getResponseFormat();
//    	    }
//
//    	    // ===== Build User Prompt =====
//    	    StringBuilder userPromptBuilder = new StringBuilder();
//    	    userPromptBuilder.append("Generate ").append(qTypeConfig.getMaxQuestions()).append(" multi-select questions and answers. ");
//    	    userPromptBuilder.append("Generated questions should adhere to Bloom's Taxonomy levels:\n")
//    	        .append(" - **Easy** - Recall/Remember type questions using 1-2 direct concepts.\n")
//    	        .append(" - **Medium** - Understand type questions using 2 or more combined concepts.\n")
//    	        .append(" - **Hard** - Apply/Evaluate type questions requiring selection of multiple valid answers from combined concepts.\n\n");
//
//    	    userPromptBuilder.append("**Strict Instructions (Must NOT be violated):**\n")
//    	        .append("1. Each question MUST have more than one correct option.\n")
//    	        .append("2. DO NOT generate overlapping, ambiguous, or trick options.\n")
//    	        .append("3. Distractors must be conceptually valid within the same domain.\n")
//    	        .append("4. DO NOT introduce calculation errors in numerical-type questions.\n")
//    	        .append("5. Response must be plain text only (no LaTeX, no backslashes).\n")
//    	        .append("6. Each question must have ").append(qTypeConfig.getMaxOptions()).append(" comma-separated choices.\n")
//    	        .append("7. Clearly specify all correct answers in the output.\n")
//    	        .append("8. Output must include ONLY the formatted Q&A pairs (no commentary).\n")
//    	        .append("9. Strictly follow directives at all times.\n");
//
//    	    if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
//    	        userPromptBuilder.append("**User Directives:** ").append(inputBean.getDirectives()).append("\n");
//    	    }
//    	    if (qTypeConfig.isEnableHint()) {
//    	        userPromptBuilder.append("**Enable Hint for each question:** Yes\n");
//    	    }
//    	    if (qTypeConfig.isEnableKnowMore()) {
//    	        userPromptBuilder.append("**Enable Know More for each question:** Yes\n");
//    	    }
//    	    if (!Strings.isNullOrEmpty(inputBean.getUserInput())) {
//    	        userPromptBuilder.append("User instruction: ").append(inputBean.getUserInput()).append("\n");
//    	    }
//    	    final String user_message = userPromptBuilder.toString();
//
//    	    // ===== If Image Provided =====
//    	    if (chunk != null && !Strings.isNullOrEmpty(chunk.getImagePath()) &&
//    	            !inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT")) {
//    	        List<String> base64Images = new ArrayList<>();
//    	        base64Images.add(encodeImageToBase64(chunk.getImagePath()));
//    	        JSONArray prompt_messages_with_image = prepareAzurePromptWithImage(base64Images, system_message, user_message);
//    	        logger.error("Final prompt (image-based): " + prompt_messages_with_image);
//    	        return prompt_messages_with_image;
//    	    }
//    	    else if (chunk != null && !Strings.isNullOrEmpty(chunk.getText())) {
//    	        userPromptBuilder.append("Input Context: ").append(chunk.getText()).append("\n");
//    	    }
//
//    	    // ===== Otherwise, use normal text prompt =====
//    	    JSONArray contentArray = new JSONArray();
//    	    Map<String, Object> systemMessage = new HashMap<>();
//    	    Map<String, Object> userMessageActualPrompt = new HashMap<>();
//
//    	    // System message
//    	    contentArray.add(createContentMap(system_message));
//    	    systemMessage.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.SYSTEM);
//    	    systemMessage.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(systemMessage);
//
//    	    // User message
//    	    contentArray = new JSONArray();
//    	    contentArray.add(createContentMap(user_message));
//    	    userMessageActualPrompt.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.USER);
//    	    userMessageActualPrompt.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(userMessageActualPrompt);
//
//    	    logger.error("Final prompt (text-based): " + prompt_messages);
//    	    return prompt_messages;
//    	}

    	
    	
//    	@SuppressWarnings("unchecked")
//    	public JSONArray getShortAnswerPrompt(InputBean inputBean, Chunk chunk) throws IOException {
//    	    JSONArray prompt_messages = new JSONArray();
//    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("faq");
//
//    	    // ===== Build System Persona Prompt =====
//    	    String system_message =
//    	        "You are an AI authoring assistant for education domain, designed to generate structured question-answer pairs. " +
//    	        "If images are provided, generate questions ONLY from the images. " +
//    	        "If no images but context is provided, generate questions ONLY from that context. " +
//    	        "If neither images nor context are provided, generate questions from user instructions and directives only. " +
//    	        "Ensure the questions are relevant to the directive and vary in complexity as appropriate. " +
//    	        "Responses should be concise yet detailed enough to fully address the question. " +
//    	        "\n\n**Instructions**\n1. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_1 +
//    	        "\n2. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_2 +
//    	        "\n3. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_3 +
//    	        "\n4. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_4 +
//    	        "\n5. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_5 +
//    	        "\n\n**You must return valid JSON that conforms to the schema provided by the system.** " +
//    	        "\nDo not include any extra text, explanations, or commentary outside the JSON."+
//    	        "\n JSON format: "+ qTypeConfig.getResponseFormat();
//    	    
//    	    if(inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT"))
//    	    	system_message += "\n JSON format: " + qTypeConfig.getResponseFormat();
//    	    
//    	    // ===== Build User Prompt =====
//    	    StringBuilder userPromptBuilder = new StringBuilder();
//    	    userPromptBuilder.append("Generate ").append(qTypeConfig.getMaxQuestions())
//    	        .append(" short-answer type questions and answers. ");
//    	    userPromptBuilder.append("Generated questions should adhere to Bloom's Taxonomy levels:\n")
//    	        .append(" - **Easy** - Recall/Remember type questions using 1-2 direct concepts.\n")
//    	        .append(" - **Medium** - Understand type questions using 2 or more combined concepts.\n")
//    	        .append(" - **Hard** - Create/Evaluate type questions applying 2 or more concepts to real-life scenarios.\n\n");
//
//    	    userPromptBuilder.append("**Strict Instructions (Must NOT be violated):**\n")
//    	        .append("1. DO NOT introduce calculation errors in numerical-type questions.\n")
//    	        .append("2. Response must be plain text only (no LaTeX, no backslashes).\n")
//    	        .append("3. If any particular topic is specified then generate Q&A from that topic only.\n")
//    	        .append("4. Output must include ONLY the formatted Q&A pairs (no commentary).\n")
//    	        .append("5. Strictly follow directives at all times.\n");
//
//    	    if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
//    	        userPromptBuilder.append("**User Directives:** ").append(inputBean.getDirectives()).append("\n");
//    	    }
//    	    if (qTypeConfig.isEnableHint()) {
//    	        userPromptBuilder.append("**Enable Hint for each question:** Yes\n");
//    	    }
//    	    if (qTypeConfig.isEnableKnowMore()) {
//    	        userPromptBuilder.append("**Enable Know More for each question:** Yes\n");
//    	    }
//    	    if (!Strings.isNullOrEmpty(inputBean.getUserInput())) {
//    	        userPromptBuilder.append("User instruction: ").append(inputBean.getUserInput()).append("\n");
//    	    }
//
//    	    // ===== Handle Image or Context =====
//    	    if (chunk != null && !Strings.isNullOrEmpty(chunk.getImagePath()) &&
//    	    		!inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT")) {
//    	        // Image-based prompt
//    	        List<String> base64Images = new ArrayList<>();
//    	        base64Images.add(encodeImageToBase64(chunk.getImagePath()));
//    	        JSONArray prompt_messages_with_image = prepareAzurePromptWithImage(base64Images, system_message, userPromptBuilder.toString());
//    	        logger.error("Final prompt (image-based short-answer): " + prompt_messages_with_image);
//    	        return prompt_messages_with_image;
//    	    } else if (chunk != null && !Strings.isNullOrEmpty(chunk.getText())) {
//    	        // Add context if present
//    	        userPromptBuilder.append("Input Context: ").append(chunk.getText()).append("\n");
//    	    }
//
//    	    final String user_message = userPromptBuilder.toString();
//
//    	    // ===== Otherwise, use normal text prompt =====
//    	    JSONArray contentArray = new JSONArray();
//    	    Map<String, Object> systemMessage = new HashMap<>();
//    	    Map<String, Object> userMessageActualPrompt = new HashMap<>();
//
//    	    // System message
//    	    contentArray.add(createContentMap(system_message));
//    	    systemMessage.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.SYSTEM);
//    	    systemMessage.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(systemMessage);
//
//    	    // User message
//    	    contentArray = new JSONArray();
//    	    contentArray.add(createContentMap(user_message));
//    	    userMessageActualPrompt.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.USER);
//    	    userMessageActualPrompt.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(userMessageActualPrompt);
//
//    	    logger.error("Final prompt (text-based short-answer): " + prompt_messages);
//    	    return prompt_messages;
//    	}
//
    	
//    	@SuppressWarnings("unchecked")
//    	public JSONArray getTrueFalsePrompt(InputBean inputBean, Chunk chunk) throws IOException {
//    	    JSONArray prompt_messages = new JSONArray();
//    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("truefalse");
//
//    	    // ===== Build System Persona Prompt =====
//    	    String system_message =
//    	        "You are an AI authoring assistant for education domain, designed to generate structured question-answer pairs. " +
//    	        "If images are provided, generate questions ONLY from the images. " +
//    	        "If no images but context is provided, generate questions ONLY from that context. " +
//    	        "If neither images nor context are provided, generate questions from user instructions and directives only. " +
//    	        "Ensure the questions are relevant to the directive and vary in complexity as appropriate. " +
//    	        "Responses should be concise yet detailed enough to fully address the question. " +
//    	        "\n\n**Instructions**\n1. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_1 +
//    	        "\n2. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_2 +
//    	        "\n3. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_3 +
//    	        "\n4. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_4 +
//    	        "\n5. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_5 +
//    	        "\n\n**You must return valid JSON that conforms to the schema provided by the system.** " +
//    	        "\nDo not include any extra text, explanations, or commentary outside the JSON.";
//    	    
//    	    if(inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT"))
//    	    	system_message += "\n JSON format: " + qTypeConfig.getResponseFormat();
//
//    	    // ===== Build User Prompt =====
//    	    StringBuilder userPromptBuilder = new StringBuilder();
//    	    userPromptBuilder.append("Generate ").append(qTypeConfig.getMaxQuestions())
//    	        .append(" True-False type questions and answers. ");
//    	    userPromptBuilder.append("Generated questions should adhere to Bloom's Taxonomy levels:\n")
//    	        .append(" - **Easy** - Recall/Remember type questions using 1-2 direct concepts.\n")
//    	        .append(" - **Medium** - Understand type questions using 2 or more combined concepts.\n")
//    	        .append(" - **Hard** - Create/Evaluate type questions applying 2 or more concepts to real-life scenarios.\n\n");
//
//    	    userPromptBuilder.append("**Strict Instructions (Must NOT be violated):**\n")
//    	        .append("1. DO NOT introduce calculation errors in numerical-type questions.\n")
//    	        .append("2. Response must be plain text only (no LaTeX, no backslashes).\n")
//    	        .append("3. If any particular topic is specified then generate Q&A from that topic only.\n")
//    	        .append("4. Output must include ONLY the formatted Q&A pairs (no commentary).\n")
//    	        .append("5. Strictly follow directives at all times.\n");
//
//    	    if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
//    	        userPromptBuilder.append("**User Directives:** ").append(inputBean.getDirectives()).append("\n");
//    	    }
//    	    if (qTypeConfig.isEnableHint()) {
//    	        userPromptBuilder.append("**Enable Hint for each question:** Yes\n");
//    	    }
//    	    if (qTypeConfig.isEnableKnowMore()) {
//    	        userPromptBuilder.append("**Enable Know More for each question:** Yes\n");
//    	    }
//    	    if (!Strings.isNullOrEmpty(inputBean.getUserInput())) {
//    	        userPromptBuilder.append("User instruction: ").append(inputBean.getUserInput()).append("\n");
//    	    }
//
//    	    // ===== Handle Image or Context =====
//    	    if (chunk != null && !Strings.isNullOrEmpty(chunk.getImagePath()) &&
//    	    		!inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT")) {
//    	        // Image-based prompt
//    	        List<String> base64Images = new ArrayList<>();
//    	        base64Images.add(encodeImageToBase64(chunk.getImagePath()));
//    	        JSONArray prompt_messages_with_image = prepareAzurePromptWithImage(base64Images, system_message, userPromptBuilder.toString());
//    	        logger.error("Final prompt (image-based truefalse): " + prompt_messages_with_image);
//    	        return prompt_messages_with_image;
//    	    } else if (chunk != null && !Strings.isNullOrEmpty(chunk.getText())) {
//    	        // Add context if present
//    	        userPromptBuilder.append("Input Context: ").append(chunk.getText()).append("\n");
//    	    }
//
//    	    final String user_message = userPromptBuilder.toString();
//
//    	    // ===== Otherwise, use normal text prompt =====
//    	    JSONArray contentArray = new JSONArray();
//    	    Map<String, Object> systemMessage = new HashMap<>();
//    	    Map<String, Object> userMessageActualPrompt = new HashMap<>();
//
//    	    // System message
//    	    contentArray.add(createContentMap(system_message));
//    	    systemMessage.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.SYSTEM);
//    	    systemMessage.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(systemMessage);
//
//    	    // User message
//    	    contentArray = new JSONArray();
//    	    contentArray.add(createContentMap(user_message));
//    	    userMessageActualPrompt.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.USER);
//    	    userMessageActualPrompt.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(userMessageActualPrompt);
//
//    	    logger.error("Final prompt (text-based truefalse): " + prompt_messages);
//    	    return prompt_messages;
//    	}
    	

    	
//    	@SuppressWarnings("unchecked")
//    	public JSONArray getFillInTheBlanksPrompt(InputBean inputBean, Chunk chunk) throws IOException {
//    	    JSONArray prompt_messages = new JSONArray();
//    	    QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("fillblanks");
//
//    	    // ===== Build System Persona Prompt =====
//    	    String system_message =
//    	        "You are an AI authoring assistant for education domain, designed to generate structured question-answer pairs. " +
//    	        "If images are provided, generate questions ONLY from the images. " +
//    	        "If no images but context is provided, generate questions ONLY from that context. " +
//    	        "If neither images nor context are provided, generate questions from user instructions and directives only. " +
//    	        "Ensure the questions are relevant to the directive and vary in complexity as appropriate. " +
//    	        "Responses should be concise yet detailed enough to fully address the question. " +
//    	        "\n\n**Instructions**\n1. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_1 +
//    	        "\n2. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_2 +
//    	        "\n3. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_3 +
//    	        "\n4. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_4 +
//    	        "\n5. " + PromptConstants.RESPONSE_INSTRUCTION_PROMPT_5 +
//    	        "\n\n**You must return valid JSON that conforms to the schema provided by the system.** " + 
//    	        "\nDo not include any extra text, explanations, or commentary outside the JSON." +
//    	        "\n JSON format: " + qTypeConfig.getResponseFormat();
//
//    	    if (inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT")) {
//    	        system_message += "\n JSON format: " + qTypeConfig.getResponseFormat();
//    	    }
//
//    	    // ===== Build User Prompt =====
//    	    StringBuilder userPromptBuilder = new StringBuilder();
//    	    userPromptBuilder.append("Generate ").append(qTypeConfig.getMaxQuestions()).append(" fill-in-the-blank questions and answers. ");
//    	    userPromptBuilder.append("Generated questions should adhere to Bloom's Taxonomy levels:\n")
//    	        .append(" - **Easy** - Recall/Remember type blanks using 1-2 direct concepts.\n")
//    	        .append(" - **Medium** - Understand type blanks requiring conceptual fill-ins.\n")
//    	        .append(" - **Hard** - Apply/Evaluate type blanks where learners must infer correct terms from context.\n\n");
//
//    	    userPromptBuilder.append("**Strict Instructions (Must NOT be violated):**\n")
//    	        .append("1. Each question MUST contain one or more blanks (use ____ to indicate blank).\n")
//    	        .append("2. Provide correct answer(s) for each blank explicitly.\n")
//    	        .append("3. DO NOT generate ambiguous or multiple possible answers unless explicitly instructed.\n")
//    	        .append("4. DO NOT introduce spelling or factual errors.\n")
//    	        .append("5. Response must be plain text only (no LaTeX, no backslashes).\n")
//    	        .append("6. Output must include ONLY the formatted Q&A pairs (no commentary).\n")
//    	        .append("7. Strictly follow directives at all times.\n");
//
//    	    if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
//    	        userPromptBuilder.append("**User Directives:** ").append(inputBean.getDirectives()).append("\n");
//    	    }
//    	    if (qTypeConfig.isEnableHint()) {
//    	        userPromptBuilder.append("**Enable Hint for each question:** Yes\n");
//    	    }
//    	    if (qTypeConfig.isEnableKnowMore()) {
//    	        userPromptBuilder.append("**Enable Know More for each question:** Yes\n");
//    	    }
//    	    if (!Strings.isNullOrEmpty(inputBean.getUserInput())) {
//    	        userPromptBuilder.append("User instruction: ").append(inputBean.getUserInput()).append("\n");
//    	    }
//    	    final String user_message = userPromptBuilder.toString();
//
//    	    // ===== If Image Provided =====
//    	    if (chunk != null && !Strings.isNullOrEmpty(chunk.getImagePath()) &&
//    	            !inputBean.getAIvendorCredNodeMap().containsKey("OPENAI_GPT")) {
//    	        List<String> base64Images = new ArrayList<>();
//    	        base64Images.add(encodeImageToBase64(chunk.getImagePath()));
//    	        JSONArray prompt_messages_with_image = prepareAzurePromptWithImage(base64Images, system_message, user_message);
//    	        logger.error("Final prompt (image-based): " + prompt_messages_with_image);
//    	        return prompt_messages_with_image;
//    	    }
//    	    else if (chunk != null && !Strings.isNullOrEmpty(chunk.getText())) {
//    	        userPromptBuilder.append("Input Context: ").append(chunk.getText()).append("\n");
//    	    }
//
//    	    // ===== Otherwise, use normal text prompt =====
//    	    JSONArray contentArray = new JSONArray();
//    	    Map<String, Object> systemMessage = new HashMap<>();
//    	    Map<String, Object> userMessageActualPrompt = new HashMap<>();
//
//    	    // System message
//    	    contentArray.add(createContentMap(system_message));
//    	    systemMessage.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.SYSTEM);
//    	    systemMessage.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(systemMessage);
//
//    	    // User message
//    	    contentArray = new JSONArray();
//    	    contentArray.add(createContentMap(user_message));
//    	    userMessageActualPrompt.put(MlFrameworkConstants.ROLE, MlFrameworkConstants.USER);
//    	    userMessageActualPrompt.put(MlFrameworkConstants.CONTENT, contentArray);
//    	    prompt_messages.add(userMessageActualPrompt);
//
//    	    logger.error("Final prompt (text-based): " + prompt_messages);
//    	    return prompt_messages;
//    	}

    	
 



}





















































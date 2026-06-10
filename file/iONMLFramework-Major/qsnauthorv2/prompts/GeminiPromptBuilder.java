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

public class GeminiPromptBuilder {
	private static final Log logger = LogFactory.getLog(GeminiPromptBuilder.class);
	Map<String, String> parameterMap = new HashMap<String, String>();

	public JSONArray getMCQPrompt(InputBean inputBean, Chunk chunk) throws IOException {
		logger.error("-----Inside getMCQPrompt----");
		QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("mcq");
		JSONArray promptMessages = new JSONArray();
		try {
			JSONObject payloadFromGenAI = callGenAIUtility(
				inputBean.getAIvendorCredNodeMap(),
				Long.parseLong(inputBean.getOrgId()),
				Long.parseLong(inputBean.getAppId()),
				Integer.parseInt(inputBean.getApiID()),
				"mcq"
			);
			logger.error("Payload received from GenAI framework: " + payloadFromGenAI);
			ObjectMapper objectMapper = new ObjectMapper();
			// Gemini: top-level "contents" key, no "payload" wrapper
			JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");
			JsonNode contentsNode = payloadNode.get("contents");
			logger.error("Extracted contents array: " + contentsNode);
			JSONParser parser = new JSONParser();
			JSONArray dbMessages = (JSONArray) parser.parse(contentsNode.toString());
			promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "MCQ");
			logger.error("Final prompt messages after replacement (MCQ): " + promptMessages);
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
		JSONArray promptMessages = new JSONArray();
		try {
			JSONObject payloadFromGenAI = callGenAIUtility(
				inputBean.getAIvendorCredNodeMap(),
				Long.parseLong(inputBean.getOrgId()),
				Long.parseLong(inputBean.getAppId()),
				Integer.parseInt(inputBean.getApiID()),
				"msq"
			);
			logger.error("Payload received from GenAI framework: " + payloadFromGenAI);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");
			JsonNode contentsNode = payloadNode.get("contents");
			logger.error("Extracted contents array: " + contentsNode);
			JSONParser parser = new JSONParser();
			JSONArray dbMessages = (JSONArray) parser.parse(contentsNode.toString());
			promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "multi-select");
			logger.error("Final prompt messages after replacement (MSQ): " + promptMessages);
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
		JSONArray promptMessages = new JSONArray();
		try {
			JSONObject payloadFromGenAI = callGenAIUtility(
				inputBean.getAIvendorCredNodeMap(),
				Long.parseLong(inputBean.getOrgId()),
				Long.parseLong(inputBean.getAppId()),
				Integer.parseInt(inputBean.getApiID()),
				"faq"
			);
			logger.error("Payload received from GenAI framework (FAQ): " + payloadFromGenAI);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");
			JsonNode contentsNode = payloadNode.get("contents");
			logger.error("Extracted contents array: " + contentsNode);
			JSONParser parser = new JSONParser();
			JSONArray dbMessages = (JSONArray) parser.parse(contentsNode.toString());
			promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "short-answer");
			logger.error("Final prompt messages after replacement (FAQ): " + promptMessages);
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
		JSONArray promptMessages = new JSONArray();
		try {
			JSONObject payloadFromGenAI = callGenAIUtility(
				inputBean.getAIvendorCredNodeMap(),
				Long.parseLong(inputBean.getOrgId()),
				Long.parseLong(inputBean.getAppId()),
				Integer.parseInt(inputBean.getApiID()),
				"truefalse"
			);
			logger.error("Payload received from GenAI framework (True/False): " + payloadFromGenAI);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");
			JsonNode contentsNode = payloadNode.get("contents");
			logger.error("Extracted contents array: " + contentsNode);
			JSONParser parser = new JSONParser();
			JSONArray dbMessages = (JSONArray) parser.parse(contentsNode.toString());
			promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "TrueFalse");
			logger.error("Final prompt messages after replacement (True/False): " + promptMessages);
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
		JSONArray promptMessages = new JSONArray();
		try {
			JSONObject payloadFromGenAI = callGenAIUtility(
				inputBean.getAIvendorCredNodeMap(),
				Long.parseLong(inputBean.getOrgId()),
				Long.parseLong(inputBean.getAppId()),
				Integer.parseInt(inputBean.getApiID()),
				"fillintheblanks"
			);
			logger.error("Payload received from GenAI framework: " + payloadFromGenAI);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");
			JsonNode contentsNode = payloadNode.get("contents");
			logger.error("Extracted contents array: " + contentsNode);
			JSONParser parser = new JSONParser();
			JSONArray dbMessages = (JSONArray) parser.parse(contentsNode.toString());
			promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "fill-in-the-blank");
			logger.error("Final prompt messages after replacement (fill-in-the-blanks): " + promptMessages);
		} catch (Exception e) {
			logger.error("Error while generating Fill in the Blanks prompt", e);
			throw new IOException("Failed to generate Fill in the Blanks prompt", e);
		}
		return promptMessages;
	}

	public JSONArray getComprehensionPrompt(InputBean inputBean, Chunk chunk) throws IOException {
		logger.error("-----Inside ComprehensionPrompt----");
		QuestionTypeConfig qTypeConfig = inputBean.getQuestionTypeConfigMap().get("comprehensionmcq");
		JSONArray promptMessages = new JSONArray();
		try {
			JSONObject payloadFromGenAI = callGenAIUtility(
				inputBean.getAIvendorCredNodeMap(),
				Long.parseLong(inputBean.getOrgId()),
				Long.parseLong(inputBean.getAppId()),
				Integer.parseInt(inputBean.getApiID()),
				"comprehension"
			);
			logger.error("Payload received from GenAI framework: " + payloadFromGenAI);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode payloadNode = objectMapper.readTree(payloadFromGenAI.toString()).get("payload");
			JsonNode contentsNode = payloadNode.get("contents");
			logger.error("Extracted contents array: " + contentsNode);
			JSONParser parser = new JSONParser();
			JSONArray dbMessages = (JSONArray) parser.parse(contentsNode.toString());
			promptMessages = preparePromptFromDBPayload(dbMessages, inputBean, chunk, qTypeConfig, "comprehension");
			logger.error("Final prompt messages after replacement (comprehension): " + promptMessages);
		} catch (Exception e) {
			logger.error("Error while generating comprehension prompt", e);
			throw new IOException("Failed to generate comprehension prompt", e);
		}
		return promptMessages;
	}

	@SuppressWarnings("unchecked")
	public JSONArray preparePromptFromDBPayload(JSONArray dbMessages, InputBean inputBean, Chunk chunk,
			QuestionTypeConfig qTypeConfig, String QuestionType) {
		JSONArray updatedMessages = new JSONArray();
		boolean imageInserted = false;

		for (int i = 0; i < dbMessages.size(); i++) {
			JSONObject message = (JSONObject) dbMessages.get(i);
			String role = (String) message.get("role");

			// Gemini uses "parts" key, not "content"
			JSONArray partsArray = (JSONArray) message.get("parts");

			JSONArray updatedPartsArray = new JSONArray();

			for (int j = 0; j < partsArray.size(); j++) {
				JSONObject partItem = (JSONObject) partsArray.get(j);

				// Gemini parts have only "text" key directly — no "type" key
				if (partItem.containsKey("text")) {
					String text = (String) partItem.get("text");

					// Replace placeholders
					text = replaceAllPlaceholders(text, inputBean, chunk, qTypeConfig, QuestionType);

					JSONObject updatedPart = new JSONObject();
					updatedPart.put("text", text);
					updatedPartsArray.add(updatedPart);

					// Inject image after first text block of first user message
					if (!imageInserted && "user".equals(role) &&
							chunk != null && !Strings.isNullOrEmpty(chunk.getImagePath())) {
						try {
							String base64Image = encodeImageToBase64(chunk.getImagePath());
							JSONObject imageBlock = buildImageContent(base64Image);
							updatedPartsArray.add(imageBlock);
							imageInserted = true;
						} catch (Exception e) {
							logger.error("Image encoding failed, skipping image injection", e);
						}
					}

				} else {
					// Keep non-text parts unchanged
					updatedPartsArray.add(partItem);
				}
			}

			// Rebuild message with "parts" key
			JSONObject updatedMessage = new JSONObject();
			updatedMessage.put("role", role);
			updatedMessage.put("parts", updatedPartsArray);
			updatedMessages.add(updatedMessage);
		}

		return updatedMessages;
	}

	private String replaceAllPlaceholders(String text, InputBean inputBean, Chunk chunk,
			QuestionTypeConfig qTypeConfig, String QuestionType) {
		logger.error("Inside replaceholders ------");
		logger.error("qTypeConfig is " + qTypeConfig.toString());

		String fileType = inputBean.getFileType();
		boolean imageInserted = false;
		String imagebased = "";

		String maxQuestions = ("kb".equalsIgnoreCase(fileType) || "file".equalsIgnoreCase(fileType))
				? String.valueOf(Math.min(20, qTypeConfig.getMaxQuestions()))
				: String.valueOf(qTypeConfig.getMaxQuestions());

		if (!imageInserted && chunk != null && !Strings.isNullOrEmpty(chunk.getImagePath())) {
			imagebased = ("kb".equalsIgnoreCase(fileType) || "file".equalsIgnoreCase(fileType))
					? "from the given image" : "";
		}

		text = text
				.replaceAll("\\{Imagebased}", imagebased)
				.replaceAll("\\{quesTypeConfig}", String.valueOf(qTypeConfig.getResponseFormat()))
				.replaceAll("\\{MaxQuestions}", maxQuestions)
				.replaceAll("\\{QuestionType}", QuestionType)
				.replaceAll("\\{MaxOptions}", String.valueOf(qTypeConfig.getMaxOptions()));

		JSONObject directives = inputBean.getDirectives();
		if (directives != null && !directives.isEmpty()) {
			String retained = (String) directives.get("QuestionsToBeRetained");
			String discarded = (String) directives.get("QuestionsToBeDiscarded");

			String retainedPrompt = (retained != null) ? "\n Following Questions already exist. Do not generate similar questions : " + retained : "";
			String discardedPrompt = (discarded != null) ? "\n User did not like following questions : " + discarded : "";

			JSONObject filteredDirectives = new JSONObject();
			filteredDirectives.putAll(directives);
			filteredDirectives.remove("QuestionsToBeRetained");
			filteredDirectives.remove("QuestionsToBeDiscarded");

			String generalDirectives = (!filteredDirectives.isEmpty())
					? "**User Directives:** " + filteredDirectives.toJSONString() : "";

			text = text.replace("{User Directives}", generalDirectives + retainedPrompt + discardedPrompt);
		} else {
			text = text.replace("{User Directives}", "");
		}

		if (qTypeConfig.isEnableHint()) {
			text = text.replace("{EnableHint}", "**Enable Hint for each question:** Yes");
			text = text.replace("{HintDescription}",
					"10. The hint must help the student **eliminate wrong choices or think critically**, but should **not paraphrase, echo, "
					+ "or closely resemble the correct answer**. Use logic, indirect context, and common scenarios as hint to guide learners - "
					+ "not keyword clues.");
		} else {
			text = text.replace("{EnableHint}", "");
			text = text.replace("{HintDescription}", "");
		}

		if (qTypeConfig.isEnableKnowMore()) {
			text = text.replace("{EnableKnowMore}", "**Enable Know More for each question:** Yes");
			text = text.replace("{KnowMoreDescription}",
					"11. The 'Know More' section must give a detailed conceptual justification explaining **why the "
					+ "correct answer is right and why the other options are incorrect**");
		} else {
			text = text.replace("{EnableKnowMore}", "");
			text = text.replace("{KnowMoreDescription}", "");
		}

		if (!Strings.isNullOrEmpty(inputBean.getUserInput())) {
			text = text.replace("{UserInstruction}", "User instruction: " + inputBean.getUserInput());
		} else {
			text = text.replace("{UserInstruction}", "");
		}

		if (chunk != null && Strings.isNullOrEmpty(chunk.getImagePath()) && !Strings.isNullOrEmpty(chunk.getText())) {
			text = text.replace("{Input Context}", "Input Context: " + chunk.getText());
		} else {
			text = text.replace("{Input Context}", "");
		}

		return text;
	}

	private JSONObject buildImageContent(String base64Image) {
		// Gemini inline image format inside parts
		JSONObject inlineData = new JSONObject();
		inlineData.put("mime_type", "image/jpeg");
		inlineData.put("data", base64Image);

		JSONObject imagePart = new JSONObject();
		imagePart.put("inline_data", inlineData);
		return imagePart;
	}

	private static String encodeImageToBase64(String imagePath) throws IOException {
		byte[] fileContent = Files.readAllBytes(new File(imagePath).toPath());
		return Base64.getEncoder().encodeToString(fileContent);
	}

	public JSONObject callGenAIUtility(final Map<String, HashMap<String, String>> vendorDetailsMap,
			final long orgId, final long appId, final int apiId, String questionType) {
		logger.error("[[FLAG]] Sending request to GenAI Framework!!");
		logger.error(" vendor details received for Question gen{} " + vendorDetailsMap);
		PayloadGeneratorService pgs = new PayloadGeneratorService(questionType);
		long vendorserviceid = fetchServiceIdFromVendorCredMap(vendorDetailsMap);
		String vendorName = vendorDetailsMap.keySet().iterator().next();
		parameterMap.put(GenAIFrameworkConstants.SERVICE_ID, String.valueOf(vendorserviceid));
		parameterMap.put("serviceName", vendorName);
		logger.error("[[FLAG]] The Parameter Map looks like: " + parameterMap.toString());
		JSONObject payloadFromGenAi = (JSONObject) pgs.generatePayload(Long.valueOf(0), parameterMap, apiId, orgId, appId);
		logger.error("[[FLAG]] Payload received from GenAI Framework: " + payloadFromGenAi.toString());
		return payloadFromGenAi;
	}

	public long fetchServiceIdFromVendorCredMap(Map<String, HashMap<String, String>> vendorCredMap) {
		long vendorserviceid = -1;
		Iterator<Entry<String, HashMap<String, String>>> iterator = vendorCredMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, HashMap<String, String>> entry = iterator.next();
			Map<String, String> innerMap = entry.getValue();
			if (innerMap.containsKey(GenAIFrameworkConstants.SERVICE_ID)) {
				String serviceId = innerMap.get(GenAIFrameworkConstants.SERVICE_ID);
				vendorserviceid = Long.parseLong(serviceId);
				logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "serviceId: " + serviceId);
			} else {
				logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "inner map doesn't contain serviceid key");
			}
		}
		return vendorserviceid;
	}
}

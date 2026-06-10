package com.tcsion.ml.qsnauthorv2.prompts;


import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;

public class GPTResponseFormatBuilder {

    public JSONObject buildResponseFormat(String questionType, QuestionTypeConfig qTypeConfig) {
        JSONObject responseFormat;

        switch (questionType.toLowerCase()) {
            case "mcq": responseFormat = buildMcqResponseFormat(qTypeConfig); break;
            case "truefalse": responseFormat = buildTrueFalseResponseFormat(qTypeConfig); break;
            case "faq": responseFormat = buildShortAnswerResponseFormat(qTypeConfig); break;
            case "msq": responseFormat = buildMsqResponseFormat(qTypeConfig); break;
            case "fillblanks": responseFormat = buildFillBlanksResponseFormat(qTypeConfig); break;
            case "comprehensionmcq" : responseFormat = buildComprehensionMcqResponseFormat(qTypeConfig); break;
            default: throw new IllegalArgumentException("Unsupported question type: " + questionType);
        }
        return responseFormat;
    }

    @SuppressWarnings("unchecked")
    private JSONObject buildMcqResponseFormat(QuestionTypeConfig qTypeConfig) {
        JSONObject properties = new JSONObject();

        JSONObject question = new JSONObject();
        question.put("type", "string");
        properties.put("question", question);

        JSONObject options = new JSONObject();
        options.put("type", "array");
        JSONObject choiceItem = new JSONObject();
        choiceItem.put("type", "string");
        options.put("items", choiceItem);
        options.put("numOptions", qTypeConfig.getMaxOptions());
        properties.put("options", options);

        JSONObject answer = new JSONObject();
        answer.put("type", "string");
        answer.put("description", "Should exactly match with one of the options");
        properties.put("answer", answer);

        if (qTypeConfig.isEnableHint()) {
            JSONObject hint = new JSONObject();
            hint.put("type", "string");
            properties.put("hint", hint);
        }
        if (qTypeConfig.isEnableKnowMore()) {
            JSONObject knowMore = new JSONObject();
            knowMore.put("type", "string");
            properties.put("knowMore", knowMore);
        }

        List<String> requiredFields = new ArrayList<>();
        requiredFields.add("question");
        requiredFields.add("options");
        requiredFields.add("answer");
        if (qTypeConfig.isEnableHint()) requiredFields.add("hint");
        if (qTypeConfig.isEnableKnowMore()) requiredFields.add("knowMore");

        return wrapAsResponseFormat("mcq_generation", properties, requiredFields);
    }
    
    @SuppressWarnings("unchecked")
	private JSONObject buildMsqResponseFormat(QuestionTypeConfig qTypeConfig) {
        JSONObject properties = new JSONObject();

        // Question text
        JSONObject question = new JSONObject();
        question.put("type", "string");
        properties.put("question", question);

        // Options array
        JSONObject options = new JSONObject();
        options.put("type", "array");
        JSONObject choiceItem = new JSONObject();
        choiceItem.put("type", "string");
        options.put("items", choiceItem);
        options.put("numOptions", qTypeConfig.getMaxOptions());
        properties.put("options", options);

        // Multiple correct answers (array of strings)
        JSONObject answer = new JSONObject();
        answer.put("type", "array");
        JSONObject answerItem = new JSONObject();
        answerItem.put("type", "string");
        answer.put("items", answerItem);
        properties.put("answer", answer);

        // Optional fields
        if (qTypeConfig.isEnableHint()) {
            JSONObject hint = new JSONObject();
            hint.put("type", "string");
            properties.put("hint", hint);
        }
        if (qTypeConfig.isEnableKnowMore()) {
            JSONObject knowMore = new JSONObject();
            knowMore.put("type", "string");
            properties.put("knowMore", knowMore);
        }

        // Required fields
        List<String> requiredFields = new ArrayList<>();
        requiredFields.add("question");
        requiredFields.add("options");
        requiredFields.add("answer"); // note plural here
        if (qTypeConfig.isEnableHint()) requiredFields.add("hint");
        if (qTypeConfig.isEnableKnowMore()) requiredFields.add("knowMore");

        return wrapAsResponseFormat("msq_generation", properties, requiredFields);
    }

    
    @SuppressWarnings("unchecked")
    private static JSONObject buildTrueFalseResponseFormat(QuestionTypeConfig qTypeConfig) {
        JSONObject properties = new JSONObject();

        JSONObject question = new JSONObject();
        question.put("type", "string");
        properties.put("question", question);

        JSONObject answer = new JSONObject();
        answer.put("type", "string");
        JSONArray enums = new JSONArray();
        enums.add("True");
        enums.add("False");
        answer.put("enum", enums);
        properties.put("answer", answer);

        if (qTypeConfig.isEnableHint()) {
            JSONObject hint = new JSONObject();
            hint.put("type", "string");
            properties.put("hint", hint);
        }
        if (qTypeConfig.isEnableKnowMore()) {
            JSONObject knowMore = new JSONObject();
            knowMore.put("type", "string");
            properties.put("knowMore", knowMore);
        }

        List<String> requiredFields = new ArrayList<>();
        requiredFields.add("question");
        requiredFields.add("answer");
        if (qTypeConfig.isEnableHint()) requiredFields.add("hint");
        if (qTypeConfig.isEnableKnowMore()) requiredFields.add("knowMore");

        return wrapAsResponseFormat("true_false_generation", properties, requiredFields);
    }
    
    @SuppressWarnings("unchecked")
    private static JSONObject buildShortAnswerResponseFormat(QuestionTypeConfig qTypeConfig) {
        JSONObject properties = new JSONObject();

        JSONObject question = new JSONObject();
        question.put("type", "string");
        properties.put("question", question);

        JSONObject answer = new JSONObject();
        answer.put("type", "string");
        properties.put("answer", answer);

        if (qTypeConfig.isEnableHint()) {
            JSONObject hint = new JSONObject();
            hint.put("type", "string");
            properties.put("hint", hint);
        }
        if (qTypeConfig.isEnableKnowMore()) {
            JSONObject knowMore = new JSONObject();
            knowMore.put("type", "string");
            properties.put("knowMore", knowMore);
        }

        List<String> requiredFields = new ArrayList<>();
        requiredFields.add("question");
        requiredFields.add("answer");
        if (qTypeConfig.isEnableHint()) requiredFields.add("hint");
        if (qTypeConfig.isEnableKnowMore()) requiredFields.add("knowMore");

        return wrapAsResponseFormat("short_answer_generation", properties, requiredFields);
    }
    
    @SuppressWarnings("unchecked")
	private JSONObject buildFillBlanksResponseFormat(QuestionTypeConfig qTypeConfig) {
        JSONObject properties = new JSONObject();

        // Question text with blanks
        JSONObject question = new JSONObject();
        question.put("type", "string");
        properties.put("question", question);

        // Answer(s) for the blank(s)
        JSONObject answer = new JSONObject();
        answer.put("type", "string");
        
        properties.put("answer", answer);

        // Optional fields
        if (qTypeConfig.isEnableHint()) {
            JSONObject hint = new JSONObject();
            hint.put("type", "string");
            properties.put("hint", hint);
        }
        if (qTypeConfig.isEnableKnowMore()) {
            JSONObject knowMore = new JSONObject();
            knowMore.put("type", "string");
            properties.put("knowMore", knowMore);
        }

        // Required fields
        List<String> requiredFields = new ArrayList<>();
        requiredFields.add("question");
        requiredFields.add("answer");
        if (qTypeConfig.isEnableHint()) requiredFields.add("hint");
        if (qTypeConfig.isEnableKnowMore()) requiredFields.add("knowMore");

        return wrapAsResponseFormat("fib_generation", properties, requiredFields);
    }

    @SuppressWarnings("unchecked")
    private JSONObject buildComprehensionMcqResponseFormat(QuestionTypeConfig qTypeConfig) {
        // Question Item Schema
        JSONObject questionProperties = new JSONObject();

        // Question text
        JSONObject question = new JSONObject();
        question.put("type", "string");
        questionProperties.put("question", question);

        // Options
        JSONObject options = new JSONObject();
        options.put("type", "array");

        JSONObject optionItem = new JSONObject();
        optionItem.put("type", "string");

        options.put("items", optionItem);
        options.put("numOptions", qTypeConfig.getMaxOptions());

        questionProperties.put("options", options);

        // Answer
        JSONObject answer = new JSONObject();
        answer.put("type", "string");
        answer.put("description", "Should exactly match with one of the options");
        questionProperties.put("answer", answer);

        // Hint
        if (qTypeConfig.isEnableHint()) {
            JSONObject hint = new JSONObject();
            hint.put("type", "string");
            hint.put("description", "Should give an hint of why the correct answer");
            questionProperties.put("Hint", hint);
        }

        // KnowMore
        if (qTypeConfig.isEnableKnowMore()) {
            JSONObject knowMore = new JSONObject();
            knowMore.put("type", "string");
            knowMore.put("description", "Detail explanation of why the correct option is correct");
            questionProperties.put("KnowMore", knowMore);
        }

        // Required fields inside each question
        JSONArray questionRequired = new JSONArray();
        questionRequired.add("question");
        questionRequired.add("options");
        questionRequired.add("answer");

        if (qTypeConfig.isEnableHint()) questionRequired.add("Hint");
        if (qTypeConfig.isEnableKnowMore()) questionRequired.add("KnowMore");

        JSONObject questionItemSchema = new JSONObject();
        questionItemSchema.put("type", "object");
        questionItemSchema.put("properties", questionProperties);
        questionItemSchema.put("required", questionRequired);
        questionItemSchema.put("additionalProperties", false);

        // Questions Array

        JSONObject questionsArray = new JSONObject();
        questionsArray.put("type", "array");
        questionsArray.put("items", questionItemSchema);


        // Root Schema

        JSONObject rootProperties = new JSONObject();

        // Passage
        JSONObject passage = new JSONObject();
        passage.put("type", "string");
        passage.put("description","A comprehensive, self-contained academic passage generated strictly based on the user input or directives");

        rootProperties.put("passage", passage);
        rootProperties.put("questions", questionsArray);

        JSONArray rootRequired = new JSONArray();
        rootRequired.add("passage");
        rootRequired.add("questions");

        JSONObject rootSchema = new JSONObject();
        rootSchema.put("type", "object");
        rootSchema.put("properties", rootProperties);
        rootSchema.put("required", rootRequired);
        rootSchema.put("additionalProperties", false);


        // Final Response Format Wrapper


        JSONObject schemaWrapper = new JSONObject();
        schemaWrapper.put("name", "comprehension_mcq_generation");
        schemaWrapper.put("schema", rootSchema);
        schemaWrapper.put("strict", true);

        JSONObject responseFormat = new JSONObject();
        responseFormat.put("type", "json_schema");
        responseFormat.put("json_schema", schemaWrapper);

        return responseFormat;
    }



    @SuppressWarnings("unchecked")
	private static JSONObject wrapAsResponseFormat(String schemaName, JSONObject properties, List<String> requiredFields) {
        JSONObject questionItemSchema = new JSONObject();
        questionItemSchema.put("type", "object");
        questionItemSchema.put("properties", properties);

        JSONArray requiredArray = new JSONArray();
        requiredArray.addAll(requiredFields);
        questionItemSchema.put("required", requiredArray);
        questionItemSchema.put("additionalProperties", false);

        JSONObject questionsSchema = new JSONObject();
        questionsSchema.put("type", "array");
        questionsSchema.put("items", questionItemSchema);

        JSONObject rootSchema = new JSONObject();
        rootSchema.put("type", "object");
        JSONObject rootProperties = new JSONObject();
        rootProperties.put("questions", questionsSchema);
        rootSchema.put("properties", rootProperties);

        JSONArray rootRequired = new JSONArray();
        rootRequired.add("questions");
        rootSchema.put("required", rootRequired);
        rootSchema.put("additionalProperties", false);

        JSONObject responseFormat = new JSONObject();
        responseFormat.put("type", "json_schema");

        JSONObject schemaWrapper = new JSONObject();
        schemaWrapper.put("name", schemaName);
        schemaWrapper.put("schema", rootSchema);
        schemaWrapper.put("strict", true);

        responseFormat.put("json_schema", schemaWrapper);

        return responseFormat;
    }

    public static void main(String[] args) throws Exception {
        QuestionTypeConfig qTypeConfig = new QuestionTypeConfig();
        qTypeConfig.setEnableHint(true);
        qTypeConfig.setEnableKnowMore(true);
        qTypeConfig.setMaxOptions(4);
        qTypeConfig.setMaxQuestions(5);

        GPTResponseFormatBuilder formatter = new GPTResponseFormatBuilder();
//        System.out.println(formatter.buildResponseFormat("mcq", qTypeConfig).toJSONString());
//        System.out.println(formatter.buildResponseFormat("truefalse", qTypeConfig).toJSONString());
//        System.out.println(formatter.buildResponseFormat("faq", qTypeConfig).toJSONString());
//        System.out.println(formatter.buildResponseFormat("comprehensionmcq", qTypeConfig).toJSONString());
    }
}

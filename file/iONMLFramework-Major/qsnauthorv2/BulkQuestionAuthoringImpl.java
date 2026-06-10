package com.tcsion.ml.qsnauthorv2;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.Utils.ExceptionLogUtility;
import com.tcsion.ml.Utils.MlFrameworkConstants;
import com.tcsion.ml.qsnauthorv2.beans.BulkAuthoringInputBean;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.MetadataBean;
import com.tcsion.ml.qsnauthorv2.beans.OutputBean;
import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;
import com.tcsion.ml.qsnauthorv2.inputtypes.TextBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.prompts.GPTResponseFormatBuilder;
import com.tcsion.ml.qsnauthorv2.util.MetadataUtility;

public class BulkQuestionAuthoringImpl {

    private static final Log logger = LogFactory.getLog(BulkQuestionAuthoringImpl.class);

    private static final int THREAD_POOL_SIZE      = 5;
    private static final int AWAIT_TIMEOUT_SECONDS = 600;

    // Static shared executor — created once, never shut down between calls
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    // JVM shutdown hook to gracefully drain the pool on application exit
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.error("Shutdown hook triggered — shutting down BulkQuestionAuthoringImpl executor.");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, "BulkAuthoringExecutor-ShutdownHook"));
    }

    private final ObjectMapper            mapper            = new ObjectMapper();
    private final MetadataUtility         metadataUtility   = new MetadataUtility();
    private       GPTResponseFormatBuilder responseFormatter = new GPTResponseFormatBuilder();

    public String processApiByUseCase(final JSONObject inputDetailsJson,
                                      final JSONObject outputJson) throws Exception {

        logger.error("BulkQuestionAuthoringImpl :: processApiByUseCase :: START");

        //  1. Parse the common envelope fields
        final String apiID     = inputDetailsJson.getString("apiID");
        final int    port      = inputDetailsJson.getInt("port");
        final String jobId     = inputDetailsJson.getString("jobId");
        final String orgId     = inputDetailsJson.getString("orgId");
        final String appId     = inputDetailsJson.getString("appId");
        final String requestId = inputDetailsJson.getString("requestId");
        final String userId    = inputDetailsJson.getString("logged_by");

        // requestInput node contains only fileType and filePaths
        final JsonNode node = mapper.readValue(inputDetailsJson.getString("requestInput"), JsonNode.class);
        logger.error("requestInput node: " + node);

        final String fileType = node.get("fileType").textValue();
        logger.error("fileType: " + fileType);

        if (!"json".equalsIgnoreCase(fileType)) {
            throw new IllegalArgumentException(
                    "BulkQuestionAuthoringImpl expects fileType='json', got: " + fileType);
        }

        // filePaths
        final String filePathsStr = node.get("filePaths").asText();
        logger.error("filePathsStr: " + filePathsStr);
        String[] filePaths = new String[0];
        if (!Strings.isNullOrEmpty(filePathsStr) && !"null".equals(filePathsStr)) {
            final String stripped = filePathsStr.substring(1, filePathsStr.length() - 1);
            final String[] parts  = stripped.split(",");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            filePaths = parts;
        }
        logger.error("Parsed filePaths: " + java.util.Arrays.toString(filePaths));

        // metadataBean
        final JSONObject   textLimitJson = inputDetailsJson.getJSONObject("textLimitMap");
        final MetadataBean metadataBean  = metadataUtility.fetchAsyncMetadataFromCacheBulkAuthoring(textLimitJson, orgId);
        
        String vendorName = "";

        // vendorCredMap (optional)
        Map<String, HashMap<String, String>> aiVendorCredNodeMap = null;
        if (inputDetailsJson.has("vendorCredMap")) {
            try {
                final JSONObject aiVendorCredNode =inputDetailsJson.getJSONObject("vendorCredMap");
                aiVendorCredNodeMap = mapper.readValue(aiVendorCredNode.toString(),new TypeReference<Map<String, HashMap<String, String>>>() {});
                Map<String, HashMap<String, String>> vendorMap =aiVendorCredNodeMap;
				if (vendorMap != null && !vendorMap.isEmpty()) {
				    vendorName = vendorMap.keySet().iterator().next();
				} else {
				    logger.error("Vendor details map is null/empty for apiId={}"+apiID  +"orgId={} "+ orgId+ "appid={} "+appId);
				}
            } catch (Exception e) {
                ExceptionLogUtility.logException("Exception while parsing vendorCredMap:", e, logger);
            }
        }

        //  2. Read each JSON file — deserialize into BulkAuthoringInputBeans,
        //     then map each one to an InputBean
        final List<InputBean> inputBeans = new ArrayList<>();

        for (final String filePath : filePaths) {
            if (Strings.isNullOrEmpty(filePath)) continue;

            logger.error("Reading directives from file: " + filePath);

            try (FileReader fileReader = new FileReader(filePath.trim())) {

                final List<BulkAuthoringInputBean> bulkAuthorBeans = mapper.readValue(
                        fileReader, new TypeReference<List<BulkAuthoringInputBean>>() {});

                logger.error("Loaded " + bulkAuthorBeans.size() + " json item(s) from: " + filePath);
                logger.error("Bulk Auhtoring Total Bean "+ bulkAuthorBeans.toString());

                for (final BulkAuthoringInputBean bulkAuthorBean : bulkAuthorBeans) {
                    logger.info("Processing bulkAuthorBean: " + bulkAuthorBean);

                    final InputBean inputBean = mapBulkAuthorBeanToInputBean(
                            bulkAuthorBean, apiID, port, jobId, orgId, appId,
                            requestId, userId, fileType, metadataBean,
                            aiVendorCredNodeMap,vendorName);

                    logger.error("Built InputBean | questionNo=" + bulkAuthorBean.getQuestionId()
                            + " questionType=" + bulkAuthorBean.getQuestionType() + " | " + inputBean);

                    inputBeans.add(inputBean);
                }

            } catch (Exception e) {
                ExceptionLogUtility.logException( "Failed to read/parse JSON file: " + filePath, e, logger);
                // log and continue — remaining files still get processed
            }
        }

        if (inputBeans.isEmpty()) {
            logger.error("No InputBeans built. Returning empty response. JobId: " + jobId);
            outputJson.put("returnCode", "204");
            outputJson.put("message", "No proper json found. JobId: " + jobId);
            return outputJson.toString();
        }

        logger.error("Total InputBeans to process: " + inputBeans.size());

        //  3. Submit each InputBean to the shared static thread pool
        final List<Future<OutputBean>> futures = new ArrayList<>();

        for (final InputBean inputBean : inputBeans) {
            futures.add(executor.submit(new Callable<OutputBean>() {
                @Override
                public OutputBean call() {
                    final String qId = inputBean.getDirectives() != null
                            ? String.valueOf(inputBean.getDirectives().get("questionId"))
                            : "?";
                        
                    logger.error("Task START | questionNo=" + qId + " | thread=" + Thread.currentThread().getName());
                    try {
                        final TextBasedAuthoring textBasedAuthoring = new TextBasedAuthoring();
                        final OutputBean result = textBasedAuthoring.questionAuthoring(inputBean);
                        logger.error("Task END | questionNo=" + qId + " | returnCode=" + result.getReturnCode());
                        return result;
                    } catch (Exception e) {
                        ExceptionLogUtility.logException("Task FAILED | questionNo=" + qId, e, logger);
                        final OutputBean failBean = new OutputBean();
                        failBean.setReturnCode("500");
                        failBean.setMessage("Failed for questionNo=" + qId + " : " + e.getMessage());
                        return failBean;
                    }
                }
            }));
        }

        //  4. Wait for each future individually with a per-task timeout.
        //     The shared executor is NOT shut down — it remains available for future calls.
        for (int i = 0; i < futures.size(); i++) {
            try {
                futures.get(i).get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                logger.error("Future " + i + " exceeded timeout (" + AWAIT_TIMEOUT_SECONDS + "s). Cancelling task.");
                futures.get(i).cancel(true);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting for future " + i + ".");
            } catch (ExecutionException ee) {
                ExceptionLogUtility.logException("Future " + i + " threw an exception during await:", ee, logger);
            }
        }

        //  5. Collect results from futures
        final List<OutputBean> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                final OutputBean ob = futures.get(i).get();
                if (ob != null) {
                	results.add(ob);
                	
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.error("Future " + i + " was interrupted during result collection.");
            } catch (ExecutionException ee) {
                ExceptionLogUtility.logException("Future " + i + " threw an exception during result collection:", ee, logger);
            } catch (java.util.concurrent.CancellationException ce) {
                logger.error("Future " + i + " was cancelled (timeout). Skipping.");
            }
        }
        logger.error("Completed " + results.size() + "/" + inputBeans.size() + " tasks.");
        

        //  6. Merge all OutputBeans into a single combined output
        @SuppressWarnings("unchecked")
        final org.json.simple.JSONObject combinedOutput     = new org.json.simple.JSONObject();
        final ArrayList<Map<String, String>> combinedAudit  = new ArrayList<>();
        final org.json.simple.JSONObject combinedThirdParty = new org.json.simple.JSONObject();
        final org.json.simple.JSONArray outputArray = new org.json.simple.JSONArray();
        boolean anyFailure   = false;
        int     successCount = 0;

        for (int i = 0; i < results.size(); i++) {
            final OutputBean ob  = results.get(i);
            final String     key = "question_" + (i + 1);
            final String questionId = String.valueOf(inputBeans.get(i).getDirectives().get("questionId"));

            org.json.simple.JSONObject item = new org.json.simple.JSONObject();
            item.put("questionId", questionId);

            if (ob.getOutput() != null) {
                item.put("output", ob.getOutput());
                combinedOutput.put(key, ob.getOutput());
                successCount++;
            } else {
                item.put("output", new org.json.simple.JSONObject());
                combinedOutput.put(key, new org.json.simple.JSONObject());
                anyFailure = true;
            }

            outputArray.add(item);

            if (ob.getVendorAuditDetails() != null) {
                combinedAudit.addAll(ob.getVendorAuditDetails());
            }

            if (ob.getThirdPartyDetails() != null && !ob.getThirdPartyDetails().isEmpty()) {
                combinedThirdParty.putAll(ob.getThirdPartyDetails());
            }
        }

        logger.info("The outputarray : "+ outputArray.toString());
        
//        Path resultFilePath = null;
//
//        if (filePaths.length > 0) {
//            try {
//                resultFilePath = writeOutputFile(filePaths[0], outputArray);
//            } catch (Exception e) {
//                logger.error("Failed to write output file", e);
//            }
//        }
        

        final String mergedReturnCode = anyFailure ? "500" : "200";
        final String mergedMessage    = "Bulk authoring completed. Success: "
                + successCount + " / Total: " + results.size();

        logger.error("Merged authoring returnCode=" + mergedReturnCode + " | " + mergedMessage);

        //  7. Build final response using the output template
        try {
//            final JSONObject executionOutputJson = new JSONObject(combinedOutput);
        	final org.json.JSONArray executionOutputJson =
        	        new org.json.JSONArray(outputArray.toJSONString());

            if (outputJson.has(MlFrameworkConstants.RequestStatus)) {
                final String fieldName = outputJson.get(MlFrameworkConstants.RequestStatus)
                        .toString().replace("<", "").replace(">", "");
                outputJson.remove(MlFrameworkConstants.RequestStatus);
                outputJson.put(fieldName, mergedMessage);
            } else {
                logger.error("Missing RequestStatus from output template");
            }
            
//            if (outputJson.has(MlFrameworkConstants.ResultFilePath)) {
//                final String fieldName = outputJson.get(MlFrameworkConstants.ResultFilePath)
//                        .toString().replace("<", "").replace(">", "");
//                outputJson.remove(MlFrameworkConstants.ResultFilePath);
//                outputJson.put(fieldName, resultFilePath);
//            } else {
//                logger.error("Missing RequestStatus from output template");
//            }

            if (outputJson.has(MlFrameworkConstants.GeneratedOutput)) {
                final String fieldName = outputJson.get(MlFrameworkConstants.GeneratedOutput)
                        .toString().replace("<", "").replace(">", "");
                outputJson.remove(MlFrameworkConstants.GeneratedOutput);
                outputJson.put(fieldName, executionOutputJson);
            } else {
                logger.error("Missing GeneratedOutput from output template");
            }

            if (outputJson.has(MlFrameworkConstants.userInput)) {
                final String fieldName = outputJson.get(MlFrameworkConstants.userInput)
                        .toString().replace("<", "").replace(">", "");
                outputJson.remove(MlFrameworkConstants.userInput);
                outputJson.put(fieldName, "");
            } else {
                logger.error("Missing userInput from output template");
            }

            if (outputJson.has(MlFrameworkConstants.ReturnCode)) {
                final String fieldName = outputJson.get(MlFrameworkConstants.ReturnCode)
                        .toString().replace("<", "").replace(">", "");
                outputJson.remove(MlFrameworkConstants.ReturnCode);
                outputJson.put(fieldName, mergedReturnCode);
            } else {
                logger.error("Missing ReturnCode from output template");
            }

            if (outputJson.has(MlFrameworkConstants.THIRD_PARTY_DETAILS)) {
                final String fieldName = outputJson.get(MlFrameworkConstants.THIRD_PARTY_DETAILS)
                        .toString().replace("<", "").replace(">", "");
                outputJson.remove(MlFrameworkConstants.THIRD_PARTY_DETAILS);
                outputJson.put(fieldName, combinedThirdParty);
            } else {
                logger.error("Missing THIRD_PARTY_DETAILS from output template");
            }

        } catch (Exception e) {
            logger.error("Exception while building final response: " + e);
        }

        outputJson.put(MlFrameworkConstants.VENDOR_AUDIT_DETAILS, combinedAudit);

        logger.error("BulkQuestionAuthoringImpl :: processApiByUseCase :: END | jobId=" + jobId);
        logger.info("output json combind : " + outputJson.toString());
        return outputJson.toString();
        
    }

    //  Maps a BulkAuthoringInputBean to a fully populated InputBean
    @SuppressWarnings("unchecked")
    private InputBean mapBulkAuthorBeanToInputBean(
            final BulkAuthoringInputBean bulkAuthorBean,
            final String apiID,
            final int    port,
            final String jobId,
            final String orgId,
            final String appId,
            final String requestId,
            final String userId,
            final String fileType,
            final MetadataBean metadataBean,
            final Map<String, HashMap<String, String>> aiVendorCredNodeMap,String vendorName) {

        final InputBean inputBean = new InputBean();

        // common envelope fields
        inputBean.setApiID(apiID);
        inputBean.setPort(port);
        inputBean.setJobId(jobId);
        inputBean.setOrgId(orgId);
        inputBean.setAppId(appId);
        inputBean.setRequestId(requestId);
        inputBean.setUserId(userId);
        inputBean.setFileType("text");
        inputBean.setMetadataBean(metadataBean);
        inputBean.setAIvendorCredNodeMap(aiVendorCredNodeMap);
        inputBean.setUserInput(bulkAuthorBean.getAdditionalUserInstructions());

        final QuestionTypeConfig config = new QuestionTypeConfig();
        config.setEnableHint(bulkAuthorBean.isHintRequired());
        config.setEnableKnowMore(bulkAuthorBean.isExplanationRequired());
        config.setMaxOptions(bulkAuthorBean.getNumberOfAnswerOptions());
        config.setMaxQuestions(bulkAuthorBean.getQuestionCount());

        Map<String, QuestionTypeConfig> questionTypeConfigMap = new HashMap<String, QuestionTypeConfig>();
        questionTypeConfigMap.put(bulkAuthorBean.getQuestionType(), config);

        org.json.simple.JSONObject responseFormat = responseFormatter.buildResponseFormat(
                bulkAuthorBean.getQuestionType(), config);

        config.setResponseFormat(responseFormat);
        inputBean.setQuestionTypeConfigMap(questionTypeConfigMap);
        
        // map all BulkAuthoringInputBean fields into the directives JSONObject
        final org.json.simple.JSONObject directivesJson = new org.json.simple.JSONObject();
        directivesJson.put("questionId", bulkAuthorBean.getQuestionId());
        directivesJson.put("subject",    bulkAuthorBean.getSubject());
        directivesJson.put("topic",      bulkAuthorBean.getTopic());
        directivesJson.put("subTopic",   bulkAuthorBean.getSubTopic());
        directivesJson.put("concept",    bulkAuthorBean.getConcept());
        directivesJson.put(bulkAuthorBean.getAdditionalDirective1Label(), bulkAuthorBean.getAdditionalDirective1Value());
        directivesJson.put(bulkAuthorBean.getAdditionalDirective2Label(), bulkAuthorBean.getAdditionalDirective2Value());
        directivesJson.put("difficultyLevel", bulkAuthorBean.getDifficultyLevel());
        directivesJson.put("educationLevel",  bulkAuthorBean.getEducationLevel());
        inputBean.setDirectives(directivesJson);
        
        Map<String, String> parameterMap = new HashMap<>();
		parameterMap.put(GenAIFrameworkConstants.SERVICE_ID, String.valueOf(inputBean.getUserRegisteredServiceId()));
    	parameterMap.put("serviceName", vendorName);
		inputBean.setParameterMap(parameterMap);

        return inputBean;
    }
    
    
    
//    private Path writeOutputFile(String inputFilePath,
//    		org.json.simple.JSONArray outputArray) throws Exception {
//
//    	Path inputPath = Paths.get(inputFilePath);
//    	Path directory = inputPath.getParent();
//
//    	if (directory == null || !Files.exists(directory)) {
//    		throw new java.io.IOException("Invalid input directory: " + inputFilePath);
//    	}
//
//    	if (!Files.isWritable(directory)) {
//    		throw new java.io.IOException("No write permission for directory: " + directory);
//    	}
//
//    	Path outputPath = directory.resolve("output.json");
//
//    	Files.write(
//    			outputPath,
//    			outputArray.toJSONString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
//    			java.nio.file.StandardOpenOption.CREATE,
//    			java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
//    			);
//
//    	logger.error("Output file written at: " + outputPath.toAbsolutePath());
//    	return outputPath.toAbsolutePath();
//    }
}
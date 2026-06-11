package com.tcsion.ml.Services;

import java.net.InetAddress;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tcsion.ml.Beans.RinxBean;
import com.tcsion.ml.Utils.ExceptionLogUtility;
import com.tcsion.ml.Utils.MlFrameworkConstants;
import com.tcsion.ml.Utils.MlFrameworkUtils;

/**
 * Rinx CV parsing service.
 *
 * Flow (llmEnabled == 1):
 *   1. Slice vendorCredMap to the selected vendor node only
 *   2. Try LLM Pipeline (CVLlmEnhancer.tryLlmPipeline)
 *   3. On any failure / null result -> fall through to legacy RINX HTTP call
 *
 * Flow (llmEnabled == 0):
 *   1. Legacy RINX HTTP call
 */
public class Rinx {

    private static final Log logger = LogFactory.getLog(Rinx.class);

    public RinxBean rinxAsyncFlask(
            String inputFilePath,
            String outputFolderPath,
            int port,
            int llmEnabled,
            JSONObject userJson,
            JsonNode vendorCredMap,
            String useCaseId,
            String appId,
            String apiID,
            String orgId,
            String selectedVendorName,
            HashMap<String, Object> usageDetailsMap) {

        logger.error("-- Inside rinxAsyncFlask --");
        logger.error("llmEnabled=" + llmEnabled
                + " | selectedVendorName=" + selectedVendorName
                + " | userJson=" + (userJson != null ? "present(" + userJson.length() + " keys)" : "null")
                + " | vendorCredMap=" + (vendorCredMap != null ? "present" : "null")
                + " | inputFilePath=" + inputFilePath);

        RinxBean rb = new RinxBean();

        // -----------------------------------------------------------------------
        // Path & security validations 
        // -----------------------------------------------------------------------
        boolean isDirectory                 = false;
        boolean isInputFilePathValid        = false;
        boolean isOutputFolderPathValid     = false;
        boolean isDirectoryTraversalAttempt = false;
        boolean isFileExtensionValid        = false;

        final MlFrameworkUtils utility_obj = new MlFrameworkUtils();
        isInputFilePathValid        = utility_obj.validateImagePath(inputFilePath);
        isOutputFolderPathValid     = utility_obj.validateFolderPath(outputFolderPath);
        isDirectory                 = utility_obj.validateDirectory(outputFolderPath);
        isDirectoryTraversalAttempt = utility_obj.checkDirectoryTraversalAttempt(outputFolderPath);
        isFileExtensionValid        = utility_obj.validateFileExtensionPDFDocxDoc(inputFilePath);

        if (!isInputFilePathValid) {
            rb.setMessage(MlFrameworkConstants.FILE_PATH_DOES_NOT_EXIST_LOGGER_STMT);
            rb.setJsonOutput("");
            rb.setXmlOutput("");
            rb.setStatus("-1");
            return rb;
        }

        if (!isFileExtensionValid) {
            rb.setMessage(MlFrameworkConstants.INVALID_EXTENSION_ERROR);
            rb.setJsonOutput("");
            rb.setXmlOutput("");
            rb.setStatus("-1");
            return rb;
        }

        if (isDirectoryTraversalAttempt) {
            rb.setMessage(MlFrameworkConstants.PATH_IS_NOT_ALLOWED);
            rb.setJsonOutput("");
            rb.setXmlOutput("");
            rb.setStatus("-1");
            return rb;
        }

        if (!isOutputFolderPathValid) {
            rb.setJsonOutput("");
            rb.setXmlOutput("");
            rb.setMessage(MlFrameworkConstants.FOLDER_PATH_DOES_NOT_EXIST_LOGGER_STMT);
            rb.setStatus("-1");
            return rb;
        }

        if (!isDirectory) {
            rb.setJsonOutput("");
            rb.setXmlOutput("");
            rb.setMessage(MlFrameworkConstants.PATH_IS_NOT_DIRECTORY);
            rb.setStatus("-1");
            return rb;
        }

        // -----------------------------------------------------------------------
        // LLM pipeline � only when llmEnabled == 1
        //
        // vendorCredMap may contain credentials for multiple vendors.
        // We slice out only the selected vendor's node and wrap it back into
        // a single-entry ObjectNode { "VENDOR_NAME": { ...creds... } }
        // so CVLlmEnhancer receives the exact structure it always expects.
        // CVLlmEnhancer reads the first key of whatever node it receives �
        // zero changes needed there.
        //
        // If the selected vendor's node is missing from vendorCredMap,
        // we log clearly and fall through to RINX. No silent failures.
        // -----------------------------------------------------------------------
        if (llmEnabled == 1) {
            if (vendorCredMap == null) {
                logger.error("[Rinx] llmEnabled=1 but vendorCredMap is null � falling through to legacy RINX");
            } else {
                try {
                    JsonNode selectedVendorNode = vendorCredMap.get(selectedVendorName);

                    if (selectedVendorNode == null || selectedVendorNode.isNull()) {
                        logger.error("[Rinx] vendorCredMap does not contain an entry for vendor="
                                + selectedVendorName
                                + " � falling through to legacy RINX."
                                + " Verify caller is sending credentials for this vendor.");
                    } else {
                        // Wrap into single-entry map � CVLlmEnhancer reads first key as vendorName
                        ObjectNode singleVendorMap = JsonNodeFactory.instance.objectNode();
                        singleVendorMap.set(selectedVendorName, selectedVendorNode);

                        logger.error("[Rinx] -- Attempting LLM pipeline | vendor=" + selectedVendorName + " --");

                        RinxBean llmResult = CVLlmEnhancer.tryLlmPipeline(
                                inputFilePath, outputFolderPath,
                                userJson, singleVendorMap,
                                useCaseId, appId, apiID, orgId,
                                usageDetailsMap);

                        if (llmResult != null && ("1".equals(llmResult.getStatus())
                        	       || "CVPARSE_200".equals(llmResult.getStatus()))){
                            logger.error("[Rinx] -- LLM pipeline succeeded | vendor=" + selectedVendorName + " --");
                            return llmResult;
                        }

                        logger.error("[Rinx] -- LLM pipeline did not succeed | vendor=" + selectedVendorName
                                + " | falling through to legacy RINX --");
                    }

                } catch (Exception llmEx) {
                    logger.error("[Rinx] -- LLM pipeline exception | vendor=" + selectedVendorName
                            + " | error=" + llmEx.getMessage()
                            + " | falling through to legacy RINX --");
                    ExceptionLogUtility.logException("LLM pipeline exception inside Rinx:", llmEx, logger);
                }
            }
        }

        // -----------------------------------------------------------------------
        // Legacy RINX HTTP call �  unchanged
        // -----------------------------------------------------------------------
        try {
            String result = rinxJava(inputFilePath, outputFolderPath, port);
            logger.error("Result returned " + result);

            JSONObject result_jobj = new JSONObject(result);
            rb.setJsonOutput(result_jobj.get("Output_Json_File_Path").toString());
            rb.setXmlOutput(result_jobj.get("Output_Xml_File_Path").toString());
            rb.setMessage(result_jobj.get("Return_Message").toString());
            rb.setStatus(result_jobj.get("Return_Code").toString());

            // Mark engine in audit map for legacy path
            if (usageDetailsMap != null) {
                usageDetailsMap.put("engineId", "RINX");
            }

        } catch (Exception e) {
            ExceptionLogUtility.logException("Exception occured: {}", e, logger);
            rb.setJsonOutput("");
            rb.setXmlOutput("");
            rb.setMessage(MlFrameworkConstants.SOMETHING_IS_WRONG_IN_SCRIPT_EXECUTION);
            rb.setStatus("-1");
        }

        return rb;
    }

    // -----------------------------------------------------------------------
    // LEGACY RINX HTTP CALL � unchanged 
    // -----------------------------------------------------------------------
    public static String rinxJava(String inputFilePath, String outputFolderPath, int port)
            throws ParseException {

        HttpPost post = null;
        String responseString = null;
        String texts = null;

        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            String addr = InetAddress.getLocalHost().getHostAddress();

            logger.error("Port number " + port);
            post = new HttpPost("http://" + addr + ":" + port + "/rinx");
            logger.error("http://" + addr + ":" + port + "/rinx");

            logger.error("Input folder path: " + inputFilePath);
            logger.error("output folder path: " + outputFolderPath);

            JSONObject json = new JSONObject();
            json.put("inputFilePath", inputFilePath);
            json.put("outputFolderPath", outputFolderPath);

            StringEntity params = new StringEntity(json.toString());
            logger.error("----------THE HTTP REQUEST IS----------" + json.toString());

            post.addHeader("content-type", "application/json");
            post.setProtocolVersion(HttpVersion.HTTP_1_1);
            post.setEntity(params);

            HttpResponse response = httpclient.execute(post);

            int response_code = response.getStatusLine().getStatusCode();
            logger.error(response.getStatusLine().toString());
            logger.error(response.toString());
            logger.error("THE RESPONSE CODE IS : " + response_code);

            if (response_code == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                responseString = EntityUtils.toString(entity, "UTF-8");
                logger.error("---THE RESPONSE STRING IS: " + responseString);
            } else {
                logger.error("ERROR OCCURED IN SYNC SENDHTTP SCRIPT,STATUS CODE NOT 200");
                logger.error("---THE RESPONSE STRING IS: " + responseString);
            }

        } catch (Exception e) {
            ExceptionLogUtility.logException("Exception Occurred : {}", e, logger);
            responseString = null;
        } finally {
            if (post != null) {
                texts = responseString;
                post.releaseConnection();
            }
        }

        return texts;
    }
}

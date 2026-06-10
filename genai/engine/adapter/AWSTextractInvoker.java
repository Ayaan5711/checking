package com.tcs.genai.engine.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import com.amazonaws.services.textract.model.AnalyzeDocumentRequest;
import com.amazonaws.services.textract.model.AnalyzeDocumentResult;
import com.amazonaws.services.textract.model.DetectDocumentTextRequest;
import com.amazonaws.services.textract.model.DetectDocumentTextResult;
import com.amazonaws.services.textract.model.Document;
import com.amazonaws.services.textract.model.FeatureType;
import com.tcs.genai.engine.utils.EngineConstants;
import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapterFactory;


@SuppressWarnings("unchecked")
public class AWSTextractInvoker implements EngineInvokerAdapter {
	
	private static final Log logger = LogFactory
			.getLog(VendorPayloadAdapterFactory.class);

	@Override
	public JSONObject invokeEngine(String sourceDoc, String extractionType, Map<String, HashMap<String, String>> vendorProps) {
		
		
		JSONObject finalOutput = new JSONObject();
		
		String access_key_id = null;
		String secret_key_id = null;
		String region = null;
		
		try {
			Iterator<Entry<String, HashMap<String, String>>> iterator =  vendorProps.entrySet().iterator();
			while (iterator.hasNext()) {
			    Entry<String, HashMap<String, String>> entry = iterator.next();
			    Map<String,String> innerMap = entry.getValue();

			    access_key_id = innerMap.get(EngineConstants.ACCESS_KEY);
				secret_key_id = innerMap.get(EngineConstants.SECRET_KEY);
				region = innerMap.get(EngineConstants.REGION);
			}
			
//			String service = vendorProps.keySet().iterator().next();
			
			if (extractionType.equalsIgnoreCase("detect")) {
				finalOutput = awsTextractDetectDocument(sourceDoc, access_key_id, secret_key_id, region);
			} else if (extractionType.equalsIgnoreCase("analyze")) {
				finalOutput = awsTextractAnalyzeDocument(sourceDoc, access_key_id, secret_key_id, region);
			} else {
				logger.error("Invalid extraction Type for AWS it should be either detect or analyze");
				finalOutput.put(EngineConstants.AWS_TEXTRACT_DETECT_DOCUMENT_RESULT, null);
				finalOutput.put(EngineConstants.VENDOR_RESPONSE_STATUS, "Invalid extraction Type for AWS it should be either detect or analyze");
			}
		} catch (Exception e) {
			logger.error("Exception in invokeEngineAWS: " + e);
		}

		return finalOutput;
	}
	
	public JSONObject awsTextractDetectDocument(String sourceDoc, String access_key_id, String secret_key_id, String region) {
		logger.error("AI Vendor inside awsTextractDetectDocument");
		
		JSONObject finalVendorResultMap = new JSONObject();
		DetectDocumentTextResult detectDocumentTextResult = null;
		AmazonTextract client = null;
		ByteBuffer imageBytes = null;
		AmazonTextractClientBuilder clientBuilder = null;

		try(InputStream inputStream = Files.newInputStream(Paths.get(sourceDoc))) {
			clientBuilder = AmazonTextractClientBuilder.standard().withRegion(
					Regions.fromName(region));
			clientBuilder.setCredentials(new AWSStaticCredentialsProvider(
					new BasicAWSCredentials(access_key_id,secret_key_id)));

			logger.error("AWS building client");
			client = clientBuilder.build();
//			logger.error("AWS clientBuilder: " + clientBuilder);

//			InputStream inputStream = new FileInputStream(new File(sourceDoc));
			imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));

			DetectDocumentTextRequest detectDocumentTextRequest = new DetectDocumentTextRequest().withDocument(new Document().withBytes(imageBytes));
			
			detectDocumentTextResult = client
					.detectDocumentText(detectDocumentTextRequest);
			
			finalVendorResultMap.put(EngineConstants.AWS_TEXTRACT_DETECT_DOCUMENT_RESULT, detectDocumentTextResult);
			inputStream.close();

		} catch (NoClassDefFoundError | IOException | NoSuchMethodError
				| SdkClientException unableToConnectTextract) {
			logger.error("AI Vendor inside  catch block of awsTextractDetectDocument");
			logger.error("AI Vendor Exception occured in invoking AWS Textract Detect Document Text API {} "
					+ unableToConnectTextract);
			
			detectDocumentTextResult = null;
			
			finalVendorResultMap.put(EngineConstants.AWS_TEXTRACT_DETECT_DOCUMENT_RESULT, detectDocumentTextResult);
			finalVendorResultMap.put(EngineConstants.VENDOR_RESPONSE_STATUS, unableToConnectTextract.getMessage());
		}

		return finalVendorResultMap;

	}
	
	public JSONObject awsTextractAnalyzeDocument(String sourceDoc, String access_key_id, String secret_key_id, String region) {

		AnalyzeDocumentResult analyzeDocumentResult = null;
		AmazonTextract client = null;
		ByteBuffer imageBytes = null;
		AmazonTextractClientBuilder clientBuilder = null;
		JSONObject finalVendorResultMap = new JSONObject();
		logger.error("AI Vendor inside awsTextractAnalyzeDocument");

		try(InputStream inputStream = Files.newInputStream(Paths.get(sourceDoc))) {
			clientBuilder = AmazonTextractClientBuilder.standard().withRegion(
					Regions.fromName(region));
			clientBuilder.setCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(access_key_id,secret_key_id)));

			logger.error("AWS building client");
			client = clientBuilder.build();
//			logger.error("AWS clientBuilder: " + clientBuilder);
//			InputStream inputStream = new FileInputStream(new File(sourceDoc));
			imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
			AnalyzeDocumentRequest analyzeDocumentRequest = new AnalyzeDocumentRequest().withDocument(new Document().withBytes(imageBytes))
					.withFeatureTypes(FeatureType.FORMS);

			analyzeDocumentResult = client.analyzeDocument(analyzeDocumentRequest);
			
			finalVendorResultMap.put(EngineConstants.AWS_TEXTRACT_ANALYZE_DOCUMENT_RESULT, analyzeDocumentResult);

			inputStream.close();
		} catch (AmazonServiceException ase) {
		    logger.error("Caught AmazonServiceException, which means your request made it to AWS, but was rejected.");
		    logger.error("Error Message:    " + ase.getMessage());
		    logger.error("HTTP Status Code: " + ase.getStatusCode());
		    logger.error("AWS Error Code:   " + ase.getErrorCode());
		    logger.error("Error Type:       " + ase.getErrorType());
		    logger.error("Request ID:       " + ase.getRequestId());

		    analyzeDocumentResult = null;
		    finalVendorResultMap.put(EngineConstants.AWS_TEXTRACT_ANALYZE_DOCUMENT_RESULT, analyzeDocumentResult);
		} catch (SdkClientException sce) {
			finalVendorResultMap.put(EngineConstants.VENDOR_RESPONSE_STATUS, sce.getMessage());
		    logger.error("Caught SdkClientException, which means the client couldn't connect to AWS.");
		    logger.error("Error Message: " + sce.getMessage());

		    analyzeDocumentResult = null;
		    finalVendorResultMap.put(EngineConstants.AWS_TEXTRACT_ANALYZE_DOCUMENT_RESULT, analyzeDocumentResult);
		    finalVendorResultMap.put(EngineConstants.VENDOR_RESPONSE_STATUS, sce.getMessage());
		} catch (Exception e) {
			logger.error("Exception occured in invoking AWS Textract Analyze Document API {} "+ e);
			analyzeDocumentResult = null;
			finalVendorResultMap.put(EngineConstants.AWS_TEXTRACT_ANALYZE_DOCUMENT_RESULT, analyzeDocumentResult);
			finalVendorResultMap.put(EngineConstants.VENDOR_RESPONSE_STATUS, e.getMessage());
		} 

		return finalVendorResultMap;

	}
	
	@Override
	public JSONObject invokeAiEngine(JSONObject payload,
			Map<String, HashMap<String, String>> vendorProps) {
		return null;
	}

	

}

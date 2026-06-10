package com.tcs.genai.engine.adapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import com.tcs.genai.engine.utils.EngineConstants;
import com.tcs.genai.engine.utils.GoogleVisionErrorResponseHandler;
import com.tcs.genai.prompt.service.interfacehub.VendorPayloadAdapterFactory;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;


@SuppressWarnings("unchecked")
public class GoogleVisionInvoker implements EngineInvokerAdapter {

    private static final Log logger = LogFactory.getLog(VendorPayloadAdapterFactory.class);

    @Override
    public JSONObject invokeEngine(String sourceDoc, String extractionType,
            Map<String, HashMap<String, String>> vendorProps) {
        JSONObject finalOutput = new JSONObject();
        String GOOGLE_VISION_URL = null;
        String GOOGLE_VISION_API_KEY = null;

        try {
            Iterator<Entry<String, HashMap<String, String>>> iterator = vendorProps.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, HashMap<String, String>> entry = iterator.next();
                Map<String, String> innerMap = entry.getValue();

                GOOGLE_VISION_URL = innerMap.get(EngineConstants.GOOGLE_VISION_URL);
                GOOGLE_VISION_API_KEY = innerMap.get(EngineConstants.GOOGLE_VISION_API_KEY);
                extractionType = "TEXT_DETECTION";

                finalOutput = getDataFromGoogleVision(sourceDoc, extractionType, GOOGLE_VISION_URL, GOOGLE_VISION_API_KEY);
            }
        } catch (Exception e) {
            logger.error("[GoogleVisionInvoker] Exception in invokeEngineGoogleVision: " + e);
        }

        return finalOutput;
    }


    public JSONObject getDataFromGoogleVision(String sourceDoc, String extractionType,
            String GOOGLE_VISION_URL, String GOOGLE_VISION_API_KEY) {

        logger.error("[GoogleVisionInvoker] Extracting text from image using Google Vision: " + sourceDoc);
        JSONObject finalOutput = new JSONObject();

        // Build request body once — reused across retries
        String requestBody = null;
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(sourceDoc));
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
//            logger.error("Image Converted to B64!! Size: " + imageBytes.length + " bytes");

            JSONObject image = new JSONObject();
            image.put("content", base64Image);

            JSONObject feature = new JSONObject();
            feature.put("type", extractionType);

            JSONArray features = new JSONArray();
            features.add(feature);

            JSONObject request = new JSONObject();
            request.put("image", image);
            request.put("features", features);

            JSONArray requests = new JSONArray();
            requests.add(request);

            JSONObject body = new JSONObject();
            body.put("requests", requests);

            requestBody = body.toJSONString();

        } catch (Exception e) {
            logger.error("Failed to read image or build request payload: " + e.getMessage(), e);
            finalOutput.put(GenAIFrameworkConstants.VENDOR_RESPONSE, null);
            finalOutput.put(GenAIFrameworkConstants.STATUS_CODE, GenAIFrameworkConstants.GEN_410);
            finalOutput.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_410_MSG);
            return finalOutput;
        }

        // Retry loop — mirrors GenericHTTPCall.retry()
        int maxRetries = 2;
        int attempt = 0;
        JSONObject lastAttemptResult = new JSONObject();

        while (attempt <= maxRetries) {
            logger.info("[GoogleVisionInvoker] HTTP POST attempt " + (attempt + 1) + " of " + (maxRetries + 1));
            lastAttemptResult = makeHttpCall(GOOGLE_VISION_URL, GOOGLE_VISION_API_KEY, requestBody);

            int statusCode = (int) lastAttemptResult.get(GenAIFrameworkConstants.STATUS_CODE); // safe: makeHttpCall always stores raw int
            if (statusCode == HttpStatus.SC_OK) {
                break; // success
            }

            logger.error("[GoogleVisionInvoker] Attempt " + (attempt + 1) + " failed with status: " + statusCode);
            attempt++;
            if (attempt <= maxRetries) {
                try {
                    logger.error("Retrying in 10 seconds...");
                    Thread.sleep(attempt * 10000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        int finalStatusCode = (int) lastAttemptResult.get(GenAIFrameworkConstants.STATUS_CODE);
        GoogleVisionErrorResponseHandler googleVisionErrorHandler = new GoogleVisionErrorResponseHandler();
        Map<String, String> engineStatus = googleVisionErrorHandler.errorMessageForStatus(finalStatusCode);
        lastAttemptResult.put(GenAIFrameworkConstants.STATUS_CODE, engineStatus.get(EngineConstants.ENGINE_RETURN_CODE));
        lastAttemptResult.put(GenAIFrameworkConstants.STATUS_MESSAGE, engineStatus.get(EngineConstants.ENGINE_RETURN_MESSAGE));

        return lastAttemptResult;
    }

    
//    CODE FOR REASSEMBLING THE VISION EXTRACTED TEXT =*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*
    
    private String reassembleTextByCoordinates(org.json.JSONObject fullTextAnnotation, int yTolerance) {
        StringBuilder result = new StringBuilder();
        try {
            org.json.JSONArray pages = fullTextAnnotation.getJSONArray("pages");

            for (int p = 0; p < pages.length(); p++) {
                org.json.JSONObject page = pages.getJSONObject(p);
                org.json.JSONArray blocks = page.getJSONArray("blocks");

                // Build a sortable list of (snappedY, leftX, blockText)
                List<int[]> sortKeys = new ArrayList<>();       // [snappedY, leftX, index]
                List<String> blockTexts = new ArrayList<>();

                for (int b = 0; b < blocks.length(); b++) {
                    org.json.JSONObject block = blocks.getJSONObject(b);
                    String blockText = extractBlockText(block);
                    if (blockText.trim().isEmpty()) continue;

                    int topY = getBlockTopY(block);
                    int leftX = getBlockLeftX(block);
                    int snappedY = (topY / yTolerance) * yTolerance;

                    sortKeys.add(new int[]{snappedY, leftX, blockTexts.size()});
                    blockTexts.add(blockText);
                }

             // Sort by snappedY first, then leftX
                Collections.sort(sortKeys, new Comparator<int[]>() {
                    @Override
                    public int compare(int[] a, int[] b2) {
                        if (a[0] != b2[0]) return Integer.compare(a[0], b2[0]);
                        return Integer.compare(a[1], b2[1]);
                    }
                });

                for (int[] key : sortKeys) {
                    result.append(blockTexts.get(key[2]));
                }
            }
        } catch (Exception e) {
            logger.error("[GoogleVisionInvoker] Error in reassembleTextByCoordinates: " + e.getMessage(), e);
            // Fall back to raw text if anything goes wrong
            try {
                return fullTextAnnotation.getString("text");
            } catch (Exception ex) {
                return "";
            }
        }
        return result.toString();
    }

    private int getBlockTopY(org.json.JSONObject block) throws Exception {
        org.json.JSONArray vertices = block.getJSONObject("boundingBox").getJSONArray("vertices");
        int minY = Integer.MAX_VALUE;
        for (int i = 0; i < vertices.length(); i++) {
            org.json.JSONObject v = vertices.getJSONObject(i);
            if (v.has("y")) minY = Math.min(minY, v.getInt("y"));
        }
        return minY == Integer.MAX_VALUE ? 0 : minY;
    }

    private int getBlockLeftX(org.json.JSONObject block) throws Exception {
        org.json.JSONArray vertices = block.getJSONObject("boundingBox").getJSONArray("vertices");
        int minX = Integer.MAX_VALUE;
        for (int i = 0; i < vertices.length(); i++) {
            org.json.JSONObject v = vertices.getJSONObject(i);
            if (v.has("x")) minX = Math.min(minX, v.getInt("x"));
        }
        return minX == Integer.MAX_VALUE ? 0 : minX;
    }

    private String extractBlockText(org.json.JSONObject block) throws Exception {
        StringBuilder blockText = new StringBuilder();
        org.json.JSONArray paragraphs = block.getJSONArray("paragraphs");
        
        for (int p = 0; p < paragraphs.length(); p++) {
            org.json.JSONArray words = paragraphs.getJSONObject(p).getJSONArray("words");

            for (int w = 0; w < words.length(); w++) {
                org.json.JSONArray symbols = words.getJSONObject(w).getJSONArray("symbols");

                for (int s = 0; s < symbols.length(); s++) {
                    org.json.JSONObject symbol = symbols.getJSONObject(s);
                    blockText.append(symbol.getString("text"));

                    // Respect break type from symbol property
                    if (symbol.has("property")) {
                        org.json.JSONObject prop = symbol.getJSONObject("property");
                        if (prop.has("detectedBreak")) {
                            String breakType = prop.getJSONObject("detectedBreak").getString("type");
                            switch (breakType) {
                                case "SPACE":
                                    blockText.append(" ");
                                    break;
                                case "LINE_BREAK":
                                case "EOL_SURE_SPACE":
                                    blockText.append("\n");
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
        return blockText.toString();
    }
    
//    =*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*
    

    private JSONObject makeHttpCall(String GOOGLE_VISION_URL, String GOOGLE_VISION_API_KEY, String requestBody) {
        JSONObject finalOutput = new JSONObject();
        HttpURLConnection con = null;

        try {
            String urlStr = GOOGLE_VISION_URL + "?key=" + GOOGLE_VISION_API_KEY;
//            logger.error("Calling URL: " + urlStr);

            URL url = new URL(urlStr);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(30000); // 30s to establish connection
            con.setReadTimeout(60000);    // 60s to wait for response
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            long startTime = System.currentTimeMillis();
            OutputStream os = con.getOutputStream();
            os.write(requestBody.getBytes("UTF-8"));
            os.flush();

            String responseBody = getResponse(con);
            int statusCode = con.getResponseCode();
            long endTime = System.currentTimeMillis();
            logger.error("[GoogleVisionInvoker] Response received in " + (endTime - startTime) + "ms, status: " + statusCode);

            if (statusCode != HttpStatus.SC_OK) {
                logger.error("[GoogleVisionInvoker] Vision API Error - Status: " + statusCode + ", Response: " + responseBody);
                finalOutput.put(GenAIFrameworkConstants.VENDOR_RESPONSE, null);
                finalOutput.put(GenAIFrameworkConstants.STATUS_CODE, statusCode); // raw int
                finalOutput.put(GenAIFrameworkConstants.STATUS_MESSAGE, con.getResponseMessage());
                return finalOutput;
            }

            org.json.JSONObject result = new org.json.JSONObject(responseBody);
            org.json.JSONArray responsesArray = result.getJSONArray("responses");
            org.json.JSONObject firstResponse = responsesArray.getJSONObject(0);

            if (firstResponse.has("fullTextAnnotation")) {
            	org.json.JSONObject fullTextAnnotation = firstResponse.getJSONObject("fullTextAnnotation");
//                String fullText = firstResponse.getJSONObject("fullTextAnnotation").getString("text");
            	String fullText = reassembleTextByCoordinates(fullTextAnnotation, 40);
//                logger.error("Successfully extracted " + fullText.length() + " characters");
                finalOutput.put(GenAIFrameworkConstants.VENDOR_RESPONSE, fullText);
                finalOutput.put(GenAIFrameworkConstants.STATUS_CODE, statusCode); // raw int
                finalOutput.put(GenAIFrameworkConstants.STATUS_MESSAGE, con.getResponseMessage());
            } else {
                logger.error("[GoogleVisionInvoker] No text detected in the image");
                finalOutput.put(GenAIFrameworkConstants.VENDOR_RESPONSE, null);
                finalOutput.put(GenAIFrameworkConstants.STATUS_CODE, statusCode); // raw int
                finalOutput.put(GenAIFrameworkConstants.STATUS_MESSAGE, con.getResponseMessage());
            }

        } catch (Exception e) {
            logger.error("[GoogleVisionInvoker] Exception during Google Vision HTTP call: " + e.getMessage(), e);
            finalOutput.put(GenAIFrameworkConstants.VENDOR_RESPONSE, null);
            finalOutput.put(GenAIFrameworkConstants.STATUS_CODE, 500); // raw int for unknown failure
            finalOutput.put(GenAIFrameworkConstants.STATUS_MESSAGE, GenAIFrameworkConstants.GEN_410_MSG);
        } finally {
            if (con != null) con.disconnect(); // mirrors GenericHTTPCall finally block
        }

        return finalOutput;
    }


    // Mirrors getThirdPartyResponse() in GenericHTTPCall exactly
    private String getResponse(HttpURLConnection con) {
        StringBuilder response = new StringBuilder();
        String responseStr = "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            responseStr = response.toString();
        } catch (Exception e) {
            InputStream es = con.getErrorStream();
            if (es != null) {
                responseStr = new BufferedReader(new InputStreamReader(es))
                        .lines().collect(Collectors.joining("\n"));
                logger.error("Error response from Vision API: " + responseStr);
            }
        }
        return responseStr;
    }


    @Override
    public JSONObject invokeAiEngine(JSONObject payload,
            Map<String, HashMap<String, String>> vendorProps)
            throws JSONException, NullPointerException {
        return null;
    }

}
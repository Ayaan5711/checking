package com.tcs.genai.engine.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import com.tcs.genai.gemini.auth.GcpCredentialsProvider;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;

/**
 * Handles HTTP POST calls to the Vertex AI Gemini generateContent endpoint.
 *
 * <p>Mirrors the structure of {@link GenericHTTPCall} so that {@code GeminiEngineInvoker}
 * can follow the exact same invocation flow as {@code AzureAIEngineInvoker}:
 * <ol>
 *   <li>Instantiate with endpoint URL, GCP credentials file path, and serialised request JSON.</li>
 *   <li>Call {@link #makeCall()} — fetches a short-lived OAuth2 token, POSTs the request.</li>
 *   <li>On non-200, call {@link #retryWithBackoff(int, int)} for exponential-backoff retries.</li>
 * </ol>
 *
 * <p>Returns a {@code JSONObject} with the same three keys used across all adapters:
 * <ul>
 *   <li>{@code extractedText} — raw response body string</li>
 *   <li>{@code statuscode}    — HTTP integer status code</li>
 *   <li>{@code statusmessage} — HTTP status message or error description</li>
 * </ul>
 */
public class GeminiHTTPCall {

    private static final Log logger = LogFactory.getLog(GeminiHTTPCall.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 120_000;

    private final String endpointUrl;
    private final String credentialsFilePath;
    private final String requestJson;

    public GeminiHTTPCall(String endpointUrl, String credentialsFilePath, String requestJson) {
        this.endpointUrl          = endpointUrl;
        this.credentialsFilePath  = credentialsFilePath;
        this.requestJson          = requestJson;
    }

    /**
     * Obtains a fresh OAuth2 Bearer token and POSTs the request to the Vertex AI endpoint.
     *
     * @return JSONObject with {@code extractedText}, {@code statuscode}, {@code statusmessage}
     */
    @SuppressWarnings("unchecked")
    public JSONObject makeCall() {
        JSONObject finalVendorResultMap = new JSONObject();
        HttpURLConnection con = null;

        try {
            // Obtain short-lived OAuth2 token using service-account credentials
            GcpCredentialsProvider credentialsProvider = new GcpCredentialsProvider(credentialsFilePath);
            String accessToken = credentialsProvider.getAccessToken();

            byte[] requestBytes = requestJson.getBytes(StandardCharsets.UTF_8);

            URL url = new URL(endpointUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("Authorization", "Bearer " + accessToken);
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(requestBytes);
            }

            int statusCode = con.getResponseCode();
            String statusMessage = con.getResponseMessage();
            String responseBody;

            if (statusCode >= 200 && statusCode < 300) {
                responseBody = readStream(con.getInputStream());
            } else {
                responseBody = readStream(con.getErrorStream());
                logger.error(GenAIFrameworkConstants.LOGGER_ERROR
                        + " [GeminiHTTPCall] Non-2xx from Vertex AI. Status: " + statusCode
                        + " Body: " + responseBody);
            }

            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, responseBody);
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, statusCode);
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, statusMessage);

        } catch (Exception e) {
            logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION
                    + " [GeminiHTTPCall] Exception occurred during connection: " + e);
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_CODE, 999);
            finalVendorResultMap.put(GenAIFrameworkConstants.VENDOR_RESPONSE, "");
            finalVendorResultMap.put(GenAIFrameworkConstants.STATUS_MESSAGE, "Gemini engine invocation failed: " + e.getMessage());
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        logger.info("[GeminiHTTPCall][makeCall] finalVendorResultMap: " + finalVendorResultMap);
        return finalVendorResultMap;
    }

    /**
     * Retries the call with exponential back-off until a 200 is received or retries are exhausted.
     * Mirrors {@link GenericHTTPCall#retryWithBackoff(int, int)}.
     *
     * @param maxRetries   maximum number of retry attempts
     * @param baseDelayMs  base delay in milliseconds (doubles with each retry)
     */
    public JSONObject retryWithBackoff(int maxRetries, int baseDelayMs) throws InterruptedException {
        JSONObject vendorResponse = null;
        int retryCount = 1;
        while (retryCount < maxRetries) {
            if (retryCount > 0) {
                long delay = (long) (baseDelayMs * Math.pow(2, retryCount - 1));
                Thread.sleep(delay);
            }
            vendorResponse = makeCall();
            int statusCode = (int) vendorResponse.get(GenAIFrameworkConstants.STATUS_CODE);
            logger.error(GenAIFrameworkConstants.LOGGER_ERROR
                    + " [GeminiHTTPCall] Exponential backoff retry: " + (retryCount + 1)
                    + ", response: " + vendorResponse);
            if (statusCode == HttpStatus.SC_OK) {
                break;
            }
            retryCount++;
        }
        return vendorResponse;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String readStream(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }
}

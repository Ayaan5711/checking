package com.tcs.genai.gemini.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

/**
 * Provides GCP OAuth2 access tokens for authenticating with Vertex AI
 * using a service account JSON key file.
 */
public class GcpCredentialsProvider {

    private static final String CLOUD_PLATFORM_SCOPE =
            "https://www.googleapis.com/auth/cloud-platform";

    private final String credentialsFilePath;

    /**
     * @param credentialsFilePath absolute path to a service account JSON key file.
     */
    public GcpCredentialsProvider(String credentialsFilePath) {
        this.credentialsFilePath = credentialsFilePath;
    }

    /**
     * Returns a valid OAuth2 Bearer token string.
     * Loads the service account JSON, scopes it, and calls {@code refresh()} to
     * obtain a fresh token — matching the proven pattern from {@code GetGcpAccessToken}.
     *
     * @throws IOException if the credentials file cannot be read or the token request fails
     */
    public String getAccessToken() throws IOException {
        GoogleCredentials credentials = ServiceAccountCredentials
                .fromStream(new FileInputStream(credentialsFilePath))
                .createScoped(Collections.singletonList(CLOUD_PLATFORM_SCOPE));

        credentials.refresh();

        String token = credentials.getAccessToken().getTokenValue();
        java.util.Date expiry = credentials.getAccessToken().getExpirationTime();

        System.out.println("Token expires at: " + expiry);

        if (token == null) {
            throw new IOException(
                    "Failed to obtain an access token from: " + credentialsFilePath);
        }

        return token;
    }
}


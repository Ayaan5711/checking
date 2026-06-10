package com.tcs.genai.gemini.model;

import java.util.Collections;
import java.util.List;

/**
 * Wrapper for the Vertex AI {@code publishers.models.list} API response.
 *
 * <pre>
 * {
 *   "publisherModels": [ ... ],
 *   "nextPageToken": "..."
 * }
 * </pre>
 */
public class PublisherModelListResponse {

    private List<PublisherModel> publisherModels;
    private String nextPageToken;

    public List<PublisherModel> getPublisherModels() {
        return publisherModels != null ? publisherModels : Collections.<PublisherModel>emptyList();
    }
    public void setPublisherModels(List<PublisherModel> v) { this.publisherModels = v; }

    public String getNextPageToken()         { return nextPageToken; }
    public void setNextPageToken(String v)   { this.nextPageToken = v; }
}


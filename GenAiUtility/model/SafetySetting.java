package com.tcs.genai.gemini.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single Vertex AI safety setting that controls how the model handles
 * potentially harmful content in a given category.
 *
 * <p>API reference:
 * https://cloud.google.com/vertex-ai/generative-ai/docs/reference/rest/v1/SafetySetting
 *
 * <p>Example usage — block only high-probability harm:
 * <pre>
 *   SafetySetting.of(SafetySetting.HarmCategory.HARM_CATEGORY_HATE_SPEECH,
 *                    SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafetySetting {

    private String category;
    private String threshold;

    public SafetySetting() {}

    public SafetySetting(String category, String threshold) {
        this.category  = category;
        this.threshold = threshold;
    }

    /** Convenience factory using the typed enum constants. */
    public static SafetySetting of(HarmCategory category, HarmBlockThreshold threshold) {
        return new SafetySetting(category.value(), threshold.value());
    }

    public String getCategory()          { return category; }
    public void setCategory(String v)    { this.category = v; }

    public String getThreshold()         { return threshold; }
    public void setThreshold(String v)   { this.threshold = v; }

    // -------------------------------------------------------------------------
    // Enum constants
    // -------------------------------------------------------------------------

    /**
     * Harm categories recognised by the Vertex AI safety filter.
     */
    public enum HarmCategory {
        HARM_CATEGORY_UNSPECIFIED("HARM_CATEGORY_UNSPECIFIED"),
        HARM_CATEGORY_HATE_SPEECH("HARM_CATEGORY_HATE_SPEECH"),
        HARM_CATEGORY_DANGEROUS_CONTENT("HARM_CATEGORY_DANGEROUS_CONTENT"),
        HARM_CATEGORY_HARASSMENT("HARM_CATEGORY_HARASSMENT"),
        HARM_CATEGORY_SEXUALLY_EXPLICIT("HARM_CATEGORY_SEXUALLY_EXPLICIT"),
        HARM_CATEGORY_CIVIC_INTEGRITY("HARM_CATEGORY_CIVIC_INTEGRITY");

        private final String apiValue;
        HarmCategory(String apiValue) { this.apiValue = apiValue; }
        public String value() { return apiValue; }
    }

    /**
     * Thresholds that determine when content is blocked.
     *
     * <ul>
     *   <li>{@code BLOCK_NONE} — never block (monitoring only)</li>
     *   <li>{@code BLOCK_ONLY_HIGH} — block only high-probability harm</li>
     *   <li>{@code BLOCK_MEDIUM_AND_ABOVE} — block medium and high (default)</li>
     *   <li>{@code BLOCK_LOW_AND_ABOVE} — block low, medium, and high (strictest)</li>
     *   <li>{@code OFF} — safety filter disabled for this category</li>
     * </ul>
     */
    public enum HarmBlockThreshold {
        HARM_BLOCK_THRESHOLD_UNSPECIFIED("HARM_BLOCK_THRESHOLD_UNSPECIFIED"),
        BLOCK_NONE("BLOCK_NONE"),
        BLOCK_ONLY_HIGH("BLOCK_ONLY_HIGH"),
        BLOCK_MEDIUM_AND_ABOVE("BLOCK_MEDIUM_AND_ABOVE"),
        BLOCK_LOW_AND_ABOVE("BLOCK_LOW_AND_ABOVE"),
        OFF("OFF");

        private final String apiValue;
        HarmBlockThreshold(String apiValue) { this.apiValue = apiValue; }
        public String value() { return apiValue; }
    }
}


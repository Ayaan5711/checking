package com.tcs.genai.gemini.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for the Vertex AI Gemini generateContent API.
 *
 * API reference:
 * https://cloud.google.com/vertex-ai/generative-ai/docs/reference/rest/v1/projects.locations.publishers.models/generateContent
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerateContentRequest {

    /** The conversation turns (user / model messages). */
    private List<Content> contents;

    /** Optional system instruction that guides the model's behaviour. */
    private Content systemInstruction;

    /** Sampling and output-length parameters. */
    private GenerationConfig generationConfig;

    /** Per-category safety thresholds. When null the model's defaults apply. */
    private List<SafetySetting> safetySettings;

    public GenerateContentRequest() {}

    public GenerateContentRequest(List<Content> contents,
                                  Content systemInstruction,
                                  GenerationConfig generationConfig) {
        this.contents = contents;
        this.systemInstruction = systemInstruction;
        this.generationConfig = generationConfig;
    }

    public GenerateContentRequest(List<Content> contents,
                                  Content systemInstruction,
                                  GenerationConfig generationConfig,
                                  List<SafetySetting> safetySettings) {
        this.contents          = contents;
        this.systemInstruction = systemInstruction;
        this.generationConfig  = generationConfig;
        this.safetySettings    = safetySettings;
    }

    public List<Content> getContents() { return contents; }
    public void setContents(List<Content> contents) { this.contents = contents; }

    public Content getSystemInstruction() { return systemInstruction; }
    public void setSystemInstruction(Content systemInstruction) { this.systemInstruction = systemInstruction; }

    public GenerationConfig getGenerationConfig() { return generationConfig; }
    public void setGenerationConfig(GenerationConfig generationConfig) { this.generationConfig = generationConfig; }

    public List<SafetySetting> getSafetySettings() { return safetySettings; }
    public void setSafetySettings(List<SafetySetting> safetySettings) { this.safetySettings = safetySettings; }
}


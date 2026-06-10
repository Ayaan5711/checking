package com.tcs.genai.gemini.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Controls the generation behaviour of the Gemini model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerationConfig {

    private Double temperature;
    private Integer maxOutputTokens;
    private Double topP;
    /** Fixed random seed — when set alongside temperature=0 produces deterministic output. */
    private Integer seed;

    public GenerationConfig() {}

    public GenerationConfig(double temperature, int maxOutputTokens, double topP) {
        this.temperature = temperature;
        this.maxOutputTokens = maxOutputTokens;
        this.topP = topP;
    }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }

    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }

    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }
}


package com.tcs.genai.gemini.model;

/**
 * Immutable domain object holding a resolved prompt configuration.
 *
 * Created from a {@link com.tcs.genai.gemini.prompt.PromptConfigService} lookup
 * (currently JSON-file-backed; can be swapped for a database-backed implementation
 * without any changes to the callers).
 */
public class PromptConfig {

    private final String systemPrompt;
    private final String userPrompt;
    private final double temperature;
    private final double topP;
    private final int    maxTokens;

    public PromptConfig(String systemPrompt,
                        String userPrompt,
                        double temperature,
                        double topP,
                        int    maxTokens) {
        this.systemPrompt = systemPrompt;
        this.userPrompt   = userPrompt;
        this.temperature  = temperature;
        this.topP         = topP;
        this.maxTokens    = maxTokens;
    }

    public String getSystemPrompt() { return systemPrompt; }
    public String getUserPrompt()   { return userPrompt; }
    public double getTemperature()  { return temperature; }
    public double getTopP()         { return topP; }
    public int    getMaxTokens()    { return maxTokens; }
}


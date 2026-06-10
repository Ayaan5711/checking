package com.tcs.genai.gemini.model;

import java.util.List;

/**
 * Represents a single publisher model entry returned by the Vertex AI
 * {@code publishers.models.list} REST API.
 *
 * Only the fields that are commonly useful for display are mapped here;
 * unknown JSON fields are silently ignored by the configured {@link com.fasterxml.jackson.databind.ObjectMapper}.
 */
public class PublisherModel {

    private String name;
    private String displayName;
    private String description;
    private String launchStage;
    private String versionId;
    private List<String> frameworks;

    public String getName()        { return name; }
    public void setName(String v)  { this.name = v; }

    public String getDisplayName()       { return displayName; }
    public void setDisplayName(String v) { this.displayName = v; }

    public String getDescription()       { return description; }
    public void setDescription(String v) { this.description = v; }

    public String getLaunchStage()       { return launchStage; }
    public void setLaunchStage(String v) { this.launchStage = v; }

    public String getVersionId()         { return versionId; }
    public void setVersionId(String v)   { this.versionId = v; }

    public List<String> getFrameworks()          { return frameworks; }
    public void setFrameworks(List<String> v)    { this.frameworks = v; }

    /**
     * Returns a short, human-readable identifier derived from the {@code name} field.
     * For example, {@code "publishers/google/models/gemini-2.0-flash-001"} becomes
     * {@code "gemini-2.0-flash-001"}.
     */
    public String getModelId() {
        if (name == null) {
            return "";
        }
        int lastSlash = name.lastIndexOf('/');
        return lastSlash >= 0 ? name.substring(lastSlash + 1) : name;
    }
}


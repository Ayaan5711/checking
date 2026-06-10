package com.tcs.genai.gemini.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Represents a single turn in the conversation, or a system instruction block.
 * The {@code role} field is omitted for system instructions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Content {

    private String role;
    private List<Part> parts;

    public Content() {}

    /** Constructor for user/model turns (role required). */
    public Content(String role, List<Part> parts) {
        this.role = role;
        this.parts = parts;
    }

    /** Constructor for system instructions (no role). */
    public Content(List<Part> parts) {
        this.parts = parts;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<Part> getParts() { return parts; }
    public void setParts(List<Part> parts) { this.parts = parts; }
}


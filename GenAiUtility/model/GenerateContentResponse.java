package com.tcs.genai.gemini.model;

import java.util.List;

/**
 * Response body from the Vertex AI Gemini generateContent API.
 */
public class GenerateContentResponse {

    private List<Candidate> candidates;
    private UsageMetadata usageMetadata;
    private PromptFeedback promptFeedback;

    public List<Candidate> getCandidates() { return candidates; }
    public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }

    public UsageMetadata getUsageMetadata() { return usageMetadata; }
    public void setUsageMetadata(UsageMetadata usageMetadata) { this.usageMetadata = usageMetadata; }

    public PromptFeedback getPromptFeedback() { return promptFeedback; }
    public void setPromptFeedback(PromptFeedback promptFeedback) { this.promptFeedback = promptFeedback; }

    /**
     * Convenience: extracts the text of the first candidate's first part.
     * Returns an empty string when no candidates or parts are present.
     */
    public String getFirstCandidateText() {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        Content content = candidates.get(0).getContent();
        if (content == null || content.getParts() == null || content.getParts().isEmpty()) {
            return "";
        }
        String text = content.getParts().get(0).getText();
        return text != null ? text : "";
    }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    public static class Candidate {
        private Content content;
        private String finishReason;
        private int index;

        public Content getContent() { return content; }
        public void setContent(Content content) { this.content = content; }

        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
    }

    public static class UsageMetadata {
        private int promptTokenCount;
        private int candidatesTokenCount;
        private int totalTokenCount;

        public int getPromptTokenCount() { return promptTokenCount; }
        public void setPromptTokenCount(int promptTokenCount) { this.promptTokenCount = promptTokenCount; }

        public int getCandidatesTokenCount() { return candidatesTokenCount; }
        public void setCandidatesTokenCount(int candidatesTokenCount) { this.candidatesTokenCount = candidatesTokenCount; }

        public int getTotalTokenCount() { return totalTokenCount; }
        public void setTotalTokenCount(int totalTokenCount) { this.totalTokenCount = totalTokenCount; }
    }

    /**
     * Present when the prompt itself was blocked before any candidate was generated.
     * {@code blockReason} values include {@code MODEL_ARMOR}, {@code SAFETY}, {@code OTHER}.
     */
    public static class PromptFeedback {
        private String blockReason;
        private String blockReasonMessage;

        public String getBlockReason() { return blockReason; }
        public void setBlockReason(String blockReason) { this.blockReason = blockReason; }

        public String getBlockReasonMessage() { return blockReasonMessage; }
        public void setBlockReasonMessage(String blockReasonMessage) { this.blockReasonMessage = blockReasonMessage; }
    }
}


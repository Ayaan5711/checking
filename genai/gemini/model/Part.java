package com.tcs.genai.gemini.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single part of a Gemini content block.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@code text} — plain text content</li>
 *   <li>{@code fileData} — reference to a file stored in GCS</li>
 *   <li>{@code inlineData} — base64-encoded file bytes embedded directly in the request
 *       (no GCS required; subject to request-size limits ~20 MB)</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Part {

    private String     text;
    private FileData   fileData;
    private InlineData inlineData;

    public Part() {}

    public Part(String text) {
        this.text = text;
    }

    /** Factory method for a GCS-backed file part (video, audio, PDF, etc.). */
    public static Part ofFile(String gcsUri, String mimeType) {
        Part p = new Part();
        p.fileData = new FileData(mimeType, gcsUri);
        return p;
    }

    /**
     * Factory method for an inline (base64-encoded) file part.
     * Use this when the file is on the local filesystem and you do not want to upload to GCS.
     *
     * @param data     raw file bytes
     * @param mimeType MIME type, e.g. {@code "video/mp4"}
     */
    public static Part ofInlineData(byte[] data, String mimeType) {
        Part p = new Part();
        p.inlineData = new InlineData(mimeType, data);
        return p;
    }

    public String getText()   { return text; }
    public void setText(String text) { this.text = text; }

    public FileData getFileData()              { return fileData; }
    public void setFileData(FileData fileData) { this.fileData = fileData; }

    public InlineData getInlineData()                { return inlineData; }
    public void setInlineData(InlineData inlineData) { this.inlineData = inlineData; }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    /** Reference to a file stored in Google Cloud Storage. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FileData {
        private String mimeType;
        private String fileUri;

        public FileData() {}

        public FileData(String mimeType, String fileUri) {
            this.mimeType = mimeType;
            this.fileUri  = fileUri;
        }

        public String getMimeType()       { return mimeType; }
        public void setMimeType(String v) { this.mimeType = v; }

        public String getFileUri()        { return fileUri; }
        public void setFileUri(String v)  { this.fileUri = v; }
    }

    /**
     * Inline (base64-encoded) file data embedded directly in the request.
     * Jackson serialises {@code byte[]} as a Base64 string automatically.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InlineData {
        private String mimeType;
        private byte[] data;

        public InlineData() {}

        public InlineData(String mimeType, byte[] data) {
            this.mimeType = mimeType;
            this.data     = data;
        }

        public String getMimeType()       { return mimeType; }
        public void setMimeType(String v) { this.mimeType = v; }

        public byte[] getData()           { return data; }
        public void setData(byte[] v)     { this.data = v; }
    }
}


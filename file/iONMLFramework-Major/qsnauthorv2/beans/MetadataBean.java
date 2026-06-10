package com.tcsion.ml.qsnauthorv2.beans;

public class MetadataBean {
	private int chunkSize = 15;
	private int gpt4oChunkSize = 15;
	private boolean shuffleChunks = false;
	private int topK = 3;
	private int sequenceLength = 300;
	private float temperature = 0.7f;
	private float cosineThreshold = 0.75f;

	public int getChunkSize() {
		return chunkSize;
	}
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
	public int getGpt4oChunkSize() {
		return gpt4oChunkSize;
	}
	public void setGpt4oChunkSize(int gpt4oChunkSize) {
		this.gpt4oChunkSize = gpt4oChunkSize;
	}
	public boolean getShuffleChunks() {
		return shuffleChunks;
	}
	public void setShuffleChunks(boolean shuffleChunks) {
		this.shuffleChunks = shuffleChunks;
	}
	public int getTopK() {
		return topK;
	}
	public void setTopK(int topK) {
		this.topK = topK;
	}
	public int getSequenceLength() {
		return sequenceLength;
	}
	public void setSequenceLength(int sequenceLength) {
		this.sequenceLength = sequenceLength;
	}
	public float getTemperature() {
		return temperature;
	}
	public void setTemperature(float temperature) {
		this.temperature = temperature;
	}

	public float getCosineThreshold() {
		return cosineThreshold;
	}
	public void setCosineThreshold(float cosineThreshold) {
		this.cosineThreshold = cosineThreshold;
	}
	@Override
	public String toString() {
		return "QuestionAuthoringMetadataBean [chunkSize=" + chunkSize
				+ ", gpt4oChunkSize=" + gpt4oChunkSize + ", shuffleChunks="
				+ shuffleChunks + ", topK=" + topK + ", sequenceLength="
				+ sequenceLength + ", temperature=" + temperature
				+ ", cosineThreshold=" + cosineThreshold + "]";
	}

}

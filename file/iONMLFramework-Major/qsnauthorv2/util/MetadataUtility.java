package com.tcsion.ml.qsnauthorv2.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcsion.ml.qsnauthorv2.beans.MetadataBean;

public class MetadataUtility {
	private final Log logger=LogFactory.getLog(MetadataUtility.class);
	final ObjectMapper mapper = new ObjectMapper();
	
	public MetadataBean fetchAsyncMetadataFromCache(JSONObject textLimitJson, String orgId) throws JsonParseException, JsonMappingException, IOException{
		final Map<String, String> textLimitsMap = mapper.readValue(textLimitJson.toString(),new TypeReference<HashMap<String, String>>(){});
		logger.error("final textLimitsMap ::"+textLimitsMap);
		
		MetadataBean metadataBean = new MetadataBean();
		
		String chunkSizeProperty = "ionml.QuestionGenerationV2UC.ChunkSize";
		if (textLimitsMap.containsKey(chunkSizeProperty+"."+orgId)){
			metadataBean.setChunkSize(Integer.parseInt(textLimitsMap.get(chunkSizeProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(chunkSizeProperty)){
			metadataBean.setChunkSize(Integer.parseInt(textLimitsMap.get(chunkSizeProperty)));
		}
		else{
			logger.error(chunkSizeProperty + " not present in the textLimitsMap");
		}
		
		String gptChunkSizeProperty = "ionml.QuestionGenerationV2UC.GPT4o.ChunkSize";
		if (textLimitsMap.containsKey(gptChunkSizeProperty+"."+orgId)){
			metadataBean.setGpt4oChunkSize(Integer.parseInt(textLimitsMap.get(gptChunkSizeProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(gptChunkSizeProperty)){
			metadataBean.setGpt4oChunkSize(Integer.parseInt(textLimitsMap.get(gptChunkSizeProperty)));
		}
		else{
			logger.error(gptChunkSizeProperty + " not present in the textLimitsMap");
		}
		
		String shuffleChunksProperty = "ionml.QuestionGenerationV2UC.GPT4o.shuffleChunks";
		if (textLimitsMap.containsKey(shuffleChunksProperty+"."+orgId)){
			metadataBean.setShuffleChunks(Integer.parseInt(textLimitsMap.get(shuffleChunksProperty+"."+orgId))!=0);
		}
		else if(textLimitsMap.containsKey(shuffleChunksProperty)){
			metadataBean.setShuffleChunks(Integer.parseInt(textLimitsMap.get(shuffleChunksProperty))!=0);
		}
		else{
			logger.error(shuffleChunksProperty + " not present in the textLimitsMap");
		}
		
		String topKProperty = "ionml.QuestionGenerationV2UC.TopK";
		if (textLimitsMap.containsKey(topKProperty+"."+orgId)){
			metadataBean.setTopK(Integer.parseInt(textLimitsMap.get(topKProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(topKProperty)){
			metadataBean.setTopK(Integer.parseInt(textLimitsMap.get(topKProperty)));
		}
		else{
			logger.error(topKProperty + " not present in the textLimitsMap");
		}
		
		String seqLengthProperty = "ionml.QuestionGenerationV2UC.SequenceLength";
		if (textLimitsMap.containsKey(seqLengthProperty+"."+orgId)){
			metadataBean.setSequenceLength(Integer.parseInt(textLimitsMap.get(seqLengthProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(seqLengthProperty)){
			metadataBean.setSequenceLength(Integer.parseInt(textLimitsMap.get(seqLengthProperty)));
		}
		else{
			logger.error(seqLengthProperty + " not present in the textLimitsMap");
		}
		

		String temparatureProperty = "ionml.QuestionGenerationV2UC.Temperature";
		if (textLimitsMap.containsKey(temparatureProperty+"."+orgId)){
			metadataBean.setTemperature(Float.parseFloat(textLimitsMap.get(temparatureProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(temparatureProperty)){
			metadataBean.setTemperature(Float.parseFloat(textLimitsMap.get(temparatureProperty)));
		}
		else{
			logger.error(temparatureProperty + " not present in the textLimitsMap");
		}
		
		String cosineProperty = "ionml.QuestionGenerationV2UC.CosineThreshold";
		if (textLimitsMap.containsKey(cosineProperty+"."+orgId)){
			metadataBean.setCosineThreshold(Float.parseFloat(textLimitsMap.get(cosineProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(cosineProperty)){
			metadataBean.setCosineThreshold(Float.parseFloat(textLimitsMap.get(cosineProperty)));
		}
		else{
			logger.error(cosineProperty + " not present in the textLimitsMap");
		}
		
		logger.info("metadataBean:"+ metadataBean);
		return metadataBean;
	}
	
	public MetadataBean fetchAsyncMetadataFromCacheBulkAuthoring(JSONObject textLimitJson, String orgId) throws JsonParseException, JsonMappingException, IOException{
		final Map<String, String> textLimitsMap = mapper.readValue(textLimitJson.toString(),new TypeReference<HashMap<String, String>>(){});
		logger.error("final textLimitsMap ::"+textLimitsMap);
		
		MetadataBean metadataBean = new MetadataBean();
		
		String chunkSizeProperty = "ionml.BulkItemQuestionGenerationUC.ChunkSize";
		if (textLimitsMap.containsKey(chunkSizeProperty+"."+orgId)){
			metadataBean.setChunkSize(Integer.parseInt(textLimitsMap.get(chunkSizeProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(chunkSizeProperty)){
			metadataBean.setChunkSize(Integer.parseInt(textLimitsMap.get(chunkSizeProperty)));
		}
		else{
			logger.error(chunkSizeProperty + " not present in the textLimitsMap");
		}
		
		String gptChunkSizeProperty = "ionml.BulkItemQuestionGenerationUC.GPT4o.ChunkSize";
		if (textLimitsMap.containsKey(gptChunkSizeProperty+"."+orgId)){
			metadataBean.setGpt4oChunkSize(Integer.parseInt(textLimitsMap.get(gptChunkSizeProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(gptChunkSizeProperty)){
			metadataBean.setGpt4oChunkSize(Integer.parseInt(textLimitsMap.get(gptChunkSizeProperty)));
		}
		else{
			logger.error(gptChunkSizeProperty + " not present in the textLimitsMap");
		}
		
		String shuffleChunksProperty = "ionml.BulkItemQuestionGenerationUC.GPT4o.shuffleChunks";
		if (textLimitsMap.containsKey(shuffleChunksProperty+"."+orgId)){
			metadataBean.setShuffleChunks(Integer.parseInt(textLimitsMap.get(shuffleChunksProperty+"."+orgId))!=0);
		}
		else if(textLimitsMap.containsKey(shuffleChunksProperty)){
			metadataBean.setShuffleChunks(Integer.parseInt(textLimitsMap.get(shuffleChunksProperty))!=0);
		}
		else{
			logger.error(shuffleChunksProperty + " not present in the textLimitsMap");
		}
		
		String topKProperty = "ionml.BulkItemQuestionGenerationUC.TopK";
		if (textLimitsMap.containsKey(topKProperty+"."+orgId)){
			metadataBean.setTopK(Integer.parseInt(textLimitsMap.get(topKProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(topKProperty)){
			metadataBean.setTopK(Integer.parseInt(textLimitsMap.get(topKProperty)));
		}
		else{
			logger.error(topKProperty + " not present in the textLimitsMap");
		}
		
		String seqLengthProperty = "ionml.BulkItemQuestionGenerationUC.SequenceLength";
		if (textLimitsMap.containsKey(seqLengthProperty+"."+orgId)){
			metadataBean.setSequenceLength(Integer.parseInt(textLimitsMap.get(seqLengthProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(seqLengthProperty)){
			metadataBean.setSequenceLength(Integer.parseInt(textLimitsMap.get(seqLengthProperty)));
		}
		else{
			logger.error(seqLengthProperty + " not present in the textLimitsMap");
		}
		

		String temparatureProperty = "ionml.BulkItemQuestionGenerationUC.Temperature";
		if (textLimitsMap.containsKey(temparatureProperty+"."+orgId)){
			metadataBean.setTemperature(Float.parseFloat(textLimitsMap.get(temparatureProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(temparatureProperty)){
			metadataBean.setTemperature(Float.parseFloat(textLimitsMap.get(temparatureProperty)));
		}
		else{
			logger.error(temparatureProperty + " not present in the textLimitsMap");
		}
		
		String cosineProperty = "ionml.BulkItemQuestionGenerationUC.CosineThreshold";
		if (textLimitsMap.containsKey(cosineProperty+"."+orgId)){
			metadataBean.setCosineThreshold(Float.parseFloat(textLimitsMap.get(cosineProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(cosineProperty)){
			metadataBean.setCosineThreshold(Float.parseFloat(textLimitsMap.get(cosineProperty)));
		}
		else{
			logger.error(cosineProperty + " not present in the textLimitsMap");
		}
		
		logger.info("metadataBean:"+ metadataBean);
		return metadataBean;
	}
	
	public MetadataBean fetchSyncMetadataFromCache(JSONObject textLimitJson, String orgId) throws JsonParseException, JsonMappingException, IOException{
		final Map<String, String> textLimitsMap = mapper.readValue(textLimitJson.toString(),new TypeReference<HashMap<String, String>>(){});
		logger.error("final textLimitsMap ::"+textLimitsMap);
		
		MetadataBean metadataBean = new MetadataBean();
		
		String chunkSizeProperty = "ionml.QuestionGenerationSync.ChunkSize";
		if (textLimitsMap.containsKey(chunkSizeProperty+"."+orgId)){
			metadataBean.setChunkSize(Integer.parseInt(textLimitsMap.get(chunkSizeProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(chunkSizeProperty)){
			metadataBean.setChunkSize(Integer.parseInt(textLimitsMap.get(chunkSizeProperty)));
		}
		else{
			logger.error("ionml.QuestionGenerationSync.ChunkSize not present in the textLimitsMap");
		}
		
		String gptChunkSizeProperty = "ionml.QuestionGenerationSync.GPT4o.ChunkSize";
		if (textLimitsMap.containsKey(gptChunkSizeProperty+"."+orgId)){
			metadataBean.setGpt4oChunkSize(Integer.parseInt(textLimitsMap.get(gptChunkSizeProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(gptChunkSizeProperty)){
			metadataBean.setGpt4oChunkSize(Integer.parseInt(textLimitsMap.get(gptChunkSizeProperty)));
		}
		else{
			logger.error("ionml.QuestionGenerationSync.GPT4o.ChunkSize not present in the textLimitsMap");
		}
		
		String shuffleChunksProperty = "ionml.QuestionGenerationSync.ShuffleChunks";
		if (textLimitsMap.containsKey(shuffleChunksProperty+"."+orgId)){
			metadataBean.setShuffleChunks(Integer.parseInt(textLimitsMap.get(shuffleChunksProperty+"."+orgId))!=0);
		}
		else if(textLimitsMap.containsKey(shuffleChunksProperty)){
			metadataBean.setShuffleChunks(Integer.parseInt(textLimitsMap.get(shuffleChunksProperty))!=0);
		}
		else{
			logger.error("ionml.QuestionGenerationSync.ShuffleChunks not present in the textLimitsMap");
		}
		
		String topKProperty = "ionml.QuestionGenerationSync.topK";
		if (textLimitsMap.containsKey(topKProperty+"."+orgId)){
			metadataBean.setTopK(Integer.parseInt(textLimitsMap.get(topKProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(topKProperty)){
			metadataBean.setTopK(Integer.parseInt(textLimitsMap.get(topKProperty)));
		}
		else{
			logger.error("ionml.QuestionGenerationSync.topK not present in the textLimitsMap");
		}
		
		String seqLengthProperty = "ionml.QuestionGenerationSync.SequenceLength";
		if (textLimitsMap.containsKey(seqLengthProperty+"."+orgId)){
			metadataBean.setSequenceLength(Integer.parseInt(textLimitsMap.get(seqLengthProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(seqLengthProperty)){
			metadataBean.setSequenceLength(Integer.parseInt(textLimitsMap.get(seqLengthProperty)));
		}
		else{
			logger.error("ionml.QuestionGenerationSync.SequenceLength not present in the textLimitsMap");
		}
		

		String temparatureProperty = "ionml.QuestionGenerationSync.Temperature";
		if (textLimitsMap.containsKey(temparatureProperty+"."+orgId)){
			metadataBean.setTemperature(Float.parseFloat(textLimitsMap.get(temparatureProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(temparatureProperty)){
			metadataBean.setTemperature(Float.parseFloat(textLimitsMap.get(temparatureProperty)));
		}
		else{
			logger.error("ionml.QuestionGenerationSync.Temperature not present in the textLimitsMap");
		}
		
		String cosineProperty = "ionml.QuestionGenerationSync.CosineThreshold";
		if (textLimitsMap.containsKey(cosineProperty+"."+orgId)){
			metadataBean.setCosineThreshold(Float.parseFloat(textLimitsMap.get(cosineProperty+"."+orgId)));
		}
		else if(textLimitsMap.containsKey(cosineProperty)){
			metadataBean.setCosineThreshold(Float.parseFloat(textLimitsMap.get(cosineProperty)));
		}
		else{
			logger.error(cosineProperty + " not present in the textLimitsMap");
		}
		
		logger.info("metadataBean:"+ metadataBean);
		return metadataBean;
	}
}

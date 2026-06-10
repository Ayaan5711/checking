package com.tcsion.ml.qsnauthorv2.inputtypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Strings;
import com.tcsion.ml.Utils.MlFrameworkConstants;
import com.tcsion.ml.filereader.Chunk;
import com.tcsion.ml.filereader.ChunkingStrategy;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.OutputBean;
import com.tcsion.ml.qsnauthorv2.engines.GPTBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.engines.HomeGrownBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.engines.GeminiBasedAuthoring;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.qsnauthorv2.util.QsnAuthConstants;
import com.tcsion.ml.qsnauthorv2.util.Utility;

public class TextBasedAuthoring implements AuthoringInterface {
	private static final Log logger = LogFactory.getLog(TextBasedAuthoring.class);
	private OutputBean outputBean = new OutputBean();
	private Utility utility = new Utility();
	private GPTBasedAuthoring qsnAuthGPT = new GPTBasedAuthoring();
	private HomeGrownBasedAuthoring qsnAuthHG = new HomeGrownBasedAuthoring();
	private GeminiBasedAuthoring qsnAuthGemini = new GeminiBasedAuthoring();	 
	
	@Override
	public OutputBean questionAuthoring(InputBean inputBean) throws Exception {
		if (inputBean.getUserInput()==null || Strings.isNullOrEmpty(inputBean.getUserInput().trim())) {
	        logger.error("User has not provided any input. Trying to generate based on directives.");
	        inputBean.setUserInput("");
		}
		
		List<Chunk> allChunks = chunkContent(inputBean.getUserInput(), getChunkingStrategy(), inputBean.getUserId());
		logger.error("all chunks : "+allChunks);
		inputBean.setRetrievedChunks(allChunks);
		
		logger.error("After processing chunks the final input bean : "+inputBean.toString());
		
		if (inputBean.getAIvendorCredNodeMap()==null || inputBean.getAIvendorCredNodeMap().isEmpty() ||
			inputBean.getAIvendorCredNodeMap().containsKey(MlFrameworkConstants.TCS_iON_AI_ML)||
			inputBean.getAIvendorCredNodeMap().containsKey(MlFrameworkConstants.QUESTION_GENERATION_OPENSOURCE))
		{
			outputBean = qsnAuthHG.authoringByHGAI(inputBean);
		} 
		else if (inputBean.getAIvendorCredNodeMap().containsKey(GenAIFrameworkConstants.GEMINI_3_5_FLASH)) {
	    outputBean = qsnAuthGemini.authoringByGemini(inputBean);
		} 
		else {
			outputBean = qsnAuthGPT.authoringByGPT(inputBean);
		}
		logger.error(outputBean);
		return outputBean;
	}
	
	public List<Chunk> chunkContent(String context, ChunkingStrategy strategy, String userId) throws IOException {
	    List<Chunk> chunks = new ArrayList<>();

	    String content = context.trim().replaceAll("[^\\x00-\\x7F]", " ");
	    int globalChunkId = 1;
	    String chunkingLevel = strategy.getChunkingLevel().toLowerCase(Locale.ROOT);
	    int itemCount = strategy.getItemCount();

	    List<String> units = new ArrayList<>();

	    if (chunkingLevel.equals("paragraph")) {
	        // Split by double newlines (basic paragraph delimiter)
	        String[] paragraphs = content.split("(\\r?\\n){2,}");
	        for (String paragraph : paragraphs) {
	            String cleanParagraph = paragraph.trim();
	            if (!cleanParagraph.isEmpty()) {
	                units.add(cleanParagraph);
	            }
	        }
	    } else if (chunkingLevel.equals("sentence")) {
	        // Split by sentence-ending punctuation followed by whitespace and capital letter
	        String[] sentences = content.split("(?<=[.!?])\\s+(?=[A-Z])");
	        for (String sentence : sentences) {
	            String cleanSentence = sentence.trim();
	            if (!cleanSentence.isEmpty()) {
	                units.add(cleanSentence);
	            }
	        }
	    } else {
	        throw new IllegalArgumentException("Unsupported chunking level: " + chunkingLevel);
	    }

	    // Create chunks
	    StringBuilder chunkBuilder = new StringBuilder();
	    int counter = 0;
	    for (String unit : units) {
	        chunkBuilder.append(unit).append(" ");
	        counter++;

	        if (counter == itemCount) {
	            chunks.add(new Chunk(String.valueOf(globalChunkId++), chunkBuilder.toString().trim(), null, null));
	            chunkBuilder.setLength(0);
	            counter = 0;
	        }
	    }

	    // Add remaining content if any
	    if (chunkBuilder.length() > 0) {
	        chunks.add(new Chunk(String.valueOf(globalChunkId), chunkBuilder.toString().trim(), null, null));
	    }

	    return chunks;
	}
	
	private ChunkingStrategy getChunkingStrategy(){
		ChunkingStrategy strategy = new ChunkingStrategy();
		String chunkLevel = "sentence";
		int sentencesPerChunk = 20;
		String chunkLevelFromDB = utility.getGblGlobalProperty("ionml.dataindexer.chunk.level.context");
		chunkLevel = Strings.isNullOrEmpty(chunkLevelFromDB) ? "sentence" : chunkLevelFromDB;
		String sentencesPerChunkFromDB = utility.getGblGlobalProperty("ionml.dataindexer.chunk.sentences.context");
		try {
			sentencesPerChunk = Integer.parseInt(sentencesPerChunkFromDB);
        } catch (Exception e) {
        	logger.error("Exception in integer parsing:"+ e);
        	sentencesPerChunk = 20;
        }
		strategy.setChunkingLevel(chunkLevel);
		strategy.setItemCount(sentencesPerChunk);
        return strategy;
	}
}

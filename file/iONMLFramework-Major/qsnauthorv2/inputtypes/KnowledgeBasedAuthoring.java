package com.tcsion.ml.qsnauthorv2.inputtypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Strings;
import com.tcsion.ml.Utils.MlFrameworkConstants;
import com.tcsion.ml.fileindexer.DataRetriever;
import com.tcsion.ml.filereader.Chunk;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.OutputBean;
import com.tcsion.ml.qsnauthorv2.engines.GPTBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.engines.HomeGrownBasedAuthoring;
import com.tcsion.ml.qsnauthorv2.engines.GeminiBasedAuthoring;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.qsnauthorv2.util.QsnAuthConstants;
import com.tcsion.ml.qsnauthorv2.util.SerachTextGenerator;
import com.tcsion.ml.qsnauthorv2.util.Utility;
import com.tcsion.ml.search.solr.FetchSolarConfig;
import com.tcsion.ml.search.solr.SolrAdminClient;

public class KnowledgeBasedAuthoring implements AuthoringInterface {
	private static final Log logger = LogFactory.getLog(KnowledgeBasedAuthoring.class);
	private OutputBean outputBean = new OutputBean();
	private Utility utility = new Utility();
	private GPTBasedAuthoring qsnAuthGPT = new GPTBasedAuthoring();
	private HomeGrownBasedAuthoring qsnAuthHG = new HomeGrownBasedAuthoring();
	private GeminiBasedAuthoring qsnAuthGemini = new GeminiBasedAuthoring();	 

	private DataRetriever retriever = new DataRetriever();
	private SerachTextGenerator serachTextGenerator = new SerachTextGenerator();
	
	@SuppressWarnings("unchecked")
	@Override
	public OutputBean questionAuthoring(InputBean inputBean) throws Exception{
		
		if (inputBean.getFilePaths()==null || inputBean.getFilePaths().length==0) {
			outputBean.setOutput(null);
			outputBean.setMessage(QsnAuthConstants.NO_FILES_PROVIDED);
			outputBean.setReturnCode(QsnAuthConstants.QSNGEN_985);
			return outputBean;
		}
		
		for (String filePath: inputBean.getFilePaths()){
			if (!utility.validateFileExtension(filePath)) {
	            logger.error("File type not supported - " + filePath);
	        }
		}
		
		final String collectionName = utility.prepareCollectionName(inputBean.getAppId(), inputBean.getOrgId(), inputBean.getEntity());
		logger.error("collectionName: " + collectionName);
		
		final String solrUrl=FetchSolarConfig.fetchAndStoreProperty(MlFrameworkConstants.ION_SEARCH_URL);
		SolrAdminClient adminclient = new SolrAdminClient(solrUrl);
		
		Map<String, Object> collectionMap = adminclient.listCollections();
		List<String> collections = (ArrayList<String>) collectionMap.getOrDefault("collections", null);
		if (collections == null || !collections.contains(collectionName)) {
			logger.error("Collection name not found");
			outputBean.setOutput(null);
			outputBean.setMessage(QsnAuthConstants.FILES_NOT_PREPARED_FOR_AUTHORING);
			outputBean.setReturnCode(QsnAuthConstants.QSNGEN_988);
			return outputBean;
		}
		
		String filters = buildInFilter("filePath", inputBean.getFilePaths());
		logger.error("filters: " + filters);
		
//		StringBuilder searchTextBuilder = new StringBuilder();
		
//		if (inputBean.getUserInput() != null && !inputBean.getUserInput().trim().isEmpty()) {
//		    searchTextBuilder.append(inputBean.getUserInput());
//		}
//		if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
//		    if (searchTextBuilder.length() > 0) {
//		        searchTextBuilder.append(" ");
//		    }
//		    
//		    
//		    searchTextBuilder.append(inputBean.getDirectives().toString());
//		}
		
		String searchText = serachTextGenerator.QueryGenerator(inputBean);
		logger.error("searchtext is : "+searchText);

		List<Chunk> retrievedChunks = retriever.retrieveDataFromCollection(solrUrl, collectionName, 
				searchText, inputBean.getMetadataBean().getCosineThreshold(), 
				filters, inputBean.getRequestId(), inputBean.getPort());
		logger.error("retrievedChunks: " + retrievedChunks);
		inputBean.setRetrievedChunks(retrievedChunks);
		
		if(retrievedChunks==null || retrievedChunks.isEmpty()){
			logger.error("No match found in knowledge base.");
			outputBean.setOutput(null);
			outputBean.setMessage(QsnAuthConstants.NO_MATCH_FOUND_FOR_THE_TOPIC);
			outputBean.setReturnCode(QsnAuthConstants.QSNGEN_989);
			return outputBean;
		}
		
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
		return outputBean;
	}
	
	public static String buildInFilter(String fieldName, String[] values) {
	    if (values == null || values.length == 0) {
	        throw new IllegalArgumentException("Values array cannot be null or empty");
	    }

	    // Escape double quotes in values and wrap each in quotes
	    String joinedValues = Arrays.stream(values)
	            .map(v -> "\"" + v.replace("\"", "\\\"") + "\"")
	            .collect(Collectors.joining(" ")); // space = OR in Solr

	    return String.format("%s:(%s)", fieldName, joinedValues);
	}
	
}

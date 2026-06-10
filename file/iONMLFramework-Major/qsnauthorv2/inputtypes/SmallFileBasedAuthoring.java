package com.tcsion.ml.qsnauthorv2.inputtypes;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Strings;
import com.tcsion.ml.Utils.MlFrameworkConstants;
import com.tcsion.ml.Utils.MlFrameworkUtils;
import com.tcsion.ml.fileindexer.DataIndexer;
import com.tcsion.ml.fileindexer.DataRetriever;
import com.tcsion.ml.fileindexer.DataIndexOutputBean;
import com.tcsion.ml.filereader.Chunk;
import com.tcsion.ml.filereader.ChunkingStrategy;
import com.tcsion.ml.filereader.FileReaderInterface;
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

public class SmallFileBasedAuthoring implements AuthoringInterface {
	private static final Log logger = LogFactory.getLog(SmallFileBasedAuthoring.class);
	private OutputBean outputBean = new OutputBean();
	private Utility utility = new Utility();
	private MlFrameworkUtils mlUtility = new MlFrameworkUtils();
	private DataIndexer indexer = new DataIndexer();
	private DataRetriever retriever = new DataRetriever();
	private GPTBasedAuthoring qsnAuthGPT = new GPTBasedAuthoring();
	private HomeGrownBasedAuthoring qsnAuthHG = new HomeGrownBasedAuthoring();
	private GeminiBasedAuthoring qsnAuthGemini = new GeminiBasedAuthoring();	 
	private SerachTextGenerator serachTextGenerator = new SerachTextGenerator();

	
	@Override
	public OutputBean questionAuthoring(InputBean inputBean) throws Exception{
		
		if (!mlUtility.validateMLFilePath(inputBean.getFilePaths()[0])) {
            logger.error("File does not exist - " + inputBean.getFilePaths()[0]);
            outputBean.setMessage(QsnAuthConstants.FILE_PATH_DOES_NOT_EXIST);
            outputBean.setReturnCode(QsnAuthConstants.QSNGEN_996);
            return outputBean;
        }
		if (!utility.validateFileExtension(inputBean.getFilePaths()[0])) {
            logger.error("File extension not allowed for file - " + inputBean.getFilePaths()[0]);
            outputBean.setMessage(QsnAuthConstants.INVALID_FILE_EXTENSION);
            outputBean.setReturnCode(QsnAuthConstants.QSNGEN_995);
            return outputBean;
        }
		
		List<Chunk> retrievedChunks = null;
		
		if (!(inputBean.getDirectives()!=null && inputBean.getDirectives().isEmpty()) || 
				!Strings.isNullOrEmpty(inputBean.getUserInput())) {
			logger.error("Retrieving matching contents from vector database.");
			final String solrUrl=FetchSolarConfig.fetchAndStoreProperty(MlFrameworkConstants.ION_SEARCH_URL);
			final int numOfShards = Integer.parseInt(FetchSolarConfig.fetchAndStoreProperty(MlFrameworkConstants.ION_SEARCH_NUM_OF_SHARDS));
			final int replicationFactor = Integer.parseInt(FetchSolarConfig.fetchAndStoreProperty(MlFrameworkConstants.ION_SEARCH_NUM_OF_REPLICAS));
	
			String collectionName = "small_file_" + utility.prepareCollectionName(inputBean.getAppId(), inputBean.getOrgId(), inputBean.getEntity()) + "_"+ inputBean.getFilePaths()[0].toLowerCase(Locale.ROOT).replaceAll("^.*[\\\\/]", "").replaceAll("[^a-z0-9_-]", "_");
	
			SolrAdminClient adminclient = new SolrAdminClient(solrUrl);
			int collectionCreationStatusCode = indexer.createCollectionIfNotExistsAndUpdateSchema(adminclient, 
														collectionName, numOfShards, replicationFactor);
			if (collectionCreationStatusCode==501) {
				outputBean.setMessage(QsnAuthConstants.UNABLE_TO_CREATE_VECTOR_STORE);
				outputBean.setReturnCode(QsnAuthConstants.QSNGEN_501);
				return outputBean;
			} else if (collectionCreationStatusCode==502){
				outputBean.setMessage(QsnAuthConstants.UNABLE_TO_UPDATE_SCHEMA);
				outputBean.setReturnCode(QsnAuthConstants.QSNGEN_502);
				return outputBean;
			} else if(collectionCreationStatusCode==201){
				logger.error("collectionName: " + collectionName + " already exists");
			} else {
				logger.error("collectionName: " + collectionName + " created successfully");
			}
			
			DataIndexOutputBean ragOutputBean = indexer.processAndSendToVectorDB(
					inputBean.getFilePaths()[0],collectionName, null, 
					solrUrl, inputBean.getRequestId(), inputBean.getPort());
			logger.error("ragOutputBean: " + ragOutputBean);
			
//			StringBuilder searchTextBuilder = new StringBuilder();
//			if (inputBean.getUserInput() != null && !inputBean.getUserInput().trim().isEmpty()) {
//			    searchTextBuilder.append(inputBean.getUserInput());
//			}
//			if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
//			    if (searchTextBuilder.length() > 0) {
//			        searchTextBuilder.append(" ");
//			    }
//			    searchTextBuilder.append(inputBean.getDirectives().toString());
//			}
	
	
			/*StringBuilder searchTextBuilder = new StringBuilder();
			if (inputBean.getDirectives() != null && !inputBean.getDirectives().isEmpty()) {
			    org.json.simple.JSONObject simpleDirectives = inputBean.getDirectives();
			    org.json.JSONObject directivesJson = new org.json.JSONObject(simpleDirectives.toJSONString());

			    Iterator<String> keys = directivesJson.keys();
			    while (keys.hasNext()) {
			        String key = keys.next();
			        org.json.JSONObject directive = directivesJson.getJSONObject(key);
			        if (directive.optBoolean("mandatory", false)) {
			            org.json.JSONArray values = directive.optJSONArray("value");
			            if (values != null) {
			                for (int i = 0; i < values.length(); i++) {
			                    if (searchTextBuilder.length() > 0) 
			                    	searchTextBuilder.append(", ");
			                    searchTextBuilder.append(values.getString(i));
			                }
			            }
			        }
			    }
			}

			String searchText = searchTextBuilder.toString();*/
			
			

			String searchText = serachTextGenerator.QueryGenerator(inputBean);

			logger.error("searchtext is : "+searchText);

			
			
			retrievedChunks = retriever.retrieveDataFromCollection(solrUrl, collectionName, 
					searchText, inputBean.getMetadataBean().getCosineThreshold(), "", 
					inputBean.getRequestId(), inputBean.getPort());
			logger.error("retrievedChunks: " + retrievedChunks);
			
			// Delete collection if newly created for small file
			if (collectionCreationStatusCode==200){
				Map<String, Object> response = adminclient.deleteCollection(collectionName);
				logger.error("Collection deletion response: " + response);
			}
			
			/*if(retrievedChunks==null || retrievedChunks.isEmpty()){
				outputBean.setOutput(null);
				outputBean.setMessage(QsnAuthConstants.NO_MATCH_FOUND_FOR_THE_TOPIC);
				outputBean.setReturnCode(QsnAuthConstants.QSNGEN_989);
				return outputBean;
			}*/
			
			if(retrievedChunks==null || retrievedChunks.isEmpty()){
				logger.error("No match found. Generating questions based on whole document.");
				FileReaderInterface reader = utility.getReaderForFile(inputBean.getFilePaths()[0]);
				ChunkingStrategy strategy = utility.getChunkingStrategy(inputBean.getFilePaths()[0]);
				retrievedChunks = reader.readFile(inputBean.getFilePaths()[0], strategy);
			}
			
			
		} 
		else {
			logger.error("Not going to check vector DB. Generating from file directly.");
			FileReaderInterface reader = utility.getReaderForFile(inputBean.getFilePaths()[0]);
			ChunkingStrategy strategy = utility.getChunkingStrategy(inputBean.getFilePaths()[0]);
			retrievedChunks = reader.readFile(inputBean.getFilePaths()[0], strategy);
			logger.error("retrievedChunks: " + retrievedChunks);
			
			if(retrievedChunks==null || retrievedChunks.isEmpty()){
				outputBean.setOutput(null);
				outputBean.setMessage(QsnAuthConstants.FILE_EMPTY_OR_CORRUPTED);
				outputBean.setReturnCode(QsnAuthConstants.QSNGEN_986);
				return outputBean;
			}
		}
		
		inputBean.setRetrievedChunks(retrievedChunks);
		
		if (inputBean.getAIvendorCredNodeMap() == null || inputBean.getAIvendorCredNodeMap().isEmpty() ||
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
}

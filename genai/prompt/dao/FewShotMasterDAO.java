package com.tcs.genai.prompt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.tcs.genai.prompt.cache.GenAIPromptDetailsCacheLoaderService;
import com.tcs.genai.prompt.utils.GenAIFWQueryUtils;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.metadatahandler.util.DBConnectionUtility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;




public class FewShotMasterDAO {

	private DBConnectionUtility dbConnectionObj = null;
	
	private static final Log logger = LogFactory.getLog(UseCasePromptMappingDAO.class);
	
	public FewShotMasterDAO(DBConnectionUtility dbConnectionObj) {
		this.dbConnectionObj =  dbConnectionObj;
	}
	
	public JSONArray fetchFewshotDetailsFromDB(String fewshots_id) {
			
			String fewshot_name = null;
			String user_message = null;
			String assistant_message = null;
			
			JSONArray prompt_details = new JSONArray();
			JSONObject globalPropertyPropNameMap = new JSONObject();
			
				
			String[] stringArray = fewshots_id.split(",");
	        int[] fewshots_id_list = new int[stringArray.length];
	        for (int i = 0; i < stringArray.length; i++) {
	        	fewshots_id_list[i] = Integer.parseInt(stringArray[i]);
	        }
	        String query = GenAIFWQueryUtils.FEWSHOT_DETAILS_QUERY;
			
	        for (int i = 0; i< fewshots_id_list.length; i++) {
	        	
	   
					if(dbConnectionObj != null){
						logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "Going to make customer global connection");
						try(Connection conn =  this.dbConnectionObj.getCustomerGlobalConnection();
							PreparedStatement preparedStatement = conn.prepareStatement(query);) {				
							preparedStatement.setLong(1, fewshots_id_list[i]);
							
							try(ResultSet resultSet = preparedStatement.executeQuery();){
								while (resultSet.next()) {
									fewshot_name = resultSet.getString("fewshot_name");
									user_message = resultSet.getString("user_message");
									assistant_message = resultSet.getString("assistant_message");
									globalPropertyPropNameMap.put("fewshot_name", fewshot_name);
									globalPropertyPropNameMap.put("user_message", user_message);
									globalPropertyPropNameMap.put("assistant_message", assistant_message);
								}
								prompt_details.add(globalPropertyPropNameMap);
								GenAIPromptDetailsCacheLoaderService.fewShotDetailsPromptCache.put(fewshots_id_list[i], prompt_details);
							}
						} catch (Exception e) {
							logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION+"Exception occured while adding data from result set {} " , e);
						}
					} else {
						logger.error("DB connection not initialized correctly");
					}
	        	
	        }
			
			
			
			return prompt_details;
		}
}

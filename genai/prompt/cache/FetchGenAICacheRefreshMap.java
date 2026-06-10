package com.tcs.genai.prompt.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tcs.genai.prompt.utils.GenAIFWQueryUtils;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.metadatahandler.service.FetchGlobalPropertyValue;
import com.tcsion.ml.metadatahandler.util.DBConnectionUtility;

public class FetchGenAICacheRefreshMap {
	
	private DBConnectionUtility dbConnectionObj = new DBConnectionUtility();

	private static final Log logger = LogFactory.getLog(FetchGlobalPropertyValue.class);
	public FetchGenAICacheRefreshMap() {
		//Empty constructor
	}
	
	public Map<String, String> getCacheRefreshInterval(String property_name) {
		Map<String,String> cacheRefreshDetailsMap = null;
		if(GenAIPromptDetailsCacheLoaderService.genAICacheRefreshIntervalMap.getIfPresent(property_name) != null){
			cacheRefreshDetailsMap = GenAIPromptDetailsCacheLoaderService.genAICacheRefreshIntervalMap.getIfPresent(property_name);
		}else{
			cacheRefreshDetailsMap = fetchPropertyValueFromGlobalPropertiesFromDB();
			//GenAIPromptDetailsCacheLoaderService.genAICacheRefreshIntervalMap.put(GenAIFrameworkConstants.GENAI_CACHE_REFRESH_INTERVAL_PROPERTY, cacheRefreshDetailsMap);
		}
		return cacheRefreshDetailsMap;
	}
	
	
	public Map<String, String> fetchPropertyValueFromGlobalPropertiesFromDB() {
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		Connection conn = null;
		String propname = null;
		String propvalue = null;
		Map<String,String> globalPropertyPropNameMap = new HashMap<>();
		
		try {
			if(dbConnectionObj != null){
				String query = GenAIFWQueryUtils.GENAI_CACHE_INTERVAL;
				logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "Going to make customer global connection");

				conn =  this.dbConnectionObj.getCustomerGlobalConnection();
				preparedStatement = conn.prepareStatement(query);
				resultSet = preparedStatement.executeQuery();
				while (resultSet.next()) {					
					propvalue = resultSet.getString("propvalue");
					globalPropertyPropNameMap.put(GenAIFrameworkConstants.GENAI_CACHE_REFRESH_INTERVAL_PROPERTY, propvalue);
				}
				
			}else{
				logger.info("DB connection not initialized correctly");
		}
			
		} catch (Exception e) {
			logger.error("Exception occured while fetching third party allowed input type list {} " , e);
		}finally{
			GenAIPromptDetailsCacheLoaderService.genAICacheLoaderCloseConnection(resultSet, preparedStatement, conn);
		}
		
		
		return globalPropertyPropNameMap;
	}
	
	

}

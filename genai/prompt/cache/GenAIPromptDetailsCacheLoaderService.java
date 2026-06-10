package com.tcs.genai.prompt.cache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.metadatahandler.util.DBConnectionUtility;

public class GenAIPromptDetailsCacheLoaderService {

	public DBConnectionUtility dbConnectionObj;

	// Code to dynamically refresh vendor cache refresh time
	public static Map<String, String> promptMappingsCacheRefreshIntervalMap = null;
	public static String cacheRefreshInterval = null;
	public static TimeUnit timeUint = TimeUnit.SECONDS;
	public static long timeValue = 24*60*60;
	public static Cache<String, Map<String, String>> genAICacheRefreshIntervalMap = CacheBuilder.newBuilder().expireAfterWrite(timeValue, timeUint).build();

	private static final Log logger = LogFactory
			.getLog(GenAIPromptDetailsCacheLoaderService.class);

	public GenAIPromptDetailsCacheLoaderService(
			final DBConnectionUtility dbConnectionObj) {
		try {
			this.dbConnectionObj = dbConnectionObj;

		} catch (Exception e) {
			logger.error("Exception occured : {} ", e);
		}
	}

	static {
		try {
			promptMappingsCacheRefreshIntervalMap = new FetchGenAICacheRefreshMap()
					.getCacheRefreshInterval(GenAIFrameworkConstants.GENAI_CACHE_REFRESH_INTERVAL_PROPERTY);

			if (promptMappingsCacheRefreshIntervalMap != null
					&& promptMappingsCacheRefreshIntervalMap
							.containsKey(GenAIFrameworkConstants.GENAI_CACHE_REFRESH_INTERVAL_PROPERTY)) {
				cacheRefreshInterval = promptMappingsCacheRefreshIntervalMap
						.get(GenAIFrameworkConstants.GENAI_CACHE_REFRESH_INTERVAL_PROPERTY);
				timeValue = Long.parseLong(cacheRefreshInterval);
				logger.info("timeValue: " + timeValue);
			} else {
				// Time value should be in seconds
				timeValue = 3 * 60 * 60;
				logger.info("No values fetched from DB for cache refresh so using timeValue: "
						+ timeValue);
			}
		} catch (Exception e) {
			logger.error(
					"Exception occurs during cache refresh interval value fetching from DB. Exception {} ",
					e);
			timeValue = 3 * 60 * 60;
			logger.info("Setting refresh time interval to 3 hours.");

		}
	}

	public static final Cache<String, String> promptmappingCache = CacheBuilder
			.newBuilder().expireAfterWrite(timeValue, timeUint).build();
	// Guava Caches for different maps
	public static final Cache<String, JSONObject> masterDetailsPromptCache = CacheBuilder
			.newBuilder().expireAfterWrite(timeValue, timeUint).build();

	public static final Cache<String, JSONArray> guardRailCombinedCache = CacheBuilder
			.newBuilder().expireAfterWrite(timeValue, timeUint).build();

	public static final Cache<String, JSONArray> guardRailDetailsPromptCache = CacheBuilder
			.newBuilder().expireAfterWrite(timeValue, timeUint).build();

	public static final Cache<Integer, JSONArray> fewShotDetailsPromptCache = CacheBuilder
			.newBuilder().expireAfterWrite(timeValue, timeUint).build();

	public static final Cache<String, JSONArray> fewShotCombinedCache = CacheBuilder
			.newBuilder().expireAfterWrite(timeValue, timeUint).build();
	/*public static Cache<String, Map<String, String>> genAICacheRefreshIntervalMap = CacheBuilder
			.newBuilder().expireAfterWrite(timeValue, timeUint).build();*/

	
	public static void genAICacheLoaderCloseConnection(ResultSet resultSet, PreparedStatement preparedStatement, Connection conn){
		if(resultSet != null){
			try {
				resultSet.close();
				//logger.error("Resultset closed.");
			} catch (SQLException e) {
				logger.error(GenAIFrameworkConstants.RESULT_EXCEPTION_MESSAGE+ e+ logger);
			}
		}
		if(preparedStatement != null){
			try {
				preparedStatement.close();
				logger.info("preparedStatement closed.");
			} catch (SQLException e) {
				logger.error(GenAIFrameworkConstants.PREPARED_STATEMENT_EXCEPTION_MESSAGE + e + logger);
			}
		}
		if(conn != null){
			try {
				conn.close();
				//logger.error("conn closed.");
			} catch (SQLException e) {
				logger.error(GenAIFrameworkConstants.GLOBAL_CONNECTION_EXCEPTION_MESSAGE +  e + logger);
			}
		}
	}
}
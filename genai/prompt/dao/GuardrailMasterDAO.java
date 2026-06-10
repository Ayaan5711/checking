package com.tcs.genai.prompt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tcs.genai.prompt.cache.GenAIPromptDetailsCacheLoaderService;
import com.tcs.genai.prompt.utils.GenAIFWQueryUtils;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.metadatahandler.util.DBConnectionUtility;

public class GuardrailMasterDAO {

	private DBConnectionUtility dbConnectionObj = null;

	private JSONArray guardRailDetailsCache = null;

	private static final Log logger = LogFactory
			.getLog(UseCasePromptMappingDAO.class);

	public GuardrailMasterDAO(DBConnectionUtility dbConnectionObj) {
		this.dbConnectionObj = dbConnectionObj;
	}

	@SuppressWarnings("unchecked")
	public JSONArray fetchGuardrailDetails(String guardrail_id) {
		JSONArray guardRailsList = new JSONArray();
		String[] guardrailList = guardrail_id.split(",");	

		for(String guardRaildId :  guardrailList ){
			
			guardRaildId = guardRaildId.trim();
			try{
				guardRailDetailsCache = GenAIPromptDetailsCacheLoaderService.guardRailDetailsPromptCache.getIfPresent(guardRaildId);

			}catch(Exception e){
//				logger.error("guardRailDetailsCache does not contains data it is null"+ guardRailDetailsCache);
			}  
                // Use cached guardrail details
			JSONObject guardRailDetails  = fetchGuardRailFromTable(Long.parseLong(guardRaildId));
			guardRailsList.add(guardRailDetails);

			GenAIPromptDetailsCacheLoaderService.guardRailDetailsPromptCache.put(guardRaildId, guardRailsList);
			}
		// Cache guardrail details

		return guardRailsList;
	}

	@SuppressWarnings("unchecked")
	private JSONObject fetchGuardRailFromTable(long guardRaildId) {
		// TODO Auto-generated method stub
		JSONObject guardRailsDetails = new JSONObject();
		String query = GenAIFWQueryUtils.GUARDRAIL_DETAILS_QUERY;
		logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "Going to make customer global connection");

		try (Connection conn = this.dbConnectionObj.getCustomerGlobalConnection();
				PreparedStatement preparedStatement = conn
						.prepareStatement(query)) {

			preparedStatement.setLong(1, guardRaildId);

			try (ResultSet resultSet = preparedStatement.executeQuery();) {
				while (resultSet.next()) {
					guardRailsDetails.put(GenAIFWQueryUtils.GUARD_RAILS_NAME,
							resultSet.getString("guardrail_name"));
					guardRailsDetails.put(
							GenAIFWQueryUtils.GUARD_RAILS_INSTRUCTION,
							resultSet.getString("guardrail_instruction"));
				}
			}
		} catch (Exception e) {
			logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION
					+ " Exception occured {} " + e);
		}

		return guardRailsDetails;
	}

}

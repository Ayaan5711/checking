package com.tcs.genai.prompt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.tcs.genai.prompt.bean.ResponseMaster;
import com.tcs.genai.prompt.cache.GenAIPromptDetailsCacheLoaderService;
import com.tcs.genai.prompt.utils.GenAIFWQueryUtils;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.metadatahandler.util.DBConnectionUtility;

public class PromptMasterDAO {
    private static final Log logger = LogFactory.getLog(PromptMasterDAO.class);
    private final DBConnectionUtility dbConnectionUtility;
    ResponseMaster responseMasterObject = new ResponseMaster();
    

    public PromptMasterDAO(DBConnectionUtility dbConnectionUtility, ResponseMaster responseMasterObject) {
        this.dbConnectionUtility = dbConnectionUtility;
        this.responseMasterObject = responseMasterObject;
    }

    public JSONObject fetchPromptDetailsFromMasterPromptTable(long promptId) {
        try {
            JSONObject cachedData = getCachedPromptDetails(promptId);
            if (cachedData != null) {
                return cachedData;
            }
            return fetchPromptDetailsFromDB(promptId);
        } catch (Exception e) {
            logger.error(GenAIFrameworkConstants.LOGGER_CACHE+"Exception occurred while fetching prompt details: ", e);
            return null;
        }
    }

    private JSONObject getCachedPromptDetails(long promptId) {
        try {
            JSONObject cacheData = GenAIPromptDetailsCacheLoaderService.masterDetailsPromptCache.getIfPresent(String.valueOf(promptId));
            if (cacheData != null) {
                logger.info(GenAIFrameworkConstants.LOGGER_CACHE+GenAIFrameworkConstants.LOGGER_CACHE+"Fetching prompt details from cache for PromptID: " + promptId);
                return cacheData;
            }
        } catch (Exception e) {
            logger.error(GenAIFrameworkConstants.LOGGER_CACHE+"Error fetching cache data: ", e);
        }
        return null;
    }

    public JSONObject fetchPromptDetailsFromDB(long promptId) {
        String query = GenAIFWQueryUtils.PROMPT_DETAILS_QUERY;
        JSONObject masterPromptDetails = new JSONObject();
		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + "Going to make customer global connection");

        try (Connection conn = dbConnectionUtility.getCustomerGlobalConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setLong(1, promptId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    masterPromptDetails.put(GenAIFWQueryUtils.MASTER_PROMPT_ID, promptId);
                    masterPromptDetails.put(GenAIFWQueryUtils.MASTER_PROMPT_NAME, resultSet.getString(GenAIFWQueryUtils.MASTER_PROMPT_NAME));
                    masterPromptDetails.put(GenAIFWQueryUtils.MASTER_PROMPT_TEMPLATE, resultSet.getString(GenAIFWQueryUtils.MASTER_PROMPT_TEMPLATE));

                    String guardrailId = resultSet.getString(GenAIFWQueryUtils.MASTER_PROMPT_GUARDRAIL);
                    String fewshotsExampleId = resultSet.getString(GenAIFWQueryUtils.MASTER_PROMPT_FEWSHOT);
                    
                    if (isValidId(guardrailId)) {
                        masterPromptDetails.put(GenAIFWQueryUtils.CACHE_MASTER_PROMPT_GUARDRAIL_ID, guardrailId);
                        masterPromptDetails.put(GenAIFWQueryUtils.MASTER_PROMPT_GUARDRAIL, fetchGuardrailDetails(guardrailId));
                    }
                    
                    if (isValidId(fewshotsExampleId)) {
                        masterPromptDetails.put(GenAIFWQueryUtils.CACHE_MASTER_PROMPT_FEWSHOT_ID, fewshotsExampleId);
                        masterPromptDetails.put(GenAIFWQueryUtils.MASTER_PROMPT_FEWSHOT, fetchFewshotDetails(fewshotsExampleId));
                    }
                    
                    GenAIPromptDetailsCacheLoaderService.masterDetailsPromptCache.put(String.valueOf(promptId), masterPromptDetails);
                }
            }
        } catch (Exception e) {
            logger.error(GenAIFrameworkConstants.LOGGER_CACHE+"Error fetching prompt details from DB: ", e);
            responseMasterObject.setStatus(GenAIFrameworkConstants.GEN_405);
            responseMasterObject.setMsg(GenAIFrameworkConstants.GEN_405_MSG);
        }
        return masterPromptDetails;
    }

    public JSONArray fetchGuardrailDetails(String guardrailId){
        try {
            JSONArray cacheData = GenAIPromptDetailsCacheLoaderService.guardRailDetailsPromptCache.getIfPresent(guardrailId);
            if (cacheData != null) {
                return cacheData;
            }
            return new GuardrailMasterDAO(dbConnectionUtility).fetchGuardrailDetails(guardrailId);
        } catch (Exception e) {
            logger.error(GenAIFrameworkConstants.LOGGER_CACHE+"Error fetching guardrail details: ", e);
            responseMasterObject.setStatus(GenAIFrameworkConstants.GEN_406);
            responseMasterObject.setMsg(GenAIFrameworkConstants.GEN_406_MSG);
            return new JSONArray();
        }
    }

    public JSONArray fetchFewshotDetails(String fewshotsExampleId) {
        try {
            JSONArray cacheData = GenAIPromptDetailsCacheLoaderService.fewShotDetailsPromptCache.getIfPresent(Integer.parseInt(fewshotsExampleId));
            if (cacheData != null) {
                return cacheData;
            }
            return new FewShotMasterDAO(dbConnectionUtility).fetchFewshotDetailsFromDB(fewshotsExampleId);
        } catch (Exception e) {
            logger.error(GenAIFrameworkConstants.LOGGER_CACHE +"Error fetching fewshot details: ", e);
            responseMasterObject.setStatus(GenAIFrameworkConstants.GEN_407);
            responseMasterObject.setMsg(GenAIFrameworkConstants.GEN_407_MSG);
            return new JSONArray();
        }
    }


    public boolean isValidId(String id) {
        return id != null && !id.equals("0");
    }
		

}

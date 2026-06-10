package com.tcs.genai.prompt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.cache.Cache;
import com.tcs.genai.prompt.bean.ResponseMaster;
import com.tcs.genai.prompt.cache.GenAIPromptDetailsCacheLoaderService;
import com.tcs.genai.prompt.utils.GenAIFWQueryUtils;
import com.tcs.genai.prompt.utils.GenAIFrameworkConstants;
import com.tcsion.ml.metadatahandler.util.DBConnectionUtility;


public class UseCasePromptMappingDAO {
	
	private DBConnectionUtility dbConnectionObj = new DBConnectionUtility();

	private String promptmappingCacheContent = null;
	
	ResponseMaster responseMasterObject = new ResponseMaster();

//	private HashMap<String, String> promptmappingCache = new HashMap<String, String>();
	
	private static final Log logger = LogFactory.getLog(UseCasePromptMappingDAO.class);
	
	public UseCasePromptMappingDAO(ResponseMaster responseMasterObject) {
		this.responseMasterObject = responseMasterObject;
		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + " Use case prompt dao constructor called.");
	}
	
	public String getPromptMapping(final int apiID, final long useCaseId, final long orgId,final long vendorserviceid, String recognizer_entity) {
		
		String promptListforUsecase = null;
		try {
			promptListforUsecase = getPromptSequenceForUsecaseId(apiID, useCaseId, orgId,vendorserviceid, recognizer_entity) ;			
		} catch (Exception e) {
			logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + "Exception occured while Prompt Mapping: " + e) ;
		}
		return promptListforUsecase;
	}
	
	
	
	public String getPromptSequenceForUsecaseId(final int apiID, final long usecaseid, final long orgId,final long vendorserviceid, String recognizer_entity){
		String promptSequenceListForUseacase = null;
		String key = String.valueOf(usecaseid)+"_"+String.valueOf(apiID)+"_"+String.valueOf(orgId)+"_"+String.valueOf(vendorserviceid)+"_"+recognizer_entity;
//		logger.error(GenAIFrameworkConstants.LOGGER_CACHE + " key {}: "+ key);
		
		try{
			promptmappingCacheContent = GenAIPromptDetailsCacheLoaderService.promptmappingCache.getIfPresent(key);
		
		
		}catch(Exception e){
			logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception occured while fetching  promptmappingCacheContent {} " +  e);
		}
//		logger.error(GenAIFrameworkConstants.LOGGER_CACHE + " key {}: "+ key);
//		logger.error(GenAIFrameworkConstants.LOGGER_CACHE + "promptmappingCache from cache are {}: "+(GenAIPromptDetailsCacheLoaderService.promptmappingCache.getIfPresent(key)));
		if(promptmappingCacheContent != null){

			promptSequenceListForUseacase = GenAIPromptDetailsCacheLoaderService.promptmappingCache.getIfPresent(key);
			responseMasterObject.setStatus(GenAIFrameworkConstants.GEN_201);
			responseMasterObject.setMsg(GenAIFrameworkConstants.GEN_201_MSG);
		
		}else{
			try {
//				logger.error("Cache miss going to fetch details from db");
				promptSequenceListForUseacase = fetchPromptSequenceFromDB(apiID, usecaseid, orgId, vendorserviceid, recognizer_entity);
//				logger.error("GenAIUtility: " + promptSequenceListForUseacase);
				//GenAIPromptDetailsCacheLoaderService.promptmappingCache.put(key, promptSequenceListForUseacase);
				Cache<String, String> promptmappingCache = GenAIPromptDetailsCacheLoaderService.promptmappingCache;
//				logger.error("GenAIMappingCache: " + promptmappingCache);
			} catch (Exception e) {
				logger.error("[FLAG] Exception in getPromptSequenceForUsecaseId {}: ", e);
			}
	
		}

		return promptSequenceListForUseacase;
	}
	
	public String fetchPromptSequenceFromDB(final int apiId, final long  usecaseId, final long orgId,final long  vendorserviceid, String recognizer_entity){
		
//		logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + " Going to fetch prompt ids mapped to usecase from table.");
//		logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + " Going to fetch prompt ids mapped to usecase and vendor service id from table. "+ vendorserviceid);

		String prompidlist = null;
		String query = GenAIFWQueryUtils.PROMPT_ID_LIST_QUERY ;
//		logger.info(GenAIFrameworkConstants.LOGGER_DEBUG + "Going to make customer global connection");
		 try (Connection conn = this.dbConnectionObj.getCustomerGlobalConnection()) { 
		
        prompidlist = getPrompIdFromDB(conn, query, apiId, usecaseId, orgId, vendorserviceid, recognizer_entity);
//        logger.error("prompidlist: " + prompidlist);

//        logger.error("Params: " + apiId + "_" +   usecaseId + "_" +  orgId + "_" +  vendorserviceid + "_" +  recognizer_entity);
        if (prompidlist == null) {
        	long defaultOrgId = GenAIFrameworkConstants.DEFAULT_ORGID;
            prompidlist = getPrompIdFromDB(conn, query, apiId, usecaseId, defaultOrgId , vendorserviceid, recognizer_entity);
//            logger.error("Default org prompidlist: " + prompidlist);
        } else {
            logger.debug(GenAIFrameworkConstants.LOGGER_DEBUG + " UseCase OrgID Exist in DB");
        }
        
//        logger.error("Final prompidlist: " + prompidlist);
        // Final status handling
			if (prompidlist == null) {
				responseMasterObject.setStatus(GenAIFrameworkConstants.GEN_401);
				responseMasterObject.setMsg(GenAIFrameworkConstants.GEN_401_MSG);
			} else if (prompidlist.equals("")) {
				responseMasterObject.setStatus(GenAIFrameworkConstants.GEN_408);
				responseMasterObject.setMsg(GenAIFrameworkConstants.GEN_408_MSG);
			} else {
				responseMasterObject.setStatus(GenAIFrameworkConstants.GEN_201);
				responseMasterObject.setMsg(GenAIFrameworkConstants.GEN_201_MSG);
			}
			
		}catch(Exception e){
			logger.error(GenAIFrameworkConstants.LOGGER_EXCEPTION + " Exception occured while fetching data from db {} " +  e);
			responseMasterObject.setStatus(GenAIFrameworkConstants.GEN_409);
			responseMasterObject.setMsg(GenAIFrameworkConstants.GEN_409_MSG);
		}
		
		return prompidlist;
	}
	
	private String getPrompIdFromDB(Connection conn, String query, int apiId,
			long usecaseId, long orgId, long vendorserviceid,
			String recognizer_entity) {
		String prompidlist = "";
		try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
			preparedStatement.setInt(1, apiId);
			preparedStatement.setLong(2, usecaseId);
			preparedStatement.setLong(3, orgId);
			preparedStatement.setLong(4, vendorserviceid);
			preparedStatement.setString(5, recognizer_entity);
			logger.error("PS: " + preparedStatement.toString());

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				/*if (resultSet.next()) {
					return resultSet.getString("prompidlist");
				}*/
				
				while(resultSet.next()){
					long fetchedOrgId = resultSet.getLong("orgid");
					logger.error("fetchedOrgId: " + fetchedOrgId);
					
					if(fetchedOrgId == orgId){
						prompidlist= resultSet.getString("prompidlist");
					}else if(fetchedOrgId == 1){
						prompidlist = resultSet.getString("prompidlist");
					}else{
						logger.error("[PromptBuilder: UseCasePromptMappingDAO] Fetched OrgId and Received Org Id does not match. FetchedOrgId_OrgId is: " + fetchedOrgId + "_" + orgId);
					}
				}
			}
		} catch (SQLException e) {
			logger.error(GenAIFrameworkConstants.LOGGER_DEBUG + "Exception Occured while fetching promptidlist for orgid : " + orgId, e);
		}
		return prompidlist;
	}
   
}
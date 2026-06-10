package com.tcsion.ml.qsnauthorv2.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.base.Strings;
import com.tcs.base.exception.AccessRightsException;
import com.tcs.base.exception.DatabaseException;
import com.tcs.base.exception.ExecutionException;
import com.tcs.base.helpers.DataCenters;
import com.tcsion.ml.Utils.DBConnectionUtility;
import com.tcsion.ml.filereader.ChunkingStrategy;
import com.tcsion.ml.filereader.DocxFileReader;
import com.tcsion.ml.filereader.FileReaderInterface;
import com.tcsion.ml.filereader.ImageFileReader;
import com.tcsion.ml.filereader.PdfFileReader;
import com.tcsion.ml.filereader.TxtFileReader;
import com.tcsion.ml.markingschemegeneration.MarkingSchemeGenerationService;
import com.tcsion.ml.qsnauthorv2.beans.InputBean;
import com.tcsion.ml.qsnauthorv2.beans.QuestionTypeConfig;


public class Utility {
	private static final Log logger = LogFactory.getLog(Utility.class);
	private final static DBConnectionUtility dbConnObject = new DBConnectionUtility();
	private final int dcnId = DataCenters.getInstance().getCurrentDcnId();
	private static final ConcurrentHashMap<String, String> gblPropertyCache = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, String> gblGlobalPropertyCache = new ConcurrentHashMap<>();
	
	public String prepareCollectionName(String appId, String orgId, String entity) {
	    // Regex: replace anything that's not a-z, A-Z, 0-9, _ or - with "_"
	    String safeAppId = appId.replaceAll("[^a-zA-Z0-9_-]", "_");
	    String safeOrgId = orgId.replaceAll("[^a-zA-Z0-9_-]", "_");
	    String safeEntity = entity.replaceAll("[^a-zA-Z0-9_-]", "_");

	    // Build collection name
	    String collectionName = safeAppId + "_" + safeOrgId + "_" + safeEntity;

	    // Ensure it does not start with a hyphen
	    collectionName = collectionName.replaceAll("^-+", "");

	    logger.info("Collection Name: " + collectionName);
	    return collectionName;
	}
	
	public FileReaderInterface getReaderForFile(String filePath) {
        if (filePath.endsWith(".pdf")) return new PdfFileReader();
        if (filePath.endsWith(".docx")) return new DocxFileReader();
        if (filePath.endsWith(".txt")) return new TxtFileReader();
        if (filePath.endsWith(".png") || filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") || filePath.endsWith(".gif") || filePath.endsWith(".webp")) return new ImageFileReader();
        return null;
    }
	
	public ChunkingStrategy getChunkingStrategy(String filePath){
		ChunkingStrategy strategy = new ChunkingStrategy();
		String chunkLevel = "sentence";
		int sentencesPerChunk = 20;
		
		String fileExtension = getFileExtension(filePath).toLowerCase(Locale.ROOT);
		String chunkLevelFromDB = getGblGlobalProperty("ionml.dataindexer.chunk.level."+fileExtension);
		chunkLevel = Strings.isNullOrEmpty(chunkLevelFromDB) ? "sentence" : chunkLevelFromDB;
		
		String sentencesPerChunkFromDB = getGblGlobalProperty("ionml.dataindexer.chunk.sentences."+fileExtension);
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
	
	public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1 || fileName.lastIndexOf('.') == 0) {
            return ""; // No extension found
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
	
	public List<String> partitionTextIntoChunks(String text, int chunkSize) {
        String[] sentences = text.split("(?<=\\.|\\!|\\?)\\s+");
        List<String> chunks = new ArrayList<>();
        
        for (int i = 0; i < sentences.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, sentences.length);
            StringBuilder chunk = new StringBuilder();
            for (int j = i; j < end; j++) {
                chunk.append(sentences[j]).append(" ");
            }
            chunks.add(chunk.toString().trim());
        }
        return chunks;
    }
	
    public String getGblProperty(long appId, long orgId, long usecaseid, String propertyName) {
        String cacheKey = appId + "_" + orgId + "_" + usecaseid + "_" + propertyName;
        
        if (gblPropertyCache.containsKey(cacheKey)) {
            logger.error("Cache hit for key: " + cacheKey);
            return gblPropertyCache.get(cacheKey);
        }

        String propertyValue = null;
        String query = "select * from gblproperties where prpappid=? and prporgid=? and prpftrid=? and prpname=?";
        
        try (Connection conn = dbConnObject.getGlobalConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setLong(1, appId);
            pstmt.setLong(2, orgId);
            pstmt.setLong(3, usecaseid);
            pstmt.setString(4, propertyName);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int orgIdFetched = rs.getInt("prporgid");
                    logger.info("orgIdFetched: " + orgIdFetched);
                    
                    propertyValue = rs.getString("prpvalue");
                    logger.info("Property fetched for Org: " + orgIdFetched + " propName: " + propertyName + " = " + propertyValue);
                    
                    if (Objects.equals(orgId, (long) orgIdFetched)) {
                    	break;
                    }
                }
                
                if (!Strings.isNullOrEmpty(propertyValue)){
                	gblPropertyCache.put(cacheKey, propertyValue);
                }
            }
        } catch (SQLException | DatabaseException | AccessRightsException | ExecutionException e) {
            logger.error("Exception Occurred while fetching ranking status from db: {}", e);
        }

        return propertyValue;
    }
	
    public String getGblGlobalProperty(String propertyName) {

        if (gblGlobalPropertyCache.containsKey(propertyName)) {
            logger.error("Cache hit for global property key: " + propertyName);
            return gblGlobalPropertyCache.get(propertyName);
        }

        String propertyValue = null;
        String query = "SELECT propname, propvalue, propdcnid FROM gblglobalproperties "
                     + "WHERE propname = ? AND proprowstate >= 0 AND propdcnid IN (1, ?)";

        try (Connection conn = dbConnObject.getGlobalConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, propertyName);
            pstmt.setLong(2, dcnId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int dcnIdFetched = rs.getInt("propdcnid");
                    logger.info("dcnIdFetched: " + dcnIdFetched);

                    propertyValue = rs.getString("propvalue");
                    logger.info("Property fetched for DCN: " + dcnIdFetched + " propName: " + propertyName + " = " + propertyValue);

                    if (Objects.equals(dcnId, (long) dcnIdFetched)) {
                        break;
                    }
                }
                if (!Strings.isNullOrEmpty(propertyValue)){
                	gblGlobalPropertyCache.put(propertyName, propertyValue);
                }
            }

        } catch (SQLException | DatabaseException | AccessRightsException | ExecutionException e) {
            logger.error("Exception occurred while fetching global property from DB: {}", e);
        }

        return propertyValue;
    }
    
    public String cleanInput(String inputToBeCleaned){
		String cleanedInput = null;
		cleanedInput = inputToBeCleaned.replaceAll("\u2019", "\'").replaceAll("\u201D", "\"").replaceAll("\u2018", "\'").replaceAll("\u201C", "\"");
		cleanedInput = cleanedInput.replaceAll("[^\\p{ASCII}]", " ");
		return cleanedInput;
	}
    
    public String sentenceWithLowestDistance(String reference, List<String> sentences) {
        int minDistance = Integer.MAX_VALUE;
        String bestMatch = "";

        for (String sentence : sentences) {
            int dist = computeEditDistance(reference, sentence);
            if (dist < minDistance) {
                minDistance = dist;
                bestMatch = sentence;
            }
        }

        return bestMatch;
    }

    public int computeEditDistance(String s1, String s2) {
        int[][] dp = new int[s1.length()+1][s2.length()+1];
        for(int i=0;i<=s1.length();i++){
            for(int j=0;j<=s2.length();j++){
                if(i==0){
                    dp[i][j]=j;
                }else if(j==0){
                    dp[i][j]=i;
                }else if(s1.charAt(i-1)==s2.charAt(j-1)){
                    dp[i][j]=dp[i-1][j-1];
                }else{
                    dp[i][j]=1+Math.min(dp[i-1][j-1],Math.min(dp[i-1][j],dp[i][j-1]));
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }
    
    @SuppressWarnings("unchecked")
    public JSONObject consolidateQuestions(List<JSONArray> questionLists, int maxQuestions, String qsnType) {
        JSONArray finalQuestions = new JSONArray();
        for (JSONArray questions : questionLists) {
            for (int i = 0; i < questions.size() && finalQuestions.size() < maxQuestions; i++) {
                finalQuestions.add(questions.get(i));
            }
            if (finalQuestions.size() >= maxQuestions) {
                break;
            }
        }
        JSONObject finalOutput = new JSONObject();
        finalOutput.put(qsnType, finalQuestions);
        return finalOutput;
    }
    
    @SuppressWarnings("unchecked")
	public JSONObject consolidateQuestionsForMultipleTypes(
            Map<String, List<JSONArray>> questionListsByType,
            Map<String, QuestionTypeConfig> configMap) {

        JSONObject finalOutput = new JSONObject();

        for (Map.Entry<String, List<JSONArray>> entry : questionListsByType.entrySet()) {
            String qsnType = entry.getKey();
            List<JSONArray> questionLists = entry.getValue();

            int maxQuestions = configMap.get(qsnType).getMaxQuestions();
            JSONArray finalQuestions = new JSONArray();

            for (JSONArray questions : questionLists) {
                for (int i = 0; i < questions.size() && finalQuestions.size() < maxQuestions; i++) {
                    finalQuestions.add(questions.get(i));
                }
                if (finalQuestions.size() >= maxQuestions) {
                    break;
                }
            }

            finalOutput.put(qsnType, finalQuestions);
        }

        return finalOutput;
    }
    
    public static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".txt", ".pdf", ".docx", ".jpg", ".png", ".jpeg");
    public boolean validateFileExtension(String filePath) {
        if (Strings.isNullOrEmpty(filePath)) return false;

        String filePathLower = filePath.toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.stream().anyMatch(filePathLower::endsWith);
    }
    
    @SuppressWarnings("unchecked")
	public JSONObject parseSerializedMap(String rawDirectives) {
        JSONObject result = new JSONObject();
        try {
            if (rawDirectives == null || rawDirectives.length() < 2) return result;

            String cleaned = rawDirectives.substring(1, rawDirectives.length() - 1); // Remove surrounding braces
            String[] entries = cleaned.split(",");

            for (String entry : entries) {
                String[] keyValue = entry.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    result.put(key, value);
                }
            }
        } catch (Exception e) {
        	logger.error("Error parsing input: " + e.getMessage());
        }

        return result;
    }
    
    
    public void processFAQWithMarkingScheme(
            JSONArray faqArray,
            Map<String, HashMap<String, String>> vendorDetailsMap,
            JSONObject directives,
            long appId,
            long orgId,
            int apiId,
            long useCaseId,
            String userRegisteredServiceId) {

        if (faqArray == null || faqArray.isEmpty()) return;

        String subject = "English";
        String grade = "7";

        if (directives != null) {

            Object subjectObj = directives.get("subject");
            if (subjectObj != null && !subjectObj.toString().isEmpty()) {
                subject = subjectObj.toString();
            }

            Object gradeObj = directives.get("grade");
            if (gradeObj != null && !gradeObj.toString().isEmpty()) {
                grade = gradeObj.toString();
            }
        }

        MarkingSchemeGenerationService service =
                new MarkingSchemeGenerationService(vendorDetailsMap);

        for (Object obj : faqArray) {

            JSONObject faq = (JSONObject) obj;
            String question = (String) faq.get("question");


            if (question == null || question.isEmpty()) continue;

            try {
            	
            	logger.error("Integration of faq with marking scheme");
            	
                JSONObject markingResponse =
                        service.executeMarkingSchemeGeneration(
                                null,
                                "medium",
                                "english",
                                question,
                                subject,   
                                grade,     
                                "score",
                                true,
                                true,
                                new ArrayList<>(),
                                new JSONObject(),
                                appId,
                                orgId,
                                apiId,
                                useCaseId,
                                userRegisteredServiceId
                        );

                faq.put("markingScheme", markingResponse.getOrDefault("markingSchemeResultJsonOutput", "{}"));
                logger.error("faq with marking scheme: "+ faq);

            } catch (Exception e) {
                logger.error("Marking scheme failed for question: " + question, e);
            }
        }
    }

    
    public static void main(String[] args){
    	Utility util = new Utility();
    	JSONObject json = util.parseSerializedMap("{topic=Excretory system of humans}");
//    	System.out.print(json.toJSONString());
    	
    }
}

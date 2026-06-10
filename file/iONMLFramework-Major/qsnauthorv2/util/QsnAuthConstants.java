package com.tcsion.ml.qsnauthorv2.util;

public final class QsnAuthConstants {
	private QsnAuthConstants(){
		throw new IllegalStateException("Utility class. Can not instantiate.");
	}
	public static final String QSNGEN_001 = "QSNGEN_001";
	public static final String COMPLETED_SUCCESSFULLY= "Completed successfully";
	
	public static final String QSNGEN_002 = "QSNGEN_002";
	public static final String HARMFUL_OR_INAPPROPRIATE_CONTENT = "Harmful or inappropriate content detected.";
	
	public static final String QSNGEN_003 = "QSNGEN_003";
	public static final String COMPLETED_PARTIALLY = "Completed partially.";
	
	public static final String QSNGEN_429 = "QSNGEN_429";
	public static final String TOO_MANY_REQUESTS = "Too Many Requests";
	
	public static final String QSNGEN_501 = "QSNGEN_501";
	public static final String UNABLE_TO_CREATE_VECTOR_STORE = "Unable to create Vector Store for the uploaded file.";
	
	public static final String QSNGEN_502 = "QSNGEN_502";
	public static final String UNABLE_TO_UPDATE_SCHEMA = "Unable to update schema of Vector Store for the uploaded file.";
	
	public static final String QSNGEN_985 = "QSNGEN_985";
	public static final String NO_FILES_PROVIDED = "No files provided for Knowledge based authoring.";
	
	public static final String QSNGEN_986 = "QSNGEN_986";
	public static final String FILE_EMPTY_OR_CORRUPTED = "File is empty or corrupted.";
	
	public static final String QSNGEN_987 = "QSNGEN_987";
	public static final String ISSUE_IN_THIRD_PARTY_INTEGRATION = "Issue in Third Party Integration";
	
	public static final String QSNGEN_988 = "QSNGEN_988";
	public static final String FILES_NOT_PREPARED_FOR_AUTHORING = "Files are not prepared for authoring.";
	
	public static final String QSNGEN_989 = "QSNGEN_989";
	public static final String NO_MATCH_FOUND_FOR_THE_TOPIC = "No matching content was found on the given topic in the file. Please check the topic and try again.";
	
	public static final String QSNGEN_990 = "QSNGEN_990";
	public static final String USER_INPUT_CANNOT_BE_EMPTY = "User Input or Directives both cannot be null or empty.";
	
	public static final String QSNGEN_991 = "QSNGEN_991";
	public static final String QSNGEN_992 = "QSNGEN_992";
	public static final String QSNGEN_993 = "QSNGEN_993";
	
	public static final String QSNGEN_994 = "QSNGEN_994";
	public static final String COMPLETED_BUT_QUESTION_NOT_GENERATED = "Completed but unable to generate any question.";
	
	public static final String QSNGEN_995 = "QSNGEN_995";
	public static final String INVALID_FILE_EXTENSION = "Invalid file extension";
	
	public static final String QSNGEN_996 = "QSNGEN_996";
	public static final String FILE_PATH_DOES_NOT_EXIST = "File path does not exist";
	
	public static final String QSNGEN_997 = "QSNGEN_997";
	public static final String INVALID_TYPE_OF_INPUT = "Invalid type Of Input";
	
	public static final String QSNGEN_998 = "QSNGEN_998";
	
	public static final String QSNGEN_999 = "QSNGEN_999";
	public static final String SOMETHING_IS_WRONG_IN_SCRIPT_EXECUTION = "Issue during execution of the script for the API";
	
	
	public static final String GUARDRAIL_VIOLATION = "Guardrail violation";
	
	public static final String QSNGEN_503 = "QSNGEN_503";
	public static final String SERVICE_UNAVAILABLE = "Service Unavailable";
	
	public static final String CANT_GENERATE_QUESTIONS_FROM_THIS_TEXT = "Can not generate questions from this text.";	
	
}

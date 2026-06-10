package com.tcsion.ml.qsnauthorv2.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkAuthoringInputBean {

    private long    questionId;
    private String questionType;
    private int    questionCount;
    private int    numberOfAnswerOptions;
    private boolean hintRequired;
    private boolean explanationRequired;
    private String subject;
    private String topic;
    private String subTopic;
    private String concept;
    private String additionalDirective1Label;
    private String additionalDirective1Value;
    private String additionalDirective2Label;
    private String additionalDirective2Value;
    private String difficultyLevel;
    private String educationLevel;
    private String additionalUserInstructions;


    public long getQuestionId() {
		return questionId;
	}
	public void setQuestionId(long questionId) {
		this.questionId = questionId;
	}
	public String getQuestionType() {
        return questionType;
    }
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public int getQuestionCount() {
        return questionCount;
    }
    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public int getNumberOfAnswerOptions() {
        return numberOfAnswerOptions;
    }
    public void setNumberOfAnswerOptions(int numberOfAnswerOptions) {
        this.numberOfAnswerOptions = numberOfAnswerOptions;
    }

    public boolean isHintRequired() {
        return hintRequired;
    }
    public void setHintRequired(boolean hintRequired) {
        this.hintRequired = hintRequired;
    }

    public boolean isExplanationRequired() {
        return explanationRequired;
    }
    public void setExplanationRequired(boolean explanationRequired) {
        this.explanationRequired = explanationRequired;
    }

    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSubTopic() {
        return subTopic;
    }
    public void setSubTopic(String subTopic) {
        this.subTopic = subTopic;
    }

    public String getConcept() {
        return concept;
    }
    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getAdditionalDirective1Label() {
        return additionalDirective1Label;
    }
    public void setAdditionalDirective1Label(String additionalDirective1Label) {
        this.additionalDirective1Label = additionalDirective1Label;
    }

    public String getAdditionalDirective1Value() {
        return additionalDirective1Value;
    }
    public void setAdditionalDirective1Value(String additionalDirective1Value) {
        this.additionalDirective1Value = additionalDirective1Value;
    }

    public String getAdditionalDirective2Label() {
        return additionalDirective2Label;
    }
    public void setAdditionalDirective2Label(String additionalDirective2Label) {
        this.additionalDirective2Label = additionalDirective2Label;
    }

    public String getAdditionalDirective2Value() {
        return additionalDirective2Value;
    }
    public void setAdditionalDirective2Value(String additionalDirective2Value) {
        this.additionalDirective2Value = additionalDirective2Value;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }
    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getEducationLevel() {
        return educationLevel;
    }
    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getAdditionalUserInstructions() {
        return additionalUserInstructions;
    }
    public void setAdditionalUserInstructions(String additionalUserInstructions) {
        this.additionalUserInstructions = additionalUserInstructions;
    }

    @Override
    public String toString() {
        return "BulkAuthoringInputBean ["
                + "questionId=" + questionId
                + ", questionType=" + questionType
                + ", questionCount=" + questionCount
                + ", numberOfAnswerOptions=" + numberOfAnswerOptions
                + ", hintRequired=" + hintRequired
                + ", explanationRequired=" + explanationRequired
                + ", subject=" + subject
                + ", topic=" + topic
                + ", subTopic=" + subTopic
                + ", concept=" + concept
                + ", additionalDirective1Label=" + additionalDirective1Label
                + ", additionalDirective1Value=" + additionalDirective1Value
                + ", additionalDirective2Label=" + additionalDirective2Label
                + ", additionalDirective2Value=" + additionalDirective2Value
                + ", difficultyLevel=" + difficultyLevel
                + ", educationLevel=" + educationLevel
                + ", additionalUserInstructions=" + additionalUserInstructions
                + "]";
    }
}

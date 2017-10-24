package com.redhat.engineering.jiracf.model;

public class SimpleIssueView {

    private String key;
    private String typeString;
    private String statusString;
    private String priorityString;
    private String resolutionString;

    public SimpleIssueView(String key, String typeString, String statusString, String priorityString, String resolutionString) {
        this.key = key;
        this.typeString = typeString;
        this.statusString = statusString;
        this.priorityString = priorityString;
        this.resolutionString = resolutionString;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTypeString() {
        return typeString;
    }

    public void setTypeString(String typeString) {
        this.typeString = typeString;
    }

    public String getStatusString() {
        return statusString;
    }

    public void setStatusString(String statusString) {
        this.statusString = statusString;
    }

    public String getPriorityString() {
        return priorityString;
    }

    public void setPriorityString(String priorityString) {
        this.priorityString = priorityString;
    }

    public String getResolutionString() {
        return resolutionString;
    }

    public void setResolutionString(String resolutionString) {
        this.resolutionString = resolutionString;
    }
}

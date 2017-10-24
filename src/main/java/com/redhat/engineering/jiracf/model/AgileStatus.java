package com.redhat.engineering.jiracf.model;

public enum AgileStatus {
    TODO("10000", "TO DO"), IN_PROGRESS("3", "IN PROGRESS"), DONE("10001", "DONE");

    private String id;
    private String text;

    AgileStatus(final String id, final String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}

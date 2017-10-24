package com.redhat.engineering.jiracf.model;

public enum AgileType {

    STORY("10001", "Story"), TASK("10100", "Task"), BUG("10102", "Bug"), EPIC("10000", "Epic");
    private String id;
    private String text;

    AgileType(final String id, final String text) {
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

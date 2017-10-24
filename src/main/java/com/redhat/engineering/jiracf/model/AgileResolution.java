package com.redhat.engineering.jiracf.model;

public enum AgileResolution {
    DONE("10000", "DONE");

    private String id;
    private String text;

    AgileResolution(final String id, final String text) {
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

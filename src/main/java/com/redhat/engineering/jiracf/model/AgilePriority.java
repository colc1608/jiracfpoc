package com.redhat.engineering.jiracf.model;

public enum AgilePriority {

    MEDIUM("3", "Medium"), HIGH("2", "High"), HIGHEST("1", "Highest"), LOW("4", "Low"), LOWEST("5", "Lowest");
    private String id;
    private String text;

    AgilePriority(final String id, final String text) {
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

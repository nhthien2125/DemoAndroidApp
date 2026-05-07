package com.neith.subjectdemo.fn.model;

public class ProjectHighlight {
    private String name;
    private String code;
    private String revenue;
    private String cost;

    public ProjectHighlight(String name, String code, String revenue, String cost) {
        this.name = name;
        this.code = code;
        this.revenue = revenue;
        this.cost = cost;
    }

    public String getName() { return name; }
    public String getCode() { return code; }
    public String getRevenue() { return revenue; }
    public String getCost() { return cost; }
}

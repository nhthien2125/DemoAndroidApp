package com.neith.subjectdemo.fn.model;

public class TransportExpense {
    private String date;
    private String code;
    private String material;
    private String project;
    private String amount;

    public TransportExpense(String date, String code, String material, String project, String amount) {
        this.date = date;
        this.code = code;
        this.material = material;
        this.project = project;
        this.amount = amount;
    }

    public String getDate() { return date; }
    public String getCode() { return code; }
    public String getMaterial() { return material; }
    public String getProject() { return project; }
    public String getAmount() { return amount; }
}

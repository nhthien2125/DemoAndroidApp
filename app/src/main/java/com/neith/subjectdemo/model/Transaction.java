package com.neith.subjectdemo.model;

public class Transaction {
    private int id;
    private int type; // 0=expense, 1=income
    private double amount;
    private String description;

    // Constructor
    public Transaction(int id, int type, double amount, String description) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.id = id;
    }

    public int getId() { return id; }
    public int getType() { return type; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
}
package com.neith.subjectdemo.fn.model;

public class Transaction {
    private final int id;
    private final int type; // 0=expense, 1=income
    private final double amount;
    private final String description;

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
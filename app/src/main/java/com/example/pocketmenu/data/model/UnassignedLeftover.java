package com.example.pocketmenu.data.model;

public class UnassignedLeftover {
    private String recipeId;
    private int remainingPortions;
    private boolean perishable;
    private int validDays;

    // Empty constructor
    public UnassignedLeftover() {}

    // Constructor
    public UnassignedLeftover(String recipeId, int remainingPortions,
                              boolean perishable, int validDays) {
        this.recipeId = recipeId;
        this.remainingPortions = remainingPortions;
        this.perishable = perishable;
        this.validDays = validDays;
    }

    // Getters
    public String getRecipeId() { return recipeId; }
    public int getRemainingPortions() { return remainingPortions; }
    public boolean isPerishable() { return perishable; }
    public int getValidDays() { return validDays; }

    // Setters
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public void setRemainingPortions(int remainingPortions) { this.remainingPortions = remainingPortions; }
    public void setPerishable(boolean perishable) { this.perishable = perishable; }
    public void setValidDays(int validDays) { this.validDays = validDays; }
}
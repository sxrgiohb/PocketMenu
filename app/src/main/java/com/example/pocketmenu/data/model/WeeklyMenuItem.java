package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.PropertyName;

// Represents the planning of a meal assigned to a concrete day
public class WeeklyMenuItem {
    private int dayOfWeek;
    private String recipeId;
    private int portions;
    // Forces the field to be named "isLeftover" in Firestore (it would be "Leftover" otherwise)
    @PropertyName("isLeftover")
    private boolean isLeftover;
    private String sourceRecipeId;
    private boolean perishable;
    private int validDays;

    // Empty constructor
    public WeeklyMenuItem() {}

    // Main recipe constructor
    public WeeklyMenuItem(int dayOfWeek, String recipeId, int portions,
                          boolean perishable, int validDays) {
        this.dayOfWeek = dayOfWeek;
        this.recipeId = recipeId;
        this.portions = portions;
        this.isLeftover = false;
        this.sourceRecipeId = null;
        this.perishable = perishable;
        this.validDays = validDays;
    }

    // Consumed leftover constructor
    public WeeklyMenuItem(int dayOfWeek, String recipeId,
                          String sourceRecipeId, int portions) {
        this.dayOfWeek = dayOfWeek;
        this.recipeId = recipeId;
        this.portions = portions;
        this.isLeftover = true;
        this.sourceRecipeId = sourceRecipeId;
        this.perishable = false;
        this.validDays = 0;
    }

    // Getters
    public int getDayOfWeek() { return dayOfWeek; }
    public String getRecipeId() { return recipeId; }
    public int getPortions() { return portions; }
    public String getSourceRecipeId() { return sourceRecipeId; }
    public boolean isPerishable() { return perishable; }
    public int getValidDays() { return validDays; }
    @PropertyName("isLeftover")
    public boolean isLeftover() { return isLeftover; }

    // Setters
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public void setPortions(int portions) { this.portions = portions; }
    public void setSourceRecipeId(String sourceRecipeId) { this.sourceRecipeId = sourceRecipeId; }
    public void setPerishable(boolean perishable) { this.perishable = perishable; }
    public void setValidDays(int validDays) { this.validDays = validDays; }
    @PropertyName("isLeftover")
    public void setLeftover(boolean leftover) { isLeftover = leftover; }
}
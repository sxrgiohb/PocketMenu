package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.PropertyName;

public class WeeklyMenuItem {
    private int dayOfWeek;
    private String recipeId;
    private int portions;

    @PropertyName("isLeftover")
    private boolean isLeftover;

    private String sourceRecipeId;
    private boolean perishable;
    private int validDays;

    public WeeklyMenuItem() {}

    // Constructor para receta principal
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

    // Constructor para sobra consumida
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

    public int getDayOfWeek() { return dayOfWeek; }
    public String getRecipeId() { return recipeId; }
    public int getPortions() { return portions; }
    public String getSourceRecipeId() { return sourceRecipeId; }
    public boolean isPerishable() { return perishable; }
    public int getValidDays() { return validDays; }

    @PropertyName("isLeftover")
    public boolean isLeftover() { return isLeftover; }

    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public void setPortions(int portions) { this.portions = portions; }
    public void setSourceRecipeId(String sourceRecipeId) { this.sourceRecipeId = sourceRecipeId; }
    public void setPerishable(boolean perishable) { this.perishable = perishable; }
    public void setValidDays(int validDays) { this.validDays = validDays; }

    @PropertyName("isLeftover")
    public void setLeftover(boolean leftover) { isLeftover = leftover; }
}
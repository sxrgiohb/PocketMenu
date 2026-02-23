package com.example.pocketmenu.data.model;

public class WeeklyMenuItem {
    private int dayOfWeek;
    private String recipeId;
    private int portions;

    public WeeklyMenuItem() {
    }

    public WeeklyMenuItem(int dayOfWeek, String recipeId, int portions) {
        this.dayOfWeek = dayOfWeek;
        this.recipeId = recipeId;
        this.portions = portions;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public int getPortions() {
        return portions;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public void setPortions(int portions) {
        this.portions = portions;
    }
}

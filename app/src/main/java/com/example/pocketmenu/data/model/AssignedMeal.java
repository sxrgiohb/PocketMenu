package com.example.pocketmenu.data.model;

public class AssignedMeal {
    private String id;
    private String userId;
    private String mealId;
    private String date;
    private String mealTime;
    private int consumedPortions;
    private boolean leftover;

    //Empty constructor
    public AssignedMeal(){

    }

    //Constructor
    public AssignedMeal(String id, String userId, String mealId, String date, String mealTime, int consumedPortions, boolean leftover) {
        this.id = id;
        this.userId = userId;
        this.mealId = mealId;
        this.date = date;
        this.mealTime = mealTime;
        this.consumedPortions = consumedPortions;
        this.leftover = leftover;
    }

    //Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getMealId() {
        return mealId;
    }

    public String getDate() {
        return date;
    }

    public String getMealTime() {
        return mealTime;
    }

    public int getConsumedPortions() {
        return consumedPortions;
    }

    public boolean isLeftover() {
        return leftover;
    }

    //Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMealId(String mealId) {
        this.mealId = mealId;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setMealTime(String mealTime) {
        this.mealTime = mealTime;
    }

    public void setConsumedPortions(int consumedPortions) {
        this.consumedPortions = consumedPortions;
    }

    public void setLeftover(boolean leftover) {
        this.leftover = leftover;
    }
}

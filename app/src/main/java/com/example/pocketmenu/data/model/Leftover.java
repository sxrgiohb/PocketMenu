package com.example.pocketmenu.data.model;

import java.util.Date;

public class Leftover {
    private String id;
    private String userId;
    private String mealId;
    private String mealName;
    private int leftoversLeft;
    private Date cookingDate;
    private Date expirationDate;

    //Empty constructor
    public Leftover(){

    }

    //Constructor
    public Leftover(String id, String userId, String mealId, String mealName, int leftoversLeft, Date cookingDate, Date expirationDate) {
        this.id = id;
        this.userId = userId;
        this.mealId = mealId;
        this.mealName = mealName;
        this.leftoversLeft = leftoversLeft;
        this.cookingDate = cookingDate;
        this.expirationDate = expirationDate;
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

    public String getMealName() {
        return mealName;
    }

    public int getLeftoversLeft() {
        return leftoversLeft;
    }

    public Date getCookingDate() {
        return cookingDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
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

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public void setLeftoversLeft(int leftoversLeft) {
        this.leftoversLeft = leftoversLeft;
    }

    public void setCookingDate(Date cookingDate) {
        this.cookingDate = cookingDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }
}

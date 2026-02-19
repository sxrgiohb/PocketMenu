package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;

public class Menu {
    @DocumentId
    private String id;
    private String userId;
    private String recipeId;
    private Date date;

    //Empty constructor
    public Menu() {}

    //Constructor
    public Menu(String userId, String recipeId, Date date) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.date = date;
    }

    //Getters
    public String getId() {
        return id;
    }
    public String getUserId() {
        return userId;
    }
    public String getRecipeId() {
        return recipeId;
    }
    public Date getDate() {
        return date;
    }

    //Setters
    public void setId(String id) {
        this.id = id;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }
    public void setDate(Date date) {
        this.date = date;
    }
}

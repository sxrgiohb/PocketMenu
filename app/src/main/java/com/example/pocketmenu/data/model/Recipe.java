package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.DocumentId;

import java.util.List;
public class Recipe {
    @DocumentId
    private String id;
    private String userId;
    private String urlImg;
    private String name;
    private int portion;
   private String description;
   private boolean isFavorite;
    private List<Ingredient> ingredients;

    //Empty constructor
    public Recipe() {

    }

    //Constructor

    public Recipe(String userId, String name, String description, int portion, List<Ingredient> ingredients) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.portion = portion;
        this.ingredients = ingredients;
        this.urlImg = "";
        this.isFavorite = false;
    }

    //Getters

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUrlImg() {
        return urlImg;
    }

    public String getName() {
        return name;
    }


    public int getPortion() {
        return portion;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }


    //Setters


    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUrlImg(String urlImg) {
        this.urlImg = urlImg;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPortion(int portion) {
        this.portion = portion;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }
}

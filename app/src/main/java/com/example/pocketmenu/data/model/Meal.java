package com.example.pocketmenu.data.model;

import java.util.List;

public class Meal {

    private String id;
    private String name;
    private String recipe;
    private int portion;
    private String urlImg;
    private String userId;
    private List<Ingredient> ingredients;

    //Empty constructor
    public Meal() {

    }

    //Constructor
    public Meal(String id, String name, String recipe, int portion, String urlImg, String userId, List<Ingredient> ingredients) {
        this.id = id;
        this.name = name;
        this.recipe = recipe;
        this.portion = portion;
        this.urlImg = urlImg;
        this.userId = userId;
        this.ingredients = ingredients;
    }

    //Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRecipe() {
        return recipe;
    }

    public int getPortion() {
        return portion;
    }

    public String getUrlImg() {
        return urlImg;
    }

    public String getUserId() {
        return userId;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    //Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public void setPortion(int portion) {
        this.portion = portion;
    }

    public void setUrlImg(String urlImg) {
        this.urlImg = urlImg;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }
}




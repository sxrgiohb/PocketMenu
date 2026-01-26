package com.example.pocketmenu.data.model;

public class Ingredient {
    private String name;
    private double quantity;
    private String unit;
    private String category;
    private String store;

    //Empty constructor
    public Ingredient (){

    }

    //Constructor
    public Ingredient(String name, double quantity, String unit, String category, String store) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.store = store;

    }

    //Getters
    public String getName() {
        return name;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getCategory() {
        return category;
    }

    public String getStore() {
        return store;
    }

    //Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStore(String store) {
        this.store = store;
    }
}

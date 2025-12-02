package com.example.pocketmenu.data.model;

public class ShoppingList {

    private String id;
    private String userId;
    private String productName;
    private double totalQuantity;
    private String unitsMeasurement;
    private String category;
    private boolean isBought;
    private boolean isAdded;

    //Empty constructor
    public ShoppingList(){

    }

    //Constructor
    public ShoppingList(String id, String userId, String productName, double totalQuantity, String unitsMeasurement, String category, boolean isBought, boolean isAdded) {
        this.id = id;
        this.userId = userId;
        this.productName = productName;
        this.totalQuantity = totalQuantity;
        this.unitsMeasurement = unitsMeasurement;
        this.category = category;
        this.isBought = isBought;
        this.isAdded = isAdded;
    }

    //Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getProductName() {
        return productName;
    }

    public double getTotalQuantity() {
        return totalQuantity;
    }

    public String getUnitsMeasurement() {
        return unitsMeasurement;
    }

    public String getCategory() {
        return category;
    }

    public boolean isBought() {
        return isBought;
    }

    public boolean isAdded() {
        return isAdded;
    }

    //Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setTotalQuantity(double totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public void setUnitsMeasurement(String unitsMeasurement) {
        this.unitsMeasurement = unitsMeasurement;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setBought(boolean bought) {
        isBought = bought;
    }

    public void setAdded(boolean added) {
        isAdded = added;
    }
}

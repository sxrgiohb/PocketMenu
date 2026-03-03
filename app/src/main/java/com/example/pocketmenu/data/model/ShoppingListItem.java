package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

public class ShoppingListItem {

    @DocumentId
    private String id;
    private String userId;
    private String weekId;
    private String name;
    private double quantity;
    private String unit;
    private String category;
    private String store;
    private boolean checked;

    @PropertyName("isExtra")
    private boolean isExtra;

    private String recipeId;

    public ShoppingListItem() {}


    public ShoppingListItem(String userId, String weekId, String name,
                            double quantity, String unit, String category,
                            String store, String recipeId) {
        this.userId = userId;
        this.weekId = weekId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.store = store;
        this.checked = false;
        this.isExtra = false;
        this.recipeId = recipeId;
    }

    public ShoppingListItem(String userId, String weekId, String name,
                            double quantity, String unit, String category,
                            String store, boolean isExtra) {
        this.userId = userId;
        this.weekId = weekId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.store = store;
        this.checked = false;
        this.isExtra = true;
        this.recipeId = null;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getWeekId() { return weekId; }
    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getCategory() { return category; }
    public String getStore() { return store; }
    public boolean isChecked() { return checked; }
    public String getRecipeId() { return recipeId; }

    @PropertyName("isExtra")
    public boolean isExtra() { return isExtra; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setWeekId(String weekId) { this.weekId = weekId; }
    public void setName(String name) { this.name = name; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setCategory(String category) { this.category = category; }
    public void setStore(String store) { this.store = store; }
    public void setChecked(boolean checked) { this.checked = checked; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    @PropertyName("isExtra")
    public void setExtra(boolean extra) { isExtra = extra; }
}
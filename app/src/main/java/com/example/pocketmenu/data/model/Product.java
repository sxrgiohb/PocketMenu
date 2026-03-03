package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.DocumentId;

public class Product {

    @DocumentId
    private String id;
    private String userId;
    private String name;
    private String unit;
    private String category;
    private String store;

    public Product() {}

    public Product(String userId, String name, String unit,
                   String category, String store) {
        this.userId = userId;
        this.name = name;
        this.unit = unit;
        this.category = category;
        this.store = store;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public String getCategory() { return category; }
    public String getStore() { return store; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setCategory(String category) { this.category = category; }
    public void setStore(String store) { this.store = store; }
}
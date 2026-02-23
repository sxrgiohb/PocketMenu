package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.DocumentId;

import java.util.List;

public class WeeklyMenuTemplate {
    @DocumentId
    private String id;
    private String userId;
    private String name;
    private boolean isFavorite;
    private List<WeeklyMenuItem> items; // lista de recetas con día de la semana

    public WeeklyMenuTemplate() {
    }

    public WeeklyMenuTemplate(String id, String userId, String name, boolean isFavorite, List<WeeklyMenuItem> items) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.isFavorite = isFavorite;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public List<WeeklyMenuItem> getItems() {
        return items;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setItems(List<WeeklyMenuItem> items) {
        this.items = items;
    }
}
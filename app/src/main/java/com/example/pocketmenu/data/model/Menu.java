package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class Menu {
    @DocumentId
    private String id;
    private String userId;
    private String recipeId;
    private Date date;
    private int usedPortions;
    private String name;
    private boolean isFavorite;

    @PropertyName("isFromLeftover")
    private boolean isFromLeftover;

    private String sourceRecipeId;
    private String sourceMenuId;
    private boolean leftoverPerishable;
    private int leftoverValidDays;

    public Menu() {}

    // Constructor para receta principal
    public Menu(String userId, String recipeId, Date date, int usedPortions,
                String name, boolean isFavorite) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.date = date;
        this.usedPortions = usedPortions;
        this.name = name;
        this.isFavorite = isFavorite;
        this.isFromLeftover = false;
        this.sourceRecipeId = null;
        this.sourceMenuId = null;
        this.leftoverPerishable = false;
        this.leftoverValidDays = 0;
    }

    // Constructor para sobra consumida
    public Menu(String userId, String recipeId, Date date, int usedPortions,
                String name, boolean isFavorite,
                boolean isFromLeftover, String sourceRecipeId, String sourceMenuId,
                boolean leftoverPerishable, int leftoverValidDays) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.date = date;
        this.usedPortions = usedPortions;
        this.name = name;
        this.isFavorite = isFavorite;
        this.isFromLeftover = isFromLeftover;
        this.sourceRecipeId = sourceRecipeId;
        this.sourceMenuId = sourceMenuId;
        this.leftoverPerishable = leftoverPerishable;
        this.leftoverValidDays = leftoverValidDays;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getRecipeId() { return recipeId; }
    public Date getDate() { return date; }
    public int getUsedPortions() { return usedPortions; }
    public String getName() { return name; }
    public boolean isFavorite() { return isFavorite; }
    public String getSourceRecipeId() { return sourceRecipeId; }
    public String getSourceMenuId() { return sourceMenuId; }
    public boolean isLeftoverPerishable() { return leftoverPerishable; }
    public int getLeftoverValidDays() { return leftoverValidDays; }

    @PropertyName("isFromLeftover")
    public boolean isFromLeftover() { return isFromLeftover; }

    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public void setDate(Date date) { this.date = date; }
    public void setUsedPortions(int usedPortions) { this.usedPortions = usedPortions; }
    public void setName(String name) { this.name = name; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setSourceRecipeId(String sourceRecipeId) { this.sourceRecipeId = sourceRecipeId; }
    public void setSourceMenuId(String sourceMenuId) { this.sourceMenuId = sourceMenuId; }
    public void setLeftoverPerishable(boolean leftoverPerishable) { this.leftoverPerishable = leftoverPerishable; }
    public void setLeftoverValidDays(int leftoverValidDays) { this.leftoverValidDays = leftoverValidDays; }

    @PropertyName("isFromLeftover")
    public void setFromLeftover(boolean fromLeftover) { isFromLeftover = fromLeftover; }
}
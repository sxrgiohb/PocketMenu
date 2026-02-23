package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;

public class Menu {
    @DocumentId
    private String id;
    private String userId;
    private String recipeId;
    private Date date;
    private int usedPortions; // cuántas porciones de la receta se usan en esta asignación
    private String name;

    private boolean isFavorite;


    // Empty constructor required for Firestore
    public Menu() {}

    public Menu(String userId, String recipeId, Date date, int usedPortions, String name, boolean isFavorite) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.date = date;
        this.usedPortions = usedPortions;
        this.name = name;
        this.isFavorite = isFavorite;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getRecipeId() { return recipeId; }
    public Date getDate() { return date; }
    public int getUsedPortions() { return usedPortions; }
    public String getName() { return name; }
    public boolean isFavorite() { return isFavorite; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public void setDate(Date date) { this.date = date; }
    public void setUsedPortions(int usedPortions) { this.usedPortions = usedPortions; }
    public void setName(String name) { this.name = name; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}

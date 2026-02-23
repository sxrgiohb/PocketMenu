package com.example.pocketmenu.data.model;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;

public class Leftover {
    @DocumentId
    private String id;
    private String userId;
    private String recipeId;
    private String sourceMenuId;
    private int remainingPortions;
    private boolean perishable;
    private Date firstAssignedDate;
    private int validDays;

    public Leftover() {}

    public Leftover(String userId, String recipeId, String sourceMenuId,int remainingPortions, boolean perishable, Date firstAssignedDate, int validDays) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.sourceMenuId = sourceMenuId;
        this.remainingPortions = remainingPortions;
        this.perishable = perishable;
        this.firstAssignedDate = firstAssignedDate;
        this.validDays = validDays;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getRecipeId() { return recipeId; }
    public String getSourceMenuId() { return sourceMenuId; }
    public int getRemainingPortions() { return remainingPortions; }
    public boolean getPerishable() { return perishable; }
    public Date getFirstAssignedDate() { return firstAssignedDate; }
    public int getValidDays() { return validDays; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public void setSourceMenuId(String sourceMenuId) { this.sourceMenuId = sourceMenuId; }
    public void setRemainingPortions(int remainingPortions) { this.remainingPortions = remainingPortions; }
    public void setPerishable(boolean perishable) { this.perishable = perishable; }
    public void setFirstAssignedDate(Date firstAssignedDate) { this.firstAssignedDate = firstAssignedDate; }
    public void setValidDays(int validDays) { this.validDays = validDays; }
}

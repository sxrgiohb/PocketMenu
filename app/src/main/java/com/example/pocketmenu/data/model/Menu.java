package com.example.pocketmenu.data.model;

import java.util.Map;
public class Menu {

    private String id;
    private String userId;
    private String menuName;
    private String description;
    private Map<String, Map<String,String>> menuStructure;

    //Empty constructor
    public Menu(){

    }

    //Constructor
    public Menu(String id, String userId, String menuName, String description, Map<String, Map<String, String>> menuStructure) {
        this.id = id;
        this.userId = userId;
        this.menuName = menuName;
        this.description = description;
        this.menuStructure = menuStructure;
    }

    //Getters

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getMenuName() {
        return menuName;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Map<String, String>> getMenuStructure() {
        return menuStructure;
    }

    //Setters

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMenuStructure(Map<String, Map<String, String>> menuStructure) {
        this.menuStructure = menuStructure;
    }
}

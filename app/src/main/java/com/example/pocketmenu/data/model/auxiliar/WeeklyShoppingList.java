package com.example.pocketmenu.data.model.auxiliar;

import com.example.pocketmenu.data.model.ShoppingListItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WeeklyShoppingList {

    private final String weekId;
    private final Date monday;
    private final List<ShoppingListItem> items;

    public WeeklyShoppingList(String weekId, Date monday, List<ShoppingListItem> items) {
        this.weekId = weekId;
        this.monday = monday;
        this.items = items;
    }

    public String getWeekId() { return weekId; }
    public Date getMonday() { return monday; }
    public List<ShoppingListItem> getItems() {
        return items != null ? items : new ArrayList<>();
    }
}
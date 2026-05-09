package com.example.pocketmenu.data.model.auxiliar;

import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.Menu;
import com.example.pocketmenu.data.model.Recipe;

// represents the assignment of a recipe to a specific menu, managing the relationship between the recipe, the menu and any leftovers.
public class MenuAssignment {
    private Menu menu;
    private Recipe recipe;
    private Leftover leftover;

    // Constructor
    public MenuAssignment(Menu menu, Recipe recipe, Leftover leftover) {
        this.menu = menu;
        this.recipe = recipe;
        this.leftover = leftover;
    }

    // Calculates how many portions are left to assign
    public int getAvailablePortions() {
        // Total portions - used portions
        int available = recipe.getPortion() - menu.getUsedPortions();
        // Checks for existing leftovers
        if (leftover != null) {
            available += leftover.getRemainingPortions();
        }
        return available;
    }

    // Consume a portion of the recipe or leftover
    public void consumePortion() {
        // Checks for remaining portions
        if (recipe.getPortion() - menu.getUsedPortions() > 0) {
            // Consumes one portion
            menu.setUsedPortions(menu.getUsedPortions() + 1);
            // Checks for leftovers
        } else if (leftover != null && leftover.getRemainingPortions() > 0) {
            // Consumes one portion
            leftover.setRemainingPortions(leftover.getRemainingPortions() - 1);
        }
    }

    // Getters
    public Menu getMenu() { return menu; }
    public Recipe getRecipe() { return recipe; }
    public Leftover getLeftover() { return leftover; }

    // Setters
    public void setMenu(Menu menu) { this.menu = menu; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    public void setLeftover(Leftover leftover) { this.leftover = leftover; }
}

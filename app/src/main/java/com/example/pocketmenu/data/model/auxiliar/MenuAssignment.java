package com.example.pocketmenu.data.model.auxiliar;

import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.Menu;
import com.example.pocketmenu.data.model.Recipe;

// Represents the assignment of a recipe to a specific menu, managing the relationship between the recipe, the menu and any leftovers.
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

    // Getters
    public Menu getMenu() { return menu; }
    public Recipe getRecipe() { return recipe; }
    public Leftover getLeftover() { return leftover; }

    // Setters
    public void setMenu(Menu menu) { this.menu = menu; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    public void setLeftover(Leftover leftover) { this.leftover = leftover; }
}

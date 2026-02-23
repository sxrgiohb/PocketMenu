package com.example.pocketmenu.data.model.auxiliar;

import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.Menu;
import com.example.pocketmenu.data.model.Recipe;

public class MenuAssignment {
    private Menu menu;
    private Recipe recipe;
    private Leftover leftover;

    public MenuAssignment(Menu menu, Recipe recipe, Leftover leftover) {
        this.menu = menu;
        this.recipe = recipe;
        this.leftover = leftover;
    }

    // Calcula cuántas porciones quedan disponibles para asignar
    public int getAvailablePortions() {
        int available = recipe.getPortion() - menu.getUsedPortions();
        if (leftover != null) {
            available += leftover.getRemainingPortions();
        }
        return available;
    }

    // Consume una porción de la receta o sobra
    public void consumePortion() {
        if (recipe.getPortion() - menu.getUsedPortions() > 0) {
            menu.setUsedPortions(menu.getUsedPortions() + 1);
        } else if (leftover != null && leftover.getRemainingPortions() > 0) {
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

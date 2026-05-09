package com.example.pocketmenu.data.model.auxiliar;

import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.Recipe;

// Represent the combination of a recipe and its leftovers
public class LeftoverWithRecipe {
    private final Leftover leftover;
    private final Recipe recipe;

    // Constructor
    public LeftoverWithRecipe(Leftover leftover, Recipe recipe) {
        this.leftover = leftover;
        this.recipe = recipe;
    }

    // Getters
    public Leftover getLeftover() { return leftover; }
    public Recipe getRecipe() { return recipe; }
}
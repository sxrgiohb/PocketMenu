package com.example.pocketmenu.data.model.auxiliar;

import com.example.pocketmenu.data.model.Leftover;
import com.example.pocketmenu.data.model.Recipe;

public class LeftoverWithRecipe {
    private final Leftover leftover;
    private final Recipe recipe;

    public LeftoverWithRecipe(Leftover leftover, Recipe recipe) {
        this.leftover = leftover;
        this.recipe = recipe;
    }

    public Leftover getLeftover() { return leftover; }
    public Recipe getRecipe() { return recipe; }
}
package com.example.pocketmenu.utils;

public class RecipeValidator {

    private RecipeValidator() {}

    public static boolean isNameValid(String name) {
        return name != null && !name.trim().isEmpty();
    }

    public static boolean isPortionsValid(String portionsText) {
        if (portionsText == null || portionsText.trim().isEmpty()) return true;
        try {
            int portions = Integer.parseInt(portionsText.trim());
            return portions > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int parsePortions(String portionsText) {
        if (portionsText == null || portionsText.trim().isEmpty()) return 1;
        try {
            return Integer.parseInt(portionsText.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
package com.example.pocketmenu.utils;

public class RecipeValidator {

    // Empty constructor
    private RecipeValidator() {}

    // Checks if the name is not null or empty
    public static boolean isNameValid(String name) {
        return name != null && !name.trim().isEmpty();
    }

    // Checks the portions input
    public static boolean isPortionsValid(String portionsText) {
        // Portions equals 1 if the input is empty
        if (portionsText == null || portionsText.trim().isEmpty()) return true;
        try {
            int portions = Integer.parseInt(portionsText.trim());
            return portions > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Transforms the portions input into an integer
    public static int parsePortions(String portionsText) {
        if (portionsText == null || portionsText.trim().isEmpty()) return 1;
        try {
            return Integer.parseInt(portionsText.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
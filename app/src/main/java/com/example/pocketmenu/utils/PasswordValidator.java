package com.example.pocketmenu.utils;

public class PasswordValidator {

    private PasswordValidator() {}

    public static boolean isValid(String password) {
        if (password == null) return false;
        return password.length() >= 8 &&
                password.matches(".*\\d.*") &&
                password.matches(".*[A-Z].*");
    }

    public static boolean hasMinLength(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean hasNumber(String password) {
        return password != null && password.matches(".*\\d.*");
    }

    public static boolean hasUpperCase(String password) {
        return password != null && password.matches(".*[A-Z].*");
    }
}
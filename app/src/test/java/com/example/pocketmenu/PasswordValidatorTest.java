package com.example.pocketmenu;

import com.example.pocketmenu.utils.PasswordValidator;

import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordValidatorTest {

    @Test
    public void nullPassword_isNotValid() {
        assertFalse(PasswordValidator.isValid(null));
    }

    @Test
    public void emptyPassword_isNotValid() {
        assertFalse(PasswordValidator.isValid(""));
    }

    @Test
    public void passwordTooShort_isNotValid() {
        assertFalse(PasswordValidator.isValid("Pass1"));
    }

    @Test
    public void passwordWithoutNumber_isNotValid() {
        assertFalse(PasswordValidator.isValid("Password"));
    }

    @Test
    public void passwordWithoutUpperCase_isNotValid() {
        assertFalse(PasswordValidator.isValid("password1"));
    }

    @Test
    public void passwordExactlyEightChars_isValid() {
        assertTrue(PasswordValidator.isValid("Password1"));
    }

    @Test
    public void strongPassword_isValid() {
        assertTrue(PasswordValidator.isValid("StrongPassword123"));
    }

    @Test
    public void hasMinLength_shortPassword_returnsFalse() {
        assertFalse(PasswordValidator.hasMinLength("Pass1"));
    }

    @Test
    public void hasMinLength_longPassword_returnsTrue() {
        assertTrue(PasswordValidator.hasMinLength("Password123"));
    }

    @Test
    public void hasNumber_noNumber_returnsFalse() {
        assertFalse(PasswordValidator.hasNumber("Password"));
    }

    @Test
    public void hasNumber_withNumber_returnsTrue() {
        assertTrue(PasswordValidator.hasNumber("Password1"));
    }

    @Test
    public void hasUpperCase_noUpperCase_returnsFalse() {
        assertFalse(PasswordValidator.hasUpperCase("password1"));
    }

    @Test
    public void hasUpperCase_withUpperCase_returnsTrue() {
        assertTrue(PasswordValidator.hasUpperCase("Password1"));
    }
}
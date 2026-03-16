package com.example.pocketmenu;

import com.example.pocketmenu.utils.RecipeValidator;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecipeValidatorTest {

    // isNameValid
    @Test
    public void nullName_isNotValid() {
        assertFalse(RecipeValidator.isNameValid(null));
    }

    @Test
    public void emptyName_isNotValid() {
        assertFalse(RecipeValidator.isNameValid(""));
    }

    @Test
    public void blankName_isNotValid() {
        assertFalse(RecipeValidator.isNameValid("   "));
    }

    @Test
    public void validName_isValid() {
        assertTrue(RecipeValidator.isNameValid("Paella"));
    }

    // isPortionsValid
    @Test
    public void nullPortions_isValid() {
        assertTrue(RecipeValidator.isPortionsValid(null));
    }

    @Test
    public void emptyPortions_isValid() {
        assertTrue(RecipeValidator.isPortionsValid(""));
    }

    @Test
    public void validPortions_isValid() {
        assertTrue(RecipeValidator.isPortionsValid("4"));
    }

    @Test
    public void negativePortions_isNotValid() {
        assertFalse(RecipeValidator.isPortionsValid("-1"));
    }

    @Test
    public void zeroPortions_isNotValid() {
        assertFalse(RecipeValidator.isPortionsValid("0"));
    }

    @Test
    public void nonNumericPortions_isNotValid() {
        assertFalse(RecipeValidator.isPortionsValid("abc"));
    }

    @Test
    public void decimalPortions_isNotValid() {
        assertFalse(RecipeValidator.isPortionsValid("2.5"));
    }

    // parsePortions
    @Test
    public void nullPortionsText_returnsOne() {
        assertEquals(1, RecipeValidator.parsePortions(null));
    }

    @Test
    public void emptyPortionsText_returnsOne() {
        assertEquals(1, RecipeValidator.parsePortions(""));
    }

    @Test
    public void validPortionsText_returnsCorrectValue() {
        assertEquals(4, RecipeValidator.parsePortions("4"));
    }

    @Test
    public void invalidPortionsText_returnsOne() {
        assertEquals(1, RecipeValidator.parsePortions("abc"));
    }
}
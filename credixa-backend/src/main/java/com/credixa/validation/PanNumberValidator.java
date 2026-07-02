package com.credixa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PanNumberValidator implements ConstraintValidator<PanNumber, String> {

    // PAN Format: 5 Alphabets, 4 Digits, 1 Alphabet
    private static final String PAN_PATTERN = "^[A-Z]{5}[0-9]{4}[A-Z]$";

    @Override
    public void initialize(PanNumber constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String pan, ConstraintValidatorContext context) {
        if (pan == null || pan.isEmpty()) {
            return false;
        }
        return Pattern.matches(PAN_PATTERN, pan);
    }
}

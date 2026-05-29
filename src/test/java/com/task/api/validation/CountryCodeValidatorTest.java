package com.task.api.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CountryCodeValidatorTest {

    private CountryCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CountryCodeValidator();
    }

    @Nested
    @DisplayName("Valid country codes")
    class ValidCodes {

        @ParameterizedTest
        @ValueSource(strings = {"PL", "US", "DE", "GB", "FR", "JP"})
        @DisplayName("should accept valid ISO 3166-1 alpha-2 codes")
        void shouldAcceptValidCodes(String code) {
            assertThat(validator.isValid(code, null)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"pl", "us", "de"})
        @DisplayName("should accept lowercase valid codes")
        void shouldAcceptLowercaseCodes(String code) {
            assertThat(validator.isValid(code, null)).isTrue();
        }

        @Test
        @DisplayName("should accept null (delegated to @NotBlank)")
        void shouldAcceptNull() {
            assertThat(validator.isValid(null, null)).isTrue();
        }

        @Test
        @DisplayName("should accept blank (delegated to @NotBlank)")
        void shouldAcceptBlank() {
            assertThat(validator.isValid("", null)).isTrue();
        }
    }

    @Nested
    @DisplayName("Invalid country codes")
    class InvalidCodes {

        @ParameterizedTest
        @ValueSource(strings = {"XX", "ZZ", "QQ", "AA"})
        @DisplayName("should reject non-existent 2-letter codes")
        void shouldRejectInvalidCodes(String code) {
            assertThat(validator.isValid(code, null)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"TOOLONG", "USA", "P"})
        @DisplayName("should reject codes with wrong length")
        void shouldRejectWrongLengthCodes(String code) {
            assertThat(validator.isValid(code, null)).isFalse();
        }
    }
}

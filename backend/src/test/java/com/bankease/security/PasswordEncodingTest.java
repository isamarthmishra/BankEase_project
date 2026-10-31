package com.bankease.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncodingTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void testPasswordIsHashedAndNotPlainText() {
        String rawPassword = "SuperSecurePassword@2026";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }

    @Test
    void testDifferentSaltsForSamePassword() {
        String rawPassword = "SuperSecurePassword@2026";
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        assertNotEquals(encoded1, encoded2, "BCrypt must produce different salts on each hash");
        assertTrue(passwordEncoder.matches(rawPassword, encoded1));
        assertTrue(passwordEncoder.matches(rawPassword, encoded2));
    }

    @Test
    void testWrongPasswordFailsMatch() {
        String rawPassword = "SuperSecurePassword@2026";
        String encoded = passwordEncoder.encode(rawPassword);

        assertFalse(passwordEncoder.matches("WrongPassword", encoded));
    }
}

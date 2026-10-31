package com.bankease.security;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // 256-bit base64 test key
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour

        userDetails = new User("test@example.com", "password123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isBlank());

        String username = jwtService.extractUsername(token);
        assertEquals("test@example.com", username);

        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertTrue(isValid);
    }

    @Test
    void testTokenInvalidForDifferentUser() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = new User("other@example.com", "password123",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        boolean isValid = jwtService.isTokenValid(token, otherUser);
        assertFalse(isValid);
    }

    @Test
    void testTokenExpiration() {
        // Set expiration to negative value to test expired token
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String expiredToken = jwtService.generateToken(userDetails);

        assertThrows(Exception.class, () -> jwtService.isTokenValid(expiredToken, userDetails));
    }
}

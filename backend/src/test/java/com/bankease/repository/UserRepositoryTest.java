package com.bankease.repository;

import com.bankease.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should persist and retrieve user by email")
    void testSaveAndFindByEmail() {
        User user = new User("Database Test", "db.test@example.com", "hashedPass", "1234567890");
        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<User> found = userRepository.findByEmail("db.test@example.com");
        assertTrue(found.isPresent());
        assertEquals("Database Test", found.get().getName());
    }

    @Test
    @DisplayName("Should enforce unique email constraint at the database layer")
    void testUniqueEmailConstraint() {
        User user1 = new User("User One", "unique.test@example.com", "hash1", "123");
        userRepository.saveAndFlush(user1);

        User user2 = new User("User Two", "unique.test@example.com", "hash2", "456");
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
}

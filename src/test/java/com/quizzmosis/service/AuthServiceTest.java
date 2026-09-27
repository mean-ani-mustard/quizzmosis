package com.quizzmosis.service;

import com.quizzmosis.dao.UserDAO;
import com.quizzmosis.model.User;
import com.quizzmosis.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registration and login rules. AuthService gets its UserDAO through the constructor, so here it
 * gets an in-memory fake instead of the real one and the tests run without a database.
 */
class AuthServiceTest {

    private InMemoryUsers users;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        auth = new AuthService(users);
    }

    // ----- registration -----

    @Test
    void registrationStoresAHashNotThePassword() throws ValidationException {
        User user = auth.register("newbie", "New Bie", "long enough", "long enough");

        assertEquals("newbie", user.getUsername());
        assertEquals("New Bie", user.getDisplayName());
        String storedHash = users.findPasswordHash("newbie");
        assertNotEquals("long enough", storedHash);
        assertTrue(PasswordHasher.verify("long enough", storedHash));
    }

    @Test
    void inputIsTrimmedAndDisplayNameDefaultsToUsername() throws ValidationException {
        User user = auth.register("  newbie ", "   ", "long enough", "long enough");

        assertEquals("newbie", user.getUsername());
        assertEquals("newbie", user.getDisplayName());
    }

    @Test
    void invalidUsernamesAreRejected() {
        for (String username : new String[] {null, "", "ab", "has space", "<script>", "x".repeat(31)}) {
            assertThrows(ValidationException.class,
                    () -> auth.register(username, "Name", "long enough", "long enough"), "username " + username);
        }
    }

    @Test
    void passwordMustHaveTheRightLength() {
        ValidationException tooShort = assertThrows(ValidationException.class,
                () -> auth.register("newbie", "", "seven77", "seven77"));
        assertTrue(tooShort.getMessage().contains("at least 8"), tooShort.getMessage());

        String longPassword = "p".repeat(129);
        assertThrows(ValidationException.class, () -> auth.register("newbie", "", longPassword, longPassword));
    }

    @Test
    void passwordsMustMatch() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.register("newbie", "", "long enough", "long enougj"));
        assertEquals("The two passwords don't match.", e.getMessage());
    }

    @Test
    void displayNameCanBeAtMost50Characters() {
        assertThrows(ValidationException.class,
                () -> auth.register("newbie", "d".repeat(51), "long enough", "long enough"));
    }

    @Test
    void usernameCanOnlyBeTakenOnce() throws ValidationException {
        auth.register("newbie", "", "long enough", "long enough");

        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.register("newbie", "Someone else", "other password", "other password"));
        assertEquals("That username is already taken.", e.getMessage());
    }

    // ----- login -----

    @Test
    void loginWithTheRightPasswordReturnsTheUser() throws ValidationException {
        auth.register("newbie", "New Bie", "long enough", "long enough");

        User user = auth.login(" newbie ", "long enough");

        assertNotNull(user);
        assertEquals("New Bie", user.getDisplayName());
        assertEquals(1, users.lastLoginUpdates);
    }

    @Test
    void loginWithTheWrongPasswordFails() throws ValidationException {
        auth.register("newbie", "", "long enough", "long enough");

        assertNull(auth.login("newbie", "wrong password"));
        assertNull(auth.login("newbie", ""));
        assertEquals(0, users.lastLoginUpdates);
    }

    @Test
    void loginWithAnUnknownUserOrMissingInputFails() {
        assertNull(auth.login("nobody", "long enough"));
        assertNull(auth.login(null, "long enough"));
        assertNull(auth.login("nobody", null));
    }

    // ----- account settings -----

    @Test
    void displayNameCanBeChangedAndFallsBackToTheUsername() throws ValidationException {
        User user = auth.register("newbie", "New Bie", "long enough", "long enough");

        assertEquals("Newbie Two", auth.changeDisplayName(user, "  Newbie Two "));
        assertEquals("Newbie Two", users.findById(user.getUserId()).getDisplayName());
        assertEquals("newbie", auth.changeDisplayName(user, ""));
        assertThrows(ValidationException.class, () -> auth.changeDisplayName(user, "d".repeat(51)));
    }

    @Test
    void passwordChangeNeedsTheCurrentPassword() throws ValidationException {
        User user = auth.register("newbie", "", "long enough", "long enough");

        ValidationException wrong = assertThrows(ValidationException.class,
                () -> auth.changePassword(user, "not my password", "brand new pass", "brand new pass"));
        assertEquals("Your current password isn't right.", wrong.getMessage());
        assertThrows(ValidationException.class, () -> auth.changePassword(user, "long enough", "short", "short"));
        assertThrows(ValidationException.class,
                () -> auth.changePassword(user, "long enough", "brand new pass", "brand new pasS"));

        auth.changePassword(user, "long enough", "brand new pass", "brand new pass");

        assertNull(auth.login("newbie", "long enough"));
        assertNotNull(auth.login("newbie", "brand new pass"));
    }

    @Test
    void deletingTheAccountNeedsThePasswordAndAnotherAdmin() throws ValidationException {
        User admin = auth.register("boss", "", "long enough", "long enough");
        admin.setAdmin(true);
        users.admins = 1;

        assertThrows(ValidationException.class, () -> auth.deleteAccount(admin, "wrong password"));
        ValidationException onlyAdmin = assertThrows(ValidationException.class, () -> auth.deleteAccount(admin, "long enough"));
        assertTrue(onlyAdmin.getMessage().contains("only administrator"), onlyAdmin.getMessage());

        users.admins = 2;
        auth.deleteAccount(admin, "long enough");

        assertNull(users.findByUsername("boss"));
    }

    /** A stand-in for the real UserDAO that keeps users in a map. */
    private static class InMemoryUsers extends UserDAO {

        private final Map<String, User> usersByName = new HashMap<>();
        private final Map<String, String> hashesByName = new HashMap<>();
        private int lastLoginUpdates;
        private int admins;

        @Override
        public int create(String username, String passwordHash, String displayName) {
            if (usersByName.containsKey(username)) {
                return -1;
            }
            User user = new User();
            user.setUserId(usersByName.size() + 1);
            user.setUsername(username);
            user.setDisplayName(displayName);
            usersByName.put(username, user);
            hashesByName.put(username, passwordHash);
            return user.getUserId();
        }

        @Override
        public User findById(int userId) {
            return usersByName.values().stream().filter(u -> u.getUserId() == userId).findFirst().orElse(null);
        }

        @Override
        public User findByUsername(String username) {
            return usersByName.get(username);
        }

        @Override
        public String findPasswordHash(String username) {
            return hashesByName.get(username);
        }

        @Override
        public void updateLastLogin(int userId) {
            lastLoginUpdates++;
        }

        @Override
        public void updateDisplayName(int userId, String displayName) {
            findById(userId).setDisplayName(displayName);
        }

        @Override
        public void updatePasswordHash(int userId, String passwordHash) {
            hashesByName.put(findById(userId).getUsername(), passwordHash);
        }

        @Override
        public int countAdmins() {
            return admins;
        }

        @Override
        public boolean delete(int userId) {
            User user = findById(userId);
            hashesByName.remove(user.getUsername());
            return usersByName.remove(user.getUsername()) != null;
        }
    }
}

package com.quizzmosis.service;

import com.quizzmosis.dao.UserDAO;
import com.quizzmosis.model.User;
import com.quizzmosis.security.PasswordHasher;

import java.util.regex.Pattern;

/**
 * Registration, login and account changes (display name, password, deleting the account), kept
 * out of the servlets so the rules can be tested on their own.
 */
public class AuthService {

    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MAX_DISPLAY_NAME_LENGTH = 50;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_.-]{3,30}");

    /**
     * Checked when a username doesn't exist, so a failed login takes as long for an unknown
     * username as for a wrong password. Otherwise the response time would reveal which
     * usernames are registered.
     */
    private static final String DUMMY_HASH = PasswordHasher.hash("dummy-password-for-timing");

    private final UserDAO users;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO users) {
        this.users = users;
    }

    /**
     * Validates the input, hashes the password and creates the account.
     *
     * @throws ValidationException with a user-friendly message when something is wrong
     */
    public User register(String username, String displayName, String password, String confirmPassword)
            throws ValidationException {
        username = username == null ? "" : username.trim();
        displayName = displayName == null ? "" : displayName.trim();

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ValidationException(
                    "Usernames must be 3-30 characters long and may only contain letters, digits, '.', '_' and '-'.");
        }
        checkNewPassword(password, confirmPassword);
        displayName = checkDisplayName(displayName, username);
        if (users.findByUsername(username) != null) {
            throw new ValidationException("That username is already taken.");
        }

        int userId = users.create(username, PasswordHasher.hash(password), displayName);
        if (userId < 0) {
            // Someone registered the same name between our check and the insert.
            throw new ValidationException("That username is already taken.");
        }
        return users.findById(userId);
    }

    /**
     * Changes the name other people see. An empty name means "use my username".
     *
     * @return the name that was saved
     */
    public String changeDisplayName(User user, String displayName) throws ValidationException {
        String name = checkDisplayName(displayName == null ? "" : displayName.trim(), user.getUsername());
        users.updateDisplayName(user.getUserId(), name);
        return name;
    }

    /** Changes the password, after checking the current one. */
    public void changePassword(User user, String currentPassword, String newPassword, String confirmPassword)
            throws ValidationException {
        checkCurrentPassword(user, currentPassword);
        checkNewPassword(newPassword, confirmPassword);
        users.updatePasswordHash(user.getUserId(), PasswordHasher.hash(newPassword));
    }

    /**
     * Deletes the account and everything that belongs to it, after checking the password. The
     * only administrator can't do this, so the site always keeps one.
     */
    public void deleteAccount(User user, String password) throws ValidationException {
        checkCurrentPassword(user, password);
        if (user.isAdmin() && users.countAdmins() <= 1) {
            throw new ValidationException("You're the only administrator. Make someone else an admin first.");
        }
        users.delete(user.getUserId());
    }

    private void checkCurrentPassword(User user, String password) throws ValidationException {
        String storedHash = users.findPasswordHash(user.getUsername());
        if (password == null || storedHash == null || !PasswordHasher.verify(password, storedHash)) {
            throw new ValidationException("Your current password isn't right.");
        }
    }

    private static void checkNewPassword(String password, String confirmPassword) throws ValidationException {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Passwords must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Passwords can be at most " + MAX_PASSWORD_LENGTH + " characters long.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("The two passwords don't match.");
        }
    }

    /** An empty display name becomes the username. */
    private static String checkDisplayName(String displayName, String username) throws ValidationException {
        String name = displayName.isEmpty() ? username : displayName;
        if (name.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new ValidationException("Display names can be at most " + MAX_DISPLAY_NAME_LENGTH + " characters long.");
        }
        return name;
    }

    /** Returns the user if the credentials are correct, otherwise {@code null}. */
    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        username = username.trim();
        String storedHash = users.findPasswordHash(username);
        if (storedHash == null) {
            PasswordHasher.verify(password, DUMMY_HASH);
            return null;
        }
        if (!PasswordHasher.verify(password, storedHash)) {
            return null;
        }
        User user = users.findByUsername(username);
        users.updateLastLogin(user.getUserId());
        return user;
    }
}

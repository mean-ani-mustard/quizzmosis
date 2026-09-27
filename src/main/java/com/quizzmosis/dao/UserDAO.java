package com.quizzmosis.dao;

import com.quizzmosis.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Reads and writes the {@code users} table. */
public class UserDAO {

    /** Every column except password_hash, which only {@link #findPasswordHash} ever reads. */
    static final String USER_COLUMNS =
            "u.user_id, u.username, u.display_name, u.mantra, u.show_challenge_history, u.is_admin, "
                    + "u.created_at, u.last_login";

    /**
     * Creates a user.
     *
     * @return the new user's id, or -1 if the username is already taken
     */
    public int create(String username, String passwordHash, String displayName) {
        try {
            return Jdbc.insert("INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?)",
                    username, passwordHash, displayName);
        } catch (DataAccessException e) {
            if (e.isConstraintViolation()) {
                return -1;
            }
            throw e;
        }
    }

    public User findById(int userId) {
        return Jdbc.queryOne("SELECT " + USER_COLUMNS + " FROM users u WHERE u.user_id = ?",
                UserDAO::map, userId);
    }

    public User findByUsername(String username) {
        return Jdbc.queryOne("SELECT " + USER_COLUMNS + " FROM users u WHERE u.username = ?",
                UserDAO::map, username);
    }

    /** The stored password hash for a login check, or {@code null} if there is no such user. */
    public String findPasswordHash(String username) {
        return Jdbc.queryOne("SELECT password_hash FROM users WHERE username = ?",
                rs -> rs.getString(1), username);
    }

    public void updateLastLogin(int userId) {
        Jdbc.update("UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
    }

    public List<User> findAll() {
        return Jdbc.query("SELECT " + USER_COLUMNS + " FROM users u ORDER BY u.username", UserDAO::map);
    }

    public int count() {
        return Jdbc.queryInt("SELECT COUNT(*) FROM users");
    }

    public void updateDisplayName(int userId, String displayName) {
        Jdbc.update("UPDATE users SET display_name = ? WHERE user_id = ?", displayName, userId);
    }

    public void updatePasswordHash(int userId, String passwordHash) {
        Jdbc.update("UPDATE users SET password_hash = ? WHERE user_id = ?", passwordHash, userId);
    }

    public int countAdmins() {
        return Jdbc.queryInt("SELECT COUNT(*) FROM users WHERE is_admin = TRUE");
    }

    /** Sets the profile mantra; {@code null} removes it. */
    public void updateMantra(int userId, String mantra) {
        Jdbc.update("UPDATE users SET mantra = ? WHERE user_id = ?", mantra, userId);
    }

    /** Shows or hides the user's challenge history on their profile. It is never deleted. */
    public void setShowChallengeHistory(int userId, boolean show) {
        Jdbc.update("UPDATE users SET show_challenge_history = ? WHERE user_id = ?", show, userId);
    }

    public boolean setAdmin(int userId, boolean admin) {
        return Jdbc.update("UPDATE users SET is_admin = ? WHERE user_id = ?", admin, userId) > 0;
    }

    /**
     * Deletes a user. Their quizzes, attempts, friendships, messages, achievements and
     * announcements are removed by the database (ON DELETE CASCADE in schema.sql).
     */
    public boolean delete(int userId) {
        return Jdbc.update("DELETE FROM users WHERE user_id = ?", userId) > 0;
    }

    /** Maps a row selected with {@link #USER_COLUMNS}. Also used by FriendsDAO. */
    static User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setDisplayName(rs.getString("display_name"));
        user.setMantra(rs.getString("mantra"));
        user.setShowChallengeHistory(rs.getBoolean("show_challenge_history"));
        user.setAdmin(rs.getBoolean("is_admin"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        return user;
    }
}

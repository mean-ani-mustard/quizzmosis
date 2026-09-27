package com.quizzmosis.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * A registered user.
 *
 * <p>The logged-in user is kept in the HTTP session, which is why this class is Serializable.
 * The password hash is deliberately not part of it: it is only ever read inside
 * {@code UserDAO.findPasswordHash} while checking a login.</p>
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String displayName;
    /** A short motto shown on the profile, or {@code null}. */
    private String mantra;
    /** Whether friends see this user's challenge history on their profile. */
    private boolean showChallengeHistory = true;
    private boolean admin;
    private Timestamp createdAt;
    private Timestamp lastLogin;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMantra() {
        return mantra;
    }

    public void setMantra(String mantra) {
        this.mantra = mantra;
    }

    public boolean isShowChallengeHistory() {
        return showChallengeHistory;
    }

    public void setShowChallengeHistory(boolean showChallengeHistory) {
        this.showChallengeHistory = showChallengeHistory;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }
}

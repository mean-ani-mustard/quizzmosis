package com.quizzmosis.model;

import java.sql.Timestamp;

/** A badge users can earn. {@code icon} is a Bootstrap Icons class such as "bi-trophy-fill". */
public class Achievement {

    private int achievementId;
    private String name;
    private String description;
    private String icon;
    private Timestamp earnedAt;

    public int getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(int achievementId) {
        this.achievementId = achievementId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    /** When the user earned it, or {@code null} if they haven't (yet). */
    public Timestamp getEarnedAt() {
        return earnedAt;
    }

    public void setEarnedAt(Timestamp earnedAt) {
        this.earnedAt = earnedAt;
    }

    public boolean isEarned() {
        return earnedAt != null;
    }
}

package com.quizzmosis.dao;

import com.quizzmosis.model.Achievement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Achievements and which users earned them. */
public class AchievementDAO {

    /** All achievements; {@code earnedAt} is filled in for the ones {@code userId} has earned. */
    public List<Achievement> findAllForUser(int userId) {
        return Jdbc.query("SELECT a.achievement_id, a.name, a.description, a.icon, ua.earned_at "
                        + "FROM achievements a "
                        + "LEFT JOIN user_achievements ua ON ua.achievement_id = a.achievement_id AND ua.user_id = ? "
                        + "ORDER BY a.achievement_id",
                AchievementDAO::map, userId);
    }

    /** Only the achievements {@code userId} has earned, oldest first. */
    public List<Achievement> findEarnedBy(int userId) {
        return Jdbc.query("SELECT a.achievement_id, a.name, a.description, a.icon, ua.earned_at "
                        + "FROM achievements a JOIN user_achievements ua ON ua.achievement_id = a.achievement_id "
                        + "WHERE ua.user_id = ? ORDER BY ua.earned_at, a.achievement_id",
                AchievementDAO::map, userId);
    }

    public Achievement findById(int achievementId) {
        return Jdbc.queryOne("SELECT achievement_id, name, description, icon, NULL AS earned_at "
                + "FROM achievements WHERE achievement_id = ?", AchievementDAO::map, achievementId);
    }

    /**
     * Gives an achievement to a user.
     *
     * @return {@code true} if it was newly earned, {@code false} if the user already had it
     */
    public boolean award(int userId, int achievementId) {
        // INSERT IGNORE skips the row when the (user_id, achievement_id) primary key already exists.
        return Jdbc.update("INSERT IGNORE INTO user_achievements (user_id, achievement_id) VALUES (?, ?)",
                userId, achievementId) > 0;
    }

    private static Achievement map(ResultSet rs) throws SQLException {
        Achievement achievement = new Achievement();
        achievement.setAchievementId(rs.getInt("achievement_id"));
        achievement.setName(rs.getString("name"));
        achievement.setDescription(rs.getString("description"));
        achievement.setIcon(rs.getString("icon"));
        achievement.setEarnedAt(rs.getTimestamp("earned_at"));
        return achievement;
    }
}

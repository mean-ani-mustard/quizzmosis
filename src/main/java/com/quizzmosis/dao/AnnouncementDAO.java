package com.quizzmosis.dao;

import com.quizzmosis.model.Announcement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Site-wide announcements managed by administrators. */
public class AnnouncementDAO {

    private static final String SELECT_ANNOUNCEMENT =
            "SELECT a.announcement_id, a.title, a.content, a.created_at, a.is_active FROM announcements a ";

    private static final String NEWEST_FIRST = " ORDER BY a.created_at DESC, a.announcement_id DESC";

    public int create(String title, String content, int createdBy) {
        return Jdbc.insert("INSERT INTO announcements (title, content, created_by) VALUES (?, ?, ?)",
                title, content, createdBy);
    }

    /** Announcements shown on everyone's home page. */
    public List<Announcement> findActive() {
        return Jdbc.query(SELECT_ANNOUNCEMENT + "WHERE a.is_active = TRUE" + NEWEST_FIRST, AnnouncementDAO::map);
    }

    public List<Announcement> findAll() {
        return Jdbc.query(SELECT_ANNOUNCEMENT + NEWEST_FIRST, AnnouncementDAO::map);
    }

    /** Shows a hidden announcement again, or hides a visible one. */
    public boolean toggleActive(int announcementId) {
        return Jdbc.update("UPDATE announcements SET is_active = NOT is_active WHERE announcement_id = ?",
                announcementId) > 0;
    }

    public boolean delete(int announcementId) {
        return Jdbc.update("DELETE FROM announcements WHERE announcement_id = ?", announcementId) > 0;
    }

    private static Announcement map(ResultSet rs) throws SQLException {
        Announcement announcement = new Announcement();
        announcement.setAnnouncementId(rs.getInt("announcement_id"));
        announcement.setTitle(rs.getString("title"));
        announcement.setContent(rs.getString("content"));
        announcement.setCreatedAt(rs.getTimestamp("created_at"));
        announcement.setActive(rs.getBoolean("is_active"));
        return announcement;
    }
}

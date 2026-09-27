package com.quizzmosis.dao;

import com.quizzmosis.model.Announcement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnouncementDAOIT extends DatabaseTestBase {

    private final AnnouncementDAO announcements = new AnnouncementDAO();

    @Test
    void hiddenAnnouncementsAreOnlyShownToAdmins() {
        int admin = createUser("admin");
        int welcome = announcements.create("Welcome", "Have fun!", admin);
        int maintenance = announcements.create("Maintenance", "Down on Sunday.", admin);

        assertTrue(announcements.toggleActive(maintenance));

        assertEquals(List.of("Welcome"), titles(announcements.findActive()));
        assertEquals(2, announcements.findAll().size(), "the admin page lists all of them");

        announcements.toggleActive(maintenance);
        assertEquals(2, announcements.findActive().size());

        assertTrue(announcements.delete(welcome));
        assertEquals(List.of("Maintenance"), titles(announcements.findAll()));
    }

    private static List<String> titles(List<Announcement> list) {
        return list.stream().map(Announcement::getTitle).toList();
    }
}

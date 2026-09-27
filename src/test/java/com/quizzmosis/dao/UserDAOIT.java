package com.quizzmosis.dao;

import com.quizzmosis.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDAOIT extends DatabaseTestBase {

    @Test
    void createdUserCanBeFoundByIdAndByUsername() {
        int id = userDAO.create("alice", "stored-hash", "Alice Johnson");

        User user = userDAO.findById(id);
        assertEquals("alice", user.getUsername());
        assertEquals("Alice Johnson", user.getDisplayName());
        assertFalse(user.isAdmin());
        assertNotNull(user.getCreatedAt());
        assertNull(user.getLastLogin());
        assertEquals(id, userDAO.findByUsername("alice").getUserId());
        assertEquals("stored-hash", userDAO.findPasswordHash("alice"));
    }

    @Test
    void unknownUsersAreNull() {
        assertNull(userDAO.findById(123_456));
        assertNull(userDAO.findByUsername("nobody"));
        assertNull(userDAO.findPasswordHash("nobody"));
    }

    @Test
    void usernamesAreUniqueIgnoringCase() {
        assertTrue(userDAO.create("alice", "hash", "Alice") > 0);

        assertEquals(-1, userDAO.create("alice", "hash", "Another Alice"));
        assertEquals(-1, userDAO.create("ALICE", "hash", "Loud Alice"));
        assertEquals(1, userDAO.count());
    }

    @Test
    void mantraCanBeSetAndRemoved() {
        int id = createUser("alice");
        assertNull(userDAO.findById(id).getMantra(), "new users have no mantra");

        userDAO.updateMantra(id, "Curiosity first, answers second.");
        assertEquals("Curiosity first, answers second.", userDAO.findById(id).getMantra());

        userDAO.updateMantra(id, null);
        assertNull(userDAO.findById(id).getMantra());
    }

    @Test
    void challengeHistoryIsShownUntilTheUserHidesIt() {
        int id = createUser("alice");
        assertTrue(userDAO.findById(id).isShowChallengeHistory());

        userDAO.setShowChallengeHistory(id, false);
        assertFalse(userDAO.findById(id).isShowChallengeHistory());

        userDAO.setShowChallengeHistory(id, true);
        assertTrue(userDAO.findByUsername("alice").isShowChallengeHistory());
    }

    @Test
    void displayNameAndPasswordCanBeChanged() {
        int id = userDAO.create("alice", "old-hash", "Alice");

        userDAO.updateDisplayName(id, "Alice Johnson");
        userDAO.updatePasswordHash(id, "new-hash");

        assertEquals("Alice Johnson", userDAO.findById(id).getDisplayName());
        assertEquals("new-hash", userDAO.findPasswordHash("alice"));
    }

    @Test
    void adminsAreCounted() {
        int alice = createUser("alice");
        createUser("bob");
        assertEquals(0, userDAO.countAdmins());

        userDAO.setAdmin(alice, true);

        assertEquals(1, userDAO.countAdmins());
    }

    @Test
    void lastLoginIsRecorded() {
        int id = createUser("alice");

        userDAO.updateLastLogin(id);

        assertNotNull(userDAO.findById(id).getLastLogin());
    }

    @Test
    void adminRightsCanBeGrantedAndRemoved() {
        int id = createUser("alice");

        assertTrue(userDAO.setAdmin(id, true));
        assertTrue(userDAO.findById(id).isAdmin());
        assertTrue(userDAO.setAdmin(id, false));
        assertFalse(userDAO.findById(id).isAdmin());
        assertFalse(userDAO.setAdmin(999_999, true), "no such user");
    }

    @Test
    void deletingAUserDeletesEverythingThatBelongsToThem() {
        int alice = createUser("alice");
        int bob = createUser("bob");
        int quiz = createQuiz(alice, "Alice's quiz");
        addTypedQuestion(quiz, "Question?", "Answer");
        int bobsQuiz = createQuiz(bob, "Bob's quiz");
        int attempt = saveAttempt(alice, bobsQuiz, 1, 1, 10);
        saveAttempt(bob, quiz, 1, 1, 10);
        new MessageDAO().sendNote(alice, bob, "Hi Bob");
        new FriendsDAO().sendRequest(alice, bob);
        new ChallengeDAO().create(bob, alice, bobsQuiz, "Beat me");
        new QuizCommentDAO().add(bobsQuiz, alice, attempt, "Nice quiz");

        assertTrue(userDAO.delete(alice));

        assertNull(userDAO.findById(alice));
        assertNull(quizDAO.findById(quiz));
        assertEquals(0, attemptDAO.countByUser(bob), "Bob's attempt at Alice's quiz went with the quiz");
        assertTrue(new MessageDAO().findThreads(bob).isEmpty());
        assertTrue(new FriendsDAO().findIncomingRequests(bob).isEmpty());
        assertEquals(0, queryInt("SELECT COUNT(*) FROM challenges"));
        assertTrue(new QuizCommentDAO().findByQuiz(bobsQuiz).isEmpty());
        assertEquals(List.of("bob"), usernames(userDAO.findAll()));
    }

    private static List<String> usernames(List<User> users) {
        return users.stream().map(User::getUsername).toList();
    }
}

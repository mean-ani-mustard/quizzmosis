package com.quizzmosis.service;

import com.quizzmosis.dao.FriendsDAO;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizVisibility;
import com.quizzmosis.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Who may see a quiz. Alice (1) is the author, bob (2) is her friend, carol (3) is a stranger. */
class QuizAccessTest {

    private static final int ALICE = 1;
    private static final int BOB = 2;
    private static final int CAROL = 3;

    /** Only alice and bob are friends; no database needed. */
    private final QuizAccess access = new QuizAccess(new FriendsDAO() {
        @Override
        public boolean areFriends(int userId, int otherId) {
            return (userId == ALICE && otherId == BOB) || (userId == BOB && otherId == ALICE);
        }
    });

    @Test
    void publicQuizIsVisibleToEveryone() {
        Quiz quiz = quizBy(ALICE, QuizVisibility.PUBLIC);

        assertTrue(access.isVisibleTo(quiz, ALICE));
        assertTrue(access.isVisibleTo(quiz, BOB));
        assertTrue(access.isVisibleTo(quiz, CAROL));
    }

    @Test
    void friendsOnlyQuizIsVisibleToTheAuthorAndTheirFriends() {
        Quiz quiz = quizBy(ALICE, QuizVisibility.FRIENDS);

        assertTrue(access.isVisibleTo(quiz, ALICE));
        assertTrue(access.isVisibleTo(quiz, BOB));
        assertFalse(access.isVisibleTo(quiz, CAROL));
    }

    @Test
    void privateQuizIsVisibleOnlyToTheAuthor() {
        Quiz quiz = quizBy(ALICE, QuizVisibility.PRIVATE);

        assertTrue(access.isVisibleTo(quiz, ALICE));
        assertFalse(access.isVisibleTo(quiz, BOB), "not even to friends");
        assertFalse(access.isVisibleTo(quiz, CAROL));
    }

    @Test
    void adminsCanOpenAnyQuizButOthersNeedToSeeIt() {
        Quiz quiz = quizBy(ALICE, QuizVisibility.PRIVATE);

        assertTrue(access.canOpen(quiz, user(CAROL, true)), "an admin can check it before deleting it");
        assertFalse(access.isVisibleTo(quiz, CAROL), "but it still doesn't show up in her lists");
        assertFalse(access.canOpen(quiz, user(BOB, false)));
        assertTrue(access.canOpen(quiz, user(ALICE, false)));
    }

    private static Quiz quizBy(int ownerId, QuizVisibility visibility) {
        Quiz quiz = new Quiz();
        quiz.setOwnerId(ownerId);
        quiz.setVisibility(visibility);
        return quiz;
    }

    private static User user(int id, boolean admin) {
        User user = new User();
        user.setUserId(id);
        user.setAdmin(admin);
        return user;
    }
}

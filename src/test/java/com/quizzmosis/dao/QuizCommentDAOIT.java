package com.quizzmosis.dao;

import com.quizzmosis.model.QuizComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizCommentDAOIT extends DatabaseTestBase {

    private final QuizCommentDAO comments = new QuizCommentDAO();
    private int alice;
    private int bob;
    private int quiz;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        quiz = createQuiz(alice, "World Capitals");
    }

    @Test
    void commentsAreShownNewestFirstWithTheScore() {
        int first = comments.add(quiz, bob, saveAttempt(bob, quiz, 3, 4, 20), "Nice one");
        int second = comments.add(quiz, alice, saveAttempt(alice, quiz, 4, 4, 10), "My own quiz!");
        execute("UPDATE quiz_comments SET created_at = NOW() - INTERVAL 1 DAY WHERE comment_id = ?", first);

        List<QuizComment> list = comments.findByQuiz(quiz);

        assertEquals(List.of(second, first), list.stream().map(QuizComment::getCommentId).toList());
        QuizComment bobs = list.get(1);
        assertEquals("bob", bobs.getUsername());
        assertEquals("Nice one", bobs.getContent());
        assertEquals(3, bobs.getScore());
        assertEquals(4, bobs.getTotalQuestions());
        assertEquals(75, bobs.getPercentage());
        assertTrue(comments.findByQuiz(createQuiz(alice, "Other quiz")).isEmpty());
    }

    @Test
    void eachAttemptGetsOneComment() {
        int attempt = saveAttempt(bob, quiz, 1, 1, 5);

        assertTrue(comments.add(quiz, bob, attempt, "First thought") > 0);
        assertEquals(-1, comments.add(quiz, bob, attempt, "Second thought"));

        assertEquals("First thought", comments.findByAttempt(attempt).getContent());
        assertNull(comments.findByAttempt(saveAttempt(bob, quiz, 0, 1, 5)), "a new attempt can get its own comment");
    }

    @Test
    void aCommentStaysWhenItsResultIsCleared() {
        int attempt = saveAttempt(bob, quiz, 1, 1, 5);
        int id = comments.add(quiz, bob, attempt, "Still here");

        quizDAO.clearHistory(quiz);

        QuizComment comment = comments.findById(id);
        assertEquals("Still here", comment.getContent());
        assertNull(comment.getAttemptId());
        assertNull(comment.getScore());
        assertNull(comment.getPercentage());
    }

    @Test
    void commentsCanBeDeletedAndGoWithTheirQuiz() {
        int kept = comments.add(quiz, bob, saveAttempt(bob, quiz, 1, 1, 5), "Keep me");
        int removed = comments.add(quiz, alice, saveAttempt(alice, quiz, 1, 1, 5), "Delete me");

        assertTrue(comments.delete(removed));
        assertFalse(comments.delete(removed), "already gone");
        assertEquals(List.of(kept), comments.findByQuiz(quiz).stream().map(QuizComment::getCommentId).toList());

        quizDAO.delete(quiz);
        assertNull(comments.findById(kept));
    }
}

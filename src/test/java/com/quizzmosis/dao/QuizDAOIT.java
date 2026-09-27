package com.quizzmosis.dao;

import com.quizzmosis.model.MessageType;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizSort;
import com.quizzmosis.model.QuizVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizDAOIT extends DatabaseTestBase {

    private int alice;

    @BeforeEach
    void createOwner() {
        alice = createUser("alice");
    }

    @Test
    void createdQuizIsStoredWithItsSettings() {
        Quiz quiz = new Quiz();
        quiz.setOwnerId(alice);
        quiz.setTitle("Java Basics");
        quiz.setDescription("Check your Java knowledge.");
        quiz.setMultiplePages(true);
        quiz.setImmediateCorrection(true);
        int id = quizDAO.create(quiz);

        Quiz stored = quizDAO.findById(id);
        assertEquals("Java Basics", stored.getTitle());
        assertEquals("Check your Java knowledge.", stored.getDescription());
        assertEquals("alice", stored.getOwnerUsername());
        assertFalse(stored.isRandomOrder());
        assertTrue(stored.isMultiplePages());
        assertTrue(stored.isImmediateCorrection());
        assertNotNull(stored.getCreationDate());
    }

    @Test
    void quizCountsItsQuestionsAndPlays() {
        int quiz = createQuiz(alice, "Capitals");
        assertFalse(quizDAO.findById(quiz).isPlayable(), "no questions yet");

        addTypedQuestion(quiz, "Capital of France?", "Paris");
        addTypedQuestion(quiz, "Capital of Japan?", "Tokyo");
        saveAttempt(alice, quiz, 2, 2, 30);

        Quiz stored = quizDAO.findById(quiz);
        assertEquals(2, stored.getQuestionCount());
        assertEquals(1, stored.getAttemptCount());
        assertTrue(stored.isPlayable());
    }

    @Test
    void homePageListsOnlyQuizzesThatHaveQuestions() {
        int empty = createQuiz(alice, "Empty");
        int popular = createQuiz(alice, "Popular");
        int newest = createQuiz(alice, "Newest");
        addTypedQuestion(popular, "Q?", "A");
        addTypedQuestion(newest, "Q?", "A");
        saveAttempt(alice, popular, 1, 1, 5);
        saveAttempt(alice, popular, 1, 1, 5);

        assertEquals(List.of(popular, newest), ids(quizDAO.findPopular(alice, 10)));
        assertEquals(List.of(newest, popular), ids(quizDAO.findRecent(alice, 10)));
        assertEquals(List.of(newest, popular, empty), ids(quizDAO.findAll()), "the full list shows every quiz");
    }

    @Test
    void searchLooksAtTitleAndDescription() {
        int java = createQuiz(alice, "Java Basics");
        int capitals = createQuiz(alice, "World Capitals");

        assertEquals(List.of(java), ids(search("java")));
        assertEquals(List.of(capitals), ids(search("ALL ABOUT WORLD")), "matches the description");
        assertEquals(List.of(), ids(search("%")), "% is not a wildcard here");
        assertEquals(1, quizDAO.countVisible(alice, "java"));
        assertEquals(2, quizDAO.countVisible(alice, ""));
    }

    @Test
    void quizzesAreListedAndCountedPerOwner() {
        int bob = createUser("bob");
        int first = createQuiz(alice, "First");
        int second = createQuiz(alice, "Second");
        createQuiz(bob, "Bob's quiz");

        assertEquals(List.of(second, first), ids(quizDAO.findByOwner(alice, alice)));
        assertEquals(2, quizDAO.countByOwner(alice));
        assertEquals(3, quizDAO.count());
    }

    @Test
    void deletingAQuizDeletesItsQuestionsResultsAndChallenges() {
        int bob = createUser("bob");
        int quiz = createQuiz(alice, "Doomed");
        addMultipleChoiceQuestion(quiz, "2 + 2?", "4", "3", "5");
        int attempt = saveAttempt(bob, quiz, 1, 1, 12);
        int challenge = new ChallengeDAO().create(alice, bob, quiz, "Beat this!");
        new MessageDAO().sendChallengeUpdate(MessageType.CHALLENGE_DECLINED, bob, alice, challenge, "No thanks");
        new QuizCommentDAO().add(quiz, bob, attempt, "Too hard");

        assertTrue(quizDAO.delete(quiz));

        assertNull(quizDAO.findById(quiz));
        assertTrue(questionDAO.findByQuiz(quiz).isEmpty());
        assertEquals(0, queryInt("SELECT COUNT(*) FROM question_choices"));
        assertEquals(0, attemptDAO.countByUser(bob));
        assertEquals(0, queryInt("SELECT COUNT(*) FROM challenges"));
        assertEquals(0, queryInt("SELECT COUNT(*) FROM messages"), "the message about the challenge went with it");
        assertEquals(0, queryInt("SELECT COUNT(*) FROM quiz_comments"));
        assertFalse(quizDAO.delete(quiz), "already gone");
    }

    @Test
    void clearingHistoryOnlyRemovesThatQuizzesResults() {
        int quiz = createQuiz(alice, "Cleared");
        int other = createQuiz(alice, "Kept");
        saveAttempt(alice, quiz, 1, 1, 10);
        saveAttempt(alice, quiz, 0, 1, 10);
        saveAttempt(alice, other, 1, 1, 10);

        assertEquals(2, quizDAO.clearHistory(quiz));

        assertEquals(0, quizDAO.findById(quiz).getAttemptCount());
        assertEquals(1, quizDAO.findById(other).getAttemptCount());
    }

    @Test
    void titleDescriptionAndSettingsCanBeChanged() {
        int id = createQuiz(alice, "Draft");
        Quiz quiz = quizDAO.findById(id);
        assertNull(quiz.getTimeLimitSeconds(), "no time limit unless the author sets one");

        quiz.setTitle("Final title");
        quiz.setDescription(null);
        quiz.setRandomOrder(true);
        quiz.setMultiplePages(true);
        quiz.setImmediateCorrection(true);
        quiz.setVisibility(QuizVisibility.FRIENDS);
        quiz.setTimeLimitSeconds(300);
        assertTrue(quizDAO.update(quiz));

        Quiz saved = quizDAO.findById(id);
        assertEquals("Final title", saved.getTitle());
        assertNull(saved.getDescription());
        assertTrue(saved.isRandomOrder() && saved.isMultiplePages() && saved.isImmediateCorrection());
        assertEquals(QuizVisibility.FRIENDS, saved.getVisibility());
        assertEquals(300, saved.getTimeLimitSeconds());
        assertEquals(5, saved.getTimeLimitMinutes());
        assertEquals(alice, saved.getOwnerId(), "the owner never changes");
    }

    @Test
    void quizListCanBeSortedAndPaged() {
        int bob = createUser("bob");
        int carol = createUser("carol");
        int older = createQuiz(alice, "Older");
        int played = createQuiz(alice, "Played a lot");
        int loved = createQuiz(alice, "Loved");
        execute("UPDATE quizzes SET creation_date = NOW() - INTERVAL 3 DAY WHERE quiz_id = ?", older);
        execute("UPDATE quizzes SET creation_date = NOW() - INTERVAL 2 DAY WHERE quiz_id = ?", played);
        execute("UPDATE quizzes SET creation_date = NOW() - INTERVAL 1 DAY WHERE quiz_id = ?", loved);
        saveAttempt(bob, played, 1, 1, 10);
        saveAttempt(carol, played, 1, 1, 10);
        saveAttempt(bob, loved, 1, 1, 10);
        QuizRatingDAO ratings = new QuizRatingDAO();
        ratings.rate(loved, bob, 5);
        ratings.rate(played, bob, 2);
        ratings.rate(played, carol, 3);

        assertEquals(List.of(loved, played, older), ids(quizDAO.findVisible(alice, "", QuizSort.NEWEST, 10, 0)));
        assertEquals(List.of(played, loved, older), ids(quizDAO.findVisible(alice, "", QuizSort.POPULAR, 10, 0)));
        assertEquals(List.of(loved, played, older), ids(quizDAO.findVisible(alice, "", QuizSort.RATED, 10, 0)),
                "unrated quizzes come last");
        assertEquals(List.of(played), ids(quizDAO.findVisible(alice, "", QuizSort.NEWEST, 1, 1)), "page 2 of size 1");
        assertEquals(3, quizDAO.countVisible(alice, ""));

        Quiz ratedTwice = quizDAO.findById(played);
        assertEquals(2.5, ratedTwice.getAverageRating(), 0.001);
        assertEquals(2, ratedTwice.getRatingCount());
        assertNull(quizDAO.findById(older).getAverageRating(), "nobody rated it yet");
    }

    private List<Quiz> search(String text) {
        return quizDAO.findVisible(alice, text, QuizSort.NEWEST, 10, 0);
    }

    private static List<Integer> ids(List<Quiz> quizzes) {
        return quizzes.stream().map(Quiz::getQuizId).toList();
    }
}

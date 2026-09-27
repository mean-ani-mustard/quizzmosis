package com.quizzmosis.dao;

import com.quizzmosis.model.AttemptSummary;
import com.quizzmosis.model.ChallengeStatus;
import com.quizzmosis.model.MessageType;
import com.quizzmosis.model.Quiz;
import com.quizzmosis.model.QuizSort;
import com.quizzmosis.model.QuizVisibility;
import com.quizzmosis.model.Timeframe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every list in the app must follow the same rule: public quizzes for everyone, "friends only"
 * quizzes for the author's friends, "only me" quizzes for the author. Alice writes one quiz of each
 * kind; bob is her friend and carol is a stranger.
 */
class QuizVisibilityIT extends DatabaseTestBase {

    private final FriendsDAO friends = new FriendsDAO();
    private final LeaderboardDAO leaderboard = new LeaderboardDAO();
    private final MessageDAO messages = new MessageDAO();
    private final ChallengeDAO challenges = new ChallengeDAO();

    private int alice;
    private int bob;
    private int carol;
    private int publicQuiz;
    private int friendsQuiz;
    private int privateQuiz;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        carol = createUser("carol");
        friends.sendRequest(alice, bob);
        friends.acceptRequest(bob, alice);

        publicQuiz = createQuiz(alice, "Public quiz", QuizVisibility.PUBLIC);
        friendsQuiz = createQuiz(alice, "Friends quiz", QuizVisibility.FRIENDS);
        privateQuiz = createQuiz(alice, "Private quiz", QuizVisibility.PRIVATE);
    }

    @Test
    void visibilityIsStoredAndCanBeChanged() {
        assertEquals(QuizVisibility.FRIENDS, quizDAO.findById(friendsQuiz).getVisibility());

        setVisibility(friendsQuiz, QuizVisibility.PUBLIC);

        assertEquals(QuizVisibility.PUBLIC, quizDAO.findById(friendsQuiz).getVisibility());
    }

    @Test
    void quizListsShowOnlyWhatTheViewerMaySee() {
        assertEquals(List.of(privateQuiz, friendsQuiz, publicQuiz), ids(quizDAO.findVisibleTo(alice)));
        assertEquals(List.of(friendsQuiz, publicQuiz), ids(quizDAO.findVisibleTo(bob)));
        assertEquals(List.of(publicQuiz), ids(quizDAO.findVisibleTo(carol)));

        assertEquals(List.of(friendsQuiz, publicQuiz), ids(quizDAO.findVisible(bob, "quiz", QuizSort.NEWEST, 10, 0)));
        assertEquals(List.of(publicQuiz), ids(quizDAO.findVisible(carol, "quiz", QuizSort.NEWEST, 10, 0)));
        assertEquals(2, quizDAO.countVisible(bob, ""));
        assertEquals(1, quizDAO.countVisible(carol, "quiz"));

        assertEquals(List.of(friendsQuiz, publicQuiz), ids(quizDAO.findByOwner(alice, bob)), "alice's profile");
        assertEquals(List.of(publicQuiz), ids(quizDAO.findByOwner(alice, carol)));
    }

    @Test
    void homePageListsFollowTheSameRule() {
        for (int quiz : new int[] {publicQuiz, friendsQuiz, privateQuiz}) {
            addTypedQuestion(quiz, "Question?", "Answer");
        }

        assertEquals(List.of(friendsQuiz, publicQuiz), ids(quizDAO.findRecent(bob, 10)));
        assertEquals(List.of(publicQuiz), ids(quizDAO.findRecent(carol, 10)));
        assertEquals(List.of(publicQuiz), ids(quizDAO.findPopular(carol, 10)));
    }

    @Test
    void resultsAtHiddenQuizzesStayHidden() {
        saveAttempt(alice, publicQuiz, 1, 1, 10);
        saveAttempt(alice, friendsQuiz, 1, 1, 10);
        saveAttempt(alice, privateQuiz, 1, 1, 10);

        assertEquals(List.of("Private quiz", "Friends quiz", "Public quiz"), titles(attemptDAO.findHistory(alice, 10, 0)));
        assertEquals(List.of("Friends quiz", "Public quiz"), titles(attemptDAO.findRecentByFriendsOf(bob, 10)),
                "bob's home page: what his friends played");
        assertEquals(List.of("Friends quiz", "Public quiz"), titles(attemptDAO.findRecentByUser(alice, bob, 10)),
                "alice's profile as bob sees it");
        assertEquals(List.of("Public quiz"), titles(attemptDAO.findRecentByUser(alice, carol, 10)));
    }

    @Test
    void leaderboardOnlyCountsQuizzesTheViewerMaySee() {
        saveAttempt(carol, publicQuiz, 1, 2, 10);
        saveAttempt(bob, friendsQuiz, 2, 2, 10);
        saveAttempt(alice, privateQuiz, 2, 2, 5);

        assertEquals(List.of("alice", "bob", "carol"), usernames(leaderboard.findTop(null, Timeframe.ALL, alice, 10)));
        assertEquals(List.of("bob", "carol"), usernames(leaderboard.findTop(null, Timeframe.ALL, bob, 10)));
        assertEquals(List.of("carol"), usernames(leaderboard.findTop(null, Timeframe.ALL, carol, 10)));
    }

    @Test
    void challengeDisappearsWhenItsQuizIsMadePrivate() {
        challenges.create(alice, bob, friendsQuiz, "Try this one!");
        messages.sendNote(alice, bob, "Hi!");
        assertEquals(1, challenges.findPendingFor(bob).size());
        assertEquals(1, challenges.findHistory(bob, bob, 10).size());
        assertEquals(2, messages.countInboxBadge(bob), "the note and the challenge");

        setVisibility(friendsQuiz, QuizVisibility.PRIVATE);

        assertTrue(challenges.findPendingFor(bob).isEmpty());
        assertTrue(challenges.findHistory(bob, bob, 10).isEmpty());
        assertEquals(1, messages.countInboxBadge(bob), "only the note is left");
        assertEquals(1, challenges.findHistory(alice, alice, 10).size(), "alice still sees her own quiz");
    }

    @Test
    void newsAboutAChallengeDisappearsWithItsQuiz() {
        int carolsQuiz = createQuiz(carol, "Carol's quiz", QuizVisibility.PUBLIC);
        int challenge = challenges.create(alice, bob, carolsQuiz, "Go!");
        challenges.respond(challenge, bob, ChallengeStatus.DECLINED, "Not today");
        messages.sendChallengeUpdate(MessageType.CHALLENGE_DECLINED, bob, alice, challenge, "Not today");
        assertEquals(1, messages.findChallengeUpdates(alice).size());
        assertEquals(1, messages.countInboxBadge(alice));

        setVisibility(carolsQuiz, QuizVisibility.PRIVATE);

        assertTrue(messages.findChallengeUpdates(alice).isEmpty());
        assertEquals(0, messages.countInboxBadge(alice));
    }

    @Test
    void unfriendingHidesFriendsOnlyQuizzes() {
        friends.removeFriendship(alice, bob);

        assertEquals(List.of(publicQuiz), ids(quizDAO.findVisibleTo(bob)));
    }

    private int createQuiz(int ownerId, String title, QuizVisibility visibility) {
        Quiz quiz = new Quiz();
        quiz.setOwnerId(ownerId);
        quiz.setTitle(title);
        quiz.setVisibility(visibility);
        return quizDAO.create(quiz);
    }

    private static List<Integer> ids(List<Quiz> quizzes) {
        return quizzes.stream().map(Quiz::getQuizId).toList();
    }

    private static List<String> titles(List<AttemptSummary> rows) {
        return rows.stream().map(AttemptSummary::getQuizTitle).toList();
    }

    private static List<String> usernames(List<AttemptSummary> rows) {
        return rows.stream().map(AttemptSummary::getUsername).toList();
    }
}

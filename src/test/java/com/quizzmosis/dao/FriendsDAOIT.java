package com.quizzmosis.dao;

import com.quizzmosis.model.FriendshipStatus;
import com.quizzmosis.model.User;
import com.quizzmosis.model.UserWithStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendsDAOIT extends DatabaseTestBase {

    private final FriendsDAO friends = new FriendsDAO();
    private int alice;
    private int bob;

    @BeforeEach
    void createUsers() {
        alice = createUser("alice");
        bob = createUser("bob");
    }

    @Test
    void requestStaysPendingUntilItIsAnswered() {
        assertEquals(FriendshipStatus.REQUEST_SENT, friends.sendRequest(alice, bob));

        assertEquals(FriendshipStatus.REQUEST_SENT, friends.getStatus(alice, bob));
        assertEquals(FriendshipStatus.REQUEST_RECEIVED, friends.getStatus(bob, alice));
        assertEquals(List.of("alice"), usernames(friends.findIncomingRequests(bob)));
        assertEquals(List.of("bob"), usernames(friends.findOutgoingRequests(alice)));
        assertFalse(friends.areFriends(alice, bob));
    }

    @Test
    void acceptingARequestMakesBothUsersFriends() {
        friends.sendRequest(alice, bob);

        assertTrue(friends.acceptRequest(bob, alice));

        assertTrue(friends.areFriends(alice, bob));
        assertTrue(friends.areFriends(bob, alice));
        assertEquals(List.of("bob"), usernames(friends.findFriends(alice)));
        assertEquals(List.of("alice"), usernames(friends.findFriends(bob)));
        assertTrue(friends.findIncomingRequests(bob).isEmpty());
    }

    @Test
    void askingSomeoneWhoAlreadyAskedYouAcceptsTheirRequest() {
        friends.sendRequest(alice, bob);

        assertEquals(FriendshipStatus.FRIENDS, friends.sendRequest(bob, alice));
        assertTrue(friends.areFriends(alice, bob));
    }

    @Test
    void sendingTheSameRequestTwiceIsHarmless() {
        friends.sendRequest(alice, bob);

        assertEquals(FriendshipStatus.REQUEST_SENT, friends.sendRequest(alice, bob));
        assertEquals(1, queryInt("SELECT COUNT(*) FROM friends"));
    }

    @Test
    void requestCanBeDeclinedOrWithdrawn() {
        friends.sendRequest(alice, bob);
        assertTrue(friends.rejectRequest(bob, alice));
        assertEquals(FriendshipStatus.NONE, friends.getStatus(alice, bob));

        friends.sendRequest(alice, bob);
        assertTrue(friends.cancelRequest(alice, bob));
        assertEquals(FriendshipStatus.NONE, friends.getStatus(bob, alice));

        assertFalse(friends.acceptRequest(bob, alice), "there is nothing left to accept");
    }

    @Test
    void unfriendingRemovesTheFriendshipForBoth() {
        friends.sendRequest(alice, bob);
        friends.acceptRequest(bob, alice);

        assertTrue(friends.removeFriendship(bob, alice));

        assertEquals(FriendshipStatus.NONE, friends.getStatus(alice, bob));
        assertEquals(FriendshipStatus.NONE, friends.getStatus(bob, alice));
        assertTrue(friends.findFriends(alice).isEmpty());
    }

    @Test
    void nobodyCanBefriendThemselves() {
        assertEquals(FriendshipStatus.SELF, friends.getStatus(alice, alice));
        assertThrows(IllegalArgumentException.class, () -> friends.sendRequest(alice, alice));
    }

    @Test
    void mutualFriendsAreTheFriendsBothUsersHave() {
        int carol = createUser("carol");
        int dave = createUser("dave");
        int erin = createUser("erin");
        makeFriends(alice, carol);
        makeFriends(alice, dave);
        makeFriends(bob, dave);
        makeFriends(bob, carol);
        makeFriends(bob, erin);
        friends.sendRequest(alice, erin);

        assertEquals(List.of("carol", "dave"), usernames(friends.findMutualFriends(alice, bob)));
        assertEquals(List.of("carol", "dave"), usernames(friends.findMutualFriends(bob, alice)));
        assertEquals(List.of("alice", "bob"), usernames(friends.findMutualFriends(carol, dave)),
                "people don't have to be friends to have friends in common");
        assertTrue(friends.findMutualFriends(alice, erin).isEmpty(), "a pending request doesn't count");
    }

    @Test
    void suggestionsAreFriendsOfFriendsWithTheMostInCommonFirst() {
        int carol = createUser("carol");
        int dave = createUser("dave");
        int erin = createUser("erin");
        int frank = createUser("frank");
        createUser("grace");
        makeFriends(alice, bob);
        makeFriends(alice, carol);
        makeFriends(bob, dave);
        makeFriends(carol, dave);
        makeFriends(carol, erin);
        makeFriends(bob, frank);
        friends.sendRequest(frank, alice);

        List<UserWithStatus> suggestions = friends.findSuggestions(alice, 10);

        assertEquals(List.of("dave", "erin"), peopleNames(suggestions),
                "dave shares bob and carol, erin shares carol; frank already asked alice, grace shares nobody");
        assertEquals(List.of(2, 1), suggestions.stream().map(UserWithStatus::getMutualFriends).toList());
        assertEquals(FriendshipStatus.NONE, suggestions.get(0).getStatus());
        assertEquals(List.of("dave"), peopleNames(friends.findSuggestions(alice, 1)), "limit");
        assertTrue(friends.findSuggestions(createUser("loner"), 10).isEmpty());
    }

    @Test
    void peopleListShowsEveryoneWhoIsNotAFriendYet() {
        int carol = createUser("carol");
        int dave = createUser("dave");
        int erin = createUser("erin");
        makeFriends(alice, bob);
        makeFriends(bob, carol);
        friends.sendRequest(alice, dave);
        friends.sendRequest(erin, alice);

        List<UserWithStatus> people = friends.findPeople(alice, Set.of(), 10, 0);

        assertEquals(List.of("carol", "dave", "erin"), peopleNames(people), "bob is already a friend; carol has one in common");
        assertEquals(List.of(FriendshipStatus.NONE, FriendshipStatus.REQUEST_SENT, FriendshipStatus.REQUEST_RECEIVED),
                people.stream().map(UserWithStatus::getStatus).toList());
        assertEquals(1, people.get(0).getMutualFriends());
    }

    @Test
    void peopleListIsPagedAndLeavesOutTheSuggestions() {
        int carol = createUser("carol");
        int dave = createUser("dave");
        int erin = createUser("erin");

        assertEquals(List.of("bob", "carol"), peopleNames(friends.findPeople(alice, Set.of(), 2, 0)));
        assertEquals(List.of("dave", "erin"), peopleNames(friends.findPeople(alice, Set.of(), 2, 2)), "second page");
        assertEquals(4, friends.countPeople(alice, Set.of()));

        assertEquals(List.of("bob", "erin"), peopleNames(friends.findPeople(alice, Set.of(carol, dave), 10, 0)));
        assertEquals(2, friends.countPeople(alice, Set.of(carol, dave)));
        assertEquals(List.of(), friends.findPeople(erin, Set.of(alice, bob, carol, dave), 10, 0));
    }

    @Test
    void searchMatchesUsernameOrDisplayNameButNotTheSearcher() {
        int carol = userDAO.create("carol", "hash", "Carol Lee");
        execute("UPDATE users SET display_name = 'Bob Johnson' WHERE user_id = ?", bob);
        execute("UPDATE users SET display_name = 'Alice Johnson' WHERE user_id = ?", alice);
        makeFriends(alice, carol);

        assertEquals(List.of("bob"), peopleNames(friends.searchPeople(alice, "johnson", 10)));
        assertEquals(List.of("carol"), peopleNames(friends.searchPeople(alice, "CAR", 10)));
        assertEquals(List.of("bob", "carol"), peopleNames(friends.searchPeople(alice, "o", 10)));
        assertEquals(List.of("bob"), peopleNames(friends.searchPeople(alice, "o", 1)), "limit");

        List<UserWithStatus> results = friends.searchPeople(alice, "o", 10);
        assertEquals(FriendshipStatus.NONE, results.get(0).getStatus());
        assertEquals(FriendshipStatus.FRIENDS, results.get(1).getStatus(), "friends are found too, marked as friends");
    }

    @Test
    void wildcardCharactersInASearchAreTakenLiterally() {
        createUser("snake_case");

        assertEquals(List.of(), peopleNames(friends.searchPeople(alice, "%", 10)));
        assertEquals(List.of("snake_case"), peopleNames(friends.searchPeople(alice, "_", 10)));
    }

    private static List<String> usernames(List<User> users) {
        return users.stream().map(User::getUsername).toList();
    }

    private static List<String> peopleNames(List<UserWithStatus> people) {
        return people.stream().map(person -> person.getUser().getUsername()).toList();
    }
}

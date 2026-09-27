package com.quizzmosis.model;

/**
 * Another user as seen by the current one: how they are related, and how many friends they
 * have in common (for search results, friend suggestions and the people list).
 */
public class UserWithStatus {

    private final User user;
    private final FriendshipStatus status;
    private final int mutualFriends;

    public UserWithStatus(User user, FriendshipStatus status, int mutualFriends) {
        this.user = user;
        this.status = status;
        this.mutualFriends = mutualFriends;
    }

    public User getUser() {
        return user;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public int getMutualFriends() {
        return mutualFriends;
    }
}

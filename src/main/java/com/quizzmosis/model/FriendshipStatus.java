package com.quizzmosis.model;

/** How the current user is related to another user. */
public enum FriendshipStatus {
    /** Looking at your own profile. */
    SELF,
    NONE,
    FRIENDS,
    /** You sent them a request that they haven't answered yet. */
    REQUEST_SENT,
    /** They sent you a request that you haven't answered yet. */
    REQUEST_RECEIVED
}

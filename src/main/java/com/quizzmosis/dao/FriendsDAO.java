package com.quizzmosis.dao;

import com.quizzmosis.model.FriendshipStatus;
import com.quizzmosis.model.User;
import com.quizzmosis.model.UserWithStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Friend requests and friendships (the {@code friends} table).
 *
 * <ul>
 *   <li>a <b>pending</b> row (A, B) means A sent B a friend request;</li>
 *   <li>two <b>accepted</b> rows (A, B) and (B, A) mean A and B are friends, so "who are my
 *       friends?" is always the simple query {@code WHERE user_id = me}.</li>
 * </ul>
 * Rejecting a request or unfriending someone simply deletes the rows.
 */
public class FriendsDAO {

    private static final String FRIEND_USERS =
            "SELECT " + UserDAO.USER_COLUMNS + " FROM friends f JOIN users u ON u.user_id = ";

    /** How {@code userId} is related to {@code otherId}. */
    public FriendshipStatus getStatus(int userId, int otherId) {
        if (userId == otherId) {
            return FriendshipStatus.SELF;
        }
        String mine = rowStatus(userId, otherId);
        String theirs = rowStatus(otherId, userId);
        if ("accepted".equals(mine)) {
            return FriendshipStatus.FRIENDS;
        }
        if ("pending".equals(mine)) {
            return FriendshipStatus.REQUEST_SENT;
        }
        if ("pending".equals(theirs)) {
            return FriendshipStatus.REQUEST_RECEIVED;
        }
        return FriendshipStatus.NONE;
    }

    public boolean areFriends(int userId, int otherId) {
        return getStatus(userId, otherId) == FriendshipStatus.FRIENDS;
    }

    /**
     * Sends a friend request. If the other user already sent us one, it is accepted instead,
     * and nothing happens if the two users are already friends or a request is already pending.
     *
     * @return the relationship afterwards
     */
    public FriendshipStatus sendRequest(int fromUserId, int toUserId) {
        requireDifferentUsers(fromUserId, toUserId);
        FriendshipStatus status = getStatus(fromUserId, toUserId);
        switch (status) {
            case REQUEST_RECEIVED:
                acceptRequest(fromUserId, toUserId);
                return FriendshipStatus.FRIENDS;
            case NONE:
                // "ON DUPLICATE KEY UPDATE" turns a double-click into a harmless no-op.
                Jdbc.update("INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, 'pending') "
                        + "ON DUPLICATE KEY UPDATE user_id = user_id", fromUserId, toUserId);
                return FriendshipStatus.REQUEST_SENT;
            default:
                return status;
        }
    }

    /**
     * {@code receiverId} accepts the request {@code senderId} sent them: the pending row becomes
     * accepted and the reverse row is added, both in one transaction.
     *
     * @return {@code false} if there was no such pending request
     */
    public boolean acceptRequest(int receiverId, int senderId) {
        requireDifferentUsers(receiverId, senderId);
        return Jdbc.inTransaction(connection -> {
            int updated = Jdbc.update(connection,
                    "UPDATE friends SET status = 'accepted' "
                            + "WHERE user_id = ? AND friend_id = ? AND status = 'pending'",
                    senderId, receiverId);
            if (updated == 0) {
                return false;
            }
            Jdbc.update(connection,
                    "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, 'accepted') "
                            + "ON DUPLICATE KEY UPDATE status = 'accepted'",
                    receiverId, senderId);
            return true;
        });
    }

    /** {@code receiverId} declines the request from {@code senderId}. */
    public boolean rejectRequest(int receiverId, int senderId) {
        requireDifferentUsers(receiverId, senderId);
        return deletePendingRequest(senderId, receiverId);
    }

    /** {@code senderId} takes back the request they sent to {@code receiverId}. */
    public boolean cancelRequest(int senderId, int receiverId) {
        requireDifferentUsers(senderId, receiverId);
        return deletePendingRequest(senderId, receiverId);
    }

    private boolean deletePendingRequest(int senderId, int receiverId) {
        return Jdbc.update("DELETE FROM friends WHERE user_id = ? AND friend_id = ? AND status = 'pending'",
                senderId, receiverId) > 0;
    }

    /** Unfriends two users, or cancels a request between them, whichever direction it went. */
    public boolean removeFriendship(int userId, int otherId) {
        requireDifferentUsers(userId, otherId);
        return Jdbc.update("DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)",
                userId, otherId, otherId, userId) > 0;
    }

    public List<User> findFriends(int userId) {
        return Jdbc.query(FRIEND_USERS + "f.friend_id WHERE f.user_id = ? AND f.status = 'accepted' "
                + "ORDER BY u.display_name", UserDAO::map, userId);
    }

    /** People who sent {@code userId} a request that is still pending. */
    public List<User> findIncomingRequests(int userId) {
        return Jdbc.query(FRIEND_USERS + "f.user_id WHERE f.friend_id = ? AND f.status = 'pending' "
                + "ORDER BY f.request_date DESC", UserDAO::map, userId);
    }

    /** People {@code userId} sent a request to that is still pending. */
    public List<User> findOutgoingRequests(int userId) {
        return Jdbc.query(FRIEND_USERS + "f.friend_id WHERE f.user_id = ? AND f.status = 'pending' "
                + "ORDER BY f.request_date DESC", UserDAO::map, userId);
    }

    /** Friends that both users have (for "mutual friends" on profiles). */
    public List<User> findMutualFriends(int userId, int otherId) {
        return Jdbc.query("SELECT " + UserDAO.USER_COLUMNS + " FROM friends mine "
                        + "JOIN friends theirs ON theirs.friend_id = mine.friend_id "
                        + "JOIN users u ON u.user_id = mine.friend_id "
                        + "WHERE mine.user_id = ? AND mine.status = 'accepted' "
                        + "AND theirs.user_id = ? AND theirs.status = 'accepted' ORDER BY u.display_name",
                UserDAO::map, userId, otherId);
    }

    /**
     * Every other user, seen from the user whose id goes into the four "?": how many friends
     * they have in common, and the request each of them sent the other (if any). Wrapped in a
     * derived table ("people") so the WHERE clause can use those computed columns.
     */
    private static final String PEOPLE = "SELECT * FROM (SELECT " + UserDAO.USER_COLUMNS + ", "
            + "(SELECT COUNT(*) FROM friends mine JOIN friends theirs ON theirs.friend_id = mine.friend_id "
            + " WHERE mine.user_id = ? AND mine.status = 'accepted' "
            + " AND theirs.user_id = u.user_id AND theirs.status = 'accepted') AS mutual_count, "
            + "(SELECT sent.status FROM friends sent WHERE sent.user_id = ? AND sent.friend_id = u.user_id) AS sent_status, "
            + "(SELECT got.status FROM friends got WHERE got.user_id = u.user_id AND got.friend_id = ?) AS received_status "
            + "FROM users u WHERE u.user_id <> ?) people ";

    /**
     * Friends of friends that the user has no friendship or request with yet, the most mutual
     * friends first.
     */
    public List<UserWithStatus> findSuggestions(int userId, int limit) {
        return Jdbc.query(PEOPLE + "WHERE people.sent_status IS NULL AND people.received_status IS NULL "
                        + "AND people.mutual_count > 0 ORDER BY people.mutual_count DESC, people.display_name LIMIT ?",
                FriendsDAO::mapPerson, userId, userId, userId, userId, limit);
    }

    /**
     * One page of everybody the user isn't friends with (yet), the most mutual friends first,
     * leaving out {@code excludedIds} (the people already suggested above the list).
     */
    public List<UserWithStatus> findPeople(int userId, Collection<Integer> excludedIds, int limit, int offset) {
        List<Object> params = peopleParams(userId, excludedIds);
        params.add(limit);
        params.add(offset);
        return Jdbc.query(PEOPLE + peopleWhere(excludedIds)
                        + " ORDER BY people.mutual_count DESC, people.display_name LIMIT ? OFFSET ?",
                FriendsDAO::mapPerson, params.toArray());
    }

    /** How many people {@link #findPeople} can show in total. */
    public int countPeople(int userId, Collection<Integer> excludedIds) {
        return Jdbc.queryInt("SELECT COUNT(*) FROM (" + PEOPLE + peopleWhere(excludedIds) + ") counted",
                peopleParams(userId, excludedIds).toArray());
    }

    private static String peopleWhere(Collection<Integer> excludedIds) {
        String where = "WHERE (people.sent_status IS NULL OR people.sent_status <> 'accepted')";
        if (!excludedIds.isEmpty()) {
            where += " AND people.user_id NOT IN (" + String.join(", ", Collections.nCopies(excludedIds.size(), "?")) + ")";
        }
        return where;
    }

    private static List<Object> peopleParams(int userId, Collection<Integer> excludedIds) {
        List<Object> params = new ArrayList<>(List.of(userId, userId, userId, userId));
        params.addAll(excludedIds);
        return params;
    }

    /**
     * Other users whose username or display name contains {@code text} (friends included), with
     * how each of them is related to the searcher.
     */
    public List<UserWithStatus> searchPeople(int userId, String text, int limit) {
        String pattern = Jdbc.likePattern(text);
        return Jdbc.query(PEOPLE + "WHERE people.username LIKE ? OR people.display_name LIKE ? "
                        + "ORDER BY people.username LIMIT ?",
                FriendsDAO::mapPerson, userId, userId, userId, userId, pattern, pattern, limit);
    }

    private static UserWithStatus mapPerson(ResultSet rs) throws SQLException {
        String sent = rs.getString("sent_status");
        FriendshipStatus status;
        if ("accepted".equals(sent)) {
            status = FriendshipStatus.FRIENDS;
        } else if ("pending".equals(sent)) {
            status = FriendshipStatus.REQUEST_SENT;
        } else if ("pending".equals(rs.getString("received_status"))) {
            status = FriendshipStatus.REQUEST_RECEIVED;
        } else {
            status = FriendshipStatus.NONE;
        }
        return new UserWithStatus(UserDAO.map(rs), status, rs.getInt("mutual_count"));
    }

    private String rowStatus(int userId, int friendId) {
        return Jdbc.queryOne("SELECT status FROM friends WHERE user_id = ? AND friend_id = ?",
                rs -> rs.getString(1), userId, friendId);
    }

    private static void requireDifferentUsers(int a, int b) {
        if (a == b) {
            throw new IllegalArgumentException("A user can't be their own friend");
        }
    }
}

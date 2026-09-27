package com.quizzmosis.model;

import java.sql.Timestamp;
import java.util.List;

/** A conversation between two users: the note that started it and every reply, oldest first. */
public class NoteThread {

    /** The id of the first note; replies point to it with {@code thread_id}. */
    private final int threadId;
    /** The other person in the conversation (seen from the user looking at it). */
    private final int otherUserId;
    private final String otherDisplayName;
    private final List<Message> messages;
    private final boolean unread;
    /** Replies are only possible while the two are friends. Set by the servlet. */
    private boolean replyAllowed;

    public NoteThread(int threadId, int otherUserId, String otherDisplayName, List<Message> messages, boolean unread) {
        this.threadId = threadId;
        this.otherUserId = otherUserId;
        this.otherDisplayName = otherDisplayName;
        this.messages = List.copyOf(messages);
        this.unread = unread;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getOtherUserId() {
        return otherUserId;
    }

    public String getOtherDisplayName() {
        return otherDisplayName;
    }

    public List<Message> getMessages() {
        return messages;
    }

    /** Whether the conversation contains something the user hasn't read yet. */
    public boolean isUnread() {
        return unread;
    }

    public Message getLastMessage() {
        return messages.get(messages.size() - 1);
    }

    public Timestamp getLastActivity() {
        return getLastMessage().getSentAt();
    }

    public boolean isReplyAllowed() {
        return replyAllowed;
    }

    public void setReplyAllowed(boolean replyAllowed) {
        this.replyAllowed = replyAllowed;
    }
}

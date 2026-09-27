package com.quizzmosis.dao;

import com.quizzmosis.model.Message;
import com.quizzmosis.model.MessageType;
import com.quizzmosis.model.NoteThread;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How notes are grouped into conversations (no database needed). */
class NoteThreadGroupingTest {

    private static final int ALICE = 1;
    private static final int BOB = 2;
    private static final int CAROL = 3;

    @Test
    void repliesJoinTheConversationOfTheirFirstNote() {
        List<Message> notes = List.of(
                note(10, ALICE, BOB, null, 1, true),
                note(11, CAROL, ALICE, null, 2, true),
                note(12, BOB, ALICE, 10, 3, false),
                note(13, ALICE, BOB, 10, 4, true));

        List<NoteThread> threads = MessageDAO.groupIntoThreads(notes, ALICE);

        assertEquals(2, threads.size());
        NoteThread withBob = threads.get(0);
        assertEquals(10, withBob.getThreadId(), "bob's conversation had the latest note");
        assertEquals(BOB, withBob.getOtherUserId());
        assertEquals("user2", withBob.getOtherDisplayName());
        assertEquals(List.of(10, 12, 13), withBob.getMessages().stream().map(Message::getMessageId).toList());
        assertTrue(withBob.isUnread(), "bob's reply to alice is unread");

        NoteThread withCarol = threads.get(1);
        assertEquals(CAROL, withCarol.getOtherUserId());
        assertFalse(withCarol.isUnread());
    }

    @Test
    void onlyNotesTheUserReceivedCanBeUnread() {
        List<Message> notes = List.of(note(20, ALICE, BOB, null, 1, false));

        assertFalse(MessageDAO.groupIntoThreads(notes, ALICE).get(0).isUnread(), "alice sent it; bob hasn't read it yet");
        assertTrue(MessageDAO.groupIntoThreads(notes, BOB).get(0).isUnread());
    }

    @Test
    void conversationsWithTheSameLastMinuteAreOrderedByTheirNewestNote() {
        List<Message> notes = List.of(
                note(30, ALICE, BOB, null, 5, true),
                note(31, CAROL, ALICE, null, 5, true));

        List<NoteThread> threads = MessageDAO.groupIntoThreads(notes, ALICE);

        assertEquals(List.of(31, 30), threads.stream().map(NoteThread::getThreadId).toList());
    }

    /** A note sent {@code minute} minutes after a fixed moment; display names are "user" + id. */
    private static Message note(int id, int sender, int receiver, Integer threadId, int minute, boolean read) {
        Message message = new Message();
        message.setMessageId(id);
        message.setSenderId(sender);
        message.setSenderDisplayName("user" + sender);
        message.setReceiverId(receiver);
        message.setReceiverDisplayName("user" + receiver);
        message.setType(MessageType.NOTE);
        message.setContent("Note " + id);
        message.setThreadId(threadId);
        message.setRead(read);
        message.setSentAt(new Timestamp(1_700_000_000_000L + minute * 60_000L));
        return message;
    }
}

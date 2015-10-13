package com.dfbnc.commands;

import com.dfbnc.sockets.UserSocket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link com.dfbnc.commands.CommandOutputBuffer}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandOutputBufferTest {

    @Mock private UserSocket userSocket;
    private CommandOutputBuffer buffer;

    @Before
    public void setUp() {
        buffer = new CommandOutputBuffer(userSocket);
    }

    /**
     * Tests that string formatting is applied correctly when messages are added.
     */
    @Test
    public void testMessagesWithArguments() {
        // When messages with arguments are added
        buffer.addBotMessage("test %s", "1");
        buffer.addBotMessage("test %s %s", "2", "3");

        // Then the arguments are formatted properly
        List<String> messages = buffer.getMessages();
        assertEquals(2, messages.size());
        assertEquals("test 1", messages.get(0));
        assertEquals("test 2 3", messages.get(1));
    }

    /**
     * Tests that filtering messages removes them from the buffer correctly.
     */
    @Test
    public void testFilteringMessages() {
        // Given a bunch of messages have been added
        buffer.addBotMessage("test1");
        buffer.addBotMessage("test2");
        buffer.addBotMessage("test3");
        buffer.addBotMessage("test4");

        // When we remove messages matching a filter
        buffer.removeMessagesIf(s -> s.endsWith("2") || s.endsWith("4"));

        // Then only the remaining messages are returned
        List<String> messages = buffer.getMessages();
        assertEquals(2, messages.size());
        assertEquals("test1", messages.get(0));
        assertEquals("test3", messages.get(1));
    }

    /**
     * Tests that the message list can be completely replaced.
     */
    @Test
    public void testReplaceMessages() {
        // Given a bunch of messages have been added
        buffer.addBotMessage("test1");
        buffer.addBotMessage("test2");

        // When the messages are replaced with a new list
        List<String> newMessages = new ArrayList<>();
        newMessages.add("test4");
        newMessages.add("test5");
        buffer.setMessages(newMessages);

        // Then the newly added messages are the only ones returned
        List<String> messages = buffer.getMessages();
        assertEquals(2, messages.size());
        assertEquals("test4", messages.get(0));
        assertEquals("test5", messages.get(1));
    }

    /**
     * Tests that messages actually get sent!
     */
    @Test
    public void testSendMessages() {
        // Given a bunch of messages have been added
        buffer.addBotMessage("test1");
        buffer.addBotMessage("test%s", "2");

        // When we tell the buffer to send them
        buffer.send();

        // Then they're passed on to the socket
        verify(userSocket).sendBotMessage("test1");
        verify(userSocket).sendBotMessage("test2");
    }

}
/*
 * Copyright (c) 2006-2013 Shane Mc Cormack
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dfbnc.commands;

import com.dfbnc.config.Config;
import com.dfbnc.sockets.UserSocket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.when;

/**
 * Test Command Manager as per http://code.google.com/p/dfbnc/wiki/CommandManagerInfo
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandManagerTest {

	/** Array of CommandManagers to allow for testing */
	private final CommandManager[] commandManager = new CommandManager[5];

	/** Config to use for command managers. */
	@Mock private Config mockConfig;

	/**
	 * This guarantees a fresh set of CommandManagers for each test.
	 */
	@Before
	public void setUp() {
		for (int i = 0; i < commandManager.length; ++i) {
			commandManager[i] = new CommandManager(mockConfig);
		}
	}

	/**
	 * Test to make sure that a CommandManager does not allow itself to be added
	 * as a subCommandManager.
	 */
	@Test
	public void DontAllowSelfAsSub() {
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-self Manager to be added", commandManager[0].addSubCommandManager(commandManager[1]));
		// Don't allow self
		assertFalse("CommandManager allowed it self to be added", commandManager[0].addSubCommandManager(commandManager[0]));
	}

	/**
	 * Test to make sure that a CommandManager does not allow duplicates to be
	 * added.
	 */
	@Test
	public void DontAllowDuplicate() {
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-duplicate Manager to be added", commandManager[0].addSubCommandManager(commandManager[1]));
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-duplicate Manager to be added", commandManager[0].addSubCommandManager(commandManager[2]));
		// Don't allow duplicate
		assertFalse("CommandManager allowed duplicate to be added", commandManager[0].addSubCommandManager(commandManager[2]));
	}

	/**
	 * Test to make sure that a CommandManager does not allow deep duplicates to be
	 * added.
	 */
	@Test
	public void DontAllowDeepDuplicate() {
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-duplicate Manager to be added", commandManager[0].addSubCommandManager(commandManager[1]));
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-duplicate Manager to be added", commandManager[0].addSubCommandManager(commandManager[2]));
		// SubManager should allow this
		assertTrue("SubCommandManager didn't allow a Manager to be added", commandManager[1].addSubCommandManager(commandManager[3]));
		// Don't allow deep duplicate
		assertFalse("CommandManager allowed deep-duplicate to be added", commandManager[0].addSubCommandManager(commandManager[3]));
	}

	/**
	 * This is another test for deep duplicates, adding a subnode that has a duplicate
	 */
	@Test
	public void DontAllowDeepDuplicate2() {
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-duplicate Manager to be added", commandManager[0].addSubCommandManager(commandManager[2]));
		// Allow non-duplicate
		assertTrue("CommandManager didn't allow duplicate to be added", commandManager[0].addSubCommandManager(commandManager[3]));
		// SubManager should allow this
		assertTrue("SubCommandManager didn't allow a-non Duplicate to be added", commandManager[1].addSubCommandManager(commandManager[3]));
		// Don't allow the manager with the duplicate
		assertFalse("CommandManager allowed subCommandManager with duplicate to be added", commandManager[0].addSubCommandManager(commandManager[1]));
	}

	/**
	 * This is another test for deep duplicates, adding a duplicate to a subnode
	 * This will be allowed as the the subnode has no way of tellig that this is
	 * a duplicate.
	 */
	@Test
	public void AllowDeepDuplicate() {
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-duplicate Manager to be added", commandManager[0].addSubCommandManager(commandManager[1]));
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-duplicate Manager to be added", commandManager[0].addSubCommandManager(commandManager[2]));
		// Allow non-duplicate
		assertTrue("CommandManager didn't allow duplicate to be added", commandManager[0].addSubCommandManager(commandManager[3]));
		// Allow the Duplicate
		assertTrue("CommandManager allowed subCommandManager with duplicate to be added", commandManager[1].addSubCommandManager(commandManager[2]));
	}

	/**
	 * This tests that recursion is not possible via AllowDeepDuplicate
	 */
	@Test
	public void AllowDeepDuplicate2() {
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-duplicate Manager to be added", commandManager[0].addSubCommandManager(commandManager[1]));
		// Allow another command Manager
		assertTrue("CommandManager didn't allow a non-duplicate Manager to be added", commandManager[0].addSubCommandManager(commandManager[2]));
		// Allow non-duplicate
		assertTrue("CommandManager didn't allow duplicate to be added", commandManager[0].addSubCommandManager(commandManager[3]));
		// Don't Allow the Duplicate
		assertFalse("CommandManager allowed subCommandManager with recursion", commandManager[1].addSubCommandManager(commandManager[0]));
	}

	/**
	 * Tests the behaviour of hasSubCommandManager() with direct submanagers.
	 */
	@Test
	public void testHasDirectSubManager() {
		// Given a manager with some direct submanagers
		commandManager[0].addSubCommandManager(commandManager[1]);
		commandManager[0].addSubCommandManager(commandManager[2]);

		// Then hasSubCommandManager returns true for those managers
		assertTrue(commandManager[0].hasSubCommandManager(commandManager[1]));
		assertTrue(commandManager[0].hasSubCommandManager(commandManager[2]));

		// And false for any others
		assertFalse(commandManager[0].hasSubCommandManager(commandManager[3]));
		assertFalse(commandManager[0].hasSubCommandManager(commandManager[4]));
	}


	/**
	 * Tests the behaviour of hasSubCommandManager() with indirect submanagers.
	 */
	@Test
	public void testHasIndirectSubManager() {
		// Given a manager with some a tree of submanagers
		commandManager[0].addSubCommandManager(commandManager[1]);
		commandManager[1].addSubCommandManager(commandManager[2]);
		commandManager[2].addSubCommandManager(commandManager[3]);

		// Then the root's hasSubCommandManager returns true for all included managers
		assertTrue(commandManager[0].hasSubCommandManager(commandManager[1]));
		assertTrue(commandManager[0].hasSubCommandManager(commandManager[2]));
		assertTrue(commandManager[0].hasSubCommandManager(commandManager[3]));

		// And false for any others
		assertFalse(commandManager[0].hasSubCommandManager(commandManager[4]));
	}

	/**
	 * Tests that after adding a command it is returned by getAllCommands().
	 */
	@Test
	public void testAddCommand() {
		// Assume that the command manager starts empty.
		assumeTrue(commandManager[0].getAllCommands(true).isEmpty());

		// When we add a command
		Command command = new FakeCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command);

		// Then getAllCommands() returns a map with a single entry.
		Map<String, Command> allCommands = commandManager[0].getAllCommands(true);
		assertEquals(1, allCommands.size());
		assertTrue(allCommands.containsKey("stuff"));
		assertSame(command, allCommands.get("stuff"));
	}

	/**
	 * Tests that after removing a command it is no longer returned by getAllCommands().
	 */
	@Test
	public void testRemoveCommand() {
		// Given a command manager with one command
		Command command = new FakeCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command);
		assumeTrue(commandManager[0].getAllCommands(true).size() == 1);

		// When we delete the command
		commandManager[0].delCommand(command);

		// Then nothing is left
		Map<String, Command> allCommands = commandManager[0].getAllCommands(true);
		assertTrue(allCommands.isEmpty());
	}

	/**
	 * Tests that adding a command with the same 'handles' replaces previously added commands.
	 */
	@Test
	public void testReplaceCommand() {
		// Assume that the command manager starts empty.
		assumeTrue(commandManager[0].getAllCommands(true).isEmpty());

		// When we add a command
		Command command1 = new FakeCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command1);

		// And then replace it
		Command command2 = new FakeCommand2(commandManager[0], "stuff");
		commandManager[0].addCommand(command2);

		// Then getAllCommands() returns a map with a single entry ...
		Map<String, Command> allCommands = commandManager[0].getAllCommands(true);
		assertEquals(1, allCommands.size());
		assertTrue(allCommands.containsKey("stuff"));

		// ... corresponding to the most-recently added command
		assertSame(command2, allCommands.get("stuff"));
	}

	/**
	 * Tests that admin only commands are excluded from getAllCommands().
	 */
	@Test
	public void testAdminOnlyCommands() {
		// Assume that the command manager starts empty.
		assumeTrue(commandManager[0].getAllCommands(true).isEmpty());

		// When we add an admin-only command
		Command command = new AdminOnlyCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command);

		// Then getAllCommands() returns nothing if admin commands are disallowed
		Map<String, Command> allCommands = commandManager[0].getAllCommands(false);
		assertTrue(allCommands.isEmpty());
	}

	/**
	 * Tests retrieving commands that start with a certain value.
	 */
	@Test
	public void testCommandsStartingWith() {
		// Assume that the command manager starts empty.
		assumeTrue(commandManager[0].getAllCommands(true).isEmpty());

		// When we add some commands
		Command command1 = new FakeCommand(commandManager[0], "stuff1", "stuff2", "foo");
		commandManager[0].addCommand(command1);
		Command command2 = new FakeCommand2(commandManager[0], "far");
		commandManager[0].addCommand(command2);

		// Then commands starting with the specified prefix are returned
		Map<String, Command> stCommands = commandManager[0].getAllCommands("st", false);
		assertEquals(2, stCommands.size());
		assertEquals(command1, stCommands.get("stuff1"));
		assertEquals(command1, stCommands.get("stuff2"));

		Map<String, Command> fCommands = commandManager[0].getAllCommands("f", false);
		assertEquals(2, fCommands.size());
		assertEquals(command1, fCommands.get("foo"));
		assertEquals(command2, fCommands.get("far"));
	}

	/**
	 * Tests that if a sub-manager contains a command with the same 'handles', then the parent command takes priority
	 */
	@Test
	public void testCommandsFromSubManagersDontReplaceParentCommands() {
		// Assume that the command managers starts empty.
		assumeTrue(commandManager[0].getAllCommands(true).isEmpty());
		assumeTrue(commandManager[1].getAllCommands(true).isEmpty());

		// When we have a sub-manager
		commandManager[0].addSubCommandManager(commandManager[1]);

		// And the two managers have a command with the same 'handles'...
		Command command1 = new FakeCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command1);
		Command command2 = new FakeCommand2(commandManager[1], "stuff");
		commandManager[1].addCommand(command2);

		// Then only the parent command is returned for queries with no text
		Map<String, Command> allCommands = commandManager[0].getAllCommands(true);
		assertEquals(1, allCommands.size());
		assertSame(command1, allCommands.get("stuff"));

		// And only the parent command is returned for queries with a substring test
		Map<String, Command> stCommands = commandManager[0].getAllCommands("st", true);
		assertEquals(1, stCommands.size());
		assertSame(command1, stCommands.get("stuff"));
	}

	/**
	 * Tests that getMatchingCommand() correctly resolves a short command into a single match.
	 */
	@Test
	public void testGetMatchingCommandWithSimpleShortCommand() {
		// Given a simple command
		Command command = new FakeCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command);

		// Given short commands are enabled
		when(mockConfig.getOptionBool("general", "allowshortcommands")).thenReturn(true);

		// When we try to match it with a shortened version
		Optional<Map.Entry<String, Command>> match = commandManager[0].getMatchingCommand("st", true);

		// Then the command is returned
		assertTrue(match.isPresent());
		assertEquals("stuff", match.get().getKey());
		assertSame(command, match.get().getValue());
	}

	/**
	 * Tests that getMatchingCommand() does not expand short commands if the setting is disabled.
	 */
	@Test
	public void testGetMatchingCommandWithShortCommandsDisabled() {
		// Given a simple command
		Command command = new FakeCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command);

		// Given short commands are disabled
		when(mockConfig.getOptionBool("general", "allowshortcommands")).thenReturn(false);

		// When we try to match it with a shortened version
		Optional<Map.Entry<String, Command>> match = commandManager[0].getMatchingCommand("st", true);

		// Then nothing is returned
		assertFalse(match.isPresent());
	}

	/**
	 * Tests that getMatchingCommand() doesn't resolve short commands if the command doesn't allow it.
	 */
	@Test
	public void testGetMatchingCommandWithShortCommandNotAllowed() {
		// Given a simple command that doesn't allow itself to be shortened
		Command command = new NonShortCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command);

		// Given short commands are enabled
		when(mockConfig.getOptionBool("general", "allowshortcommands")).thenReturn(true);

		// When we try to match it with a shortened version
		Optional<Map.Entry<String, Command>> match = commandManager[0].getMatchingCommand("st", true);

		// Then nothing is returned
		assertFalse(match.isPresent());
	}

	/**
	 * Tests that getMatchingCommand() returns nothing if a short command matches multiple commands.
	 */
	@Test
	public void testGetMatchingCommandWithAmbiguousShortCommand() {
		// Given two simple commands
		Command command1 = new FakeCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command1);
		Command command2 = new FakeCommand(commandManager[0], "staff");
		commandManager[0].addCommand(command2);

		// Given short commands are enabled
		when(mockConfig.getOptionBool("general", "allowshortcommands")).thenReturn(true);

		// When we try to match with an ambiguous shortened version
		Optional<Map.Entry<String, Command>> match = commandManager[0].getMatchingCommand("st", true);

		// Then nothing is returned
		assertFalse(match.isPresent());
	}

	/**
	 * Tests that getMatchingCommand() returns a single command even if its "handles" match multiple times.
	 */
	@Test
	public void testGetMatchingCommandWithMultipleMatchingHandles() {
		// Given a simple command with multiple handles
		Command command = new FakeCommand(commandManager[0], "meh", "stuff", "staff", "stop");
		commandManager[0].addCommand(command);

		// Given short commands are enabled
		when(mockConfig.getOptionBool("general", "allowshortcommands")).thenReturn(true);

		// When we try to match with an ambiguous shortened version
		Optional<Map.Entry<String, Command>> match = commandManager[0].getMatchingCommand("st", true);

		// Then the command is returned, and the first matching handles entry is used as the key
		assertTrue(match.isPresent());
		assertEquals("stuff", match.get().getKey());
		assertSame(command, match.get().getValue());
	}

	/**
	 * Tests that getMatchingCommand() returns a single non-hidden command if there's an ambiguous short command.
	 */
	@Test
	public void testGetMatchingCommandWithHiddenAmbiguousShortCommand() {
		// Given two simple commands
		Command command1 = new FakeCommand(commandManager[0], "stuff");
		commandManager[0].addCommand(command1);
		Command command2 = new FakeCommand(commandManager[0], "*staff");
		commandManager[0].addCommand(command2);

		// Given short commands are enabled
		when(mockConfig.getOptionBool("general", "allowshortcommands")).thenReturn(true);

		// When we try to match with an ambiguous shortened version
		Optional<Map.Entry<String, Command>> match = commandManager[0].getMatchingCommand("st", true);

		// Then the non-hidden is returned
		assertTrue(match.isPresent());
		assertEquals("stuff", match.get().getKey());
		assertSame(command1, match.get().getValue());
	}

	/**
	 * Tests that getCommand() correctly matches a hidden command.
	 */
	@Test
	public void testGetHiddenCommand() {
		// Given a simple hidden command
		Command command = new FakeCommand(commandManager[0], "*staff");
		commandManager[0].addCommand(command);

		// When we try to retrieve the hidden command
		Optional<Command> match = commandManager[0].getCommand("staff");

		// Then it's returned
		assertTrue(match.isPresent());
		assertSame(command, match.get());
	}

	private static class FakeCommand extends Command {

		private final String[] handles;

		protected FakeCommand(CommandManager manager, String... handles) {
			super(manager);
			this.handles = handles;
		}

		@Override
		public void handle(UserSocket user, String[] params, CommandOutputBuffer output) {
			// Do nothing
		}

		@Override
		public String[] handles() {
			return handles;
		}

		@Override
		public String getDescription(String command) {
			return "description";
		}

	}

	private static class FakeCommand2 extends FakeCommand {

		protected FakeCommand2(CommandManager manager, String... handles) {
			super(manager, handles);
		}

	}

	private static class AdminOnlyCommand extends FakeCommand {

		protected AdminOnlyCommand(CommandManager manager, String... handles) {
			super(manager, handles);
		}

		@Override
		public boolean isAdminOnly() {
			return true;
		}

	}

	private static class NonShortCommand extends FakeCommand {

		public NonShortCommand(CommandManager manager, String... handles) {
			super(manager, handles);
		}

		@Override
		public boolean allowShort(String handle) {
			return false;
		}
	}

}
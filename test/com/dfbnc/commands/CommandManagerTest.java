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

import com.dfbnc.commands.CommandManager;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Test Command Manager as per http://code.google.com/p/dfbnc/wiki/CommandManagerInfo
 */
public class CommandManagerTest {
	/** Array of CommandManagers to allow for testing */
	CommandManager[] commandManager = new CommandManager[5];

	/**
	 * This guarentees a fresh set of CommandManagers for each test.
	 */
	@Before
	public void setUp() {
		for (int i = 0; i < commandManager.length; ++i) {
			commandManager[i] = new CommandManager();
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
}
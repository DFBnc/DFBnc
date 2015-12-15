/*
 * Copyright (c) 2006-2015 DFBnc Developers
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

import com.dfbnc.sockets.UserSocket;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Map;

/**
 * This file represents a set-type command..
 * Subclasses define what settings are settable, and what domain to use.
 */
public abstract class AbstractSetCommand extends Command {

    /** Valid Parameters. */
    protected Map<String, ParamInfo> validParams = new HashMap<>();

    /** Domain to set. */
    protected String setDomain = "misc";

    /**
     * Handle a Set-style command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     * @param output CommandOutputBuffer where output from this command should go.
     */
    @Override
    public void handle(final UserSocket user, final String[] params, final CommandOutputBuffer output) {

        String[] actualParams = params;

        // TODO: I don't really like this.
        String wantedClientID = user.getClientID();
        if (actualParams.length > 1 && actualParams[1].equalsIgnoreCase("--global")) {
            wantedClientID = null;
            actualParams = Arrays.copyOfRange(actualParams, 1, actualParams.length);
            actualParams[0] = params[0];
        } else if (actualParams.length > 1 && actualParams[1].equalsIgnoreCase("--subclient")) {
            if (actualParams.length > 2) {
                wantedClientID = actualParams[2];
                actualParams = Arrays.copyOfRange(actualParams, 2, actualParams.length);
                actualParams[0] = params[0];
            } else {
                output.addBotMessage("You must specify a subclient.");
                return;
            }
        }

        if (wantedClientID == null) {
            output.addBotMessage("[Editing Global Client Settings]");
        } else {
            output.addBotMessage("[Editing Sub-Client Settings for: %s]", actualParams[2]);
        }
        output.addBotMessage("");

        if (actualParams.length > 1) {
            actualParams[1] = getFullParam(output, actualParams, 1, validParams.keySet());
            if (actualParams[1] == null) { return; }

            if (validParams.get(actualParams[1].toLowerCase()) == null) {
                output.addBotMessage("Invalid setting '%s'.", actualParams[1]);
                output.addBotMessage("");
                output.addBotMessage("Valid settings are:");
                for (String param : validParams.keySet()) {
                    final String description = validParams.get(param).getDescription();
                    final String wantedClient = validParams.get(param).isSubClient() ? wantedClientID : null;
                    final String value = user.getAccount().getConfig(wantedClient).getOption(setDomain, param);
                    if (validParams.get(param).isSubClient()) {
                        final String globalValue = user.getAccount().getAccountConfig().getOption(setDomain, param);
                        output.addBotMessage("  %15s - %s [Current: %s]  [Account-Level: %s]", param, description, value, globalValue);
                    } else {
                        output.addBotMessage("  %15s - %s [Current: %s]", param, description, value);
                    }
                }
                return;
            }

            final ParamInfo pi = validParams.get(actualParams[1].toLowerCase());
            // Get the current value
            final String wantedClient = pi.isSubClient() ? wantedClientID : null;
            final String currentValue = user.getAccount().getConfig(wantedClient).getOption(setDomain, actualParams[1]);
            final String globalValue = user.getAccount().getAccountConfig().getOption(setDomain, actualParams[1]);
            // And the type of this param
            final ParamType paramType = pi.getType();
            // Check if user wants to change it
            if (actualParams.length > 2) {
                if (user.isReadOnly()) {
                    output.addBotMessage("Sorry, read-only sub-clients are unable to make changes to settings.");
                    return;
                }

                String newValue;
                // If its a string we get the rest of the line, else just the first word
                if (paramType == ParamType.STRING) {
                    final StringBuilder allInput = new StringBuilder();
                    for (int i = 2; i < actualParams.length; ++i) {
                        allInput.append(actualParams[i]).append(" ");
                    }
                    newValue = allInput.toString().trim();
                } else {
                    newValue = actualParams[2];
                }

                // Now validate and set.
                if (paramType == ParamType.INT || paramType == ParamType.NEGATIVEINT || paramType == ParamType.POSITIVEINT) {
                    try {
                        int newValueInt = Integer.parseInt(newValue);
                        if ((paramType == ParamType.NEGATIVEINT && newValueInt >= 0) || (paramType == ParamType.POSITIVEINT && newValueInt < 0)) {
                            output.addBotMessage("Sorry, '%s' is not a valid value for '%s'", newValue, actualParams[1]);
                            return;
                        } else {
                            user.getAccount().getConfig(wantedClient).setOption(setDomain, actualParams[1], newValueInt);
                        }
                    } catch (NumberFormatException nfe) {
                        output.addBotMessage("Sorry, '%s' is not a valid value for '%s'", newValue, actualParams[1]);
                        return;
                    }
                } else if (paramType == ParamType.FLOAT || paramType == ParamType.NEGATIVEFLOAT || paramType == ParamType.POSITIVEFLOAT) {
                    try {
                        float newValueFloat = Float.parseFloat(newValue);
                        if ((paramType == ParamType.NEGATIVEFLOAT && newValueFloat >= 0) || (paramType == ParamType.POSITIVEFLOAT && newValueFloat < 0)) {
                            output.addBotMessage("Sorry, '%s' is not a valid value for '%s'", newValue, actualParams[1]);
                            return;
                        } else {
                            user.getAccount().getConfig(wantedClient).setOption(setDomain, actualParams[1], newValueFloat);
                        }
                    } catch (NumberFormatException nfe) {
                        output.addBotMessage("Sorry, '%s' is not a valid value for '%s'", newValue, actualParams[1]);
                        return;
                    }
                } else if (paramType == ParamType.BOOL) {
                    if (newValue.equalsIgnoreCase("true") || newValue.equalsIgnoreCase("yes") || newValue.equalsIgnoreCase("on") || newValue.equalsIgnoreCase("1")) {
                        user.getAccount().getConfig(wantedClient).setOption(setDomain, actualParams[1], true);
                        newValue = "True";
                    } else {
                        user.getAccount().getConfig(wantedClient).setOption(setDomain, actualParams[1], false);
                        newValue = "False";
                    }
                } else {
                    user.getAccount().getConfig(wantedClient).setOption(setDomain, actualParams[1], newValue);
                }

                // And let the user know.
                output.addBotMessage("Changed value of '%s' from '%s' to '%s'", actualParams[1].toLowerCase(), currentValue, newValue);
            } else {
                output.addBotMessage("The current value of '%s' is: %s", actualParams[1].toLowerCase(), currentValue);
                if (pi.isSubClient()) {
                    output.addBotMessage("    The account-level value of '%s' is: %s", actualParams[1].toLowerCase(), globalValue);
                }
            }
        } else {
            output.addBotMessage("You need to choose a setting to set the value for.");
            output.addBotMessage("");
            output.addBotMessage("Valid settings are:");
            for (String param : validParams.keySet()) {
                final String description = validParams.get(param).getDescription();
                final String wantedClient = validParams.get(param).isSubClient() ? wantedClientID : null;
                final String value = user.getAccount().getConfig(wantedClient).getOption(setDomain, param);
                if (validParams.get(param).isSubClient()) {
                    final String globalValue = user.getAccount().getAccountConfig().getOption(setDomain, param);
                    output.addBotMessage("  %15s - %s [Current: %s]  [Account-Level: %s]", param, description, value, globalValue);
                } else {
                    output.addBotMessage("  %15s - %s [Current: %s]", param, description, value);
                }
            }
            output.addBotMessage("");
            output.addBotMessage("");
            output.addBotMessage("Syntax: /dfbnc [--global|--subclient <client>] %s <param> [value]", actualParams[0]);
            output.addBotMessage("");
            output.addBotMessage("Ommiting [value] will show you the current value.");
            output.addBotMessage("Passing --global will edit the account-level setting rather than this-subclient where applicable");
            output.addBotMessage("Passing --subclient <client> will edit the value for the <client> sub-client where applicable rather than this-subclient");
        }
    }

    public AbstractSetCommand(final CommandManager manager) {
        super(manager);
    }

    /** Param Types */
    public enum ParamType {
        /** Integer Param */
        INT,
        /** Non-Negative Integer Param */
        POSITIVEINT,
        /** Negative Integer Param */
        NEGATIVEINT,
        /** Float Param */
        FLOAT,
        /** Non-Negative Float Param */
        POSITIVEFLOAT,
        /** Negative Float Param */
        NEGATIVEFLOAT,
        /** Boolean Param */
        BOOL,
        /** Single-Word String Param (ie Nickname) */
        WORD,
        /** Multi-Word String Param (ie Real Name) */
        STRING
    }

    /** ParamInfo */
    public class ParamInfo {
        /** Parameter Description */
        private String description;
        /** Parameter Type */
        private ParamType type;
        /** Global or SubClient-Specific */
        private boolean subClient;

        /**
         * Create new ParamInfo
         *
         * @param description Description of this Parameter
         * @param type Type of this Parameter
         */
        public ParamInfo(final String description, final ParamType type, final boolean subClient) {
            this.description = description;
            this.type = type;
            this.subClient = subClient;
        }

        /**
         * Get the Description of this parameter
         *
         * @return Description of this param
         */
        public String getDescription() {
            return description;
        }

        /**
         * Get the Type of this parameter
         *
         * @return Type of this param
         */
        public ParamType getType() {
            return type;
        }

        /**
         * Is this a sub-client specific setting?
         *
         * @return Is this a sub-client specific setting?
         */
        public boolean isSubClient() {
            return subClient;
        }
    }

}

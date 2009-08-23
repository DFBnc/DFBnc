/*
 * Copyright (c) 2006-2007 Shane Mc Cormack
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
 *
 * SVN: $Id$
 */
package uk.org.dataforce.dfbnc.servers.irc;

import uk.org.dataforce.dfbnc.commands.Command;
import uk.org.dataforce.dfbnc.commands.CommandManager;
import uk.org.dataforce.dfbnc.UserSocket;

import java.util.HashMap;
import java.util.Map;

/**
 * This file represents the 'IRCSet' command
 */
public class IRCSetCommand extends Command {
    /** Param Types */
    private enum ParamType {
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
        STRING;
    }
    
    /** ParamInfo */
    private class ParamInfo {
        /** Parameter Description */
        private String description;
        /** Parameter Type */
        private ParamType type;
        
        /**
         * Create new ParamInfo
         *
         * @param description Description of this Parameter
         * @param type Type of this Parameter
         */
        public ParamInfo(final String description, final ParamType type) {
            this.description = description;
            this.type = type;
        }
        
        /**
         * Get the Description of this parameter
         *
         * @return Description of this param
         */
        public String getDescription() { return description; } 
                
        /**
         * Get the Type of this parameter
         *
         * @return Type of this param
         */
        public ParamType getType() { return type; } 
    }
    
    /** Valid Parameters. */
    private Map<String, ParamInfo> validParams = new HashMap<String, ParamInfo>();

    /**
     * Handle an IRCSet command.
     *
     * @param user the UserSocket that performed this command
     * @param params Params for command (param 0 is the command name)
     */
    @Override
    public void handle(final UserSocket user, final String[] params) {
        user.sendBotMessage("----------------");
        
        if (params.length > 1 && validParams.containsKey(params[1].toLowerCase())) {
            // Get the current value
            final String currentValue = user.getAccount().getProperties().getProperty("irc."+params[1], "");
            // And the type of this param
            final ParamType paramType = validParams.get(params[1].toLowerCase()).getType();
            // Check if user wants to change it
            if (params.length > 2) {
                String newValue;
                // If its a string we get the rest of the line, else just the first word
                if (paramType == ParamType.STRING) {
                    final StringBuilder allInput = new StringBuilder();
                    for (int i = 2 ; i < params.length; ++i) { allInput.append(params[i]+" "); }
                    newValue = allInput.toString().trim();
                } else {
                    newValue = params[2];
                }
                
                // Now validate and set.
                if (paramType == ParamType.INT || paramType == ParamType.NEGATIVEINT || paramType == ParamType.POSITIVEINT) {
                    try {
                        int newValueInt = Integer.parseInt(newValue);
                        if ((paramType == ParamType.NEGATIVEINT && newValueInt >= 0) || (paramType == ParamType.POSITIVEINT && newValueInt < 0)) {
                            user.sendBotMessage("Sorry, '"+newValue+"' is not a valid value for '"+params[1]+"'");
                            return;
                        } else {
                            user.getAccount().getProperties().setIntProperty("irc."+params[1], newValueInt);
                        }
                    } catch (NumberFormatException nfe) {
                        user.sendBotMessage("Sorry, '"+newValue+"' is not a valid value for '"+params[1]+"'");
                        return;
                    }
                } else if (paramType == ParamType.FLOAT || paramType == ParamType.NEGATIVEFLOAT || paramType == ParamType.POSITIVEFLOAT) {
                    try {
                        float newValueFloat = Float.parseFloat(newValue);
                        if ((paramType == ParamType.NEGATIVEFLOAT && newValueFloat >= 0) || (paramType == ParamType.POSITIVEFLOAT && newValueFloat < 0)) {
                            user.sendBotMessage("Sorry, '"+newValue+"' is not a valid value for '"+params[1]+"'");
                            return;
                        } else {
                            user.getAccount().getProperties().setFloatProperty("irc."+params[1], newValueFloat);
                        }
                    } catch (NumberFormatException nfe) {
                        user.sendBotMessage("Sorry, '"+newValue+"' is not a valid value for '"+params[1]+"'");
                        return;
                    }
                } else if (paramType == ParamType.BOOL) {
                    if (newValue.equalsIgnoreCase("true") || newValue.equalsIgnoreCase("yes") || newValue.equalsIgnoreCase("on") || newValue.equalsIgnoreCase("1")) {
                        user.getAccount().getProperties().setBoolProperty("irc."+params[1], true);
                        newValue = "True";
                    } else {
                        user.getAccount().getProperties().setBoolProperty("irc."+params[1], false);
                        newValue = "False";
                    }
                } else {
                    user.getAccount().getProperties().setProperty("irc."+params[1], newValue);
                }
                
                // And let the user know.
                user.sendBotMessage("Changed value of '"+params[1].toLowerCase()+"' from '"+currentValue+"' to '"+newValue+"'");
            } else {
                user.sendBotMessage("The current value of '"+params[1].toLowerCase()+"' is: "+currentValue);
            }
        } else {
            user.sendBotMessage("You need to choose a valid setting to set the value for.");
            user.sendBotMessage("Valid settings are:");
            for (String param : validParams.keySet()) {
                String description = validParams.get(param).getDescription();
                String value = user.getAccount().getProperties().getProperty("irc."+param, "");
                user.sendBotMessage(String.format("  %15s - %s [Current: %s]", param, description, value));
            }
            user.sendBotMessage("Syntax: /dfbnc "+params[0]+" <param> [value]");
            user.sendBotMessage("Ommiting [value] will show you the current value.");
        }
    }
    
    /**
     * What does this Command handle.
     *
     * @return String[] with the names of the tokens we handle.
     */
    @Override
    public String[] handles() {
        return new String[]{"ircset", "is"};
    }
    
    /**
     * Create a new instance of the Command Object
     *
     * @param manager CommandManager that is in charge of this Command
     */
    public IRCSetCommand (final CommandManager manager) {
        super(manager);
        
        // Add the valid params
        validParams.put("nickname", new ParamInfo("Nickname to use on IRC", ParamType.WORD));
        validParams.put("altnickname", new ParamInfo("Alternative nickname to use if nickname is taken", ParamType.WORD));
        validParams.put("realname", new ParamInfo("Realname to use on IRC", ParamType.STRING));
        validParams.put("username", new ParamInfo("Username to use on IRC", ParamType.WORD));
        
        validParams.put("bindip", new ParamInfo("IP Address to bind to for new connections", ParamType.WORD));
    }
    
    /**
     * Get a description of what this command does
     *
     * @param command The command to describe (incase one Command does multiple
     *                things under different names)
     * @return A description of what this command does
     */
    @Override
    public String getDescription(final String command) {
        return "This command lets you manipulate irc settings";
    }
    
    /**
     * Get SVN information.
     *
     * @return SVN Info String
     */
    public static String getSvnInfo () { return "$Id: Process001.java 1508 2007-06-11 20:08:12Z ShaneMcC $"; }    
}
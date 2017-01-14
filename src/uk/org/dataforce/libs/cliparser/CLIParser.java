/*
 * Copyright (c) 2006-2017 Shane Mc Cormack
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

package uk.org.dataforce.libs.cliparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.org.dataforce.libs.logger.Logger;

/**
 * Command Line argument parser.
 */
public class CLIParser {
    /** Singleton instance of CLIParser. */
    private static CLIParser me;

    /** What parameter is used for help? */
    private CLIParam helpParam = null;

    /**
     * Known arguments.
     * This hashmap stores the arguments with their flags as the key.
     */
    private Map<String, CLIParam> params = new HashMap<>();

    /**
     * Known arguments.
     * This ArrayList stores every param type. (used for help)
     */
    private List<CLIParam> paramList = new ArrayList<>();

    /**
     * Redundant Strings.
     * This ArrayList stores redundant strings found whilst parsing the params.
     */
    private List<String> redundant = new ArrayList<>();

    /**
     * Last set of arguments parsed.
     */
    private String[] lastArgs = null;

    /**
     * Get a reference to the CLIParser.
     *
     * @return The reference to the CLIParser in use
     */
    public static synchronized CLIParser getCLIParser() {
        if (me == null) { me = new CLIParser(); }
        return me;
    }

    /** Private constructor for CLIParser to prevent non-singleton instance. */
    private CLIParser() { }

    /** Clear known params from the hashtable. */
    public void clear() {
        params.clear();
        paramList.clear();
        redundant.clear();
    }

    /**
     * Add a CLIParam to the cliparser.
     *
     * @param param CLIParam sub-class to use as a parameter.
     * @return true if added, false if already exists.
     */
    public boolean add(final CLIParam param) {
        final boolean validChar = (param.getChr() == 0 || !params.containsKey(""+param.getChr()));
        final boolean validString = (param.getString().length() == 0 || !params.containsKey("-"+param.getString()));
        if (validChar && validString) {
            if (param.getChr() != 0) {
                params.put(""+param.getChr(), param);
                Logger.debug2("Added Param: [-"+param.getChr()+"]");
            }
            if (param.getString().length() > 0) {
                params.put("-"+param.getString().toLowerCase(), param);
                Logger.debug2("Added Param: [--"+param.getString()+"]");
            }
            paramList.add(param);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the number of times a param was given.
     * In the case of params with both a char and string value, this number is
     * the total for both.
     *
     * @param flag Flag to get count for
     * @return number, or -1 if the param is invalud
     */
    public int getParamNumber(final String flag) {
        final String check = (flag.length() > 1) ? flag.toLowerCase() : flag;
        if (params.containsKey(check)) {
            return params.get(check).getNumber();
        } else {
            return -1;
        }
    }

    /**
     * Check if the given param was given on the command line.
     * In the case of params with both a char and string value, this will check
     * either.
     *
     * @param flag Flag to check
     * @return True if given, false if not given or invalid.
     */
    public boolean paramGiven(final String flag) {
        return getParamNumber(flag) > 0;
    }

    /**
     * Get a CLIParam object for a given flag.
     *
     * @param flag Flag to get param for
     * @return CLIParam object, or null if there is none.
     */
    public CLIParam getParam(final String flag) {
        final String check = (flag.length() > 1) ? flag.toLowerCase() : flag;
        if (params.containsKey(check)) {
            return params.get(check);
        } else {
            return null;
        }
    }

    /**
     * Get the list of params.
     *
     * @return list of params.
     */
    public List<CLIParam> getParamList() {
        return paramList;
    }

    /**
     * Get the list of redundant strings.
     *
     * @return list of redundant strings.
     */
    public List<String> getRedundant() {
        final List<String> result = new ArrayList<>();
        for (String item : redundant) {
            result.add(item);
        }
        return result;
    }

    /**
     * Set the "help" command.
     *
     * @param param Param to look for in wantsHelp.
     */
    public void setHelp(final CLIParam param) {
        helpParam = param;
    }

    /**
     * Check if the help parameter has been passed to the CLI.
     *
     * @param args Arguments passed to CLI
     * @return True if the designated help parameter has been requested
     */
    public boolean wantsHelp(String[] args) {
        if (helpParam == null) { return false; }
        for (final String arg : args) {
            if (arg.length() > 1 && arg.charAt(0) == '-') {
                final String name = arg.substring(1);
                if (name.equals("-")) {
                    return false;
                } else {
                    final CLIParam param = getParam(name);
                    if (param == helpParam) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Show the help
     *
     * @param title Title of application
     * @param usage CLI Usage String
     */
    public void showHelp(final String title, final String usage) {
        System.out.println(title);
        System.out.println("------------------");
        System.out.println(usage);
        System.out.println(" ");
        for (CLIParam param : this.getParamList()) {
            if (param.getChr() != 0) {
                System.out.print("-"+param.getChr()+" ");
            } else {
                System.out.print("   ");
            }
            if (param.getString().length() > 0) {
                System.out.print("--"+param.getString()+" ");
            } else {
                System.out.print("\t\t");
            }
            System.out.println("\t"+param.getDescription());
        }
    }

    /**
     * Given a string array of arguments, parse as CLI Params.
     *
     * @param args Arguments to pass
     * @param strict if True, will terminate if a given param is invalid.
     */
    public void parseArgs(final String[] args, final boolean strict) {
        lastArgs = args;
        CLIParam lastParam = null;
        boolean allRedundant = false;
        for (String arg : args) {
            if (arg.length() > 1 && arg.charAt(0) == '-' && !allRedundant) {
                if (lastParam != null) { lastParam.setValue(""); }
                String fullparam = arg.substring(1);
                if (fullparam.equals("-")) {
                    allRedundant = true;
                } else {
                    final ArrayList<String> givenParams = new ArrayList<>();
                    if (arg.charAt(1) == '-') {
                        givenParams.add(fullparam);
                    } else {
                        for (int i = 1; i < arg.length(); i++) {
                            givenParams.add("" + arg.charAt(i));
                        }
                    }
                    for (String name : givenParams) {
                        lastParam = getParam(name);
                        if (lastParam != null) {
                            Logger.debug("Got Param: -"+name);
                            lastParam.incNumber();
                        } else {
                            Logger.warning("Unknown Param: -"+name);
                            if (helpParam != null) {
                                String command = "";
                                if (helpParam.getString().length() > 0) {
                                    command = helpParam.getString();
                                } else if (helpParam.getChr() != 0) {
                                    command = ""+helpParam.getChr();
                                }
                                if (command.length() > 0) {
                                    Logger.warning("Use "+command+" to get help.");
                                }
                            }
                            if (strict) {
                                System.exit(1);
                            }
                        }
                    }
                }
            } else {
                if (arg.charAt(0) == '\\' && arg.length() > 1) { arg = arg.substring(1); }
                if (lastParam != null && !allRedundant && lastParam.setValue(arg)) {
                    Logger.debug2("Param Value: "+arg);
                    lastParam = null;
                } else {
                    Logger.debug2("Redundant Value: "+arg);
                    redundant.add(arg);
                }
            }
        }
    }

    /**
     * Get the last args that were parsed.
     *
     * @return Last args that were parsed.
     */
    public String[] getLastArgs() {
        return lastArgs;
    }
}

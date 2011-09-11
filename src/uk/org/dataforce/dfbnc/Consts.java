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
 */
package uk.org.dataforce.dfbnc;

/**
 * This file stores various constants for the irc protocol
 */
public final class Consts {
    // Standard Replies
    public final static int RPL_WELCOME = 1;
    public final static int RPL_YOURHOST = 2;
    public final static int RPL_CREATED = 3;
    public final static int RPL_MYINFO = 4;
    public final static int RPL_ISUPPORT = 5;

    // Errors
    public final static int ERR_UNKNOWNCOMMAND = 421;
    public final static int ERR_NOTREGISTERED = 451;
    public final static int ERR_NEEDMOREPARAMS = 461;
    public final static int ERR_PASSWDMISMATCH = 464;
    
    
    // Whois
    public final static int RPL_WHOISUSER = 311;
    public final static int RPL_WHOISSERVER = 312;
    public final static int RPL_WHOISIDLE = 317;
    public final static int RPL_ENDOFWHOIS = 318;

    /**
     * Prevent Creation of Consts object
     */
    private Consts() {    }
}
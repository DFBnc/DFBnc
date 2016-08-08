/*
 * Copyright (c) 2006-2016 DFBnc Developers
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
package com.dfbnc.sockets.secure;

import java.security.Principal;
import java.security.cert.Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

/**
 * Event related to a completed ssl handshake.
 *
 * Based on javax.net.ssl.HandshakeCompletedEvent;
 */
public class HandshakeCompletedEvent {
    private final SSLByteChannel myByteChannel;
    private final SSLSession mySession;

    /**
     * Create a new HandshakeCompletedEvent.
     *
     * @param byteChannel The SSLByteChannel that completed the handshake.
     */
    public HandshakeCompletedEvent(final SSLByteChannel byteChannel) {
        myByteChannel = byteChannel;
        mySession = byteChannel.getSSLEngine().getSession();
    }

    /**
     * Returns the cipher suite in use by the session which was produced by the handshake.
     *
     * @return the name of the cipher suite negotiated during this session.
     */
    public String getCipherSuite() {
        return mySession.getCipherSuite();
    }

    /**
     * Returns the certificate(s) that were sent to the peer during handshaking.
     *
     * @return an ordered array of certificates, with the local certificate
     *         first followed by any certificate authorities. If no
     *         certificates were sent, then null is returned.
     */
    public Certificate[] getLocalCertificates() {
        return mySession.getLocalCertificates();
    }

    /**
     * Returns the principal that was sent to the peer during handshaking.
     *
     * @return the principal sent to the peer. Returns an X500Principal of the
     *         end-entity certificate for X509-based cipher suites, and
     *         KerberosPrincipal for Kerberos cipher suites. If no principal
     *         was sent, then null is returned.
     */
    public Principal getLocalPrincipal() {
        return mySession.getLocalPrincipal();
    }

    /**
     * Returns the identity of the peer which was identified as part of defining the session.
     *
     * Note: this method exists for compatibility with previous releases.
     *       New applications should use getPeerCertificates() instead.
     *
     * @return an ordered array of peer X.509 certificates, with the peer's own
     *         certificate first followed by any certificate authorities.
     *         (The certificates are in the original JSSE X509Certificate format).
     * @throws SSLPeerUnverifiedException - if the peer is not verified.
     */
    public X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        return mySession.getPeerCertificateChain();
    }

    /**
     * Returns the identity of the peer which was established as part of defining the session.
     *
     * @return an ordered array of the peer certificates, with the peer's own
     *         certificate first followed by any certificate authorities.
     * @throws SSLPeerUnverifiedException if the peer is not verified.
     */
    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        return mySession.getPeerCertificates();
    }

    /**
     * Returns the identity of the peer which was established as part of defining the session.
     *
     * @return the peer's principal. Returns an X500Principal of the end-entity
     *         certificate for X509-based cipher suites, and KerberosPrincipal
     *         for Kerberos cipher suites.
     * @throws SSLPeerUnverifiedException if the peer's identity has not been verified
     */
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return mySession.getPeerPrincipal();
    }

    /**
     * Returns the session that triggered this event.
     *
     * @return the SSLSession for this handshake.
     */
    public SSLSession getSession() {
        return mySession;
    }

    /**
     * Returns the SSLByteChannel which is the source of this event.
     *
     * @return Returns the ByteChannel which is the source of this event.
     */
    public SSLByteChannel getSSLByteChannel() {
        return myByteChannel;
    }
}

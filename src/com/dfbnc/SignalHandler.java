/*
 *  Copyright 2017 Shane Mc Cormack <shanemcc@gmail.com>.
 *  See LICENSE.txt for licensing details.
 */
package com.dfbnc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import uk.org.dataforce.libs.logger.Logger;

/**
 * Handle signals sent to BNC where possible.
 * This relies on sun.misc.Signal and sun.misc.SignalHandler which may not be
 * in every JVM, so we use reflection to use them where possible.
 *
 * @author Shane Mc Cormack <shanemcc@gmail.com>
 */
public class SignalHandler {
    /** The DFBnc instance that this SignalHandler is for. */
    private final DFBnc myBnc;

    /** Ignore signals if we are deactivated. */
    private boolean inactive;

    /**
     * Create the SignalHandler
     *
     * @param bnc DFBnc instance
     */
    public SignalHandler(final DFBnc bnc) {
        myBnc = bnc;
        inactive = false;

        try {
            activate();
        } catch (final Exception ex) {
            Logger.error("Unable to listen for signals: " + ex.getMessage());
        }
    }

    public void activate() throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final Class<?> handlerCl = Class.forName("sun.misc.SignalHandler");
        final Class<?> signalCl = Class.forName("sun.misc.Signal");

        final Constructor signalCtor = signalCl.getConstructor(String.class);
        final Method signalHandle = signalCl.getMethod("handle", signalCl, handlerCl);

        // Create a proxy class that implements SignalHandler
        final Class<?> proxyClass = Proxy.getProxyClass(signalCl.getClassLoader(), handlerCl);

        // This is used by the instance of proxyClass to dispatch method calls
        final InvocationHandler invHandler = (final Object proxy, final Method method, final Object[] args) -> {
            // proxy is the SignalHandler's "this" rederence
            // method will be the handle(Signal) method
            // args[0] will be an instance of Signal
            // If you're using this object for multiple signals, you'll
            // you'll need to use the "getName" method to determine which
            // signal you have caught.

            try {
                final Method signalGetName = signalCl.getMethod("getName");
                final String name = (String)signalGetName.invoke(args[0]);

                myBnc.signal(name);
            } catch (final Throwable t) {
                /** Do nothing. */
            }

            return null;
        };

        // Get the constructor and create an instance of proxyClass
        final Constructor<?> proxyCtor = proxyClass.getConstructor(InvocationHandler.class);
        final Object handler = proxyCtor.newInstance(invHandler);

        // Create the signal and call Signal.handle to bind handler to signal
        signalHandle.invoke(null, signalCtor.newInstance("HUP"), handler);
    }

    /**
     * Inactivates this handler.
     */
    public void inactivate() {
        inactive = true;
    }
}
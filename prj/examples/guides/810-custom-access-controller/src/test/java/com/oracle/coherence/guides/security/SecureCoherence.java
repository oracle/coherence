/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.config.Config;
import com.tangosol.net.Coherence;

import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.security.Security;

import javax.security.auth.Subject;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * A simple class that runs {@link Coherence} with a
 * {@link Subject} created from a JAAS login.
 *
 * @author Jonathan Knight 2025.04.11
 */
@SuppressWarnings("resource")
public class SecureCoherence
    {
    /**
     * Perform a JAAS login and run Coherence within the context
     * of the logged in {@link Subject}.
     *
     * @param args  the program arguments
     */
    public static void main(String[] args)
        {
        Subject subject = Security.login(new TestCallBackHandler());
        Subject.doAs(subject, (PrivilegedAction<Void>) () ->
            {
            try
                {
                String         sClient = Config.getProperty("coherence.client");
                Coherence.Mode mode    = null;

                try
                    {
                    mode = Coherence.Mode.fromClientName(sClient);
                    }
                catch (IllegalArgumentException e)
                    {
                    // ignored
                    }

                Coherence coherence;
                if (mode == null)
                    {
                    coherence = Coherence.clusterMember();
                    }
                else
                    {
                    coherence = Coherence.builder(CoherenceConfiguration.create(), mode).build();
                    }

                coherence.start().join();
                coherence.whenClosed().join();
                }
            catch (Throwable t)
                {
                Logger.err("Caught exception in SecureCoherence server", t);
                s_thrown = Exceptions.getRootCause(t);
                }

            // block so that the process does not end if an exception is thrown
            synchronized (s_waiter)
                {
                try
                    {
                    s_waiter.wait();
                    }
                catch (InterruptedException ignored)
                    {
                    }
                }
            return null;
            });
        }

    /**
     * A utility method to start {@link Coherence}.
     *
     * @throws Exception if Coherence fails to start
     */
    public static void startAndWait() throws Exception
        {
        Subject subject = Security.login(new TestCallBackHandler());
        Subject.doAs(subject, (PrivilegedExceptionAction<Coherence>) () ->
                Coherence.clusterMember().startAndWait());
        }

    /**
     * Return any exception thrown during Coherence start up.
     *
     * @return any exception thrown during Coherence start up
     */
    public static Throwable getThrown()
        {
        return s_thrown;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A simple object to use to wait on indefinitely.
     */
    private static final Object s_waiter = new Object();

    /**
     * Any exception thrown during Coherence startup
     */
    static Throwable s_thrown;
    }

/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.config.Config;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;

/**
 * A wrapper around Coherence where we expect start-up to fail
 */
@SuppressWarnings("resource")
public class DisallowedCoherence
    {
    public static void main(String[] args) throws Exception
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
            Logger.err("Caught exception in DisallowedCoherence.main()", t);
            s_thrown = Exceptions.getRootCause(t);
            }
        synchronized (s_waiter)
            {
            s_waiter.wait();
            }
        }

    public static HasThrown hasThrown()
        {
        return new HasThrown();
        }

    // ----- inner class: HasThrown -----------------------------------------

    public static class HasThrown
            implements RemoteCallable<Boolean>
        {
        public HasThrown()
            {
            }

        public Boolean call()
            {
            Logger.err("Invoking GetThrown, thrown=" + s_thrown);
            return s_thrown instanceof SecurityException || s_thrown instanceof InterruptedException;
            }
        }

    // ----- data members ---------------------------------------------------

    private static final Object s_waiter = new Object();

    private static Throwable s_thrown;
    }

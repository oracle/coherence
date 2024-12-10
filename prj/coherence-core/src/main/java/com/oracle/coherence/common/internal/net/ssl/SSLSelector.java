/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.ssl;

import com.oracle.coherence.common.internal.net.WrapperSelector;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;


/**
 * SSLSelector implementation.
 *
 * @author mf
 */
public class SSLSelector
    extends WrapperSelector
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SSLSelector
    *
    * @param selector  the selector to wrap
    * @param provider  the SSLSocketProvider
    *
    * @throws java.io.IOException  if an I/O error occurs
    */
    public SSLSelector(Selector selector, SelectorProvider provider)
            throws IOException
        {
        super(selector, provider);
        }


    // ----- SSLSelector interface ------------------------------------------

    /**
     * Update the specified SelectionKey's interest ops.
     *
     * @param key   the key to update
     * @param nOps  the ops of interest
     */
    public void setInterestOps(SelectionKey key, int nOps)
        {
        synchronized (m_oInterestLock)
            {
            wakeup(); // COH-7389, ensure that we don't block indefinitely if we're in select

            key.interestOps(nOps);
            }
        }

    // ----- Selector interface ---------------------------------------------


    @Override
    public int select(long timeout)
            throws IOException
        {
         // COH-7389, ensure that we don't other threads block indefinitely if we're in select
        synchronized (m_oInterestLock) {}

        return super.select(timeout);
        }


    // ----- Object interface -----------------------------------------------

    public String toString()
        {
        return "SSLSelector(" + m_delegate + ")";
        }


    // ----- data members ----------------------------------------------------


    /**
     * Monitor used to prevent concurrent calls to Selector.select() and SelectionKey.interestOps, which
     * blocks indefinitely on IBM's JDK.
     */
    protected final Object m_oInterestLock = new Object();
    }

/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.application;


import com.tangosol.net.Service;

import com.tangosol.util.MapListener;


/**
 * Helper methods for Container aware logic.
 *
 * @author gg  2005.10.24
 * @since Coherence 12.2.1
 */
public class ContainerHelper
    {
    /**
     * Initialize the thread context for the given Service.
     *
     * @param service a Service instance
     */
    public static void initializeThreadContext(Service service)
        {
        }

    /**
     * Wrap the specified MapListener in such a way that the returned listener
     * would dispatch events using the caller's context rather than context
     * associated with the specified Service.
     *
     * @param service  a Service instance
     * @param listener the listener to wrap
     *
     * @return the corresponding context aware listener
     */
    public static MapListener getContextAwareListener(Service service, MapListener listener)
        {
        return listener;
        }
    }

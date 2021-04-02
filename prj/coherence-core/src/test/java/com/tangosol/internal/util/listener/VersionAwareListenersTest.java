/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.listener;

import com.tangosol.util.MapListener;
import com.tangosol.util.listener.SimpleMapListener;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for VersionAwareListeners
 *
 * @author hr  2021.02.10
 */
public class VersionAwareListenersTest
    {
    @Test
    public void testEquality()
        {
        MapListener<Integer, String> listener = new SimpleMapListener<Integer, String>().versioned();
        MapListener<Integer, String> listenerVersioned = VersionAwareListeners.createListener(listener);

        assertEquals(listener, listenerVersioned);
        assertEquals(listenerVersioned, listener);

        assertEquals(listener.hashCode(), listenerVersioned.hashCode());
        assertEquals(listenerVersioned.hashCode(), listener.hashCode());
        }
    }
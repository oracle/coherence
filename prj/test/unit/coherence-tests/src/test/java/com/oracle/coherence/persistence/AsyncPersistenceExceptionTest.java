/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for AsyncPersistenceException.
 *
 * @author jh  2013.07.17
 */
public class AsyncPersistenceExceptionTest
    {

    // ----- test methods ---------------------------------------------------

    @Test
    public void testInitReceipt()
        {
        final AsyncPersistenceException e = new AsyncPersistenceException("test");
        assertNull(e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        assertNull(e.getReceipt());

        AsyncPersistenceException ee;
        ee = e.initReceipt(NullPersistentStore.INSTANCE);
        assertTrue(ee == e);
        assertNull(e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        assertTrue(e.getReceipt() == NullPersistentStore.INSTANCE);

        ee = e.initReceipt(NullPersistenceEnvironment.INSTANCE);
        assertTrue(ee == e);
        assertNull(e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        assertTrue(e.getReceipt() == NullPersistentStore.INSTANCE);
        }
    }

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
 * Unit test for PersistenceException.
 *
 * @author jh  2013.07.17
 */
public class PersistenceExceptionTest
    {

    // ----- test methods ---------------------------------------------------

    @Test
    public void testInitPersistenceEnvironment()
        {
        final PersistenceException e = new PersistenceException("test");
        assertNull(e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());

        PersistenceException ee;
        ee = e.initPersistenceEnvironment(NullPersistenceEnvironment.INSTANCE);
        assertTrue(ee == e);
        assertTrue(e.getPersistenceEnvironment() == NullPersistenceEnvironment.INSTANCE);
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());

        ee = e.initPersistenceEnvironment(null);
        assertTrue(ee == e);
        assertTrue(e.getPersistenceEnvironment() == NullPersistenceEnvironment.INSTANCE);
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        }

    @Test
    public void testInitPersistenceManager()
        {
        final PersistenceException e = new PersistenceException("test");
        assertNull(e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());

        PersistenceException ee;
        ee = e.initPersistenceManager(NullPersistenceManager.INSTANCE);
        assertTrue(ee == e);
        assertNull(e.getPersistenceEnvironment());
        assertTrue(e.getPersistenceManager() == NullPersistenceManager.INSTANCE);
        assertNull(e.getPersistentStore());

        ee = e.initPersistenceManager(null);
        assertTrue(ee == e);
        assertNull(e.getPersistenceEnvironment());
        assertTrue(e.getPersistenceManager() == NullPersistenceManager.INSTANCE);
        assertNull(e.getPersistentStore());
        }

    @Test
    public void testInitPersistenceStore()
        {
        final PersistenceException e = new PersistenceException("test");
        assertNull(e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());

        PersistenceException ee;
        ee = e.initPersistentStore(NullPersistentStore.INSTANCE);
        assertTrue(ee == e);
        assertNull(e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertTrue(e.getPersistentStore() == NullPersistentStore.INSTANCE);

        ee = e.initPersistentStore(null);
        assertTrue(ee == e);
        assertNull(e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertTrue(e.getPersistentStore() == NullPersistentStore.INSTANCE);
        }
    }

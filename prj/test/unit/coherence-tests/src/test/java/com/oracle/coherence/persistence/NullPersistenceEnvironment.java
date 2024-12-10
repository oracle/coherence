/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

/**
 * NO-OP implementation of PersistenceEnvironment.
 *
 * @author jh  2013.12.04
 */
public class NullPersistenceEnvironment
        implements PersistenceEnvironment
    {
    @Override
    public PersistenceManager openActive()
        {
        return null;
        }

    @Override
    public PersistenceManager openSnapshot(String sSnapshot)
        {
        return null;
        }

    @Override
    public PersistenceManager createSnapshot(String sSnapshot, PersistenceManager manager)
        {
        return null;
        }

    @Override
    public boolean removeSnapshot(String sSnapshot)
        {
        return false;
        }

    @Override
    public String[] listSnapshots()
        {
        return new String[0];
        }

    @Override
    public void release()
        {
        }

    /**
     * Singleton instance.
     */
    public static final NullPersistenceEnvironment INSTANCE = new NullPersistenceEnvironment();
    }

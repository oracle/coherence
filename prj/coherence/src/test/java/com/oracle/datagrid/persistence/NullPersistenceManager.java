/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

import com.oracle.coherence.common.base.Collector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * NO-OP implementation of PersistenceManager.
 *
 * @author jh  2013.12.04
 */
public class NullPersistenceManager
        implements PersistenceManager, PersistenceTools
    {
    @Override
    public String getName()
        {
        return null;
        }

    @Override
    public PersistentStore createStore(String sId)
        {
        return null;
        }

    @Override
    public PersistentStore open(String sId, PersistentStore store)
        {
        return null;
        }

    @Override
    public PersistentStore open(String sId, PersistentStore store, Collector collector)
        {
        return null;
        }

    @Override
    public void close(String sId)
        {
        }

    @Override
    public boolean delete(String sId, boolean fSafe)
        {
        return false;
        }

    @Override
    public String[] list()
        {
        return new String[0];
        }

    @Override
    public String[] listOpen()
        {
        return new String[0];
        }

    @Override
    public void read(String sId, InputStream in)
            throws IOException
        {
        }

    @Override
    public void write(String sId, OutputStream out)
            throws IOException
        {
        }

    @Override
    public void release()
        {
        }

    @Override
    public PersistenceTools getPersistenceTools()
        {
        return this;
        }

    // ----- PersistenceTools interface ---------------------------------

    @Override
    public OfflinePersistenceInfo getPersistenceInfo()
        {
        return null;
        }

    @Override
    public void validate()
        {
        }

    @Override
    public PersistenceStatistics getStatistics()
        {
        return null;
        }

    /**
     * Singleton instance.
     */
    public static final NullPersistenceManager INSTANCE = new NullPersistenceManager();
    }
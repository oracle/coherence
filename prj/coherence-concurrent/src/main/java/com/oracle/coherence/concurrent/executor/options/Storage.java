/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.options;

import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.DistributedCacheService;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.concurrent.Executor;

/**
 * An {@link TaskExecutorService.Registration.Option} to specify whether an {@link Executor} is running on a storage
 * enabled Coherence server.
 * <p>
 * See {@link DistributedCacheService#isLocalStorageEnabled()}
 *
 * @author phf
 * @since 21.12
 */
public class Storage
        implements TaskExecutorService.Registration.Option, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Storage} (required for serialization).
     */
    public Storage()
        {
        }

    /**
     * Constructs a {@link Storage}.
     *
     * @param fStorageEnabled  whether the Coherence server in which the {@link Executor}
     *                         is running is storage enabled.
     */
    protected Storage(boolean fStorageEnabled)
        {
        m_fStorageEnabled = fStorageEnabled;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return whether the Coherence server in which the {@link Executor} is running is storage enabled.
     *
     * @return whether the Coherence server in which the {@link Executor} is running is storage enabled
     */
    public boolean isEnabled()
        {
        return m_fStorageEnabled;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public int hashCode()
        {
        return Boolean.valueOf(m_fStorageEnabled).hashCode();
        }

    @Override
    public boolean equals(Object obj)
        {
        if (obj == this)
            {
            return true;
            }

        if (obj instanceof Storage)
            {
            return m_fStorageEnabled == ((Storage) obj).m_fStorageEnabled;
            }
        else
            {
            return false;
            }
        }

    @Override
    public String toString()
        {
        return "Storage{" + m_fStorageEnabled + '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_fStorageEnabled = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeBoolean(m_fStorageEnabled);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_fStorageEnabled = in.readBoolean(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBoolean(0, m_fStorageEnabled);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a {@link Storage} {@link TaskExecutorService.Registration.Option}.
     *
     * @param fStorageEnabled  whether the Option represents enabled (true), or disabled (false)
     *
     * @return a {@link Storage} {@link TaskExecutorService.Registration.Option}
     */
    public static Storage enabled(boolean fStorageEnabled)
        {
        return fStorageEnabled ? ENABLED : DISABLED;
        }

    /**
     * Return a {@link Storage} enabled {@link TaskExecutorService.Registration.Option}.
     *
     * @return a {@link Storage} {@link TaskExecutorService.Registration.Option}
     */
    public static Storage enabled()
        {
        return ENABLED;
        }

    /**
     * Return a {@link Storage} disabled {@link TaskExecutorService.Registration.Option}.
     *
     * @return a {@link Storage} {@link TaskExecutorService.Registration.Option}
     */
    public static Storage disabled()
        {
        return DISABLED;
        }

   // ----- constants ------------------------------------------------------

    /**
     * Read-only {@link Storage} set to {@code enabled}.
     */
    protected final static Storage ENABLED = new Storage(true);

    /**
     * Read-only {@link Storage} set to {@code disabled}.
     */
    protected final static Storage DISABLED = new Storage(false);

   // ----- data members ---------------------------------------------------

    /**
     * Whether the Coherence server in which the {@link Executor} is running
     * is storage {@code enabled}.
     */
    protected boolean m_fStorageEnabled;
    }

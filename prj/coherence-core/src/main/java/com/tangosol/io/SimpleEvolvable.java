/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import com.tangosol.io.Evolvable;

import com.tangosol.util.Binary;

/**
 * Simple implementation of {@link Evolvable} interface.
 *
 * @author as  2013.04.19
 * @since  12.2.1
 */
public class SimpleEvolvable
        implements Evolvable
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create SimpleEvolvable instance.
     *
     * @param nImplVersion  the implementation version of the class this
     *                      Evolvable is for
     */
    public SimpleEvolvable(int nImplVersion)
        {
        m_nImplVersion = nImplVersion;
        }

    // ---- Evolvable implementation ----------------------------------------

    /**
     * {@inheritDoc}
     */
    public int getImplVersion()
        {
        return m_nImplVersion;
        }

    /**
     * {@inheritDoc}
     */
    public int getDataVersion()
        {
        return m_nDataVersion;
        }

    /**
     * {@inheritDoc}
     */
    public void setDataVersion(int nDataVersion)
        {
        this.m_nDataVersion = nDataVersion;
        }

    /**
     * {@inheritDoc}
     */
    public Binary getFutureData()
        {
        return m_binFutureData;
        }

    /**
     * {@inheritDoc}
     */
    public void setFutureData(Binary binFutureData)
        {
        this.m_binFutureData = binFutureData;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Implementation version.
     */
    protected int m_nImplVersion;

    /**
     * Data version.
     */
    protected int m_nDataVersion;

    /**
     * Future data.
     */
    protected Binary m_binFutureData;
    }

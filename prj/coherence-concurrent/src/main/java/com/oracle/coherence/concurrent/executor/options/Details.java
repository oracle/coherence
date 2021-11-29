/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.options;

import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

import java.util.Objects;

import java.util.concurrent.ExecutorService;

/**
 * An {@link TaskExecutorService.Registration.Option} to specify the {@link Details}
 * of a registered {@link ExecutorService}.
 *
 * This option shouldn't be used for targeting tasks.
 *
 * @author rl 11.12.2021
 * @since 21.12
 */
public class Details
        implements TaskExecutorService.Registration.Option, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Details} (required for Serializable).
     */
    @SuppressWarnings("unused")
    public Details()
        {
        }

    /**
     * Constructs a {@link Details} object.
     *
     * @param sDetails  the details of the executor
     */
    protected Details(String sDetails)
        {
        m_sDetails = sDetails;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the the {@link Details}.
     *
     * @return a {@link Details}
     */
    public String getName()
        {
        return m_sDetails;
        }

    /**
     * Obtains a {@link Details} with a specific info.
     *
     * @param sDetails  the details
     *
     * @return a {@link Details}
     */
    public static Details of(String sDetails)
        {
        return new Details(sDetails);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object object)
        {
        if (this == object)
            {
            return true;
            }

        if (!(object instanceof Details))
            {
            return false;
            }

        Details name = (Details) object;

        return Objects.equals(m_sDetails, name.m_sDetails);

        }

    @Override
    public int hashCode()
        {
        return m_sDetails != null ? m_sDetails.hashCode() : 0;
        }

    @Override
    public String toString()
        {
        return "ExecutorDetails{" + m_sDetails + '}';
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sDetails = in.readString(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sDetails);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The details to use when no details have been provided.
     */
    public static Details UNKNOWN = Details.of("Unknown");

    // ----- data members ---------------------------------------------------

    /**
     * The details of the executor.
     */
    protected String m_sDetails;
    }

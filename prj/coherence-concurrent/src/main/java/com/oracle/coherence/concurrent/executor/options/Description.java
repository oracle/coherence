/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.options;

import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

import java.util.concurrent.ExecutorService;

/**
 * An {@link TaskExecutorService.Registration.Option} to specify the {@link Description}
 * of a registered {@link ExecutorService}.
 *
 * This option shouldn't be used for targeting tasks.
 *
 * @author rl 11.12.2021
 * @since 21.12
 */
public class Description
        implements TaskExecutorService.Registration.Option, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Description} (required for serialization).
     */
    @SuppressWarnings("unused")
    public Description()
        {
        }

    /**
     * Constructs a {@link Description} object.
     *
     * @param sDescription  the description of the executor
     */
    protected Description(String sDescription)
        {
        m_sDescription = sDescription;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the {@link Description}.
     *
     * @return a {@link Description}
     */
    public String getName()
        {
        return m_sDescription;
        }

    /**
     * Obtains a {@link Description} based on the provided argument.
     *
     * @param sDescription  the description
     *
     * @return a {@link Description}
     */
    public static Description of(String sDescription)
        {
        return new Description(sDescription);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object object)
        {
        if (this == object)
            {
            return true;
            }

        if (!(object instanceof Description))
            {
            return false;
            }

        Description name = (Description) object;

        return Objects.equals(m_sDescription, name.m_sDescription);

        }

    @Override
    public int hashCode()
        {
        return m_sDescription != null ? m_sDescription.hashCode() : 0;
        }

    @Override
    public String toString()
        {
        return "Description{" + m_sDescription + '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sDescription = ExternalizableHelper.readUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeUTF(out, m_sDescription);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sDescription = in.readString(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sDescription);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The description to use when no description has been provided.
     */
    public static Description UNKNOWN = Description.of("None");

    // ----- data members ---------------------------------------------------

    /**
     * The details of the executor.
     */
    protected String m_sDescription;
    }

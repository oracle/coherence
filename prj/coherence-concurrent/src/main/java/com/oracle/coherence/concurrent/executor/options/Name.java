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

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

import java.util.concurrent.ExecutorService;

/**
 * An {@link TaskExecutorService.Registration.Option} to specify the {@link Name}
 * of a registered {@link ExecutorService}.
 *
 * @author rl 11.12.2021
 * @since 21.12
 */
public class Name
        implements TaskExecutorService.Registration.Option, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Name} (required for serialization).
     */
    @SuppressWarnings("unused")
    public Name()
        {
        }

    /**
     * Constructs a {@link Name}.
     *
     * @param sName  the executor name
     */
    protected Name(String sName)
        {
        m_sName = sName;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the name of the {@link Name}.
     *
     * @return the name of the {@link Name}
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Obtains a {@link Name} with a specific name.
     *
     * @param sName  the name of the {@link Name}
     *
     * @return a {@link Name}
     */
    public static Name of(String sName)
        {
        return new Name(sName);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object object)
        {
        if (this == object)
            {
            return true;
            }

        if (!(object instanceof Name))
            {
            return false;
            }

        Name name = (Name) object;

        return Objects.equals(m_sName, name.m_sName);

        }

    @Override
    public int hashCode()
        {
        return m_sName != null ? m_sName.hashCode() : 0;
        }

    @Override
    public String toString()
        {
        return "Name{" + m_sName + '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sName = ExternalizableHelper.readUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeUTF(out, m_sName);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sName = in.readString(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sName);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Name group for executors registered without a name.
     */
    public static Name UNNAMED = Name.of("UnNamed");

    // ----- data members ---------------------------------------------------

    /**
     * The name of the {@link Name}.
     */
    protected String m_sName;
    }

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
import java.util.concurrent.Executor;

/**
 * An {@link TaskExecutorService.Registration.Option} to specify the {@link Role}
 * of an {@link Executor} when it is registered.
 *
 * @author bo
 * @since 21.12
 */
public class Role
        implements TaskExecutorService.Registration.Option, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Role} (required for serialization).
     */
    @SuppressWarnings("unused")
    public Role()
        {
        }

    /**
     * Constructs a {@link Role}.
     *
     * @param sName  the name of the role
     */
    protected Role(String sName)
        {
        m_sName = sName;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the name of the {@link Role}.
     *
     * @return the name of the {@link Role}
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Obtains a {@link Role} with a specific name.
     *
     * @param sName  the name of the {@link Role}
     *
     * @return a {@link Role}
     */
    public static Role of(String sName)
        {
        return new Role(sName);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object object)
        {
        if (this == object)
            {
            return true;
            }

        if (!(object instanceof Role))
            {
            return false;
            }

        Role role = (Role) object;

        return Objects.equals(m_sName, role.m_sName);

        }

    @Override
    public int hashCode()
        {
        return m_sName != null ? m_sName.hashCode() : 0;
        }

    @Override
    public String toString()
        {
        return "Role{" + m_sName + '}';
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

    // ----- data members ---------------------------------------------------

    /**
     * The name of the {@link Role}.
     */
    protected String m_sName;
    }

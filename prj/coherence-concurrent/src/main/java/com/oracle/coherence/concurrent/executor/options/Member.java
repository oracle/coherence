/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.options;

import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.util.OptionsByType;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

/**
 * An {@link TaskExecutorService.Registration.Option} defining a
 * Coherence {@link com.tangosol.net.Member}.
 *
 * @author bo
 * @since 21.12
 */
public class Member
        implements TaskExecutorService.Registration.Option, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Member} (required for serialization).
     */
    public Member()
        {
        }

    /**
     * Constructs a {@link Member} {@link TaskExecutorService.Registration.Option}.
     *
     * @param member  the {@link com.tangosol.net.Member}
     */
    protected Member(com.tangosol.net.Member member)
        {
        m_member = member;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Obtains a {@link Member} {@link TaskExecutorService.Registration.Option} based
     * on the specified {@link com.tangosol.net.Member}.
     *
     * @param member  the {@link com.tangosol.net.Member}
     *
     * @return a {@link Member}
     */
    public static Member of(com.tangosol.net.Member member)
        {
        return new Member(member);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the {@link com.tangosol.net.Member}.
     *
     * @return the {@link com.tangosol.net.Member}
     */
    public com.tangosol.net.Member get()
        {
        return m_member;
        }

    /**
     * Auto-detects the {@link Member} based on the current environment.
     *
     * @return a {@link Member}
     */
    @OptionsByType.Default
    public static Member autoDetect()
        {
        return new Member(CacheFactory.getCluster().getLocalMember());
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object object)
        {
        if (this == object)
            {
            return true;
            }

        if (!(object instanceof Member))
            {
            return false;
            }

        Member other = (Member) object;

        return Objects.equals(m_member, other.m_member);

        }

    @Override
    public int hashCode()
        {
        return m_member != null ? m_member.hashCode() : 0;
        }

    @Override
    public String toString()
        {
        return m_member.toString();
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_member = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_member);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_member = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_member);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link com.tangosol.net.Member}.
     */
    private com.tangosol.net.Member m_member;
    }

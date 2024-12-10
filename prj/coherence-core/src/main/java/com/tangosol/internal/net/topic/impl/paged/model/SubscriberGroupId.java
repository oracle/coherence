/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

import java.util.concurrent.atomic.AtomicLong;

/**
 * An identifier for a subscriber group for a topic.
 *
 * @author jk 2015.09.30
 * @since Coherence 14.1.1
 */
// implementation note: SubscriptionGroupId does not implement Evolvable
// because adding fields would affect the "equality"
// of a key
public class SubscriberGroupId
        implements ExternalizableLite, PortableObject,Comparable<SubscriberGroupId>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public SubscriberGroupId()
        {
        }

    /**
     * Construct a named (durable) subscriber.
     *
     * @param sName the group name
     *
     * @see #withName(String)
     */
    public SubscriberGroupId(String sName)
        {
        m_sGroupId  = Objects.requireNonNull(sName, "Subscriber group name cannot be null");
        m_ldtMember = 0; // durable subscribers aren't tied to the life of a member
        }

    /**
     * Construct an anonymous (non-durable) subscriber
     *
     * @param member  the member associated with the subscriber.
     *
     * @see #anonymous()
     */
    public SubscriberGroupId(Member member)
        {
        m_sGroupId  = String.valueOf(s_counter.incrementAndGet());
        m_ldtMember = member.getTimestamp();
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Return the group name.
     *
     * @return the group name
     */
    public String getGroupName()
        {
        return m_sGroupId;
        }

    /**
     * Return the {@link Member#getTimestamp timestamp} for the member associated with an anonymous subscriber group.
     *
     * @return the timestamp if the group is anonymous, or 0 for durable groups
     */
    public long getMemberTimestamp()
        {
        return m_ldtMember;
        }

    /**
     * Returns {@code true} if this is a pseudo-group created for an anonymous subscriber.
     *
     * @return {@code true} if this is a pseudo-group created for an anonymous subscriber
     */
    public boolean isAnonymous()
        {
        return m_ldtMember != 0;
        }

    /**
     * Returns {@code true} if this is a durable subscriber group.
     *
     * @return {@code true} if this is a durable subscriber group
     */
    public boolean isDurable()
        {
        return !isAnonymous();
        }

    // ----- Comparable methods ---------------------------------------------

    @Override
    public int compareTo(SubscriberGroupId other)
        {
        if (other == null)
            {
            return 1;
            }

        int i = m_sGroupId.compareTo(other.m_sGroupId);
        if (i == 0)
            {
            i = Long.compare(m_ldtMember, other.m_ldtMember);
            }

        return i;
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sGroupId  = ExternalizableHelper.readSafeUTF(in);
        m_ldtMember = in.readLong();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sGroupId);
        out.writeLong(m_ldtMember);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sGroupId  = in.readString(0);
        m_ldtMember = in.readLong(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sGroupId);
        out.writeLong(1, m_ldtMember);
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        SubscriberGroupId that = (SubscriberGroupId) o;

        return m_ldtMember == that.m_ldtMember && Base.equals(m_sGroupId, that.m_sGroupId);
        }

    @Override
    public int hashCode()
        {
        return HashHelper.hash(m_ldtMember, m_sGroupId.hashCode());
        }

    @Override
    public String toString()
        {
        long lTimestamp = m_ldtMember;
        return getClass().getSimpleName() + "(" + (lTimestamp == 0 ? "" : lTimestamp + "/") + m_sGroupId + ")";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a SubscriberGroupId from a name.
     *
     * @param sName  the name
     *
     * @return the SubscriberGroupId
     */
    public static SubscriberGroupId withName(String sName)
        {
        return new SubscriberGroupId(sName);
        }

    /**
     * Create an anonymous SubscriberGroupId.
     *
     * @return the SubscriberGroupId
     */
    public static SubscriberGroupId anonymous()
        {
        return new SubscriberGroupId(CacheFactory.getCluster().getLocalMember());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The unique part of this {@link SubscriberGroupId}.
     */
    private String m_sGroupId;

    /**
     * The associated member timestamp for anonymous groups, when the member dies the subscriber will be automatically removed.
     *
     * Note: this leverages the fact that coherence will never assign two members of the same cluster the same timestamp
     */
    private long m_ldtMember;

    /**
     * Counter for generating unique anonymous group names scoped within the corresponding member's ID
     */
    private static final AtomicLong s_counter = new AtomicLong();
    }

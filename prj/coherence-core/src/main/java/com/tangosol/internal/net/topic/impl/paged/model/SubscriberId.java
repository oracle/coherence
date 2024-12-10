/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import com.tangosol.util.comparator.SafeComparator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

/**
 * An identifier for a topic subscriber.
 *
 * @author Jonathan Knight  2022.06.02
 * @since 22.06
 */
public class SubscriberId
        implements Subscriber.Id, Comparable<SubscriberId>, PortableObject, ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization
     */
    public SubscriberId()
        {
        this(0L, null);
        }

    /**
     * Create a {@link SubscriberId}.
     *
     * @param nId   the subscriber combined id
     * @param uuid  the owning member's {@link UUID}
     */
    public SubscriberId(long nId, UUID uuid)
        {
        m_nNotificationId = PagedTopicSubscriber.notificationIdFromId(nId);
        m_nMemberId       = PagedTopicSubscriber.memberIdFromId(nId);
        m_uuid            = uuid;
        m_nId             = nId;
        }

    /**
     * Create a {@link SubscriberId}.
     *
     * @param nNotificationId  the subscriber notification id
     * @param nMemberId        the subscriber id
     * @param uuid             the owning member's {@link UUID}
     */
    public SubscriberId(int nNotificationId, int nMemberId, UUID uuid)
        {
        m_nNotificationId = nNotificationId;
        m_nMemberId       = nMemberId;
        m_uuid            = uuid;
        m_nId             = PagedTopicSubscriber.createId(m_nNotificationId, m_nMemberId);
        }

    // ----- accessors ------------------------------------------------------

    public int getNotificationId()
        {
        return m_nNotificationId;
        }

    public int getMemberId()
        {
        return m_nMemberId;
        }

    public UUID getUID()
        {
        return m_uuid;
        }

    public long getId()
        {
        return m_nId;
        }

    // ----- Comparable methods ---------------------------------------------

    @Override
    public int compareTo(SubscriberId other)
        {
        int n = Long.compare(m_nId, other.m_nId);
        if (n == 0)
            {
            n = SafeComparator.compareSafe(null, m_uuid, other.m_uuid);
            }
        return n;
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nNotificationId = in.readInt();
        m_nMemberId       = in.readInt();
        m_uuid            = ExternalizableHelper.readObject(in);
        m_nId             = in.readLong();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(m_nNotificationId);
        out.writeInt(m_nMemberId);
        ExternalizableHelper.writeObject(out, m_uuid);
        out.writeLong(m_nId);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nNotificationId = in.readInt(0);
        m_nMemberId       = in.readInt(1);
        m_uuid            = in.readObject(2);
        m_nId             = in.readLong(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nNotificationId);
        out.writeInt(1, m_nMemberId);
        out.writeObject(2, m_uuid);
        out.writeLong(3, m_nId);
        }

    // ----- Object methods -------------------------------------------------

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
        SubscriberId that = (SubscriberId) o;
        return m_nNotificationId == that.m_nNotificationId
                && m_nMemberId == that.m_nMemberId
                && Objects.equals(m_uuid, that.m_uuid);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_nNotificationId, m_nMemberId, m_uuid);
        }

    @Override
    public String toString()
        {
        return "SubscriberId{" +
                "notificationId=" + m_nNotificationId +
                ", memberId=" + m_nMemberId +
                ", uid=" + m_uuid +
                ", id=" + m_nId +
                '}';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * A main method that takes one or more subscriber identifiers and parses
     * them to display the corresponding member id and notification id.
     *
     * @param args  one or more subscriber identifiers
     */
    public static void main(String[] args)
        {
        for (String s : args)
            {
            long nId = Long.parseLong(s);
            System.out.println(PagedTopicSubscriber.notificationIdFromId(nId)
                                       + " " + PagedTopicSubscriber.memberIdFromId(nId));
            }
        }

    // ----- constructors ---------------------------------------------------

    /**
     * A null subscriber identifier.
     */
    public static final SubscriberId NullSubscriber = new SubscriberId();

    // ----- data members ---------------------------------------------------

    /**
     * This subscriber's notification id.
     */
    private int m_nNotificationId;

    /**
     * The owning member id.
     */
    private int m_nMemberId;

    /**
     * The owning member {@link UUID}.
     */
    private UUID m_uuid;

    /**
     * The combined subscriber id.
     */
    private long m_nId;
    }

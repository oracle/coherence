/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.oracle.coherence.common.collections.Arrays;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.util.HashHelper;

import java.io.IOException;

import java.util.Collection;
import java.util.HashSet;

/**
 * Per-partition state associated with paged topics.
 *
 * @author mf  2016.02.25
 * @since Coherence 14.1.1
 */
public class Usage
    extends AbstractEvolvable
    implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------
    /**
     * Default constructor (for serialization)
     */
    public Usage()
        {
        }

    // -----  PagedTopicPartition interface ---------------------------------

    /**
     * Return the topic's global tail.
     *
     * @return the tail
     */
    public long getPublicationTail()
        {
        return m_lTailPublication;
        }

    /**
     * Set the topic's global tail.
     *
     * @param lPage the tail
     */
    public void setPublicationTail(long lPage)
        {
        m_lTailPublication = lPage;
        }

    /**
     * Return the partition's head page or {@link Page#NULL_PAGE} if unknown.
     *
     * @return the head page
     */
    public long getPartitionHead()
        {
        return m_lHead;
        }

    /**
     * Set the partition's head page.
     *
     * @param lPage the head page
     */
    public void setPartitionHead(long lPage)
        {
        m_lHead = lPage;
        }

    /**
     * Return the partition's tail page, or {@link Page#NULL_PAGE} if unknown.
     *
     * @return the tail page
     */
    public long getPartitionTail()
        {
        return m_lTail;
        }

    /**
     * Set the partition's tail page.
     *
     * @param lPage the tail page
     */
    public void setPartitionTail(long lPage)
        {
        m_lTail = lPage;
        }

    /**
     * Set the greatest page ever held by this partition.
     *
     * @param lPage the page
     */
    public void setPartitionMax(long lPage)
        {
        m_lMax = lPage;
        }

    /**
     * Return the greatest page ever held by this partition.
     *
     * @return the page
     */
    public long getPartitionMax()
        {
        return m_lMax;
        }

    /**
     * Return a mutable list of the anonymous subscribers for this partition.
     *
     * @return the anonymous subscribers or null
     */
    public Collection<SubscriberGroupId> getAnonymousSubscribers()
        {
        return m_colAnonymousSubscribers;
        }

    /**
     * Set the anonymous subscribers for this partition.
     *
     * @param subAnon the anonymous subscriber to add
     */
    public void addAnonymousSubscriber(SubscriberGroupId subAnon)
        {
        Collection<SubscriberGroupId> col = m_colAnonymousSubscribers;
        if (!(col instanceof HashSet)) // i.e. ImmutableArrayList from POF deserialization
            {
            m_colAnonymousSubscribers = col = col == null ? new HashSet<>() : new HashSet<>(col);
            }
        col.add(subAnon);
        }

    /**
     * Remove the anonymous subscribers for this partition.
     *
     * @param anon the anonymous subscriber to remove
     */
    public void removeAnonymousSubscriber(SubscriberGroupId anon)
        {
        Collection<SubscriberGroupId> col = m_colAnonymousSubscribers;
        if (col == null)
            {
            return;
            }
        else if (!(col instanceof HashSet)) // i.e. ImmutableArrayList from POF deserialization
            {
            m_colAnonymousSubscribers = col = new HashSet<>(col);
            }
        col.remove(anon);
        }

    /**
     * Increment the waiting subscriber count.
     *
     * @return the new count
     */
    public int incrementWaitingSubscriberCount()
        {
        return adjustWaitingSubscriberCount(1);
        }

    /**
     * Decrement the waiting subscriber count.
     *
     * @return the new count
     */
    public int decrementWaitingSubscriberCount()
        {
        return adjustWaitingSubscriberCount(-1);
        }

    /**
     * Adjust the waiting subscriber count.
     *
     * @param cAdjust  the amount to adjust the count by
     *
     * @return the new count
     */
    public int adjustWaitingSubscriberCount(int cAdjust)
        {
        return (m_cWaitingSubscribers += cAdjust);
        }

    public int getWaitingSubscriberCount()
        {
        return m_cWaitingSubscribers;
        }

    /**
     * Reset the waiting subscriber count to zero.
     *
     * @return the prior value
     */
    public int resetWaitingSubscriberCount()
        {
        int c = m_cWaitingSubscribers;
        m_cWaitingSubscribers = 0;
        return c;
        }

    /**
     * Add the specified notifier to the removal notification set.
     *
     * @param nNotifierId  the notifier id
     */
    public void addRemovalNotifier(int nNotifierId)
        {
        // Note: usage pattern will result in this method being called at most once per deserialization of the
        // parent object, thus it is far more efficient to just store as a pre-sorted array then in a more
        // friendly structure such as a LongArray which would pay the cost of needlessly getting sorted/balanced on each
        // deserialization.

        m_anNotifyOnRemove = Arrays.binaryInsert(m_anNotifyOnRemove, nNotifierId);
        }

    public int[] getRemovalNotifiers()
        {
        return m_anNotifyOnRemove;
        }

    /**
     * Remove and return all notifiers from the set of waiting removal notifiers
     */
    public int[] resetRemovalNotifiers()
        {
        int[] anNotify = m_anNotifyOnRemove;
        m_anNotifyOnRemove = null;
        return anNotify;
        }

    // ----- EvolvablePortableObject interface ------------------------------

    @Override
    public int getImplVersion()
        {
        return DATA_VERSION;
        }

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_lTailPublication        = in.readLong(0);
        m_lHead                   = in.readLong(1);
        m_lTail                   = in.readLong(2);
        m_lMax                    = in.readLong(3);
        m_colAnonymousSubscribers = in.readCollection(4, new HashSet<>());
        m_cWaitingSubscribers     = in.readInt(5);
        m_anNotifyOnRemove        = in.readObject(6);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong(0, m_lTailPublication);
        out.writeLong(1, m_lHead);
        out.writeLong(2, m_lTail);
        out.writeLong(3, m_lMax);
        out.writeCollection(4, m_colAnonymousSubscribers);
        out.writeInt(5, m_cWaitingSubscribers);
        out.writeIntArray(6, m_anNotifyOnRemove);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        int[] anRemove = m_anNotifyOnRemove;
        return getClass().getSimpleName()
                + "(globalTail=" + m_lTailPublication
                + ", head=" + m_lHead
                + ", tail=" + m_lTail
                + ", maxPage=" + m_lMax
                + ", waitingPubs=" + (anRemove == null ? 0 : anRemove.length)
                + ", waitingSubs=" + m_cWaitingSubscribers
                + ", removalNotifiers=" + java.util.Arrays.toString(m_anNotifyOnRemove)
                + ", anonSubs=" + m_colAnonymousSubscribers + ")";
        }

    // ----- inner class: Key -----------------------------------------------

    /**
     * The Key for Usage entries.
     */
    // implementation note: Key does not implement Evolvable
    // because adding fields would affect the "equality"
    // of a key
    public static class Key
        implements KeyPartitioningStrategy.PartitionAwareKey, PortableObject, Comparable<Usage.Key>
        {
        /**
         * Deserialization constructor.
         */
        public Key()
            {
            }

        /**
         * Construct a Key.
         *
         * @param nPartition  the partition id
         * @param nChannel    the channel id
         */
        public Key(int nPartition, int nChannel)
            {
            m_nPartition = nPartition;
            m_nChannel   = nChannel;
            }

        // ----- serialization interface ------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_nPartition = in.readInt(0);
            m_nChannel   = in.readInt(1);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeInt(0, m_nPartition);
            out.writeInt(1, m_nChannel);
            }

        // ----- Comparable interface ---------------------------------------

        @Override
        public int compareTo(Key o)
            {
            int n = Integer.compare(m_nChannel, o.m_nChannel);
            return n == 0 ? Integer.compare(m_nPartition, o.m_nPartition) : n;
            }

        // ----- Object interface -------------------------------------------

        @Override
        public int hashCode()
            {
            return HashHelper.hash(m_nPartition, m_nChannel);
            }

        @Override
        public String toString()
            {
            return "UsageKey(partition=" + m_nPartition + ", channel=" + m_nChannel + ")";
            }

        @Override
        public boolean equals(Object oThat)
            {
            if (oThat instanceof Key)
                {
                Key that = (Key) oThat;
                return that.m_nPartition == m_nPartition && that.m_nChannel == m_nChannel;
                }
            return false;
            }

        // ----- Key interface ----------------------------------------------

        public int getChannelId()
            {
            return m_nChannel;
            }

        // ----- PartitionAwareKey interface --------------------------------

        @Override
        public int getPartitionId()
            {
            return m_nPartition;
            }

        // ----- data members -----------------------------------------------

        /**
         * The partitioned id.
         */
        private int m_nPartition;

        /**
         * The channel id.
         */
        private int m_nChannel;
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The tail page across all partitions.
     *
     * Note: this value is only maintained within a single well known partition once the topic has been initialized.
     */
    private long m_lTailPublication = Page.NULL_PAGE;

    /**
     * The partition's head.
     */
    private long m_lHead = Page.NULL_PAGE;

    /**
     * The partition's tail.
     */
    private long m_lTail = Page.NULL_PAGE;

    /**
     * The greatest page ever held by this partition.
     */
    private long m_lMax = Page.NULL_PAGE;

    /**
     * The anonymous subscribers.
     */
    private Collection<SubscriberGroupId> m_colAnonymousSubscribers;

    /**
     * The number of subscribers waiting for a new page to be inserted.
     */
    private int m_cWaitingSubscribers;

    /**
     * Set of notifiers to notify upon a page removal.
     */
    private int[] m_anNotifyOnRemove;
    }

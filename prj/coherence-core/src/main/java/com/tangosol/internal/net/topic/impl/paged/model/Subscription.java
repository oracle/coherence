/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.util.Filter;
import com.tangosol.util.HashHelper;

import java.io.IOException;

import java.util.Objects;
import java.util.function.Function;

/**
 * Subscriber data for a particular cache partition.
 *
 * @author mf  2016.02.22
 * @since Coherence 14.1.1
 */
public class Subscription
    extends AbstractEvolvable
    implements EvolvablePortableObject
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Empty constructor for serialization.
     */
    public Subscription()
        {
        }

    // ----- TopicPartitionSubscription methods -----------------------

    /**
     * Return the subscriber's head page across all partitions
     *
     * @return the head page
     */
    public long getSubscriptionHead()
        {
        return m_lHeadSubscription;
        }

    /**
     * Set the subscriber's head page across all partitions
     *
     * @param lPage  the head page
     */
    public void setSubscriptionHead(long lPage)
        {
        m_lHeadSubscription = lPage;
        }

    /**
     * Return the subscriber's page within the partition
     *
     * @return the page
     */
    public long getPage()
        {
        return m_lPage;
        }

    /**
     * Set the subscriber's page within the partition
     *
     * @param lPage the page
     */
    public void setPage(long lPage)
        {
        m_lPage = lPage;
        }

    /**
     * Return the read position for this partition's active page.
     *
     * @return the read position
     */
    public int getPosition()
        {
        return m_nPosition;
        }

    /**
     * Set the read position for this partition's active page.
     *
     * @param nPosition the position
     */
    public void setPosition(int nPosition)
        {
        m_nPosition = nPosition;
        }

    /**
     * Return the subscription's filter, or null
     *
     * @return the filter
     */
    public Filter getFilter()
        {
        return m_filter;
        }

    /**
     * Set the subscription's filter
     *
     * @param filter the filter
     */
    public void setFilter(Filter filter)
        {
        m_filter = filter;
        }

    /**
     * Return the subscription's converter, or null.
     *
     * @return the converter {@link Function}
     */
    public Function getConverter()
        {
        return m_fnConvert;
        }

    /**
     * Set the subscription's converter {@link Function}.
     *
     * @param fnConvert  the filter
     */
    public void setConverter(Function fnConvert)
        {
        m_fnConvert = fnConvert;
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
        m_lHeadSubscription = in.readLong(0);
        m_lPage             = in.readLong(1);
        m_nPosition         = in.readInt(2);
        m_filter            = in.readObject(3);
        m_fnConvert         = in.readObject(4);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong(0, m_lHeadSubscription);
        out.writeLong(1, m_lPage);
        out.writeInt(2, m_nPosition);
        out.writeObject(3, m_filter);
        out.writeObject(4, m_fnConvert);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "(head=" + m_lHeadSubscription + ", page=" + m_lPage
            + ", position=" + m_nPosition + ", filter=" + m_filter + ", converter=" + m_fnConvert + ")";
        }

    // ----- Key class ------------------------------------------------------

    /**
     * Key for the parent class
     */
    // implementation note: Key does not implement Evolvable
    // because adding fields would affect the "equality"
    // of a key
    public static class Key
            implements KeyPartitioningStrategy.PartitionAwareKey, PortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * A default constructor used for serialization.
         */
        public Key()
            {
            }

        /**
         * Create a {@link Key} for the specified partition
         * and subscriber.
         *
         * @param nPartition  the partition id
         * @param nChannel    the channel id
         * @param groupId     the subscriber id
         */
        public Key(int nPartition, int nChannel, SubscriberGroupId groupId)
            {
            m_nPartition = nPartition;
            m_nChannel   = nChannel;
            m_groupId    = groupId;
            }

        // ----- accessor methods -------------------------------------------

        /**
         * Obtain the subscriber that this key is for.
         *
         * @return the subscriber that this key is for
         */
        public SubscriberGroupId getGroupId()
            {
            return m_groupId;
            }

        /**
         * Return the channel id.
         *
         * @return the channel id.
         */
        public int getChannelId()
            {
            return m_nChannel;
            }

        // ----- PartitionAwareKey methods ----------------------------------

        @Override
        public int getPartitionId()
            {
            return m_nPartition;
            }

        // ----- PortableObject methods -------------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_nPartition = in.readInt(0);
            m_nChannel   = in.readInt(1);
            m_groupId    = in.readObject(2);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(0, m_nPartition);
            out.writeInt(1, m_nChannel);
            out.writeObject(2, m_groupId);
            }

        // ----- object methods ---------------------------------------------

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

            Key that = (Key) o;
            return m_nPartition == that.m_nPartition &&
                   m_nChannel   == that.m_nChannel   &&
                   Objects.equals(m_groupId, that.m_groupId);
            }

        @Override
        public int hashCode()
            {
            int hash = HashHelper.hash(m_nPartition, m_nChannel);

            return HashHelper.hash(m_groupId, hash);
            }

        @Override
        public String toString()
            {
            return "SubscriberPartitionKey(Partition=" + m_nPartition +
                    ", channel=" + m_nChannel +
                    ", subscriber='" + m_groupId + "\')";
            }

        // ----- data members -----------------------------------------------

        /**
         * The partition id of this key.
         */
        private int m_nPartition;

        /**
         * The channel id.
         */
        private int m_nChannel;

        /**
         * The subscriber that this key is for.
         */
        private SubscriberGroupId m_groupId;
        }

     // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The head page across all partitions for this subscriber.
     *
     * Note: this value is only maintained within a single well known partition once the subscription has been initialized.
     */
    private long m_lHeadSubscription = Page.NULL_PAGE;

    /**
     * The page of interest to the subscriber within this partition.
     */
    private long m_lPage = Page.NULL_PAGE;

    /**
     * The read position within the active page, or Integer.MAX_VALUE when the current page is exhausted, but another has
     * yet to arrive for this partition.
     */
    private int m_nPosition;

    /**
     * The subscriber filter, or null.
     */
    private Filter m_filter;

    /**
     * The optional converter to use to convert the value before returning to it the subscriber.
     */
    private Function m_fnConvert;
    }

/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.oracle.coherence.common.collections.Arrays;

import com.tangosol.internal.util.Primes;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.util.ClassHelper;

import java.io.IOException;

/**
 * This class represents information about a page in a topic.
 *
 * In the implementation of a topic elements in the topic are
 * stored in pages so as to store the elements in groups for more
 * efficient access but still spread them around the cluster in a reasonably
 * even manner.
 *
 * @author jk 2015.05.18
 * @since Coherence 14.1.1
 */
public class Page
        extends AbstractEvolvable
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization
     */
    public Page()
        {
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Obtain the id of the next page in this partition or NULL_PAGE if unknown
     *
     * @return the id of the next page
     */
    public long getNextPartitionPage()
        {
        return m_lNextPage;
        }

    /**
     * Set the id of the next page in this partition.
     *
     * @param lPage the next page
     */
    public void setNextPartitionPage(long lPage)
        {
        m_lNextPage = lPage;
        }

    /**
     * Obtain the current tail of this page.
     *
     * @return the current tail of this page
     */
    public int getTail()
        {
        return m_nTail;
        }

    /**
     * Set the current tail of this page.
     *
     * @param nTail  the current tail of this page
     */
    public void setTail(int nTail)
        {
        m_nTail = nTail;
        }

    /**
     * Obtain page size in bytes.
     *
     * @return the current page size.
     */
    public int getByteSize()
        {
        return m_cb;
        }

    /**
     * Set the page size in bytes.
     *
     * @param cb  the current byte size of this page
     */
    public void setByteSize(int cb)
        {
        m_cb = cb;
        }

    /**
     * Determine whether this {@link Page} is accepting offers
     * of new elements or is sealed an no longer accepting offers.
     *
     * @return true if this {@link Page} is sealed and no longer
     *              accepting offers of new elements
     */
    public boolean isSealed()
        {
        return m_fSealed;
        }

    /**
     * Set whether this {@link Page} is sealed and no longer
     * accepting offers of new elements.
     *
     * @param fSealed flag indicating that this {@link Page} is
     *                sealed and not accepting offers of new elements
     */
    public void setSealed(boolean fSealed)
        {
        m_fSealed = fSealed;
        }

    /**
     * Add the specified notifier to the insertion notification set.
     *
     * @param nNotifierId  the notifier id
     */
    public void addInsertionNotifier(int nNotifierId)
        {
        // Note: usage pattern will result in this method being called at most once per deserialization of the
        // parent object, thus it is far more efficient to just store as a pre-sorted array then in a more
        // friendly structure such as a LongArray which would pay the cost of needlessly getting sorted/balanced on each
        // deserialization.

        m_anNotifiers = Arrays.binaryInsert(m_anNotifiers, nNotifierId);
        }

    /**
     * Remove and return all notifiers from the set of waiting subscribers.
     */
    public int[] resetInsertionNotifiers()
        {
        int[] anNotify = m_anNotifiers;
        m_anNotifiers = null;
        return anNotify;
        }

    /**
     * Return the array of insertion notifiers.
     *
     * @return the insertion notifier array
     */
    public int[] getInsertionNotifiers()
        {
        return m_anNotifiers;
        }

    /**
     * Set the insertion notifier array.
     *
     * @param anNotifiers an array of sorted notifier ids
     */
    public void setInsertionNotifies(int[] anNotifiers)
        {
        m_anNotifiers = anNotifiers;
        }

    /**
     * Adjust the pages reference count
     *
     * @param c the amount to adjust by
     *
     * @return the new count
     */
    public int adjustReferenceCount(int c)
        {
        return m_cRefs += c;
        }

    /**
     * Return true if the page has subscribers.
     *
     * @return true iff there are attached subscribers
     */
    public boolean isSubscribed()
        {
        return m_cRefs > 0;
        }

    // ----- QueuePage methods --------------------------------------------

    /**
     * Marks this page as being empty
     */
    public void markEmpty()
        {
        m_nTail = EMPTY;
        m_cb    = 0;
        }

    /**
     * Returns true if this page is empty, otherwise returns false.
     *
     * @return true if this page is empty, otherwise returns false
     */
    public boolean isEmpty()
        {
        return m_nTail == EMPTY;
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
        m_lNextPage   = in.readLong(0);
        m_nTail       = in.readInt(1);
        m_fSealed     = in.readBoolean(2);
        m_cb          = in.readInt(3);
        m_anNotifiers = in.readIntArray(4);
        m_cRefs       = in.readInt(5);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong(0, m_lNextPage);
        out.writeInt(1, m_nTail);
        out.writeBoolean(2, m_fSealed);
        out.writeInt(3, m_cb);
        out.writeIntArray(4, m_anNotifiers);
        out.writeInt(5, m_cRefs);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Provide a human-readable representation of this object.
     *
     * @return a String whose contents represent the value of this object
     */
    public String toString()
        {
        int[] anNotify = m_anNotifiers;
        return ClassHelper.getSimpleName(getClass()) + "(next=" + m_lNextPage + ", tail=" + m_nTail
                + ", bytes=" + m_cb + ", sealed=" + m_fSealed + ", refs=" + m_cRefs
                + ", waiting=" + (anNotify == null ? 0 : anNotify.length) + ')';
        }

    // ----- inner class: Key -----------------------------------------------

    /**
     * Key class for Pages.
     *
     * Note we don't simply use Long as that doesn't ensure even key distribution like this class
     * can.  Since page numbers aren't random their distribution won't be either and we can get
     * clumpy partitions.
     */
    // implementation note: Key does not implement Evolvable
    // because adding fields would affect the "equality"
    // of a key
    public static class Key
        implements KeyPartitioningStrategy.PartitionAwareKey, PortableObject
        {
        // ----- Constructors -----------------------------------------------

        /**
         * Deserialization constructor
         */
        public Key()
            {
            }

        /**
         * Construct a Key.
         *
         * @param nChannel  the associated channnel
         * @param lPage     the associated page
         */
        public Key(int nChannel, long lPage)
            {
            m_nChannel = nChannel;
            m_lPage    = lPage;
            }

        // ----- Key methods ------------------------------------------------

        /**
         * Return the channel id.
         *
         * @return the channel id
         */
        public int getChannelId()
            {
            return m_nChannel;
            }

        /**
         * Return the page id.
         *
         * @return the page id
         */
        public long getPageId()
            {
            return m_lPage;
            }

        @Override
        public int getPartitionId()
            {
            return mapPageToPartition(m_nChannel, m_lPage);
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public int hashCode()
            {
            return getPartitionId();
            }

        @Override
        public boolean equals(Object oThat)
            {
            if (oThat instanceof Key)
                {
                Key that = (Key) oThat;
                return m_nChannel == that.m_nChannel && m_lPage == that.m_lPage;
                }

            return false;
            }

        @Override
        public String toString()
            {
            return "PageKey(channel=" + m_nChannel + ", page=" + m_lPage + ")";
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_nChannel = in.readInt(0);
            m_lPage    = in.readLong(1);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeInt(0, m_nChannel);
            out.writeLong(1, m_lPage);
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Map the specified page id to its partition
         *
         * @param nChannel the channel
         * @param lPage    the page
         *
         * @return the partition
         */
        public static int mapPageToPartition(int nChannel, long lPage)
            {
            // multiplying the page by a large prime randomizes the mapping of page to partition "randomly"
            // this is done to counteract the fact that coherence is likely to clump sequential partitions
            // onto the same member and we want our pages to be spread evenly.  This is especially important
            // when using size limited topics.  Imagine a server holding N consecutive partitions, and
            // a high-units of XMB if X < N and the page size is 1MB then we this server could reach its
            // high-units limit and disallow more entries into the entire topic while the other cache servers
            // sat well under their limit.

            // We also include the channel to help push the same page numbers for different channels into different
            // partitions in order to help spread load across servers

            // Note: this is better then just randomized, based on Fermet's little theorem the distribution across
            // partitions will be perfectly balanced, i.e. with a partition count of N, a partition which holds
            // page X will next hold page X+N. We could use this to improve our intra partition page tracking
            // to compute the next page rather then to wait to discover it.
            return (int) (lPage * Primes.next(1000003, nChannel));
            }

        // ----- data members -------------------------------------------

        /**
         * The channel.
         */
        protected int m_nChannel;

        /**
         * The page
         */
        protected long m_lPage;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The value used for the head and tail that signifies that this page is empty.
     */
    public static final int EMPTY = -1;

    /**
     * The id used for an unknown page.
     */
    public static final long NULL_PAGE = -1;

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The id of the next page in this partition, or NULL_PAGE if unknown
     */
    protected long m_lNextPage = NULL_PAGE;

    /**
     * The id of the current tail element of this {@link Page}
     */
    protected int m_nTail = EMPTY;

    /**
     * A flag indicating whether this page is sealed and hence no longer accepting offers
     */
    protected boolean m_fSealed;

    /**
     * The current size of this page.
     */
    protected int m_cb;

    /**
     * The page's reference count.
     */
    protected int m_cRefs;

    /**
     * The set of notifiers to notify after an insert into the page
     */
    protected int[] m_anNotifiers;
    }
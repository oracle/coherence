/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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

import com.tangosol.net.topic.Position;

import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.EntryExtractor;

import java.io.IOException;

import java.util.Map;

/**
 * This class represents information about a page in a topic.
 * <p/>
 * In the implementation of a topic elements in the topic are
 * stored in pages to store the elements in groups for more
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
     * Obtain the id of the previous page in this partition or NULL_PAGE if unknown
     *
     * @return the id of the previous page
     */
    public long getPreviousPartitionPage()
        {
        return m_lPrevPage;
        }

    /**
     * Set the id of the previous page in this partition.
     *
     * @param lPage the previous page
     */
    public void setPreviousPartitionPage(long lPage)
        {
        m_lPrevPage = lPage;
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
     * Return the timestamp of the first element in the page.
     *
     * @return the timestamp of the first element in the page
     */
    public long getHeadTimestamp()
        {
        return m_lTimestampHead;
        }

    /**
     * Set the timestamp of the first element in the page.
     *
     * @param lTimestamp  the timestamp of the first element in the page
     */
    public void setTimestampHead(long lTimestamp)
        {
        m_lTimestampHead = lTimestamp;
        }

    /**
     * Return the timestamp of the last element in the page.
     *
     * @return the timestamp of the last element in the page
     */
    public long getTailTimestamp()
        {
        return m_lTimestampTail;
        }

    /**
     * Set the timestamp of the last element in the page.
     *
     * @param lTimestamp  the timestamp of the last element in the page
     */
    public void setTimestampTail(long lTimestamp)
        {
        m_lTimestampTail = lTimestamp;
        }

    /**
     * Compares the timestamps for the head and tail elements of this page with the specified
     * timestamp.
     *
     * @param lTimestamp  the timestamp to check
     *
     * @return a value of zero if the timestamp falls between the head and tail timestamps,
     *         a value less than zero if this page timestamps are less than the timestamp,
     *         or a value greater than zero if this page's timestamps are greater than the
     *         specified timestamp.
     */
    public int compareTimestamp(long lTimestamp)
        {
        if (m_lTimestampHead > lTimestamp)
            {
            // this page is after the specified timestamp
            return 1;
            }
        else if (m_lTimestampTail < lTimestamp)
            {
            // this page is before the specified timestamp
            return -1;
            }
        else
            {
            // the timestamp falls within this page's timestamps
            return 0;
            }
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
     * Determine whether this {@link Page} is sealed and empty.
     * <p>
     * Empty, sealed, pages are typically inserted by publish requests when is size limited
     * topic is full. Removal of this page on a commit will then trigger notifications to
     * blocked {@link com.tangosol.net.topic.Publisher publishers} that they can attempt to
     * publish more messages.
     *
     * @return true if this {@link Page} is sealed and empty
     */
    public boolean isSealedAndEmpty()
        {
        return m_fSealed && m_nTail == Page.EMPTY;
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
     * Increment the pages reference count by one.
     *
     * @return the new count
     */
    public int incrementReferenceCount()
        {
        return adjustReferenceCount(1);
        }

    /**
     * Decrement the pages reference count by one.
     *
     * @return the new count
     */
    public int decrementReferenceCount()
        {
        return adjustReferenceCount(-1);
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
     * Returns this {@link Page pages} reference count.
     *
     * @return this {@link Page pages} reference count
     */
    public int getReferenceCount()
        {
        return m_cRefs;
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
        if (getImplVersion() >= 2)
            {
            m_lPrevPage = in.readLong(6);
            m_lTimestampHead = in.readLong(7);
            m_lTimestampTail = in.readLong(8);
            }
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
        out.writeLong(6, m_lPrevPage);
        out.writeLong(7, m_lTimestampHead);
        out.writeLong(8, m_lTimestampTail);
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
        return ClassHelper.getSimpleName(getClass()) + "(next=" + m_lNextPage + ", prev=" + m_lPrevPage
                + ", tail=" + m_nTail + " headTime=" + m_lTimestampHead + " tailTime=" + m_lTimestampTail
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
        implements KeyPartitioningStrategy.PartitionAwareKey, PortableObject, Comparable<Page.Key>
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

        // ----- Comparable methods -----------------------------------------

        @Override
        public int compareTo(Key other)
            {
            int n = Integer.compare(m_nChannel, other.m_nChannel);
            if (n == 0)
                {
                n = Long.compare(m_lPage, other.m_lPage);
                }
            return n;
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

    // ----- inner class HeadExtractor --------------------------------------

    /**
     * An {@link EntryExtractor} that can extract the head position from a {@link Page}.
     */
    public static class HeadExtractor
            extends EntryExtractor
            implements PortableObject
        {
        @Override
        @SuppressWarnings("rawtypes")
        public Object extractFromEntry(Map.Entry entry)
            {
            return new PagedPosition(((Page.Key) entry.getKey()).getPageId(), 0);
            }

        /**
         * A singleton instance of {@link HeadExtractor}.
         */
        @SuppressWarnings("unchecked")
        public static ValueExtractor<Map.Entry<Page.Key, Page>, Position> INSTANCE = new HeadExtractor();
        }

    // ----- inner class TailExtractor --------------------------------------

    /**
     * An {@link EntryExtractor} that can extract the tail position from a {@link Page}.
     */
    public static class TailExtractor
            extends EntryExtractor
            implements PortableObject
        {
        @Override
        @SuppressWarnings("rawtypes")
        public Object extractFromEntry(Map.Entry entry)
            {
            return new PagedPosition(((Page.Key) entry.getKey()).getPageId(), ((Page) entry.getValue()).getTail());
            }

        /**
         * A singleton instance of {@link TailExtractor}.
         */
        @SuppressWarnings("unchecked")
        public static ValueExtractor<Map.Entry<Page.Key, Page>, Position> INSTANCE = new TailExtractor();
        }

    // ----- inner class ElementExtractor -----------------------------------

    /**
     * An {@link EntryExtractor} that can extract an {@link com.tangosol.net.topic.Subscriber.Element}
     * from the topic content cache.
     * <p>
     * This requires a custom extractor because the cache values are not actually serialized elements,
     * they are just custom {@link com.tangosol.util.Binary} instances.
     *
     * @param <V>  the type of the element's value
     */
    public static class ElementExtractor<V>
            extends EntryExtractor
            implements PortableObject
        {
        @Override
        @SuppressWarnings("rawtypes")
        public Subscriber.Element<V> extractFromEntry(Map.Entry entry)
            {
            BinaryEntry<?, ?> binaryEntry = (BinaryEntry) entry;
            return PageElement.fromBinary(binaryEntry.getBinaryValue(), binaryEntry.getSerializer());
            }

        @Override
        @SuppressWarnings("rawtypes")
        public Object extract(Object oTarget)
            {
            return extractFromEntry((Map.Entry) oTarget);
            }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public static <V, E> ValueExtractor<V, E> chained(ValueExtractor<Subscriber.Element<V>, E> extractor)
            {
            return new ChainedExtractor<>(new ElementExtractor<>(), extractor);
            }

        @SuppressWarnings("unchecked")
        public static <V> ValueExtractor<Object, Subscriber.Element<V>> instance()
            {
            return (ElementExtractor<V>) INSTANCE;
            }

        private static final ElementExtractor<?> INSTANCE = new ElementExtractor<>();
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
    public static final int DATA_VERSION = 2;

    // ----- data members ---------------------------------------------------

    /**
     * The id of the next page in this partition, or NULL_PAGE if unknown
     */
    protected long m_lNextPage = NULL_PAGE;

    /**
     * The id of the previous page in this partition, or NULL_PAGE if unknown
     */
    protected long m_lPrevPage = NULL_PAGE;

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

    /**
     * The timestamp of the first element in the page.
     */
    protected long m_lTimestampHead;

    /**
     * The timestamp of the last element in the page.
     */
    protected long m_lTimestampTail;
    }
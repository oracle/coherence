/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} for polling
 * from the head element of a {@link com.tangosol.internal.net.topic.impl.paged.model.Page}.
 *
 * @author jk 2015.05.22
 * @since Coherence 14.1.1
 */
public class PollProcessor
        extends AbstractPagedTopicProcessor<Subscription.Key, Subscription, PollProcessor.Result>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization
     */
    public PollProcessor()
        {
        super(PagedTopicPartition::ensureTopic);
        }

    /**
     * Create a {@link PollProcessor} that will perform a poll from the
     * head {@link com.tangosol.internal.net.topic.impl.paged.model.Page} of a {@link com.tangosol.internal.net.topic.impl.paged.PagedTopic}.
     *
     * @param lPage             the page to poll from
     * @param cElements         the desired number of elements to poll
     * @param nNotifyPostEmpty  notification key to delete when emptying the empty state, or zero for none
     */
    public PollProcessor(long lPage, int cElements, int nNotifyPostEmpty)
        {
        super(PagedTopicPartition::ensureTopic);
        m_lPage            = lPage;
        m_cElements        = cElements;
        m_nNotifyPostEmpty = nNotifyPostEmpty;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Result process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        // Note: the PollProcessor and OfferProcessor both target the page, this ensures
        // that they cannot run concurrently on the same page.  Simply sharing the same association does not ensure
        // this.

        return ensureTopic(entry).pollFromPageHead(
                (BinaryEntry<Subscription.Key, Subscription>) entry, m_lPage, m_cElements, m_nNotifyPostEmpty);
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
        m_lPage            = in.readLong(0);
        m_cElements        = in.readInt(1);
        m_nNotifyPostEmpty = in.readInt(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong(0, m_lPage);
        out.writeInt(1, m_cElements);
        out.writeInt(2, m_nNotifyPostEmpty);
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The page to poll.
     */
    protected long m_lPage;

    /**
     * The desired number of elements.
     */
    protected int m_cElements;

    /**
     * Post empty notification key.
     */
    protected int m_nNotifyPostEmpty;

    /**
     * A {@link Result} is returned to a consumer as the
     * result of a poll operation on a topic.
     *
     * @author jk 2015.08.24
     * @since Coherence 14.1.1
     */
    public static class Result
            extends AbstractEvolvable
            implements EvolvablePortableObject
        {
        // ----- constructors ---------------------------------------------------

        /**
         * The default constructor used for serialization.
         */
        public Result()
            {
            }

        /**
         * Create a {@link Result}.
         *
         * @param cElementsRemaining  true iff the target page has been exhausted
         * @param nNext               the index of the next element in the page
         * @param listElements        the elements to return in this result
         */
        public Result(int cElementsRemaining, int nNext, List<Binary> listElements)
            {
            m_cElementsRemaining = cElementsRemaining;
            m_nNext              = nNext;
            m_listElements       = listElements == null ? Collections.emptyList() : listElements;
            }

        // ----- accessor methods -----------------------------------------------

        /**
         * Return the number of remaining elements, or {@link #EXHAUSTED} if there are none and the
         * page will be accepting no new elements.
         *
         * @return the number of remaining elements
         */
        public int getRemainingElementCount()
            {
            return m_cElementsRemaining;
            }

        /**
         * Return the index of the Next element in the page.
         *
         * @return the index of the Next element in the page
         */
        public int getNextIndex()
            {
            return m_nNext;
            }

        /**
         * Obtain the elements polled from the topic in this result.
         *
         * @return  the elements polled from the topic in this result
         */
        public List<Binary> getElements()
            {
            return m_listElements == null ? Collections.emptyList() : m_listElements;
            }

        // ----- EvolvablePortableObject interface --------------------------

        @Override
        public int getImplVersion()
            {
            return DATA_VERSION;
            }

       @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_cElementsRemaining = in.readInt(0);
            m_nNext              = in.readInt(1);
            m_listElements       = in.readCollection(2, new ArrayList<>());
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(0, m_cElementsRemaining);
            out.writeInt(1, m_nNext);
            out.writeCollection(2, m_listElements, Binary.class);
            }

        // ----- object methods -------------------------------------------------

        @Override
        public String toString()
            {
            return "TopicPollResult(" +
                    "remaining=" + m_cElementsRemaining +
                    ", next=" + m_nNext +
                    ", retrieved=" + m_listElements +
                    ')';
            }

        // ----- constants ------------------------------------------------------

        /**
         * Special value indicating that the page is empty and will not accept any
         * new elements.
         */
        public static final int EXHAUSTED = -1;

        /**
         * {@link EvolvablePortableObject} data version of this class.
         */
        public static final int DATA_VERSION = 1;

        /**
         * Special value indicating that the subscriber is unknown.
         */
        public static final int UNKNOWN_SUBSCRIBER = -2;

        // ----- data members ---------------------------------------------------

        /**
         * The number of elements remaining in this page, or EXHAUSTED.
         */
        private int m_cElementsRemaining;

        /**
         * The index of the next element in this page.
         *
         * This can be used by the subscriber to help in detection channel collisions.
         */
        private int m_nNext;

        /**
         * A list containing the elements retrieved from the topic
         */
        private List<Binary> m_listElements;
        }
    }

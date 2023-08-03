/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.AbstractEvolvable;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.topic.TopicException;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.UUID;
import com.tangosol.util.ValueExtractor;

import java.io.IOException;

import java.util.Collection;

import java.util.stream.Collectors;

/**
 * The EnsureSubscriberPartitionProcessor ensures that the subscriber is known to each partition
 *
 * @author mf  2016.02.16
 * @since Coherence 14.1.1
 */
public class EnsureSubscriptionProcessor
        extends AbstractPagedTopicProcessor<Subscription.Key, Subscription, EnsureSubscriptionProcessor.Result>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------
    
    /**
     * Default constructor (for serialization).
     */
    @SuppressWarnings("unused")
    public EnsureSubscriptionProcessor()
        {
        super(PagedTopicPartition::ensureTopic);
        }

    /**
     * Construct the processor.
     *
     * @param nPhase         the initialization phase
     * @param alPage         the page (by channel) at which to start pinning
     * @param filter         the filter indicating which values are of interest
     * @param extractor      the optional converter function to convert values before they are
     *                       returned to subscribers
     * @param subscriberId   the unique identifier of the subscriber
     * @param fReconnect     {@code true} if this is a subscriber reconnection
     */
    public EnsureSubscriptionProcessor(int nPhase, long[] alPage, Filter<?> filter, ValueExtractor<?, ?> extractor,
                                       SubscriberId subscriberId, boolean fReconnect, boolean fCreateGroupOnly,
                                       long lSubscriptionId)
        {
        super(PagedTopicPartition::ensureTopic);

        m_nPhase           = nPhase;
        m_alPage           = alPage;
        m_filter           = filter;
        m_extractor        = extractor;
        m_subscriberId     = subscriberId;
        m_fReconnect       = fReconnect;
        m_fCreateGroupOnly = fCreateGroupOnly;
        m_lSubscriptionId  = lSubscriptionId;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Result process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        try
            {
            long[] alPage = ensureTopic(entry).ensureSubscription(entry.getKey(), this);
            return new Result(alPage, m_lSubscriptionId, null);
            }
        catch (Throwable thrown)
            {
            Logger.err(thrown);
            return new Result(null, m_lSubscriptionId, thrown);
            }
        }

    // ----- accessors ------------------------------------------------------

    public int getPhase()
        {
        return m_nPhase;
        }

    public long[] getPages()
        {
        return m_alPage;
        }

    public Filter<?> getFilter()
        {
        return m_filter;
        }

    public ValueExtractor<?, ?> getConverter()
        {
        return m_extractor;
        }

    public SubscriberId getSubscriberId()
        {
        return m_subscriberId;
        }

    public boolean isReconnect()
        {
        return m_fReconnect;
        }

    public boolean isCreateGroupOnly()
        {
        return m_fCreateGroupOnly;
        }

    public long getSubscriptionId()
        {
        return m_lSubscriptionId;
        }

    /**
     * Set the subscription identifier.
     *
     * @param lSubscriptionId  the subscription identifier
     */
    public void setSubscriptionId(long lSubscriptionId)
        {
        m_lSubscriptionId = lSubscriptionId;
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
        int  nVersion      = getDataVersion();
        UUID uuid          = null;
        long nSubscriberId = 0;

        m_nPhase    = in.readInt(0);
        m_alPage    = in.readLongArray(1);
        m_filter    = in.readObject(2);
        m_extractor = in.readObject(3);

        if (nVersion >= 2)
            {
            nSubscriberId      = in.readLong(4);
            m_fReconnect       = in.readBoolean(5);
            m_fCreateGroupOnly = in.readBoolean(6);
            }

        if (nVersion >= 3)
            {
            uuid = in.readObject(7);
            }

        if (nVersion >= 4)
            {
            m_lSubscriptionId = in.readLong(8);
            }

        m_subscriberId = new SubscriberId(nSubscriberId, uuid);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeInt(0, m_nPhase);
        out.writeLongArray(1, m_alPage);
        out.writeObject(2, m_filter);
        out.writeObject(3, m_extractor);
        out.writeObject(4, m_subscriberId.getId());
        out.writeBoolean(5, m_fReconnect);
        out.writeBoolean(6, m_fCreateGroupOnly);
        out.writeObject(7, m_subscriberId.getUID());
        out.writeLong(8, m_lSubscriptionId);
        }

    // ----- inner class: Result --------------------------------------------

    /**
     * The result returned by the {@link EnsureSubscriptionProcessor}.
     */
    public static class Result
            extends AbstractEvolvable
            implements EvolvablePortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public Result()
            {
            }

        /**
         * Create a result.
         *
         * @param alPage         the array of pages
         * @param lSubscription  the subscription identifier
         * @param error          any error that may have occurred
         */
        public Result(long[] alPage, long lSubscription, Throwable error)
            {
            m_alPage        = alPage;
            m_lSubscription = lSubscription;
            m_error         = error;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Returns {@code true} if this result contains an error.
         *
         * @return {@code true} if this result contains an error
         */
        public boolean hasError()
            {
            return m_error != null;
            }

        /**
         * Returns the array of pages.
         *
         * @return the array of pages
         */
        public long[] getPages()
            {
            return m_alPage;
            }

        /**
         * Returns any error that occurred, or {@code null} if no error occurred.
         *
         * @return any error that occurred, or {@code null} if no error occurred
         */
        public Throwable getError()
            {
            return m_error;
            }

        /**
         * Return the subscription identifier.
         *
         * @return  the subscription identifier
         */
        public long getSubscription()
            {
            return m_lSubscription;
            }

        // ----- EvolvablePortableObject methods ----------------------------

        @Override
        public int getImplVersion()
            {
            return DATA_VERSION;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            int nVersion = getDataVersion();
            m_alPage = in.readLongArray(0);
            m_error  = in.readObject(1);

            if (nVersion >= 3)
                {
                m_lSubscription = in.readLong(2);
                }
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeLongArray(0, m_alPage);
            out.writeObject(1, m_error);
            out.writeLong(2, m_lSubscription);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Returns a collection of pages from the results.
         * <p>
         * This method will throw a {@link TopicException} if any of the results is
         * an error result.
         *
         * @param colResult  the results to get the pages from
         *
         * @return a collection of pages from the results
         *
         * @throws TopicException if any of the results is an error result.
         */
        public static Collection<long[]> assertPages(Collection<Result> colResult)
            {
            if (colResult == null)
                {
                return null;
                }

            TopicException error = EnsureSubscriptionProcessor.Result.findFirstError(colResult);
            if (error != null)
                {
                throw error;
                }
            return getPages(colResult);
            }

        /**
         * Returns the first error from any error result, or {@code null}
         * if there are no errors.
         *
         * @param colResult  the results to get the error from
         *
         * @return the first error from any error result, or {@code null}
         *         if there are no errors
         */
        public static TopicException findFirstError(Collection<Result> colResult)
            {
            if (colResult == null)
                {
                return null;
                }

            return colResult.stream()
                    .filter(EnsureSubscriptionProcessor.Result::hasError)
                    .map(Result::getError)
                    .map(TopicException::new)
                    .findFirst()
                    .orElse(null);
            }

        /**
         * Returns a collection of pages from the results.
         *
         * @param colResult  the results to get the pages from
         *
         * @return a collection of pages from the results
         */
        public static Collection<long[]> getPages(Collection<Result> colResult)
            {
            if (colResult == null)
                {
                return null;
                }

            return colResult.stream()
                    .filter(r -> !r.hasError())
                    .map(Result::getPages)
                    .collect(Collectors.toList());
            }

        // ----- constants --------------------------------------------------

        /**
         * The evolvable data version for the {@link Result} class.
         */
        public static final int DATA_VERSION = 3;

        // ----- data members -----------------------------------------------

        /**
         * The subscription pages.
         */
        private long[] m_alPage;

        /**
         * Any error that occurred ensuring the subscription.
         */
        private Throwable m_error;

        /**
         * The subscription id.
         */
        private long m_lSubscription;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Mode indicating that we just want to inquire about any pinned page.
     */
    public static final int PHASE_INQUIRE = 0;

    /**
     * Mode indicating that we want to pin pages.
     */
    public static final int PHASE_PIN = 1;

    /**
     * Mode indicating that we want to advance to the specified page.
     */
    public static final int PHASE_ADVANCE = 2;

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 4;

    // ----- data members ---------------------------------------------------

    /**
     * The initialization phase.
     */
    private int m_nPhase;

    /**
     * The subscriber per-channel page to advance to, or null during inquire
     */
    private long[] m_alPage;

    /**
     * Optional subscriber filter.
     */
    private Filter<?> m_filter;

    /**
     * The optional converter function to convert values before they are returned to subscribers.
     */
    private ValueExtractor<?, ?> m_extractor;

    /**
     * The subscriber identifier.
     */
    private SubscriberId m_subscriberId;

    /**
     * A flag indicating that this is a reconnection.
     */
    private boolean m_fReconnect;

    /**
     * A flag indicating that this is only a subscriber group creation.
     */
    private boolean m_fCreateGroupOnly;

    /**
     * The unique id of the subscriber group.
     */
    private long m_lSubscriptionId;
    }

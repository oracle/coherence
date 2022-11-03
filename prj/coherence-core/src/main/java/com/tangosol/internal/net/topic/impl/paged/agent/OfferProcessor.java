/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.model.Page;

import com.tangosol.io.AbstractEvolvable;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.EvolvablePortableObject;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LongArray;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.IOException;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * An {@link AbstractProcessor} that offers one or more
 * elements to the tail page of a topic.
 *
 * @author jk 2015.05.16
 * @since Coherence 14.1.1
 */
public class OfferProcessor
        extends AbstractPagedTopicProcessor<Page.Key, Page, OfferProcessor.Result>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public OfferProcessor()
        {
        super(PagedTopicPartition::ensureTopic);
        }

    /**
     * Create a {@link OfferProcessor} that will offer the specified elements
     * to a page.
     *
     * @param listElements     the elements to offer to the page
     * @param nNotifyPostFull  the key on which to notify once the topic is no longer full, or zero for none
     * @param fSealPage        flag indicating whether the page should be sealed
     *                         if this offer is successful
     */
    public OfferProcessor(List<Binary> listElements, int nNotifyPostFull, boolean fSealPage)
        {
        this(listElements, nNotifyPostFull, fSealPage, PagedTopicPartition::ensureTopic);
        }

    /**
     * Create a {@link OfferProcessor} that will offer the specified
     * elements to a page with the expected version.
     *
     * @param listElements     the elements to offer to the page
     * @param nNotifyPostFull  the key on which to notify once the topic is no longer full, or zero for none
     * @param fSealPage        flag indicating whether the page should be sealed
     *                         if this offer is successful
     * @param supplier         the {@link Function} to use to provide a
     *                         {@link PagedTopicPartition} instance
     */
    protected OfferProcessor(List<Binary> listElements, int nNotifyPostFull, boolean fSealPage,
                             Function<BinaryEntry<Page.Key, Page>, PagedTopicPartition> supplier)
        {
        super(supplier);

        m_listValues      = listElements;
        m_nNotifyPostFull = nNotifyPostFull;
        m_fSealPage       = fSealPage;
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Obtain the {@link List} of elements to be offered.
     *
     * @return the {@link List} of elements to be offered
     */
    public List<Binary> getElements()
        {
        return m_listValues;
        }

    /**
     * Set the {@link List} of values to be offered.
     *
     * @param listValues  the {@link List} of values to be offered
     */
    public void setValues(List<Binary> listValues)
        {
        m_listValues = listValues;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Result process(InvocableMap.Entry<Page.Key, Page> entry)
        {
        // Note: the PollProcessor and OfferProcessor both target the page, this ensures
        // that they cannot run concurrently on the same page.  Simply sharing the same association does not ensure
        // this.
        return ensureTopic(entry).offerToPageTail((BinaryEntry<Page.Key, Page>) entry, this);
        }

    // ----- accessors ------------------------------------------------------

    public List<Binary> getListValues()
        {
        return m_listValues;
        }

    public int getNotifyPostFull()
        {
        return m_nNotifyPostFull;
        }

    public boolean isSealPage()
        {
        return m_fSealPage;
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
        m_listValues      = in.readCollection(0, new LinkedList<>());
        m_nNotifyPostFull = in.readInt(1);
        m_fSealPage       = in.readBoolean(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeCollection(0, m_listValues, Binary.class);
        out.writeInt(1, m_nNotifyPostFull);
        out.writeBoolean(2, m_fSealPage);
        }

    // ----- inner class: Result --------------------------------------------

    /**
     * This returned as the result of invoking an offer to a topic.
     *
     * @author jk 2015.05.16
     * @since Coherence 14.1.1
     */
    public static class Result
            extends AbstractEvolvable
            implements EvolvablePortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization
         */
        public Result()
            {
            }

        /**
         * Create a {@link Result}.
         *
         * @param status     the status of the offer invocation
         * @param cAccepted  the number of elements successfully offered.
         * @param cbFree     the pages remaining capacity
         * @param nOffset    the offset of the first accepted element
         */
        public Result(Status status, int cAccepted, int cbFree, int nOffset)
            {
            this(status, cAccepted, cbFree, null, nOffset);
            }

        /**
         * Create a {@link Result}.
         *
         * @param status     the status of the offer invocation
         * @param cAccepted  the number of elements successfully offered.
         * @param cbFree     the pages remaining capacity
         * @param aErrors    any errors that occurred adding elements
         * @param nOffset    the offset of the first accepted element
         */
        public Result(Status status, int cAccepted, int cbFree, LongArray<Throwable> aErrors, int nOffset)
            {
            m_status     = status;
            m_cAccepted  = cAccepted;
            m_cbPageFree = cbFree;
            m_aErrors    = aErrors;
            m_nOffset    = nOffset;
            }

        // ----- QueueOfferResult methods -----------------------------------

        /**
         * Obtain the number of elements accepted into the page.
         *
         * @return the number of elements accepted into the page
         */
        public int getAcceptedCount()
            {
            return m_cAccepted;
            }

        /**
         * Obtain the status of the offer invocation.
         *
         * @return the status of the offer invocation
         */
        public Status getStatus()
            {
            return m_status;
            }

        /**
         * Obtain the maximum page capacity.
         * This value can be used to limit the size of the next
         * offer to the queue.
         *
         * @return the maximum page capacity
         */
        public int getPageCapacity()
            {
            return m_cbPageFree;
            }

        /**
         * Obtain any errors which occurred as part of the offer.
         *
         * @return the LongArray of exceptions
         */
        public LongArray<Throwable> getErrors()
            {
            return m_aErrors;
            }

        /**
         * Returns the offset of the first accepted element or {@code -1}
         * if no elements were accepted.
         *
         * @return the offset of the first accepted elementor {@code -1}
         *         if no elements were accepted
         */
        public int getOffset()
            {
            return m_nOffset;
            }

        // ----- EvolvablePortableObject interface --------------------------

       @Override
        public int getImplVersion()
            {
            return DATA_VERSION;
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_status     = Status.valueOf(in.readString(0));
            m_cAccepted  = in.readInt(1);
            m_cbPageFree = in.readInt(2);
            m_aErrors    = in.readObject(3);
            if (getImplVersion() >= 2)
                {
                m_nOffset = in.readInt(4);
                }
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_status.name());
            out.writeInt(1, m_cAccepted);
            out.writeInt(2, m_cbPageFree);
            out.writeObject(3, m_aErrors);
            out.writeInt(4, m_nOffset);
            }

        @Override
        public String toString()
            {
            return "Result(" +
                    " status=" + m_status +
                    ", offset=" + m_nOffset +
                    ", accepted=" + m_cAccepted +
                    ", pageFree=" + m_cbPageFree +
                    ", errors=" + m_aErrors +
                    ')';
            }

        // ----- inner class: Status ----------------------------------------

        /**
         * An enum representing different status values
         * that a {@link Result} can have.
         */
        public enum Status
            {
            /**
             * The offer invocation was successful and all elements were
             * accepted into the page.
             */
            Success,

            /**
             * The offer invocation was unsuccessful as the page was sealed.
             * The offer may have been partially successful if multiple elements
             * had been offered.
             */
            PageSealed,

            /**
             * The offer invocation was unsuccessful as the topic was full.
             * The offer may have been partially successful if multiple elements
             * had been offered.
             */
            TopicFull
            }

        // ----- constants --------------------------------------------------

        /**
         * {@link EvolvablePortableObject} data version of this class.
         */
        public static final int DATA_VERSION = 2;

        // ----- data members -----------------------------------------------

        /**
         * The status of the offer invocation this {@link Result} represents.
         */
        protected Status m_status;

        /**
         * The number of elements successfully offered.
         */
        protected int m_cAccepted;

        /**
         * The remaining capacity in the page if an offer was successful
         * or the {@link Page} maximum capacity if the
         * offer should be retired on the next page.
         */
        protected int m_cbPageFree;

        /**
         * An array of errors for specific elements.
         * This array will hold the error for a specific element
         * if that element was rejected.
         */
        protected LongArray<Throwable> m_aErrors;

        /**
         * The offset of the first accepted element.
         */
        protected int m_nOffset;
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 2;

    // ----- data members ---------------------------------------------------

    /**
     * The elements to offer to the topic. The elements have already been serialized
     * to Binary values.
     */
    protected List<Binary> m_listValues;

    /**
     * The post full notifier.
     */
    protected int m_nNotifyPostFull;

    /**
     * A flag indicating that after the offer is processed the page should be sealed
     * to further offers regardless of the result of this offer.
     * <p>
     * This is primarily used by clients that know they have more than a pages worth of
     * elements to send and hence know that the page will be filled by this request.
     */
    protected boolean m_fSealPage;
    }

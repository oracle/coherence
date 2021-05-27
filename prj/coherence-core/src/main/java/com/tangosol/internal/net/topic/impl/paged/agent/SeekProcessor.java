/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.AbstractEvolvable;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.topic.Position;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import java.io.IOException;

import java.util.Comparator;
import java.util.function.Function;

/**
 * An entry processor that moves a {@link Subscription} to a new position.
 *
 * @author Jonathan Knight 2021.04.27
 * @since 21.06
 */
public class SeekProcessor
        extends AbstractPagedTopicProcessor<Subscription.Key, Subscription, SeekProcessor.Result>
        implements EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public SeekProcessor()
        {
        this(PagedTopicPartition::ensureTopic, null, 0L);
        }

    /**
     * Create a {@link SeekProcessor} to seek to the specified position.
     *
     * @param position       the position to move to
     * @param nSubscriberId  the identifier of the subscriber
     */
    public SeekProcessor(PagedPosition position, long nSubscriberId)
        {
        this(PagedTopicPartition::ensureTopic, position, nSubscriberId);
        }

    /**
     * Create a {@link SeekProcessor} to seek to the specified position.
     *
     * @param supplier       the supplier to provide a {@link PagedTopicPartition} from a {@link BinaryEntry}
     * @param position       the position to move to
     * @param nSubscriberId  the identifier of the subscriber performing the seek
     */
    @SuppressWarnings("rawtypes")
    SeekProcessor(Function<BinaryEntry, PagedTopicPartition> supplier, PagedPosition position, long nSubscriberId)
        {
        super(supplier);
        m_position      = position;
        m_nSubscriberId = nSubscriberId;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Result process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        return ensureTopic(entry).seekPosition((BinaryEntry<Subscription.Key, Subscription>) entry,
                                               m_position, m_nSubscriberId);
        }

    @Override
    public int getImplVersion()
        {
        return DATA_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nSubscriberId = in.readLong(0);
        m_position      = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_nSubscriberId);
        out.writeObject(1, m_position);
        }

    // ----- inner class: Result --------------------------------------------

    /**
     * The result of a seek request.
     */
    public static class Result
            extends AbstractEvolvable
            implements EvolvablePortableObject, Comparable<Result>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        public Result()
            {
            }

        /**
         * Create a {@link Result}.
         *
         * @param positionHead  the new head {@link Position} for the subscriber
         * @param positionSeek  the new head {@link Position} actually seeked to
         */
        public Result(PagedPosition positionHead, PagedPosition positionSeek)
            {
            m_positionHead = positionHead;
            m_positionSeek = positionSeek;
            }

        // ----- accessors --------------------------------------------------

        public PagedPosition getHead()
            {
            return m_positionHead;
            }

        public PagedPosition getSeekPosition()
            {
            return m_positionSeek;
            }

        // ----- Comparable methods -----------------------------------------

        @Override
        public int compareTo(Result o)
            {
            int n = COMPARATOR.compare(m_positionHead, o.m_positionHead);
            return n == 0 ? COMPARATOR.compare(m_positionSeek, o.m_positionSeek) : n;
            }

        // ----- EvolvablePortableObject methods ----------------------------

        @Override
        public int getImplVersion()
            {
            return RESULT_DATA_VERSION;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_positionHead = in.readObject(0);
            m_positionSeek = in.readObject(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_positionHead);
            out.writeObject(1, m_positionSeek);
            }

        // ----- constants ------------------------------------------------------

        /**
         * {@link EvolvablePortableObject} data version of this {@link Result} class.
         */
        public static final int RESULT_DATA_VERSION = 1;

        /**
         * A comparator to safely order {@link Result} instance by {@link Position}.
         */
        private static final Comparator<PagedPosition> COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

        // ----- data members ---------------------------------------------------

        /**
         * The new head position of the subscriber.
         */
        private PagedPosition m_positionHead;

        /**
         * The position actually seeked to.
         */
        private PagedPosition m_positionSeek;
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The identifier of the subscriber performing the seek.
     */
    private long m_nSubscriberId;

    /**
     * The position to seek to.
     */
    private PagedPosition m_position;
    }

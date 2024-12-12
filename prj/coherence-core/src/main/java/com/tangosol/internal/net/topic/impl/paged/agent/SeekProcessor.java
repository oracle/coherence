/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.SeekResult;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.AbstractEvolvable;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.topic.Position;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
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
        extends AbstractPagedTopicProcessor<Subscription.Key, Subscription, SeekResult>
        implements EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public SeekProcessor()
        {
        this(PagedTopicPartition::ensureTopic, null, SubscriberId.NullSubscriber);
        }

    /**
     * Create a {@link SeekProcessor} to seek to the specified position.
     *
     * @param position      the position to move to
     * @param subscriberId  the identifier of the subscriber
     */
    public SeekProcessor(PagedPosition position, SubscriberId subscriberId)
        {
        this(PagedTopicPartition::ensureTopic, position, subscriberId);
        }

    /**
     * Create a {@link SeekProcessor} to seek to the specified position.
     *
     * @param supplier      the supplier to provide a {@link PagedTopicPartition} from a {@link BinaryEntry}
     * @param position      the position to move to
     * @param subscriberId  the identifier of the subscriber performing the seek
     */
    SeekProcessor(Function<BinaryEntry<Subscription.Key, Subscription>, PagedTopicPartition> supplier, PagedPosition position, SubscriberId subscriberId)
        {
        super(supplier);
        m_position      = position;
        m_subscriberId = subscriberId;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public SeekResult process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        return ensureTopic(entry).seekPosition((BinaryEntry<Subscription.Key, Subscription>) entry,
                                               m_position, m_subscriberId);
        }

    @Override
    public int getImplVersion()
        {
        return DATA_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        int  nVersion = in.getVersionId();
        long nId      = in.readLong(0);
        m_position    = in.readObject(1);
        if (nVersion >= 2)
            {
            m_subscriberId = in.readObject(2);
            }
        else
            {
            m_subscriberId = new SubscriberId(nId, null);
            }
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_subscriberId.getId());
        out.writeObject(1, m_position);
        out.writeObject(2, m_subscriberId);
        }

    // ----- inner class: Result --------------------------------------------

    /**
     * The result of a seek request.
     */
    public static class Result
            extends AbstractEvolvable
            implements SeekResult, EvolvablePortableObject, ExternalizableLite
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
        public Result(Position positionHead, Position positionSeek)
            {
            m_positionHead = positionHead;
            m_positionSeek = positionSeek;
            }

        // ----- accessors --------------------------------------------------

        @Override
        public Position getHead()
            {
            return m_positionHead;
            }

        @Override
        public Position getSeekPosition()
            {
            return m_positionSeek;
            }

        // ----- Comparable methods -----------------------------------------

        @Override
        public int compareTo(SeekResult o)
            {
            int n = COMPARATOR.compare(m_positionHead, o.getHead());
            return n == 0 ? COMPARATOR.compare(m_positionSeek, o.getSeekPosition()) : n;
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

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_positionHead = ExternalizableHelper.readObject(in);
            m_positionSeek = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_positionHead);
            ExternalizableHelper.writeObject(out, m_positionSeek);
            }

        // ----- constants ------------------------------------------------------

        /**
         * {@link EvolvablePortableObject} data version of this {@link Result} class.
         */
        public static final int RESULT_DATA_VERSION = 1;

        /**
         * A comparator to safely order {@link Result} instance by {@link Position}.
         */
        private static final Comparator<Position> COMPARATOR = Comparator.nullsLast(Comparator.naturalOrder());

        // ----- data members ---------------------------------------------------

        /**
         * The new head position of the subscriber.
         */
        private Position m_positionHead;

        /**
         * The position actually seeked to.
         */
        private Position m_positionSeek;
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 2;

    // ----- data members ---------------------------------------------------

    /**
     * The identifier of the subscriber performing the seek.
     */
    private SubscriberId m_subscriberId;

    /**
     * The position to seek to.
     */
    private PagedPosition m_position;
    }

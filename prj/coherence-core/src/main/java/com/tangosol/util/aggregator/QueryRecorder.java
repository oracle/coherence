/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.QueryRecord;
import com.tangosol.util.SimpleQueryRecord;
import com.tangosol.util.Streamer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;

/**
 * This aggregator is used to produce a {@link QueryRecord} object that
 * contains an estimated or actual cost of the query execution for a given filter.
 * <p>
 * For example, the following code will print a <i>QueryRecord</i>,
 * containing the estimated query cost and corresponding execution steps.
 * <pre>
 *   QueryRecorder agent  = new QueryRecorder(RecordType.EXPLAIN);
 *   QueryRecord   record = (QueryRecord) cache.aggregate(filter, agent);
 *   System.out.println(record);
 * </pre>
 *
 * @param <K>  the type of the Map entry keys
 * @param <V>  the type of the Map entry values
 *
 * @author tb 2011.05.26
 */
public class  QueryRecorder<K, V>
        implements InvocableMap.StreamingAggregator<K, V, QueryRecord.PartialResult, QueryRecord>,
                   ExternalizableLite, PortableObject
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public QueryRecorder()
        {
        }

    /**
     * Construct a QueryRecorder.
     *
     * @param type  the type for this aggregator
     */
    public QueryRecorder(RecordType type)
        {
        m_type = type;
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Get the record type for this query recorder.
     *
     * @return the record type enum
     */
    public RecordType getType()
        {
        return m_type;
        }


    // ----- InvocableMap.EntryAggregator interface -------------------------

    /**
     * {@inheritDoc}
     */
    public QueryRecord aggregate(Set<? extends InvocableMap.Entry<? extends K, ? extends V>> setEntries)
        {
        throw new UnsupportedOperationException(
                "QueryRecorder cannot be used by this service.");
        }


    // ----- InvocableMap.StreamingAggregator interface ---------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, QueryRecord.PartialResult, QueryRecord> supply()
        {
        return this;
        }

    @Override
    public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
        {
        throw new UnsupportedOperationException(
                "QueryRecorder cannot be used by this service.");
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        throw new UnsupportedOperationException(
                "QueryRecorder cannot be used by this service.");
        }

    @Override
    public boolean combine(QueryRecord.PartialResult partialResult)
        {
        m_results.add(partialResult);
        return true;
        }

    @Override
    public QueryRecord.PartialResult getPartialResult()
        {
        return null;
        }

    @Override
    public QueryRecord finalizeResult()
        {
        return new SimpleQueryRecord(m_type, m_results);
        }

    @Override
    public int characteristics()
        {
        return PARALLEL;
        }

// ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_type = RecordType.fromInt(in.readInt());
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeInt(m_type.toInt());
        }


    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_type = RecordType.fromInt(in.readInt(0));
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_type.toInt());
        }


    // ----- RecordType enum ------------------------------------------------

    /**
     * RecordType enum specifies whether the {@link QueryRecorder} should be
     * used to produce a {@link QueryRecord} object that
     * contains an estimated or an actual cost of the query execution.
     */
    public enum RecordType
        {
        /**
         * Produce a {@link QueryRecord} object that contains an estimated cost
         * of the query execution.
         */
        EXPLAIN,

        /**
         * Produce a {@link QueryRecord} object that contains the actual cost of
         * the query execution.
         */
        TRACE;

        /**
         * Convert an RecordType to an integer.
         *
         * @return the integer
         */
        public int toInt()
            {
            return ordinal();
            }

        /**
         * Convert an integer value to an RecordType
         *
         * @param nOrdinal  the ordinal value of an RecordType
         *
         * @return the RecordType
         */
        public static RecordType fromInt(int nOrdinal)
            {
            return RecordType.class.getEnumConstants()[nOrdinal];
            }
        }


    // ----- data fields ----------------------------------------------------

    /**
     * This aggregator record type.
     */
    @JsonbProperty("type")
    private RecordType m_type;

    /**
     * The list to accumulate results into
     */
    private transient List<QueryRecord.PartialResult> m_results = new ArrayList<>();
    }

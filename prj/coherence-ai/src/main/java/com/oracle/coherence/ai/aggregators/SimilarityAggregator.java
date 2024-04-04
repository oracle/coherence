/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.aggregators;

import com.oracle.coherence.ai.Converters;
import com.oracle.coherence.ai.VectorOp;

import com.oracle.coherence.ai.internal.BinaryVector;
import com.oracle.coherence.ai.results.BinaryQueryResult;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import jakarta.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryAggregator} to execute a
 * similarity query.
 */
public class SimilarityAggregator<KeyType>
        implements InvocableMap.StreamingAggregator<KeyType, BinaryVector, SortedSet<BinaryQueryResult>, List<BinaryQueryResult>>,
                ExternalizableLite, PortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public SimilarityAggregator()
        {
        }

    /**
     * Create a {@link SimilarityAggregator}.
     *
     * @param operation        the {@link VectorOp} to execute
     * @param maxResults       the maximum number of results to return
     * @param naturalOrder     {@code true} to sort results in natural order
     * @param includeVector    {@code true} to include vectors in the returned results
     * @param includeMetadata  {@code true} to include metadata in the returned results
     */
    public SimilarityAggregator(VectorOp<Float> operation, int maxResults, boolean naturalOrder,
                                boolean includeVector, boolean includeMetadata)
        {
        m_operation       = operation;
        m_maxResults      = maxResults;
        m_naturalOrder    = naturalOrder;
        m_includeVector   = includeVector;
        m_includeMetadata = includeMetadata;
        m_results         = naturalOrder ? new TreeSet<>() : new TreeSet<>(Comparator.reverseOrder());
        }

    @Override
    public InvocableMap.StreamingAggregator<KeyType, BinaryVector, SortedSet<BinaryQueryResult>, List<BinaryQueryResult>> supply()
        {
        return new SimilarityAggregator<>(m_operation, m_maxResults, m_naturalOrder, m_includeVector, m_includeMetadata);
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends KeyType, ? extends BinaryVector> entry)
        {
        BinaryEntry<?, ?> binaryEntry     = entry.asBinaryEntry();
        Binary            binaryKey       = binaryEntry.getBinaryKey();
        Binary            binaryValue     = binaryEntry.getBinaryValue();
        ReadBuffer        bufVector       = Converters.extractVector(binaryValue);
        ReadBuffer        binMetadata     = Converters.extractMetadata(binaryValue);
        ReadBuffer        bufResultVector = m_includeVector ? bufVector : null;
        Float             similarity      = m_operation.apply(bufVector);
        BinaryQueryResult result          = new BinaryQueryResult(similarity, binaryKey, bufResultVector, binMetadata);

        m_results.add(result);
        if (m_results.size() > m_maxResults)
            {
            m_results.removeLast();
            }
        return true;
        }

    @Override
    public boolean combine(SortedSet<BinaryQueryResult> partialResult)
        {
        Iterator<BinaryQueryResult> it   = partialResult.iterator();
        int                         size = m_results.size();

        while (size < m_maxResults && it.hasNext())
            {
            m_results.add(it.next());
            size++;
            }

        while (it.hasNext())
            {
            m_results.add(it.next());
            m_results.removeLast();
            }

        return true;
        }

    @Override
    public SortedSet<BinaryQueryResult> getPartialResult()
        {
        return m_results;
        }

    @Override
    public List<BinaryQueryResult> finalizeResult()
        {
        return new ArrayList<>(m_results);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_maxResults   = in.readInt(0);
        m_operation    = in.readObject(1);
        m_naturalOrder = in.readBoolean(2);
        m_results      = m_naturalOrder ? new TreeSet<>() : new TreeSet<>(Comparator.reverseOrder());
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_maxResults);
        out.writeObject(1, m_operation);
        out.writeBoolean(2, m_naturalOrder);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_maxResults   = in.readInt();
        m_operation    = ExternalizableHelper.readObject(in);
        m_naturalOrder = in.readBoolean();
        m_results      = m_naturalOrder ? new TreeSet<>() : new TreeSet<>(Comparator.reverseOrder());
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(m_maxResults);
        ExternalizableHelper.writeObject(out, m_operation);
        out.writeBoolean(m_naturalOrder);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The maximum number of results to return.
     */
    @JsonbProperty("maxResults")
    private int m_maxResults;

    private transient SortedSet<BinaryQueryResult> m_results;

    /**
     * The {@link VectorOp} to execute.
     */
    @JsonbProperty("operation")
    private VectorOp<Float> m_operation;

    /**
     * A flag to determine whether results are sorted in natural
     * order or in reverse order.
     */
    @JsonbProperty("naturalOrder")
    private boolean m_naturalOrder;


    /**
     * {@code true} if the result should contain the corresponding vector.
     */
    @JsonbProperty("includeVector")
    private boolean m_includeVector;

    /**
     * {@code true} if the result should contain the corresponding vector metadata.
     */
    @JsonbProperty("includeMetadata")
    private boolean m_includeMetadata;
    }

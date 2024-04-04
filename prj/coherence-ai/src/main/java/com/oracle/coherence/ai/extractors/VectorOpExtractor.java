/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.extractors;

import com.oracle.coherence.ai.VectorOp;

import com.tangosol.util.Binary;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapTrigger;

import com.tangosol.util.extractor.EntryExtractor;

import jakarta.json.bind.annotation.JsonbProperty;

import java.util.Map;
import java.util.Objects;

/**
 * A {@link com.tangosol.util.ValueExtractor} that performs a
 * {@link VectorOp} on a vector and returns the result.
 *
 * @param <R>  the type of vector this extractor applies to
 */
@SuppressWarnings("rawtypes")
public class VectorOpExtractor<R>
        extends EntryExtractor
    {
    /**
     * Default constructor for serialization.
     */
    public VectorOpExtractor()
        {
        }

    /**
     * Create a {@link VectorOpExtractor}.
     *
     * @param op  the {@link VectorOp} to execute to extract values
     */
    public VectorOpExtractor(VectorOp<R> op)
        {
        this.m_op = op;
        }

    @Override
    public Object extract(Object oTarget)
        {
        return extractFromBinary((Binary) oTarget);
        }

    @Override
    public Object extractFromEntry(Map.Entry entry)
        {
        if (entry instanceof InvocableMap.Entry<?,?>)
            {
            return extract(((InvocableMap.Entry<?,?>) entry).asBinaryEntry().getBinaryValue());
            }
        Binary binary = (Binary) entry.getValue();
        return extractFromBinary(binary);
        }

    @Override
    public Object extractOriginalFromEntry(MapTrigger.Entry entry)
        {
        return extractFromBinary(entry.asBinaryEntry().getBinaryValue());
        }

    /**
     * Apply the {@link VectorOp} to the vector contained in the
     * specified {@link Binary}.
     *
     * @param binary  the {@link Binary} containing the vector data
     *
     * @return  the result of applying the operation to the vector
     */
    protected Object extractFromBinary(Binary binary)
        {
        return m_op.apply(binary);
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        VectorOpExtractor<?> that = (VectorOpExtractor<?>) o;
        return Objects.equals(m_op, that.m_op);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(super.hashCode(), m_op);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link VectorOp} to use to extract values from a vector.
     */
    @JsonbProperty("operation")
    private VectorOp<R> m_op;
    }

/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Predicate;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A {@code java.util.function.Predicate} based {@link ExtractorFilter}.
 *
 * @author as  2014.08.25
 * @author bo  2015.05.27
 *
 * @since 12.2.1
 */
public class PredicateFilter<T, E>
        extends ExtractorFilter<T, E>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public PredicateFilter()
        {
        }

    /**
     * Constructs a {@link PredicateFilter}.
     *
     * @param predicate  predicate for testing the value
     */
    public PredicateFilter(Predicate<? super E> predicate)
        {
        super(ValueExtractor.identityCast());

        m_predicate = predicate;
        }

    /**
     * Constructs a {@link PredicateFilter}.
     *
     * @param extractor  the {@link ValueExtractor} for extracting a value
     * @param predicate  predicate for testing the extracted value
     *
     */
    public PredicateFilter(ValueExtractor<? super T, ? extends E> extractor, Predicate<? super E> predicate)
        {
        super(extractor);

        m_predicate = predicate;
        }

    /**
     * Constructs a {@link PredicateFilter}.
     *
     * @param sMethodName  the method to extract a value for testing
     * @param predicate    predicate for testing the extracted value
     */
    public PredicateFilter(String sMethodName, Predicate<? super E> predicate)
        {
        super(sMethodName);

        m_predicate = predicate;
        }

    // ---- ExtractorFilter implementation ----------------------------------

    @Override
    protected boolean evaluateExtracted(E extracted)
        {
        return m_predicate.test(extracted);
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_predicate = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        ExternalizableHelper.writeObject(out, m_predicate);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        super.readExternal(reader);

        m_predicate = reader.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        super.writeExternal(writer);

        writer.writeObject(1, m_predicate);
        }

    // ---- data members ----------------------------------------------------

    /**
     * The {@link Predicate} for filtering extracted values.
     */
    @JsonbProperty("predicate")
    protected Predicate<? super E> m_predicate;
    }

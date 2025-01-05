/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.extractor;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ValueExtractor;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collection Extractor is used to extract values from Collections using the provided {@link ValueExtractor}.
 * Important Note:
 *
 * <ul>
 *     <li>If the {@link ValueExtractor} is null, an {@link IllegalStateException} is raised.
 *     <li>If the provided {@link Collection} is null or empty, an empty {@link List} is returned.
 * </ul>
 *
 * @author Gunnar Hillert 2024.08.28
 * @param <T> – the type of the value to extract from
 * @param <E> - the type of value that will be extracted
 */
public class CollectionExtractor<T, E>
        extends AbstractExtractor<Collection<T>, List<E>>
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the {@link ExternalizableLite} interface).
     */
    public CollectionExtractor()
        {
        }

    /**
     * Construct a CollectionExtractor based on the specified {@link ValueExtractor}.
     *
     * @param extractor  the ValueExtractor
     */
    public CollectionExtractor(ValueExtractor<T, E> extractor)
        {
        if (extractor == null)
            {
            throw new IllegalArgumentException("The CollectionExtractor requires a ValueExtractor to be specified");
            }
        m_extractor = extractor;
        }

    /**
     * Construct a CollectionExtractor based on the specified {@link ValueExtractor}.
     *
     * @param extractor  the ValueExtractor
     * @param nTarget    one of the {@link #VALUE} or {@link #KEY} values
     */
    @JsonbCreator
    public CollectionExtractor(@JsonbProperty("extractor")
                               ValueExtractor<T, E> extractor,
                               @JsonbProperty("target")
                               int nTarget)
        {
        this(extractor);

        azzert(nTarget == VALUE || nTarget == KEY, String.format(
                "nTarget must be either %s or %s", VALUE, KEY));
        m_nTarget = nTarget;
        }

    // ----- CollectionExtractor methods ---------------------------------------

    /**
     * Extract the value from the passed {@link Collection} using the underlying extractor.
     * If the {@link ValueExtractor} is null, an {@link IllegalStateException} is raised.
     * If the provided {@link Collection} is null or empty, an empty {@link List} is returned.
     */
    @Override
    public List<E> extract(Collection<T> target)
        {
        if (m_extractor == null)
            {
            throw new IllegalStateException("The CollectionExtractor requires a ValueExtractor to be specified");
            }
        List<E> results = new ArrayList<>();
        if (target == null || target.isEmpty())
            {
            return results;
            }

        for (T res : target)
            {
            results.add(m_extractor.extract(res));
            }
        return results;
        }

    @Override
    public ValueExtractor<Collection<T>, List<E>> fromKey()
        {
        return new CollectionExtractor<T, E>(m_extractor, KEY);
        }

// ----- CanonicallyNamed interface -------------------------------------

    /**
     * Compute a canonical name.
     *
     * @return canonical name.
     */
    @Override
    public String getCanonicalName()
        {
        if (m_sNameCanon == null)
            {
            m_sNameCanon = m_extractor.getCanonicalName();
            }
        return m_sNameCanon;
        }

    // ----- ExternalizableLite interface -----------------------------------



    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_extractor = readObject(in);
        m_nTarget   = readInt(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_extractor);
        writeInt(out, m_nTarget);
        }


    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_extractor = in.readObject(0);
        m_nTarget   = in.readInt(1);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_extractor);
        out.writeInt(1, m_nTarget);
        }

    // ----- data fields ----------------------------------------------------

    /**
     * The underlying ValueExtractor.
     */
    @JsonbProperty("extractor")
    protected ValueExtractor<T, E> m_extractor;

    }

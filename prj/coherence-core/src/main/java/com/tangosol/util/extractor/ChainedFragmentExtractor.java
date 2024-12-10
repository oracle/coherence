/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.extractor;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Fragment;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A {@code ValueExtractor} that extracts a {@link Fragment} from a
 * nested property of the target object.
 *
 * @param <T> the type of the target object to extract from
 *
 * @author Aleks Seovic  2021.02.22
 * @since 21.06
 */
public class ChainedFragmentExtractor<T, E>
        extends AbstractExtractor<T, Fragment<E>>
        implements ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public ChainedFragmentExtractor()
        {
        }

    /**
     * Construct {@code ChainedFragmentExtractor} instance.
     *
     * @param from         an extractor for the nested property to extract the fragment from
     * @param aExtractors  an array of extractors to pass to {@link FragmentExtractor}
     */
    public ChainedFragmentExtractor(ValueExtractor<? super T, ? extends E> from, ValueExtractor<? super E, ?>... aExtractors)
        {
        m_from = ValueExtractor.of(from);
        m_fragmentExtractor = new FragmentExtractor<>(aExtractors);
        }

    // ---- ValueExtractor interface ----------------------------------------

    @Override
    public Fragment<E> extract(T target)
        {
        E value = m_from.extract(target);
        return m_fragmentExtractor.extract(value);
        }

    @Override
    public String getCanonicalName()
        {
        return m_from.getCanonicalName();
        }

    // ---- ExternalizableLite interface ------------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        m_from = readObject(in);
        m_fragmentExtractor = readObject(in);
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        writeObject(out, m_from);
        writeObject(out, m_fragmentExtractor);
        }

    // ---- PortableObject interface ----------------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        m_from = in.readObject(0);
        m_fragmentExtractor = in.readObject(1);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_from);
        out.writeObject(1, m_fragmentExtractor);
        }

    // ---- data members ----------------------------------------------------

    /**
     * An extractor for the nested property to extract the fragment from.
     */
    private ValueExtractor<? super T, ? extends E> m_from;

    /**
     * A fragment extractor.
     */
    private FragmentExtractor<E> m_fragmentExtractor;
    }

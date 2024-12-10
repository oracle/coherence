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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link ValueExtractor} that is used to extract a {@link Fragment} from
 * an object.
 *
 * @param <T>  the type of the object to extract a fragment from
 *
 * @author Aleks Seovic  2021.02.22
 * @since 21.06
 */
public class FragmentExtractor<T>
        extends AbstractExtractor<T, Fragment<T>>
        implements ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public FragmentExtractor()
        {
        }

    /**
     * Construct {@code FragmentExtractor} instance.
     *
     * @param aExtractors  an array of extractors to use
     */
    public FragmentExtractor(ValueExtractor<? super T, ?>[] aExtractors)
        {
        m_lstExtractors = Stream.of(aExtractors)
                .map(ValueExtractor::of)
                .collect(Collectors.toList());
        }

    // ---- ValueExtractor interface ----------------------------------------

    @Override
    public Fragment<T> extract(T target)
        {
        int nPos = 0;
        Map<String, Object> mapAttr = new HashMap<>();
        for (ValueExtractor<? super T, ?> extractor : m_lstExtractors)
            {
            String sName = extractor.getCanonicalName();
            if (sName == null)
                {
                sName = "$" + nPos;
                nPos++;
                }
            mapAttr.put(sName, extractor.extract(target));
            }
        return new Fragment<>(mapAttr);
        }

    // ---- ExternalizableLite interface ------------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        m_lstExtractors = new ArrayList<>();
        readCollection(in, m_lstExtractors, null);
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        writeCollection(out, m_lstExtractors);
        }

    // ---- PortableObject interface ----------------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        m_lstExtractors = new ArrayList<>();
        in.readCollection(0, m_lstExtractors);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeCollection(0, m_lstExtractors);
        }

    // ---- data members ----------------------------------------------------

    /**
     * A list of extractors to use.
     */
    private List<ValueExtractor<? super T, ?>> m_lstExtractors;
    }

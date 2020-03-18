/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.comparator;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Comparator;

import javax.json.bind.annotation.JsonbProperty;

import static com.tangosol.util.Base.azzert;

/**
 * Comparator implementation that uses specified {@link ValueExtractor} to extract value(s) to be used for comparison.
 *
 * @author as  2010.09.09
 */
public class ExtractorComparator<T>
        implements Comparator<T>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor (for PortableObject).
     */
    public ExtractorComparator()
        {
        }

    /**
     * Construct a ExtractorComparator with the specified extractor.
     *
     * @param extractor  the ValueExtractor to use by this filter
     */
    public <E extends Comparable<? super E>> ExtractorComparator(ValueExtractor<? super T, ? extends E> extractor)
        {
        azzert(extractor != null);
        m_extractor = Lambdas.ensureRemotable(extractor);
        }

    // ---- Comparator implementation ---------------------------------------

    /**
    * Compares extracted values (by specified <tt>ValueExtractor</tt>) of given arguments for order.
    *
    * @param o1  the first object to be compared
    * @param o2  the second object to be compared
    *
    * @return a negative integer, zero, or a positive integer as the first
    * 	       argument is less than, equal to, or greater than the second
    *
    * @throws ClassCastException if the arguments' types prevent them from being compared by this Comparator.
    */
    @Override
    public int compare(T o1, T o2)
        {
        Comparable a1 = o1 instanceof InvocableMap.Entry
                        ? ((InvocableMap.Entry<?, ?>) o1).extract(m_extractor)
                        : m_extractor.extract(o1);
        Comparable a2 = o2 instanceof InvocableMap.Entry
                        ? ((InvocableMap.Entry<?, ?>) o2).extract(m_extractor)
                        : m_extractor.extract(o2);

        if (a1 == null)
            {
            return a2 == null ? 0 : -1;
            }

        if (a2 == null)
            {
            return +1;
            }

        return a1.compareTo(a2);
        }

    // ---- ExternalizableLite implementation -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_extractor = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_extractor);
        }


    // ---- PortableObject implementation -----------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_extractor = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_extractor);
        }

    // ---- accessors --------------------------------------------------------

    /**
     * Returns the {@link ValueExtractor} to extract value(s) to be used in comparison.
     *
     * @return the {@link ValueExtractor} to extract value(s) to be used in comparison
     */
    public ValueExtractor<? super T, ? extends Comparable> getExtractor()
        {
        return m_extractor;
        }

    // --- data members -----------------------------------------------------

    /**
     * <tt>ValueExtractor</tt> to extract value(s) to be used in comparison
     */
    @JsonbProperty("extractor")
    private ValueExtractor<? super T, ? extends Comparable> m_extractor;
    }

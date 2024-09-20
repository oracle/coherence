/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.tangosol.coherence.dslquery.UniversalExtractorBuilder;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.ChainedComparator;
import com.tangosol.util.comparator.ExtractorComparator;
import com.tangosol.util.comparator.InverseComparator;
import com.tangosol.util.comparator.SafeComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.tangosol.util.extractor.AbstractExtractor.VALUE;

/**
 * Comparator builder that provides a small set of builder methods to
 * simplify creation of a chain of {@link ExtractorComparator}s from given
 * {@link ValueExtractor}s.
 *
 * @see ExtractorComparator
 * @see ValueExtractor
 *
 * @author ic  2011.06.30
 */
public class ComparatorBuilder
    {

    // ----- public API -----------------------------------------------------

    /**
     * Add comparator to this builder.
     *
     * @param sExpr  expression used to create an expression extractor
     *
     * @return this ComparatorBuilder
     */
    public ComparatorBuilder asc(String sExpr)
        {
        return asc(createExtractor(sExpr));
        }

    /**
     * Add comparator to this builder.
     *
     * @param extractor  extractor used to extract values
     *
     * @return this ComparatorBuilder
     */
    public ComparatorBuilder asc(ValueExtractor extractor)
        {
        m_listComparators.add(createComparator(extractor));
        return this;
        }

    /**
     * Add comparator to builder.
     *
     * @param sExpr  expression used to create expression extractor
     *
     * @return this ComparatorBuilder
     */
    public ComparatorBuilder desc(String sExpr)
        {
        return desc(createExtractor(sExpr));
        }

    /**
     * Add comparator to this builder.
     *
     * @param extractor  extractor used to extract values
     *
     * @return this ComparatorBuilder
     */
    public ComparatorBuilder desc(ValueExtractor extractor)
        {
        m_listComparators.add(inverse(createComparator(extractor)));
        return this;
        }

    /**
     * Build comparator.
     *
     * @return comparator representing current state of builder
     */
    public Comparator build()
        {
        Comparator[] array = new Comparator[m_listComparators.size()];
        return new ChainedComparator(m_listComparators.toArray(array));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Append specified comparator to the end of list of comparators owned by
     * this builder.
     *
     * @param comparator  comparator to add
     */
    protected void add(Comparator comparator)
        {
        m_listComparators.add(comparator);
        }

    /**
     * Reverse ordering of a given comparator.
     *
     * @param comparator  comparator to be reversed
     *
     * @return a reversed comparator
     */
    protected Comparator inverse(Comparator comparator)
        {
        return new InverseComparator(comparator);
        }

    /**
     * Create extractor comparator.
     *
     * @param sExpr  expression used to create expression extractor
     *
     * @return comparator that will evaluate given expression when comparing
     *         values
     */
    protected Comparator createComparator(String sExpr)
        {
        return createComparator(createExtractor(sExpr));
        }

    /**
     * Create extractor comparator.
     *
     * @param extractor  extractor used while comparing values
     *
     * @return comparator that will use given extractor when comparing values
     */
    protected Comparator createComparator(ValueExtractor extractor)
        {
        return safe(new ExtractorComparator(extractor));
        }

    /**
     * Wrap given comparator with a {@link SafeComparator}.
     *
     * @param comparator  to be wrapped
     *
     * @return <tt>SafeComparator</tt> wrapping given comparator
     */
    protected Comparator safe(Comparator comparator)
        {
        return new SafeComparator(comparator);
        }

    /**
     * Create expression extractor.
     *
     * @param sExpr  expression to create extractor for
     *
     * @return extractor for the specified expression
     */
    protected ValueExtractor createExtractor(String sExpr)
        {
        return new UniversalExtractorBuilder().realize("", VALUE, sExpr);
        }

    // ----- data members ---------------------------------------------------

    /**
     * List of comparators owned by this builder.
     */
    private final List<Comparator> m_listComparators = new ArrayList<Comparator>();
    }

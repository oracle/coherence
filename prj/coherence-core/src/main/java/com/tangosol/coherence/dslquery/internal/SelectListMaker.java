/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExtractorBuilder;

import com.tangosol.coherence.dslquery.operator.BaseOperator;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.DistinctValues;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.ReducerAggregator;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.MultiExtractor;

import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.ExtractorProcessor;

import java.util.List;

/**
 * SelectListMaker is a visitor class that converts a given Abstract Syntax
 * Tree into implementation Objects for a select query. The implementation
 * Objects are typically subclasses of InvocableMap.EntryAggregator.
 * The SelectListMaker can also be used to make ValueExtractors.
 *
 * @author djl  2009.08.31
 */
public class SelectListMaker
        extends AbstractCoherenceQueryWalker
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new SelectListMaker using given array for indexed
     * Bind vars and the given Map for named bind variables.
     *
     * @param indexedBindVars  the indexed bind variables
     * @param namedBindVars    the named bind variables
     * @param language         the CoherenceQueryLanguage instance to use
     */
    public SelectListMaker(List indexedBindVars, ParameterResolver namedBindVars,
                           CoherenceQueryLanguage language)
        {
        super(indexedBindVars, namedBindVars, language);
        }

    // ----- SelectListMaker API --------------------------------------------

    /**
     * Test to see if the receiver had nodes that are call which would mean
     * the result is an aggregation
     *
     * @return   the results of the test
     */
    public boolean hasCalls()
        {
        return m_nCallCount > 0;
        }

    /**
     * Test to see if the results of processing resulted in an aggregation.
     *
     * @return   the results of the test
     */
    public boolean isAggregation()
        {
        return m_nCallCount == m_aResults.length;
        }

    /**
     * Get the resultant Object[] from the tree processing
     *
     * @return   the results of processing
     */
    public Object[] getResults()
        {
        return m_aResults;
        }

    /**
     * Turn the results of tree processing into a ValueExtractor
     *
     * @return   the resulting ValueExtractor
     */
    public ValueExtractor getResultsAsValueExtractor()
        {
        if (hasCalls() || m_aResults.length == 0)
            {
            return null;
            }
        else if (m_aResults.length == 1)
            {
            return (ValueExtractor) m_aResults[0];
            }
        else
            {
            ValueExtractor[] extractors = new ValueExtractor[m_aResults.length];

            for (int i = 0; i < m_aResults.length; i++)
                {
                extractors[i] = (ValueExtractor) m_aResults[i];
                }

            return new MultiExtractor(extractors);
            }
        }

    /**
     * Turn the results of tree processing into a DistinctValues aggregator
     *
     * @return   the resulting DistinctValues
     */
    public DistinctValues getDistinctValues()
        {
        if (hasCalls())
            {
            return null;
            }

        return new DistinctValues(getResultsAsValueExtractor());
        }

    /**
     * Turn the results of tree processing into an InvocableMap.EntryProcessor
     * that will return the results of a query
     *
     * @return   the resulting EntryProcessor
     */
    public InvocableMap.EntryProcessor getResultsAsEntryProcessor()
        {
        if (hasCalls())
            {
            return null;
            }

        if (m_aResults.length == 1)
            {
            return new ExtractorProcessor((ValueExtractor) m_aResults[0]);
            }

        InvocableMap.EntryProcessor[] processors = new InvocableMap.EntryProcessor[m_aResults.length];

        for (int i = 0; i < m_aResults.length; i++)
            {
            processors[i] = new ExtractorProcessor((ValueExtractor) m_aResults[i]);
            }

        return new CompositeProcessor(processors);
        }

    /**
     * Turn the results of tree processing into an InvocableMap.EntryAggregator
     * that will return the results of a query.
     *
     * @return   the resulting EntryAggregator
     */
    public InvocableMap.EntryAggregator getResultsAsReduction()
        {
        int nIdentifierCount = m_aResults.length;

        if (hasCalls())
            {
            return null;
            }

        if (nIdentifierCount == 1)
            {
            return new ReducerAggregator((ValueExtractor) m_aResults[0]);
            }

        ValueExtractor[] aExtractors = new ValueExtractor[nIdentifierCount];

        for (int i = 0; i < nIdentifierCount; ++i)
            {
            aExtractors[i] = (ValueExtractor) m_aResults[i];
            }

        return new ReducerAggregator(new MultiExtractor(aExtractors));
        }

    /**
     * Turn the results of tree processing into an
     * InvocableMap.EntryAggregator that will perform the aggregation.
     *
     * @return   the resulting EntryAggregator
     */
    public InvocableMap.EntryAggregator getResultsAsEntryAggregator()
        {
        int nIdentifierCount = m_aResults.length - m_nCallCount;

        if (!hasCalls())
            {
            return null;
            }

        if (m_aResults.length == 1)
            {
            return (InvocableMap.EntryAggregator) m_aResults[0];
            }

        if (m_aResults.length == m_nCallCount)
            {
            InvocableMap.EntryAggregator[] aAggregators = new InvocableMap.EntryAggregator[m_aResults.length];

            for (int i = 0; i < m_aResults.length; i++)
                {
                aAggregators[i] = (InvocableMap.EntryAggregator) m_aResults[i];
                }

            return CompositeAggregator.createInstance(aAggregators);
            }

        ValueExtractor[]             aExtractors = new ValueExtractor[nIdentifierCount];
        InvocableMap.EntryAggregator aggregator;

        for (int i = 0; i < nIdentifierCount; ++i)
            {
            aExtractors[i] = (ValueExtractor) m_aResults[i];
            }

        if (m_nCallCount == 1)
            {
            aggregator = (InvocableMap.EntryAggregator) m_aResults[nIdentifierCount];
            }
        else
            {
            InvocableMap.EntryAggregator[] aAggregators = new InvocableMap.EntryAggregator[m_nCallCount];

            for (int i = 0; i < m_nCallCount; i++)
                {
                aAggregators[i] = (InvocableMap.EntryAggregator) m_aResults[nIdentifierCount + i];
                }

            aggregator = CompositeAggregator.createInstance(aAggregators);
            }

        if (nIdentifierCount == 1)
            {
            return GroupAggregator.createInstance(aExtractors[0], aggregator);
            }

        return GroupAggregator.createInstance(new MultiExtractor(aExtractors), aggregator);
        }

    /**
     * Process the AST Tree using the given Term
     *
     * @param sCacheName  the name of the cache to make select list for
     * @param term        the AST to walk to make the select list
     *
     * @return the resulting Object[] of results
     */
    public Object[] makeSelectsForCache(String sCacheName, NodeTerm term)
        {
        m_sCacheName = sCacheName;
        m_term       = term;
        m_aResults   = new Object[m_term.length()];

        int nCount = m_term.length();

        setResult(null);

        for (int i = 1; i <= nCount; ++i)
            {
            m_term.termAt(i).accept(this);
            m_aResults[i - 1] = getResult();
            }

        return m_aResults;
        }

    // ----- AbstractCoherenceQueryWalker API ---------------------------------------

    /**
     * The receiver has classified a binary operation node
     *
     * @param sOperator  the String representing the operator
     * @param termLeft   the left Term of the operation
     * @param termRight  the left Term of the operation
     */
    @Override
    protected void acceptBinaryOperator(String sOperator, Term termLeft, Term termRight)
        {
        BaseOperator operator = f_language.getOperator(sOperator);

        setResult(operator.makeExtractor(termLeft, termRight, this));
        }

    /**
     * The receiver has classified a call node
     *
     * @param sFunctionName  the function name
     * @param term           a Term whose children are the parameters to the call
     */
    @Override
    protected void acceptCall(String sFunctionName, NodeTerm term)
        {
        super.acceptCall(sFunctionName, term);

        Object oResult = getResult();

        if (oResult instanceof InvocableMap.EntryAggregator)
            {
            m_nCallCount++;
            }
        }

    @Override
    protected void acceptIdentifier(String sIdentifier)
        {
        if (acceptIdentifierInternal(sIdentifier))
            {
            return;
            }

        ExtractorBuilder builder = f_language.getExtractorBuilder();

        setResult(builder.realize(m_sCacheName, AbstractExtractor.VALUE, sIdentifier));
        }

    @Override
    protected void acceptPath(NodeTerm term)
        {
        acceptPathAsChainedExtractor(m_sCacheName, term);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the cache that the select list is being built for
     */
    protected String m_sCacheName;

    /**
     * The Term that is the AST that encodes select list
     */
    protected NodeTerm m_term;

    /**
     * The count of classifications that are calls
     * tree of Objects is built
     */
    protected int m_nCallCount = 0;

    /**
     * The results of the classification
     */
    protected Object[] m_aResults;
    }

/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.SimpleParameterList;

import com.tangosol.coherence.dslquery.internal.AbstractCoherenceQueryWalker;
import com.tangosol.coherence.dslquery.internal.ConstructorQueryWalker;

import com.tangosol.coherence.dslquery.operator.BaseOperator;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.NeverFilter;
import com.tangosol.util.filter.NotFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * FilterBuilder is a visitor class that converts a given Abstract Syntax
 * Tree into a Filter.  The Filter can be a deep nesting of Filters.
 *
 * @author djl  2009.08.31
 * @author jk   2013.12.02
 */
public class FilterBuilder
        extends AbstractCoherenceQueryWalker
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new FilterBuilder.
     */
    public FilterBuilder()
        {
        this(Collections.emptyList(), new NullParameterResolver(), new CoherenceQueryLanguage());
        }

    /**
     * Construct a new FilterBuilder.
     *
     * @param language  the {@link CoherenceQueryLanguage} instance to use
     */
    public FilterBuilder(CoherenceQueryLanguage language)
        {
        this(Collections.emptyList(), new NullParameterResolver(), language);
        }

    /**
     * Construct a new FilterBuilder with given binding environment.
     *
     * @param aoBindVars  an Object array of indexed bind variables
     */
    public FilterBuilder(Object[] aoBindVars)
        {
        this(Arrays.asList(aoBindVars), new NullParameterResolver(), new CoherenceQueryLanguage());
        }

    /**
     * Construct a new FilterBuilder that can construct a Filter from the
     * given Term.
     *
     * @param term  the Term to use in the construction of a filter
     */
    public FilterBuilder(Term term)
        {
        this();
        m_term = term;
        }

    /**
     * Construct a new FilterBuilder with given binding environment.
     *
     * @param aoBindVars        indexed bind variables
     * @param mapNamedBindVars  named bind variables
     */
    public FilterBuilder(Object[] aoBindVars, Map mapNamedBindVars)
        {
        this(Arrays.asList(aoBindVars),
             new ResolvableParameterList(newParameterListFromMap(mapNamedBindVars)),
             new CoherenceQueryLanguage());
        }

    /**
     * Construct a new FilterBuilder with given default bind variables.
     *
     * @param indexedBindVars  the indexed bind variables
     * @param namedBindVars    the named bind variables
     * @param language         the {@link CoherenceQueryLanguage} instance to use
     */
    public FilterBuilder(List indexedBindVars,
                         ParameterResolver namedBindVars,
                         CoherenceQueryLanguage language)
        {
        super(indexedBindVars, namedBindVars, language);
        f_listDefaultBindVars  = m_listBindVars;
        f_defaultNamedBindVars = m_namedBindVars;
        }

    // ----- Filter construction API ----------------------------------------

    /**
     * Make a new Filter from the set AST.
     *
     * @return the constructed Filter
     */
    public Filter makeFilter()
        {
        return makeFilter(m_term, f_listDefaultBindVars, f_defaultNamedBindVars);
        }

    /**
     * Make a new Filter from the given AST.
     *
     * @param term  the AST to turn into a Filter
     *
     * @return the constructed Filter
     */
    public Filter makeFilter(Term term)
        {
        return makeFilter(term, f_listDefaultBindVars, f_defaultNamedBindVars);
        }

    /**
     * Make a new Filter from the given AST using given array for Bind vars.
     *
     * @param term               the AST to turn into a Filter
     * @param aoIndexedBindVars  the array of Objects to use for Bind vars
     *
     * @return the constructed Filter
     */
    public Filter makeFilter(Term term, Object[] aoIndexedBindVars)
        {
        return makeFilter(term, Arrays.asList(aoIndexedBindVars), f_defaultNamedBindVars);
        }

    /**
     * Make a new Filter from the given AST using the given bind variables.
     *
     * @param term               the AST to turn into a Filter
     * @param aoIndexedBindVars  the array of Objects to use for bind variables
     * @param mapNamedBindVars   the named bind variables to use
     *
     * @return the constructed Filter
     */
    public Filter makeFilter(Term term, Object[] aoIndexedBindVars, Map mapNamedBindVars)
        {
        return makeFilter(term, Arrays.asList(aoIndexedBindVars),
                          new ResolvableParameterList((mapNamedBindVars)));
        }

    /**
     * Make a new Filter from the given AST using the given bind variables.
     *
     * @param term           the AST to turn into a Filter
     * @param listBindVars   the indexed bind variables
     * @param namedBindVars  the named bind variables
     *
     * @return the constructed Filter
     */
    public Filter makeFilter(Term term, List listBindVars, ParameterResolver namedBindVars)
        {
        return makeFilterForCache(null, term, listBindVars, namedBindVars);
        }

    /**
     * Make a new Filter from the given AST using given array for Bind vars.
     *
     * @param sCacheName       the name of the cache the Filter is to query
     * @param term             the AST to turn into a Filter
     * @param indexedBindVars  the indexed bind variables to use
     * @param namedBindVars    the named bind variables to use
     *
     * @return the constructed Filter
     */
    public Filter makeFilterForCache(String sCacheName, Term term, List indexedBindVars,
                                     ParameterResolver namedBindVars)
        {
        m_sCacheName    = sCacheName;
        m_term          = term;
        m_listBindVars  = indexedBindVars == null
                            ? f_listDefaultBindVars
                            : indexedBindVars;
        m_namedBindVars = namedBindVars == null
                            ? f_defaultNamedBindVars
                            : namedBindVars;

        setResult(null);

        m_term.accept(this);

        Object oResult = getResult();
        if (oResult instanceof Filter)
            {
            return (Filter) oResult;
            }

        if (oResult instanceof Boolean)
            {
            return ((Boolean) oResult).booleanValue()
                               ? AlwaysFilter.INSTANCE
                               : NeverFilter.INSTANCE;
            }

        throw new RuntimeException("Filter not specified. " + oResult + " Found instead!");
        }

    /**
     * Process the AST Tree using the given Term that represents getter.
     *
     * @param term  the AST used
     *
     * @return the resulting ValueExtractor
     */
    public ValueExtractor makeExtractor(NodeTerm term)
        {
        m_term = term;

        setResult(null);
        term.accept(this);

        return (ValueExtractor) getResult();
        }

    // ----- DefaultCoherenceQueryWalker API ---------------------------------------

    @Override
    protected void acceptList(NodeTerm termList)
        {
        int      cTerms = termList.length();
        Object[] ao     = new Object[cTerms];

        for (int i = 1; i <= cTerms; i++)
            {
            termList.termAt(i).accept(this);
            ao[i - 1] = getResult();
            }

        setResult(ao);
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

    /**
     * This method will take a Binary Operator and the left and right {@link Term}s
     * for the operator and result in the creation of a {@link Filter} or a {@link ValueExtractor}.
     */
    @Override
    protected void acceptBinaryOperator(String sOperator, Term termLeft, Term termRight)
        {
        BaseOperator operator = f_language.getOperator(sOperator);

        if (operator == null)
            {
            throw new RuntimeException("Cannot build filter from unknown operator " + sOperator);
            }

        if (operator.isConditional())
            {
            setResult(operator.makeFilter(termLeft, termRight, this));
            }
        else
            {
            setResult(operator.makeExtractor(termLeft, termRight, this));
            }
        }

    @Override
    protected void acceptUnaryOperator(String sOperator, Term t)
        {
        switch (sOperator)
            {
            case "new" :
                ConstructorQueryWalker walker = new ConstructorQueryWalker(m_listBindVars, m_namedBindVars, f_language);

                t.accept(walker);
                setResult(reflectiveMakeObject(true, walker.getResult()));
                break;

            case "!" :
                t.accept(this);
                NotFilter filter = new NotFilter((Filter) getResult());
                setResult(filter);
                break;

            case "-" :
                t.accept(this);

                AtomicTerm atomicTerm = m_atomicTerm;
                if (atomicTerm.isNumber())
                    {
                    Number number = atomicTerm.negativeNumber((Number) getResult());
                    setResult(number);
                    }
                break;

            case "+" :
                t.accept(this);
                break;

            default :
                setResult(null);
                break;
            }
        }

    @Override
    protected void acceptPath(NodeTerm term)
        {
        acceptPathAsChainedExtractor(m_sCacheName, term);
        }


    // ----- static helpers -------------------------------------------------

    /**
     * Convert the given {@link Map} into a {@link ParameterList}.
     * The key of the map is used as the parameter name and the
     * corresponding value as the parameter value.
     *
     * @param map  the Map to convert to a ParameterList
     *
     * @return a ParameterList made up of the contents of the specified Map
     */
    protected static ParameterList newParameterListFromMap(Map<?,?> map)
        {
        SimpleParameterList list = new SimpleParameterList();

        for (Map.Entry entry : map.entrySet())
            {
            list.add(new Parameter(String.valueOf(entry.getKey()), entry.getValue()));
            }

        return list;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Term that is the AST that encodes the Filter to be made.
     */
    protected Term m_term;

    /**
     * The cache name this FilterBuilder is building {@link Filter}s to query
     * against.
     * <p>
     * This is used by the {@link CoherenceQueryLanguage language's} {@link
     * ExtractorBuilder} to map attribute names to POF indices. A null cache
     * name results in the use of {@link com.tangosol.util.extractor.UniversalExtractor UniversalExtractor}s.
     */
    protected String m_sCacheName;

    /**
     * The default indexed bind variables.
     */
    protected final List f_listDefaultBindVars;

    /**
     * The default named bind variables.
     */
    protected final ParameterResolver f_defaultNamedBindVars;
    }

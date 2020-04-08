/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.TermWalker;

import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ValueExtractor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * A base class for CohQL Operator implementations.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public abstract class BaseOperator<F extends Filter>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an instance of a BaseOperator with the specified
     * symbol, conditional flag and aliases.
     *
     * @param sSymbol       the symbol for this operator
     * @param fConditional  a flag indicating whether this operator is conditional
     * @param asAlias      an optional list of aliases for this operator
     */
    protected BaseOperator(String sSymbol, boolean fConditional, String... asAlias)
        {
        f_sSymbol      = sSymbol;
        f_fConditional = fConditional;
        f_asAlias      = asAlias;
        }

    // ----- BaseOperator methods -------------------------------------------

    /**
     * Return the symbol to use in CohQL that represents this operator.
     *
     * @return the symbol to use in CohQL that represents this operator
     */
    public String getSymbol()
        {
        return f_sSymbol;
        }

    /**
     * Return the alternative symbols to use in CohQL that represent this operator.
     *
     * @return the alternative symbols to use in CohQL that represent this operator
     */
    public String[] getAliases()
        {
        return f_asAlias;
        }

    /**
     * Create a {@link Filter} for this {@link BaseOperator} using the
     * specified left and right {@link Term}s.
     * <p>
     * Note: This method should be thread safe as operators are stored
     * in a static map so may be called by multiple threads.
     *
     * @param termLeft   the left term to use to build a Filter
     * @param termRight  the right term to use to build a Filter
     * @param walker     the {@link TermWalker} to use to process the left and
     *                   right terms
     *
     * @return a Filter representing this operation.
     */
    public F makeFilter(Term termLeft, Term termRight, TermWalker walker)
        {
        Object oLeft  = walker.walk(termLeft);
        Object oRight = walker.walk(termRight);

        return makeFilter(oLeft, oRight);
        }

    /**
     * Create a {@link Filter} for this {@link BaseOperator} using the
     * specified left and right values.
     * <p>
     * Note: This method should be thread safe as operators are stored
     * in a static map so may be called by multiple threads.
     *
     * @param oLeft   the left value to use to build a Filter
     * @param oRight  the right value to use to build a Filter
     *
     * @return a Filter representing this operation
     */
    public F makeFilter(Object oLeft, Object oRight)
        {
        throw new UnsupportedOperationException("Unsupported binary operator (" + getSymbol() + ")");
        }

    /**
     * Create a {@link ValueExtractor} for this {@link BaseOperator} using the
     * specified left and right {@link Term}s.
     * <p>
     * Note: This method should be thread safe as operators are stored
     * in a static map so may be called by multiple threads.
     *
     * @param termLeft   the left term to use to build a ValueExtractor
     * @param termRight  the right term to use to build a ValueExtractor
     * @param walker     the {@link TermWalker} to use to process the left and
     *                   right terms
     *
     * @return a ValueExtractor representing this operation
     */
    public ValueExtractor makeExtractor(Term termLeft, Term termRight, TermWalker walker)
        {
        Object oLeft  = walker.walk(termLeft);
        Object oRight = walker.walk(termRight);

        return makeExtractor(oLeft, oRight);
        }

    /**
     * Create a {@link ValueExtractor} for this {@link BaseOperator} using the
     * specified left and right values.
     * <p>
     * Note: This method should be thread safe as operators are stored
     * in a static map so may be called by multiple threads.
     *
     * @param oLeft   the left value to use to build a ValueExtractor
     * @param oRight  the right value to use to build a ValueExtractor
     *
     * @return a ValueExtractor representing this operation
     */
    public ValueExtractor makeExtractor(Object oLeft, Object oRight)
        {
        throw new UnsupportedOperationException("Unsupported binary operator (" + getSymbol() + ")");
        }

    /**
     * Return true if this operator can be used as a conditional operator.
     *
     * @return true if this operator can be used as a conditional operator
     */
    public boolean isConditional()
        {
        return f_fConditional;
        }

    /**
     * Add this operator to the given {@link TokenTable}.
     * This typically means adding this operator using its
     * symbol and also adding any aliases.
     *
     * @param tokenTable  the TokenTable to add this operator to
     */
    public abstract void addToTokenTable(TokenTable tokenTable);

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "BaseOperator(symbol=" + f_sSymbol + ", aliases=" + Arrays.toString(f_asAlias) + ", conditional="
               + f_fConditional + ')';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Add any aliases of this operator to the specified token table.
     *
     * @param tokenTable  the token table to add aliases to
     */
    protected void addAliases(TokenTable tokenTable)
        {
        String   sSymbol = getSymbol();
        String[] asAlias = getAliases();

        for (String sAlias : asAlias)
            {
            tokenTable.alias(sAlias, sSymbol);
            }
        }

    /**
     * Return an immutable Set accounting for the provided object being an array,
     * a Collection or a single item in the returned Set.
     *
     * @param oValue  either an object array, a collection or a single item to
     *                be returned as a Set
     *
     * @return a Set contained the provided object
     */
    protected static Set unmodifiableSet(Object oValue)
        {
        if (oValue instanceof Set)
            {
            return (Set) oValue;
            }

        Set set;

        if (oValue instanceof Object[])
            {
            set = new ImmutableArrayList((Object[]) oValue).getSet();
            }
        else if (oValue instanceof Collection)
            {
            set = new ImmutableArrayList((Collection) oValue).getSet();
            }
        else
            {
            set = Collections.singleton(oValue);
            }

        return set;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The symbol for this operator.
     */
    protected final String f_sSymbol;

    /**
     * An array of optional aliases for this operator.
     */
    protected final String[] f_asAlias;

    /**
     * Flag indicating whether this operator can be used as a conditional operator,
     * for example ==, &gt;=, etc, as opposed to a non-conditional operator such as +, -, etc.
     */
    protected final boolean f_fConditional;
    }

/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.TermWalker;

import com.tangosol.util.filter.ComparisonFilter;

/**
 * A base class for comparison operators, which are operators
 * that are used in conditional clauses such as equals, greater than,
 * less than, etc.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public abstract class ComparisonOperator
        extends BaseOperator<ComparisonFilter>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a comparison operator with the given symbol and aliases.
     *
     * @param sSymbol    the symbol for this operator
     * @param asAliases  any aliases for this operator
     */
    protected ComparisonOperator(String sSymbol, String... asAliases)
        {
        super(sSymbol, true, asAliases);
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public ComparisonFilter makeFilter(Term termLeft, Term termRight, TermWalker walker)
        {
        String sRightFunctor = termRight.getFunctor();

        // Bug 27250717 - RFA: QueryHelper.createFilter causes StackOverFlow when comparing 2 identifiers 
        if (sRightFunctor.equals("identifier") && termLeft.getFunctor().equals("identifier"))
            {
            throw new UnsupportedOperationException("The use of identifier on both sides of an expression is not supported");
            }

        if (sRightFunctor.equals(
                "identifier") &&!(((AtomicTerm) termRight.termAt(1)).getValue().equalsIgnoreCase(
                    "null") || ((AtomicTerm) termRight.termAt(1)).getValue().equalsIgnoreCase(
                    "true") || ((AtomicTerm) termRight.termAt(1)).getValue().equalsIgnoreCase(
                    "false")) || (sRightFunctor.equals("derefNode") || sRightFunctor.equals("callNode")))
            {
            return flip().makeFilter(termRight, termLeft, walker);
            }

        return super.makeFilter(termLeft, termRight, walker);
        }

    // ----- ComparisonOperator API -----------------------------------------

    /**
     * Return the operator to use if this operation needs to
     * be flipped due to the CohQL statement having the literal
     * on the left hand side.
     * For example if the statement was "2 == foo" this would
     * need to be flipped to put the literal on the right so giving
     * the statement "foo == 2" and the flipped operator is still ==.
     * But for another example such as "2 &gt;= foo" flipping this give
     * the statement "foo &lt;= 2" so the operator has changed from &gt;= to &lt;=
     *
     * @return the operator to use if this operation needs to
     * be flipped due to the CohQL statement having the literal
     * on the left hand side.
     */
    public abstract BaseOperator<ComparisonFilter> flip();
    }

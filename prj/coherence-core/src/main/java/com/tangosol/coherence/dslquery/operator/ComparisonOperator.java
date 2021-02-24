/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.TermWalker;

import com.tangosol.util.filter.ComparisonFilter;

import java.util.Arrays;

/**
 * A base class for comparison operators, which are operators
 * that are used in conditional clauses such as equals, greater than,
 * less than, etc.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
@SuppressWarnings("rawtypes")
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
        String  sRightFunctor       = termRight.getFunctor();
        String  sLeftFunctor         = termLeft.getFunctor();
        boolean fLeftIsIdentifier    = "identifier".equals(sLeftFunctor);
        boolean fRightIsIdentifier   = "identifier".equals(sRightFunctor);
        boolean fRightIsBinding      = "bindingNode".equals(sRightFunctor);
        boolean fLeftHasIdentifiers  = Arrays.stream(termLeft.children())
                .anyMatch(t -> "identifier".equals(t.getFunctor()));
        boolean fRightHasIdentifiers = !fRightIsBinding
                && Arrays.stream(termRight.children()).anyMatch(t -> "identifier".equals(t.getFunctor()));

        // Bug 27250717 - RFA: QueryHelper.createFilter causes StackOverFlow when comparing 2 identifiers
        if ((fRightIsIdentifier && fLeftIsIdentifier) || (fLeftHasIdentifiers && fRightHasIdentifiers))
            {
            String sMsg = "The use of identifier on both sides of an expression is not supported";
            throw new UnsupportedOperationException(sMsg);
            }

        if (sRightFunctor.equals(
                "identifier") && !(((AtomicTerm) termRight.termAt(1)).getValue().equalsIgnoreCase(
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

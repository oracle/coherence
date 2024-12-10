/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.coherence.dsltools.precedence.InfixOPToken;
import com.tangosol.coherence.dsltools.precedence.OPToken;
import com.tangosol.coherence.dsltools.precedence.TokenTable;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.TermWalker;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.ComparisonFilter;
import com.tangosol.util.filter.EqualsFilter;

/**
 * An operator implementation representing the equality operator.
 *
 * @author jk 2013.12.03
 * @since Coherence 12.2.1
 */
public class EqualsOperator
        extends ComparisonOperator
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a EqualsOperator.
     */
    protected EqualsOperator()
        {
        super("==", "=", "is");
        }

    // ----- ComparisonOperator methods -------------------------------------

    @Override
    public ComparisonOperator flip()
        {
        return this;
        }

    // ----- BaseOperator methods -------------------------------------------

    @Override
    public ComparisonFilter makeFilter(Term termLeft, Term termRight, TermWalker walker)
        {
        Term termNot = Terms.newTerm(OPToken.UNARY_OPERATOR_NODE, AtomicTerm.createString("!"));

        if (termRight.getFunctor().equals(termNot.getFunctor()) && termNot.headChildrenTermEqual(termRight))
            {
            // The statement was "foo is not x" but the parser thinks the
            // operation is Equals when it should actually be a Not Equals
            return NotEqualsOperator.INSTANCE.makeFilter(termLeft, termRight.termAt(2), walker);
            }

        return super.makeFilter(termLeft, termRight, walker);
        }

    @Override
    public ComparisonFilter makeFilter(Object oLeft, Object oRight)
        {
        return new EqualsFilter((ValueExtractor) oLeft, oRight);
        }

    @Override
    public void addToTokenTable(TokenTable tokenTable)
        {
        tokenTable.addToken(new InfixOPToken(f_sSymbol, OPToken.PRECEDENCE_RELATIONAL, OPToken.BINARY_OPERATOR_NODE));
        addAliases(tokenTable);
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of the EqualsOperator.
     */
    public static final EqualsOperator INSTANCE = new EqualsOperator();
    }

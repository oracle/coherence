/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * A Hamcrest Matcher that can be used in tests
 * to match {@link Term} instances.
 *
 * @author jk  2013.12.05
 */
public class TermMatcher<T extends Term>
        implements Matcher<T>
    {
    public TermMatcher(String ast)
        {
        this(Terms.create(ast));
        }

    public TermMatcher(Term term)
        {
        this.ast = term;
        }

    @Override
    public boolean matches(Object item)
        {
        if (ast == null && item == null)
            {
            return true;
            }
        else if (ast == null)
            {
            return false;
            }

        return (item instanceof Term) && ast.termEqual((Term) item);
        }

    public void describeMismatch(Object item, Description mismatchDescription)
        {
        if (item instanceof Term)
            {
            mismatchDescription.appendText("but found ");
            mismatchDescription.appendValue(item);
            }
        else
            {
            mismatchDescription.appendText("expected Term instance but got ");
            mismatchDescription.appendValue(item);
            }
        }

    @Override
    public void _dont_implement_Matcher___instead_extend_BaseMatcher_()
        {
        }

    @Override
    public void describeTo(Description description)
        {
        description.appendText("<");
        description.appendText(ast.fullFormString());
        description.appendText(">");
        }

    /**
     * Static helper method to create a matcher for a given AST term
     *
     * @param ast the expected AST for the term
     *
     * @return a matcher that will test against the given AST
     */
    public static <T extends Term> Matcher<T> matchingTerm(String ast)
        {
        return new TermMatcher<T>(ast);
        }

    /**
     * Static helper method to create a matcher for a given AST term
     *
     * @param ast the expected AST for the term
     *
     * @return a matcher that will test against the given AST
     */
    public static <T extends Term> Matcher<T> matchingTerm(T ast)
        {
        return new TermMatcher<T>(ast);
        }

    protected Term ast;
    }

/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.coherence.dsltools.termtrees.Term;
import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.progress.ThreadSafeMockingProgress;
import org.mockito.internal.util.Primitives;

import java.io.Serializable;

/**
 * @author jk  2013.12.23
 */
public class TermMockitoMatcher
        implements ArgumentMatcher<Term>, Serializable
    {
    public TermMockitoMatcher(Term wanted)
        {
        this.wanted = wanted;
        }

    public boolean matches(Term actual)
        {
        if (wanted == null)
            {
            return actual == null;
            }

        return (actual instanceof Term) && wanted.termEqual((Term) actual);
        }

    public void describeTo(Description description)
        {
        description.appendText(describe(wanted));
        }

    public String describe(Object object)
        {
        return "" + object;
        }

    @Override
    public boolean equals(Object o)
        {
        if (o == null ||!this.getClass().equals(o.getClass()))
            {
            return false;
            }

        TermMockitoMatcher other = (TermMockitoMatcher) o;

        return this.wanted == null && other.wanted == null || this.wanted != null && this.wanted.equals(other.wanted);
        }

    @Override
    public int hashCode()
        {
        return 1;
        }

    public SelfDescribing withExtraTypeInfo()
        {
        return new SelfDescribing()
            {
            public void describeTo(Description description)
                {
                description.appendText(describe("(" + wanted.getClass().getSimpleName() + ") " + wanted));
                }
            };
        }

    public boolean typeMatches(Object object)
        {
        return wanted != null && object != null && object.getClass() == wanted.getClass();
        }

    public static <T extends Term> T termEquals(T value)
        {
        mockingProgress.getArgumentMatcherStorage().reportMatcher(new TermMockitoMatcher(value));
        return (T) returnFor(value.getClass());
        }

    private static <T> T returnFor(Class<T> clazz)
        {
        return Primitives.isPrimitiveOrWrapper(clazz) ? Primitives.defaultValue(clazz) : null;
        }

    private static MockingProgress mockingProgress = ThreadSafeMockingProgress.mockingProgress();

    private final Term             wanted;
    }

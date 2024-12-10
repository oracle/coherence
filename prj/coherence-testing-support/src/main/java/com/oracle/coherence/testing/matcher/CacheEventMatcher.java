/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.matcher;

import com.oracle.coherence.common.base.Objects;
import com.tangosol.net.cache.CacheEvent;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.List;

public class CacheEventMatcher<T>
        extends BaseMatcher<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a CacheEventMatcher with the provided {@link Matcher}.
     *
     * @param expected  the {@link CacheEvent} to call with the event
     */
    protected CacheEventMatcher(CacheEvent<?, ?> expected)
        {
        m_expected     = expected;
        m_listExpected = null;
        }

    /**
     * Constructs a CacheEventMatcher with the provided {@link Matcher}.
     *
     * @param expected  the {@link CacheEvent} to call with the event
     */
    protected CacheEventMatcher(List<? super CacheEvent<?, ?>> expected)
        {
        m_listExpected = expected;
        m_expected     = null;
        }

    // ----- Matcher methods ------------------------------------------------

    @Override
    public boolean matches(Object item)
        {
        if (m_expected != null)
            {
            if (item instanceof CacheEvent<?,?> event)
                {
                return eventsMatch(m_expected, event);
                }
            return false;
            }
        else
            {
            if (item instanceof List<?> list)
                {
                if (m_listExpected.size() != list.size())
                    {
                    return false;
                    }
                for (int i = 0; i < m_listExpected.size(); i++)
                    {
                    Object o = list.get(i);
                    if (o instanceof CacheEvent<?,?> event)
                        {
                        CacheEvent<?, ?> o1 = (CacheEvent<?, ?>) m_listExpected.get(i);
                        if (!eventsMatch(o1, event))
                            {
                            return false;
                            }
                        }
                    else
                        {
                        return false;
                        }
                    }
                return true;
                }
            return false;
            }
        }

    @Override
    public void describeTo(Description description)
        {
        if (m_listExpected != null)
            {
            description.appendText("is ").appendValue(m_listExpected);
            }
        else
            {
            description.appendText("is ").appendValue(m_expected);
            }
        }

    // ----- helper methods -------------------------------------------------

    protected boolean eventsMatch(CacheEvent<?, ?> actual, CacheEvent<?, ?> expected)
        {
        if (actual == null && expected == null)
            {
            return true;
            }
        if (actual == null || expected == null)
            {
            return false;
            }
        if (actual.getId() != expected.getId())
            {
            return false;
            }
        if (actual.isExpired() != expected.isExpired())
            {
            return false;
            }
        if (actual.isPriming() != expected.isPriming())
            {
            return false;
            }
        if (actual.isSynthetic() != expected.isSynthetic())
            {
            return false;
            }
        if (actual.isVersionUpdate() != expected.isVersionUpdate())
            {
            return false;
            }
        if (!Objects.equals(actual.getKey(), expected.getKey()))
            {
            return false;
            }
        if (!Objects.equals(actual.getOldValue(), expected.getOldValue()))
            {
            return false;
            }
        if (!Objects.equals(actual.getNewValue(), expected.getNewValue()))
            {
            return false;
            }
        if (actual.getTransformationState() != expected.getTransformationState())
            {
            return false;
            }
        return true;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The expected event.
     */
    private final CacheEvent<?, ?> m_expected;

    /**
     * The expected list of events.
     */
    private final List<? super CacheEvent<?, ?>> m_listExpected;
    }

/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.matcher;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * A {@link Matcher} implementation that locates a thread group based on
 * the name provided via {@link #matches(Object)} and passes the active
 * thread count to the provided {@link Matcher}.
 *
 * @param <T>  the type the provided matcher will operate against
 */
public class ThreadGroupMatcher<T>
        extends BaseMatcher<String>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a ThreadGroupMatcher with the provided {@link Matcher}.
     *
     * @param threadCountMatcher  the {@link Matcher} to call with the
     *                            active thread count or null
     */
    protected ThreadGroupMatcher(Matcher<T> threadCountMatcher)
        {
        m_threadCountMatcher = threadCountMatcher;
        }

    // ----- Matcher methods ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(Object item)
        {
        if (!(item instanceof String))
            {
            throw new IllegalArgumentException("ThreadGroupMatcher should be used with " +
                    "a string representing the thread group name");
            }

        ThreadGroup threadGrp = locateThreadGroup((String) item);

        return m_threadCountMatcher.matches(threadGrp == null ? null : threadGrp.activeCount());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void describeTo(Description description)
        {
        description.appendValue("A thread group active count must match: ")
                .appendDescriptionOf(m_threadCountMatcher);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Based on a thread group name find the thread group searching the
     * entire tree.
     *
     * @param sThreadGroupName  the thread group name to find
     *
     * @return the thread group with the provided name or null
     */
    private ThreadGroup locateThreadGroup(String sThreadGroupName)
        {
        ThreadGroup grp = Thread.currentThread().getThreadGroup();

        for (; !sThreadGroupName.equals(grp.getName()) && grp.getParent() != null; grp = grp.getParent())

        if (sThreadGroupName.equals(grp.getName()))
            {
            return grp;
            }

        return findGroup(sThreadGroupName, grp);
        }

    /**
     * Recursively find the thread group looking at all children.
     *
     * @param sThreadGroupName  the thread group name to find
     * @param grp               the thread group to inspect
     *
     * @return the thread group with the provided name or null
     */
    private ThreadGroup findGroup(String sThreadGroupName, ThreadGroup grp)
        {
        ThreadGroup[] aGrps = new ThreadGroup[grp.activeGroupCount()];
        int           c     = grp.enumerate(aGrps);
        for (int i = 0; i < c; ++i)
            {
            ThreadGroup subGrp = aGrps[i];
            if (subGrp != null && sThreadGroupName.equals(subGrp.getName()))
                {
                return subGrp;
                }
            }

        for (int i = 0; i < c; ++i)
            {
            ThreadGroup match = findGroup(sThreadGroupName, aGrps[i]);
            if (match != null)
                {
                return match;
                }
            }
        return null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Matcher} to call with either the active thread count or
     * null.
     */
    private Matcher<T> m_threadCountMatcher;
    }
/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import java.util.Collection;
import java.util.HashSet;

/**
 * Thread related helpers.
 *
 * @author phf 2020.08.27
 */
public class ThreadHelper
    {
    /**
     * Get a collection of threads whose names start with the given prefix.
     *
     * @param sPrefix  the thread name prefix
     *
     * @return the thread
     */
    public static Collection<Thread> getThreadsByPrefix(String sPrefix)
        {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();

        ThreadGroup parentThreadGroup;
        while ((parentThreadGroup = threadGroup.getParent()) != null)
            {
            threadGroup = parentThreadGroup;
            }

        Thread[] aThreads = new Thread[threadGroup.activeCount() + 1];
        threadGroup.enumerate(aThreads);

        Collection<Thread> colThreads = new HashSet<>();
        for (Thread thread : aThreads)
            {
            String sName = thread == null ? null : thread.getName();

            if (sName != null && sName.startsWith(sPrefix))
                {
                colThreads.add(thread);
                }
            }

        return colThreads;
        }
    }

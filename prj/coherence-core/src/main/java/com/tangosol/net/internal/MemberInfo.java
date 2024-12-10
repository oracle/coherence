/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;


import com.tangosol.util.SafeLinkedList;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * MemberInfo holds information related to the routing of asynchronous
 * requests for a given Member.
 *
 * @author gg 11.08.2013
 * @since Coherence 12.1.3
 */
public class MemberInfo
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct the MemberInfo.
     */
    public MemberInfo()
        {
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the atomic counter for outstanding asynchronous requests.
     *
     * @return the atomic counter
     */
    public AtomicInteger getCounter()
        {
        return f_atomicCounter;
        }

    /**
     * Return the list of backlog continuations.
     *
     * @return the list of backlog continuations
     */
    public SafeLinkedList getBacklogContinuations()
        {
        return m_listContinuations;
        }

    /**
     * Ensure the initialization of the list of backlog continuations.
     *
     * @return  the list of backlog continuations (never null)
     */
    public SafeLinkedList ensureBacklogContinuations()
        {
        SafeLinkedList list = m_listContinuations;
        if (list == null)
            {
            synchronized (this)
                {
                list = m_listContinuations;
                if (list == null)
                    {
                    list = m_listContinuations = new SafeLinkedList();
                    }
                }
            }
        return list;
        }


    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        SafeLinkedList list = m_listContinuations;

        return "MemberInfo{Pending requests=" + f_atomicCounter.get() +
               "; Waiting notifications="+ (list == null ? 0 : list.size()) + "}";
        }

    // ----- data fields ----------------------------------------------------

    /**
     * Atomic counter for outstanding asynchronous requests.
     */
    private final AtomicInteger f_atomicCounter = new AtomicInteger();

    /**
     * The list of Continuations that need to be called when the request backlog
     * goes back to normal.
     */
    private volatile SafeLinkedList m_listContinuations;
    }

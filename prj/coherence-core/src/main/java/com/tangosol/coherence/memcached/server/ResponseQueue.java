/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.tangosol.coherence.memcached.server.BinaryConnection.BinaryResponse;

/**
 * ResponseQueue is a specialized single producer multi consumer queue used by
 * the memcached adapter. All the responses that need to be sent to the client
 * are added to this queue and are sent in the same order. The requests could finish
 * in a different order since they are executed as async EPs. But this queue's FIFO order
 * ensures that the responses are sent back in the order of the received requests
 * irrespective of the request completion.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class ResponseQueue
    {
    /**
     * Add a pending BinaryResponse to the queue. It will be sent to the client
     * in the same order when its associated request is completed. The add always
     * happens single threaded on the SelectionService thread serving the client
     * connection.
     *
     * @param response  BinaryResponse.
     */
    public void add(BinaryResponse response)
        {
        BinaryResponse oldTail = m_tail;
        if (oldTail != null)
            {
            oldTail.m_next = response;
            }
        m_tail              = response;
        BinaryResponse head = m_head;
        if (head == null)
            {
            m_head = response;
            if (oldTail != null)
                {
                oldTail.m_next = null;
                }
            }
        else if (head == oldTail)
            {
            synchronized (this) // synch to avoid concurrent update of head by consumers
                {
                if (m_head == null) // consumers didn't see the newly added response
                    {
                    m_head         = response;
                    oldTail.m_next = null;
                    }
                }
            }
        }

    /**
     * Check if the given response can be sent back to the client.
     *
     * @param response  Response that needs to be checked for flushing to client.
     * @param fOwner    Did the current thread complete the associated request for the given response
     *
     * @return true if the response can be flushed to the client.
     */
    public boolean isFlushable(BinaryResponse response, boolean fOwner)
        {
        if (response == null)
            {
            return false;
            }
        else if (fOwner)
            {
            // send response back to client if it is at the head of the queue
            return m_head == response;
            }
        else
            {
            synchronized (response)
                {
                // send response back to client if it is at the head of the queue and is marked deferred.
                if (m_head == response && response.m_fDeferred)
                    {
                    response.m_fDeferred = false;
                    return true;
                    }
                return false;
                }
            }
        }

    /**
     * Mark the given response as deferred since it cannot be sent to the client by the owning thread.
     *
     * @param response  Response to be marked as deferred
     */
    public void markDeferred(BinaryResponse response)
        {
        synchronized (response)
            {
            response.m_fDeferred = true;
            }
        }

    /**
     * Remove the response from the head and return the next response.
     *
     * @param response  Response to be removed if it is the head.
     *
     * @return next response in the queue
     */
    public BinaryResponse removeAndGetNext(BinaryResponse response)
        {
        BinaryResponse nextResponse = response.m_next;
        if (nextResponse == null) // last response in the q.
            {
            synchronized (this) // synch to avoid concurrent update by producer
                {
                nextResponse    = response.m_next; // ensure head points to right response that may have been added concurrently.
                response.m_next = null;
                m_head          = nextResponse;
                }
            }
        else
            {
            response.m_next = null;
            m_head          = nextResponse;
            }
        return nextResponse;
        }

    /**
     * Size of the queue - pending responses.
     *
     * @return size of the queue
     */
    public int size()
        {
        BinaryResponse head = m_head;
        BinaryResponse tail = m_tail;
        if (head != null && tail != null)
            {
            return (int) (m_tail.m_request.m_lId - head.m_request.m_lId + 1);
            }
        return 0;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Head of the queue.
     */
    protected volatile BinaryResponse m_head;

    /**
     * Tail of the queue.
     */
    protected volatile BinaryResponse m_tail;
    }
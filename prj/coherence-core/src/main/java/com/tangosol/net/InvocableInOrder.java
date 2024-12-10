/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


/**
* The InvocableInOrder interface allows to control the ordering in which
* the results of Invocable tasks are returned back to the caller by the
* Invocation service with a thread pool.
* <p>
* Consider the following scenario.  Cluster node A executes two asynchronous
* queries Q1 and Q2 calling {@link InvocationService#execute execute}.
* Both queries are to be executed on a cluster node B; Q2 is called after Q1
* and therefore received by the InvocationService on the node B in that order.
* <p>
* If the Invocation service is configured to use a thread pool of more than
* one thread and the processing time of Q1 is significantly longer then the
* processing time of Q2, then the result of Q2 execution will be returned
* to the caller ahead of the response for Q1.
* <p>
* If a client application semantics require "in order" response guarantees,
* the corresponding Invocable tasks will have to implement InvocableInOrder
* interface and return "true" from isRespondInOrder() method.
*
* @author gg 2003-06-12
*
* @see AbstractInvocable
*
* @since Coherence 2.2
*/
public interface InvocableInOrder
        extends Invocable
    {
    /**
    * Determine whether this Invocable object has to preserve the order of
    * responses according to the order of requests.
    *
    * @return true if the response order must be preserved; false otherwise
    */
    public boolean isRespondInOrder();
    }
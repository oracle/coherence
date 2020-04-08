/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


/**
* The InvocationObserver is an object that asynchronously receives 
* notification of results from the execution of Invocable objects.
*
* @author cp/gg 2003-02-17
*
* @since Coherence 2.1
*/
public interface InvocationObserver
    {
    /**
    * This method is called by the {@link InvocationService} to inform the
    * InvocationObserver that a member has finished running the
    * {@link Invocable} object; the result of the invocation, if any, is
    * provided to the InvocationObserver. The result will be null
    * if no result is registered by the Invocable object, or if it
    * explicitly registers a result whose value is null.
    * 
    * @param member   cluster Member that has finished the execution
    *                 of the Invocable object
    * @param oResult  the result, if any, of the invocation
    */
    public void memberCompleted(Member member, Object oResult);

    /**
    * This method is called by the {@link InvocationService} to inform the
    * InvocationObserver that a member has thrown an exception while
    * running the {@link Invocable} object.
    *
    * @param member    cluster Member that encountered an exception
    *                  while executing the Invocable object
    * @param eFailure  the Throwable object that was encountered
    */
    public void memberFailed(Member member, Throwable eFailure);

    /**
    * This method is called by the {@link InvocationService} to inform the
    * InvocationObserver that a member that the {@link Invocable} object was
    * intended for execution upon has left the service (or the cluster).
    * <p>
    * It cannot be determined whether the member never received the
    * Invocable object, received and began execution of it and left
    * before finishing, or even completed execution of it without
    * managing to report a result.
    *
    * @param member    cluster Member that left the service before
    *                  reporting the completion of the execution of
    *                  the Invocable object
    */
    public void memberLeft(Member member);

    /**
    * This method is called by the {@link InvocationService} to inform the 
    * InvocationObserver that all service members have either finished 
    * the {@link Invocable} object execution or are not (or no longer) running
    * the InvocationService.
    */
    public void invocationCompleted();
    }

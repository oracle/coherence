/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.util.Map;
import java.util.Set;


/**
* The InvocationService is a Service for delivering executable objects to
* cluster members for distributed invocation. The executable objects must
* implement the Invocable interface, which extends the Java Runnable
* interface. Using this interface, application code can cause execution of
* an Invocable object to occur on any one, any set of, or all cluster members
* that are running the InvocationService.
* <p>
* Note: Invocable objects that do not implement the {@link PriorityTask}
* interface will execute without any timeout. Furthermore, the executing
* service or worker thread will not be protected by the service guardian while
* the invocation is in progress.
* <p>
*
* @author cp 2003-01-05
*
* @since Coherence 2.1
*/
public interface InvocationService
        extends Service
    {
    /**
    * Asynchronously invoke the specified task on each of the specified
    * members. This method may return before the task is executed, or before
    * it finishes executing, or after it finishes executing, or any of the
    * above; however, it is expected that aggressive implementations will
    * attempt to return as quickly as possible.
    *
    * @param task        the Invocable object to distribute to the specified
    *                    members in order to be invoked on those members
    * @param setMembers  (optional) a set of cluster members to which the
    *                    Invocable object will be distributed; if null, the
    *                    Invocable object will be distributed to all cluster
    *                    members that are running this service
    * @param observer    (optional) the InvocationObserver object that will
    *                    receive notifications related to the Invocable object
    */
    public void execute(Invocable task, Set setMembers, InvocationObserver observer);

    /**
    * Synchronously invoke the specified task on each of the specified
    * members. This method will not return until the specified members have
    * completed their processing, failed in their processing, or died trying.
    * <p>
    * Members that are specified but are not currently running the
    * InvocationService will not invoke the specified Invocable object.
    * Members that leave (gracefully or otherwise) before the invocation
    * completes will not register a result, and the amount of processing
    * that completed is indeterminate. Members that encounter an exception
    * during invocation will not be retried and no result is returned. 
    * Specifically, the result for a given member will
    * not be present under the following conditions:
    * <ul>
    * <li>if the member did not exist</li>
    * <li>if the member was not running the service at the time that the
    *     query method was invoked</li>
    * <li>if the member left (via the shutdown or stop methods, or
    *     unexpectedly) before responding</li>
    * <li>if the member encountered an exception while processing and had
    *     not registered a non-null result</li>
    * <li>if the member completed successfully but registered no result</li>
    * <li>if the member completed successfully but explicitly registered a
    *     result of null</li>
    * </ul>
    *
    * @param task        the Invocable object to distribute to the specified
    *                    members in order to be invoked on those members
    * @param setMembers  (optional) a set of cluster members to which the
    *                    Invocable object will be distributed; if null, the
    *                    Invocable object will be distributed to all cluster
    *                    members that are running this service
    *
    * @return a Map of result objects keyed by Member object
    */
    public Map query(Invocable task, Set setMembers);


    // ----- constants ------------------------------------------------------

    /**
    * Invocation service type constant.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_DEFAULT = "Invocation";

    /**
    * Remote invocation service type constant.
    *
    * @see Cluster#ensureService(String, String)
    */
    public static final String TYPE_REMOTE  = "RemoteInvocation";
    }

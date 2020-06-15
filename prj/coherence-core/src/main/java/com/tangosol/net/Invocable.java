/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.io.Serializable;


/**
* The Invocable object is a cluster-portable object that can be invoked on
* any set of remote members and each can optionally register a return value
* for the invocation.
* <p>
* When an Invocable object is received for execution, the order of execution
* is:
* <ul>
* <li>the Invocable object is deserialized by the {@link InvocationService};
* <li>the InvocationService provides a reference to itself to the Invocable
*     object by calling the init method;
* <li>the InvocationService invokes the Invocable object by calling the run
*     method;
* <li>if the InvocationService is responsible for returning a value from the
*     invocation, it obtains the value by calling the getResult method.
* </ul>
* Starting with Coherence 3.3 it's possible to control the task scheduling
* priority and timeout by also implementing {@link PriorityTask} interface.
*
* @author cp 2003-01-05
* @since Coherence 2.1
*/
public interface Invocable
        extends Runnable, Serializable
    {
    /**
    * Called by the InvocationService exactly once on this Invocable object
    * as part of its initialization.
    * <p>
    * <b>Note:</b> implementations of the Invocable interface that store the
    * service reference must do so only in a transient field.
    *
    * @param service  the containing InvocationService
    */
    public void init(InvocationService service);

    /**
    * Called exactly once by the InvocationService to invoke this Invocable
    * object.
    */
    public void run();

    /**
    * Determine the result from the invocation of this object. This method
    * is called by the InvocationService after the run method returns.
    *
    * @return the invocation result, if any
    */
    public Object getResult();
    }

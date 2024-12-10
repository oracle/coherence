/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.component;


/**
* This interface is implemented by any class that wishes to be registered
* within an execution context.  There are two reasons to implement this
* interface:
* <ol>
* <li>  To react to registration within a context
* <li>  To react to deregistration from the same
* </ol>
* Most importantly, the deregistration allows the object to clean itself
* up.  One example is a session EJB client "removing" its session beans.
*
* @version 1.00, 2001.04.13
* @author  Cameron Purdy
*/
public interface ExecutionContextAware
    {
    /**
    * Invoked when the object is registered within an execution context.
    *
    * @param ctx  the ExecutionContext
    */
    public void valueBound(ExecutionContext ctx);

    /**
    * Invoked when the object is unregistered by an execution context.
    *
    * @param ctx  the ExecutionContext
    */
    public void valueUnbound(ExecutionContext ctx);
    }

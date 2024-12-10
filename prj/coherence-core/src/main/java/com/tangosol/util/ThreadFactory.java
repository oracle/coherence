/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* Factory interface for Thread creation.
*
* @author mf 2006.09.06
*/
public interface ThreadFactory
        extends java.util.concurrent.ThreadFactory
    {
    /**
    * Create a Thread with the specified group, runnable, and name.
    *
    * @param group     (optional) the thread's thread group
    * @param runnable  (optional) the thread's runnable
    * @param sName     (optional) the thread's name
    *
    * @return a new thread using the specified parameters
    */
    public Thread makeThread(ThreadGroup group, Runnable runnable, String sName);
    }
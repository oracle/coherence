/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.util.Base;

import java.io.Serializable;


/**
* An abstract base for Invocable and PriorityTask implementations.
*
* @author gg 2003.03.31, 2007.03.11
* @since Coherence 2.2
*/
public abstract class AbstractInvocable
        extends    Base
        implements Invocable, InvocableInOrder, PriorityTask, Serializable
    {
    // ----- Invocable interface --------------------------------------------

    /**
    * Called by the InvocationService exactly once on this Invocable object
    * as part of its initialization.
    *
    * @param service the containing InvocationService
    */
    public void init(InvocationService service)
        {
        m_service = service;
        }

    /**
    * {@inheritDoc}
    */
    public Object getResult()
        {
        return m_oResult;
        }


    // ----- InvocableInOrder interface -------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * This implementation returns "false".
    */
    public boolean isRespondInOrder()
        {
        return false;
        }


    // ----- PriorityTask interface -----------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * This implementation returns {@link #SCHEDULE_STANDARD SCHEDULE_STANDARD}.
    */
    public int getSchedulingPriority()
        {
        return SCHEDULE_STANDARD;
        }

    /**
    * {@inheritDoc}
    * <p>
    * This implementation returns {@link #TIMEOUT_DEFAULT TIMEOUT_DEFAULT}.
    */
    public long getExecutionTimeoutMillis()
        {
        return TIMEOUT_DEFAULT;
        }

    /**
    * {@inheritDoc}
    * <p>
    * This implementation is a no-op.
    */
    public void runCanceled(boolean fAbandoned)
        {
        }

    /**
    * {@inheritDoc}
    * <p>
    * This implementation returns {@link #TIMEOUT_DEFAULT TIMEOUT_DEFAULT}.
    */
    public long getRequestTimeoutMillis()
        {
        return TIMEOUT_DEFAULT;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the containing InvocationService.
    *
    * @return the containing InvocationService
    */
    public InvocationService getService()
        {
        return m_service;
        }

    /**
    * Set the result of the invocation.
    *
    * @param oResult  the invocation result
    */
    protected void setResult(Object oResult)
        {
        m_oResult = oResult;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The InvocationService.
    */
    private transient InvocationService m_service;

    /**
    * The result of invocation.
    */
    private Object m_oResult;
    }
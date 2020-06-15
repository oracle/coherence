/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* An abstract base for PriorityTask implementations. It implements all
* PriorityTask interface methods and is intended to be extended for concrete
* uses.
*
* @author gg 2007.03.20
* @since Coherence 3.3
*/
public abstract class AbstractPriorityTask
        extends    ExternalizableHelper
        implements PriorityTask, ExternalizableLite, PortableObject
    {
    // ----- PriorityTask interface -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public int getSchedulingPriority()
        {
        return m_iSchedulingPriority;
        }

    /**
    * {@inheritDoc}
    */
    public long getExecutionTimeoutMillis()
        {
        return m_lExecutionTimeout;
        }

    /**
    * {@inheritDoc}
    */
    public long getRequestTimeoutMillis()
        {
        return m_lRequestTimeout;
        }

    /**
    * {@inheritDoc}
    * <p>
    * This implementation is a no-op.
    */
    public void runCanceled(boolean fAbandoned)
        {
        }


    // ----- mutators ------------------------------------------------------

    /**
    * Specify this task's scheduling priority. Valid values are one of the
    * SCHEDULE_* constants.
    *
    * @param iPriority  this task's scheduling priority
    */
    public void setSchedulingPriority(int iPriority)
        {
        if (iPriority < SCHEDULE_STANDARD
         || iPriority > SCHEDULE_IMMEDIATE)
            {
            throw new IllegalArgumentException("Invalid priority: " + iPriority);
            }
        m_iSchedulingPriority = iPriority;
        }

    /**
    * Specify the maximum amount of time this task is allowed to run before the
    * corresponding service will attempt to stop it.
    *
    * @param lTimeout the task timeout value in milliseconds
    */
    public void setExecutionTimeoutMillis(long lTimeout)
        {
        if (lTimeout < TIMEOUT_NONE)
            {
            throw new IllegalArgumentException("Invalid timeout: " + lTimeout);
            }
        m_lExecutionTimeout = lTimeout;
        }
    /**
    * Specify the maximum amount of time a calling thread is willing to wait for
    * a result of the request execution.
    *
    * @param lTimeout  the request timeout value in milliseconds
    */
    public void setRequestTimeoutMillis(long lTimeout)
        {
        if (lTimeout < TIMEOUT_NONE)
            {
            throw new IllegalArgumentException("Invalid timeout: " + lTimeout);
            }
        m_lRequestTimeout = lTimeout;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_iSchedulingPriority = readInt(in);
        m_lExecutionTimeout   = readLong(in);
        m_lRequestTimeout     = readLong(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeInt(out,  m_iSchedulingPriority);
        writeLong(out, m_lExecutionTimeout);
        writeLong(out, m_lRequestTimeout);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * The AbstractPriorityTask implementation reserves property indexes 0 - 9.
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_iSchedulingPriority = in.readInt(0);
        m_lExecutionTimeout   = in.readLong(1);
        m_lRequestTimeout     = in.readLong(2);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The AbstractPriorityTask implementation reserves property indexes 0 - 9.
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_iSchedulingPriority);
        out.writeLong(1, m_lExecutionTimeout);
        out.writeLong(2, m_lRequestTimeout);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The scheduling priority value.
    */
    @JsonbProperty("schedulingPriority")
    private int m_iSchedulingPriority = SCHEDULE_STANDARD;

    /**
    * The task execution timeout value.
    */
    @JsonbProperty("executionTimeout")
    private long m_lExecutionTimeout = TIMEOUT_DEFAULT;

    /**
    * The request timeout value.
    */
    @JsonbProperty("requestTimeout")
    private long m_lRequestTimeout = TIMEOUT_DEFAULT;
    }
/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.PriorityTask;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author jk 2016.03.29
 */
public class PriorityProcessor
        extends AbstractProcessor
        implements InvocableMap.EntryProcessor, PriorityTask, PortableObject, ExternalizableLite
    {

    public PriorityProcessor()
        {
        }

    public PriorityProcessor(long cMillis, long timeout, String sReturnValue)
        {
        m_cMillis      = cMillis;
        m_timeout      = timeout;
        m_sReturnValue = sReturnValue;
        }

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        try
            {
            long cSteps = m_cMillis / 1000;

            for (long i = 0; i< cSteps && m_fRun; i++)
                {
                Thread.sleep(1000);
                GuardSupport.heartbeat();
                }
            }
        catch (InterruptedException e)
            {
            e.printStackTrace();
            }

        return m_sReturnValue;
        }

    @Override
    public long getExecutionTimeoutMillis()
        {
        return m_timeout;
        }

    @Override
    public long getRequestTimeoutMillis()
        {
        return m_timeout;
        }

    @Override
    public int getSchedulingPriority()
        {
        return SCHEDULE_IMMEDIATE;
        }

    @Override
    public void runCanceled(boolean fAbandoned)
        {
        m_fRun = false;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_timeout      = in.readLong(0);
        m_cMillis      = in.readLong(1);
        m_sReturnValue = in.readString(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_timeout);
        out.writeLong(1, m_cMillis);
        out.writeString(2, m_sReturnValue);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_timeout      = ExternalizableHelper.readLong(in);
        m_cMillis      = ExternalizableHelper.readLong(in);
        m_sReturnValue = ExternalizableHelper.readUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeLong(out, m_timeout);
        ExternalizableHelper.writeLong(out, m_cMillis);
        ExternalizableHelper.writeUTF(out, m_sReturnValue);
        }

    private long m_timeout = TIMEOUT_DEFAULT;

    private long m_cMillis;

    private String m_sReturnValue;

    private boolean m_fRun = true;
    }
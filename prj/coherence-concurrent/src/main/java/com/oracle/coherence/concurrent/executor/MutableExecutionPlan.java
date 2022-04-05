/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.concurrent.Executor;

import java.util.function.Predicate;

/**
 * A mutable implementation of {@link ExecutionPlan}.
 *
 * @author bo
 * @since 21.12
 */
public class MutableExecutionPlan
        implements ExecutionPlan, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an empty, unsatisfied {@link MutableExecutionPlan}.
     */
    public MutableExecutionPlan()
        {
        m_mapActions            = new LinkedHashMap<>();
        m_fSatisfied            = false;
        m_cPendingRecoveryCount = 0;
        }

    /**
     * Constructs a {@link MutableExecutionPlan} based on another {@link ExecutionPlan}.
     *
     * @param executionPlan  the {@link ExecutionPlan}
     */
    public MutableExecutionPlan(ExecutionPlan executionPlan)
        {
        this();

        if (executionPlan != null)
            {
            for (Iterator<String> iterator = executionPlan.getIds(); iterator.hasNext(); )
                {
                String sId    = iterator.next();
                Action action = executionPlan.getAction(sId);

                m_mapActions.put(sId, action);
                }

            m_fSatisfied            = executionPlan.isSatisfied();
            m_cPendingRecoveryCount = executionPlan.getPendingRecoveryCount();
            }
        }

    // ----- public methods -------------------------------------------------

    /**
     * Sets the number of recovery operations that should occur.
     *
     * @param cPendingRecoveryCount  see {@link #getPendingRecoveryCount()}
     */
    public void setPendingRecoveryCount(int cPendingRecoveryCount)
        {
        m_cPendingRecoveryCount = cPendingRecoveryCount;
        }

    /**
     * Assigns the specified {@link Executor} to the {@link Task}.
     *
     * @param sId  the identity of the {@link Executor}
     */
    public void assign(String sId)
        {
        m_mapActions.put(sId, Action.ASSIGN);
        }

    /**
     * Assigns the specified {@link Executor} to the {@link Task} (for recovery).
     *
     * @param sId  the identity of the {@link Executor}
     */
    public void recover(String sId)
        {
        m_mapActions.put(sId, Action.RECOVER);
        }

    /**
     * Releases the specified {@link Executor} for the {@link Task}.
     *
     * @param sId  the identity of the {@link Executor}
     */
    public void release(String sId)
        {
        m_mapActions.put(sId, Action.RELEASE);
        }

    /**
     * Determines if the {@link ExecutionStrategy} contains an {@link Action} for the specified {@link Executor}.
     *
     * @param sId  the identity of the {@link Executor}
     *
     * @return <code>true</code> if the {@link ExecutionStrategy} contains an {@link Action}
     *         for the specified {@link Executor}, <code>false</code> otherwise
     */
    public boolean contains(String sId)
        {
        return m_mapActions.containsKey(sId);
        }

    /**
     * Sets if the {@link ExecutionPlan} is satisfied.
     *
     * @param fSatisfied  is satisfied
     */
    public void setSatisfied(boolean fSatisfied)
        {
        m_fSatisfied = fSatisfied;
        }

    // ----- ExecutionPlan interface ----------------------------------------

    @Override
    public Action getAction(String sId)
        {
        return m_mapActions.get(sId);
        }

    @Override
    public boolean setAction(String sId, Action action)
        {
        if (m_mapActions.get(sId) != action)
            {
            m_mapActions.put(sId, action);

            return true;
            }
        return false;
        }

    @Override
    public int size()
        {
        return m_mapActions.size();
        }

    @Override
    public int count(Predicate<? super Action> predicate)
        {
        return (int) m_mapActions.values().stream().filter(predicate).count();
        }

    @Override
    public boolean isSatisfied()
        {
        return m_fSatisfied;
        }

    @Override
    public int getPendingRecoveryCount()
        {
        return m_cPendingRecoveryCount;
        }

    @Override
    public boolean optimize()
        {
        // assume no changes will occur
        boolean fChanged = false;

        // remove mappings for actions that have been released
        for (Iterator<Map.Entry<String, Action>> iterator = m_mapActions.entrySet().iterator(); iterator.hasNext(); )
            {
            Map.Entry<String, Action> entry = iterator.next();

            if (entry.getValue() == Action.RELEASE)
                {
                iterator.remove();

                fChanged = true;
                }
            }

        return fChanged;
        }

    @Override
    public Iterator<String> getIds()
        {
        return m_mapActions.keySet().iterator();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder builder = new StringBuilder();

        builder.append("ExecutionPlan{");

        builder.append("satisfied=");
        builder.append(m_fSatisfied);

        builder.append(", pendingRecoveryCount=");
        builder.append(m_cPendingRecoveryCount);

        builder.append(", actions=[");

        boolean first = true;

        for (String executorServiceId : m_mapActions.keySet())
            {
            if (first)
                {
                first = false;
                }
            else
                {
                builder.append(", ");
                }

            builder.append("(");
            builder.append(executorServiceId);
            builder.append(",");
            builder.append(m_mapActions.get(executorServiceId));
            builder.append(")");
            }

        builder.append("]}");

        return builder.toString();
        }

    @Override
    public boolean equals(Object other)
        {
        if (this == other)
            {
            return true;
            }

        if (!(other instanceof MutableExecutionPlan))
            {
            return false;
            }

        MutableExecutionPlan that = (MutableExecutionPlan) other;

        if (m_fSatisfied != that.m_fSatisfied)
            {
            return false;
            }

        return m_mapActions.equals(that.m_mapActions);

        }

    @Override
    public int hashCode()
        {
        int result = m_mapActions.hashCode();

        result = 31 * result + (m_fSatisfied ? 1 : 0);

        return result;
        }

    // ----- ExternalizableLite interface -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        ExternalizableHelper.readMap(in, m_mapActions, null);
        m_fSatisfied            = in.readBoolean();
        m_cPendingRecoveryCount = ExternalizableHelper.readInt(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeMap(out, m_mapActions);
        out.writeBoolean(m_fSatisfied);
        ExternalizableHelper.writeInt(out, m_cPendingRecoveryCount);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_mapActions            = in.readMap(0, m_mapActions);
        m_fSatisfied            = in.readBoolean(1);
        m_cPendingRecoveryCount = in.readInt(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeMap(0,     m_mapActions);
        out.writeBoolean(1, m_fSatisfied);
        out.writeInt(2,     m_cPendingRecoveryCount);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Action}s by {@link Executor} identity.
     */
    protected LinkedHashMap<String, Action> m_mapActions;

    /**
     * Indicates when an {@link ExecutionPlan} has satisfied the requirements of the defining {@link
     * ExecutionStrategy}.
     */
    protected boolean m_fSatisfied;

    /**
     * The number of effective assignments in the past that failed (for some reason) that can be re-assigned in the
     * future as {@link Action#RECOVER} actions.
     */
    protected int m_cPendingRecoveryCount;
    }

/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;

import com.tangosol.util.processor.ConditionalRemove;

import java.io.IOException;
import java.io.Serializable;

import java.util.Iterator;

import java.util.concurrent.Executor;

import static com.oracle.coherence.concurrent.executor.ExecutionPlan.Action;

/**
 * Represents the state of a {@link Task} assignment to a registered {@link Executor}.
 *
 * @author bo
 * @since 21.06
 */
public class ClusteredAssignment
        implements Serializable, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ClusteredAssignment} (required for Serializable).
     */
    @SuppressWarnings("unused")
    public ClusteredAssignment()
        {
        }

    /**
     * Constructs a {@link ClusteredAssignment}.
     *
     * @param executorId  the {@link Executor} identity
     * @param sTaskId     the {@link Task} identity
     */
    public ClusteredAssignment(String executorId, String sTaskId)
        {
        m_sExecutorId = executorId;
        m_sTaskId     = sTaskId;
        m_state       = State.ASSIGNED;
        m_fRecovered  = false;
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sExecutorId = in.readString(0);
        m_sTaskId     = in.readString(1);
        m_state       = in.readObject(2);
        m_fRecovered  = in.readBoolean(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0,  m_sExecutorId);
        out.writeString(1,  m_sTaskId);
        out.writeObject(2,  m_state);
        out.writeBoolean(3, m_fRecovered);
        }

    // ----- enum: State ----------------------------------------------------

    /**
     * The current state of the assignment.
     */
    public enum State
        {
            /**
             * The {@link Task} has been assigned to the {@link Executor} but has yet to commence running.
             */
            ASSIGNED,

            /**
             * The {@link Executor} has commenced executing the {@link Task}.
             */
            EXECUTING,

            /**
             * The {@link Executor} has completed executing the {@link Task} and should not be re-scheduled for further
             * execution.   This does not mean however that the {@link Task} has been completed.
             */
            EXECUTED
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the {@link Executor} identity.
     *
     * @return the {@link Executor} identity
     */
    public String getExecutorId()
        {
        return m_sExecutorId;
        }

    /**
     * Obtains the {@link Task} identity.
     *
     * @return the {@link Task} identity
     */
    public String getTaskId()
        {
        return m_sTaskId;
        }

    /**
     * Obtains the {@link State} of the assignment.
     *
     * @return the {@link State}
     */
    public State getState()
        {
        return m_state;
        }

    /**
     * Sets the {@link State} of the assignment.
     *
     * @param state  the {@link State}
     */
    public void setState(State state)
        {
        m_state = state;
        }

    /**
     * Determines if the {@link Task} was recovered from being previously assigned to a different {@link Executor}.
     *
     * @return <code>true</code> if previously assigned to a different {@link Executor},
     *         <code>false</code> otherwise
     */
    public boolean isRecovered()
        {
        return m_fRecovered;
        }

    /**
     * Sets if the {@link Task} was previously assigned to a different {@link Executor}.
     *
     * @param fRecovered  <code>true</code> if previously assigned to a different {@link Executor},
     *                   <code>false</code> otherwise
     */
    public void setRecovered(boolean fRecovered)
        {
        m_fRecovered = fRecovered;
        }

    /**
     * Obtains the {@link NamedCache} key to use for the {@link ClusteredAssignment}.
     *
     * @return the {@link NamedCache} key for this assignment
     */
    public String getCacheKey()
        {
        return getCacheKey(m_sExecutorId, m_sTaskId);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "ClusteredAssignment{" + "executorId='" + m_sExecutorId + '\'' + ", taskId='"
               + m_sTaskId + '\'' + ", state=" + m_state + ", recovered=" + m_fRecovered + '}';
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (!(o instanceof ClusteredAssignment))
            {
            return false;
            }

        ClusteredAssignment that = (ClusteredAssignment) o;

        if (m_fRecovered != that.m_fRecovered)
            {
            return false;
            }

        if (!m_sExecutorId.equals(that.m_sExecutorId))
            {
            return false;
            }

        if (!m_sTaskId.equals(that.m_sTaskId))
            {
            return false;
            }

        return m_state == that.m_state;
        }

    @Override
    public int hashCode()
        {
        int result = m_sExecutorId.hashCode();

        result = 31 * result + m_sTaskId.hashCode();
        result = 31 * result + (m_state != null ? m_state.hashCode() : 0);
        result = 31 * result + (m_fRecovered ? 1 : 0);

        return result;
        }

    // ----- static methods --------------------------------------------------

    /**
     * Obtains the {@link NamedCache} key to use for the {@link ClusteredAssignment}.
     *
     * @param sExecutorId  the {@link Executor} identity
     * @param sTaskId      the {@link Task} identity
     *
     * @return the {@link NamedCache} key for this assignment
     */
    public static String getCacheKey(String sExecutorId, String sTaskId)
        {
        return sExecutorId + ':' + sTaskId;
        }

    /**
     * Process a list of assignments for a task.
     *
     * @param sTaskId        the task
     * @param executionPlan  the list of assignments
     * @param service        the {@link CacheService}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerAssignments(String sTaskId, ExecutionPlan executionPlan, CacheService service)
        {
        NamedCache       cacheAssignments = service.ensureCache(CACHE_NAME, null);
        Iterator<String> iter             = executionPlan.getIds();

        while (iter.hasNext())
            {
            String              executorId = iter.next();
            ClusteredAssignment assignment = new ClusteredAssignment(executorId, sTaskId);

            cacheAssignments.invoke(assignment.getCacheKey(),
                                    new AssignmentProcessor(assignment, executionPlan.getAction(executorId)));
            }
        }

    /**
     * Remove assignments for a task.
     *
     * @param sTaskId  the task
     * @param service  the {@link CacheService}
     */
    @SuppressWarnings("unchecked")
    public static void removeAssignments(String sTaskId, CacheService service)
        {
        service.ensureCache(CACHE_NAME,
                            null).invokeAll(new EqualsFilter<String, String>("getTaskId", sTaskId),
                                            new ConditionalRemove<>(AlwaysFilter.INSTANCE, false));
        }

    // ----- inner class: AssignmentProcessor -------------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} which updates an assignment due to an assignment {@link Action}.
     */
    public static class AssignmentProcessor
            extends PortableAbstractProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link AssignmentProcessor} (required for Serializable).
         */
        @SuppressWarnings("unused")
        public AssignmentProcessor()
            {
            }

        /**
         * Construct an {@link AssignmentProcessor} on a given {@link ClusteredAssignment} and assignment
         * {@link Action}.
         *
         * @param assignment  the {@link ClusteredAssignment}
         * @param action      the assignment {@link Action}
         */
        public AssignmentProcessor(ClusteredAssignment assignment, Action action)
            {
            m_action     = action;
            m_assignment = assignment;
            }

        // ----- PortableAbstractProcessor methods --------------------------

        @SuppressWarnings("rawtypes")
        @Override
        public Object process(InvocableMap.Entry entry)
            {
            ExecutorTrace.log(() -> String.format("ClusteredAssignment State for Executor [%s] being configured because of [%s]", entry.getKey(), m_action));

            switch (m_action)
                {
                case ASSIGN:
                case RECOVER:

                    ClusteredAssignment current = (ClusteredAssignment) entry.getValue();

                    // only update when it doesn't exist or is not equal
                    if (!entry.isPresent() || entry.isPresent() && !current.equals(m_assignment))
                        {
                        m_assignment.setState(State.ASSIGNED);
                        m_assignment.setRecovered(m_action == Action.RECOVER);

                        //noinspection unchecked
                        entry.setValue(m_assignment);
                        }

                    break;

                case REASSIGN:
                case RELEASE:

                    if (entry.isPresent())
                        {
                        entry.remove(true);
                        }

                    break;
                }

            return null;
            }

        // ----- PortableObject methods -------------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_action     = in.readObject(0);
            m_assignment = in.readObject(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_action);
            out.writeObject(1, m_assignment);
            }

        // ----- data members -----------------------------------------------

        /**
         * The assignment {@link Action}.
         */
        protected Action m_action;

        /**
         * The {@link ClusteredAssignment}.
         */
        protected ClusteredAssignment m_assignment;
        }

    // ----- inner class: SetStateProcessor ---------------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to compare and set the state of a {@link ClusteredAssignment}, returning
     * the previous state.
     */
    public static class SetStateProcessor
            extends PortableAbstractProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link SetStateProcessor} (required for Serializable).
         */
        @SuppressWarnings("unused")
        public SetStateProcessor()
            {
            }

        /**
         * Constructs a {@link SetStateProcessor} that ignores the current state.
         *
         * @param desired   the desired state
         */
        @SuppressWarnings("unused")
        public SetStateProcessor(State desired)
            {
            m_previous = null;
            m_desired  = desired;
            }

        /**
         * Constructs a {@link SetStateProcessor}.
         *
         * @param previous  the previous state (<code>null</code> if any state ok to replace)
         * @param desired   the desired state
         */
        public SetStateProcessor(State previous,
                                 State desired)
            {
            m_previous = previous;
            m_desired  = desired;
            }

        @SuppressWarnings("rawtypes")
        @Override
        public Object process(InvocableMap.Entry entry)
            {
            if (entry.isPresent())
                {
                ClusteredAssignment assignment = (ClusteredAssignment) entry.getValue();
                State               existing   = assignment.getState();

                if (existing != null && existing.equals(m_previous) || m_previous == null)
                    {
                    assignment.setState(m_desired);

                    //noinspection unchecked
                    entry.setValue(assignment);

                    ExecutorTrace.log(() -> String.format("ClusteredAssignment State for Executor [%s] changed from [%s] to [%s]", entry.getKey(), m_previous, m_desired));
                    }

                return existing;
                }
            else
                {
                return null;
                }
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_previous = in.readObject(0);
            m_desired  = in.readObject(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_previous);
            out.writeObject(1, m_desired);
            }

        // ----- data members -----------------------------------------------

        /**
         * The previous {@link State}.
         */
        protected State m_previous;

        /**
         * The desired {@link State}.
         */
        protected State m_desired;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The {@link NamedCache} in which instance of this class will be placed.
     */
    public static String CACHE_NAME = "ClusteredExecutorService.assignments";

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Executor} identity.
     */
    protected String m_sExecutorId;

    /**
     * The {@link Task} identity.
     */
    protected String m_sTaskId;

    /**
     * The {@link State} of the assignment.
     */
    protected State m_state;

    /**
     * Indicates if the {@link Task} was recovered after previously being allocated to another {@link Executor}.
     */
    protected boolean m_fRecovered;
    }

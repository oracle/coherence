/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.internal.Cause;
import com.oracle.coherence.concurrent.executor.internal.ClusterMemberAware;
import com.oracle.coherence.concurrent.executor.internal.Leased;
import com.oracle.coherence.concurrent.executor.internal.LiveObject;

import com.oracle.coherence.concurrent.executor.options.ClusterMember;
import com.oracle.coherence.concurrent.executor.options.Member;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import com.oracle.coherence.concurrent.executor.options.Name;
import com.oracle.coherence.concurrent.executor.util.OptionsByType;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.UID;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;

import com.tangosol.util.processor.ConditionalRemove;
import com.tangosol.util.processor.ExtractorProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * A Coherence-based implementation of a {@link TaskExecutorService.ExecutorInfo}.
 *
 * @author bo
 *
 * @since 21.12
 */
public class ClusteredExecutorInfo
        implements TaskExecutorService.ExecutorInfo, LiveObject,
                   Leased, ClusterMemberAware, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ClusteredExecutorInfo} (for serialization support).
     */
    @SuppressWarnings("unused")
    public ClusteredExecutorInfo()
        {
        // required for serialization
        }

    /**
     * Constructs a {@link ClusteredExecutorInfo} with the specified parameters.
     *
     * @param sIdentity        the identity
     * @param ldtUpdate        the time since the epoc when the {@link TaskExecutorService.ExecutorInfo} was last updated
     * @param cMaxMemory       the maximum memory as reported by {@link Runtime#maxMemory()}
     * @param totalMemory      the total memory as reported by {@link Runtime#totalMemory()}
     * @param freeMemory       the free memory as reported by {@link Runtime#freeMemory()}
     * @param optionsByType    the {@link TaskExecutorService.Registration.Option}s for the {@link Executor}
     */
    public ClusteredExecutorInfo(String sIdentity, long ldtUpdate, long cMaxMemory, long totalMemory,
                                 long freeMemory, OptionsByType<TaskExecutorService.Registration.Option> optionsByType)
        {
        m_sIdentity       = sIdentity;
        m_optionsByType   = optionsByType;
        m_ldtUpdate       = ldtUpdate;
        m_cMaxMemory      = cMaxMemory;
        m_cTotalMemory    = totalMemory;
        m_cFreeMemory     = freeMemory;
        m_cCompleted      = 0;
        m_cRejected       = 0;
        m_cInProgress     = 0;
    }

    // ----- public methods -------------------------------------------------

    /**
     * Touches the last update time for the {@link TaskExecutorService.ExecutorInfo}.
     */
    public void touch()
        {
        m_ldtUpdate = CacheFactory.getSafeTimeMillis();
        }

    // ----- ExecutorInfo interface -----------------------------------------

    @Override
    public String getId()
        {
        return m_sIdentity;
        }

    /**
     * Obtains the current {@link State} of the {@link Executor}.
     *
     * @return the current {@link State}
     */
    @Override
    public State getState()
        {
        return m_state;
        }

    @Override
    public long getLastUpdateTime()
        {
        return m_ldtUpdate;
        }

    @Override
    public long getMaxMemory()
        {
        return m_cMaxMemory;
        }

    @Override
    public long getTotalMemory()
        {
        return m_cTotalMemory;
        }

    @Override
    public long getFreeMemory()
        {
        return m_cFreeMemory;
        }

    @Override
    public <T extends TaskExecutorService.Registration.Option> T getOption(Class<T> clzOfOption, T defaultIfNotFound)
        {
        return m_optionsByType.get(clzOfOption, defaultIfNotFound);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Sets the current {@link State} of the {@link Executor}.
     *
     * @param state  the desired {@link State}
     */
    public void setState(State state)
        {
        ExecutorTrace.log(() -> String.format("ClusteredExecutorInfo [%s] changing state from [%s] to [%s]",
                                        m_sIdentity, m_state, state));

        m_state = state;
        }

    /**
     * Return the number of completed tasks.
     *
     * @return the number of completed tasks
     */
    @SuppressWarnings("unused")
    public long getTasksCompletedCount()
        {
        return m_cCompleted;
        }

    /**
     * Return the number rejected tasks.
     *
     * @return the number rejected tasks
     */
    @SuppressWarnings("unused")
    public long getTasksRejectedCount()
        {
        return m_cRejected;
        }

    /**
     * Return the number of tasks currently in progress.
     *
     * @return the number of tasks currently in progress
     */
    @SuppressWarnings("unused")
    public long getTasksInProgressCount()
        {
        return m_cInProgress;
        }

    /**
     * Sets the maximum memory as reported by {@link Runtime#maxMemory()}.
     *
     * @param cMaxMemory the maximum memory
     */
    public void setMaxMemory(long cMaxMemory)
        {
        m_cMaxMemory = cMaxMemory;
        }

    /**
     * Sets the total memory as reported by {@link Runtime#totalMemory()}.
     *
     * @param cTotalMemory  the total memory
     */
    public void setTotalMemory(long cTotalMemory)
        {
        m_cTotalMemory = cTotalMemory;
        }

    /**
     * Sets the free memory as reported by {@link Runtime#freeMemory()}.
     *
     * @param cFreeMemory  the free memory
     */
    public void setFreeMemory(long cFreeMemory)
        {
        m_cFreeMemory = cFreeMemory;
        }

    /**
     * Sets the number of completed tasks.
     *
     * @param cTasksCompleted  the number of completed tasks
     */
    public void setTasksCompletedCount(long cTasksCompleted)
        {
        m_cCompleted = cTasksCompleted;
        }

    /**
     * Sets the number of task failures.
     *
     * @param cTasksFailed  the number of failed tasks
     */
    public void setTasksFailedCount(long cTasksFailed)
        {
        m_cRejected = cTasksFailed;
        }

    /**
     * Sets the number of tasks currently in progress.
     *
     * @param cTasksInProgress  the number of tasks in progress
     */
    public void setTasksInProgressCount(long cTasksInProgress)
        {
        m_cInProgress = cTasksInProgress;
        }

    /**
     * Return the logical name of this {@code ClusteredExecutorInfo}.
     *
     * @return the logical name of this {@code ClusteredExecutorInfo}
     */
    public String getExecutorName()
        {
        return getOption(Name.class, Name.UNNAMED).getName();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "ClusteredExecutorInfo{" + "identity='" + m_sIdentity + '\'' + ", state=" + m_state + ", lastUpdateTime="
               + m_ldtUpdate + ", maxMemory=" + m_cMaxMemory + ", totalMemory=" + m_cTotalMemory + ", freeMemory="
               + m_cFreeMemory + ", optionsByType=" + m_optionsByType + ", completed="
               + m_cCompleted + ", in-progress=" + m_cInProgress + ", rejected=" + m_cRejected + '}';
        }

    // ----- Leased interface -----------------------------------------------

    @Override
    public long getLeaseExpiryTime()
        {
        return m_ldtUpdate + LEASE_DURATION_MS;
        }

    @Override
    public void renew()
        {
        touch();
        }

    @Override
    public boolean onLeaseExpiry()
        {
        ExecutorTrace.log(() -> String.format("Lease for Executor [%s] in [%s] has expired", m_sIdentity, m_state));

        // only set the state to closing if we're not already closing / closed
        if (m_state != State.CLOSED && m_state != State.CLOSING)
            {
            setState(State.CLOSING);

            return true;
            }
        else
            {
            return false;
            }
        }

    // ----- LiveObject interface -------------------------------------------

    @SuppressWarnings("rawtypes")
    @Override
    public ComposableContinuation onInserted(CacheService service, Entry entry, final Cause cause)
        {
        ExecutorTrace.log(() -> String.format("Inserted [%s] due to [%s]", this, cause));

        switch (m_state)
            {
            case JOINING:

                // schedule introducing the Executor to existing (non-completed) Tasks
                return new JoiningContinuation(m_sIdentity, service);

            case RUNNING:

                // nothing to do when we're running
                return null;

            case CLOSING_GRACEFULLY:

                // stop accepting new tasks
                return new ClosingGracefullyContinuation(m_sIdentity, service);

            case CLOSING:

                // schedule cleanup of the Executor (including from Tasks)
                return new ClosingContinuation(m_sIdentity, service);

            case CLOSED:

                // schedule removing the ExecutionServiceInfo
                return new RemoveContinuation(m_sIdentity, service);

            default:
                return null;
            }
        }

    @SuppressWarnings("rawtypes")
    @Override
    public ComposableContinuation onUpdated(CacheService service, Entry entry, final Cause cause)
        {
        ExecutorTrace.log(() -> String.format("Updated [%s] due to [%s]", this, cause));

        switch (m_state)
            {
            case JOINING:

                // this shouldn't happen (there's no way to update to the JOINING state)
                return null;

            case RUNNING:

                // nothing to do when we're running
                return null;

            case CLOSING_GRACEFULLY:

                // stop accepting new tasks
                return new ClosingGracefullyContinuation(m_sIdentity, service);

            case CLOSING:

                // schedule cleanup of the Executor (including from Tasks)
                return new ClosingContinuation(m_sIdentity, service);

            case CLOSED:

                // schedule removing the ExecutionServiceInfo
                return new RemoveContinuation(m_sIdentity, service);

            default:
                return null;
            }
        }

    @SuppressWarnings("rawtypes")
    @Override
    public ComposableContinuation onDeleted(CacheService service, Entry entry, Cause cause)
        {
        ExecutorTrace.log(() -> String.format("Deleted [%s] due to [%s]", this, cause));

        String sExecutorId = (String) entry.getKey();

        service.getResourceRegistry().unregisterResource(ClusteredExecutorInfo.class, sExecutorId);

        // nothing to do when we delete an ExecutorInfo
        return null;
        }

    // ----- ClusterMemberAware interface -----------------------------------

    @Override
    public UID getUid()
        {
        return m_optionsByType.get(ClusterMember.class) == null
               ? null
               : m_optionsByType.get(Member.class, Member.autoDetect()).get().getUid();
        }

    @Override
    public boolean onMemberJoined()
        {
        Logger.fine(() -> String.format("Executor [%s] in [%s] has joined the cluster", m_sIdentity, m_state));

        // no-op
        return false;
        }

    @Override
    public boolean onMemberLeaving()
        {
        // no-op
        return false;
        }

    @Override
    public boolean onMemberLeft()
        {
        Logger.fine(() -> String.format("Executor [%s] in [%s] has left the cluster", m_sIdentity, m_state));


        // only set the state to closing if we're not already closing / closed
        if (m_state != State.CLOSED && m_state != State.CLOSING)
            {
            setState(State.CLOSING);

            return true;
            }
        else
            {
            return false;
            }
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_state         = ExternalizableHelper.readObject(in);
        m_sIdentity     = ExternalizableHelper.readUTF(in);
        m_optionsByType = ExternalizableHelper.readObject(in);
        m_ldtUpdate     = ExternalizableHelper.readLong(in);
        m_cMaxMemory    = ExternalizableHelper.readLong(in);
        m_cTotalMemory  = ExternalizableHelper.readLong(in);
        m_cFreeMemory   = ExternalizableHelper.readLong(in);
        m_cCompleted    = ExternalizableHelper.readLong(in);
        m_cRejected     = ExternalizableHelper.readLong(in);
        m_cInProgress   = ExternalizableHelper.readLong(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_state);
        ExternalizableHelper.writeUTF(out, m_sIdentity);
        ExternalizableHelper.writeObject(out, m_optionsByType);
        ExternalizableHelper.writeLong(out, m_ldtUpdate);
        ExternalizableHelper.writeLong(out, m_cMaxMemory);
        ExternalizableHelper.writeLong(out, m_cTotalMemory);
        ExternalizableHelper.writeLong(out, m_cFreeMemory);
        ExternalizableHelper.writeLong(out, m_cCompleted);
        ExternalizableHelper.writeLong(out, m_cRejected);
        ExternalizableHelper.writeLong(out, m_cInProgress);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_state         = in.readObject(0);
        m_sIdentity     = in.readString(1);
        m_optionsByType = in.readObject(2);
        m_ldtUpdate     = in.readLong(3);
        m_cMaxMemory    = in.readLong(4);
        m_cTotalMemory  = in.readLong(5);
        m_cFreeMemory   = in.readLong(6);
        m_cCompleted    = in.readLong(7);
        m_cRejected     = in.readLong(8);
        m_cInProgress   = in.readLong(9);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0,  m_state);
        out.writeString(1,  m_sIdentity);
        out.writeObject(2,  m_optionsByType);
        out.writeLong(3,    m_ldtUpdate);
        out.writeLong(4,    m_cMaxMemory);
        out.writeLong(5,    m_cTotalMemory);
        out.writeLong(6,    m_cFreeMemory);
        out.writeLong(7,    m_cCompleted);
        out.writeLong(8,    m_cRejected);
        out.writeLong(9,    m_cInProgress);
        }

    // ----- inner class: ClosingContinuation -------------------------------

    /**
     * A {@link ComposableContinuation} to close a {@link ClusteredExecutorInfo},
     * including cleaning up assigned {@link Task}s.
     */
    public static class ClosingContinuation
            implements ComposableContinuation
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link ClosingContinuation}.
         *
         * @param sExecutorId   the {@link Executor} identity to update
         * @param cacheService  the {@link CacheService}
         */
        public ClosingContinuation(String sExecutorId, CacheService cacheService)
            {
            f_cacheService = cacheService;
            f_sExecutorId  = sExecutorId;
            }

        // -----  ComposableContinuation methods ----------------------------

        @SuppressWarnings("unchecked")
        @Override
        public void proceed(Object o)
            {
            ExecutorTrace.log(() -> String.format("Closing Executor [%s]", f_sExecutorId));

            // remove task assignments for the Executor
            ExecutorTrace.log(() -> String.format("Determining Tasks Assigned to Executor [%s]", f_sExecutorId));

            // determine all tasks assigned to the executor
            Map<String, String> assignmentMap = f_cacheService.ensureCache(ClusteredAssignment.CACHE_NAME, null)
                    .invokeAll(new EqualsFilter<String, String>("getExecutorId", f_sExecutorId),
                               new ExtractorProcessor<>("getTaskId"));

            Logger.finer(() -> String.format("Found %d Tasks Assigned to Executor [%s].  Notifying them of the Closing Executor", assignmentMap.size(), f_sExecutorId));

            // notify the Tasks assigned to the Executor to re-assign
            f_cacheService.ensureCache(ClusteredTaskManager.CACHE_NAME, null).
                    invokeAll(assignmentMap.values(), new ClusteredTaskManager.NotifyExecutionStrategyProcessor());

            Logger.finer(() -> String.format("Removing Assignments for Executor [%s]", f_sExecutorId));

            // now remove the assignments for the tasks
            f_cacheService.ensureCache(ClusteredTaskManager.CACHE_NAME, null)
                    .invokeAll(assignmentMap.keySet(), new ConditionalRemove<String, String>(AlwaysFilter.INSTANCE));

            Logger.finer(() -> String.format("Notifying Executor [%s] that it is now Closed", f_sExecutorId));

            // change the state of the Executor to be CLOSED
            f_cacheService.ensureCache(CACHE_NAME, null).invoke(f_sExecutorId, new SetStateProcessor(State.CLOSED));
            }

        @Override
        public ComposableContinuation compose(ComposableContinuation continuation)
            {
            return continuation;
            }

        // ----- data members -----------------------------------------------

        /**
         * The unique identity of the {@link Executor}.
         */
        protected final String f_sExecutorId;

        /**
         * The {@link CacheService} for accessing the {@link TaskExecutorService.ExecutorInfo} cache.
         */
        protected final CacheService f_cacheService;
        }

    // ----- inner class: ClosingGracefullyContinuation ---------------------

    /**
     * A {@link ComposableContinuation} to gracefully close a {@link ClusteredExecutorInfo},
     * allowing already assigned {@link Task}s to complete.
     */
    public static class ClosingGracefullyContinuation
            implements ComposableContinuation
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link ClosingGracefullyContinuation}.
         *
         * @param sExecutorId   the {@link Executor} identity to update
         * @param cacheService  the {@link CacheService}
         */
        public ClosingGracefullyContinuation(String sExecutorId, CacheService cacheService)
            {
            m_sExecutorId  = sExecutorId;
            f_cacheService = cacheService;
            }

        // ----- ComposableContinuation methods -----------------------------

        @Override
        public void proceed(Object o)
            {
            // determine if there are any Tasks currently running on this Executor
            long currentTaskCount = f_cacheService.ensureCache(ClusteredAssignment.CACHE_NAME, null)
                    .stream(new EqualsFilter<String, String>("getExecutorId", m_sExecutorId)).count();

            if (currentTaskCount == 0)
                {
                // proceed to CLOSING
                //noinspection unchecked
                f_cacheService.ensureCache(CACHE_NAME, null)
                        .invoke(m_sExecutorId, new SetStateProcessor(State.CLOSING));
                }
            else
                {
                Logger.finer(() -> String.format("Graceful closing of Executor [%s]. %d assigned task(s) remaining",
                                                 m_sExecutorId, currentTaskCount));
                }
            }

        @Override
        public ComposableContinuation compose(ComposableContinuation continuation)
            {
            return continuation;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "ClosingGracefullyContinuation{" + "ExecutorId='" + m_sExecutorId + '\'' + '}';
            }

        // ----- data members -----------------------------------------------

        /**
         * The unique identity of the {@link Executor}.
         */
        protected final String m_sExecutorId;

        /**
         * The {@link CacheService} for accessing the {@link TaskExecutorService.ExecutorInfo} cache.
         */
        protected final CacheService f_cacheService;
        }

    // ----- JoiningContinuation interface ----------------------------------

    /**
     * A {@link ComposableContinuation} to commence orchestrating a new {@link Executor}
     * with existing {@link Task}s.
     */
    public static class JoiningContinuation
            implements ComposableContinuation, PortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link JoiningContinuation} (required for serialization).
         */
        @SuppressWarnings("unused")
        public JoiningContinuation()
            {
            }

        /**
         * Constructs an {@link JoiningContinuation}.
         *
         * @param cacheService  the {@link CacheService}
         * @param sExecutorId   the {@link Executor} identity to update
         */
        public JoiningContinuation(String sExecutorId, CacheService cacheService)
            {
            m_sExecutorId  = sExecutorId;
            m_cacheService = cacheService;
            }

        // ----- ComposableContinuation methods -----------------------------

        @SuppressWarnings("unchecked")
        @Override
        public void proceed(Object o)
            {
            // change the state of the Executor to be Running
            m_cacheService.ensureCache(CACHE_NAME,
                                       null).invoke(m_sExecutorId, new SetStateProcessor(State.JOINING, State.RUNNING));

            m_cacheService.ensureCache(ClusteredTaskManager.CACHE_NAME,
                                       null).invokeAll(AlwaysFilter.INSTANCE,
                                                     new ClusteredTaskManager.NotifyExecutionStrategyProcessor());

            Logger.fine(() -> String.format("Executor [%s] joined.", m_sExecutorId));
            }

        @Override
        public ComposableContinuation compose(ComposableContinuation continuation)
            {
            return continuation;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader pofReader) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter pofWriter) throws IOException
            {
            }

        // ----- data members -----------------------------------------------

        /**
         * The unique identity of the {@link Executor}.
         */
        protected String m_sExecutorId;

        /**
         * The {@link CacheService} for accessing the {@link TaskExecutorService.ExecutorInfo} cache.
         */
        protected CacheService m_cacheService;
        }

    /**
     * A {@link ComposableContinuation} to remove a {@link ClusteredExecutorInfo}.
     */
    public static class RemoveContinuation
            implements ComposableContinuation
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link ClosingContinuation}.
         *
         * @param sExecutorId   the {@link Executor} identity to update
         * @param cacheService  the {@link CacheService}
         */
        public RemoveContinuation(String sExecutorId, CacheService cacheService)
            {
            f_sExecutorId  = sExecutorId;
            f_cacheService = cacheService;
            }

        // ----- ComposableContinuation methods -----------------------------

        @Override
        public void proceed(Object o)
            {
            // change the state of the Executor to be CLOSED
            f_cacheService.ensureCache(CACHE_NAME, null).remove(f_sExecutorId);

            Logger.fine(() -> String.format("Removed Executor [%s]", f_sExecutorId));
            }

        @Override
        public ComposableContinuation compose(ComposableContinuation continuation)
            {
            return continuation;
            }

        // ----- data members -----------------------------------------------

        /**
         * The unique identity of the {@link Executor}.
         */
        protected final String f_sExecutorId;

        /**
         * The {@link CacheService} for accessing the {@link TaskExecutorService.ExecutorInfo} cache.
         */
        protected final CacheService f_cacheService;
        }

    // ----- inner class: SetStateProcessor ---------------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to compare and set the state of a {@link ClusteredExecutorInfo}, returning
     * the previous state.
     */
    public static class SetStateProcessor
            extends PortableAbstractProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link SetStateProcessor} (required for serialization).
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
        public SetStateProcessor(State desired)
            {
            m_previous = null;
            m_desired  = desired;
            }

        /**
         * Constructs a {@link SetStateProcessor}.
         *
         * @param previous  the previous state ({@code null} if any state ok to replace)
         * @param desired   the desired state
         */
        public SetStateProcessor(State previous, State desired)
            {
            m_previous = previous;
            m_desired  = desired;
            }

        // ----- PortableAbstractProcessor methods --------------------------

        @Override
        public Object process(@SuppressWarnings("rawtypes") InvocableMap.Entry entry)
            {
            if (entry.isPresent())
                {
                ClusteredExecutorInfo info     = (ClusteredExecutorInfo) entry.getValue();
                State                 existing = info.getState();

                if ((existing.equals(m_previous) || m_previous == null) && !existing.equals(m_desired))
                    {
                    // check that the state transition is valid
                    boolean fValidTransition = true;

                    switch (m_desired)
                        {
                        case JOINING:

                            // should never transition to JOINING
                            fValidTransition = false;
                            break;

                        case RUNNING:
                            if (!(existing.equals(State.JOINING) || existing.equals(State.REJECTING)))
                                {
                                fValidTransition = false;
                                }

                            break;

                        case REJECTING:
                            if (!existing.equals(State.RUNNING))
                                {
                                fValidTransition = false;
                                }

                            break;

                        case CLOSING_GRACEFULLY:
                            if (existing == State.CLOSED || existing == State.CLOSING)
                                {
                                fValidTransition = false;
                                }

                            break;

                        case CLOSING:
                            if (existing.equals(State.CLOSED))
                                {
                                fValidTransition = false;
                                }

                            break;

                        case CLOSED:

                            // any transition to CLOSED is fine
                            break;

                        default:

                            // unknown state? allow
                        }

                    if (!fValidTransition)
                        {
                        Logger.warn(() -> String.format("Invalid transition for Executor[%s] from [%s] to [%s].",
                                                        info.getId(), existing, m_desired));

                        return null;
                        }

                    info.setState(m_desired);

                    //noinspection unchecked
                    entry.setValue(info);
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

    // ----- inner class: TouchProcessor ------------------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to update the last update timestamp
     * of a {@link ClusteredExecutorInfo}.
     */
    public static class TouchProcessor
            extends PortableAbstractProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link TouchProcessor} (required for serialization).
         */
        public TouchProcessor()
            {
            }

        // ----- PortableAbstractProcessor methods --------------------------

        @Override
        public Object process(@SuppressWarnings("rawtypes") Entry entry)
            {
            if (entry.isPresent())
                {
                ClusteredExecutorInfo info = (ClusteredExecutorInfo) entry.getValue();

                info.touch();

                //noinspection unchecked
                entry.setValue(info);
                }

            return null;
            }
        }

    // ----- TouchRunnable --------------------------------------------------

    /**
     * A {@link Runnable} to asynchronously touch.
     */
    public static class TouchRunnable
            implements Runnable
        {
        // ----- constructors -----------------------------------------------

        /**
         * @param sExecutorId   the {@link Executor} identity to update
         * @param cacheService  the {@link CacheService}
         */
        public TouchRunnable(String sExecutorId, CacheService cacheService)
            {
            f_sExecutorId  = sExecutorId;
            f_cacheService = cacheService;
            }

        // ----- Runnable interface -----------------------------------------

        @Override
        public void run()
            {
            // acquire the executor info cache
            @SuppressWarnings("unchecked") NamedCache<String, ?> cache =
                    f_cacheService.ensureCache(ClusteredExecutorInfo.CACHE_NAME, null);

            //noinspection unchecked
            cache.invoke(f_sExecutorId, new TouchProcessor());
            }

        // ----- data members -----------------------------------------------

        /**
         * The unique identity of the {@link Executor}.
         */
        protected final String f_sExecutorId;

        /**
         * The {@link CacheService} for accessing the {@link TaskExecutorService.ExecutorInfo} cache.
         */
        protected final CacheService f_cacheService;
        }

    // ----- UpdateInfoProcessor --------------------------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to update the state of an {@link TaskExecutorService.ExecutorInfo}.
     */
    public static class UpdateInfoProcessor
            extends PortableAbstractProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link UpdateInfoProcessor} (required for serialization).
         */
        @SuppressWarnings("unused")
        public UpdateInfoProcessor()
            {
            }

        /**
         * Constructs an {@link UpdateInfoProcessor}.
         *
         * @param cMaxMemory        the maximum memory as reported by {@link Runtime#maxMemory()}
         * @param cTotalMemory      the total memory as reported by {@link Runtime#totalMemory()}
         * @param cFreeMemory       the free memory as reported by {@link Runtime#freeMemory()}
         * @param fTerminated       whether the monitored {@link Executor} is terminated.
         * @param cTasksCompleted   the completed tasks count.
         * @param cTasksFailed      the failed tasks count.
         * @param cTasksInProgress  the in progress tasks count.
         */
        @SuppressWarnings("unused")
        public UpdateInfoProcessor(long cMaxMemory, long cTotalMemory, long cFreeMemory, boolean fTerminated,
                                   long cTasksCompleted, long cTasksFailed, long cTasksInProgress)
            {
            m_cMaxMemory       = cMaxMemory;
            m_cTotalMemory     = cTotalMemory;
            m_cFreeMemory      = cFreeMemory;
            m_fTerminated      = fTerminated;
            m_cTasksCompleted  = cTasksCompleted;
            m_cTasksFailed     = cTasksFailed;
            m_cTasksInProgress = cTasksInProgress;
            }

        // ----- PortableAbstractProcessor methods --------------------------

        @Override
        public Object process(@SuppressWarnings("rawtypes") InvocableMap.Entry entry)
            {
            // only update when there's an entry
            if (entry.isPresent())
                {
                ClusteredExecutorInfo info = (ClusteredExecutorInfo) entry.getValue();

                // update the info
                info.setMaxMemory(m_cMaxMemory);
                info.setTotalMemory(m_cTotalMemory);
                info.setFreeMemory(m_cFreeMemory);
                info.setTasksCompletedCount(m_cTasksCompleted);
                info.setTasksFailedCount(m_cTasksFailed);
                info.setTasksInProgressCount(m_cTasksInProgress);

                State currentState = info.getState();

                if (m_fTerminated && !(currentState == State.CLOSING || currentState == State.CLOSED))
                    {
                    Logger.fine(() -> String.format("Executor [%s] has been terminated", entry.getKey()));

                    info.setState(State.CLOSING);
                    }

                // touch the info (to mark is as updated)
                info.touch();

                //noinspection unchecked
                entry.setValue(info);
                }

            return null;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_cMaxMemory       = in.readLong(0);
            m_cTotalMemory     = in.readLong(1);
            m_cFreeMemory      = in.readLong(2);
            m_fTerminated      = in.readBoolean(3);
            m_cTasksCompleted  = in.readLong(4);
            m_cTasksFailed     = in.readLong(5);
            m_cTasksInProgress = in.readLong(6);

            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeLong(0,    m_cMaxMemory);
            out.writeLong(1,    m_cTotalMemory);
            out.writeLong(2,    m_cFreeMemory);
            out.writeBoolean(3, m_fTerminated);
            out.writeLong(4,    m_cTasksCompleted);
            out.writeLong(5,    m_cTasksFailed);
            out.writeLong(6,    m_cTasksInProgress);

            }

        // ----- data members -----------------------------------------------

        /**
         * The maximum memory as reported by {@link Runtime#maxMemory()}.
         */
        protected long m_cMaxMemory;

        /**
         * The total memory as reported by {@link Runtime#totalMemory()}.
         */
        protected long m_cTotalMemory;

        /**
         * The free memory as reported by {@link Runtime#freeMemory()}.
         */
        protected long m_cFreeMemory;

        /**
         * Whether the Executor has been terminated.
         */
        protected boolean m_fTerminated;

        /**
         * The tasks completed count.
         */
        protected long m_cTasksCompleted;

        /**
         * The tasks failed count.
         */
        protected long m_cTasksFailed;

        /**
         * The tasks in progress count.
         */
        protected long m_cTasksInProgress;
        }

    // ----- UpdateInfoRunnable ---------------------------------------------

    /**
     * A {@link Runnable} to asynchronously update the state of {@link ClusteredExecutorInfo} using the information
     * based on the current {@link Runtime}.
     */
    public static class UpdateInfoRunnable
            implements Runnable
        {
        // ----- constructors ----------------------------------------------

        /**
         * Constructs an {@link UpdateInfoRunnable}.
         *
         * @param cacheService           the {@link CacheService}
         * @param sExecutorId            the {@link Executor} identity to update
         * @param executor               the {@link Executor} to update
         * @param clusteredRegistration  the {@link ClusteredRegistration} with task counts stats
         */
        public UpdateInfoRunnable(CacheService cacheService, String sExecutorId, Executor executor,
                                  ClusteredRegistration clusteredRegistration)
            {
            f_cacheService          = cacheService;
            f_sExecutorId           = sExecutorId;
            f_monitoredExecutor     = executor;
            f_clusteredRegistration = clusteredRegistration;
            }

        // ----- Runnable interface -----------------------------------------

        @Override
        public void run()
            {
            if (s_fPerformUpdate)
                {
                ExecutorTrace.log(() -> String.format("Updating Information for Executor [%s]", f_sExecutorId));

                // acquire the Runtime so we can capture local runtime information
                Runtime runtime = Runtime.getRuntime();

                // update the cluster information for the executor
                // acquire the executor info cache
                //noinspection unchecked
                NamedCache<String, ?> cache = f_cacheService.ensureCache(ClusteredExecutorInfo.CACHE_NAME, null);

                //noinspection unchecked
                cache.invoke(f_sExecutorId,
                             new UpdateInfoProcessor(runtime.maxMemory(),
                                                     runtime.totalMemory(),
                                                     runtime.freeMemory(),
                                                     f_monitoredExecutor instanceof ExecutorService
                                                        && ((ExecutorService) f_monitoredExecutor).isTerminated(),
                                                     f_clusteredRegistration.getTasksCompletedCount(),
                                                     f_clusteredRegistration.getTasksRejectedCount(),
                                                     f_clusteredRegistration.getTasksInProgressCount()));
                }
            else
                {
                ExecutorTrace.log(() -> String.format("Skipping Information Update for Executor [%s]", f_sExecutorId));
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * Field description.
         */
        public static volatile boolean s_fPerformUpdate = true;

        /**
         * The unique identity of the {@link Executor}.
         */
        protected final String f_sExecutorId;

        /**
         * The {@link CacheService} for accessing the {@link TaskExecutorService.ExecutorInfo} cache.
         */
        protected final CacheService f_cacheService;

        /**
         * The {@link ClusteredRegistration} that contains stats on task counts.
         */
        protected final ClusteredRegistration f_clusteredRegistration;

        /**
         * The locally running {@link Executor} to monitor.
         */
        protected final Executor f_monitoredExecutor;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The {@link NamedCache} in which instance of this class will be placed.
     */
    public static final String CACHE_NAME = "executor-executors";

    /**
     * The duration of a lease for {@link Executor}s.
     * <p>
     * When {@link TaskExecutorService.ExecutorInfo} for an {@link Executor} has not
     * been updated for the lease duration time, the {@link Executor} is considered to
     * be offline and should automatically be de-registered.
     */
    public static long LEASE_DURATION_MS = 30 * 1000;    // 30 seconds

    // ----- data members ---------------------------------------------------

    /**
     * The {@link State} of the {@link Executor}.
     */
    protected State m_state = State.JOINING;    // initialize here to ensure state is never null

    /**
     * The identity of the {@link TaskExecutorService.Registration}.
     */
    protected String m_sIdentity;

    /**
     * The {@link TaskExecutorService.Registration.Option}s for the {@link Executor}.
     */
    protected OptionsByType<TaskExecutorService.Registration.Option> m_optionsByType;

    /**
     * The cluster time when the {@link TaskExecutorService.ExecutorInfo} was last updated.
     */
    protected long m_ldtUpdate;

    /**
     * The maximum memory as reported by {@link Runtime#maxMemory()}.
     */
    protected long m_cMaxMemory;

    /**
     * The total memory as reported by {@link Runtime#totalMemory()}.
     */
    protected long m_cTotalMemory;

    /**
     * The free memory as reported by {@link Runtime#freeMemory()}.
     */
    protected long m_cFreeMemory;

    /**
     * The completed tasks count.
     */
    protected long m_cCompleted;

    /**
     * The rejected tasks count.
     */
    protected long m_cRejected;

    /**
     * The tasks that are in progress count.
     */
    protected long m_cInProgress;
    }

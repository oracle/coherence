/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.internal;

import com.oracle.coherence.concurrent.executor.ClusteredTaskManager;
import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.PortableAbstractProcessor;

import com.oracle.coherence.concurrent.executor.options.Debugging;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.coherence.config.Config;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;

import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.DaemonThreadFactory;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A ClusteredTaskManager event interceptor that orders the incoming tasks and put them in pending
 * state when the orchestrated tasks reaches given capacity.  Dispatches the pending tasks in a batch
 * at a time as orchestrated tasks complete.
 *
 * @author bo, lh
 * @since 21.12
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ClusteredTaskInterceptor
        implements EventInterceptor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ClusteredTaskInterceptor}.
     *
     * @param sServiceName  the service name the interceptor is registered with
     */
    public ClusteredTaskInterceptor(String sServiceName)
        {
        f_cacheService       = (DistributedCacheService) CacheFactory.getCluster().getService(sServiceName);
        f_cOrchestratedTasks = new AtomicInteger(0);
        f_fPendingTasks      = new AtomicBoolean(false);
        f_executorService    = Executors.newSingleThreadExecutor(new DaemonThreadFactory("TaskInterceptorThread-"));

        int cParts = f_cacheService.getPartitionCount();

        f_sequences = new AtomicLongArray(cParts);

        for (int i = 0; i < cParts; i++)
            {
            f_sequences.set(i, 0);
            }

        f_atomicPartsPending.set(new PartitionSet(cParts));
        f_mapPendingTasks = new ConcurrentHashMap<>();

        Hook.addShutdownHook(f_cacheService.getBackingMapManager().getCacheFactory().getResourceRegistry(),
                             f_executorService::shutdown);
        }

    // ----- ClusteredTaskInterceptor methods -------------------------------

    /**
     * Add an entry to the key map.
     *
     * @param keyMap    the sequence to key map
     * @param sequence  the sequence
     * @param key       the task key
     */
    @SuppressWarnings("unused")
    protected synchronized void addKey(LongArray<String> keyMap, long sequence, String key)
        {
        keyMap.set(sequence, key);
        }

    /**
     * Add the given partition to the set of partitions with pending work. This is invoked when the first new task with
     * the given partition ID is added.
     *
     * @param nPid  the id that has pending task
     */
    public void addPending(int nPid)
        {
        AtomicReference<PartitionSet> atomicSetPending = f_atomicPartsPending;
        PartitionSet                  partsPending     = atomicSetPending.get();
        PartitionSet                  newPartsPending  = new PartitionSet(partsPending);

        newPartsPending.add(nPid);

        do
            {
            partsPending = atomicSetPending.get();
            assert partsPending != null;
            }
        while (!atomicSetPending.compareAndSet(partsPending, newPartsPending));
        }

    /**
     * Remove the given partition from the set of partitions with pending work. This is invoked when a task is
     * completed.
     *
     * @param nPid  the id of the partition that is no longer present on this member
     *
     * @return true if the given partition ID is removed from the {@link PartitionSet}; false otherwise
     */
    protected boolean removePending(int nPid)
        {
        PartitionSet partsCurrent = f_atomicPartsPending.get();

        if (partsCurrent == null || partsCurrent.isEmpty())
            {
            return false;
            }

        PartitionSet partsNew;
        boolean      fRemoved;

        do
            {
            partsCurrent = f_atomicPartsPending.get();
            partsNew     = new PartitionSet(partsCurrent);
            fRemoved     = partsNew.remove(nPid);
            }
        while (!f_atomicPartsPending.compareAndSet(partsCurrent, partsNew));

        return fRemoved;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the associated {@link CacheService}.
     *
     * @return the associated {@link CacheService}
     */
    public CacheService getCacheService()
        {
        return f_cacheService;
        }

    /**
     * Gets the {@link Task} sequence number of a given partition.
     *
     * @param nPartitionId  partition ID
     *
     * @return the {@link Task} sequence number
     */
    @SuppressWarnings("unused")
    public long getSequence(int nPartitionId)
        {
        return f_sequences.get(nPartitionId);
        }

    /**
     * Gets the next {@link Task} sequence number of a given partition.
     *
     * @param nPartitionId  partition ID
     *
     * @return the next available {@link Task} sequence number
     */
    public long getNextSequence(int nPartitionId)
        {
        return f_sequences.incrementAndGet(nPartitionId);
        }

    /**
     * Resets the {@link Task} sequence number of a given partition.
     *
     * @param nPartitionId  partition ID
     *
     * @return the reset {@link Task} sequence number
     */
    @SuppressWarnings("unused")
    public long resetSequence(int nPartitionId)
        {
        return f_sequences.getAndSet(nPartitionId, 0);
        }

    /**
     * Returns the last partition ID of pending tasks processed.
     *
     * @return the last partition ID processed
     */
    @SuppressWarnings("unused")
    public int getLastPartitionId()
        {
        return m_nLastPartitionId;
        }

    /**
     * Set the last partition ID of the pending tasks processed.
     *
     * @param nPartitionId partition ID
     */
    public void setLastPartitionId(int nPartitionId)
        {
        m_nLastPartitionId = nPartitionId;
        }

        /**
         * Returns the maximum batch size.
         *
         * @return the maximum batch size
         */
    public int getBatchMax()
        {
        return f_cMaxBatch;
        }

    // ----- EventInterceptor interface -------------------------------------

    @Override
    public void onEvent(Event event)
        {
        Set<BinaryEntry> setBinaryEntries = Collections.EMPTY_SET;

        if (event instanceof TransactionEvent)
            {
            setBinaryEntries = ((TransactionEvent) event).getEntrySet();
            }
        else if (event instanceof TransferEvent)
            {
            setBinaryEntries = ((TransferEvent) event).getEntries().get(Caches.TASKS_CACHE_NAME);

            if (setBinaryEntries == null)
                {
                setBinaryEntries = Collections.EMPTY_SET;
                }
            }
        else if (event instanceof EntryEvent)
            {
            setBinaryEntries = Set.of(((EntryEvent) event).getEntry());
            }

        for (BinaryEntry binaryEntry : setBinaryEntries)
            {
            Object          oValue = binaryEntry.getValue();
            EntryEvent.Type type   = EntryEvent.Type.UPDATING;    // set the default to a type not used

            if (event instanceof TransactionEvent)
                {
                if (((TransactionEvent) event).getType() == TransactionEvent.Type.COMMITTING)
                    {
                    if (binaryEntry.getOriginalValue() == null)
                        {
                        type = EntryEvent.Type.INSERTING;
                        }
                    else if (binaryEntry.getValue() == null)
                        {
                        type = EntryEvent.Type.REMOVING;
                        }
                    }
                else if (((TransactionEvent) event).getType() == TransactionEvent.Type.COMMITTED)
                    {
                    if (binaryEntry.getOriginalValue() == null)
                        {
                        type = EntryEvent.Type.INSERTED;
                        }
                    else if (oValue == null)
                        {
                        type = EntryEvent.Type.REMOVED;
                        }
                    else
                        {
                        if (oValue instanceof ClusteredTaskManager)
                            {
                            ClusteredTaskManager manager    = (ClusteredTaskManager) oValue;
                            ClusteredTaskManager oldManager = (ClusteredTaskManager) binaryEntry.getOriginalValue();

                            if (oldManager.getState() == ClusteredTaskManager.State.PENDING
                                && manager.getState() == ClusteredTaskManager.State.ORCHESTRATED)
                                {
                                type = EntryEvent.Type.INSERTED;
                                }
                            else if (oldManager.getState() == ClusteredTaskManager.State.ORCHESTRATED
                                && manager.getState() == ClusteredTaskManager.State.TERMINATING)
                                {
                                // treat the transition to TERMINATING as a REMOVED event as the task
                                // could be retained, and we want to keep the PENDING queue moving
                                type = EntryEvent.Type.REMOVED;
                                }
                            }
                        else
                            {
                            type = EntryEvent.Type.UPDATED;
                            }
                        }
                    }
                }
            else if (event instanceof TransferEvent)
                {
                TransferEvent.Type transType = ((TransferEvent) event).getType();

                if (transType == TransferEvent.Type.ARRIVED || transType == TransferEvent.Type.RECOVERED)
                    {
                    type = EntryEvent.Type.INSERTED;
                    }
                else if (transType == TransferEvent.Type.DEPARTING)
                    {
                    type = EntryEvent.Type.REMOVED;
                    }
                }
            else
                {
                if (event.getType() == EntryEvent.Type.REMOVED)
                    {
                    // explicit remove or expiry
                    type = EntryEvent.Type.REMOVED;
                    }
                }

            switch (type)
                {
                case INSERTING:
                    if (oValue instanceof ClusteredTaskManager)
                        {
                        ClusteredTaskManager manager      = (ClusteredTaskManager) oValue;
                        int                  nPartitionId = binaryEntry.getContext()
                                .getKeyPartition(binaryEntry.getBinaryKey());

                        long lSequence    = getNextSequence(nPartitionId);
                        int cOrchestrated = f_cOrchestratedTasks.get();

                        if (cOrchestrated > f_cMaxAllowedTasks || f_fPendingTasks.get())
                            {
                            manager.setState(ClusteredTaskManager.State.PENDING);
                            f_fPendingTasks.compareAndSet(false, true);

                            PartitionSet partsPending = f_atomicPartsPending.get();

                            if (!partsPending.isFull())
                                {
                                addPending(nPartitionId);
                                }

                            LongArray keyMap = f_mapPendingTasks.get(nPartitionId);
                            if (keyMap == null)
                                {
                                f_mapPendingTasks.putIfAbsent(nPartitionId, new SparseArray());
                                }
                            synchronized (this)
                                {
                                keyMap = f_mapPendingTasks.get(nPartitionId);
                                keyMap.set(lSequence, binaryEntry.getKey());
                                }
                            }

                        manager.setPartitionId(nPartitionId);
                        manager.setTaskSequence(lSequence);
                        binaryEntry.setValue(manager);

                        Debugging debug = manager.getDebugging();
                        ExecutorTrace.log(() -> String.format("INSERTING: Task [%s], In Partition [%s], Task Sequence [%s],"
                                + " Task State [%s], Orchestrated Count [%s]",
                                manager.getTaskId(), manager.getPartitionId(),
                                manager.getTaskSequence(), manager.getState(), cOrchestrated), debug);
                        }
                    break;

                case INSERTED:
                    if (oValue instanceof ClusteredTaskManager)
                        {
                        ClusteredTaskManager manager = (ClusteredTaskManager) oValue;

                        if (manager.getState() == ClusteredTaskManager.State.ORCHESTRATED)
                            {
                            f_cOrchestratedTasks.incrementAndGet();
                            }

                        Debugging debug = manager.getDebugging();
                        ExecutorTrace.log(() -> String.format("INSERTED: Task [%s], In Partition [%s], Task Sequence [%s],"
                                + " Task State [%s], Orchestrated Count [%s]",
                                manager.getTaskId(), manager.getPartitionId(),
                                manager.getTaskSequence(), manager.getState(), f_cOrchestratedTasks.get()), debug);
                        }
                    break;

                case UPDATED:
                case REMOVING:
                    break;

                case REMOVED:
                    oValue = binaryEntry.getOriginalValue();

                    if (oValue instanceof ClusteredTaskManager)
                        {
                        ClusteredTaskManager manager = (ClusteredTaskManager) oValue;
                        int cOrchestrated;

                        if (manager.getState() == ClusteredTaskManager.State.ORCHESTRATED)
                            {
                            cOrchestrated = f_cOrchestratedTasks.decrementAndGet();
                            }
                        else
                            {
                            cOrchestrated = f_cOrchestratedTasks.get();
                            }

                        // if true, we need to wait to run more tasks
                        boolean   fDraining = cOrchestrated < f_cMaxAllowedTasks - f_cMaxBatch;

                        Debugging debug     = manager.getDebugging();
                        ExecutorTrace.log(() -> String.format(
                                "REMOVED: Orchestrated Task ID [%s], State[%s], Task Count [%s], Partition [%s], Task Sequence [%s],"
                                + " Draining [%s]",
                                cOrchestrated, manager.getTaskId(), manager.getState(), manager.getPartitionId(),
                                manager.getTaskSequence(), fDraining), debug);

                        if (!fDraining)
                            {
                            // schedule pending tasks to run
                            f_executorService.submit(new Runnable()
                                {
                                @Override
                                synchronized public void run()
                                    {
                                    int        nBatchSize = 0;
                                    int        nMaxSize   = getBatchMax();
                                    Set        setTasks   = new HashSet();
                                    NamedCache taskCache  = Caches.tasks(getCacheService());

                                    int          nPidLast      = m_nLastPartitionId;
                                    PartitionSet partsPending  = f_atomicPartsPending.get();
                                    int          cOrchestrated = f_cOrchestratedTasks.get();
                                    int          nPid          = nPidLast < 0
                                                                     ? partsPending.next(0)
                                                                     : partsPending.next(nPidLast + 1);

                                    if (cOrchestrated < f_cMaxAllowedTasks && !partsPending.isEmpty())
                                        {
                                        ExecutorTrace.log(() -> String.format("Preparing PENDING tasks to run: "
                                                + "Batch Size [%s], Last PID [%s], Pending Partitions [%s],"
                                                + " Orchestrated Count [%s]", nMaxSize, nPidLast,
                                                partsPending, cOrchestrated));
                                        }

                                    while (nPid >= 0
                                           && (nMaxSize == 0 || (nBatchSize < nMaxSize))
                                           && cOrchestrated < f_cMaxAllowedTasks)
                                        {
                                        removePending(nPid);

                                        LongArray keyMap = f_mapPendingTasks.get(nPid);
                                        int finalNPid = nPid;
;                                        if (keyMap != null)
                                            {
                                            int count = keyMap.getSize();
                                            if (count > 0)
                                                {
                                                int finalCount = count;
                                                ExecutorTrace.log(() -> String.format("Processing PENDING tasks [%s][%s] from PID [%s]",
                                                                                      finalCount, keyMap, finalNPid), debug);
                                                if (count > nMaxSize)
                                                    {
                                                    // this partition has a task count that exceeds
                                                    // the batch size; add it back to the pending partitions
                                                    addPending(nPid);
                                                    }

                                                count = 0;
                                                for (LongArray.Iterator iterTasks = keyMap.iterator();
                                                        iterTasks.hasNext(); )
                                                    {
                                                    String sKey = (String) iterTasks.next();
                                                    setTasks.add(sKey);
                                                    iterTasks.remove();
                                                    if (++count > nMaxSize)
                                                        {
                                                        break;
                                                        }
                                                    }

                                                nBatchSize += count;
                                                }
                                            }

                                        nPid = partsPending.next(nPid + 1);
                                        }

                                    if (!setTasks.isEmpty())
                                        {
                                        ExecutorTrace.log(() -> String.format(
                                                "Moving PENDING tasks [%s] to ORCHESTRATED", setTasks), debug);

                                        taskCache.invokeAll(
                                                setTasks,
                                                new SetTaskStateProcessor(ClusteredTaskManager.State.PENDING,
                                                                          ClusteredTaskManager.State.ORCHESTRATED));
                                        }
                                    else
                                        {
                                        f_fPendingTasks.compareAndSet(true, false);
                                        }

                                    setLastPartitionId(nPid);
                                    }
                                });
                            }
                        }
                    break;
                }
            }
        }

    // ----- inner class: SequenceComparator --------------------------------

    /**
     * Comparator used to compare sequence number.
     */
    public static class SequenceComparator
            implements Comparator<Object>, ExternalizableLite, PortableObject
        {
        // ----- Comparator interface ---------------------------------------

        @Override
        public int compare(Object o1,
                           Object o2)
            {
            if (o1 instanceof BinaryEntry && o2 instanceof BinaryEntry)
                {
                ClusteredTaskManager manager1 = (ClusteredTaskManager) ((BinaryEntry) o1).getValue();
                ClusteredTaskManager manager2 = (ClusteredTaskManager) ((BinaryEntry) o2).getValue();

                return (int) (manager1.getTaskSequence() - manager2.getTaskSequence());
                }
            else if (o1 instanceof ClusteredTaskManager && o2 instanceof ClusteredTaskManager)
                {
                return (int) (((ClusteredTaskManager) o1).getTaskSequence()
                              - ((ClusteredTaskManager) o2).getTaskSequence());
                }
            else if (o1 instanceof Map.Entry && o2 instanceof Map.Entry)
                {
                return (int) (((ClusteredTaskManager) ((Map.Entry) o1).getValue()).getTaskSequence()
                              - ((ClusteredTaskManager) ((Map.Entry) o2).getValue()).getTaskSequence());
                }
            else
                {
                return o1.hashCode() - o2.hashCode();
                }
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    // ----- inner class: SetTaskStateProcessor -----------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to compare and set the state of a
     * {@link ClusteredTaskManager}, returning the previous state.
     */
    public static class SetTaskStateProcessor
            extends PortableAbstractProcessor<String, ClusteredTaskManager, ClusteredTaskManager.State>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link SetTaskStateProcessor} (required for serialization).
         */
        @SuppressWarnings("unused")
        public SetTaskStateProcessor()
            {
            }

        /**
         * Constructs a {@link SetTaskStateProcessor} that ignores the current state.
         *
         * @param desired   the desired state
         */
        @SuppressWarnings("unused")
        public SetTaskStateProcessor(ClusteredTaskManager.State desired)
            {
            m_previous = null;
            m_desired  = desired;
            }

        /**
         * Constructs a {@link SetTaskStateProcessor}.
         *
         * @param previous  the previous state (<code>null</code> if any state ok to replace)
         * @param desired   the desired state
         */
        public SetTaskStateProcessor(ClusteredTaskManager.State previous,
                                     ClusteredTaskManager.State desired)
            {
            m_previous = previous;
            m_desired  = desired;
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public ClusteredTaskManager.State process(InvocableMap.Entry entry)
            {
            if (entry.isPresent())
                {
                ClusteredTaskManager       task     = (ClusteredTaskManager) entry.getValue();
                ClusteredTaskManager.State existing = task.getState();

                if (existing != null && existing.equals(m_previous) || m_previous == null)
                    {
                    task.setState(m_desired);

                    entry.setValue(task);
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
            m_desired = in.readObject(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_previous);
            out.writeObject(1, m_desired);
            }

        // ----- data members -----------------------------------------------

        /**
         * The previous {@link ClusteredTaskManager.State}.
         */
        protected ClusteredTaskManager.State m_previous;

        /**
         * The desired {@link ClusteredTaskManager.State}.
         */
        protected ClusteredTaskManager.State m_desired;
        }

    // ----- data members ----------------------------------------------------
   /**
     * Atomic reference for the partition set containing partitions that have pending tasks.
     */
    protected final AtomicReference<PartitionSet> f_atomicPartsPending = new AtomicReference<>();

    /**
     * An array that keeps track of the task sequences for each partition.
     */
    protected final AtomicLongArray f_sequences;

    /**
     * The maximum batch size.
     */
    protected final int f_cMaxBatch =
            Config.getInteger("coherence.executor.batch.max", 20);

    /**
     * The maximum tasks allowed to be run.
     */
    protected final int f_cMaxAllowedTasks =
            Config.getInteger("coherence.executor.concurrent.tasks.max", Integer.MAX_VALUE);

    /**
     * The number of orchestrated tasks.
     */
    protected final AtomicInteger f_cOrchestratedTasks;

    /**
     * A flag to indicate whether there are pending tasks.
     */
    protected final AtomicBoolean f_fPendingTasks;

    /**
     * An {@link ExecutorService} on which to perform background operations.
     */
    protected final ExecutorService f_executorService;

    /**
     * The cache service this interceptor is associated with.
     */
    protected final DistributedCacheService f_cacheService;

    /**
     * A map of pending {@link Task}s for each partition that contains sequence to task key mapping.
     */
    protected final ConcurrentHashMap<Integer, LongArray<String>> f_mapPendingTasks;

    /**
     * The last partition ID processed.
     */
    protected int m_nLastPartitionId;
    }

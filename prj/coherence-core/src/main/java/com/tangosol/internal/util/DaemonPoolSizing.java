/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.util.Base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A process-wide coordinator that assigns finite maximum sizes to live
 * platform-thread daemon pools whose configured maximum is unbounded.
 *
 * @author Aleks Seovic  2026.04.23
 * @since 26.04
 */
public final class DaemonPoolSizing
    {
    // ----- public API ----------------------------------------------------

    /**
     * Register a live platform-thread daemon pool with the process-wide
     * coordinator.
     *
     * @param oIdentity       the stable pool identity
     * @param sName           the diagnostic pool name
     * @param role            the pool role
     * @param cThreadsMin     the configured minimum
     * @param cThreadsMax     the configured maximum
     *
     * @return the live registration
     */
    public static Registration registerPool(Object oIdentity, String sName, Role role,
            int cThreadsMin, int cThreadsMax)
        {
        return COORDINATOR.register(oIdentity, sName, role, cThreadsMin, cThreadsMax);
        }

    /**
     * Return an immutable snapshot of process-wide platform-pool sizing.
     *
     * @return the current sizing snapshot
     */
    public static Snapshot getSnapshot()
        {
        return COORDINATOR.snapshot();
        }

    /**
     * The role of a managed platform-thread pool.
     */
    public enum Role
        {
        /** Blocking remote-client work. */
        BLOCKING_IO(4, 32),

        /** CPU-oriented service work. */
        SERVICE(2, 8),

        /** Infrequent cleanup and internal work. */
        AUXILIARY(1, 2);

        Role(int nWeightFactor, int nProcessorMultiple)
            {
            f_nWeightFactor      = nWeightFactor;
            f_nProcessorMultiple = nProcessorMultiple;
            }

        int getWeightFactor()
            {
            return f_nWeightFactor;
            }

        int getProcessorMultiple()
            {
            return f_nProcessorMultiple;
            }

        private final int f_nWeightFactor;
        private final int f_nProcessorMultiple;
        }

    /**
     * A live registration with the process-wide coordinator.
     */
    public static final class Registration
            implements AutoCloseable
        {
        private Registration(Coordinator coordinator, Entry entry)
            {
            f_coordinator = coordinator;
            f_entry       = entry;
            }

        /**
         * Reserve a logical target count before workers are created.
         *
         * @param cRequested  the requested target count
         *
         * @return the permitted target count
         */
        public int requestCount(int cRequested)
            {
            return f_coordinator.requestCount(f_entry, cRequested);
            }

        /**
         * Record workers whose platform threads started successfully.
         *
         * @param cStarted  the number of workers started
         */
        public void workersStarted(int cStarted)
            {
            f_coordinator.workersStarted(f_entry, cStarted);
            }

        /**
         * Release worker-start reservations after creation failed.
         *
         * @param cFailed  the number of workers that will not be started
         */
        public void workersFailed(int cFailed)
            {
            f_coordinator.workersFailed(f_entry, cFailed);
            }

        /**
         * Record platform workers that have terminated.
         *
         * @param cStopped  the number of workers stopped
         *
         * @return this pool's remaining accounted workers
         */
        public int workersStopped(int cStopped)
            {
            return f_coordinator.workersStopped(f_entry, cStopped);
            }

        /**
         * Update configuration that affects allocation.
         *
         * @param role         the pool role
         * @param cThreadsMin  the configured minimum
         * @param cThreadsMax  the configured maximum
         */
        public void update(Role role, int cThreadsMin, int cThreadsMax)
            {
            f_coordinator.update(f_entry, role, cThreadsMin, cThreadsMax);
            }

        /**
         * Return the current effective maximum.
         *
         * @return the effective maximum
         */
        public int getEffectiveMax()
            {
            return f_coordinator.getEffectiveMax(f_entry);
            }

        /**
         * Return {@code true} if this pool is automatically sized.
         *
         * @return {@code true} if this pool is automatically sized
         */
        public boolean isAutomatic()
            {
            return f_coordinator.isAutomatic(f_entry);
            }

        /**
         * Return this pool's live workers plus pending start reservations.
         *
         * @return the accounted worker count
         */
        public int getWorkerCount()
            {
            return f_coordinator.getWorkerCount(f_entry);
            }

        /**
         * Return a snapshot containing this registration's latest allocation.
         *
         * @return the coordinator snapshot
         */
        public Snapshot getSnapshot()
            {
            return f_coordinator.snapshot();
            }

        @Override
        public void close()
            {
            f_coordinator.unregister(f_entry);
            }

        private final Coordinator f_coordinator;
        private final Entry       f_entry;
        }

    /**
     * Determine the configured platform thread stack size from the current JVM
     * arguments.
     *
     * @return the configured stack size, or the conservative default if none
     *         can be determined
     */
    static long determineThreadStackSize()
        {
        long cbStack = s_cbThreadStackSize;
        if (cbStack <= 0L)
            {
            cbStack = determineThreadStackSize(getInputArguments());
            s_cbThreadStackSize = cbStack;
            }

        return cbStack;
        }

    /**
     * Determine the configured platform thread stack size from the supplied JVM
     * arguments.
     *
     * @param listArgs  the JVM input arguments
     *
     * @return the configured stack size, or the conservative default if none
     *         can be determined
     */
    static long determineThreadStackSize(List<String> listArgs)
        {
        if (listArgs != null)
            {
            for (String sArg : listArgs)
                {
                long cbStack = parseThreadStackSize(sArg);
                if (cbStack > 0L)
                    {
                    return cbStack;
                    }
                }
            }

        return DEFAULT_THREAD_STACK_SIZE;
        }

    /**
     * Parse a JVM argument that may specify the platform thread stack size.
     *
     * @param sArg  the JVM argument to parse
     *
     * @return the parsed stack size in bytes, or {@code -1} if the argument
     *         does not specify a stack size
     */
    static long parseThreadStackSize(String sArg)
        {
        if (sArg == null || sArg.isEmpty())
            {
            return -1L;
            }

        if (sArg.startsWith("-Xss"))
            {
            String sValue = sArg.substring(4);
            if (sValue.isEmpty())
                {
                return -1L;
                }

            try
                {
                return Base.parseMemorySize(sValue);
                }
            catch (RuntimeException e)
                {
                return -1L;
                }
            }

        if (sArg.startsWith("-XX:ThreadStackSize="))
            {
            String sValue = sArg.substring("-XX:ThreadStackSize=".length());
            if (sValue.isEmpty())
                {
                return -1L;
                }

            try
                {
                return Long.parseLong(sValue) * 1024L;
                }
            catch (RuntimeException e)
                {
                return -1L;
                }
            }

        return -1L;
        }

    /**
     * Return the current JVM input arguments.
     *
     * @return the current JVM input arguments
     */
    static List<String> getInputArguments()
        {
        try
            {
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
            return bean == null ? Collections.emptyList() : bean.getInputArguments();
            }
        catch (Throwable ignored)
            {
            return Collections.emptyList();
            }
        }

    /**
     * Derive a deterministic process-wide budget from stable resource inputs.
     * Negative process-memory or VMA inputs indicate that the corresponding
     * platform limit is unavailable.
     */
    static Budget deriveBudget(long cbMaxHeap, long cbStack, long cbMemoryLimit,
            long cMaxMappings, long cCurrentMappings, int cHardMax, int cProcessors)
        {
        cbStack     = cbStack > 0L ? cbStack : DEFAULT_THREAD_STACK_SIZE;
        cHardMax    = Math.max(1, cHardMax);
        cProcessors = Math.max(1, cProcessors);

        long cbStackBudget;
        if (isFiniteLimit(cbMemoryLimit))
            {
            long cbReserve = Math.max(MIN_NATIVE_RESERVE, cbMemoryLimit / NATIVE_RESERVE_DIVISOR);
            cbStackBudget = subtractFloorZero(subtractFloorZero(cbMemoryLimit, cbMaxHeap), cbReserve);
            }
        else
            {
            cbStackBudget = cbMaxHeap <= 0L ? 0L : cbMaxHeap / FALLBACK_STACK_HEAP_DIVISOR;
            }

        int cMemory = clampCount(cbStackBudget / cbStack, cHardMax);
        int cVma    = -1;
        if (cMaxMappings > 0L && cCurrentMappings >= 0L)
            {
            long cReserve   = Math.max(MIN_VMA_RESERVE, cMaxMappings / VMA_RESERVE_DIVISOR);
            long cAvailable = subtractFloorZero(subtractFloorZero(cMaxMappings, cCurrentMappings), cReserve);
            cVma = clampCount(cAvailable / MAPPINGS_PER_THREAD, cHardMax);
            }

        int cBudget = Math.min(cHardMax, cMemory);
        if (cVma >= 0)
            {
            cBudget = Math.min(cBudget, cVma);
            }
        cBudget = Math.max(1, cBudget);

        return new Budget(cBudget, cMemory, cVma, cHardMax, cProcessors,
                cbMemoryLimit, cbMaxHeap, cbStack);
        }

    private static Budget detectBudget()
        {
        long cbHeap   = Runtime.getRuntime().maxMemory();
        long cbStack  = determineThreadStackSize();
        long cbLimit  = detectMemoryLimit();
        long cMapMax  = readLongFile(new File("/proc/sys/vm/max_map_count"));
        long cMapUsed = countLines(new File("/proc/self/maps"));

        return deriveBudget(cbHeap, cbStack, cbLimit, cMapMax, cMapUsed,
                DEFAULT_DERIVED_THREAD_MAX, Runtime.getRuntime().availableProcessors());
        }

    private static long detectMemoryLimit()
        {
        long cbLimit = readCgroupLimit(new File("/sys/fs/cgroup/memory.max"));
        if (cbLimit < 0L)
            {
            cbLimit = readCgroupLimit(new File("/sys/fs/cgroup/memory/memory.limit_in_bytes"));
            }

        File fileCgroup = new File("/proc/self/cgroup");
        if (cbLimit < 0L && fileCgroup.isFile())
            {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileCgroup)))
                {
                String sLine;
                while ((sLine = reader.readLine()) != null && cbLimit < 0L)
                    {
                    String[] asPart = sLine.split(":", 3);
                    if (asPart.length != 3)
                        {
                        continue;
                        }
                    String sPath = asPart[2].startsWith("/") ? asPart[2].substring(1) : asPart[2];
                    if (asPart[0].equals("0"))
                        {
                        cbLimit = readCgroupLimit(new File(new File("/sys/fs/cgroup", sPath), "memory.max"));
                        }
                    else if (Arrays.asList(asPart[1].split(",")).contains("memory"))
                        {
                        cbLimit = readCgroupLimit(new File(
                                new File("/sys/fs/cgroup/memory", sPath), "memory.limit_in_bytes"));
                        }
                    }
                }
            catch (IOException ignored)
                {
                }
            }
        return cbLimit;
        }

    private static long readCgroupLimit(File file)
        {
        long cbLimit = readLongFile(file);
        return isFiniteLimit(cbLimit) ? cbLimit : -1L;
        }

    private static long readLongFile(File file)
        {
        if (!file.isFile())
            {
            return -1L;
            }
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
            {
            String sValue = reader.readLine();
            if (sValue == null || "max".equals(sValue.trim()))
                {
                return -1L;
                }
            return Long.parseLong(sValue.trim());
            }
        catch (IOException | RuntimeException ignored)
            {
            return -1L;
            }
        }

    private static long countLines(File file)
        {
        if (!file.isFile())
            {
            return -1L;
            }
        long cLines = 0L;
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
            {
            while (reader.readLine() != null)
                {
                cLines++;
                }
            return cLines;
            }
        catch (IOException ignored)
            {
            return -1L;
            }
        }

    private static boolean isFiniteLimit(long cbLimit)
        {
        return cbLimit > 0L && cbLimit < Long.MAX_VALUE / 2L;
        }

    private static long subtractFloorZero(long nLeft, long nRight)
        {
        if (nLeft <= 0L || nRight >= nLeft)
            {
            return 0L;
            }
        return nRight <= 0L ? nLeft : nLeft - nRight;
        }

    private static int clampCount(long cCount, int cHardMax)
        {
        return (int) Math.max(0L, Math.min((long) cHardMax, cCount));
        }

    private static int saturatedAdd(int nLeft, int nRight)
        {
        long nResult = (long) nLeft + nRight;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, nResult));
        }

    // ----- data members --------------------------------------------------

    /**
     * The detected platform thread stack size for this JVM.
     */
    private static volatile long s_cbThreadStackSize;

    // ----- inner class: Snapshot ----------------------------------------

    /**
     * An immutable view of the process-wide platform-worker budget.
     */
    public static final class Snapshot
        {
        Snapshot(Budget budget, int cPools, int cWorkers, int cAllocatedMax, boolean fOvercommitted,
                Map<String, Integer> mapAllocations)
            {
            f_budget          = budget;
            f_cPools          = cPools;
            f_cWorkers        = cWorkers;
            f_cAllocatedMax   = cAllocatedMax;
            f_fOvercommitted  = fOvercommitted;
            f_mapAllocations  = Collections.unmodifiableMap(mapAllocations);
            }

        public int getBudget()
            {
            return f_budget.getWorkerBudget();
            }

        public int getMemoryCap()
            {
            return f_budget.getMemoryCap();
            }

        public int getVmaCap()
            {
            return f_budget.getVmaCap();
            }

        public int getHardCap()
            {
            return f_budget.getHardCap();
            }

        public int getProcessorCount()
            {
            return f_budget.getProcessorCount();
            }

        public long getMemoryLimit()
            {
            return f_budget.getMemoryLimit();
            }

        public long getMaxHeap()
            {
            return f_budget.getMaxHeap();
            }

        public long getThreadStackSize()
            {
            return f_budget.getThreadStackSize();
            }

        public int getPoolCount()
            {
            return f_cPools;
            }

        public int getWorkerCount()
            {
            return f_cWorkers;
            }

        public int getAllocatedWorkerMax()
            {
            return f_cAllocatedMax;
            }

        public boolean isOvercommitted()
            {
            return f_fOvercommitted;
            }

        public Map<String, Integer> getAllocations()
            {
            return f_mapAllocations;
            }

        private final Budget               f_budget;
        private final int                  f_cPools;
        private final int                  f_cWorkers;
        private final int                  f_cAllocatedMax;
        private final boolean              f_fOvercommitted;
        private final Map<String, Integer> f_mapAllocations;
        }

    // ----- inner class: Coordinator -------------------------------------

    /**
     * Process-wide live-pool registry and growth-permit coordinator.
     */
    static final class Coordinator
        {
        Coordinator(Budget budget)
            {
            f_budget = budget;
            }

        synchronized Registration register(Object oIdentity, String sName, Role role,
                int cThreadsMin, int cThreadsMax)
            {
            if (oIdentity == null)
                {
                throw new IllegalArgumentException("Pool identity cannot be null");
                }

            Entry entry = f_mapEntries.get(oIdentity);
            if (entry == null || entry.m_fClosed)
                {
                entry = new Entry(oIdentity, sName, role, cThreadsMin, cThreadsMax);
                f_mapEntries.put(oIdentity, entry);
                }
            else
                {
                entry.update(sName, role, cThreadsMin, cThreadsMax);
                }

            recompute();
            return new Registration(this, entry);
            }

        synchronized int requestCount(Entry entry, int cRequested)
            {
            ensureOpen(entry);
            if (cRequested < 1)
                {
                throw new IllegalArgumentException("Requested number of threads must be greater than 0");
                }
            cRequested = Math.max(entry.m_cMin, cRequested);

            recompute();

            int cUsage = entry.getUsage();
            if (cRequested <= cUsage)
                {
                entry.m_cReserved = Math.max(0, cRequested - entry.m_cCurrent);
                return cRequested;
                }

            int cGranted;
            if (entry.isAutomatic())
                {
                int cAvailable = Math.max(0, f_budget.getWorkerBudget() - getWorkerUsage());
                int cCeiling   = Math.max(entry.m_cMin, entry.m_cEffectiveMax);
                cGranted       = Math.min(cRequested, Math.min(cCeiling, saturatedAdd(cUsage, cAvailable)));
                }
            else
                {
                cGranted = Math.min(cRequested, entry.m_cMax);
                }

            cGranted = Math.max(entry.m_cMin, cGranted);
            entry.m_cReserved = Math.max(0, cGranted - entry.m_cCurrent);
            recompute();
            return cGranted;
            }

        synchronized void workersStarted(Entry entry, int cStarted)
            {
            ensureOpen(entry);
            if (cStarted > 0)
                {
                int cMove = Math.min(cStarted, entry.m_cReserved);
                entry.m_cReserved -= cMove;
                entry.m_cCurrent   = saturatedAdd(entry.m_cCurrent, cStarted);
                recompute();
                }
            }

        synchronized void workersFailed(Entry entry, int cFailed)
            {
            if (!entry.m_fClosed && cFailed > 0)
                {
                entry.m_cReserved = Math.max(0, entry.m_cReserved - cFailed);
                recompute();
                }
            }

        synchronized int workersStopped(Entry entry, int cStopped)
            {
            if (!entry.m_fClosed && cStopped > 0)
                {
                entry.m_cCurrent = Math.max(0, entry.m_cCurrent - cStopped);
                recompute();
                }
            return entry.getUsage();
            }

        synchronized void update(Entry entry, Role role, int cThreadsMin, int cThreadsMax)
            {
            ensureOpen(entry);
            entry.update(entry.m_sName, role, cThreadsMin, cThreadsMax);
            recompute();
            }

        synchronized int getEffectiveMax(Entry entry)
            {
            ensureOpen(entry);
            recompute();
            return entry.m_cEffectiveMax;
            }

        synchronized boolean isAutomatic(Entry entry)
            {
            ensureOpen(entry);
            return entry.isAutomatic();
            }

        synchronized int getWorkerCount(Entry entry)
            {
            ensureOpen(entry);
            return entry.getUsage();
            }

        synchronized void unregister(Entry entry)
            {
            if (!entry.m_fClosed)
                {
                entry.m_fClosed = true;
                f_mapEntries.remove(entry.f_oIdentity);
                recompute();
                }
            }

        synchronized Snapshot snapshot()
            {
            recompute();
            Map<String, Integer> map = new LinkedHashMap<>();
            int cAllocatedMax = 0;
            for (Entry entry : sortedEntries())
                {
                map.put(entry.m_sName, entry.m_cEffectiveMax);
                cAllocatedMax = saturatedAdd(cAllocatedMax,
                        entry.isAutomatic() ? entry.m_cEffectiveMax : entry.getUsage());
                }
            return new Snapshot(f_budget, f_mapEntries.size(), getWorkerUsage(), cAllocatedMax,
                    m_fOvercommitted, map);
            }

        private void ensureOpen(Entry entry)
            {
            if (entry.m_fClosed || f_mapEntries.get(entry.f_oIdentity) != entry)
                {
                throw new IllegalStateException("Daemon-pool registration is closed");
                }
            }

        private int getWorkerUsage()
            {
            int cWorkers = 0;
            for (Entry entry : f_mapEntries.values())
                {
                cWorkers = saturatedAdd(cWorkers, entry.getUsage());
                }
            return cWorkers;
            }

        private List<Entry> sortedEntries()
            {
            List<Entry> list = new ArrayList<>(f_mapEntries.values());
            list.sort(ENTRY_COMPARATOR);
            return list;
            }

        private void recompute()
            {
            List<Entry> listAuto = new ArrayList<>();
            int         cExplicit = 0;
            int         cMin      = 0;

            for (Entry entry : f_mapEntries.values())
                {
                if (entry.isAutomatic())
                    {
                    listAuto.add(entry);
                    cMin = saturatedAdd(cMin, entry.m_cMin);
                    }
                else
                    {
                    entry.m_cEffectiveMax = entry.m_cMax;
                    cExplicit = saturatedAdd(cExplicit, entry.getUsage());
                    }
                }

            allocate(f_budget.getWorkerBudget(), f_budget.getProcessorCount(), cExplicit, listAuto);
            m_fOvercommitted = saturatedAdd(cExplicit, cMin) > f_budget.getWorkerBudget()
                    || getWorkerUsage() > f_budget.getWorkerBudget();
            }

        private final Budget                    f_budget;
        private final IdentityHashMap<Object, Entry> f_mapEntries = new IdentityHashMap<>();
        private boolean                         m_fOvercommitted;
        }

    // ----- deterministic allocator -------------------------------------

    static Map<String, Integer> allocate(int cBudget, int cProcessors, PoolSpec... aSpec)
        {
        List<Entry> list = new ArrayList<>();
        int cExplicit = 0;
        for (PoolSpec spec : aSpec)
            {
            Entry entry = new Entry(spec.f_sName, spec.f_sName, spec.f_role,
                    spec.f_cMin, spec.f_cMax);
            entry.m_cCurrent = spec.f_cCurrent;
            if (entry.isAutomatic())
                {
                list.add(entry);
                }
            else
                {
                entry.m_cEffectiveMax = entry.m_cMax;
                cExplicit = saturatedAdd(cExplicit, entry.getUsage());
                }
            }

        allocate(cBudget, cProcessors, cExplicit, list);

        Map<String, Integer> map = new LinkedHashMap<>();
        for (PoolSpec spec : aSpec)
            {
            Entry entry = findEntry(list, spec.f_sName);
            map.put(spec.f_sName, entry == null ? spec.f_cMax : entry.m_cEffectiveMax);
            }
        return map;
        }

    private static Entry findEntry(List<Entry> list, String sName)
        {
        for (Entry entry : list)
            {
            if (entry.m_sName.equals(sName))
                {
                return entry;
                }
            }
        return null;
        }

    private static void allocate(int cBudget, int cProcessors, int cExplicit, List<Entry> listAuto)
        {
        cBudget     = Math.max(1, cBudget);
        cProcessors = Math.max(1, cProcessors);
        listAuto.sort(ENTRY_COMPARATOR);

        int cAllocated = cExplicit;
        for (Entry entry : listAuto)
            {
            entry.m_cEffectiveMax = entry.m_cMin;
            cAllocated = saturatedAdd(cAllocated, entry.m_cMin);
            }

        int cRemaining = Math.max(0, cBudget - cAllocated);
        while (cRemaining-- > 0)
            {
            Entry selected = null;
            for (Entry entry : listAuto)
                {
                if (entry.m_cEffectiveMax >= entry.getProcessorCeiling(cProcessors, cBudget))
                    {
                    continue;
                    }

                if (selected == null || compareAllocation(entry, selected) < 0)
                    {
                    selected = entry;
                    }
                }

            if (selected == null)
                {
                break;
                }
            selected.m_cEffectiveMax++;
            }
        }

    private static int compareAllocation(Entry entryA, Entry entryB)
        {
        long cGrowthA = entryA.m_cEffectiveMax - entryA.m_cMin;
        long cGrowthB = entryB.m_cEffectiveMax - entryB.m_cMin;
        long nLeft    = cGrowthA * entryB.getWeight();
        long nRight   = cGrowthB * entryA.getWeight();
        int  nCompare = Long.compare(nLeft, nRight);
        return nCompare == 0 ? ENTRY_COMPARATOR.compare(entryA, entryB) : nCompare;
        }

    static final class PoolSpec
        {
        PoolSpec(String sName, Role role, int cMin)
            {
            this(sName, role, cMin, Integer.MAX_VALUE, 0);
            }

        PoolSpec(String sName, Role role, int cMin, int cMax, int cCurrent)
            {
            f_sName    = sName;
            f_role     = role;
            f_cMin     = cMin;
            f_cMax     = cMax;
            f_cCurrent = cCurrent;
            }

        private final String f_sName;
        private final Role   f_role;
        private final int    f_cMin;
        private final int    f_cMax;
        private final int    f_cCurrent;
        }

    private static final class Entry
        {
        Entry(Object oIdentity, String sName, Role role, int cMin, int cMax)
            {
            f_oIdentity = oIdentity;
            update(sName, role, cMin, cMax);
            }

        void update(String sName, Role role, int cMin, int cMax)
            {
            m_sName = sName == null || sName.isEmpty() ? "<unnamed>" : sName;
            m_role  = role == null ? Role.SERVICE : role;
            m_cMin  = Math.max(1, cMin);
            m_cMax  = Math.max(m_cMin, cMax);
            }

        boolean isAutomatic()
            {
            return m_cMax == Integer.MAX_VALUE;
            }

        int getUsage()
            {
            return saturatedAdd(m_cCurrent, m_cReserved);
            }

        int getWeight()
            {
            long nWeight = (long) m_cMin * m_role.getWeightFactor();
            return (int) Math.min(Integer.MAX_VALUE, nWeight);
            }

        int getProcessorCeiling(int cProcessors, int cBudget)
            {
            long cCeiling = (long) cProcessors * m_role.getProcessorMultiple();
            return Math.max(m_cMin, (int) Math.min((long) cBudget, cCeiling));
            }

        private final Object f_oIdentity;
        private String       m_sName;
        private Role         m_role;
        private int          m_cMin;
        private int          m_cMax;
        private int          m_cCurrent;
        private int          m_cReserved;
        private int          m_cEffectiveMax;
        private boolean      m_fClosed;
        }

    // ----- inner class: Budget ------------------------------------------

    static final class Budget
        {
        Budget(int cWorkers, int cMemory, int cVma, int cHard, int cProcessors,
                long cbMemoryLimit, long cbMaxHeap, long cbStack)
            {
            f_cWorkers       = Math.max(1, cWorkers);
            f_cMemory        = cMemory;
            f_cVma           = cVma;
            f_cHard          = cHard;
            f_cProcessors    = Math.max(1, cProcessors);
            f_cbMemoryLimit  = cbMemoryLimit;
            f_cbMaxHeap      = cbMaxHeap;
            f_cbStack        = cbStack;
            }

        int getWorkerBudget()    { return f_cWorkers; }
        int getMemoryCap()       { return f_cMemory; }
        int getVmaCap()          { return f_cVma; }
        int getHardCap()         { return f_cHard; }
        int getProcessorCount()  { return f_cProcessors; }
        long getMemoryLimit()    { return f_cbMemoryLimit; }
        long getMaxHeap()        { return f_cbMaxHeap; }
        long getThreadStackSize(){ return f_cbStack; }

        private final int  f_cWorkers;
        private final int  f_cMemory;
        private final int  f_cVma;
        private final int  f_cHard;
        private final int  f_cProcessors;
        private final long f_cbMemoryLimit;
        private final long f_cbMaxHeap;
        private final long f_cbStack;
        }

    // ----- constants -----------------------------------------------------

    /**
     * The conservative default platform thread stack size.
     */
    static final long DEFAULT_THREAD_STACK_SIZE = 1024L * 1024L;

    /**
     * The built-in hard ceiling for automatically derived daemon-pool maxima.
     */
    static final int DEFAULT_DERIVED_THREAD_MAX = 2048;

    /** Aggregate stack allowance when no stable process limit is available. */
    static final int FALLBACK_STACK_HEAP_DIVISOR = 4;

    /** Minimum native-memory reserve under a stable process limit. */
    static final long MIN_NATIVE_RESERVE = 256L * 1024L * 1024L;

    /** Reserve one fifth of a stable process-memory limit for native use. */
    static final int NATIVE_RESERVE_DIVISOR = 5;

    /** Minimum number of Linux VMAs retained outside managed workers. */
    static final int MIN_VMA_RESERVE = 8192;

    /** Reserve one quarter of the Linux VMA limit. */
    static final int VMA_RESERVE_DIVISOR = 4;

    /** Conservative stack-plus-guard mapping cost per platform worker. */
    static final int MAPPINGS_PER_THREAD = 2;

    /** Stable allocator order, independent of pool registration order. */
    private static final Comparator<Entry> ENTRY_COMPARATOR = (entryA, entryB) ->
        {
        int nCompare = entryA.m_sName.compareTo(entryB.m_sName);
        return nCompare == 0
                ? Integer.compare(System.identityHashCode(entryA.f_oIdentity),
                                  System.identityHashCode(entryB.f_oIdentity))
                : nCompare;
        };

    /** The process-wide coordinator. */
    private static final Coordinator COORDINATOR = new Coordinator(detectBudget());
    }

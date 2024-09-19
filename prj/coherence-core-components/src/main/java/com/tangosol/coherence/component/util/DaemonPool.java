
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.DaemonPool

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.util.queue.ConcurrentQueue;

import com.oracle.coherence.common.base.Associated;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.base.Notifier;

import com.oracle.coherence.common.util.AssociationPile;
import com.oracle.coherence.common.util.ConcurrentAssociationPile;
import com.oracle.coherence.common.util.Timers;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.internal.util.VirtualThreads;

import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian;
import com.tangosol.net.PriorityTask;

import com.tangosol.run.component.EventDeathException;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Gate;
import com.tangosol.util.ThreadGateLite;

import java.lang.reflect.Array;

import java.util.Iterator;
import java.util.Set;

/**
 * DaemonPool is a class thread pool implementation for processing queued
 * operations on one or more daemon threads.
 * 
 * The designable properties are:
 *     AutoStart
 *     DaemonCount
 * 
 * The simple API for the DaemonPool is:
 *     public void start()
 *     public boolean isStarted()
 *     public void add(Runnable task)
 *     public void stop()
 * 
 * The advanced API for the DaemonPool is:
 *     DaemonCount property
 *     Daemons property
 *     Queues property
 *     ThreadGroup property
 * 
 * The DaemonPool is composed of two key components:
 * 
 * 1) An array of WorkSlot components that may or may not share Queues with
 * other WorkSlots. 
 * 
 * 2) An array of Daemon components feeding off the Queues. This collection is
 * accessed by the DaemonCount and Daemons properties, and is managed by the
 * DaemonCount mutator.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class DaemonPool
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.internal.util.DaemonPool,
                   com.tangosol.net.Guardian
    {
    // ---- Fields declarations ----
    
    /**
     * Property AbandonThreshold
     *
     * The absolute value of this property specifies the number of times an
     * attempt to interrupt is made before a daemon thread is abandoned. A
     * negative value means that the abandoned thread should be stopped using
     * the [deprecated] Thread.stop() API. Could be configured via undocumented
     *  "coherence.pool.interruptcount" property.
     */
    private int __m_AbandonThreshold;
    
    /**
     * Property AutoStart
     *
     * Design-time property: Set to true to automatically start the daemon
     * threads when the DaemonPool is instantiated.
     */
    private boolean __m_AutoStart;
    
    /**
     * Property DAEMON_ABANDONED
     *
     * Indicates an abandoned daemon.
     */
    public static final int DAEMON_ABANDONED = 2;
    
    /**
     * Property DAEMON_NONPOOLED
     *
     * Indicates a non-pooled daemon
     */
    public static final int DAEMON_NONPOOLED = 1;
    
    /**
     * Property DAEMON_STANDARD
     *
     * Indicates a standard pooled daemon.
     */
    public static final int DAEMON_STANDARD = 0;
    
    /**
     * Property DaemonCount
     *
     * The number of Daemon threads that exist, if the pool has been started,
     * or the number of Daemon threads that will be created, if the pool has
     * not yet been started. This property can be set at design time or
     * programmatically before the pool is started to configure the number of
     * threads that will be created when the pool starts. Furthermore, this
     * property can be set after the pool is started to change (up or down) the
     * number of worker threads.
     */
    private int __m_DaemonCount;
    
    /**
     * Property DaemonCountMax
     *
     * The maximum number of Daemon threads that can exist.
     */
    private int __m_DaemonCountMax;
    
    /**
     * Property DaemonCountMin
     *
     * The minimum number of Daemon threads that can exist.
     */
    private int __m_DaemonCountMin;
    
    /**
     * Property DaemonIndex
     *
     * The index used to suffix the standard daemon names. The index increases
     * monotonously, so when new daemons are started (e.g. to replace abandoned
     * daemons), they will all have unique names.
     */
    private java.util.concurrent.atomic.AtomicInteger __m_DaemonIndex;
    
    /**
     * Property Daemons
     *
     * An array of currently active pooled Daemon objects. This property is
     * assigned only if the pool has been started. Since this array is modified
     * very infrequently (most of the time - never), we use the copy-on-update
     * pattern.
     */
    private transient DaemonPool.Daemon[] __m_Daemons;
    
    /**
     * Property Dependencies
     *
     * The external dependencies for this DaemonPool.
     */
    private com.tangosol.internal.util.DaemonPoolDependencies __m_Dependencies;
    
    /**
     * Property HungThreshold
     *
     * The amount of time in milliseconds that a task can execute before it is
     * considered "hung".
     * 
     * Note that a posted task that has not yet started is never considered as
     * hung.
     */
    private long __m_HungThreshold;
    
    /**
     * Property InternalGuardian
     *
     * The Guardian that this Daemon's implementation delegates to.
     */
    private transient com.tangosol.net.Guardian __m_InternalGuardian;
    
    /**
     * Property InTransition
     *
     * Indicates that the pool is either growing or shrinking.
     * 
     * @volatile
     */
    private volatile transient boolean __m_InTransition;
    
    /**
     * Property Name
     *
     * The name of this DaemonPool.
     */
    private String __m_Name;
    
    /**
     * Property Queues
     *
     * An array of currently active queues. This property is assigned only if
     * the pool has been started. Since this array is modified very
     * infrequently (most of the time - never), we use the copy-on-update
     * pattern.
     * 
     * Note: the number of queues is equal to the minimum of the number of work
     * slots and the number of daemons.
     */
    private transient com.oracle.coherence.common.util.AssociationPile[] __m_Queues;
    
    /**
     * Property ResizeTask
     *
     * The periodic "resize" task.
     * 
     * @volatile
     */
    private volatile DaemonPool.ResizeTask __m_ResizeTask;
    
    /**
     * Property ScheduledTasks
     *
     * A set of Disposables (representing tasks) which have been scheduled for
     * delayed execution, and which have not yet been executed.  These tasks
     * need to be cancelled when the pool stops otherwise they will leak
     * whatever then reference until the point at which they are executed.
     * 
     * All access to this set must occur under synchronization on the set.
     */
    private java.util.Set __m_ScheduledTasks;
    
    /**
     * Property Started
     *
     * This property returns true once the DaemonPool as successfully been
     * started via the start() method until the DaemonPool is stopped via the
     * stop() method. Otherwise it is false.
     * 
     * @volatile
     */
    private volatile transient boolean __m_Started;
    
    /**
     * Property STATS_MONITOR
     *
     * Object monitor used to access/update statistics atomically.
     */
    public final Object STATS_MONITOR;
    
    /**
     * Property StatsAbandonedCount
     *
     * The total number of abandoned Daemon threads.
     * 
     * Note: this property is purposely not reset when stats are reset.
     */
    private transient int __m_StatsAbandonedCount;
    
    /**
     * Property StatsActiveMillis
     *
     * The total number of milliseconds spent by all Daemon threads while
     * executing tasks since the last time the statistics were reset.
     * 
     * Note: this value could be greater than the time elapsed since each
     * daemon adds its own processing time when working in parallel.
     */
    private transient long __m_StatsActiveMillis;
    
    /**
     * Property StatsHungCount
     *
     * The total number of currently executing hung tasks.
     * 
     * Note: this property is purposely not reset when stats are reset.
     */
    private transient int __m_StatsHungCount;
    
    /**
     * Property StatsHungDuration
     *
     * The longest currently executing hung task duration (in milliseconds).
     * 
     * Note: this property is purposely not reset when stats are reset.
     */
    private transient long __m_StatsHungDuration;
    
    /**
     * Property StatsHungTaskId
     *
     * The id of the longest currently executing hung task. If the task does
     * not implement javax.util.concurrent.Identifiable (requires JDK 1.5),
     * return task.toString().
     * 
     * Note: this property is purposely not reset when stats are reset.
     */
    private transient String __m_StatsHungTaskId;
    
    /**
     * Property StatsLastBacklog
     *
     * The last captured value of the Backlog property since the last time the
     * statistics were reset.
     */
    private int __m_StatsLastBacklog;
    
    /**
     * Property StatsLastResetMillis
     *
     * The last time stats were reset.
     */
    private transient long __m_StatsLastResetMillis;
    
    /**
     * Property StatsLastResizeMillis
     *
     * The last time the pool was resized.
     */
    private transient long __m_StatsLastResizeMillis;
    
    /**
     * Property StatsLastTaskAddCount
     *
     * The last captured value of the StatsTaskAddCount since the last time the
     * statistics were reset.
     */
    private long __m_StatsLastTaskAddCount;
    
    /**
     * Property StatsLastTaskCount
     *
     * The last captured value of the StatsTaskCount since the last time the
     * statistics were reset.
     */
    private long __m_StatsLastTaskCount;
    
    /**
     * Property StatsMaxBacklog
     *
     * A maximum TaskBacklog value since the last time the statistics were
     * reset.
     */
    private transient int __m_StatsMaxBacklog;
    
    /**
     * Property StatsTaskAddCount
     *
     * The total number of tasks added to the pool since the last time the
     * statistics were reset.
     */
    private transient java.util.concurrent.atomic.AtomicLong __m_StatsTaskAddCount;
    
    /**
     * Property StatsTaskCount
     *
     * The total number of tasks executed by Daemon threads since the last time
     * the statistics were reset.
     */
    private transient long __m_StatsTaskCount;
    
    /**
     * Property StatsTimeoutCount
     *
     * The total number of timed-out tasks since the last time the statistics
     * were reset.
     */
    private transient int __m_StatsTimeoutCount;
    
    /**
     * Property TaskTimeout
     *
     * A default timeout value for PriorityTasks that don't explicitly specify
     * the execution timeout value.
     * This property value is injected into the DaemonPool by its container
     * (see Service.onDependencies).
     * 
     * @see #instantiateWrapperTask
     */
    private long __m_TaskTimeout;
    
    /**
     * Property ThreadGroup
     *
     * Specifies the ThreadGroup within which the daemons for this pool will be
     * created. If not specified, the current Thread's ThreadGroup will be
     * used.
     * 
     * This property can only be set at runtime, and must be configured before
     * start() is invoked to cause the daemon threads to be created within the
     * specified ThreadGroup.
     */
    private transient ThreadGroup __m_ThreadGroup;
    
    /**
     * Property ThreadPriority
     *
     * The Thread priority used by all threads in the Daemon pool.
     */
    private int __m_ThreadPriority;
    
    /**
     * Property WorkSlot
     *
     * An array of WorkSlot objects. This property is assigned only if the pool
     * was started.
     */
    private transient DaemonPool.WorkSlot[] __m_WorkSlot;
    
    /**
     * Property WorkSlotCount
     *
     * The number of WorkSlots. This number is calculated once based on the
     * number of processors and never changes.
     */
    private int __m_WorkSlotCount;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Daemon", DaemonPool.Daemon.get_CLASS());
        __mapChildren.put("ResizeTask", DaemonPool.ResizeTask.get_CLASS());
        __mapChildren.put("ScheduleTask", DaemonPool.ScheduleTask.get_CLASS());
        __mapChildren.put("StartTask", DaemonPool.StartTask.get_CLASS());
        __mapChildren.put("StopTask", DaemonPool.StopTask.get_CLASS());
        __mapChildren.put("WorkSlot", DaemonPool.WorkSlot.get_CLASS());
        __mapChildren.put("WrapperTask", DaemonPool.WrapperTask.get_CLASS());
        }
    
    // Default constructor
    public DaemonPool()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public DaemonPool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        STATS_MONITOR = new java.lang.Object();
        
        if (fInit)
            {
            __init();
            }
        }
    
    // Main initializer
    public void __init()
        {
        // private initialization
        __initPrivate();
        
        // state initialization: public and protected properties
        try
            {
            setAbandonThreshold(8);
            setDaemonCountMax(2147483647);
            setDaemonCountMin(1);
            setScheduledTasks(new java.util.HashSet());
            setStatsTaskAddCount(new java.util.concurrent.atomic.AtomicLong());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m_DaemonIndex = new java.util.concurrent.atomic.AtomicInteger();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // Getter for virtual constant RecoveryDelay
    public long getRecoveryDelay()
        {
        return 50L;
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.DaemonPool();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    /**
     * Adds a Runnable task to the queue of items to be handled by the thread
    * pool.
    * 
    * @param task  the Runnable task to execute (call the run() method of) on
    * one of the daemon threads
     */
    public void add(Runnable task)
        {
        add(task, false);
        }
    
    /**
     * Adds a Runnable task to the queue of items to be handled by the thread
    * pool.
    * 
    * @param task  the Runnable task to execute (call the run() method of) on
    * one of the daemon threads
    * @param fAggressiveTimeout  if true, the timeout interval could be counted
    * from the time the task has been posted rather than started execution
     */
    public void add(Runnable task, boolean fAggressiveTimeout)
        {
        // import com.oracle.coherence.common.base.Associated;
        // import com.tangosol.net.PriorityTask;
        // import com.tangosol.util.Base;
        
        if (isStarted())
            {
            DaemonPool.WrapperTask taskWrapper = instantiateWrapperTask(task, fAggressiveTimeout);
            int          iPriority   = taskWrapper.getPriority();
        
            if (iPriority == PriorityTask.SCHEDULE_IMMEDIATE ||
                iPriority == PriorityTask.SCHEDULE_FIRST)
                {
                // Even if there are idle daemons, there appears to be no
                // simple and efficient way to guarantee an immediate execution.
                // Create a new short-lived thread for this task;
                // it will not contribute to any statistical data    
                DaemonPool.Daemon daemon = instantiateDaemon(DAEMON_NONPOOLED, null);
                daemon.setWrapperTask(taskWrapper);
                daemon.start();
        
                return;
                }
        
            Object oAssoc = taskWrapper.getAssociatedKey();
            long   cAdded = taskWrapper.isManagementTask()
                ? getStatsTaskAddCount().get()
                : getStatsTaskAddCount().getAndIncrement();
        
            DaemonPool.WorkSlot slot = oAssoc == null
                ? findMinBacklogSlot((int) cAdded)
                : getWorkSlot(Base.mod(oAssoc.hashCode(), getWorkSlotCount()));
            slot.add(taskWrapper);
            }
        else
            {
            task.run();
            }
        }
    
    /**
     * Check the running tasks to see if any are "hung", and update the stats
    * accordingly.
     */
    public void checkHungTasks()
        {
        // import com.tangosol.util.Base;
        
        // update the backlog statistics
        getBacklog();
        
        long cHungThreshold = getHungThreshold();
        if (cHungThreshold == 0)
            {
            return;
            }
        
        long         ldtNow      = Base.getSafeTimeMillis();
        DaemonPool.WrapperTask taskLongest = null;
        long         cLongest    = -1L;
        int          cHung       = 0;
        DaemonPool.Daemon[]    aDaemon     = getDaemons();
        
        for (int i = 0, c = aDaemon == null ? 0 : aDaemon.length; i < c; i++)
            {
            DaemonPool.Daemon      daemon  = aDaemon[i];
            DaemonPool.WrapperTask wrapper = daemon.getWrapperTask();
        
            if (wrapper != null)
                {
                long ldtStart  = wrapper.getStartTime();
                long cDuration = ldtNow - ldtStart;
                if (cDuration > cHungThreshold)
                    {
                    if (cDuration > cLongest)
                        {
                        cLongest    = cDuration;
                        taskLongest = wrapper;
                        }
                    ++cHung;
                    }
                }
            }
        
        synchronized (STATS_MONITOR)
            {
            if (cHung == 0)
                {
                setStatsHungCount(0);
                setStatsHungDuration(0L);
                setStatsHungTaskId("");
                }
            else
                {
                setStatsHungCount(cHung);
                setStatsHungDuration(cLongest);
                setStatsHungTaskId(taskLongest.getTaskId());
                }
            }
        }
    
    /**
     * Create a new Default dependencies object by copying the supplies
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone, producing their variant of the
    * dependencies interface.
    * 
    * @return the cloned dependencies
     */
    protected com.tangosol.internal.util.DefaultDaemonPoolDependencies cloneDependencies(com.tangosol.internal.util.DaemonPoolDependencies deps)
        {
        // import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
        
        return new DefaultDaemonPoolDependencies(deps);
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    public void configure(com.tangosol.run.xml.XmlElement xml)
        {
        throw new UnsupportedOperationException();
        }
    
    /**
     * Create a new array from an old one by adding the specified element.
     */
    public static Object[] copyOnAdd(Object[] ao, Object oAdd)
        {
        // import java.lang.reflect.Array;
        
        int      c     = ao.length;
        Object[] aoNew = (Object[]) Array.newInstance(ao.getClass().getComponentType(), c+1);
        
        System.arraycopy(ao, 0, aoNew, 0, c);
        aoNew[c] = oAdd;
        
        return aoNew;
        }
    
    /**
     * Create a new array from an old one by removing the specified element.
     */
    public static Object[] copyOnRemove(Object[] ao, Object oRemove)
        {
        // import java.lang.reflect.Array;
        
        for (int i = 0, c = ao.length; i < c; i++)
            {
            if (ao[i] == oRemove)
                {
                Object[] aoNew = (Object[]) Array.newInstance(ao.getClass().getComponentType(), c-1);
        
                if (i > 0)
                    {
                    // head
                    System.arraycopy(ao, 0, aoNew, 0, i);
                    }
        
                if (i < c-1)
                    {
                    // tail
                    System.arraycopy(ao, i+1, aoNew, i, c-1-i);
                    }
        
                return aoNew;
                }
            }
        
        return ao;
        }
    
    /**
     * Find a WorkSlot that has its queue associated with the largest number of
    * daemons. This method should only be called while holding synchronization
    * on the DaemonPool.
     */
    protected DaemonPool.WorkSlot findMaxDaemonSharingSlot()
        {
        int[] acShare = new int[getQueues().length];
        
        DaemonPool.Daemon[] aDaemon = getDaemons();
        for (int i = 0, c = aDaemon.length; i < c; i++)
            {
            int iQ = indexOf(aDaemon[i].getQueue());
            _assert(iQ >= 0);
        
            acShare[iQ]++;
            }
        
        int cMax  = 1;  // the maximum sharing count should be greater than one
        int iSlot = -1; // the index of the slot with the shared queue
        
        for (int i = 0, c = getWorkSlotCount(); i < c; i++)
            {
            DaemonPool.WorkSlot slot = getWorkSlot(i);
        
            int iQ = indexOf(slot.getQueue());
            _assert(iQ >= 0);
        
            int cShare = acShare[iQ];
            if (cShare > cMax)
                {
                cMax  = cShare;
                iSlot = i;
                }
            }
        
        return iSlot == -1 ? null : getWorkSlot(iSlot);
        }
    
    /**
     * Find a WorkSlot that shares its queue with the largest number of other
    * slots. This method should only be called while holding synchronization on
    * the DaemonPool.
     */
    protected DaemonPool.WorkSlot findMaxQueueSharingSlot()
        {
        int[] acShare = new int[getQueues().length];
        int   cMax    = 1;  // the maximum sharing count should be greater than one
        int   iSlot   = -1; // the index of the slot with the shared queue
        
        for (int i = 0, c = getWorkSlotCount(); i < c; i++)
            {
            DaemonPool.WorkSlot slot = getWorkSlot(i);
        
            int iQ = indexOf(slot.getQueue());
            _assert(iQ >= 0);
        
            int cShare = ++acShare[iQ];
            if (cShare > cMax)
                {
                cMax  = cShare;
                iSlot = i;
                }
            }
        
        return iSlot == -1 ? null : getWorkSlot(iSlot);
        }
    
    /**
     * Find a WorkSlot that has the smallest backlog.
    * 
    * @param iSeed  the number to use as a starting point for the search; used
    * to provide an initial load balancing
     */
    protected DaemonPool.WorkSlot findMinBacklogSlot(int iSeed)
        {
        // import com.tangosol.util.Base;
        
        DaemonPool.WorkSlot slotMin = null;
        for (int i = 0, cSlots = getWorkSlotCount(), cMinSize = Integer.MAX_VALUE; i < cSlots; i++)
            {
            DaemonPool.WorkSlot slot = getWorkSlot(Base.mod(iSeed + i, cSlots));
        
            int cSize = slot.getQueue().size();
            if (cSize == 0)
                {
                return slot;
                }
        
            if (cSize < cMinSize)
                {
                cMinSize = cSize;
                slotMin  = slot;
                }
            }
        
        return slotMin;
        }
    
    /**
     * Find a queue that is associated with the smallest number of daemons. This
    * method should only be called while holding synchronization on the
    * DaemonPool.
     */
    protected com.oracle.coherence.common.util.AssociationPile findMinDaemonSharingQueue()
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        
        AssociationPile[] aQueue  = getQueues();
        int               cQueues = aQueue.length;
        int[]             acShare = new int[cQueues];
        
        DaemonPool.Daemon[] aDaemon = getDaemons();
        for (int i = 0, c = aDaemon.length; i < c; i++)
            {
            int iQ = indexOf(aDaemon[i].getQueue());
            _assert(iQ >= 0);
        
            acShare[iQ]++;
            }
        
        int cMin   = Integer.MAX_VALUE; // the minimum sharing count
        int iQueue = -1;                // the index of the least shared queue
        
        for (int iQ = 0; iQ < cQueues; iQ++)
            {
            if (acShare[iQ] < cMin)
                {
                cMin   = acShare[iQ];
                iQueue = iQ;
                }
            }
        return aQueue[iQueue];
        }
    
    /**
     * Find a queue that is associated with the smallest number of WorkSlots and
    * is different from the specified queue. This method should only be called
    * while holding synchronization on the DaemonPool.
     */
    protected com.oracle.coherence.common.util.AssociationPile findMinSlotSharingQueue(com.oracle.coherence.common.util.AssociationPile queueExcept)
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        
        AssociationPile[] aQueue  = getQueues();
        int               cQueues = aQueue.length;
        int[]             acShare = new int[cQueues];
        
        for (int i = 0, c = getWorkSlotCount(); i < c; i++)
            {
            DaemonPool.WorkSlot slot = getWorkSlot(i);
        
            int iQ = indexOf(slot.getQueue());
            _assert(iQ >= 0);
        
            acShare[iQ]++;
            }
        
        int cMin   = Integer.MAX_VALUE; // the minimum sharing count
        int iQueue = -1;                // the index of the least shared queue
        
        for (int iQ = 0; iQ < cQueues; iQ++)
            {
            if (acShare[iQ] < cMin && aQueue[iQ] != queueExcept)
                {
                cMin   = acShare[iQ];
                iQueue = iQ;
                }
            }
        return iQueue == -1 ? null : aQueue[iQueue];
        }
    
    /**
     * Find a WorkSlot associated with the specified queue.
     */
    protected DaemonPool.WorkSlot findSlot(com.oracle.coherence.common.util.AssociationPile queue)
        {
        for (int i = 0, c = getWorkSlotCount(); i < c; i++)
            {
            DaemonPool.WorkSlot slot = getWorkSlot(i);
        
            if (slot.getQueue() == queue)
                {
                return slot;
                }
            }
        return null;
        }
    
    /**
     * Notify the Daemons that they should report their latest statistics as
    * soon as possible.
     */
    public void flushStats()
        {
        DaemonPool.Daemon[] aDaemon = getDaemons();
        for (int i = 0, c = aDaemon == null ? 0 : aDaemon.length; i < c; i++)
            {
            aDaemon[i].setFlushStats(true);
            }
        }
    
    // Accessor for the property "AbandonThreshold"
    /**
     * Getter for property AbandonThreshold.<p>
    * The absolute value of this property specifies the number of times an
    * attempt to interrupt is made before a daemon thread is abandoned. A
    * negative value means that the abandoned thread should be stopped using
    * the [deprecated] Thread.stop() API. Could be configured via undocumented 
    * "coherence.pool.interruptcount" property.
     */
    public int getAbandonThreshold()
        {
        return __m_AbandonThreshold;
        }
    
    // Accessor for the property "ActiveDaemonCount"
    /**
     * Getter for property ActiveDaemonCount.<p>
    * The number of Daemon threads that are currently executing tasks. This
    * calculated property will produce a "fully correct" result only if called
    * while holding the Queue synchronization monitor.
     */
    public int getActiveDaemonCount()
        {
        Object[] aDaemon = getDaemons();
        int      cActive = 0;
        for (int i = 0, c = aDaemon == null ? 0 : aDaemon.length; i < c; i++)
            {
            DaemonPool.Daemon daemon = (DaemonPool.Daemon) aDaemon[i];
        
            if (daemon.getWrapperTask() != null)
                {
                cActive++;
                }
            }
        return cActive;
        }
    
    // Accessor for the property "Backlog"
    /**
     * Getter for property Backlog.<p>
    * A number of tasks that have been added to the pool, but not yet scheduled
    * for execution.
     */
    public int getBacklog()
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        
        AssociationPile[] aQueue   = getQueues();
        int               cBacklog = 0;
        for (int i = 0, c = aQueue == null ? 0 : aQueue.length; i < c; i++)
            {
            cBacklog += aQueue[i].size();
            }
        
        synchronized (STATS_MONITOR)
            {
            if (cBacklog > getStatsMaxBacklog())
                {
                setStatsMaxBacklog(cBacklog);
                }
            }
        return cBacklog;
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    public ClassLoader getContextClassLoader()
        {
        return null;
        }
    
    // Accessor for the property "DaemonCount"
    /**
     * Getter for property DaemonCount.<p>
    * The number of Daemon threads that exist, if the pool has been started, or
    * the number of Daemon threads that will be created, if the pool has not
    * yet been started. This property can be set at design time or
    * programmatically before the pool is started to configure the number of
    * threads that will be created when the pool starts. Furthermore, this
    * property can be set after the pool is started to change (up or down) the
    * number of worker threads.
     */
    public int getDaemonCount()
        {
        return isStarted() ? getDaemons().length : __m_DaemonCount;
        }
    
    // Accessor for the property "DaemonCountMax"
    /**
     * Getter for property DaemonCountMax.<p>
    * The maximum number of Daemon threads that can exist.
     */
    public int getDaemonCountMax()
        {
        return __m_DaemonCountMax;
        }
    
    // Accessor for the property "DaemonCountMin"
    /**
     * Getter for property DaemonCountMin.<p>
    * The minimum number of Daemon threads that can exist.
     */
    public int getDaemonCountMin()
        {
        return __m_DaemonCountMin;
        }
    
    // Accessor for the property "DaemonIndex"
    /**
     * Getter for property DaemonIndex.<p>
    * The index used to suffix the standard daemon names. The index increases
    * monotonously, so when new daemons are started (e.g. to replace abandoned
    * daemons), they will all have unique names.
     */
    protected java.util.concurrent.atomic.AtomicInteger getDaemonIndex()
        {
        return __m_DaemonIndex;
        }
    
    // Accessor for the property "Daemons"
    /**
     * Getter for property Daemons.<p>
    * An array of currently active pooled Daemon objects. This property is
    * assigned only if the pool has been started. Since this array is modified
    * very infrequently (most of the time - never), we use the copy-on-update
    * pattern.
     */
    public DaemonPool.Daemon[] getDaemons()
        {
        return __m_Daemons;
        }
    
    // From interface: com.tangosol.net.Guardian
    public float getDefaultGuardRecovery()
        {
        // import com.tangosol.net.Guardian;
        
        Guardian guardian = getInternalGuardian();
        return guardian == null ? 0 : guardian.getDefaultGuardRecovery();
        }
    
    // From interface: com.tangosol.net.Guardian
    public long getDefaultGuardTimeout()
        {
        // import com.tangosol.net.Guardian;
        
        Guardian guardian = getInternalGuardian();
        return guardian == null ? 0L : guardian.getDefaultGuardTimeout();
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    // Accessor for the property "Dependencies"
    /**
     * Getter for property Dependencies.<p>
    * The external dependencies for this DaemonPool.
     */
    public com.tangosol.internal.util.DaemonPoolDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    public com.tangosol.net.Guardian getGuardian()
        {
        return this;
        }
    
    // Accessor for the property "HungThreshold"
    /**
     * Getter for property HungThreshold.<p>
    * The amount of time in milliseconds that a task can execute before it is
    * considered "hung".
    * 
    * Note that a posted task that has not yet started is never considered as
    * hung.
     */
    public long getHungThreshold()
        {
        return __m_HungThreshold;
        }
    
    // Accessor for the property "InternalGuardian"
    /**
     * Getter for property InternalGuardian.<p>
    * The Guardian that this Daemon's implementation delegates to.
     */
    protected com.tangosol.net.Guardian getInternalGuardian()
        {
        // import Component;
        // import com.tangosol.net.Guardian;
        
        Guardian guardian = __m_InternalGuardian;
        if (guardian == null)
           {
           Component parent = get_Parent();
           if (parent instanceof Guardian)
               {
               setInternalGuardian(guardian = (Guardian) parent);
               }
           }
        
        return guardian;
        }
    
    // Accessor for the property "Name"
    /**
     * Getter for property Name.<p>
    * The name of this DaemonPool.
     */
    public String getName()
        {
        return __m_Name;
        }
    
    // Accessor for the property "Queues"
    /**
     * Getter for property Queues.<p>
    * An array of currently active queues. This property is assigned only if
    * the pool has been started. Since this array is modified very infrequently
    * (most of the time - never), we use the copy-on-update pattern.
    * 
    * Note: the number of queues is equal to the minimum of the number of work
    * slots and the number of daemons.
     */
    public com.oracle.coherence.common.util.AssociationPile[] getQueues()
        {
        return __m_Queues;
        }
    
    // Accessor for the property "ResizeTask"
    /**
     * Getter for property ResizeTask.<p>
    * The periodic "resize" task.
    * 
    * @volatile
     */
    public DaemonPool.ResizeTask getResizeTask()
        {
        return __m_ResizeTask;
        }
    
    // Accessor for the property "ScheduledTasks"
    /**
     * Getter for property ScheduledTasks.<p>
    * A set of Disposables (representing tasks) which have been scheduled for
    * delayed execution, and which have not yet been executed.  These tasks
    * need to be cancelled when the pool stops otherwise they will leak
    * whatever then reference until the point at which they are executed.
    * 
    * All access to this set must occur under synchronization on the set.
     */
    public java.util.Set getScheduledTasks()
        {
        return __m_ScheduledTasks;
        }
    
    // Accessor for the property "StatsAbandonedCount"
    /**
     * Getter for property StatsAbandonedCount.<p>
    * The total number of abandoned Daemon threads.
    * 
    * Note: this property is purposely not reset when stats are reset.
     */
    public int getStatsAbandonedCount()
        {
        return __m_StatsAbandonedCount;
        }
    
    // Accessor for the property "StatsActiveMillis"
    /**
     * Getter for property StatsActiveMillis.<p>
    * The total number of milliseconds spent by all Daemon threads while
    * executing tasks since the last time the statistics were reset.
    * 
    * Note: this value could be greater than the time elapsed since each daemon
    * adds its own processing time when working in parallel.
     */
    public long getStatsActiveMillis()
        {
        return __m_StatsActiveMillis;
        }
    
    // Accessor for the property "StatsHungCount"
    /**
     * Getter for property StatsHungCount.<p>
    * The total number of currently executing hung tasks.
    * 
    * Note: this property is purposely not reset when stats are reset.
     */
    public int getStatsHungCount()
        {
        return __m_StatsHungCount;
        }
    
    // Accessor for the property "StatsHungDuration"
    /**
     * Getter for property StatsHungDuration.<p>
    * The longest currently executing hung task duration (in milliseconds).
    * 
    * Note: this property is purposely not reset when stats are reset.
     */
    public long getStatsHungDuration()
        {
        return __m_StatsHungDuration;
        }
    
    // Accessor for the property "StatsHungTaskId"
    /**
     * Getter for property StatsHungTaskId.<p>
    * The id of the longest currently executing hung task. If the task does not
    * implement javax.util.concurrent.Identifiable (requires JDK 1.5), return
    * task.toString().
    * 
    * Note: this property is purposely not reset when stats are reset.
     */
    public String getStatsHungTaskId()
        {
        return __m_StatsHungTaskId;
        }
    
    // Accessor for the property "StatsLastBacklog"
    /**
     * Getter for property StatsLastBacklog.<p>
    * The last captured value of the Backlog property since the last time the
    * statistics were reset.
     */
    public int getStatsLastBacklog()
        {
        return __m_StatsLastBacklog;
        }
    
    // Accessor for the property "StatsLastResetMillis"
    /**
     * Getter for property StatsLastResetMillis.<p>
    * The last time stats were reset.
     */
    public long getStatsLastResetMillis()
        {
        return __m_StatsLastResetMillis;
        }
    
    // Accessor for the property "StatsLastResizeMillis"
    /**
     * Getter for property StatsLastResizeMillis.<p>
    * The last time the pool was resized.
     */
    public long getStatsLastResizeMillis()
        {
        return __m_StatsLastResizeMillis;
        }
    
    // Accessor for the property "StatsLastTaskAddCount"
    /**
     * Getter for property StatsLastTaskAddCount.<p>
    * The last captured value of the StatsTaskAddCount since the last time the
    * statistics were reset.
     */
    public long getStatsLastTaskAddCount()
        {
        return __m_StatsLastTaskAddCount;
        }
    
    // Accessor for the property "StatsLastTaskCount"
    /**
     * Getter for property StatsLastTaskCount.<p>
    * The last captured value of the StatsTaskCount since the last time the
    * statistics were reset.
     */
    public long getStatsLastTaskCount()
        {
        return __m_StatsLastTaskCount;
        }
    
    // Accessor for the property "StatsMaxBacklog"
    /**
     * Getter for property StatsMaxBacklog.<p>
    * A maximum TaskBacklog value since the last time the statistics were reset.
     */
    public int getStatsMaxBacklog()
        {
        return __m_StatsMaxBacklog;
        }
    
    // Accessor for the property "StatsTaskAddCount"
    /**
     * Getter for property StatsTaskAddCount.<p>
    * The total number of tasks added to the pool since the last time the
    * statistics were reset.
     */
    public java.util.concurrent.atomic.AtomicLong getStatsTaskAddCount()
        {
        return __m_StatsTaskAddCount;
        }
    
    // Accessor for the property "StatsTaskCount"
    /**
     * Getter for property StatsTaskCount.<p>
    * The total number of tasks executed by Daemon threads since the last time
    * the statistics were reset.
     */
    public long getStatsTaskCount()
        {
        return __m_StatsTaskCount;
        }
    
    // Accessor for the property "StatsTimeoutCount"
    /**
     * Getter for property StatsTimeoutCount.<p>
    * The total number of timed-out tasks since the last time the statistics
    * were reset.
     */
    public int getStatsTimeoutCount()
        {
        return __m_StatsTimeoutCount;
        }
    
    // Accessor for the property "TaskTimeout"
    /**
     * Getter for property TaskTimeout.<p>
    * A default timeout value for PriorityTasks that don't explicitly specify
    * the execution timeout value.
    * This property value is injected into the DaemonPool by its container (see
    * Service.onDependencies).
    * 
    * @see #instantiateWrapperTask
     */
    public long getTaskTimeout()
        {
        return __m_TaskTimeout;
        }
    
    // Accessor for the property "ThreadGroup"
    /**
     * Getter for property ThreadGroup.<p>
    * Specifies the ThreadGroup within which the daemons for this pool will be
    * created. If not specified, the current Thread's ThreadGroup will be used.
    * 
    * This property can only be set at runtime, and must be configured before
    * start() is invoked to cause the daemon threads to be created within the
    * specified ThreadGroup.
     */
    public ThreadGroup getThreadGroup()
        {
        return __m_ThreadGroup;
        }
    
    // Accessor for the property "ThreadPriority"
    /**
     * Getter for property ThreadPriority.<p>
    * The Thread priority used by all threads in the Daemon pool.
     */
    public int getThreadPriority()
        {
        return __m_ThreadPriority;
        }
    
    // Accessor for the property "WorkSlot"
    /**
     * Getter for property WorkSlot.<p>
    * An array of WorkSlot objects. This property is assigned only if the pool
    * was started.
     */
    protected DaemonPool.WorkSlot[] getWorkSlot()
        {
        return __m_WorkSlot;
        }
    
    // Accessor for the property "WorkSlot"
    /**
     * Getter for property WorkSlot.<p>
    * An array of WorkSlot objects. This property is assigned only if the pool
    * was started.
     */
    public DaemonPool.WorkSlot getWorkSlot(int i)
        {
        return getWorkSlot()[i];
        }
    
    // Accessor for the property "WorkSlotCount"
    /**
     * Getter for property WorkSlotCount.<p>
    * The number of WorkSlots. This number is calculated once based on the
    * number of processors and never changes.
     */
    public int getWorkSlotCount()
        {
        return __m_WorkSlotCount;
        }
    
    // From interface: com.tangosol.net.Guardian
    public com.tangosol.net.Guardian.GuardContext guard(com.tangosol.net.Guardable guardable)
        {
        // import com.tangosol.net.Guardian;
        
        Guardian guardian = getInternalGuardian();
        return guardian == null ? null : guardian.guard(guardable);
        }
    
    // From interface: com.tangosol.net.Guardian
    public com.tangosol.net.Guardian.GuardContext guard(com.tangosol.net.Guardable guardable, long cMillis, float flPctRecover)
        {
        // import com.tangosol.net.Guardian;
        
        Guardian guardian = getInternalGuardian();
        return guardian == null ? null : guardian.guard(guardable, cMillis, flPctRecover);
        }
    
    /**
     * Halt the thread pool, halting all worker daemons.  Halt can be called
    * even if the pool is not started (e.g. before it is started or after it is
    * stopped); in that case it will have no effect.  This will bring down the
    * DaemonPool in a forceful and ungraceful manner.
    * 
    * This method should not synchronize or block in any way.
    * This method may not return.
     */
    protected void halt()
        {
        if (isStarted())
            {
            setStarted(false);
        
            DaemonPool.Daemon[] aDaemon = getDaemons();
            for (int i = 0, c = aDaemon.length; i < c; i++)
                {
                aDaemon[i].halt();
                }
            }
        }
    
    /**
     * Find an index of the specified queue in the queue array.
     */
    protected int indexOf(com.oracle.coherence.common.util.AssociationPile queue)
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        
        // we could maintain a Map<AssociationPile, Integer> to make this more efficient,
        // but since this method is only used during the pool resizing, and the number
        // of queues is relatively small, a simple scan will do for now
        AssociationPile[] aQueue = getQueues();
        for (int i = 0, c = aQueue.length; i < c; i++)
            {
            if (aQueue[i] == queue)
                {
                return i;
                }
            }
        return -1;
        }
    
    /**
     * Instantiate a daemon thread.
    * 
    * @param nType the daemon type (one of the DAEMON_* constants)
    * @param queue the queue this daemon will be associated with (pull jobs
    * from)
    * 
    * @return a new daemon thread, in a configured state, but not yet started
     */
    protected DaemonPool.Daemon instantiateDaemon(int nType, com.oracle.coherence.common.util.AssociationPile queue)
        {
        // import com.tangosol.util.Base;
        
        DaemonPool.Daemon daemon = (DaemonPool.Daemon) _newChild("Daemon");
        String  sName  = daemon.getThreadName();
        
        switch (nType)
            {
            case DAEMON_NONPOOLED:   // fall through
                sName = "Dedicated";
            case DAEMON_STANDARD:
                int iDaemon = getDaemonIndex().getAndIncrement();
                // a hex string indicates a dynamic daemon pool
                sName += ':';
                sName += isDynamic()
                        ? "0x" + Base.toHexString(iDaemon, Math.max(4, Base.getMaxHexDigits(iDaemon)))
                            + ':' + (Base.getUpTimeMillis()/1000) // uptime in seconds to use for analysis 
                        : String.valueOf(iDaemon);
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + nType);
            }
        
        String sPoolName = getName();
        
        daemon.setThreadName(sPoolName == null ? sName : sPoolName + sName);
        daemon.setDaemonType(nType);
        daemon.setThreadGroup(getThreadGroup());
        daemon.setPriority(getThreadPriority());
        daemon.setQueue(queue);
        
        return daemon;
        }
    
    /**
     * Create a queue for this DaemonPool.
     */
    protected com.oracle.coherence.common.util.AssociationPile instantiateQueue()
        {
        // import com.oracle.coherence.common.util.ConcurrentAssociationPile;
        
        return new ConcurrentAssociationPile();
        }
    
    /**
     * Factory method: create a new WrapperTask component.
     */
    protected DaemonPool.WrapperTask instantiateWrapperTask()
        {
        // instantiate directly to avoid reflection
        DaemonPool.WrapperTask task = new DaemonPool.WrapperTask();
        _linkChild(task);
        return task;
        }
    
    /**
     * Instantiate a WrapperTask component for the specified Runnable task.
    * 
    * @param task  the Runnable task to execute (call the run() method of) on
    * one of the daemon threads
    * @param fAggressiveTimeout  if true, the timeout interval could be counted
    * from the time the task has been posted rather than started execution
     */
    public DaemonPool.WrapperTask instantiateWrapperTask(Runnable task, boolean fAggressiveTimeout)
        {
        // import com.tangosol.net.PriorityTask;
        // import com.tangosol.util.Base;
        
        long         ldtNow  = Base.getLastSafeTimeMillis();
        DaemonPool.WrapperTask wrapper = instantiateWrapperTask();
        
        wrapper.setTask(task);
        wrapper.setPostTime(ldtNow);
        
        if (task instanceof PriorityTask)
            {
            PriorityTask ptask = (PriorityTask) task;
        
            int iPriority = ptask.getSchedulingPriority();
            if (iPriority < PriorityTask.SCHEDULE_STANDARD ||
                iPriority > PriorityTask.SCHEDULE_IMMEDIATE)
                {
                _trace("Invalid scheduling priority value: " + iPriority + " for " +
                       task.getClass().getName() + "; changing to SCHEDULE_STANDARD", 2);
                iPriority = PriorityTask.SCHEDULE_STANDARD;
                }
            wrapper.setPriority(iPriority);
        
            long cTimeout = ptask.getExecutionTimeoutMillis();
            if (cTimeout == PriorityTask.TIMEOUT_DEFAULT)
                {
                cTimeout = getTaskTimeout();
                }
            else if (cTimeout == PriorityTask.TIMEOUT_NONE)
                {
                // set the timeout to a sufficiently large value
                // that has the effect of "infinite".  We don't use
                // Long.MAX_VALUE here as GuardSupport does not handle
                // heartbeats that overflow long (for performance reasons).
                cTimeout = 0x7FFFFFFL;
                }
        
            if (cTimeout > 0L)
                {
                wrapper.setTimeoutMillis(cTimeout);
                if (fAggressiveTimeout)
                    {
                    wrapper.setStopTime(ldtNow + cTimeout);
        
                    // The WrapperTask is not a thread, but we use the
                    // Guardian infrastructure to enforce aggressive task
                    // timeouts.  The WrapperTask implements Guardable and
                    // is guarded with an SLA of the desired timeout.  It will
                    // never issue a heartbeat; instead it will release the
                    // guard context when the task is scheduled.  The terminate
                    // action for the task will be to unschedule itself.
                    guard(wrapper, cTimeout, 1.0F);
                    }
                }
            }
        
        return wrapper;
        }
    
    // Accessor for the property "AutoStart"
    /**
     * Getter for property AutoStart.<p>
    * Design-time property: Set to true to automatically start the daemon
    * threads when the DaemonPool is instantiated.
     */
    protected boolean isAutoStart()
        {
        return __m_AutoStart;
        }
    
    // Accessor for the property "Dynamic"
    /**
     * Getter for property Dynamic.<p>
    * Flag that indicates whether this DaemonPool dynamically changes its
    * thread count to maximize
    * throughput and resource utilization.
     */
    public boolean isDynamic()
        {
        return getDaemonCountMin() < getDaemonCountMax();
        }
    
    // Accessor for the property "InTransition"
    /**
     * Getter for property InTransition.<p>
    * Indicates that the pool is either growing or shrinking.
    * 
    * @volatile
     */
    public boolean isInTransition()
        {
        return __m_InTransition;
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    public boolean isRunning()
        {
        return isStarted();
        }
    
    // Accessor for the property "Started"
    /**
     * Getter for property Started.<p>
    * This property returns true once the DaemonPool as successfully been
    * started via the start() method until the DaemonPool is stopped via the
    * stop() method. Otherwise it is false.
    * 
    * @volatile
     */
    public boolean isStarted()
        {
        return __m_Started;
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    /**
     * Always returns false.
     * <p>
     * COH-29179: Even if no tasks may have been taken from the backlog since the last call, the guardian
     * will eventually interrupt or abandon stuck worker threads. New replacement worker
     * threads will be created and new work will be taken from the backlog queue.
     */
    public boolean isStuck()
        {
        int  cBacklog;
        int  cDeltaBacklog;
        long cAdd;
        long cDeltaAdd;
        
        // obtain and update statistics while holding the "stats" monitor so that
        // changes are made atomically
        synchronized (STATS_MONITOR)
            {
            cBacklog      = getBacklog();
            cDeltaBacklog = cBacklog - getStatsLastBacklog();
            cAdd          = getStatsTaskAddCount().get();
            cDeltaAdd     = cAdd - getStatsLastTaskAddCount();
        
            // remember the backlog and task count for the next call
            setStatsLastBacklog(cBacklog);
            setStatsLastTaskAddCount(cAdd);
            }

        return false;
        }
    
    /**
     * Wait for all daemon threads to stop.
    * 
    * @param cMillis the number of milliseconds to wait for, or zero for
    * infinite
    * 
    * @return true iff the thread is no longer running
     */
    public boolean join(long cMillis)
        {
        // import com.tangosol.util.Base;
        
        DaemonPool.Daemon[] aDaemon = getDaemons();
        if (aDaemon != null)
            {
            long ldtTimeout = cMillis == 0 ? Long.MAX_VALUE : Base.getSafeTimeMillis() + cMillis;
        
            for (int i = 0, c = aDaemon.length; i < c; i++)
                {
                if (cMillis < 0 || !aDaemon[i].join(cMillis))
                    {
                    return false;
                    }
                cMillis = Base.computeSafeWaitTime(ldtTimeout);
                }
            }
        return true;
        }
    
    /**
     * Called by the daemon thread that is about to exit in response to the
    * StopTask execution.
     */
    public synchronized void onDaemonStop(DaemonPool.Daemon daemon, DaemonPool.StopTask taskStop)
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        
        AssociationPile queue = taskStop.getQueue();
        if (queue != null)
            {
            // we need to merge the content of the queue into the queue
            // that will become associated with that same slot;
            // (all the slots associated with this queue are already closed
            // see $StopTAsk.run())
        
            AssociationPile queueNext = queue;
            try
                {
                // find another queue that is shared with the next smallest number of slots
                AssociationPile queueMerge = findMinSlotSharingQueue(queue);
                if (queueMerge != null)
                    {
                    // merge the content of the closed queue into the queueMerge
                    while (true)
                        {
                        Object oTask = queue.poll();
                        if (oTask == null)
                            {
                            break;
                            }
                        queueMerge.add(oTask);
                        queue.release(oTask);
                        }
                    queueNext = queueMerge;
        
                    // discard the queue
                    _assert(queue.size() == 0);
                    setQueues((AssociationPile[]) copyOnRemove(getQueues(), queue));
                    }
                }
            finally
                {
                // redirect all closed slots to the new queue and re-open them 
                for (int i = 0, c = getWorkSlotCount(); i < c; i++)
                    {
                    DaemonPool.WorkSlot slot = getWorkSlot(i);
        
                    if (slot.getQueue() == queue)
                        {
                        slot.setQueue(queueNext);
                        slot.getGate().open();
                        }
                    }
                }
            }
        
        setDaemons((DaemonPool.Daemon[]) copyOnRemove(getDaemons(), daemon));
        
        taskStop.scheduleNext();
        }
    
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies from within onDependencies.  Often (though not ideal), the 
    * dependencies are copied into the component's properties.  This technique
    * isolates Dependency Injection from the rest of the component code since
    * components continue to access properties just as they did before.
     */
    protected void onDependencies(com.tangosol.internal.util.DaemonPoolDependencies deps)
        {
        setInternalGuardian(deps.getGuardian());
        setName(deps.getName());
        setDaemonCountMin(deps.getThreadCountMin());
        setDaemonCountMax(deps.getThreadCountMax());
        setDaemonCount(deps.getThreadCount());
        setThreadGroup(deps.getThreadGroup());
        setThreadPriority(deps.getThreadPriority());
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import com.tangosol.coherence.config.Config;
        
        String sCount = Config.getProperty("coherence.pool.interruptcount");
        if (sCount != null)
            {
            setAbandonThreshold(Integer.parseInt(sCount));
            }
        
        super.onInit();
        
        if (isAutoStart())
            {
            start();
            }
        }
    
    /**
     * Start a new daemon working against the same queue as the old one and
    * replace it in the daemon array.
     */
    public void replaceDaemon(DaemonPool.Daemon daemonAbandon)
        {
        DaemonPool.WrapperTask wrapper = daemonAbandon.getWrapperTask();
        
        if (wrapper != null && wrapper.getGate() != null)
            {
            // the abandoned daemon has entered the ThreadGate of another queue,
            // which could make the dynamic pool resizing impossible;
            // disable the dynamic resize to prevent a deadlock    
            DaemonPool.ResizeTask task = getResizeTask();
            if (task != null)
                { 
                synchronized (task)
                    {
                    task = getResizeTask();
                    if (task != null)
                        {
                        _trace("Stopping the dynamic pool resizing due to an abandoned thread", 2);
        
                        task.cancel();
                        setResizeTask(null);
                        }
                    }
                }
            }
        
        synchronized (this)
            {
            DaemonPool.Daemon[] aDaemon = getDaemons();
            for (int i = 0, cDaemons = aDaemon.length; i < cDaemons; i++)
                {
                DaemonPool.Daemon daemon = aDaemon[i];
        
                if (daemon == daemonAbandon)
                    {
                    DaemonPool.Daemon daemonNew = instantiateDaemon(DaemonPool.DAEMON_STANDARD, daemon.getQueue());
                    daemonNew.start();
                    aDaemon[i] = daemonNew;
        
                    return;
                    }
                }
            }
        
        // soft assert
        _trace("Failed to replace the abandoned daemon: " + daemonAbandon, 2);
        }
    
    public void resetStats()
        {
        // import com.tangosol.util.Base;
        
        synchronized (STATS_MONITOR)
            {
            //setStatsAbandonedCount(0); // purposely not reset
            setStatsActiveMillis(0L);
            //setStatsHungCount(0);      // purposely not reset
            //setStatsHungDuration(0L);  // purposely not reset 
            //setStatsHungTaskId("");    // purposely not reset
            setStatsLastBacklog(0);
            setStatsLastResizeMillis(0L);    
            setStatsLastTaskAddCount(0L);
            setStatsLastTaskCount(0L);
            setStatsMaxBacklog(0);
            getStatsTaskAddCount().set(Math.max(0L, getStatsTaskAddCount().get() - getStatsTaskCount()));
            setStatsTaskCount(0L);
            setStatsTimeoutCount(0);
        
            // update the reset timestamp
            setStatsLastResetMillis(Base.getSafeTimeMillis());
            }
        }
    
    /**
     * The specified task execution has been canceled.
    * 
    * @param fAbandoned true if the task has timed-out, but all attempts to
    * interrupt it were unsuccessful in stopping the execution; otherwise the
    * task was never started
     */
    public void runCanceled(com.tangosol.net.PriorityTask task, boolean fAbandoned)
        {
        try
            {
            task.runCanceled(fAbandoned);
            }
        catch (RuntimeException ignored) {}
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    public void schedule(Runnable task, long cMillis)
        {
        // import com.oracle.coherence.common.util.Timers;
        // import java.util.Set;
        
        if (cMillis > 0L)
            {
            if (isStarted())
                {
                DaemonPool.ScheduleTask taskSchedule = (DaemonPool.ScheduleTask) _newChild("ScheduleTask");
                taskSchedule.setDelayMillis(cMillis);
                taskSchedule.setTask(task);
        
                Set setScheduled = getScheduledTasks();
                synchronized (setScheduled)
                    {
                    if (isStarted())
                        {
                        Timers.scheduleNonBlockingTask(taskSchedule, cMillis, setScheduled);
                        }
                    }
                }
            }
        else
            {
            add(task);
            }
        }
    
    /**
     * Schedule to start a number of new Daemons.
     */
    protected void scheduleDaemonStart(int cStart)
        {
        // to avoid concurrency complications, we will start daemons one-by-one
        
        _assert(cStart > 0);
        
        DaemonPool.StartTask taskStart = (DaemonPool.StartTask) _newChild("StartTask");
        taskStart.setStartCount(cStart);
        
        add(taskStart, false);
        }
    
    /**
     * Schedule to stop a number of existing Daemons.
     */
    protected void scheduleDaemonStop(int cStop)
        {
        // to avoid concurrency complications, we will stop daemons one-by-one
        
        _assert(cStop > 0);
        
        DaemonPool.StopTask taskStop = (DaemonPool.StopTask) _newChild("StopTask");
        taskStop.setStopCount(cStop);
        
        add(taskStop, false);
        }
    
    // Accessor for the property "AbandonThreshold"
    /**
     * Setter for property AbandonThreshold.<p>
    * The absolute value of this property specifies the number of times an
    * attempt to interrupt is made before a daemon thread is abandoned. A
    * negative value means that the abandoned thread should be stopped using
    * the [deprecated] Thread.stop() API. Could be configured via undocumented 
    * "coherence.pool.interruptcount" property.
     */
    public void setAbandonThreshold(int cThreads)
        {
        __m_AbandonThreshold = cThreads;
        }
    
    // Accessor for the property "AutoStart"
    /**
     * Setter for property AutoStart.<p>
    * Design-time property: Set to true to automatically start the daemon
    * threads when the DaemonPool is instantiated.
     */
    protected void setAutoStart(boolean fAutoStart)
        {
        // design-time only property
        _assert(!is_Constructed());
        
        __m_AutoStart = (fAutoStart);
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    public void setContextClassLoader(ClassLoader loader)
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "DaemonCount"
    /**
     * Setter for property DaemonCount.<p>
    * The number of Daemon threads that exist, if the pool has been started, or
    * the number of Daemon threads that will be created, if the pool has not
    * yet been started. This property can be set at design time or
    * programmatically before the pool is started to configure the number of
    * threads that will be created when the pool starts. Furthermore, this
    * property can be set after the pool is started to change (up or down) the
    * number of worker threads.
     */
    public synchronized void setDaemonCount(int cThreads)
        {
        // import com.tangosol.util.Base;
        
        int cOrig = getDaemonCount();
        if (cThreads != cOrig)
            {
            if (cThreads <= 0)
                {
                throw new IllegalArgumentException("Requested number of threads ("
                    + cThreads + ") is invalid");
                }
        
            // once the thread pool has started, changing the count means starting
            // or stopping threads
            if (isStarted())
                {
                if (isInTransition())
                    {
                    _trace("DaemonPool \"" + getName()
                         + "\" : ignoring a repetitive pool resize request; actual size="
                         + cOrig + ", target=" + cThreads, 2);
                    return;
                    }
        
                setInTransition(true);
        
                if (cThreads > cOrig)
                    {
                    scheduleDaemonStart(cThreads - cOrig);
                    }
                else
                    {
                    // shutdown of unwanted threads should be scheduled
                    scheduleDaemonStop(cOrig - cThreads);
                    }
                }
        
            __m_DaemonCount = (cThreads);
            }
        }
    
    // Accessor for the property "DaemonCountMax"
    /**
     * Setter for property DaemonCountMax.<p>
    * The maximum number of Daemon threads that can exist.
     */
    public void setDaemonCountMax(int cThreads)
        {
        if (is_Constructed() && cThreads < 1)
            {
            throw new IllegalArgumentException("Maximum daemon count must be greater than 0");
            }
        
        __m_DaemonCountMax = (cThreads);
        }
    
    // Accessor for the property "DaemonCountMin"
    /**
     * Setter for property DaemonCountMin.<p>
    * The minimum number of Daemon threads that can exist.
     */
    public void setDaemonCountMin(int cThreads)
        {
        if (is_Constructed() && cThreads < 1)
            {
            throw new IllegalArgumentException("Minimum daemon count must be greater than 0");
            }
        
        __m_DaemonCountMin = (cThreads);
        }
    
    // Accessor for the property "DaemonIndex"
    /**
     * Setter for property DaemonIndex.<p>
    * The index used to suffix the standard daemon names. The index increases
    * monotonously, so when new daemons are started (e.g. to replace abandoned
    * daemons), they will all have unique names.
     */
    private void setDaemonIndex(java.util.concurrent.atomic.AtomicInteger atomic)
        {
        __m_DaemonIndex = atomic;
        }
    
    // Accessor for the property "Daemons"
    /**
     * Setter for property Daemons.<p>
    * An array of currently active pooled Daemon objects. This property is
    * assigned only if the pool has been started. Since this array is modified
    * very infrequently (most of the time - never), we use the copy-on-update
    * pattern.
     */
    protected void setDaemons(DaemonPool.Daemon[] aDaemon)
        {
        __m_Daemons = aDaemon;
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    // Accessor for the property "Dependencies"
    /**
     * Setter for property Dependencies.<p>
    * The external dependencies for this DaemonPool.
     */
    public void setDependencies(com.tangosol.internal.util.DaemonPoolDependencies deps)
        {
        if (getDependencies() != null)
            {
            throw new IllegalStateException("Dependencies already set");
            }
        
        if (isStarted())
            {
            throw new IllegalStateException("DaemonPool has been started");
            }
        
        __m_Dependencies = (cloneDependencies(deps).validate());
        
        // use the cloned dependencies
        onDependencies(getDependencies());
        }
    
    // Accessor for the property "HungThreshold"
    /**
     * Setter for property HungThreshold.<p>
    * The amount of time in milliseconds that a task can execute before it is
    * considered "hung".
    * 
    * Note that a posted task that has not yet started is never considered as
    * hung.
     */
    public void setHungThreshold(long cMillis)
        {
        __m_HungThreshold = cMillis;
        }
    
    // Accessor for the property "InternalGuardian"
    /**
     * Setter for property InternalGuardian.<p>
    * The Guardian that this Daemon's implementation delegates to.
     */
    protected synchronized void setInternalGuardian(com.tangosol.net.Guardian guardian)
        {
        if (isStarted())
            {
            throw new IllegalStateException("cannot modify the Guardian while the pool is running");
            }
        
        __m_InternalGuardian = (guardian);
        }
    
    // Accessor for the property "InTransition"
    /**
     * Setter for property InTransition.<p>
    * Indicates that the pool is either growing or shrinking.
    * 
    * @volatile
     */
    public void setInTransition(boolean fTransition)
        {
        // import com.tangosol.util.Base;
        
        if (!fTransition)
            {
            synchronized (STATS_MONITOR)
                {
                setStatsLastResizeMillis(Base.getSafeTimeMillis());
                }
            }
        
        __m_InTransition = (fTransition);
        }
    
    // Accessor for the property "Name"
    /**
     * Setter for property Name.<p>
    * The name of this DaemonPool.
     */
    public synchronized void setName(String sName)
        {
        if (isStarted())
            {
            throw new IllegalStateException("cannot modify the name while the pool is running");
            }
        
        __m_Name = (sName);
        }
    
    // Accessor for the property "Queues"
    /**
     * Setter for property Queues.<p>
    * An array of currently active queues. This property is assigned only if
    * the pool has been started. Since this array is modified very infrequently
    * (most of the time - never), we use the copy-on-update pattern.
    * 
    * Note: the number of queues is equal to the minimum of the number of work
    * slots and the number of daemons.
     */
    protected void setQueues(com.oracle.coherence.common.util.AssociationPile[] aQueue)
        {
        __m_Queues = aQueue;
        }
    
    // Accessor for the property "ResizeTask"
    /**
     * Setter for property ResizeTask.<p>
    * The periodic "resize" task.
    * 
    * @volatile
     */
    protected void setResizeTask(DaemonPool.ResizeTask task)
        {
        __m_ResizeTask = task;
        }
    
    // Accessor for the property "ScheduledTasks"
    /**
     * Setter for property ScheduledTasks.<p>
    * A set of Disposables (representing tasks) which have been scheduled for
    * delayed execution, and which have not yet been executed.  These tasks
    * need to be cancelled when the pool stops otherwise they will leak
    * whatever then reference until the point at which they are executed.
    * 
    * All access to this set must occur under synchronization on the set.
     */
    protected void setScheduledTasks(java.util.Set setTasks)
        {
        __m_ScheduledTasks = setTasks;
        }
    
    // Accessor for the property "Started"
    /**
     * Setter for property Started.<p>
    * This property returns true once the DaemonPool as successfully been
    * started via the start() method until the DaemonPool is stopped via the
    * stop() method. Otherwise it is false.
    * 
    * @volatile
     */
    protected void setStarted(boolean fStarted)
        {
        // cannot be started while still constructing
        _assert(!fStarted || is_Constructed());
        
        __m_Started = (fStarted);
        }
    
    // Accessor for the property "StatsAbandonedCount"
    /**
     * Setter for property StatsAbandonedCount.<p>
    * The total number of abandoned Daemon threads.
    * 
    * Note: this property is purposely not reset when stats are reset.
     */
    public void setStatsAbandonedCount(int cThreads)
        {
        __m_StatsAbandonedCount = cThreads;
        }
    
    // Accessor for the property "StatsActiveMillis"
    /**
     * Setter for property StatsActiveMillis.<p>
    * The total number of milliseconds spent by all Daemon threads while
    * executing tasks since the last time the statistics were reset.
    * 
    * Note: this value could be greater than the time elapsed since each daemon
    * adds its own processing time when working in parallel.
     */
    protected void setStatsActiveMillis(long cMillis)
        {
        __m_StatsActiveMillis = cMillis;
        }
    
    // Accessor for the property "StatsHungCount"
    /**
     * Setter for property StatsHungCount.<p>
    * The total number of currently executing hung tasks.
    * 
    * Note: this property is purposely not reset when stats are reset.
     */
    protected void setStatsHungCount(int cHung)
        {
        __m_StatsHungCount = cHung;
        }
    
    // Accessor for the property "StatsHungDuration"
    /**
     * Setter for property StatsHungDuration.<p>
    * The longest currently executing hung task duration (in milliseconds).
    * 
    * Note: this property is purposely not reset when stats are reset.
     */
    protected void setStatsHungDuration(long cDuration)
        {
        __m_StatsHungDuration = cDuration;
        }
    
    // Accessor for the property "StatsHungTaskId"
    /**
     * Setter for property StatsHungTaskId.<p>
    * The id of the longest currently executing hung task. If the task does not
    * implement javax.util.concurrent.Identifiable (requires JDK 1.5), return
    * task.toString().
    * 
    * Note: this property is purposely not reset when stats are reset.
     */
    protected void setStatsHungTaskId(String sTaskId)
        {
        __m_StatsHungTaskId = sTaskId;
        }
    
    // Accessor for the property "StatsLastBacklog"
    /**
     * Setter for property StatsLastBacklog.<p>
    * The last captured value of the Backlog property since the last time the
    * statistics were reset.
     */
    protected void setStatsLastBacklog(int cTasks)
        {
        __m_StatsLastBacklog = cTasks;
        }
    
    // Accessor for the property "StatsLastResetMillis"
    /**
     * Setter for property StatsLastResetMillis.<p>
    * The last time stats were reset.
     */
    protected void setStatsLastResetMillis(long ldtReset)
        {
        __m_StatsLastResetMillis = ldtReset;
        }
    
    // Accessor for the property "StatsLastResizeMillis"
    /**
     * Setter for property StatsLastResizeMillis.<p>
    * The last time the pool was resized.
     */
    protected void setStatsLastResizeMillis(long ldtResize)
        {
        __m_StatsLastResizeMillis = ldtResize;
        }
    
    // Accessor for the property "StatsLastTaskAddCount"
    /**
     * Setter for property StatsLastTaskAddCount.<p>
    * The last captured value of the StatsTaskAddCount since the last time the
    * statistics were reset.
     */
    protected void setStatsLastTaskAddCount(long cTasks)
        {
        __m_StatsLastTaskAddCount = cTasks;
        }
    
    // Accessor for the property "StatsLastTaskCount"
    /**
     * Setter for property StatsLastTaskCount.<p>
    * The last captured value of the StatsTaskCount since the last time the
    * statistics were reset.
     */
    protected void setStatsLastTaskCount(long cTasks)
        {
        __m_StatsLastTaskCount = cTasks;
        }
    
    // Accessor for the property "StatsMaxBacklog"
    /**
     * Setter for property StatsMaxBacklog.<p>
    * A maximum TaskBacklog value since the last time the statistics were reset.
     */
    protected void setStatsMaxBacklog(int cTasks)
        {
        __m_StatsMaxBacklog = cTasks;
        }
    
    // Accessor for the property "StatsTaskAddCount"
    /**
     * Setter for property StatsTaskAddCount.<p>
    * The total number of tasks added to the pool since the last time the
    * statistics were reset.
     */
    protected void setStatsTaskAddCount(java.util.concurrent.atomic.AtomicLong cTasks)
        {
        __m_StatsTaskAddCount = cTasks;
        }
    
    // Accessor for the property "StatsTaskCount"
    /**
     * Setter for property StatsTaskCount.<p>
    * The total number of tasks executed by Daemon threads since the last time
    * the statistics were reset.
     */
    protected void setStatsTaskCount(long cTasks)
        {
        __m_StatsTaskCount = cTasks;
        }
    
    // Accessor for the property "StatsTimeoutCount"
    /**
     * Setter for property StatsTimeoutCount.<p>
    * The total number of timed-out tasks since the last time the statistics
    * were reset.
     */
    public void setStatsTimeoutCount(int cTasks)
        {
        __m_StatsTimeoutCount = cTasks;
        }
    
    // Accessor for the property "TaskTimeout"
    /**
     * Setter for property TaskTimeout.<p>
    * A default timeout value for PriorityTasks that don't explicitly specify
    * the execution timeout value.
    * This property value is injected into the DaemonPool by its container (see
    * Service.onDependencies).
    * 
    * @see #instantiateWrapperTask
     */
    public void setTaskTimeout(long cMillis)
        {
        __m_TaskTimeout = cMillis;
        }
    
    // Accessor for the property "ThreadGroup"
    /**
     * Setter for property ThreadGroup.<p>
    * Specifies the ThreadGroup within which the daemons for this pool will be
    * created. If not specified, the current Thread's ThreadGroup will be used.
    * 
    * This property can only be set at runtime, and must be configured before
    * start() is invoked to cause the daemon threads to be created within the
    * specified ThreadGroup.
     */
    public synchronized void setThreadGroup(ThreadGroup group)
        {
        if (isStarted())
            {
            throw new IllegalStateException("cannot modify the ThreadGroup while the pool is running");
            }
        
        __m_ThreadGroup = (group);
        }
    
    // Accessor for the property "ThreadPriority"
    /**
     * Setter for property ThreadPriority.<p>
    * The Thread priority used by all threads in the Daemon pool.
     */
    public synchronized void setThreadPriority(int nPriority)
        {
        if (nPriority < Thread.MIN_PRIORITY || nPriority > Thread.MAX_PRIORITY)
            {
            throw new IllegalArgumentException("invalid daemon thread priority: " + nPriority);
            }
        
        __m_ThreadPriority = (nPriority);
        
        if (isStarted())
            {
            DaemonPool.Daemon[] daemons = getDaemons();
            for (int i = 0, c = daemons == null ? 0 : daemons.length; i < c; i++)
                {
                daemons[i].getThread().setPriority(nPriority);
                }
            }
        }
    
    // Accessor for the property "WorkSlot"
    /**
     * Setter for property WorkSlot.<p>
    * An array of WorkSlot objects. This property is assigned only if the pool
    * was started.
     */
    protected void setWorkSlot(DaemonPool.WorkSlot[] aSlot)
        {
        _assert(aSlot != null && getWorkSlot() == null, "Must not change");
        
        __m_WorkSlot = (aSlot);
        }
    
    // Accessor for the property "WorkSlot"
    /**
     * Setter for property WorkSlot.<p>
    * An array of WorkSlot objects. This property is assigned only if the pool
    * was started.
     */
    protected void setWorkSlot(int i, DaemonPool.WorkSlot slot)
        {
        _assert(slot != null && getWorkSlot(i) == null, "Must not change");
        
        getWorkSlot()[i] = slot;
        }
    
    // Accessor for the property "WorkSlotCount"
    /**
     * Setter for property WorkSlotCount.<p>
    * The number of WorkSlots. This number is calculated once based on the
    * number of processors and never changes.
     */
    protected void setWorkSlotCount(int cSlots)
        {
        _assert(cSlots > 0 && getWorkSlotCount() == 0, "Must not change");
        
        __m_WorkSlotCount = (cSlots);
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    public void shutdown()
        {
        stop();
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    /**
     * Start the thread pool, spinning up the pre-configured number of threads
    * as defined by the DaemonCount property. Start can be called even if the
    * pool is already started; in that case it will have no effect.
     */
    public synchronized void start()
        {
        // import com.tangosol.coherence.config.Config;
        // import com.oracle.coherence.common.util.AssociationPile;
        
        _assert(is_Constructed());
        
        if (!isStarted())
            {
            // create the WorkSlots
            int cSlots = Runtime.getRuntime().availableProcessors();
            try
                {
                String sSlots = Config.getProperty("coherence.daemonpool.slots");
                if  (sSlots != null)
                    {
                    cSlots = Math.abs(Integer.parseInt(sSlots));
                    }
                }
            catch (RuntimeException ignored) {}
        
            setWorkSlot(new DaemonPool.WorkSlot[cSlots]);
            setWorkSlotCount(cSlots);
            for (int i = 0; i < cSlots; i++)
                {
                DaemonPool.WorkSlot slot = (DaemonPool.WorkSlot) _newChild("WorkSlot");
                slot.setIndex(i);
                setWorkSlot(i, slot);
                }
        
            // create the Daemons and associate with the corresponding slots 
            int cDaemons = Math.max(getDaemonCount(), getDaemonCountMin());
            _assert(cDaemons > 0);

            DaemonPool.Daemon[] aDaemon  = new DaemonPool.Daemon[cDaemons];

            int               cQueues = Math.min(cDaemons, cSlots);
            AssociationPile[] aQueue  = new AssociationPile[cQueues];
        
            for (int i = 0; i < cQueues; i++)
                {
                AssociationPile queue = instantiateQueue();
        
                getWorkSlot(i).setQueue(queue);
        
                aQueue[i]  = queue;
                aDaemon[i] = instantiateDaemon(DAEMON_STANDARD, queue);
                }
        
            if (cSlots >= cDaemons)
                {
                // these slots will share the queue, and will be processed by the same daemon
                for (int iSlot = cDaemons; iSlot < cSlots; iSlot++)
                    {
                    getWorkSlot(iSlot).setQueue(
                        getWorkSlot(iSlot % cDaemons).getQueue());
                    }
                }
            else // (cDaemons > cSlots)
                {
                // these daemons share the slots,
                // so the corresponding queues will be served by multiple daemons
                for (int iDaemon = cSlots; iDaemon < cDaemons; iDaemon++)
                    {
                    aDaemon[iDaemon] = instantiateDaemon(DAEMON_STANDARD,
                        getWorkSlot(iDaemon % cSlots).getQueue());
                    }
                }

            setDaemonCount(cDaemons);
            setQueues(aQueue);
            setDaemons(aDaemon);
        
            for (int i = 0; i < cSlots; i++)
                {
                getWorkSlot(i).setActive(true);
                }
        
            for (int i = 0; i < cDaemons; i++ )
                {
                aDaemon[i].start();
                }
        
            setStarted(true);
            Object[] ao = new Object[]
                {
                Integer.valueOf(cDaemons),
                Integer.valueOf(getDaemonCountMax()),
                Integer.valueOf(getDaemonCountMin()),
                Boolean.valueOf(isDynamic()).toString(),
                Integer.valueOf(cQueues),
                Integer.valueOf(cSlots),
                };
            _trace(String.format("Started DaemonPool \"" + getName()
                + "\": [DeamonCount=%d, DaemonCountMax=%d, DaemonCountMin=%d, Dynamic=%s QueueSize=%d, WorkSlots=%d]", ao), 4);

            if (isDynamic())
                {
                // schedule a new ResizeTask
                DaemonPool.ResizeTask task = (DaemonPool.ResizeTask) _newChild("ResizeTask");
                setResizeTask(task);
        
                schedule(task, task.getPeriodMillis());
                }
            }
        }
    
    /**
     * Start a new Daemon.
     */
    public synchronized void startDaemon(DaemonPool.StartTask taskStart)
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        // import com.tangosol.util.Gate;
        
        int       cSlots   = getWorkSlotCount();
        DaemonPool.Daemon[] aDaemon  = getDaemons();
        int       cDaemons = aDaemon.length;
        
        if (cDaemons < cSlots)
            {
            // There must be at least two slots that share a queue. We need to associate
            // one of them with a new queue and create a new daemon for that queue.
            // Meanwhile, the old queue is still processed by the daemon (only one!) that
            // was responsible for all tasks coming through this slot, and we need to make
            // sure that all tasks bound to this slot that have been already added to that
            // queue are processed before the new daemon can start pulling tasks from the
            // new queue. To do that we:
            //   a) create a new queue and associate it with this slot,
            //      but don't start the corresponding daemon
            //   b) add a synthetic "StartDaemon" job to the old queue
            //   c) by the time the "StartDaemon" job is processed, it's guaranteed that
            //      all previously posted jobs that belonged to this slot are processed
        
            DaemonPool.WorkSlot slotSplit = findMaxQueueSharingSlot();
            _assert(slotSplit != null && slotSplit.isActive());
        
            AssociationPile queueOld = slotSplit.getQueue();
        
            // we need to make sure that after we place the StartTask to queueOld
            // no other threads can add anything else to it; since every "add" enters the gate,
            // we close it here to create a write-barrier and ensure read visibility
            Gate gate = slotSplit.getGate();
        
            if (!gate.close(1L))
                {
                // failed to close the slot, reschedule the task
                add(taskStart, false);
                return;
                }
        
            AssociationPile queue  = instantiateQueue();
            Daemon          daemon = instantiateDaemon(DAEMON_STANDARD, queue);
        
            setQueues((AssociationPile[]) copyOnAdd(getQueues(), queue));
            setDaemons((DaemonPool.Daemon[]) copyOnAdd(aDaemon, daemon));
            
            try
                {
                slotSplit.setActive(false);
                slotSplit.setQueue(queue);
                }
            finally
                {
                gate.open();
                }
        
            int cStart = taskStart.getStartCount();
        
            // create an "ASSOCIATION_ALL" instance
            // (we cannot reuse the passed one since it has a different association)
            taskStart = (DaemonPool.StartTask) _newChild("StartTask");
            taskStart.setDaemon(daemon);
            taskStart.setWorkSlotActivate(slotSplit);
            taskStart.setStartCount(cStart);
            taskStart.setQueue(queueOld);
        
            queueOld.add(instantiateWrapperTask(taskStart, false));
            }
        else // (cDaemons >= cSlots)
            {
            // All work slots are associated with distinct queues and some could be
            // served by multiple daemons.
            // Find a slot that is served by the minimum number of daemons and add
            // another daemon to process the corresponding queue
        
            AssociationPile queue = findMinDaemonSharingQueue();
            _assert(queue != null);
        
            Daemon daemon = instantiateDaemon(DAEMON_STANDARD, queue);
        
            setDaemons((DaemonPool.Daemon[]) copyOnAdd(aDaemon, daemon));
        
            daemon.start();
        
            taskStart.scheduleNext();
            }
        }
    
    // From interface: com.tangosol.internal.util.DaemonPool
    /**
     * Stop the thread pool, stopping all threads once they are done processing
    * any current tasks that they are working on (interrupting them to wake up
    * waiting or sleeping threads), but not actually waiting until the queue is
    * empty. Stop can be called even if the pool is not started (e.g. before it
    * is started or after it is stopped); in that case it will have no effect.
    * 
    * A "soft" shutdown that would wait for normal threads termination without
    * interrupting them could be accomplished by setting the thread count to
    * zero.
     */
    public void stop()
        {
        // import com.oracle.coherence.common.base.Disposable;
        // import java.util.Iterator;
        // import java.util.Set;
        
        if (isStarted())
            {
            // We must cancel the ResizeTask before syncronizing on
            // this DaemonPool because the task may be running and
            // already be synchronized on itself and be about to
            // synchronize on this DaemonPool, which can cause a
            // deadlock as the cancel method is synchronized
            DaemonPool.ResizeTask task = getResizeTask();
            if (task != null && task.getDaemonPool() != null)
                {
                task.cancel();
                setResizeTask(null);
                }
        
            synchronized (this)
                {
                if (isStarted())
                    {
                    setStarted(false);
        
                    DaemonPool.Daemon[] aDaemon = getDaemons();
                    for (int i = 0, c = aDaemon.length; i < c; i++)
                        {
                        aDaemon[i].stop();
                        }
        
                    // ensure any scheduled tasks are cancelled so that the Timer thread doesn't retain
                    // references to the pool or the scheduled tasks
                    Set setScheduled = getScheduledTasks();
                    synchronized (setScheduled)
                        {
                        for (Iterator iter = getScheduledTasks().iterator(); iter.hasNext(); )
                            {
                            Disposable disposable = (Disposable) iter.next();
                            iter.remove(); // calling dispose will also remove the object; we do it first to avoid
                                           // invalidating our iterator
                            disposable.dispose();
                            }
                        }
        
                    setInTransition(false);
                    }
                }
            }
        }
    
    /**
     * Find a Daemon to stop and shut it down.
     */
    public synchronized void stopDaemon(DaemonPool.StopTask taskStop)
        {
        // import com.oracle.coherence.common.util.AssociationPile;
        
        int       cSlots   = getWorkSlotCount();
        DaemonPool.Daemon[] aDaemon  = getDaemons();
        int       cDaemons = aDaemon.length;
        DaemonPool.WorkSlot slot;
        
        if (cDaemons > cSlots)
            {
            // All work slots are associated with distinct queues and some could be
            // served by multiple daemons. Find a slot that is served by the largest
            // number of daemons (must be more than one) and stop one of them.
        
            slot = findMaxDaemonSharingSlot();
            _assert(slot != null);
        
            taskStop.setWorkSlot(slot);
            }
        else
            {
            // Every queue is processed by one and only one daemon (though there could be
            // daemons that are about to be stopped). The algorithm is:
            // 1. find a queue (Q1) that is shared by the smallest number of slots;
            // 2. schedule a StopTask for the daemon corresponding to Q1;
            // 3. when the StopTask executes it will (see DaemonPool.StopTask.run()):
            //  3a) attempt to close all the slots corresponding to Q1;
            //  3b) upon success, throw an EventDeathException; otherwise re-schedule
            // 4. when the EventDeathException is caught, the departing thread will (see onDaemonStop())
            //  4a) find a queue (Q2) that is shared with the next smallest number of slots;
            //  4b) merge the content of Q1 into Q2 and discard Q1;
            //  4c) redirect all closed slots to Q2 and re-open them
        
            AssociationPile queue = findMinSlotSharingQueue(null);
        
            slot = findSlot(queue);
            _assert(slot != null);
        
            taskStop.setQueue(queue);
            }
        
        slot.add(instantiateWrapperTask(taskStop, false));
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuilder sb = new StringBuilder(get_Name());
        
        sb.append("\nWorkSlots:");
        for (int i = 0, c = getWorkSlotCount(); i < c; i++)
            {
            sb.append("\n  ").append(getWorkSlot(i));
            }
        
        DaemonPool.Daemon[] aDaemon = getDaemons();
        sb.append("\n Daemons:");
        for (int i = 0, c = aDaemon == null ? 0 : aDaemon.length; i < c; i++)
            {
            sb.append("\n  ").append(aDaemon[i]);
            }
        return sb.toString();
        }
    
    /**
     * Update the execution statistics.
    * 
    * @param daemon  the Daemon that is reporting the statistics
    * @param cTasks  the number of tasks that were executed
    * @param ldtStart  the timestamp when the execution began
    * 
    * @return current time
     */
    public long updateStats(DaemonPool.Daemon daemon, int cTasks, long ldtStart)
        {
        // import com.tangosol.util.Base;
        
        long ldtNow = Base.getSafeTimeMillis();
        if (cTasks > 0 && daemon.getDaemonType() == DAEMON_STANDARD)
            {
            synchronized (STATS_MONITOR)
                {
                setStatsTaskCount(getStatsTaskCount() + cTasks);
                setStatsActiveMillis(getStatsActiveMillis() + ldtNow - ldtStart);
                }
            }
        
        daemon.setFlushStats(false);
        return ldtNow;
        }

    // ---- class: com.tangosol.coherence.component.util.DaemonPool$Daemon
    
    /**
     * The prototypical Daemon thread component that will belong to the
     * DaemonPool. An instance of this component is created for each thread in
     * the pool.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Daemon
            extends    com.tangosol.coherence.component.util.Daemon
        {
        // ---- Fields declarations ----
        
        /**
         * Property DaemonType
         *
         * One of the $Module.DAEMON_* constants.
         */
        private int __m_DaemonType;
        
        /**
         * Property FlushStats
         *
         * Flag that indicates the Daemon should report its latest statistics
         * to the DaemonPool.
         * 
         * @volatile
         */
        private volatile boolean __m_FlushStats;
        
        /**
         * Property InterruptCount
         *
         * Number of times this daemon thread was attempted to be interrupted.
         * If this number reaches the threshold, the daemon thread will be
         * abandoned.
         * 
         * @volatile
         */
        private volatile transient int __m_InterruptCount;
        
        /**
         * Property Queue
         *
         * The Queue from which this Daemon extracts Runnable tasks.
         */
        private com.oracle.coherence.common.util.AssociationPile __m_Queue;
        
        /**
         * Property WrapperTask
         *
         * Currently running task. If the Task is set before the daemon thread
         * starts, the thread will terminate as soon as the task completes.
         * 
         * @volatile
         */
        private volatile transient DaemonPool.WrapperTask __m_WrapperTask;
        
        // Default constructor
        public Daemon()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Daemon(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setDaemonState(0);
                setDefaultGuardRecovery(0.9F);
                setDefaultGuardTimeout(60000L);
                setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
                setThreadName("Worker");
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new DaemonPool.Daemon.Guard("Guard", this, true), "Guard");
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.DaemonPool.Daemon();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool$Daemon".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        /**
         * Abandon this daemon thread due to an overly long execution that could
        * not be interrupted.
         */
        public void abandon()
            {
            // import com.oracle.coherence.common.base.Blocking;
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.ClassHelper;
            
            boolean fAbandon = false;
            Thread  thread   = null;
            
            synchronized (this)
                {
                thread = getThread();
                if (thread == null)
                    {
                    return;
                    }
            
                if (isStarted() && !isExiting())
                    {
                    fAbandon = true;
                    setExiting(true);
                    }
                }
            
            if (fAbandon)
                {
                DaemonPool      pool    = (DaemonPool) get_Module();
                DaemonPool.WrapperTask wrapper = getWrapperTask();
                String       sThread = getThreadName();
                String       sReason;
            
                synchronized (pool.STATS_MONITOR)
                    {
                    pool.setStatsAbandonedCount(pool.getStatsAbandonedCount() + 1);
                    }
            
                if (wrapper == null)
                    {
                    sReason = ", while waiting";
                    }
                else
                    {
                    sReason = " executing task \"" + wrapper.getTaskId() + "\"";
                    }
                if (wrapper != null)
                    {
                    Runnable task = wrapper.getTask();
                    if (task instanceof PriorityTask)
                        {
                        pool.runCanceled((PriorityTask) task, true);
                        }
                    }
                
                StringBuilder sbMsg = new StringBuilder();
                sbMsg.append("A worker thread \"")
                     .append(sThread)
                     .append(sReason)
                     .append(", did not respond to ")
                     .append(Math.abs(pool.getAbandonThreshold()))
                     .append(" interrupt requests. The execution was canceled.")
                     .append(" The thread ");
            
                int cAttempts = pool.getAbandonThreshold();
                if (cAttempts < 0)
                    {
                    for (int i = cAttempts; i < 0 && thread.isAlive(); i++)
                        {
                        try
                            {
                            // Thread.stop() is a deprecated method;
                            // using reflection to suppress the compilation warning.
                            ClassHelper.invoke(thread, "stop", ClassHelper.VOID);
                            Blocking.sleep(1);
                            }
                        catch (InterruptedException e)
                            {
                            Thread.interrupted();
                            break;
                            }
                        catch (Exception e)
                            {
                            break;
                            }
                        }
                    if (thread.isAlive())
                        {
                        sbMsg.append("could not be stopped and ");
                        }
                    }
            
                if (thread.isAlive())
                    {
                    sThread += "!abandoned";
                    try
                        {
                        thread.setName(sThread);
                        thread.setPriority(Thread.MIN_PRIORITY);
                        }
                    catch (RuntimeException ignored) {}
                    sbMsg.append("is abandoned...");
            
                    try
                        {
                        StackTraceElement[] atrace = thread.getStackTrace();
                        for (int i = 0, c = atrace.length; i < c; i++)
                            {
                            sbMsg.append("\n  at ")
                                 .append(atrace[i]);
                            }
                        sbMsg.append('\n');
                        }
                    catch (Throwable ignored) {}
                    }
                else
                    {
                    sbMsg.append("is stopped.");
                    }
            
                _trace(sbMsg.toString(), 1);
                }
            }
        
        // Accessor for the property "DaemonType"
        /**
         * Getter for property DaemonType.<p>
        * One of the $Module.DAEMON_* constants.
         */
        public int getDaemonType()
            {
            return __m_DaemonType;
            }
        
        // Accessor for the property "InterruptCount"
        /**
         * Getter for property InterruptCount.<p>
        * Number of times this daemon thread was attempted to be interrupted.
        * If this number reaches the threshold, the daemon thread will be
        * abandoned.
        * 
        * @volatile
         */
        public int getInterruptCount()
            {
            return __m_InterruptCount;
            }
        
        // Accessor for the property "Queue"
        /**
         * Getter for property Queue.<p>
        * The Queue from which this Daemon extracts Runnable tasks.
         */
        public com.oracle.coherence.common.util.AssociationPile getQueue()
            {
            return __m_Queue;
            }
        
        // Declared at the super level
        /**
         * Getter for property WaitMillis.<p>
        * The number of milliseconds that the daemon will wait for
        * notification. Zero means to wait indefinitely. Negative value means
        * to skip waiting altogether.
        * 
        * @see #onWait
         */
        public long getWaitMillis()
            {
            long cWait = super.getWaitMillis();
            
            if (isGuarded() || isGuardian())
                {
                // If this Daemon is being Guarded, it should not wait for longer
                // than a fraction of the timeout.  In practice, the SLA's should
                // be set fairly high (higher than the packet timeout), so limiting
                // wait times to 1 sec should be sufficient.  This saves a more
                // expensive calculation to find the exact wait time which
                // profiling/testing has shown to be expensive in critical loops.
            
                long cMaxWait = 1000L;
                cWait = cWait == 0 ? cMaxWait : Math.min(cWait, cMaxWait);
                }
            return cWait;
            }
        
        // Accessor for the property "WrapperTask"
        /**
         * Getter for property WrapperTask.<p>
        * Currently running task. If the Task is set before the daemon thread
        * starts, the thread will terminate as soon as the task completes.
        * 
        * @volatile
         */
        public DaemonPool.WrapperTask getWrapperTask()
            {
            return __m_WrapperTask;
            }
        
        // Declared at the super level
        /**
         * Halt the daemon.  Brings down the daemon in an ungraceful manner.
        * This method should not synchronize or block in any way.
        * This method may not return.
         */
        public void halt()
            {
            super.halt();
            }
        
        // Declared at the super level
        /**
         * Issue heartbeat.  See com.tangosol.net.Guardian$GuardContext.
        * 
        * @param cMillis  the duration of heartbeat to issue, or 0 for the
        * default heartbeat
         */
        public void heartbeat(long cMillis)
            {
            super.heartbeat(cMillis);
            }
        
        // Accessor for the property "FlushStats"
        /**
         * Getter for property FlushStats.<p>
        * Flag that indicates the Daemon should report its latest statistics to
        * the DaemonPool.
        * 
        * @volatile
         */
        public boolean isFlushStats()
            {
            return __m_FlushStats;
            }
        
        // Declared at the super level
        /**
         * Event notification called once the daemon's thread starts and before
        * the daemon thread goes into the "wait - perform" loop. Unlike the
        * <code>onInit()</code> event, this method executes on the daemon's
        * thread.
        * 
        * Note1: this method is called while the caller's thread is still
        * waiting for a notification to  "unblock" itself.
        * Note2: any exception thrown by this method will terminate the thread
        * immediately
         */
        protected void onEnter()
            {
            // import com.tangosol.net.GuardSupport;
            
            super.onEnter();
            
            // set the guardian-context for the service thread
            if (isGuarded())
                {
                GuardSupport.setThreadContext(getGuardable().getContext());
                }
            }
        
        // Declared at the super level
        /**
         * This event occurs when an exception is thrown from onEnter, onWait,
        * onNotify and onExit.
        * 
        * If the exception should terminate the daemon, call stop(). The
        * default implementation prints debugging information and terminates
        * the daemon.
        * 
        * @param e  the Throwable object (a RuntimeException or an Error)
        * 
        * @throws RuntimeException may be thrown; will terminate the daemon
        * @throws Error may be thrown; will terminate the daemon
         */
        protected void onException(Throwable e)
            {
            if (isExiting())
                {
                super.onException(e);
                }
            else
                {
                // let subclasses decide whether to terminate the thread
                _trace("An unhandled exception occurred on worker thread \"" + get_Name()
                        + "\":", 1);
                _trace(e);
                }
            }
        
        // Declared at the super level
        /**
         * Event notification called right before the daemon thread terminates.
        * This method is guaranteed to be called only once and on the daemon's
        * thread.
         */
        protected void onExit()
            {
            if (!isExiting())
                {
                // soft assert that this daemon is no longer in its pool
                DaemonPool.Daemon[] aDaemon = ((DaemonPool) get_Module()).getDaemons();
                for (int i = 0, c = aDaemon == null ? 0 : aDaemon.length; i < c; ++i)
                    {
                    if (this == aDaemon[i])
                        {
                        _trace("Worker thread \"" + getThreadName()
                                + "\" is exiting but still remains in its pool", 1);
                        break;
                        }
                    }
                }
            
            super.onExit();
            }
        
        // Declared at the super level
        /**
         * Event notification to perform a regular daemon activity. To get it
        * called, another thread has to set Notification to true:
        * <code>daemon.setNotification(true);</code>
        * 
        * @see #onWait
         */
        protected void onNotify()
            {
            // import com.tangosol.run.component.EventDeathException;
            // import com.tangosol.util.Base;
            
            DaemonPool pool     = (DaemonPool) get_Parent();
            boolean fOnce    = getQueue() == null;
            long    ldtStart = 0L;
            int     cTasks   = 0;
            
            DaemonPool.WrapperTask wrapper = getWrapperTask(); // not null only if fOnce
            try
                {
                while (!isExiting())
                    {
                    // Note: we don't collect any stats for a "single task" daemon
                    if (!fOnce)
                        {
                        wrapper = removeFromQueue();
                        if (wrapper == null)
                            {
                            wrapper = removeFromAnotherQueue();
                            if (wrapper == null)
                                {
                                return;
                                }
                            }
            
                        // this marks the thread as "active"
                        setWrapperTask(wrapper);
            
                        // "modulo" is much more expensive than "and"
                        if (isFlushStats() || (cTasks & 0xFFL) == 0L)
                            {
                            // make sure that we register at least every 256 tasks,
                            // so the stats are updated even if the queue is never empty
                            ldtStart = pool.updateStats(this, cTasks, ldtStart);
                            cTasks   = 0;
                            }
                        }
                    else
                        {
                        if (wrapper == null)
                            {
                            setExiting(true);
                            return;
                            }
                        }
            
                    long ldtStop        = wrapper.getStopTime();
                    long cTimeoutMillis = wrapper.getTimeoutMillis();
                    if (ldtStop > 0L)
                        {
                        cTimeoutMillis = Math.min(cTimeoutMillis, Math.max(1L, ldtStop - ldtStart));
                        }
            
                    // The following call is necessary to:
                    //   a) guard the next task;
                    //   b) reset the timeout value that could be set by the previous task
                    if (cTimeoutMillis == 0)
                        {
                        heartbeat();
                        }
                    else
                        {
                        heartbeat(cTimeoutMillis);
                        }
            
                    if (!wrapper.isManagementTask())
                        {
                        cTasks++;
                        }
            
                    try
                        {
                        wrapper.run();
                        if (fOnce)
                            {
                            setExiting(true);
                            }
                        }
                    catch (EventDeathException e)
                        {
                        Runnable task = wrapper.getTask();
                        if (task instanceof DaemonPool.StopTask)
                            {
                            pool.onDaemonStop(this, (DaemonPool.StopTask) task);
                            setExiting(true);
                            }
                        else
                            {
                            throw e;
                            }
                        }
                    finally
                        {
                        setWrapperTask(null);
            
                        release(wrapper);
                        wrapper = null; // avoid double-release if exception is thrown
                        }
                    }
                }
            finally
                {
                // account for all the work done before return or "stop"
                if (cTasks > 0)
                    {
                    pool.updateStats(this, cTasks, ldtStart);
                    }
            
                release(wrapper);
                heartbeat(); // reset the timeout value that could be set by the previous task
                }
            }
        
        // Declared at the super level
        /**
         * Event notification called when  the daemon's Thread is waiting for
        * work.
        * 
        * @see #run
         */
        protected void onWait()
                throws java.lang.InterruptedException
            {
            if (getWrapperTask() == null)
                {
                super.onWait();
                }
            }
        
        /**
         * Release the given task back to the pool that it was removed from.
         */
        protected void release(DaemonPool.WrapperTask wrapper)
            {
            // import com.oracle.coherence.common.util.AssociationPile;
            // import com.tangosol.util.Gate;
            
            if (wrapper != null)
                {
                AssociationPile queue = (AssociationPile) wrapper.get_Feed();
                if (queue != null)
                    {
                    wrapper.set_Feed(null);  // avoid double-release if exception is thrown
                    queue.release(wrapper);
                    }
            
                Gate gate = wrapper.getGate();
                if (gate != null)
                    {
                    gate.exit();
                    wrapper.setGate(null);
                    }
                }
            }
        
        /**
         * Check queues handled by other daemons for any outstanding jobs this
        * Daemon can help with.
         */
        protected DaemonPool.WrapperTask removeFromAnotherQueue()
            {
            // import Component.Util.DaemonPool$WorkSlot as DaemonPool.WorkSlot;
            // import com.oracle.coherence.common.util.AssociationPile;
            // import com.tangosol.util.Base;
            // import com.tangosol.util.Gate;
            
            DaemonPool pool = (DaemonPool) get_Parent();
            if (pool.getQueues().length <= 1)
                {
                // no other queues to help
                return null;
                }
            
            AssociationPile queueThis = getQueue();
            for (int i = 0, c = pool.getWorkSlotCount(), nHash = hashCode(); i < c; i++)
                {
                DaemonPool.WorkSlot slotThat = pool.getWorkSlot(Base.mod(nHash + i, c));
            
                Gate gateThat = slotThat.getGate();
                if (gateThat.enter(0L)) // see DaemonPool#onDaemonStop()
                    {
                    AssociationPile queueThat = slotThat.getQueue();
                    if (queueThat != queueThis && slotThat.isActive())
                        {
                        DaemonPool.WrapperTask wrapper = (DaemonPool.WrapperTask) queueThat.poll();
                        if (wrapper != null)
                            {
                            if (wrapper.isManagementTask())
                                {
                                // those are not "transferable" tasks; put them back
                                queueThat.release(wrapper);
                                queueThat.add(wrapper);
                                }
                            else
                                {
                                wrapper.set_Feed(queueThat);
                                wrapper.setGate(gateThat); // see #release()
                                return wrapper;
                                }
                            }
                        }
                    gateThat.exit();
                    }
                }
            
            return null;
            }
        
        /**
         * Check the queue handled by this Daemon for any outstanding jobs.
         */
        protected DaemonPool.WrapperTask removeFromQueue()
            {
            // import com.oracle.coherence.common.util.AssociationPile;
            
            AssociationPile queue   = getQueue();
            DaemonPool.WrapperTask    wrapper = (DaemonPool.WrapperTask) queue.poll();
            
            if (wrapper != null)
                {
                wrapper.set_Feed(queue);
                }
            
            return wrapper;
            }
        
        // Accessor for the property "DaemonType"
        /**
         * Setter for property DaemonType.<p>
        * One of the $Module.DAEMON_* constants.
         */
        public void setDaemonType(int nType)
            {
            __m_DaemonType = nType;
            }
        
        // Accessor for the property "FlushStats"
        /**
         * Setter for property FlushStats.<p>
        * Flag that indicates the Daemon should report its latest statistics to
        * the DaemonPool.
        * 
        * @volatile
         */
        public void setFlushStats(boolean fFlush)
            {
            __m_FlushStats = fFlush;
            }
        
        // Declared at the super level
        /**
         * Setter for property GuardSupport.<p>
        * The GuardSupport used by this Daemon Guardian to manage its Guardable
        * responsibilities.
         */
        public void setGuardSupport(com.tangosol.net.GuardSupport guardSupport)
            {
            super.setGuardSupport(guardSupport);
            }
        
        // Accessor for the property "InterruptCount"
        /**
         * Setter for property InterruptCount.<p>
        * Number of times this daemon thread was attempted to be interrupted.
        * If this number reaches the threshold, the daemon thread will be
        * abandoned.
        * 
        * @volatile
         */
        public void setInterruptCount(int cInterrupts)
            {
            __m_InterruptCount = cInterrupts;
            }
        
        // Accessor for the property "Queue"
        /**
         * Setter for property Queue.<p>
        * The Queue from which this Daemon extracts Runnable tasks.
         */
        public void setQueue(com.oracle.coherence.common.util.AssociationPile queue)
            {
            // import Component.Util.Queue.ConcurrentQueue;
            // import com.oracle.coherence.common.base.Notifier;
            
            _assert(getQueue() == null, "Queue is not resettable");
            
            __m_Queue = (queue);
            if (queue instanceof Notifier)
               {
               if (queue instanceof ConcurrentQueue)
                   {
                   setNotifier(((ConcurrentQueue) queue).getNotifier());
                   }
               else
                   {
                   setNotifier((Notifier) queue);
                   }
               }
            }
        
        // Accessor for the property "WrapperTask"
        /**
         * Setter for property WrapperTask.<p>
        * Currently running task. If the Task is set before the daemon thread
        * starts, the thread will terminate as soon as the task completes.
        * 
        * @volatile
         */
        public void setWrapperTask(DaemonPool.WrapperTask task)
            {
            // only the Daemon thread itself is allowed to switch tasks
            Thread thread = getThread();
            _assert(thread == null || thread == Thread.currentThread());
            
            __m_WrapperTask = (task);
            
            if (getInterruptCount() > 0)
                {
                // this worker has been unassigned a task after a successful recovery
                Thread.interrupted(); // reset the interrupt flag
                setInterruptCount(0); // reset the interrupt count
            
                // accelerate the discovery of the recovery
                heartbeat();
                }
            }
        
        // Declared at the super level
        /**
         * Starts the daemon thread associated with this component. If the
        * thread is already starting or has started, invoking this method has
        * no effect.
        * 
        * Synchronization is used here to verify that the start of the thread
        * occurs; the lock is obtained before the thread is started, and the
        * daemon thread notifies back that it has started from the run() method.
         */
        public void start()
            {
            // guard the worker daemon with the DaemonPool
            ((DaemonPool) get_Module()).guard(getGuardable());
            
            super.start();
            }

        protected Thread instantiateThread()
            {
            Thread thread = useVirtualThreads()
                   ? VirtualThreads.makeThread(getThreadGroup(), this, getThreadName())
                   : Base.makeThread(getThreadGroup(), this, getThreadName());

            thread.setDaemon(true);
            int nPriority = getPriority();
            if (nPriority != 0)
                {
                thread.setPriority(nPriority);
                }

            return thread;
            }

        protected boolean useVirtualThreads()
            {
            DaemonPool pool = (DaemonPool) get_Module();
            return VirtualThreads.isSupported()
                   && VirtualThreads.isEnabled(pool.getName())
                   && (pool.isDynamic() || getDaemonType() == DAEMON_NONPOOLED);
            }

        // Declared at the super level
        public String toString()
            {
            return getThreadName() + "@" + System.identityHashCode(this) +
                " QueueId=" + System.identityHashCode(getQueue());
            }

        // ---- class: com.tangosol.coherence.component.util.DaemonPool$Daemon$Guard
        
        /**
         * Guard provides the Guardable interface implementation for the Daemon.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Guard
                extends    com.tangosol.coherence.component.util.Daemon.Guard
            {
            // ---- Fields declarations ----
            private static com.tangosol.util.ListMap __mapChildren;
            
            // Static initializer
            static
                {
                __initStatic();
                }
            
            // Default static initializer
            private static void __initStatic()
                {
                // register child classes
                __mapChildren = new com.tangosol.util.ListMap();
                __mapChildren.put("Abandon", DaemonPool.Daemon.Guard.Abandon.get_CLASS());
                }
            
            // Default constructor
            public Guard()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Guard(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // containment initialization: children
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.DaemonPool.Daemon.Guard();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool$Daemon$Guard".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Declared at the super level
            public void recover()
                {
                // import com.tangosol.net.Guardian$GuardContext as com.tangosol.net.Guardian.GuardContext;
                // import com.tangosol.util.Base;
                
                DaemonPool pool   = (DaemonPool) get_Module();
                DaemonPool.Daemon daemon = (DaemonPool.Daemon) get_Parent();
                
                synchronized (pool.STATS_MONITOR)
                    {
                    pool.setStatsTimeoutCount(pool.getStatsTimeoutCount() + 1);
                    }
                if (daemon.isStarted() && !daemon.isExiting())
                    {
                    DaemonPool.WrapperTask wrapper = daemon.getWrapperTask();
                    if (wrapper != null)
                        {
                        String       sTaskId     = wrapper.getTaskId();
                        Thread       thread      = daemon.getThread();
                        int          cInterrupts = daemon.getInterruptCount();
                        com.tangosol.net.Guardian.GuardContext context     = getContext();
                
                        if (cInterrupts == 0)
                            {
                            long cTime = Base.getSafeTimeMillis() - wrapper.getStartTime();
                            _trace("A worker thread has been executing task: " +
                                   sTaskId + " for " + cTime + "ms and appears" +
                                   " to be stuck; attempting to interrupt: " +
                                   daemon.getThreadName(), 2);
                            }
                
                        // Try to interrupt the worker daemon up to the configured
                        // abandon-threshold number of times.  We determine that a
                        // worker has been successfully recovered if the context is
                        // healthy (indicating that a heartbeat was issued).
                        long cRecoveryMillis = pool.getRecoveryDelay();
                        for (int i = 0, c = pool.getAbandonThreshold(); i < c; i++)
                            {
                            daemon.setInterruptCount(++cInterrupts);
                            thread.interrupt();
                
                            // put current thread to a short sleep before trying to interrupt daemon thread again
                            DaemonPool.Daemon.sleep(cRecoveryMillis);
                
                            if (daemon.getInterruptCount() == 0 // the worker thread has recovered and reset the counter
                            || context == null                  // worker was abandoned by the daemon pool
                            || context.getState() == context.STATE_HEALTHY)
                                {
                                break;
                                }
                            }
                        }
                    else
                        {
                        // The worker daemon is not processing a task; it must be
                        // wedged somewhere else.  All we can do is a Thread interrupt.
                        daemon.getThread().interrupt();
                        }
                    }
                }
            
            // Declared at the super level
            public void terminate()
                {
                // import com.tangosol.util.Base;
                
                DaemonPool pool   = (DaemonPool) get_Module();
                DaemonPool.Daemon daemon = (DaemonPool.Daemon) get_Parent();
                
                daemon.setDaemonType(DaemonPool.DAEMON_ABANDONED);
                
                // try to stop the daemon on a dedicated thread
                Base.makeThread(null, (Runnable) _newChild("Abandon"), "Abandon").start();
                
                // Note:
                // We don't reach up and terminate the service here, even if we
                // don't successfully kill the stuck worker.  If the worker was stuck
                // in a way that would materially affect the service (e.g. it is
                // deadlocked with the service thread), then we expect the service
                // thread to be torn down shortly by its Guardian.    
                //
                // If the workers in the pool are locked up in a way to prevent
                // queued tasks from being processed, the service thread will notice
                // and will not issue heartbeats, and in time, will be
                // recovered/terminated.
                
                // replace the abandoned one
                pool.replaceDaemon(daemon);
                }

            // ---- class: com.tangosol.coherence.component.util.DaemonPool$Daemon$Guard$Abandon
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Abandon
                    extends    com.tangosol.coherence.Component
                    implements Runnable
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Abandon()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Abandon(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                    {
                    super(sName, compParent, false);
                    
                    if (fInit)
                        {
                        __init();
                        }
                    }
                
                // Main initializer
                public void __init()
                    {
                    // private initialization
                    __initPrivate();
                    
                    
                    // signal the end of the initialization
                    set_Constructed(true);
                    }
                
                // Private initializer
                protected void __initPrivate()
                    {
                    
                    super.__initPrivate();
                    }
                
                //++ getter for static property _Instance
                /**
                 * Getter for property _Instance.<p>
                * Auto generated
                 */
                public static com.tangosol.coherence.Component get_Instance()
                    {
                    return new com.tangosol.coherence.component.util.DaemonPool.Daemon.Guard.Abandon();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool$Daemon$Guard$Abandon".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                // From interface: java.lang.Runnable
                public void run()
                    {
                    ((DaemonPool.Daemon) get_Parent().get_Parent()).abandon();
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.DaemonPool$ResizeTask
    
    /**
     * Runnable periodic task used to implement the dynamic resizing algorithm.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ResizeTask
            extends    com.tangosol.coherence.component.Util
            implements Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property ActiveCountAverage
         *
         * A running average for the number of active Daemon threads. This
         * average is heavily weighed toward the most recent active count.
         */
        private transient double __m_ActiveCountAverage;
        
        /**
         * Property ADJUST_FASTER
         *
         * Indicates an intent to decrease the interval between the periodic
         * Resize task executions.
         */
        public static final int ADJUST_FASTER = 1;
        
        /**
         * Property ADJUST_SLOWER
         *
         * Indicates an intent to increase the interval between the periodic
         * Resize task executions.
         */
        public static final int ADJUST_SLOWER = 2;
        
        /**
         * Property Debug
         *
         */
        private static boolean __s_Debug;
        
        /**
         * Property IdleFraction
         *
         */
        private static double __s_IdleFraction;
        
        /**
         * Property IdleLimit
         *
         */
        private static int __s_IdleLimit;
        
        /**
         * Property LastActiveMillis
         *
         * The last number of total number of milliseconds spent by all Daemon
         * threads while executing tasks measured by this task.
         */
        private long __m_LastActiveMillis;
        
        /**
         * Property LastResize
         *
         * The number of daemon threads added (positive) or removed (negative)
         * during the last resize performed by this task.
         */
        private int __m_LastResize;
        
        /**
         * Property LastResizeMillis
         *
         * The last time this task resized the DaemonPool.
         */
        private long __m_LastResizeMillis;
        
        /**
         * Property LastRunMillis
         *
         * The last time this task was executed.
         */
        private long __m_LastRunMillis;
        
        /**
         * Property LastShakeMillis
         *
         * The last time the DaemonPool was "shaken" (thread count tickled) by
         * this task.
         */
        private long __m_LastShakeMillis;
        
        /**
         * Property LastTaskCount
         *
         * The last DaemonPool task count measured by this task.
         */
        private long __m_LastTaskCount;
        
        /**
         * Property LastThreadCount
         *
         * The last DaemonPool size measured by this task.
         */
        private int __m_LastThreadCount;
        
        /**
         * Property LastThroughput
         *
         * The last DaemonPool throughput measured by this task.
         */
        private double __m_LastThroughput;
        
        /**
         * Property PeriodAdjust
         *
         */
        private static long __s_PeriodAdjust;
        
        /**
         * Property PeriodMax
         *
         */
        private static long __s_PeriodMax;
        
        /**
         * Property PeriodMillis
         *
         * The period (in milliseconds) between successive executions of this
         * task.
         */
        private long __m_PeriodMillis;
        
        /**
         * Property PeriodMin
         *
         */
        private static long __s_PeriodMin;
        
        /**
         * Property PeriodShake
         *
         */
        private static long __s_PeriodShake;
        
        /**
         * Property ResizeGrow
         *
         */
        private static double __s_ResizeGrow;
        
        /**
         * Property ResizeJitter
         *
         */
        private static double __s_ResizeJitter;
        
        /**
         * Property ResizeShake
         *
         */
        private static double __s_ResizeShake;
        
        /**
         * Property ResizeShrink
         *
         */
        private static double __s_ResizeShrink;
        
        private static void _initStatic$Default()
            {
            __initStatic();
            }
        
        // Static initializer (from _initStatic)
        static
            {
            // import com.tangosol.coherence.config.Config;
            
            _initStatic$Default();
            
            setDebug(Config.getBoolean("coherence.daemonpool.debug"));
            
            // configure the various "knobs"
            setIdleFraction(getDoubleProperty ("coherence.daemonpool.idle.fraction",     getIdleFraction()));
            setIdleLimit   (getIntegerProperty("coherence.daemonpool.idle.threshold",    getIdleLimit()));
            setPeriodAdjust(getLongProperty   ("coherence.daemonpool.adjust.period",     getPeriodAdjust()));
            setPeriodMax   (getLongProperty   ("coherence.daemonpool.max.period",        getPeriodMax()));
            setPeriodMin   (getLongProperty   ("coherence.daemonpool.min.period",        getPeriodMin()));
            setPeriodShake (getLongProperty   ("coherence.daemonpool.shake.period",      getPeriodShake()));
            setResizeGrow  (getDoubleProperty ("coherence.daemonpool.grow.percentage",   getResizeGrow()));
            setResizeJitter(getDoubleProperty ("coherence.daemonpool.jitter.percentage", getResizeJitter()));
            setResizeShake (getDoubleProperty ("coherence.daemonpool.shake.percentage",  getResizeShake()));
            setResizeShrink(getDoubleProperty ("coherence.daemonpool.shrink.percentage", getResizeShrink()));
            
            if (isDebug())
                {
                Object[] ao = new Object[]
                    {
                    Double.valueOf(getIdleFraction()),
                    Integer.valueOf(getIdleLimit()),
                    Long.valueOf(getPeriodAdjust()),
                    Long.valueOf(getPeriodMax()),
                    Long.valueOf(getPeriodMin()),
                    Long.valueOf(getPeriodShake()),
                    Double.valueOf(getResizeGrow()),
                    Double.valueOf(getResizeJitter()),
                    Double.valueOf(getResizeShake()),
                    Double.valueOf(getResizeShrink()),
                    };
                _trace(String.format("ResizeTask[IdleFraction=%.2f, IdleLimit=%d, PeriodAdjust=%d, PeriodMax=%d, PeriodMin=%d, PeriodShake=%d, ResizeGrow=%.3f, ResizeJitter=%.3f, ResizeShake=%.3f, ResizeShrink=%.3f]", ao), 3);
                }
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // state initialization: static properties
            try
                {
                setIdleFraction(0.333);
                setIdleLimit(20);
                setPeriodAdjust(250L);
                setPeriodMax(10000L);
                setPeriodMin(100L);
                setPeriodShake(600000L);
                setResizeGrow(1.0);
                setResizeJitter(0.05);
                setResizeShake(0.15);
                setResizeShrink(0.25);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            }
        
        // Default constructor
        public ResizeTask()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ResizeTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.DaemonPool.ResizeTask();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool$ResizeTask".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        /**
         * Adjust the period of this task.
        * 
        * @param nAdjust one of the ADJUST_ constants
        * 
        * @return the new period
         */
        protected long adjustPeriod(int nAdjust, int cThreadsOld, int cThreadsNew)
            {
            // only adjust the period if it's possible to do so
            long cMillis = getPeriodMillis();
            
            switch (nAdjust)
                {
                case ADJUST_FASTER:
                    if (cThreadsOld != cThreadsNew)
                        {
                        // NOTE: we accelerate the analysis AND schedule the next one "immediately"
                        setPeriodMillis(Math.max(getPeriodMin(), cMillis - getPeriodAdjust()));
                        cMillis = getPeriodMin();
                        }
                    break;
            
                case ADJUST_SLOWER:
                    setPeriodMillis(cMillis = Math.min(getPeriodMax(), cMillis + getPeriodAdjust()));
                    break;
                }
            
            return cMillis;
            }
        
        /**
         * Cancel the periodic task.
         */
        public synchronized void cancel()
            {
            // the best we can do is to drop the parent reference
            // and leave a trivial shell to be discarded when it runs
            
            // the daemon pool will be null if this task is already cancelled
            DaemonPool pool = getDaemonPool();
            if (pool != null)
                {
                get_Parent()._unlinkChild(this);
                }
            }
        
        private static String format2f(double dfl)
            {
            return String.format("%.2f", new Object[] { Double.valueOf(dfl) });
            }
        
        // Accessor for the property "ActiveCountAverage"
        /**
         * Getter for property ActiveCountAverage.<p>
        * A running average for the number of active Daemon threads. This
        * average is heavily weighed toward the most recent active count.
         */
        public double getActiveCountAverage()
            {
            return __m_ActiveCountAverage;
            }
        
        // Accessor for the property "DaemonCount"
        /**
         * Getter for property DaemonCount.<p>
        * The current number of Daemon threads.
         */
        public int getDaemonCount()
            {
            return getDaemonPool().getDaemonCount();
            }
        
        // Accessor for the property "DaemonPool"
        /**
         * Getter for property DaemonPool.<p>
        * The parent DaemonPool.
         */
        public DaemonPool getDaemonPool()
            {
            return (DaemonPool) get_Module();
            }
        
        /**
         * Parse the value of the named system property as a double.
        * 
        * @param sName  the name of the system property to parse
        * @param dflDefault  the value to return if the system property is not
        * defined or has an unparseable value
        * 
        * @return the parsed value
         */
        protected static double getDoubleProperty(String sName, double dflDefault)
            {
            // import com.tangosol.coherence.config.Config;
            
            try
                {
                return Double.parseDouble(Config.getProperty(sName));
                }
            catch (RuntimeException e)
                {
                return dflDefault;
                }
            }
        
        // Accessor for the property "IdleFraction"
        /**
         * Getter for property IdleFraction.<p>
         */
        public static double getIdleFraction()
            {
            return __s_IdleFraction;
            }
        
        // Accessor for the property "IdleLimit"
        /**
         * Getter for property IdleLimit.<p>
         */
        public static int getIdleLimit()
            {
            return __s_IdleLimit;
            }
        
        /**
         * Parse the value of the named system property as an integer.
        * 
        * @param sName  the name of the system property to parse
        * @param nDefault  the value to return if the system property is not
        * defined or has an unparseable value
        * 
        * @return the parsed value
         */
        protected static int getIntegerProperty(String sName, int nDefault)
            {
            // import com.tangosol.coherence.config.Config;
            
            try
                {
                return Config.getInteger(sName, nDefault).intValue();
                }
            catch (RuntimeException e)
                {
                return nDefault;
                }
            }
        
        // Accessor for the property "LastActiveMillis"
        /**
         * Getter for property LastActiveMillis.<p>
        * The last number of total number of milliseconds spent by all Daemon
        * threads while executing tasks measured by this task.
         */
        public long getLastActiveMillis()
            {
            return __m_LastActiveMillis;
            }
        
        // Accessor for the property "LastResize"
        /**
         * Getter for property LastResize.<p>
        * The number of daemon threads added (positive) or removed (negative)
        * during the last resize performed by this task.
         */
        public int getLastResize()
            {
            return __m_LastResize;
            }
        
        // Accessor for the property "LastResizeMillis"
        /**
         * Getter for property LastResizeMillis.<p>
        * The last time this task resized the DaemonPool.
         */
        public long getLastResizeMillis()
            {
            return __m_LastResizeMillis;
            }
        
        // Accessor for the property "LastRunMillis"
        /**
         * Getter for property LastRunMillis.<p>
        * The last time this task was executed.
         */
        public long getLastRunMillis()
            {
            return __m_LastRunMillis;
            }
        
        // Accessor for the property "LastShakeMillis"
        /**
         * Getter for property LastShakeMillis.<p>
        * The last time the DaemonPool was "shaken" (thread count tickled) by
        * this task.
         */
        public long getLastShakeMillis()
            {
            return __m_LastShakeMillis;
            }
        
        // Accessor for the property "LastTaskCount"
        /**
         * Getter for property LastTaskCount.<p>
        * The last DaemonPool task count measured by this task.
         */
        public long getLastTaskCount()
            {
            return __m_LastTaskCount;
            }
        
        // Accessor for the property "LastThreadCount"
        /**
         * Getter for property LastThreadCount.<p>
        * The last DaemonPool size measured by this task.
         */
        public int getLastThreadCount()
            {
            return __m_LastThreadCount;
            }
        
        // Accessor for the property "LastThroughput"
        /**
         * Getter for property LastThroughput.<p>
        * The last DaemonPool throughput measured by this task.
         */
        public double getLastThroughput()
            {
            return __m_LastThroughput;
            }
        
        /**
         * Parse the value of the named system property as a long.
        * 
        * @param sName  the name of the system property to parse
        * @param nDefault  the value to return if the system property is not
        * defined or has an unparseable value
        * 
        * @return the parsed value
         */
        protected static long getLongProperty(String sName, long nDefault)
            {
            // import com.tangosol.coherence.config.Config;
            
            try
                {
                return Config.getLong(sName, nDefault).longValue();
                }
            catch (RuntimeException e)
                {
                return nDefault;
                }
            }
        
        // Accessor for the property "PeriodAdjust"
        /**
         * Getter for property PeriodAdjust.<p>
         */
        public static long getPeriodAdjust()
            {
            return __s_PeriodAdjust;
            }
        
        // Accessor for the property "PeriodMax"
        /**
         * Getter for property PeriodMax.<p>
         */
        public static long getPeriodMax()
            {
            return __s_PeriodMax;
            }
        
        // Accessor for the property "PeriodMillis"
        /**
         * Getter for property PeriodMillis.<p>
        * The period (in milliseconds) between successive executions of this
        * task.
         */
        public long getPeriodMillis()
            {
            return __m_PeriodMillis;
            }
        
        // Accessor for the property "PeriodMin"
        /**
         * Getter for property PeriodMin.<p>
         */
        public static long getPeriodMin()
            {
            return __s_PeriodMin;
            }
        
        // Accessor for the property "PeriodShake"
        /**
         * Getter for property PeriodShake.<p>
         */
        public static long getPeriodShake()
            {
            return __s_PeriodShake;
            }
        
        // Accessor for the property "ResizeGrow"
        /**
         * Getter for property ResizeGrow.<p>
         */
        public static double getResizeGrow()
            {
            return __s_ResizeGrow;
            }
        
        // Accessor for the property "ResizeJitter"
        /**
         * Getter for property ResizeJitter.<p>
         */
        public static double getResizeJitter()
            {
            return __s_ResizeJitter;
            }
        
        // Accessor for the property "ResizeShake"
        /**
         * Getter for property ResizeShake.<p>
         */
        public static double getResizeShake()
            {
            return __s_ResizeShake;
            }
        
        // Accessor for the property "ResizeShrink"
        /**
         * Getter for property ResizeShrink.<p>
         */
        public static double getResizeShrink()
            {
            return __s_ResizeShrink;
            }
        
        /**
         * Grow the DaemonPool.
        * 
        * @return the new size of the pool
         */
        protected int growDaemonPool(String sReason)
            {
            int cThreads = getDaemonCount();
            
            // no reason to grow an underutilized pool
            return isDaemonPoolUnderutilized() ? cThreads :
                resizeDaemonPool(Math.max(1, (int) (cThreads * getResizeGrow())), sReason);
            }
        
        /**
         * Determine if the DaemonPool is overutilized.
         *
         * @return true if the DaemonPool is overutilized
         */
        protected boolean isDaemonPoolOverutilized()
            {
            int    cThreads  = getDaemonCount();
            double dflActive = getActiveCountAverage();
            int    cIdle     = (int) (cThreads - dflActive);
            
            return cThreads < getDaemonPool().getDaemonCountMax()
                && dflActive > cThreads*(1 - getIdleFraction())
                && cIdle < getIdleLimit()
                && getLastThroughput() > 0.0;
            }
        
        /**
         * Determine if the DaemonPool is underutilized.
         *
         * @return true if the DaemonPool is underutilized
         */
        protected boolean isDaemonPoolUnderutilized()
            {
            int cThreads = getDaemonCount();
            
            return cThreads > getDaemonPool().getDaemonCountMin()
                && (getActiveCountAverage() < cThreads * getIdleFraction() || getLastThroughput() == 0.0);
            }
        
        // Accessor for the property "Debug"
        /**
         * Getter for property Debug.<p>
         */
        public static boolean isDebug()
            {
            return __s_Debug;
            }
        
        /**
         * Check if the current statistics are suspect and need to be
        * re-collected.
         */
        protected static boolean isStatisticsSuspect(long ldtLastRun, long ldtLastResize, long ldtResizeEnd, long ldtReset)
            {
            // statistics are suspect if
            //   1) they were reset after the last run, or
            //   2) the last run caused a resize, but that resize took a significant part
            //      (over 10%) of the last measuring period
            return ldtLastRun <= ldtReset
                || (ldtLastRun == ldtLastResize
                    && ldtResizeEnd - ldtLastResize > getPeriodMin() / 10);
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // import com.tangosol.util.Base;
            
            long ldtNow = Base.getSafeTimeMillis();
            setLastRunMillis(ldtNow);
            setLastShakeMillis(ldtNow);
            setPeriodMillis(getPeriodMin());
            
            DaemonPool pool = getDaemonPool();
            synchronized (pool.STATS_MONITOR)
                {
                setLastActiveMillis(pool.getStatsActiveMillis());
                setLastTaskCount(pool.getStatsTaskCount());
                }
            setLastThreadCount(pool.getDaemonCount());
            
            super.onInit();
            }
        
        /**
         * Given an optional reason, return a formatted string for logging.
        * 
        * @param sReason  the optional reason
        * @return a formatted string for logging
         */
        protected static String reasonToString(String sReason)
            {
            if (sReason == null || sReason.isEmpty())
                {
                return "";
                }
            
            return " due to " + sReason;
            }
        
        /**
         * Resize the DaemonPool.
         *
         * @param cDelta the number of daemon threads to add or remove
         * @param sReason an optional debug message as to why the pool is being
         * resized
         *
         * @return the new size of the pool
         */
        protected int resizeDaemonPool(int cDelta, String sReason)
            {
            // import com.tangosol.util.Base;
            
            DaemonPool pool = getDaemonPool();
            
            int cCurrent = pool.getDaemonCount();
            int cNew     = cCurrent + cDelta;
            
            if (cDelta == 0 || cNew < pool.getDaemonCountMin() || cNew > pool.getDaemonCountMax())
                {
                return cCurrent;
                }
            
            if (isDebug())
                {
                _trace("DaemonPool \"" + pool.getName() + "\" "
                        + (cDelta > 0 ? "increasing" : "decreasing")
                        + " the pool size from " + cCurrent + " to " + cNew + " thread(s)"
                        + reasonToString(sReason), 5);
                }
            
            pool.setDaemonCount(cNew);
            
            return cNew;
            }
        
        // From interface: java.lang.Runnable
        public synchronized void run()
            {
            // import com.tangosol.util.Base;
            
            // while his task always runs on a single thread, we use the synchronization to prevent
            // a concurrent execution of the "cancel" method, which nulls out the DaemonPool reference 
            
            DaemonPool pool = getDaemonPool();
            if (pool == null)
                {
                // terminated
                return;
                }
            
            pool.flushStats();
            
            long ldtNow        = Base.getSafeTimeMillis();
            long cPeriod       = getPeriodMillis();
            long cTasks        = 0L;
            long cActiveMillis = 0L;
            int  cThreads      = pool.getDaemonCount();
            int  cNew          = cThreads;
            
            if (pool.isInTransition())
                {
                // wait for the completion of the previous resize
                pool.schedule(this, getPeriodMin());
                
                if (isDebug())
                    {
                    _trace("DaemonPool \"" + pool.getName()
                         + "\": skipping analysis due to the pool resize in progress", 3);
                    }
                return;
                }
            
            try
                {
                // gather all pool stats
                long ldtReset;
                long ldtResizeEnd;
            
                synchronized (pool.STATS_MONITOR)
                    {
                    cActiveMillis = pool.getStatsActiveMillis();
                    cTasks        = pool.getStatsTaskCount();
                    ldtReset      = pool.getStatsLastResetMillis();
                    ldtResizeEnd  = pool.getStatsLastResizeMillis();
                    }
            
                long ldtLastRun    = getLastRunMillis();
                long ldtLastResize = getLastResizeMillis();
                
                if (cThreads != getLastThreadCount())
                    {
                    if (isDebug())
                        {
                        _trace("DaemonPool \"" + pool.getName()
                             + "\": skipping analysis because the pool was resized externally (expected="
                                + getLastThreadCount() + ", current=" + cThreads + ")", 3);
                        }
                    setLastResize(cThreads - getLastThreadCount());
                    return;
                    }
            
                if (isStatisticsSuspect(ldtLastRun, ldtLastResize, ldtResizeEnd, ldtReset))
                    {
                    if (isDebug())
                        {
                        _trace("DaemonPool \"" + pool.getName()
                             + "\": skipping analysis to gather new statistics after a resize or reset", 3);
                        }
                    cPeriod = getPeriodMin();
                    return;
                    }
            
                // calculate pool throughput
                long cTasksDelta  = cTasks - getLastTaskCount();
                long cMillisDelta = ldtNow - ldtLastRun;
            
                double dflTP     = cTasksDelta <= 0 || cMillisDelta <= 0L ? 0.0 : (cTasksDelta * 1000.0) / cMillisDelta;
                double dflActive = (getActiveCountAverage() + pool.getActiveDaemonCount()) / 2;
            
                // gather data used in resize analysis
                int    cLast       = getLastResize();
                double dflTPLast   = getLastThroughput();
                double dflTPDelta  = dflTP - dflTPLast;
                double dflTPJitter = dflTP * getResizeJitter();
            
                if (isDebug() && cTasksDelta > 0)
                    {
                    double dflUtilization = cMillisDelta <= 0L ? 0.0 :
                        ((double) (cActiveMillis - getLastActiveMillis()) / cMillisDelta);
                    Object[] ao = new Object[]
                        {
                        Integer.valueOf(cThreads),
                        Double.valueOf(dflActive),
                        Long.valueOf(cTasksDelta),
                        Long.valueOf(cMillisDelta),
                        Double.valueOf(dflTP),
                        Double.valueOf(dflUtilization),
                        };
                    _trace(String.format("DaemonPool \"" + pool.getName()
                        + "\": [Size=%d, Active=%.2f, DeltaTasks=%d, DeltaMills=%d, Throughput=%.1f, Utilization=%.2f]", ao), 3);
                    }
            
                setLastThroughput(dflTP);
                setActiveCountAverage(dflActive);
            
                // determine if it is time to "shake" the pool
                if (ldtNow >= getLastShakeMillis() + getPeriodShake())
                    {
                    setLastShakeMillis(ldtNow);
                    if (dflTP > 0.0)
                        {
                        cNew = shakeDaemonPool();
                        setLastResize(cNew - cThreads);
                        return;
                        }
                    }
            
                // determine if the pool should be resized
                if (Math.abs(dflTPDelta) > dflTPJitter)
                    {
                    if (cLast > 0)
                        {
                        // we last grew the pool and there was a notable throughput change:
                        //
                        // (1) if there was an increase, continue to grow the pool and
                        //     accelerate analysis
                        // (2) if there was a decrease, shrink the pool and decelerate
                        //     analysis to dampen oscillations
                        if (dflTPDelta > 0)
                            {
                            cNew    = growDaemonPool("an increase in throughput of " + format2f(dflTPDelta) + "op/sec");
                            cPeriod = adjustPeriod(ADJUST_FASTER, cThreads, cNew);
                            }
                        else
                            {
                            cNew    = shrinkDaemonPool("a decrease in throughput of " + format2f(-dflTPDelta) + "op/sec");
                            cPeriod = adjustPeriod(ADJUST_SLOWER, cThreads, cNew);
                            }
                        }
                    else if (cLast < 0)
                        {
                        // we last shrunk the pool and there was a notable throughput change:
                        //
                        // (1) if there was an increase, continue to shrink the pool and
                        //     accelerate analysis
                        // (2) if there was a decrease, grow the pool and decelerate
                        //     analysis to dampen oscillations
                        if (dflTPDelta > 0)
                            {
                            cNew    = shrinkDaemonPool("an increase in throughput of " + format2f(dflTPDelta) + "op/sec");
                            cPeriod = adjustPeriod(ADJUST_FASTER, cThreads, cNew);
                            }
                        else
                            {
                            cNew    = growDaemonPool("a decrease in throughput of " + format2f(-dflTPDelta) + "op/sec");
                            cPeriod = adjustPeriod(ADJUST_SLOWER, cThreads, cNew);
                            }
                        }
                    else // cLast == 0
                        {
                        if (dflTPDelta > 0)
                            {
                            // we didn't change anything and there was a notable throughput increase;
                            // keep it steady
                            }
                        else
                            {
                            // there was a notable throughput decrease;
                            // shrink the pool if it is underutilized or grow it if it is overutilized 
                            if (isDaemonPoolUnderutilized())
                                {
                                cNew    = shrinkDaemonPool("a decrease in throughput and the pool been underutilized");
                                cPeriod = adjustPeriod(ADJUST_FASTER, cThreads, cNew);
                                }
                            else if (isDaemonPoolOverutilized())
                                {
                                cNew    = growDaemonPool("an decrease in throughput and the pool being overutilized");
                                cPeriod = adjustPeriod(ADJUST_FASTER, cThreads, cNew);
                                }
                            }
                        }
                    }
                else if (ldtNow > ldtLastResize + cPeriod)
                    {
                    if (isDaemonPoolOverutilized())
                        {
                        cNew    = growDaemonPool("the pool being overutilized");
                        cPeriod = adjustPeriod(ADJUST_FASTER, cThreads, cNew);
                        }
                    else if (isDaemonPoolUnderutilized())
                        {
                        cNew    = shrinkDaemonPool("the pool being underutilized");
                        cPeriod = adjustPeriod(ADJUST_FASTER, cThreads, cNew);
                        }
                    else
                        {
                        // an optimal thread count
                        cPeriod = adjustPeriod(ADJUST_SLOWER, cThreads, cNew);
                        }
                    }
            
                setLastResize(cNew - cThreads);
                }
            finally
                {
                if (cNew != cThreads)
                    {
                    setLastResizeMillis(ldtNow);
                    }
            
                setLastRunMillis(ldtNow);
                setLastTaskCount(cTasks);
                setLastThreadCount(cNew);
                setLastActiveMillis(cActiveMillis);
            
                pool.schedule(this, cPeriod);
                }
            }
        
        // Accessor for the property "ActiveCountAverage"
        /**
         * Setter for property ActiveCountAverage.<p>
        * A running average for the number of active Daemon threads. This
        * average is heavily weighed toward the most recent active count.
         */
        protected void setActiveCountAverage(double dflAverage)
            {
            __m_ActiveCountAverage = dflAverage;
            }
        
        // Accessor for the property "Debug"
        /**
         * Setter for property Debug.<p>
         */
        public static void setDebug(boolean fDebug)
            {
            __s_Debug = fDebug;
            }
        
        // Accessor for the property "IdleFraction"
        /**
         * Setter for property IdleFraction.<p>
         */
        public static void setIdleFraction(double dFraction)
            {
            __s_IdleFraction = dFraction;
            }
        
        // Accessor for the property "IdleLimit"
        /**
         * Setter for property IdleLimit.<p>
         */
        public static void setIdleLimit(int nLimit)
            {
            __s_IdleLimit = nLimit;
            }
        
        // Accessor for the property "LastActiveMillis"
        /**
         * Setter for property LastActiveMillis.<p>
        * The last number of total number of milliseconds spent by all Daemon
        * threads while executing tasks measured by this task.
         */
        protected void setLastActiveMillis(long cMillis)
            {
            __m_LastActiveMillis = cMillis;
            }
        
        // Accessor for the property "LastResize"
        /**
         * Setter for property LastResize.<p>
        * The number of daemon threads added (positive) or removed (negative)
        * during the last resize performed by this task.
         */
        protected void setLastResize(int cThreads)
            {
            __m_LastResize = cThreads;
            }
        
        // Accessor for the property "LastResizeMillis"
        /**
         * Setter for property LastResizeMillis.<p>
        * The last time this task resized the DaemonPool.
         */
        protected void setLastResizeMillis(long ldtResize)
            {
            __m_LastResizeMillis = ldtResize;
            }
        
        // Accessor for the property "LastRunMillis"
        /**
         * Setter for property LastRunMillis.<p>
        * The last time this task was executed.
         */
        protected void setLastRunMillis(long ldtRun)
            {
            __m_LastRunMillis = ldtRun;
            }
        
        // Accessor for the property "LastShakeMillis"
        /**
         * Setter for property LastShakeMillis.<p>
        * The last time the DaemonPool was "shaken" (thread count tickled) by
        * this task.
         */
        protected void setLastShakeMillis(long ldtDown)
            {
            __m_LastShakeMillis = ldtDown;
            }
        
        // Accessor for the property "LastTaskCount"
        /**
         * Setter for property LastTaskCount.<p>
        * The last DaemonPool task count measured by this task.
         */
        protected void setLastTaskCount(long cTasks)
            {
            __m_LastTaskCount = cTasks;
            }
        
        // Accessor for the property "LastThreadCount"
        /**
         * Setter for property LastThreadCount.<p>
        * The last DaemonPool size measured by this task.
         */
        protected void setLastThreadCount(int cThreads)
            {
            __m_LastThreadCount = cThreads;
            }
        
        // Accessor for the property "LastThroughput"
        /**
         * Setter for property LastThroughput.<p>
        * The last DaemonPool throughput measured by this task.
         */
        protected void setLastThroughput(double dflTP)
            {
            __m_LastThroughput = dflTP;
            }
        
        // Accessor for the property "PeriodAdjust"
        /**
         * Setter for property PeriodAdjust.<p>
         */
        public static void setPeriodAdjust(long lAdjust)
            {
            __s_PeriodAdjust = lAdjust;
            }
        
        // Accessor for the property "PeriodMax"
        /**
         * Setter for property PeriodMax.<p>
         */
        public static void setPeriodMax(long lAdjust)
            {
            __s_PeriodMax = lAdjust;
            }
        
        // Accessor for the property "PeriodMillis"
        /**
         * Setter for property PeriodMillis.<p>
        * The period (in milliseconds) between successive executions of this
        * task.
         */
        protected void setPeriodMillis(long cMillis)
            {
            __m_PeriodMillis = cMillis;
            }
        
        // Accessor for the property "PeriodMin"
        /**
         * Setter for property PeriodMin.<p>
         */
        public static void setPeriodMin(long lAdjust)
            {
            __s_PeriodMin = lAdjust;
            }
        
        // Accessor for the property "PeriodShake"
        /**
         * Setter for property PeriodShake.<p>
         */
        public static void setPeriodShake(long lAdjust)
            {
            __s_PeriodShake = lAdjust;
            }
        
        // Accessor for the property "ResizeGrow"
        /**
         * Setter for property ResizeGrow.<p>
         */
        public static void setResizeGrow(double dGrow)
            {
            __s_ResizeGrow = dGrow;
            }
        
        // Accessor for the property "ResizeJitter"
        /**
         * Setter for property ResizeJitter.<p>
         */
        public static void setResizeJitter(double dGrow)
            {
            __s_ResizeJitter = dGrow;
            }
        
        // Accessor for the property "ResizeShake"
        /**
         * Setter for property ResizeShake.<p>
         */
        public static void setResizeShake(double dGrow)
            {
            __s_ResizeShake = dGrow;
            }
        
        // Accessor for the property "ResizeShrink"
        /**
         * Setter for property ResizeShrink.<p>
         */
        public static void setResizeShrink(double dGrow)
            {
            __s_ResizeShrink = dGrow;
            }
        
        /**
         * Resize the DaemonPool by a random amount.
        * 
        * @return the new size of the pool
         */
        protected int shakeDaemonPool()
            {
            // import com.tangosol.util.Base;
            
            int cDelta = Math.max(1, (int) (getDaemonCount() * getResizeShake()));
            int cRange = 2*cDelta + 1;
            
            return resizeDaemonPool(Base.getRandom().nextInt(cRange) - cDelta, "the pool being shaken");
            }
        
        /**
         * Shrink the DaemonPool.
        * 
        * @return the new size of the pool
         */
        protected int shrinkDaemonPool(String sReason)
            {
            return resizeDaemonPool(-Math.max(1, (int) (getDaemonCount() * getResizeShrink())), sReason);
            }
        
        // Declared at the super level
        public String toString()
            {
            return get_Name() + " for " + getDaemonPool();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.DaemonPool$ScheduleTask
    
    /**
     * Runnable task that is used to schedule a task to be added to the
     * DaemonPool.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ScheduleTask
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.PriorityTask,
                       Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property DelayMillis
         *
         * The delay in milliseconds before the task will be added to the
         * DaemonPool.
         */
        private long __m_DelayMillis;
        
        /**
         * Property Task
         *
         * The task to add to the DaemonPool.
         */
        private Runnable __m_Task;
        
        // Default constructor
        public ScheduleTask()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ScheduleTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.DaemonPool.ScheduleTask();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool$ScheduleTask".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "DaemonPool"
        /**
         * Getter for property DaemonPool.<p>
        * The parent DaemonPool.
         */
        public DaemonPool getDaemonPool()
            {
            return (DaemonPool) get_Module();
            }
        
        // Accessor for the property "DelayMillis"
        /**
         * Getter for property DelayMillis.<p>
        * The delay in milliseconds before the task will be added to the
        * DaemonPool.
         */
        public long getDelayMillis()
            {
            return __m_DelayMillis;
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getExecutionTimeoutMillis()
            {
            // import com.tangosol.net.PriorityTask;
            
            Runnable task = getTask();
            
            // The execution timeout is equal to the scheduling delay plus task execution
            // timeout. For the latter, if the wrapped task is a PriorityTask use its
            // execution timeout; otherwise, use the DaemonPool default task timeout.
            return getDelayMillis() +  (task instanceof PriorityTask
                    ? ((PriorityTask) task).getExecutionTimeoutMillis()
                    : getDaemonPool().getTaskTimeout());
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public long getRequestTimeoutMillis()
            {
            return getExecutionTimeoutMillis();
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public int getSchedulingPriority()
            {
            // import com.tangosol.net.PriorityTask;
            
            Runnable task = getTask();
            return task instanceof PriorityTask
                    ? ((PriorityTask) task).getSchedulingPriority()
                    : PriorityTask.SCHEDULE_STANDARD;
            }
        
        // Accessor for the property "Task"
        /**
         * Getter for property Task.<p>
        * The task to add to the DaemonPool.
         */
        public Runnable getTask()
            {
            return __m_Task;
            }
        
        // From interface: java.lang.Runnable
        public void run()
            {
            Runnable task = getTask();
            if (task instanceof DaemonPool.ResizeTask)
                {
                // run the non-blocking ResizeTask on this thread
                task.run();
                }
            else
                {
                getDaemonPool().add(task);
                }
            }
        
        // From interface: com.tangosol.net.PriorityTask
        public void runCanceled(boolean fAbandoned)
            {
            // import com.tangosol.net.PriorityTask;
            
            Runnable task = getTask();
            if (task instanceof PriorityTask)
                {
                ((PriorityTask) task).runCanceled(fAbandoned);
                }
            }
        
        // Accessor for the property "DelayMillis"
        /**
         * Setter for property DelayMillis.<p>
        * The delay in milliseconds before the task will be added to the
        * DaemonPool.
         */
        public void setDelayMillis(long cMillis)
            {
            __m_DelayMillis = cMillis;
            }
        
        // Accessor for the property "Task"
        /**
         * Setter for property Task.<p>
        * The task to add to the DaemonPool.
         */
        public void setTask(Runnable task)
            {
            __m_Task = task;
            }
        
        // Declared at the super level
        public String toString()
            {
            return get_Name() + " for " + getDaemonPool();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.DaemonPool$StartTask
    
    /**
     * Runnable pseudo-task that is used to start one and only one daemon
     * thread.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class StartTask
            extends    com.tangosol.coherence.component.Util
            implements com.oracle.coherence.common.base.Associated,
                       Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Daemon
         *
         * The Daemon to start.
         */
        private DaemonPool.Daemon __m_Daemon;
        
        /**
         * Property Queue
         *
         * The queue that used to be associated with the WorkSlot that this
         * StartTask should activate and to which this task needs to be added
         * back in a case of a failure to close the slot.
         * 
         * @see DaemonPool#startDaemon
         */
        private com.oracle.coherence.common.util.AssociationPile __m_Queue;
        
        /**
         * Property StartCount
         *
         * The number of daemons to start.
         */
        private int __m_StartCount;
        
        /**
         * Property WorkSlotActivate
         *
         * The WorkSlot that this StartTask should activate when the new daemon
         * starts.
         * 
         * @see DaemonPool#startDaemon
         */
        private DaemonPool.WorkSlot __m_WorkSlotActivate;
        
        // Default constructor
        public StartTask()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public StartTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.DaemonPool.StartTask();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool$StartTask".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // From interface: com.oracle.coherence.common.base.Associated
        public Object getAssociatedKey()
            {
            // import com.oracle.coherence.common.util.AssociationPile;
            
            return getDaemon() == null ? null : AssociationPile.ASSOCIATION_ALL;
            }
        
        // Accessor for the property "Daemon"
        /**
         * Getter for property Daemon.<p>
        * The Daemon to start.
         */
        public DaemonPool.Daemon getDaemon()
            {
            return __m_Daemon;
            }
        
        // Accessor for the property "Queue"
        /**
         * Getter for property Queue.<p>
        * The queue that used to be associated with the WorkSlot that this
        * StartTask should activate and to which this task needs to be added
        * back in a case of a failure to close the slot.
        * 
        * @see DaemonPool#startDaemon
         */
        public com.oracle.coherence.common.util.AssociationPile getQueue()
            {
            return __m_Queue;
            }
        
        // Accessor for the property "StartCount"
        /**
         * Getter for property StartCount.<p>
        * The number of daemons to start.
         */
        public int getStartCount()
            {
            return __m_StartCount;
            }
        
        // Accessor for the property "WorkSlotActivate"
        /**
         * Getter for property WorkSlotActivate.<p>
        * The WorkSlot that this StartTask should activate when the new daemon
        * starts.
        * 
        * @see DaemonPool#startDaemon
         */
        public DaemonPool.WorkSlot getWorkSlotActivate()
            {
            return __m_WorkSlotActivate;
            }
        
        // From interface: java.lang.Runnable
        public void run()
            {
            // import com.tangosol.util.Gate;
            
            DaemonPool.Daemon daemon = getDaemon();
            DaemonPool pool   = (DaemonPool) get_Module();
            
            if (daemon == null)
                {
                // this is a schedule task; find a daemon to start
                pool.startDaemon(this);
                }
            else
                {
                // the DaemonPool.StartTask is ASSOCIATION_ALL element, which means that by the time
                // it's polled from the queue there are no other outstanding associations,
                // and we can safely activate and start the new daemon
            
                DaemonPool.WorkSlot slot = getWorkSlotActivate();
            
                // make the "active" state visible by crossing the gate
                Gate gate = slot.getGate();
                if (!gate.close(1L))
                    {
                    // failed to close the slot, add the task back to the queue
                    getQueue().add(pool.instantiateWrapperTask(this, false));
                    return;
                    }
            
                slot.setActive(true);
            
                gate.open();
            
                daemon.start();
            
                scheduleNext();
                }
            }
        
        /**
         * Schedule additional daemon start requests if necessary.
         */
        public void scheduleNext()
            {
            DaemonPool pool = (DaemonPool) get_Module();
            
            int cStart = getStartCount() - 1;
            if (cStart > 0)
                {
                // create a "schedule start" copy;
                // (we cannot reuse the current one since it has a different association)
                DaemonPool.StartTask task = (DaemonPool.StartTask) pool._newChild("StartTask");
                task.setStartCount(cStart);
            
                pool.add(task, false);
                }
            else
                {
                pool.setInTransition(false);
                }
            }
        
        // Accessor for the property "Daemon"
        /**
         * Setter for property Daemon.<p>
        * The Daemon to start.
         */
        public void setDaemon(DaemonPool.Daemon daemon)
            {
            __m_Daemon = daemon;
            }
        
        // Accessor for the property "Queue"
        /**
         * Setter for property Queue.<p>
        * The queue that used to be associated with the WorkSlot that this
        * StartTask should activate and to which this task needs to be added
        * back in a case of a failure to close the slot.
        * 
        * @see DaemonPool#startDaemon
         */
        public void setQueue(com.oracle.coherence.common.util.AssociationPile queue)
            {
            __m_Queue = queue;
            }
        
        // Accessor for the property "StartCount"
        /**
         * Setter for property StartCount.<p>
        * The number of daemons to start.
         */
        public void setStartCount(int nCount)
            {
            __m_StartCount = nCount;
            }
        
        // Accessor for the property "WorkSlotActivate"
        /**
         * Setter for property WorkSlotActivate.<p>
        * The WorkSlot that this StartTask should activate when the new daemon
        * starts.
        * 
        * @see DaemonPool#startDaemon
         */
        public void setWorkSlotActivate(DaemonPool.WorkSlot slot)
            {
            __m_WorkSlotActivate = slot;
            }
        
        // Declared at the super level
        public String toString()
            {
            return get_Name() + " for Daemon " + getDaemon();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.DaemonPool$StopTask
    
    /**
     * Runnable pseudo-task that is used to terminate one and only one daemon
     * thread.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class StopTask
            extends    com.tangosol.coherence.component.Util
            implements Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Queue
         *
         * The Queue that this StopTask should "merge" into another queue and
         * discard afterwards.
         * 
         * @see DaemonPool#stopDaemon
         */
        private com.oracle.coherence.common.util.AssociationPile __m_Queue;
        
        /**
         * Property StopCount
         *
         * The number of daemons to stop.
         */
        private int __m_StopCount;
        
        /**
         * Property WorkSlot
         *
         * The WorkSlot that initiated this StopTask. The daemon that happens
         * to run this task needs to notify the WorkSlot that it has been
         * terminated.
         * 
         * @see DaemonPool#stopDaemon
         */
        private DaemonPool.WorkSlot __m_WorkSlot;
        
        // Default constructor
        public StopTask()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public StopTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.DaemonPool.StopTask();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool$StopTask".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "Queue"
        /**
         * Getter for property Queue.<p>
        * The Queue that this StopTask should "merge" into another queue and
        * discard afterwards.
        * 
        * @see DaemonPool#stopDaemon
         */
        public com.oracle.coherence.common.util.AssociationPile getQueue()
            {
            return __m_Queue;
            }
        
        // Accessor for the property "StopCount"
        /**
         * Getter for property StopCount.<p>
        * The number of daemons to stop.
         */
        public int getStopCount()
            {
            return __m_StopCount;
            }
        
        // Accessor for the property "WorkSlot"
        /**
         * Getter for property WorkSlot.<p>
        * The WorkSlot that initiated this StopTask. The daemon that happens to
        * run this task needs to notify the WorkSlot that it has been
        * terminated.
        * 
        * @see DaemonPool#stopDaemon
         */
        public DaemonPool.WorkSlot getWorkSlot()
            {
            return __m_WorkSlot;
            }
        
        // From interface: java.lang.Runnable
        public void run()
            {
            // import com.oracle.coherence.common.util.AssociationPile;
            // import com.tangosol.run.component.EventDeathException;
            
            DaemonPool         pool  = (DaemonPool) get_Module();
            AssociationPile queue = getQueue();
            
            if (queue == null && getWorkSlot() == null)
                {
                // this is a schedule task; find a daemon to stop
                pool.stopDaemon(this);
                }
            else
                {
                // this is an actual stop task
                if (queue != null)
                    {
                    // attempt to close all the slots corresponding to the queue
                    for (int i = 0, c = pool.getWorkSlotCount(); i < c; i++)
                        {
                        DaemonPool.WorkSlot slot = pool.getWorkSlot(i);
            
                        if (slot.getQueue() == queue)
                            {
                            if (!slot.getGate().close(1L))
                                {
                                // we failed to close one of the slots;
                                // re-open all we just closed and re-schedule
                                for (int j = 0; j < i; j++)
                                    {
                                    DaemonPool.WorkSlot slotTest = pool.getWorkSlot(j);
            
                                    if (slotTest.getQueue() == queue)
                                        {
                                        slotTest.getGate().open();
                                        }
                                    }
            
                                slot.add(pool.instantiateWrapperTask(this, false));
                                return;
                                }
                            }
                        }
                    // all the corresponding slots are successfully closed;
                    // proceed with the thread stop
                    }
            
                throw new EventDeathException(get_Name());
                }
            }
        
        /**
         * Schedule additional daemon start requests if necessary.
         */
        public void scheduleNext()
            {
            DaemonPool pool = (DaemonPool) get_Module();
            
            int cStop = getStopCount() - 1;
            if (cStop > 0)
                {
                // schedule the next stop task
                // (we can reuse this one since it has no association)
                setWorkSlot(null);
                setQueue(null);
                setStopCount(cStop);
            
                pool.add(this, false);
                }
            else
                {
                pool.setInTransition(false);
                }
            }
        
        // Accessor for the property "Queue"
        /**
         * Setter for property Queue.<p>
        * The Queue that this StopTask should "merge" into another queue and
        * discard afterwards.
        * 
        * @see DaemonPool#stopDaemon
         */
        public void setQueue(com.oracle.coherence.common.util.AssociationPile queue)
            {
            __m_Queue = queue;
            }
        
        // Accessor for the property "StopCount"
        /**
         * Setter for property StopCount.<p>
        * The number of daemons to stop.
         */
        public void setStopCount(int nCount)
            {
            __m_StopCount = nCount;
            }
        
        // Accessor for the property "WorkSlot"
        /**
         * Setter for property WorkSlot.<p>
        * The WorkSlot that initiated this StopTask. The daemon that happens to
        * run this task needs to notify the WorkSlot that it has been
        * terminated.
        * 
        * @see DaemonPool#stopDaemon
         */
        public void setWorkSlot(DaemonPool.WorkSlot slot)
            {
            __m_WorkSlot = slot;
            }
        
        // Declared at the super level
        public String toString()
            {
            return get_Name() + " for QueueId=" + System.identityHashCode(getQueue()) + "; Count=" + getStopCount();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.DaemonPool$WorkSlot
    
    /**
     * To reduce the contention across the worker threads, all tasks added to
     * the DaemonPool are directed to one of the WorkSlots in a way that
     * respects the association between tasks. The total number of slots is
     * fixed and calculated based on the number of processors. Depending on the
     * number of daemon threads, different slots may share the queues.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class WorkSlot
            extends    com.tangosol.coherence.component.Util
        {
        // ---- Fields declarations ----
        
        /**
         * Property Active
         *
         * Indicates whether or not the slot is being served by an associated
         * daemon. If the slot is not active, tasks still can be added to, but
         * should not be removed from its queue.
         */
        private boolean __m_Active;
        
        /**
         * Property Gate
         *
         * The ThreadGate associated with this WorkSlot.
         */
        private com.tangosol.util.Gate __m_Gate;
        
        /**
         * Property Index
         *
         * This WorkSlot's index in the WorkSlot array.
         */
        private int __m_Index;
        
        /**
         * Property Queue
         *
         * The Queue associated with this slot.
         */
        private com.oracle.coherence.common.util.AssociationPile __m_Queue;
        
        // Default constructor
        public WorkSlot()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public WorkSlot(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setIndex(-1);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.DaemonPool.WorkSlot();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool$WorkSlot".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        /**
         * Add the task for the corresponding queue.
         */
        public void add(DaemonPool.WrapperTask task)
            {
            // import com.tangosol.util.Gate;
            
            Gate gate = getGate();
            gate.enter(-1L);
            try
                {
                getQueue().add(task);
                }
            finally
                {
                gate.exit();
                }
            }
        
        // Accessor for the property "Gate"
        /**
         * Getter for property Gate.<p>
        * The ThreadGate associated with this WorkSlot.
         */
        public com.tangosol.util.Gate getGate()
            {
            return __m_Gate;
            }
        
        // Accessor for the property "Index"
        /**
         * Getter for property Index.<p>
        * This WorkSlot's index in the WorkSlot array.
         */
        public int getIndex()
            {
            return __m_Index;
            }
        
        // Accessor for the property "Queue"
        /**
         * Getter for property Queue.<p>
        * The Queue associated with this slot.
         */
        public com.oracle.coherence.common.util.AssociationPile getQueue()
            {
            return __m_Queue;
            }
        
        // Accessor for the property "Active"
        /**
         * Getter for property Active.<p>
        * Indicates whether or not the slot is being served by an associated
        * daemon. If the slot is not active, tasks still can be added to, but
        * should not be removed from its queue.
         */
        public boolean isActive()
            {
            return __m_Active;
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // import com.tangosol.util.ThreadGateLite;
            
            setGate(new ThreadGateLite());
            
            super.onInit();
            }
        
        // Accessor for the property "Active"
        /**
         * Setter for property Active.<p>
        * Indicates whether or not the slot is being served by an associated
        * daemon. If the slot is not active, tasks still can be added to, but
        * should not be removed from its queue.
         */
        public void setActive(boolean fActive)
            {
            __m_Active = fActive;
            }
        
        // Accessor for the property "Gate"
        /**
         * Setter for property Gate.<p>
        * The ThreadGate associated with this WorkSlot.
         */
        protected void setGate(com.tangosol.util.Gate gate)
            {
            __m_Gate = gate;
            }
        
        // Accessor for the property "Index"
        /**
         * Setter for property Index.<p>
        * This WorkSlot's index in the WorkSlot array.
         */
        public void setIndex(int nIndex)
            {
            _assert(!is_Constructed() || (getIndex() == -1 && nIndex >= 0), "Must not change");
            
            __m_Index = (nIndex);
            }
        
        // Accessor for the property "Queue"
        /**
         * Setter for property Queue.<p>
        * The Queue associated with this slot.
         */
        public void setQueue(com.oracle.coherence.common.util.AssociationPile queue)
            {
            __m_Queue = queue;
            }
        
        // Declared at the super level
        public String toString()
            {
            return get_Name() + "[" + getIndex() +
                "] QueueId=" + System.identityHashCode(getQueue()) +
                ", PendingJobs=" + getQueue().size();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.DaemonPool$WrapperTask
    
    /**
     * A task that is used to wrap the actual tasks.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class WrapperTask
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.Guardable,
                       com.tangosol.net.cache.KeyAssociation,
                       Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Gate
         *
         * An optional Gate that must be exited when this task completes.
         */
        private com.tangosol.util.Gate __m_Gate;
        
        /**
         * Property GuardContext
         *
         * The GuardContext for this task.
         */
        private transient com.tangosol.net.Guardian.GuardContext __m_GuardContext;
        
        /**
         * Property ManagementTask
         *
         * Determine if the wrapped task is a management task.
         */
        private boolean __m_ManagementTask;
        
        /**
         * Property PostTime
         *
         * The date/time (in millis) at which the underlying task was posted
         * (queued) for the execution.
         */
        private transient long __m_PostTime;
        
        /**
         * Property Priority
         *
         * The priority of the underlying task.
         */
        private transient int __m_Priority;
        
        /**
         * Property StartTime
         *
         * The date/time (in millis) at which the underlying task started the
         * execution or was canceled.
         */
        private transient long __m_StartTime;
        
        /**
         * Property StopTime
         *
         * The date/time (positive value in millis) by which the underlying
         * task is supposed to finish. Otherwise, it should be canceled or
         * interrupted.
         * 
         * Note: the StopTime will only be specified (positive) if the
         * underlying task is a PriorityTask with a specified timeout value.
         */
        private transient long __m_StopTime;
        
        /**
         * Property Task
         *
         * The underlying task.
         */
        private transient Runnable __m_Task;
        
        /**
         * Property TimeoutMillis
         *
         * The timeout  interval in millis.
         */
        private transient long __m_TimeoutMillis;
        
        // Default constructor
        public WrapperTask()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public WrapperTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.DaemonPool.WrapperTask();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/DaemonPool$WrapperTask".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        /**
         * Cancel the underlying task execution. This method is called ONLY if
        * the underlying task is a PriorityTask.
        * 
        * @param ldtTime the timestamp of the cancellation
         */
        public void cancel(long ldtTime)
            {
            // import com.tangosol.net.Guardian$GuardContext as com.tangosol.net.Guardian.GuardContext;
            // import com.tangosol.net.PriorityTask;
            
            synchronized (this)
                {
                if (getStartTime() > 0L)
                    {
                    // soft assert
                    _trace("Cannot cancel already started: " + this, 3);
                    return;
                    }
                setStartTime(ldtTime);
                setStopTime(0L);
            
                com.tangosol.net.Guardian.GuardContext context = getGuardContext();
                if (context != null)
                    {
                    context.release();
                    }
                }
            
            ((DaemonPool) get_Module()).
                runCanceled((PriorityTask) getTask(), false);
            }
        
        // From interface: com.tangosol.net.cache.KeyAssociation
        public Object getAssociatedKey()
            {
            // import com.oracle.coherence.common.base.Associated;
            
            Runnable task = getTask();
            return task instanceof Associated ?
                ((Associated) task).getAssociatedKey() : null;
            }
        
        // From interface: com.tangosol.net.Guardable
        public com.tangosol.net.Guardian.GuardContext getContext()
            {
            return getGuardContext();
            }
        
        // Accessor for the property "Gate"
        /**
         * Getter for property Gate.<p>
        * An optional Gate that must be exited when this task completes.
         */
        public com.tangosol.util.Gate getGate()
            {
            return __m_Gate;
            }
        
        // Accessor for the property "GuardContext"
        /**
         * Getter for property GuardContext.<p>
        * The GuardContext for this task.
         */
        public com.tangosol.net.Guardian.GuardContext getGuardContext()
            {
            return __m_GuardContext;
            }
        
        // Accessor for the property "PostTime"
        /**
         * Getter for property PostTime.<p>
        * The date/time (in millis) at which the underlying task was posted
        * (queued) for the execution.
         */
        public long getPostTime()
            {
            return __m_PostTime;
            }
        
        // Accessor for the property "Priority"
        /**
         * Getter for property Priority.<p>
        * The priority of the underlying task.
         */
        public int getPriority()
            {
            return __m_Priority;
            }
        
        // Accessor for the property "StartTime"
        /**
         * Getter for property StartTime.<p>
        * The date/time (in millis) at which the underlying task started the
        * execution or was canceled.
         */
        public long getStartTime()
            {
            return __m_StartTime;
            }
        
        // Accessor for the property "StopTime"
        /**
         * Getter for property StopTime.<p>
        * The date/time (positive value in millis) by which the underlying task
        * is supposed to finish. Otherwise, it should be canceled or
        * interrupted.
        * 
        * Note: the StopTime will only be specified (positive) if the
        * underlying task is a PriorityTask with a specified timeout value.
         */
        public long getStopTime()
            {
            return __m_StopTime;
            }
        
        // Accessor for the property "Task"
        /**
         * Getter for property Task.<p>
        * The underlying task.
         */
        public Runnable getTask()
            {
            return __m_Task;
            }
        
        // Accessor for the property "TaskId"
        /**
         * Getter for property TaskId.<p>
        * The id of the underlying task. If the task does not implement
        * javax.util.concurrent.Identifiable (requires JDK 1.5), return
        * toString().
         */
        public String getTaskId()
            {
            Runnable task = getTask();
            try
                {
                return String.valueOf(task);
                }
            catch (RuntimeException e)
                {
                return task.getClass().getName() + '@' + task.hashCode();
                }
            }
        
        // Accessor for the property "TimeoutMillis"
        /**
         * Getter for property TimeoutMillis.<p>
        * The timeout  interval in millis.
         */
        public long getTimeoutMillis()
            {
            return __m_TimeoutMillis;
            }
        
        // Accessor for the property "ManagementTask"
        /**
         * Getter for property ManagementTask.<p>
        * Determine if the wrapped task is a management task.
         */
        public boolean isManagementTask()
            {
            return __m_ManagementTask;
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // no-op: no children
            }
        
        // From interface: com.tangosol.net.Guardable
        public void recover()
            {
            }
        
        // From interface: java.lang.Runnable
        public void run()
            {
            // import com.tangosol.net.Guardian$GuardContext as com.tangosol.net.Guardian.GuardContext;
            // import com.tangosol.util.Base;
            
            synchronized (this)
                {
                com.tangosol.net.Guardian.GuardContext context = getGuardContext();
                if (context != null)
                    {
                    context.release();
                    setGuardContext(null);
                    }
            
                if (getStartTime() > 0L)
                    {
                    // the task has already been handled (canceled)
                    return;
                    }
                long ldtNow = Base.getSafeTimeMillis();
                setStartTime(ldtNow);
            
                if (getStopTime() == 0L)
                    {
                    long cTimeout = getTimeoutMillis();
                    if (cTimeout > 0L)
                        {
                        // non-aggressive timeout
                        setStopTime(ldtNow + cTimeout);
                        }
                    }
                }
            
            run(getTask());
            }
        
        /**
         * Run the specified task.
        * 
        * @param task  the task to run
         */
        protected void run(Runnable task)
            {
            task.run();
            }
        
        // From interface: com.tangosol.net.Guardable
        public void setContext(com.tangosol.net.Guardian.GuardContext context)
            {
            setGuardContext(context);
            }
        
        // Accessor for the property "Gate"
        /**
         * Setter for property Gate.<p>
        * An optional Gate that must be exited when this task completes.
         */
        public void setGate(com.tangosol.util.Gate gate)
            {
            __m_Gate = gate;
            }
        
        // Accessor for the property "GuardContext"
        /**
         * Setter for property GuardContext.<p>
        * The GuardContext for this task.
         */
        public void setGuardContext(com.tangosol.net.Guardian.GuardContext task)
            {
            __m_GuardContext = task;
            }
        
        // Accessor for the property "ManagementTask"
        /**
         * Setter for property ManagementTask.<p>
        * Determine if the wrapped task is a management task.
         */
        protected void setManagementTask(boolean fMgmt)
            {
            __m_ManagementTask = fMgmt;
            }
        
        // Accessor for the property "PostTime"
        /**
         * Setter for property PostTime.<p>
        * The date/time (in millis) at which the underlying task was posted
        * (queued) for the execution.
         */
        public void setPostTime(long ldt)
            {
            __m_PostTime = ldt;
            }
        
        // Accessor for the property "Priority"
        /**
         * Setter for property Priority.<p>
        * The priority of the underlying task.
         */
        public void setPriority(int iPriority)
            {
            __m_Priority = iPriority;
            }
        
        // Accessor for the property "StartTime"
        /**
         * Setter for property StartTime.<p>
        * The date/time (in millis) at which the underlying task started the
        * execution or was canceled.
         */
        protected void setStartTime(long ldt)
            {
            __m_StartTime = ldt;
            }
        
        // Accessor for the property "StopTime"
        /**
         * Setter for property StopTime.<p>
        * The date/time (positive value in millis) by which the underlying task
        * is supposed to finish. Otherwise, it should be canceled or
        * interrupted.
        * 
        * Note: the StopTime will only be specified (positive) if the
        * underlying task is a PriorityTask with a specified timeout value.
         */
        public void setStopTime(long ldt)
            {
            __m_StopTime = ldt;
            }
        
        // Accessor for the property "Task"
        /**
         * Setter for property Task.<p>
        * The underlying task.
         */
        public void setTask(Runnable task)
            {
            setManagementTask(task instanceof DaemonPool.StartTask || task instanceof DaemonPool.StopTask);
            __m_Task = (task);
            }
        
        // Accessor for the property "TimeoutMillis"
        /**
         * Setter for property TimeoutMillis.<p>
        * The timeout  interval in millis.
         */
        public void setTimeoutMillis(long cMillis)
            {
            __m_TimeoutMillis = cMillis;
            }
        
        // From interface: com.tangosol.net.Guardable
        public void terminate()
            {
            // import com.tangosol.util.Base;
            
            DaemonPool pool = (DaemonPool) get_Module();
            
            // task hasn't queued yet, but is already timed out; cancel it
            synchronized (pool.STATS_MONITOR)
                {
                pool.setStatsTimeoutCount(pool.getStatsTimeoutCount() + 1);
                }
            cancel(Base.getSafeTimeMillis());
            }
        
        // Declared at the super level
        public String toString()
            {
            // import com.tangosol.net.PriorityTask;
            // import com.tangosol.util.Base;
            
            // Output templates:
            //
            // Task(MyTask..., SCHEDULE_STANDARD, Queued for #ms, No timeout)
            // Task(MyTask..., SCHEDULE_IMMEDIATE, Running for #ms, Timeout in #ms)
            // Task(MyTask..., SCHEDULE_FIRST, Running for #ms, Stopping for #ms)
            
            StringBuilder sb = new StringBuilder();
            sb.append("Task(")
              .append(getTaskId());
            
            switch (getPriority())
                {
                case PriorityTask.SCHEDULE_STANDARD:
                    sb.append(", SCHEDULE_STANDARD");
                    break;
            
                case PriorityTask.SCHEDULE_FIRST:
                    sb.append(", SCHEDULE_FIRST");
                    break;
            
                case PriorityTask.SCHEDULE_IMMEDIATE:
                    sb.append(", SCHEDULE_IMMEDIATE");
                    break;
                }
            
            long ldtStart = getStartTime();
            long ldtStop  = getStopTime();
            long ldtPost  = getPostTime();
            long ldtNow   = Base.getSafeTimeMillis();
            
            if (ldtStart > 0L)
                {
                sb.append(", Running for ")
                  .append(ldtNow - ldtStart)
                  .append("ms");
                }
            else if (ldtPost > 0L)
                {
                sb.append(", Queued for ")
                  .append(ldtNow - ldtPost)
                  .append("ms");
                }
            
            if (ldtStop > 0L)
                {
                long cTimeout = ldtStop - ldtNow;
                if (cTimeout >= 0)
                    {
                    sb.append(", Timeout in ")
                      .append(cTimeout)
                      .append("ms");
                    }
                else
                    {
                    sb.append(", Stopping for ")
                      .append(-cTimeout)
                      .append("ms");
                    }
                }
            else if (getTimeoutMillis() > 0L)
                {
                sb.append(", Not started");
                }
            else
                {
                sb.append(", No timeout");
                }
            return sb.toString();
            }
        }
    }

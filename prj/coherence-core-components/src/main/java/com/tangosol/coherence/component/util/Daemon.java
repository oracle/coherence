
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.Daemon

package com.tangosol.coherence.component.util;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.NonBlocking;
import com.tangosol.coherence.config.Config;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardable;
import com.tangosol.run.component.EventDeathException;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.UUID;
import com.tangosol.util.WrapperException;

/**
 * This Component is used to create and manage a daemon thread.<br>
 * <br>
 * A caller may use the following methods to control the Daemon component:<br>
 * <br><ol>
 * <li><tt>start</tt> - creates and starts the daemon thread</li>
 * <li><tt>isStarted</tt> - determines whether the daemon is running</li>
 * <li><tt>stop</tt> - stops the daemon thread and releases the related
 * resources</li>
 * </ol><br>
 * Advanced options available to a designer or caller include:<br>
 * <br><ol>
 * <li><tt>ThreadGroup</tt> - before starting the daemon, a ThreadGroup object
 * can be provided that the daemon Thread will belong to</li>
 * <li><tt>Priority</tt> - before starting the daemon, a Thread priority can be
 * provided</li>
 * <li><tt>ThreadName</tt> - before starting the daemon, a Thread name can be
 * provided</li>
 * <li><tt>Thread</tt> - the actual Thread object can be accessed via this
 * property</li>
 * <li><tt>StartException</tt> - if the start method fails to start the daemon,
 * the StartException property provides the failure information</li>
 * </ol><br>
 * The daemon thread itself executes the following events while it is
 * running:<br>
 * <br><ol>
 * <li><tt>onEnter</tt> - invoked when the daemon starts</li>
 * <li><tt>onWait</tt> - invoked to wait for notification</li>
 * <li><tt>onNotify</tt> - invoked when a notification occurs</li>
 * <li><tt>onInterrupt</tt> - invoked when the thread is interrupted when
 * waiting for a notification</li>
 * <li><tt>onException</tt> - invoked when an exception occurs while invoking
 * one of the daemon events</li>
 * <li><tt>onExit</tt> - invoked before the daemon exits</li>
 * </ol><br>
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Daemon
        extends    com.tangosol.coherence.component.Util
        implements com.tangosol.net.Guardian,
                   Runnable
    {
    // ---- Fields declarations ----
    
    /**
     * Property AutoStart
     *
     * Specifies whether this Daemon component should start automatically at
     * the initialization time.
     * 
     * @see #onInit
     */
    private boolean __m_AutoStart;
    
    /**
     * Property ClockResolutionMillis
     *
     * The resolution of the system clock in milliseconds.  The value is
     * determined based on the detected OS, see _initStatic().
     * 
     * This may be manually specified via the
     * tangosol.coherence.clock_resolution system property.
     */
    private static transient long __s_ClockResolutionMillis;
    
    /**
     * Property DAEMON_EXITED
     *
     * State indicating that the daemon has exited.
     */
    public static final int DAEMON_EXITED = 4;
    
    /**
     * Property DAEMON_EXITING
     *
     * State indicating that the daemon is currently exiting.
     */
    public static final int DAEMON_EXITING = 3;
    
    /**
     * Property DAEMON_INITIAL
     *
     * State indicating that the daemon has yet to be started.
     */
    public static final int DAEMON_INITIAL = 0;
    
    /**
     * Property DAEMON_RUNNING
     *
     * State indicating that the daemon is ready for operation.
     */
    public static final int DAEMON_RUNNING = 2;
    
    /**
     * Property DAEMON_STARTING
     *
     * State indicating that the daemon is currently starting.
     */
    public static final int DAEMON_STARTING = 1;
    
    /**
     * Property DaemonState
     *
     * Specifies the state of the daemon (INITIAL, STARTING, RUNNING, EXITING,
     * EXITED).  The state is not allowed to regress, thus the ordering of the
     * daemon state values is meaningful.
     * 
     * @see #setDaemonState
     * @volatile - see stop
     * 
     * Since Coherence 3.2
     */
    private volatile transient int __m_DaemonState;
    
    /**
     * Property DefaultGuardRecovery
     *
     * Default recovery percentage for guardables manged by this Daemon.
     */
    private transient float __m_DefaultGuardRecovery;
    
    /**
     * Property DefaultGuardTimeout
     *
     * Default timeout interval for guardables manged by this Daemon.
     */
    private transient long __m_DefaultGuardTimeout;
    
    /**
     * Property Exiting
     *
     * Set to true when the daemon is instructed to stop.
     */
    private transient boolean __m_Exiting;
    
    /**
     * Property ExitMonitor
     *
     * Monitor object to coordinate clearing the thread interrupt set by stop
     * prior to running onExit().
     */
    private Object __m_ExitMonitor;
    
    /**
     * Property Guardable
     *
     * A Guardable object reresenting this Daemon. By default, all Daemon
     * components initialize the Guardable property during "init" with their
     * $Guard child component. However, some derived implementation (e.g.
     * PacketPublisher) "remove" the Guard child, leaving this property as null.
     */
    private Daemon.Guard __m_Guardable;
    
    /**
     * Property GuardSupport
     *
     * The GuardSupport used by this Daemon Guardian to manage its Guardable
     * responsibilities.
     */
    private com.tangosol.net.GuardSupport __m_GuardSupport;
    
    /**
     * Property IntervalNextMillis
     *
     * The timestamp at which the next call to onInterval should be made.
     * 
     * @see #onInterval
     */
    private transient long __m_IntervalNextMillis;
    
    /**
     * Property Notifier
     *
     * The Notifier serves an a means to wakeup an idle Daemon.
     * 
     * 
     * @see #onNotify
     * @see #onWait
     */
    private transient com.oracle.coherence.common.base.Notifier __m_Notifier;
    
    /**
     * Property Priority
     *
     * A non-zero value specifies the priority of the daemon's thread. A zero
     * value implies the Thread default priority.
     * 
     * Priority must be set before the Daemon is started (by the start method)
     * in order to have effect.
     */
    private int __m_Priority;
    
    /**
     * Property StartException
     *
     * The exception (if any) that prevented the daemon from starting
     * successfully.
     */
    private transient Throwable __m_StartException;
    
    /**
     * Property StartTimestamp
     *
     * Date/time value that this Daemon's thread has started.
     */
    private transient long __m_StartTimestamp;
    
    /**
     * Property Thread
     *
     * The daemon thread if it is running, or null before the daemon starts and
     * after the daemon stops.
     */
    private transient Thread __m_Thread;
    
    /**
     * Property ThreadGroup
     *
     * Specifies the ThreadGroup within which the daemon thread will be
     * created. If not specified, the current Thread's ThreadGroup will be
     * used.
     * 
     * This property can only be set at runtime, and must be configured before
     * start() is invoked to cause the daemon thread to be created within the
     * specified ThreadGroup.
     * 
     * @see java.lang.Thread
     * @see java.lang.ThreadGroup
     */
    private transient ThreadGroup __m_ThreadGroup;
    
    /**
     * Property ThreadName
     *
     * Specifies the name of the daemon thread. If not specified, the component
     * name will be used.
     * 
     * This property can be set at design time or runtime. If set at runtime,
     * this property must be configured before start() is invoked to cause the
     * daemon thread to have the specified name.
     */
    private String __m_ThreadName;
    
    /**
     * Property WaitMillis
     *
     * The number of milliseconds that the daemon will wait for notification.
     * Zero means to wait indefinitely. Negative value means to skip waiting
     * altogether.
     * 
     * @see #onWait
     */
    private long __m_WaitMillis;
    
    private static void _initStatic$Default()
        {
        __initStatic();
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import com.tangosol.coherence.config.Config;
        
        _initStatic$Default();
        
        // determine the clock resolution
        try
            {
            String sMillis = Config.getProperty("coherence.clock_resolution");
            if (sMillis == null)
                {
                // determine based on OS
                String sOS = System.getProperty("os.name", "");
                if (sOS.contains("Windows"))
                    {
                    if (Runtime.getRuntime().availableProcessors() > 1)
                        {
                        // Reported to be 15.625ms for multi CPU
                        // my tests show, wait is 15, sleep is 1-15
                        // wait != sleep
                        // wait(1)  .. wait(15) == 15ms
                        // wait(16) .. wait(30) == 31ms
                        // wait(32) .. wait(46) == 46ms
                        // sleep(1) 3-4ms
                        // sleep(2) 4-6ms
                        // sleep(3) 6ms
                        // sleep(4) 6-7ms
                        // sleep(5) 7ms
                        // sleep(6) 6-9ms
                        // sleep(7) 7-9ms
                        // sleep(8) 7-9ms
                        // sleep(9) 9-11ms
                        // sleep(10) 15ms
                        // sleep(11) 10-12ms
        
                        setClockResolutionMillis(16);
                        }
                    else
                        {
                        // Reported to be 10ms for single CPU
                        // time taken is actually +2-3ms but
                        // due to resolution will look like 10ms
                        // wait == sleep
                        setClockResolutionMillis(10);
                        }
                    }
                else
                    {
                    // Reported to be Linux, Solaris, OS X are all 1ms
        
                    // RedHat (2.4 Kernel) round up to neares 10, then add 9,10
                    // sleep == wait
                    // sleep(1)  .. sleep(10) == 20ms
                    // sleep(11) .. sleep(20) == 30ms
                    // sleep(21) .. sleep(30) == 39ms
                                
                    // Suse (2.6 Kernel) +1ms
                    // sleep == wait
        
                    // RedHat (2.6 Kernel) +4-7ms
                    // sleep == wait
                    // sleep(1) .. sleep(4) == 8ms
                    // sleep(5) .. sleep(8) == 12ms
                    // sleep(9) .. sleep(12) == 16ms
        
                    // as things are all over the place we go with a high value
                    setClockResolutionMillis(20);
                    }
                }
            else
                {
                // use user specified value
                setClockResolutionMillis(Integer.parseInt(sMillis));
                }
            }
        catch (Exception ignored) {}
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // state initialization: static properties
        try
            {
            setClockResolutionMillis(1L);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
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
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new Daemon.Guard("Guard", this, true), "Guard");
        
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
            __m_ExitMonitor = new java.lang.Object();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // Getter for virtual constant NonBlocking
    public boolean isNonBlocking()
        {
        return false;
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.Daemon();
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
            clz = Class.forName("com.tangosol.coherence/component/util/Daemon".replace('/', '.'));
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
    
    /**
     * Check the guardables that are guarded by this Daemon.
     */
    protected void checkGuardables()
        {
        if (isGuardian())
            {
            getGuardSupport().check();
            }
        }
    
    /**
     * Return the GuardSupport for this Daemon, creating and initializing it if
    * necessary.
    * 
    * @return the GuardSupport for this Daemon
     */
    public com.tangosol.net.GuardSupport ensureGuardSupport()
        {
        // import com.tangosol.net.GuardSupport;
        
        GuardSupport support = getGuardSupport();
        if (support == null)
            {
            synchronized (this)
                {
                support = getGuardSupport();
                if (support == null)
                    {
                    support = new GuardSupport(this);
                    setGuardSupport(support);
                    }
                }
            }
        return support;
        }
    
    // Declared at the super level
    protected void finalize()
            throws java.lang.Throwable
        {
        try
            {
            stop();
            }
        catch (Throwable ignored)
            {
            }
        
        super.finalize();
        }
    
    // Accessor for the property "ClockResolutionMillis"
    /**
     * Getter for property ClockResolutionMillis.<p>
    * The resolution of the system clock in milliseconds.  The value is
    * determined based on the detected OS, see _initStatic().
    * 
    * This may be manually specified via the
    * tangosol.coherence.clock_resolution system property.
     */
    public static long getClockResolutionMillis()
        {
        return __s_ClockResolutionMillis;
        }
    
    // Accessor for the property "DaemonState"
    /**
     * Getter for property DaemonState.<p>
    * Specifies the state of the daemon (INITIAL, STARTING, RUNNING, EXITING,
    * EXITED).  The state is not allowed to regress, thus the ordering of the
    * daemon state values is meaningful.
    * 
    * @see #setDaemonState
    * @volatile - see stop
    * 
    * Since Coherence 3.2
     */
    public int getDaemonState()
        {
        return __m_DaemonState;
        }
    
    // From interface: com.tangosol.net.Guardian
    // Accessor for the property "DefaultGuardRecovery"
    /**
     * Getter for property DefaultGuardRecovery.<p>
    * Default recovery percentage for guardables manged by this Daemon.
     */
    public float getDefaultGuardRecovery()
        {
        return __m_DefaultGuardRecovery;
        }
    
    // From interface: com.tangosol.net.Guardian
    // Accessor for the property "DefaultGuardTimeout"
    /**
     * Getter for property DefaultGuardTimeout.<p>
    * Default timeout interval for guardables manged by this Daemon.
     */
    public long getDefaultGuardTimeout()
        {
        return __m_DefaultGuardTimeout;
        }
    
    // Accessor for the property "ExitMonitor"
    /**
     * Getter for property ExitMonitor.<p>
    * Monitor object to coordinate clearing the thread interrupt set by stop
    * prior to running onExit().
     */
    protected Object getExitMonitor()
        {
        return __m_ExitMonitor;
        }
    
    // Accessor for the property "Guardable"
    /**
     * Getter for property Guardable.<p>
    * A Guardable object reresenting this Daemon. By default, all Daemon
    * components initialize the Guardable property during "init" with their
    * $Guard child component. However, some derived implementation (e.g.
    * PacketPublisher) "remove" the Guard child, leaving this property as null.
     */
    public Daemon.Guard getGuardable()
        {
        return __m_Guardable;
        }
    
    // Accessor for the property "GuardSupport"
    /**
     * Getter for property GuardSupport.<p>
    * The GuardSupport used by this Daemon Guardian to manage its Guardable
    * responsibilities.
     */
    public com.tangosol.net.GuardSupport getGuardSupport()
        {
        return __m_GuardSupport;
        }
    
    // Accessor for the property "IntervalNextMillis"
    /**
     * Getter for property IntervalNextMillis.<p>
    * The timestamp at which the next call to onInterval should be made.
    * 
    * @see #onInterval
     */
    public long getIntervalNextMillis()
        {
        return __m_IntervalNextMillis;
        }
    
    // Accessor for the property "Notifier"
    /**
     * Getter for property Notifier.<p>
    * The Notifier serves an a means to wakeup an idle Daemon.
    * 
    * 
    * @see #onNotify
    * @see #onWait
     */
    public com.oracle.coherence.common.base.Notifier getNotifier()
        {
        return __m_Notifier;
        }
    
    // Accessor for the property "Priority"
    /**
     * Getter for property Priority.<p>
    * A non-zero value specifies the priority of the daemon's thread. A zero
    * value implies the Thread default priority.
    * 
    * Priority must be set before the Daemon is started (by the start method)
    * in order to have effect.
     */
    public int getPriority()
        {
        return __m_Priority;
        }
    
    // Accessor for the property "StartException"
    /**
     * Getter for property StartException.<p>
    * The exception (if any) that prevented the daemon from starting
    * successfully.
     */
    public Throwable getStartException()
        {
        return __m_StartException;
        }
    
    // Accessor for the property "StartTimestamp"
    /**
     * Getter for property StartTimestamp.<p>
    * Date/time value that this Daemon's thread has started.
     */
    public long getStartTimestamp()
        {
        return __m_StartTimestamp;
        }
    
    // Accessor for the property "Thread"
    /**
     * Getter for property Thread.<p>
    * The daemon thread if it is running, or null before the daemon starts and
    * after the daemon stops.
     */
    public Thread getThread()
        {
        return __m_Thread;
        }
    
    // Accessor for the property "ThreadGroup"
    /**
     * Getter for property ThreadGroup.<p>
    * Specifies the ThreadGroup within which the daemon thread will be created.
    * If not specified, the current Thread's ThreadGroup will be used.
    * 
    * This property can only be set at runtime, and must be configured before
    * start() is invoked to cause the daemon thread to be created within the
    * specified ThreadGroup.
    * 
    * @see java.lang.Thread
    * @see java.lang.ThreadGroup
     */
    public ThreadGroup getThreadGroup()
        {
        return __m_ThreadGroup;
        }
    
    // Accessor for the property "ThreadName"
    /**
     * Getter for property ThreadName.<p>
    * Specifies the name of the daemon thread. If not specified, the component
    * name will be used.
    * 
    * This property can be set at design time or runtime. If set at runtime,
    * this property must be configured before start() is invoked to cause the
    * daemon thread to have the specified name.
     */
    public String getThreadName()
        {
        String sName = __m_ThreadName;
        return sName == null ? get_Name() : sName;
        }
    
    // Accessor for the property "WaitMillis"
    /**
     * Getter for property WaitMillis.<p>
    * The number of milliseconds that the daemon will wait for notification.
    * Zero means to wait indefinitely. Negative value means to skip waiting
    * altogether.
    * 
    * @see #onWait
     */
    public long getWaitMillis()
        {
        return __m_WaitMillis;
        }
    
    // From interface: com.tangosol.net.Guardian
    public com.tangosol.net.Guardian.GuardContext guard(com.tangosol.net.Guardable guardable)
        {
        return guard(guardable, getDefaultGuardTimeout(), getDefaultGuardRecovery());
        }
    
    // From interface: com.tangosol.net.Guardian
    public com.tangosol.net.Guardian.GuardContext guard(com.tangosol.net.Guardable guardable, long cMillis, float flPctRecover)
        {
        // import com.tangosol.net.GuardSupport;
        
        GuardSupport support = ensureGuardSupport();
        
        support.remove(guardable);
        return cMillis == 0L ? null : support.add(guardable, cMillis, flPctRecover);
        }
    
    /**
     * Halt the daemon.  Brings down the daemon in an ungraceful manner.
    * This method should not synchronize or block in any way.
    * This method may not return.
     */
    protected void halt()
        {
        // import com.tangosol.util.ClassHelper;
        
        setDaemonState(DAEMON_EXITING);
        
        Thread thread = getThread();
        if (thread != null)
            {
            try
                {
                thread.interrupt();
                }
            catch (Throwable ignored)
                {
                }
            }
        setThread(null);
        }
    
    /**
     * Issue heartbeat.
     */
    protected void heartbeat()
        {
        heartbeat(0);
        }
    
    /**
     * Issue heartbeat.  See com.tangosol.net.Guardian$GuardContext.
    * 
    * @param cMillis  the duration of heartbeat to issue, or 0 for the default
    * heartbeat
     */
    protected void heartbeat(long cMillis)
        {
        // import com.tangosol.net.Guardian$GuardContext as com.tangosol.net.Guardian.GuardContext;
        // import com.tangosol.net.Guardable;
        
        Guardable guard = getGuardable();
        if (guard != null)
            {
            com.tangosol.net.Guardian.GuardContext context = guard.getContext();
            if (context != null)
                {
                if (cMillis == 0)
                    {
                    context.heartbeat();
                    }
                else
                    {
                    context.heartbeat(cMillis);
                    }
                }
            }
        }
    
    /**
     * Instantiate a new thread that will be used by this Daemon.
     */
    protected Thread instantiateThread()
        {
        // import com.tangosol.util.Base;
        
        Thread thread = Base.makeThread(getThreadGroup(), this, getThreadName());
        
        thread.setDaemon(true);
        int nPriority = getPriority();
        if (nPriority != 0)
            {
            thread.setPriority(nPriority);
            }
        
        return thread;
        }
    
    // Accessor for the property "AutoStart"
    /**
     * Getter for property AutoStart.<p>
    * Specifies whether this Daemon component should start automatically at the
    * initialization time.
    * 
    * @see #onInit
     */
    public boolean isAutoStart()
        {
        return __m_AutoStart;
        }
    
    // Accessor for the property "Exiting"
    /**
     * Getter for property Exiting.<p>
    * Set to true when the daemon is instructed to stop.
     */
    public boolean isExiting()
        {
        return getDaemonState() == DAEMON_EXITING;
        }
    
    // Accessor for the property "Guarded"
    /**
     * Getter for property Guarded.<p>
    * Is this Daemon being guarded?
     */
    public boolean isGuarded()
        {
        Daemon.Guard guard = getGuardable();
        return guard != null && guard.getContext() != null;
        }
    
    // Accessor for the property "Guardian"
    /**
     * Is this Daemon serving as Guardian to any Guardable objects?
     */
    public boolean isGuardian()
        {
        // import com.tangosol.net.GuardSupport;
        
        GuardSupport support = getGuardSupport();
        return support != null && support.getGuardableCount() > 0;
        }
    
    // Accessor for the property "Started"
    /**
     * Getter for property Started.<p>
    * (Calculated) Specifies whether the daemon has been started.
     */
    public boolean isStarted()
        {
        int nState = getDaemonState();
        return nState > DAEMON_INITIAL && nState < DAEMON_EXITED;
        }
    
    /**
     * Wait for the daemon's thread to stop.
    * 
    * @param cMillis the number of milliseconds to wait for, or zero for
    * infinite
    * 
    * @return true iff the thread is no longer running
     */
    public boolean join(long cMillis)
        {
        try
            {
            Thread thread = getThread();
            if (thread != null)
                {
                thread.join(cMillis);
                return !thread.isAlive();
                }
            return true;
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            return false;
            }
        }
    
    /**
     * Event notification called once the daemon's thread starts and before the
    * daemon thread goes into the "wait - perform" loop. Unlike the
    * <code>onInit()</code> event, this method executes on the daemon's thread.
    * 
    * Note1: this method is called while the caller's thread is still waiting
    * for a notification to  "unblock" itself.
    * Note2: any exception thrown by this method will terminate the thread
    * immediately
     */
    protected void onEnter()
        {
        // import com.tangosol.util.Base;
        
        setStartTimestamp(Base.getSafeTimeMillis());
        }
    
    /**
     * This event occurs when an exception is thrown from onEnter, onWait,
    * onNotify and onExit.
    * 
    * If the exception should terminate the daemon, call stop(). The default
    * implementation prints debugging information and terminates the daemon.
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
            _trace(get_Name() + " caught an unhandled exception (" 
                + e.getClass().getName() + ": " + e.getMessage()
                + ") while exiting.", 4);
            _trace(getStackTrace(e), 9); 
            }
        else
            {
            // protect from OOME thrown by the logger
            try
                {
                _trace("Terminating " + get_Name() + " due to unhandled exception: "
                    + e.getClass().getName(), 1);
                _trace(getStackTrace(e), 2);
                }
            finally
                {
                stop();
                }
            }
        }
    
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        // import com.tangosol.net.Guardian$GuardContext as com.tangosol.net.Guardian.GuardContext;
        
        // COH-10277 - protect against the guardian concurrently releasing
        Daemon.Guard       guard   = getGuardable();
        com.tangosol.net.Guardian.GuardContext context = guard == null ? null : guard.getContext();
        if (context != null)
            {
            context.release();
            }
        
        if (isGuardian())
            {
            getGuardSupport().release();
            }
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification (kind of
    * WM_NCCREATE event) called out of setConstructed() for the topmost
    * component and that in turn notifies all the children. <p>
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as)  the control returns back to the
    * instatiator and possibly on a different thread.
     */
    public void onInit()
        {
        super.onInit();
        
        setGuardable((Daemon.Guard) _findChild("Guard"));
        
        if (isAutoStart())
            {
            start();
            }
        }
    
    /**
     * Event notification called if the daemon's thread get interrupted.
    * 
    * @see #stop
     */
    protected void onInterrupt(InterruptedException e)
        {
        if (!isExiting())
            {
            _trace("Interrupted " + get_Name() + ", " + Thread.currentThread());
            }
        }
    
    /**
     * Event notification for performing low frequency periodic maintance tasks.
    *  The interval is dictated by the WaitMillis property.
    * 
    * This is used for tasks which have a high enough cost that it is not
    * reasonble to perform them on every call to onWait() since it could be
    * called with a high frequency in the presense of work-loads with fast
    * oscillation between onWait() and onNotify().  As an example a single
    * threaded client could produce such a load.
     */
    protected void onInterval()
        {
        // heartbeat to this Guardable's guardian
        heartbeat();
        
        // check any Guardables this Daemon is responsible for
        checkGuardables();
        }
    
    /**
     * Event notification to perform a regular daemon activity. To get it
    * called, another thread has to set Notification to true:
    * <code>daemon.setNotification(true);</code>
    * 
    * @see #onWait
     */
    protected void onNotify()
        {
        }
    
    /**
     * This event occurs when the daemon's run() method is called on an
    * unexpected thread.
     */
    protected void onUnexpectedThread()
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.UUID;
        
        Thread threadDaemon = getThread();
        Thread threadThis   = Thread.currentThread();
        String sMsg;
        
        _assert(threadDaemon != threadThis);
        
        if (System.identityHashCode(threadThis) == System.identityHashCode(threadDaemon)
            && Base.equals(threadThis.getName(), threadDaemon.getName()))
            {        
            // apparently they are the same thread, but the reference equality check failes;
            // this has been seen numerous times but is not easily reproducible
            sMsg = "thread identity corruption detected; the running thread " +
                    threadThis + " has failed a reference equality check with " +
                    threadDaemon + " but matches the daemon's name and " +
                    "identity hash code; this indicates a JVM error.";
            
            // try to reset the name and see if it applies to both threads
            threadThis.setName(new UUID().toString());
            if (Base.equals(threadThis.getName(), threadDaemon.getName()))
                {
                // absolutely impossible without a JVM bug, we have two non-equal
                // references which control the same thread state
                sMsg = "Positive " + sMsg;
                }
            else if (threadThis.getThreadGroup() == threadDaemon.getThreadGroup())
                {
                // distinct threads with the same name, and group
                sMsg = "Probable " + sMsg;
                }
            else
                {
                // distinct threads of different groups
                sMsg = "Possible " + sMsg;
                }
            }
        else
            {
            // our fault
            sMsg = "run() invoked on unexpected thread " + threadThis;
            }
        
        throw new IllegalStateException(sMsg);
        }
    
    /**
     * Event notification called when  the daemon's Thread is waiting for work.
    * 
    * @see #run
     */
    protected void onWait()
            throws java.lang.InterruptedException
        {
        // Note: we don't issue a Guardian heartbeat here as
        // profiling/testing has shown that even the extra virtual call
        // can be costly in critical loops (e.g. TCMP daemons).  Daemon
        // components that intend to be guarded should override as
        // appropriate.
        //
        // Note: getWaitMillis() should likely be overriden as well to
        // limit the wait time as appropriate.
        
        long cMillis = getWaitMillis();
        if (cMillis >= 0L)
            {
            getNotifier().await(cMillis);
            }
        }
    
    // From interface: java.lang.Runnable
    /**
     * This method is called by the JVM right after this daemon's thread starts.
    * It must not be called directly.
     */
    public void run()
        {
        // import com.tangosol.run.component.EventDeathException;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.base.NonBlocking;
        
        // run() must only be invoked on the daemon thread
        if (Thread.currentThread() != getThread())
            {
            onUnexpectedThread();
            }
        
        NonBlocking nonBlocking = null;
        
        try
            {
            // any exception during onEnter kills the thread
            try
                {
                onEnter();
                }
            catch (Throwable e)
                {
                // If an exception is thrown from onEnter, we want to kill
                // the thread.  Returning from here will cause the finally block
                // run onExit() and notify waiters. 
                setStartException(e);
                setExiting(true);
                onException(e);
                return;
                }
        
            if (isNonBlocking())
                {
                // mark thread as non-blocking while running
                nonBlocking = new NonBlocking();
                }
        
            setDaemonState(DAEMON_RUNNING);
                    
            while (!isExiting())
                {
                try
                    {
                    onWait();
                    
                    if (!isExiting())
                        {
                        long ldtNow = Base.getSafeTimeMillis();
                        if (ldtNow >= getIntervalNextMillis())
                            {
                            onInterval();
                            setIntervalNextMillis(ldtNow + Math.min(1000L, getWaitMillis()));
                            }
        
                        onNotify();
                        }
                    }
                catch (EventDeathException e)
                    {
                    // a "normal" exception to "get out of" an event
                    }
                catch (InterruptedException e)
                    {
                    onInterrupt(e);
                    }
                catch (Throwable e)
                    {
                    onException(e);
                    }
                }
            }
        finally
            {
            try
                {
                try
                    {
                    // see comment in stop()
                    synchronized(getExitMonitor())
                        {
                        Thread.interrupted(); // clear interrupt flag
                        }
        
                    if (nonBlocking != null)
                        {
                        nonBlocking.close();
                        }
                    onExit();
                    }
                finally
                    {
                    setThread(null);
                    setDaemonState(DAEMON_EXITED);
                    }
                }
            catch (Throwable e)
                {
                onException(e);
                }
            }
        // the thread terminates right here
        }
    
    // Accessor for the property "AutoStart"
    /**
     * Setter for property AutoStart.<p>
    * Specifies whether this Daemon component should start automatically at the
    * initialization time.
    * 
    * @see #onInit
     */
    public void setAutoStart(boolean fAutoStart)
        {
        __m_AutoStart = fAutoStart;
        }
    
    // Accessor for the property "ClockResolutionMillis"
    /**
     * Setter for property ClockResolutionMillis.<p>
    * The resolution of the system clock in milliseconds.  The value is
    * determined based on the detected OS, see _initStatic().
    * 
    * This may be manually specified via the
    * tangosol.coherence.clock_resolution system property.
     */
    protected static void setClockResolutionMillis(long cMillis)
        {
        __s_ClockResolutionMillis = (Math.max(1, cMillis));
        }
    
    // Accessor for the property "DaemonState"
    /**
     * Change the daemon's state to the specified state iff the new state is
    * greater then the current state.  
    * 
    * Despite this property being @volatile, this setter is synchronized to
    * ensure forward only state transitions.  Additionally this allows for
    * queries of the state to be held stable by synchronizing before the get
    * and the corresponding usage.
    * 
    * State transitions also trigger a notifyAll on the daemon's monitor.
     */
    protected synchronized void setDaemonState(int nState)
        {
        if (nState > getDaemonState())
            {
            __m_DaemonState = (nState);
            notifyAll();
            }
        }
    
    // Accessor for the property "DefaultGuardRecovery"
    /**
     * Setter for property DefaultGuardRecovery.<p>
    * Default recovery percentage for guardables manged by this Daemon.
     */
    public void setDefaultGuardRecovery(float flPctRecover)
        {
        __m_DefaultGuardRecovery = flPctRecover;
        }
    
    // Accessor for the property "DefaultGuardTimeout"
    /**
     * Setter for property DefaultGuardTimeout.<p>
    * Default timeout interval for guardables manged by this Daemon.
     */
    public void setDefaultGuardTimeout(long cTimeoutMillis)
        {
        __m_DefaultGuardTimeout = cTimeoutMillis;
        }
    
    // Accessor for the property "Exiting"
    /**
     * Setter for property Exiting.<p>
    * Set to true when the daemon is instructed to stop.
     */
    protected void setExiting(boolean fExiting)
        {
        setDaemonState(DAEMON_EXITING);
        }
    
    // Accessor for the property "ExitMonitor"
    /**
     * Setter for property ExitMonitor.<p>
    * Monitor object to coordinate clearing the thread interrupt set by stop
    * prior to running onExit().
     */
    private void setExitMonitor(Object oMonitor)
        {
        __m_ExitMonitor = oMonitor;
        }
    
    // Accessor for the property "Guardable"
    /**
     * Setter for property Guardable.<p>
    * A Guardable object reresenting this Daemon. By default, all Daemon
    * components initialize the Guardable property during "init" with their
    * $Guard child component. However, some derived implementation (e.g.
    * PacketPublisher) "remove" the Guard child, leaving this property as null.
     */
    protected void setGuardable(Daemon.Guard guardable)
        {
        __m_Guardable = guardable;
        }
    
    // Accessor for the property "GuardSupport"
    /**
     * Setter for property GuardSupport.<p>
    * The GuardSupport used by this Daemon Guardian to manage its Guardable
    * responsibilities.
     */
    protected void setGuardSupport(com.tangosol.net.GuardSupport guardSupport)
        {
        __m_GuardSupport = guardSupport;
        }
    
    // Accessor for the property "IntervalNextMillis"
    /**
     * Setter for property IntervalNextMillis.<p>
    * The timestamp at which the next call to onInterval should be made.
    * 
    * @see #onInterval
     */
    protected void setIntervalNextMillis(long ldtNext)
        {
        __m_IntervalNextMillis = ldtNext;
        }
    
    // Accessor for the property "Notifier"
    /**
     * Setter for property Notifier.<p>
    * The Notifier serves an a means to wakeup an idle Daemon.
    * 
    * 
    * @see #onNotify
    * @see #onWait
     */
    protected void setNotifier(com.oracle.coherence.common.base.Notifier notifier)
        {
        __m_Notifier = notifier;
        }
    
    // Accessor for the property "Priority"
    /**
     * Setter for property Priority.<p>
    * A non-zero value specifies the priority of the daemon's thread. A zero
    * value implies the Thread default priority.
    * 
    * Priority must be set before the Daemon is started (by the start method)
    * in order to have effect.
     */
    public void setPriority(int nPriority)
        {
        __m_Priority = nPriority;
        }
    
    // Accessor for the property "StartException"
    /**
     * Setter for property StartException.<p>
    * The exception (if any) that prevented the daemon from starting
    * successfully.
     */
    public void setStartException(Throwable e)
        {
        __m_StartException = e;
        }
    
    // Accessor for the property "StartTimestamp"
    /**
     * Setter for property StartTimestamp.<p>
    * Date/time value that this Daemon's thread has started.
     */
    protected void setStartTimestamp(long lMillis)
        {
        __m_StartTimestamp = lMillis;
        }
    
    // Accessor for the property "Thread"
    /**
     * Setter for property Thread.<p>
    * The daemon thread if it is running, or null before the daemon starts and
    * after the daemon stops.
     */
    protected void setThread(Thread thread)
        {
        __m_Thread = thread;
        }
    
    // Accessor for the property "ThreadGroup"
    /**
     * Setter for property ThreadGroup.<p>
    * Specifies the ThreadGroup within which the daemon thread will be created.
    * If not specified, the current Thread's ThreadGroup will be used.
    * 
    * This property can only be set at runtime, and must be configured before
    * start() is invoked to cause the daemon thread to be created within the
    * specified ThreadGroup.
    * 
    * @see java.lang.Thread
    * @see java.lang.ThreadGroup
     */
    public void setThreadGroup(ThreadGroup group)
        {
        __m_ThreadGroup = group;
        }
    
    // Accessor for the property "ThreadName"
    /**
     * Setter for property ThreadName.<p>
    * Specifies the name of the daemon thread. If not specified, the component
    * name will be used.
    * 
    * This property can be set at design time or runtime. If set at runtime,
    * this property must be configured before start() is invoked to cause the
    * daemon thread to have the specified name.
     */
    public void setThreadName(String sName)
        {
        __m_ThreadName = sName;
        }
    
    // Accessor for the property "WaitMillis"
    /**
     * Setter for property WaitMillis.<p>
    * The number of milliseconds that the daemon will wait for notification.
    * Zero means to wait indefinitely. Negative value means to skip waiting
    * altogether.
    * 
    * @see #onWait
     */
    public void setWaitMillis(long cMillis)
        {
        __m_WaitMillis = cMillis;
        }
    
    /**
     * Causes the current thread to sleep for the specified interval.  If
    * interrupted while sleeping the interrupt flag will be set and sleep will
    * return false.
    * 
    * @return true if the thread slept, or false if its sleep was interrupted.
     */
    public static boolean sleep(long lMillis)
        {
        // import com.oracle.coherence.common.base.Blocking;
        
        try
            {
            if (lMillis == 0)
                {
                Thread.yield();
                }
            else
                {
                Blocking.sleep(lMillis);
                }
            return true;
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            return false;
            }
        }
    
    /**
     * Starts the daemon thread associated with this component. If the thread is
    * already starting or has started, invoking this method has no effect.
    * 
    * Synchronization is used here to verify that the start of the thread
    * occurs; the lock is obtained before the thread is started, and the daemon
    * thread notifies back that it has started from the run() method.
     */
    public synchronized void start()
        {
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.WrapperException;
        
        if (isStarted())
            {
            return;
            }
        
        if (getDaemonState() == DAEMON_EXITED)
            {
            throw new IllegalStateException("Daemon is not restartable");
            }
        
        Thread thread = instantiateThread();
        
        // start the thread
        setThread(thread);
        setStartException(null);
        setDaemonState(DAEMON_STARTING);
        thread.start();
        
        // wait for the thread to enter its "wait for notification" section
        try
            {
            do
                {
                Blocking.wait(this, 1000L);
                }
            while (getDaemonState() < DAEMON_RUNNING && thread.isAlive());
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new WrapperException(e);
            }
        
        Throwable e = getStartException();
        if (e != null)
            {
            setStartException(null);
        
            // throw a WrapperException so that the exception comes
            // from the current thread -- the one that called start() --
            // but still displays the information about what killed
            // the daemon thread
            throw new WrapperException(e);
            }
        
        // check that we managed to start
        if (getDaemonState() == DAEMON_STARTING)
            {
            // daemon thread failed to start or capture exception
            // the most likely cause of this condition is a reference equality failure
            // see run()
            throw new IllegalStateException("daemon thread start failed: " + thread); 
            }
        }
    
    /**
     * Stops the daemon thread associated with this component.
     */
    public void stop()
        {
        // Once setExiting(true) is invoked the daemon’s thread will attempt to clear any interrupts and then proceed to onExit.
        // In order to ensure that this doesn’t occur before we actually get to interrupt the thread we synchronize this method
        // as well as run’s call to clear the interrupt.
        synchronized(getExitMonitor())
            {
            // only go through stop() once to prevent spurious interrupts during onExit()
            if (!isExiting())
                {
                setExiting(true);
        
                Thread thread = getThread();
                if (thread != null && thread != Thread.currentThread())
                    {
                    try
                        {
                        thread.interrupt();
                        }
                    catch (Throwable ignored)
                        {
                        }
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.Daemon$Guard
    
    /**
     * Guard provides the Guardable interface implementation for the Daemon.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Guard
            extends    com.tangosol.coherence.Component
            implements com.tangosol.net.Guardable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Context
         *
         * The GuardContext guarding the associated Daemon component.
         */
        private com.tangosol.net.Guardian.GuardContext __m_Context;
        
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
            return new com.tangosol.coherence.component.util.Daemon.Guard();
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
                clz = Class.forName("com.tangosol.coherence/component/util/Daemon$Guard".replace('/', '.'));
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
        
        // From interface: com.tangosol.net.Guardable
        // Accessor for the property "Context"
        /**
         * Getter for property Context.<p>
        * The GuardContext guarding the associated Daemon component.
         */
        public com.tangosol.net.Guardian.GuardContext getContext()
            {
            return __m_Context;
            }
        
        // Accessor for the property "TimeoutDescription"
        /**
         * Return a String description of the cause, or likely cause, of this
        * Guardable exceeding its timeout.
         */
        public String getTimeoutDescription()
            {
            return null;
            }
        
        // From interface: com.tangosol.net.Guardable
        public void recover()
            {
            Daemon daemon = (Daemon) get_Parent();
            
            // Default action is to interrupt the daemon thread
            Thread thread = daemon.getThread();
            if (thread != null)
                {
                thread.interrupt();
                }
            }
        
        // From interface: com.tangosol.net.Guardable
        // Accessor for the property "Context"
        /**
         * Setter for property Context.<p>
        * The GuardContext guarding the associated Daemon component.
         */
        public void setContext(com.tangosol.net.Guardian.GuardContext context)
            {
            __m_Context = context;
            }
        
        // From interface: com.tangosol.net.Guardable
        public void terminate()
            {
            // This method should be over-ridden where applicable;
            // the base implementation does not do anything
            }
        
        // Declared at the super level
        public String toString()
            {
            String sCause = getTimeoutDescription();
            
            sCause = sCause == null ? "" : " " + sCause;
            
            return get_Name() +
                "{Daemon=" + ((Daemon) get_Parent()).getThreadName() + "}" + sCause;
            }
        }
    }


/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketListener

package com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor;

import com.oracle.coherence.common.io.BufferManager;
import com.tangosol.run.component.EventDeathException;
import com.tangosol.util.Base;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * This is a Daemon component that waits for items to process from a Queue.
 * Whenever the Queue contains an item, the onNotify event occurs. It is
 * expected that sub-classes will process onNotify as follows:
 * <pre><code>
 * Object o;
 * while ((o = getQueue().removeNoWait()) != null)
 *     {
 *     // process the item
 *     // ...
 *     }
 * </code></pre>
 * <p>
 * The Queue is used as the synchronization point for the daemon.
 * 
 * <br>
 * A client of PacketProcessor must configure:<br>
 * <br><ul>
 * <li>MemberSet property</li>
 * </ul><br>
 * A client of PacketDispatcher may configure:<br>
 * <br><ul>
 * <li>Priority property</li>
 * <li>ThreadGroup property</li>
 * </ul><br>
 * See the associated documentation for each.<br>
 * <br>
 * Once the PacketProcessor is configured, the client can start the processor
 * using the start() method.<br>
 * 
 * A client of PacketListener must configure:
 * <ul>
 * <li>UdpSocket property</li>
 * <li>ReceiveQueue property</li>
 * <li>PacketAllocator</li>
 * <li>PacketLength</li>
 * </ul>
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PacketListener
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.PacketProcessor
    {
    // ---- Fields declarations ----
    
    /**
     * Property BufferManager
     *
     * The BufferManager from which all ByteBuffers to read packets into are
     * acquired. 
     */
    private com.oracle.coherence.common.io.BufferManager __m_BufferManager;
    
    /**
     * Property PacketLength
     *
     * The maximum packet size supported by this listener.
     */
    private int __m_PacketLength;
    
    /**
     * Property StatsCpu
     *
     * Statistics: total time spent processing packets.
     */
    private transient long __m_StatsCpu;
    
    /**
     * Property StatsReset
     *
     * Statistics: Date/time value that the stats have been reset.
     */
    private transient long __m_StatsReset;
    
    /**
     * Property UdpSocket
     *
     * The UdpSocket to read packets from.
     */
    private com.tangosol.coherence.component.net.socket.UdpSocket __m_UdpSocket;
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
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        }
    
    // Default constructor
    public PacketListener()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PacketListener(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        _addChild(new com.tangosol.coherence.component.util.Daemon.Guard("Guard", this, true), "Guard");
        
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketListener();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketListener".replace('/', '.'));
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
    
    public String formatStats()
        {
        // import com.tangosol.util.Base;
        
        long   cCpu   = getStatsCpu();
        long   cTotal = Base.getSafeTimeMillis() - getStartTimestamp();
        double dCpu   = cTotal == 0L ? 0.0 : ((double) cCpu)/((double) cTotal);
        
        return "Cpu=" + cCpu + "ms (" + (float) dCpu + "%)";
        }
    
    // Accessor for the property "BufferManager"
    /**
     * Getter for property BufferManager.<p>
    * The BufferManager from which all ByteBuffers to read packets into are
    * acquired. 
     */
    public com.oracle.coherence.common.io.BufferManager getBufferManager()
        {
        return __m_BufferManager;
        }
    
    // Accessor for the property "PacketLength"
    /**
     * Getter for property PacketLength.<p>
    * The maximum packet size supported by this listener.
     */
    public int getPacketLength()
        {
        return __m_PacketLength;
        }
    
    // Accessor for the property "StatsCpu"
    /**
     * Getter for property StatsCpu.<p>
    * Statistics: total time spent processing packets.
     */
    public long getStatsCpu()
        {
        return __m_StatsCpu;
        }
    
    // Accessor for the property "StatsReset"
    /**
     * Getter for property StatsReset.<p>
    * Statistics: Date/time value that the stats have been reset.
     */
    public long getStatsReset()
        {
        return __m_StatsReset;
        }
    
    // Accessor for the property "UdpSocket"
    /**
     * Getter for property UdpSocket.<p>
    * The UdpSocket to read packets from.
     */
    public com.tangosol.coherence.component.net.socket.UdpSocket getUdpSocket()
        {
        return __m_UdpSocket;
        }
    
    // Declared at the super level
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
        super.onEnter();
        
        resetStats();
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
        // import com.oracle.coherence.common.io.BufferManager;
        // import java.net.SocketAddress;
        // import java.nio.ByteBuffer;
        
        BufferManager mgr    = getBufferManager();
        ByteBuffer    buffer = mgr.acquire(getPacketLength());
        
        SocketAddress addrSrc = getUdpSocket().receive(buffer);
        if (addrSrc == null)
            {
            mgr.release(buffer);
            }
        else
            {
            onPacket(addrSrc, mgr.truncate(buffer));
            }
        
        // no time related stats are measured here as all it could possibly include
        // is the time between the return from socket.receive(), and the return
        // of queue.add().  This would only be measuring the amount of time
        // spent enqueuing a packet, and would generally be 0ms.  Any timing we tried
        // to measure here would just be a waste of CPU in a critical loop.
        // Testing indicates that adding time measurment is too costly.
        }
    
    /**
     * Invoked when a packet is received.
     */
    protected void onPacket(java.net.SocketAddress addrSource, java.nio.ByteBuffer bufPacket)
        {
        }
    
    public void onReceiveException(Exception e)
        {
        // import com.tangosol.run.component.EventDeathException;
        
        // not enough information here to determine why the receive failed
        throw new EventDeathException(e.toString());
        }
    
    // Declared at the super level
    /**
     * Event notification called when  the daemon's Thread is waiting for work.
    * 
    * @see #run
     */
    protected void onWait()
            throws java.lang.InterruptedException
        {
        // Nothing to wait for, all the work is done in onNotify
        // breaking up the simple onNotify login into onWait & onNotify
        // costs 10MB/sec in performance tests.
        return;
        }
    
    public void resetStats()
        {
        // import com.tangosol.util.Base;
        
        setStatsCpu(0L);
        setStatsReset(Base.getSafeTimeMillis());
        }
    
    // Accessor for the property "BufferManager"
    /**
     * Setter for property BufferManager.<p>
    * The BufferManager from which all ByteBuffers to read packets into are
    * acquired. 
     */
    public void setBufferManager(com.oracle.coherence.common.io.BufferManager mgr)
        {
        __m_BufferManager = mgr;
        }
    
    // Accessor for the property "PacketLength"
    /**
     * Setter for property PacketLength.<p>
    * The maximum packet size supported by this listener.
     */
    public void setPacketLength(int pPacketLength)
        {
        __m_PacketLength = pPacketLength;
        }
    
    // Accessor for the property "StatsCpu"
    /**
     * Setter for property StatsCpu.<p>
    * Statistics: total time spent processing packets.
     */
    protected void setStatsCpu(long cMillis)
        {
        __m_StatsCpu = cMillis;
        }
    
    // Accessor for the property "StatsReset"
    /**
     * Setter for property StatsReset.<p>
    * Statistics: Date/time value that the stats have been reset.
     */
    protected void setStatsReset(long lMillis)
        {
        __m_StatsReset = lMillis;
        }
    
    // Accessor for the property "UdpSocket"
    /**
     * Setter for property UdpSocket.<p>
    * The UdpSocket to read packets from.
     */
    public void setUdpSocket(com.tangosol.coherence.component.net.socket.UdpSocket socket)
        {
        _assert(!isStarted());
        
        __m_UdpSocket = (socket);
        }
    
    // Declared at the super level
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
        if (getUdpSocket() == null)
            {
            throw new IllegalStateException("DatagramSocket is required!");
            }
        
        super.start();
        }
    
    // Declared at the super level
    /**
     * Stops the daemon thread associated with this component.
     */
    public void stop()
        {
        super.stop();
        
        try
            {
            getUdpSocket().close();
            }
        catch (Throwable e) {}
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + ':' + formatStats();
        }
    
    /**
     * Attempt to start the listener
    * 
    * @return true if this invocation started the listener, false otherwise
     */
    public synchronized boolean tryStart()
        {
        // import java.io.IOException;
        
        if (getDaemonState() == DAEMON_INITIAL)
            {
            try
                {
                getUdpSocket().open();
                start();
                return true;
                }
            catch (IOException e) {}
            }
        // else; already started or exited, leave it be
        
        return false;
        }
    }

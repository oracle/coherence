
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker

package com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor;

import com.tangosol.coherence.component.net.PacketBundle;
import com.tangosol.coherence.component.net.socket.UdpSocket;
import com.tangosol.coherence.config.Config;
import com.tangosol.util.Base;

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
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PacketSpeaker
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.PacketProcessor
    {
    // ---- Fields declarations ----
    
    /**
     * Property StatsCpu
     *
     * Statistics: total time spent while sending packets.
     */
    private transient long __m_StatsCpu;
    
    /**
     * Property StatsReset
     *
     * Statistics: Date/time value that the stats have been reset.
     */
    private transient long __m_StatsReset;
    
    /**
     * Property StatsSent
     *
     * Statistics: total number of sent packets.
     */
    private transient long __m_StatsSent;
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
        __mapChildren.put("BundlingQueue", PacketSpeaker.BundlingQueue.get_CLASS());
        __mapChildren.put("InQueue", PacketSpeaker.InQueue.get_CLASS());
        }
    
    // Default constructor
    public PacketSpeaker()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PacketSpeaker(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketSpeaker".replace('/', '.'));
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
        
        long   ldtNow = Base.getSafeTimeMillis();
        long   cCpu   = getStatsCpu();
        long   cTotal = ldtNow - getStartTimestamp();
        long   lSent  = getStatsSent();
        double dCpu   = cTotal == 0L ? 0.0 : ((double) cCpu)/((double) cTotal);
        double dThru  = cCpu   == 0L ? 0.0 : ((double) lSent*1000)/((double) cCpu);
        
        // round rates
        dCpu = ((int) (dCpu  * 1000)) / 10D; // percentage
        
        return "Cpu=" + cCpu + "ms (" + dCpu + "%)"
               + ", PacketsSent="     + lSent
               + ", Throughput="      + (int) dThru + "pkt/sec"
               + ", Queued="          + getQueue().size();
        }
    
    // Accessor for the property "StatsCpu"
    /**
     * Getter for property StatsCpu.<p>
    * Statistics: total time spent while sending packets.
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
    
    // Accessor for the property "StatsSent"
    /**
     * Getter for property StatsSent.<p>
    * Statistics: total number of sent packets.
     */
    public long getStatsSent()
        {
        return __m_StatsSent;
        }
    
    // Declared at the super level
    /**
     * Create the queue for this QueueProcessor.
     */
    protected com.tangosol.coherence.component.util.Queue instantiateQueue()
        {
        return (PacketSpeaker.BundlingQueue) _newChild("BundlingQueue");
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
        
        ((PacketSpeaker.BundlingQueue) getQueue()).setSpeakerEnabled(true);
        }
    
    // Declared at the super level
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        ((PacketSpeaker.BundlingQueue) getQueue()).setSpeakerEnabled(false);
        
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
        // import Component.Net.PacketBundle;
        // import Component.Net.Socket.UdpSocket;
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        
        // Note: we don't use getSafeTimeMillis() to avoid extra synchronization
        
        com.tangosol.coherence.component.util.Queue        queue    = getQueue();
        int          cPackets = 0;                          // burst count
        long         ldtStart = System.currentTimeMillis(); // burst start
        PacketBundle bundle;
        
        while (!isExiting())
            {
            bundle = (PacketBundle) queue.removeNoWait();
            if (bundle == null)
                {
                updateStats(cPackets, ldtStart);
        
                bundle = (PacketBundle) queue.remove();
        
                cPackets = 0;
                ldtStart = System.currentTimeMillis();
                }
        
            cPackets += bundle.getAddressCount(); // the bundle is sent as a "packet" to every address
            if (cPackets > 30000)
                {
                // ensure we periodically update; 30,000 would represent about 1/2 second
                // worth volume of 1468b packets on 1gb nic
        
                updateStats(cPackets, ldtStart);
         
                cPackets = 0;
                ldtStart = System.currentTimeMillis();
                }
        
            try
                {
                bundle.send();
                }
            catch (RuntimeException e)
                {
                if (isExiting())
                    {
                    // ignore exception during exit
                    return;
                    }
        
                // if the socket is closed ignore the exception
                if (bundle.getUdpSocket().getState() != UdpSocket.STATE_CLOSED)
                    {
                    // this will terminate the cluster service
                    throw e;
                    }
                }
            }
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
        // return immediately, onNotify does all the work
        return;
        }
    
    /**
     * Reset the statistics.
     */
    public void resetStats()
        {
        // import com.tangosol.util.Base;
        
        setStatsSent(0L);
        setStatsCpu(0L);
        setStatsReset(Base.getSafeTimeMillis());
        
        ((PacketSpeaker.BundlingQueue) getQueue()).resetStats();
        }
    
    // Accessor for the property "StatsCpu"
    /**
     * Setter for property StatsCpu.<p>
    * Statistics: total time spent while sending packets.
     */
    protected void setStatsCpu(long lMillis)
        {
        __m_StatsCpu = lMillis;
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
    
    // Accessor for the property "StatsSent"
    /**
     * Setter for property StatsSent.<p>
    * Statistics: total number of sent packets.
     */
    protected void setStatsSent(long cPackets)
        {
        __m_StatsSent = cPackets;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + ':' + formatStats();
        }
    
    /**
     * Update the packet speaker stats.
     */
    protected void updateStats(int cPackets, long ldtStart)
        {
        if (cPackets > 0)
            {
            long lDelta = System.currentTimeMillis() - ldtStart;
            if (lDelta > 0)
                {
                setStatsCpu(getStatsCpu() + lDelta);
                }
            setStatsSent(getStatsSent() + cPackets);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker$BundlingQueue
    
    /**
     * A Queue which multiplexes and bundles enqueued items onto one of many
     * internal queues based on a "target" property of the enqueued item. 
     * Items enqueued with the same target will maintain FIFO ordering, but may
     * be re-ordered with respect to items with for different targets.
     * 
     * The abstract implementation must be extended with implementations for
     * bundle(Object, Object).
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BundlingQueue
            extends    com.tangosol.coherence.component.util.queue.concurrentQueue.balancedQueue.BundlingQueue
        {
        // ---- Fields declarations ----
        
        /**
         * Property CloggedCount
         *
         * The maximum number of packets in the speaker bundling queue before
         * determining that the speaker is clogged. Zero means no limit.
         */
        private int __m_CloggedCount;
        
        /**
         * Property CloggedDelay
         *
         * The number of milliseconds to pause publisher thread when a clog
         * occurs, to wait for the clog to dissipate. (The pause is repeated
         * until the clog is gone.) Anything less than one (e.g. zero) is
         * treated as one.
         */
        private int __m_CloggedDelay;
        
        /**
         * Property MULTIPOINT_TARGET
         *
         */
        public static final Object MULTIPOINT_TARGET;
        
        /**
         * Property PendingAsyncFlushCount
         *
         * The number of upcoming flushes which will be forced to be
         * asynchronous regardless of queue size.
         */
        private transient int __m_PendingAsyncFlushCount;
        
        /**
         * Property SpeakerEnabled
         *
         * Specifies whether or not the speaker is enabled.
         */
        private boolean __m_SpeakerEnabled;
        
        /**
         * Property SynchronousAddCount
         *
         * The number of synchronous adds() since the last explicit flush.
         */
        private transient int __m_SynchronousAddCount;
        
        /**
         * Property SynchronousSendCount
         *
         * The number of synchronous sends since the last explicit flush.
         */
        private transient int __m_SynchronousSendCount;
        
        /**
         * Property VolumeThreshold
         *
         * The threshold on the number of packets which may be sent
         * synchornously (by the publisher) before switching to asynchronous
         * sends via the Speaker.
         */
        private transient int __m_VolumeThreshold;
        
        /**
         * Property VolumeTunable
         *
         * Indicates if the volume threshold should be auto-tuned at runtime.
         */
        private transient boolean __m_VolumeTunable;
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            try
                {
                MULTIPOINT_TARGET = new java.lang.Object();
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Iterator", PacketSpeaker.BundlingQueue.Iterator.get_CLASS());
            __mapChildren.put("TargetQueue", PacketSpeaker.BundlingQueue.TargetQueue.get_CLASS());
            }
        
        // Default constructor
        public BundlingQueue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BundlingQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setBatchSize(4);
                setBundlingEnabled(true);
                setCloggedCount(1024);
                setCloggedDelay(32);
                setElementList(new com.tangosol.util.RecyclingLinkedList());
                setTargetMap(new com.tangosol.util.SafeHashMap());
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
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker.BundlingQueue();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketSpeaker$BundlingQueue".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Appends the specified packet to the end of this queue, or immediately
        * perform the packet transmission on the caller's thread based on the
        * FlushState and VolumeThreshold.
        * 
        * @param oElement element to be appended to this Queue
        * 
        * @return true iff the packet was added to the queue
         */
        public boolean add(Object oElement)
            {
            // import Component.Net.Socket.UdpSocket;
            // import Component.Net.PacketBundle;
            // import com.tangosol.util.Base;
            
            // check if we should send immediately avoiding any possibility of bundling,
            // but saving the cost of queue add, notify, remove
            // NOTE: this method is only called on publisher
            PacketBundle bundle     = (PacketBundle) oElement;
            int          cSyncSends = getSynchronousSendCount();
            int          cAddr      = bundle.getAddressCount();
             
            if (!isSpeakerEnabled() ||
                (getDeferralThresholdNanos()       == 0L
                    && getPendingAsyncFlushCount() == 0
                    && cSyncSends                  <= getVolumeThreshold()
                    && isEmpty()))
                {
                // send via publisher thread
                try
                    {
                    bundle.send();
                    setSynchronousSendCount(cSyncSends + cAddr);
                    }
                catch (Exception e)
                    {
                    // if the socket is closed ignore the exception
                    if (bundle.getUdpSocket().getState() != UdpSocket.STATE_CLOSED)
                        {
                        throw Base.ensureRuntimeException(e);
                        }
                    }
                return false;
                }
            else if (size() > getCloggedCount())
                {
                // throttle publisher thread
                drainOverflow();
                }
            
            // add to the queue for later processing
            setSynchronousAddCount(getSynchronousAddCount() + cAddr);
            return super.add(oElement);
            }
        
        /**
         * This method is called on PacketPublisher thread before it enqueues a
        * packet onto the speaker.  It checks to see if the speaker is clogged
        * and if so the publisher thread will wait until the clog is resolved
        * before adding the packet.
         */
        protected void drainOverflow()
            {
            // import com.tangosol.util.Base;
            
            int cMaxPackets = getCloggedCount();
            if (cMaxPackets == 0)
                {
                return; // no throttling
                }
            
            PacketSpeaker speaker      = (PacketSpeaker) get_Module();
            long    ldtStart     = Base.getSafeTimeMillis();;
            long    ldtAlarmNext = ldtStart + 10000L;
            long    cDelayMillis = getCloggedDelay();
            
            flush();
            
            for (int cPackets = size(); cPackets > cMaxPackets && speaker.sleep(cDelayMillis); cPackets = size())
                {
                long ldtNow = Base.getSafeTimeMillis();
                if (ldtNow >= ldtAlarmNext)
                    {
                    // we've been paused for long, issue warning
                    _trace("Overloaded speaker queue; " + cPackets + "/" + cMaxPackets + " packet limit" +
                             ", Duration=" + ((ldtNow - ldtStart) / 1000L) + "s", 2);
                    ldtAlarmNext = ldtNow + 10000L;
                    }
                }
            }
        
        // Declared at the super level
        /**
         * Flush the queue.  The associated packet transmission may be performed
        * on the caller's thread based on the FlushState and  VolumeThreshold.
        * 
        * @param fAuto iff the flush was invoked automatically based on the
        * notification batch size
         */
        protected void flush(boolean fAuto)
            {
            // import Component.Net.Socket.UdpSocket;
            // import Component.Net.PacketBundle;
            // import com.tangosol.util.Base;
            
            // reset add counter
            int cPackets = size();
            if (cPackets == 0)
                {
                // nothing to do, this is the normal path when all packets
                // are handled on the publisher thread
                if (!fAuto)
                    {
                    // reset counters
                    setSynchronousSendCount(0);
                    setSynchronousAddCount(0);
                    }
                // eat the flush
                return;
                }
            
            // check if activity is low enough to perform send on publisher thread
            int     cPending     = getPendingAsyncFlushCount();
            int     cSyncSends   = getSynchronousSendCount();
            int     cSyncAdds    = getSynchronousAddCount();
            double  dAggression  = getBundlingAggression();
            boolean fSpeakerIdle = getFlushState() == FLUSH_PENDING &&
                                   cSyncAdds == cPackets;
            
            boolean fUnderLimit;
            if (isVolumeTunable() && dAggression != 0.0)
                {
                // As we are configured for aggression, and are allowed to dynamically change
                // the threshold.  Encourage bundling by decreasing the effective threshold
                // relative to the aggression level.
                fUnderLimit = cSyncSends < getVolumeThreshold() / (dAggression + 1.0);
                }
            else
                {
                fUnderLimit = cSyncSends < getVolumeThreshold();
                }
            
            if (fSpeakerIdle && fUnderLimit && cPending == 0)
                {
                // send via publisher thread
                PacketSpeaker speaker = (PacketSpeaker) get_Module();
                long    cNanos  = getDeferralThresholdNanos();
                try
                    {
                    if (cNanos != 0L)
                        {
                        // temporarily disable deferral
                        setDeferralThresholdNanos(0L);
                        }
            
                    for (Object oNext = removeNoWait(); oNext != null; oNext = removeNoWait())
                        {
                        PacketBundle bundle = (PacketBundle) oNext;
            
                        try
                            {
                            bundle.send();
                            cSyncSends += bundle.getAddressCount();
                            }
                         catch (Exception e)
                            {
                            // if the socket is closed ignore the exception
                            if (bundle.getUdpSocket().getState() != UdpSocket.STATE_CLOSED)
                                {
                                throw Base.ensureRuntimeException(e);
                                }
                            }
                        }
                    }
                finally
                    {
                    if (cNanos != 0L)
                        {
                        // reenable deferral
                        setDeferralThresholdNanos(cNanos);
                        }
                    }
            
                if (!fAuto)
                    {
                    // reset counter
                    cSyncSends = 0;
                    }
                // don't notify the speaker (no super call)
                }
            else
                {
                // offload to speaker thread
                super.flush(fAuto);
            
                // reset counter
                cSyncSends = 0;
            
                // set likelyhood of future small flushes being either sync or async
                setPendingAsyncFlushCount(fUnderLimit
                    ? Math.max(0,    cPending - 1)
                    : Math.min(1024, cPending + (int) dAggression));
                }
            
            setSynchronousAddCount(0);
            setSynchronousSendCount(cSyncSends);
            }
        
        // Accessor for the property "CloggedCount"
        /**
         * Getter for property CloggedCount.<p>
        * The maximum number of packets in the speaker bundling queue before
        * determining that the speaker is clogged. Zero means no limit.
         */
        public int getCloggedCount()
            {
            return __m_CloggedCount;
            }
        
        // Accessor for the property "CloggedDelay"
        /**
         * Getter for property CloggedDelay.<p>
        * The number of milliseconds to pause publisher thread when a clog
        * occurs, to wait for the clog to dissipate. (The pause is repeated
        * until the clog is gone.) Anything less than one (e.g. zero) is
        * treated as one.
         */
        public int getCloggedDelay()
            {
            return __m_CloggedDelay;
            }
        
        // Accessor for the property "PendingAsyncFlushCount"
        /**
         * Getter for property PendingAsyncFlushCount.<p>
        * The number of upcoming flushes which will be forced to be
        * asynchronous regardless of queue size.
         */
        public int getPendingAsyncFlushCount()
            {
            return __m_PendingAsyncFlushCount;
            }
        
        // Accessor for the property "SynchronousAddCount"
        /**
         * Getter for property SynchronousAddCount.<p>
        * The number of synchronous adds() since the last explicit flush.
         */
        public int getSynchronousAddCount()
            {
            return __m_SynchronousAddCount;
            }
        
        // Accessor for the property "SynchronousSendCount"
        /**
         * Getter for property SynchronousSendCount.<p>
        * The number of synchronous sends since the last explicit flush.
         */
        public int getSynchronousSendCount()
            {
            return __m_SynchronousSendCount;
            }
        
        // Declared at the super level
        /**
         * Get the target for the specified item.
         */
        public Object getTarget(Object oElement)
            {
            // import Component.Net.PacketBundle;
            
            PacketBundle bundle = (PacketBundle) oElement;
            
            // multipoint packets share a single queue
            return bundle.getAddressCount() == 1 ?
                bundle.getAddress(0) : MULTIPOINT_TARGET;
            }
        
        // Accessor for the property "VolumeThreshold"
        /**
         * Getter for property VolumeThreshold.<p>
        * The threshold on the number of packets which may be sent
        * synchornously (by the publisher) before switching to asynchronous
        * sends via the Speaker.
         */
        public int getVolumeThreshold()
            {
            return __m_VolumeThreshold;
            }
        
        // Accessor for the property "SpeakerEnabled"
        /**
         * Getter for property SpeakerEnabled.<p>
        * Specifies whether or not the speaker is enabled.
         */
        public boolean isSpeakerEnabled()
            {
            return __m_SpeakerEnabled;
            }
        
        // Accessor for the property "VolumeTunable"
        /**
         * Getter for property VolumeTunable.<p>
        * Indicates if the volume threshold should be auto-tuned at runtime.
         */
        public boolean isVolumeTunable()
            {
            return __m_VolumeTunable;
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
            // import com.tangosol.coherence.config.Config;
            
            setBundlingEnabled(Boolean.valueOf(Config.getProperty(
                    "coherence.speaker.bundling.enabled", "true")).booleanValue());
            
            setBatchSize(Integer.parseInt(Config.getProperty(
                    "coherence.speaker.batch", "8")));
            
            super.onInit();
            }
        
        // Accessor for the property "CloggedCount"
        /**
         * Setter for property CloggedCount.<p>
        * The maximum number of packets in the speaker bundling queue before
        * determining that the speaker is clogged. Zero means no limit.
         */
        public void setCloggedCount(int nCount)
            {
            __m_CloggedCount = nCount;
            }
        
        // Accessor for the property "CloggedDelay"
        /**
         * Setter for property CloggedDelay.<p>
        * The number of milliseconds to pause publisher thread when a clog
        * occurs, to wait for the clog to dissipate. (The pause is repeated
        * until the clog is gone.) Anything less than one (e.g. zero) is
        * treated as one.
         */
        public void setCloggedDelay(int nDelay)
            {
            __m_CloggedDelay = (Math.max(1, nDelay));
            }
        
        // Accessor for the property "PendingAsyncFlushCount"
        /**
         * Setter for property PendingAsyncFlushCount.<p>
        * The number of upcoming flushes which will be forced to be
        * asynchronous regardless of queue size.
         */
        public void setPendingAsyncFlushCount(int cFlush)
            {
            __m_PendingAsyncFlushCount = cFlush;
            }
        
        // Accessor for the property "SpeakerEnabled"
        /**
         * Setter for property SpeakerEnabled.<p>
        * Specifies whether or not the speaker is enabled.
         */
        public void setSpeakerEnabled(boolean fEnabled)
            {
            __m_SpeakerEnabled = fEnabled;
            }
        
        // Accessor for the property "SynchronousAddCount"
        /**
         * Setter for property SynchronousAddCount.<p>
        * The number of synchronous adds() since the last explicit flush.
         */
        protected void setSynchronousAddCount(int cPackets)
            {
            __m_SynchronousAddCount = cPackets;
            }
        
        // Accessor for the property "SynchronousSendCount"
        /**
         * Setter for property SynchronousSendCount.<p>
        * The number of synchronous sends since the last explicit flush.
         */
        protected void setSynchronousSendCount(int cPackets)
            {
            __m_SynchronousSendCount = cPackets;
            }
        
        // Accessor for the property "VolumeThreshold"
        /**
         * Setter for property VolumeThreshold.<p>
        * The threshold on the number of packets which may be sent
        * synchornously (by the publisher) before switching to asynchronous
        * sends via the Speaker.
         */
        public void setVolumeThreshold(int nThreshold)
            {
            __m_VolumeThreshold = nThreshold;
            }
        
        // Accessor for the property "VolumeTunable"
        /**
         * Setter for property VolumeTunable.<p>
        * Indicates if the volume threshold should be auto-tuned at runtime.
         */
        public void setVolumeTunable(boolean fTuneable)
            {
            __m_VolumeTunable = fTuneable;
            }
        
        // Declared at the super level
        public String toString()
            {
            StringBuffer sb = new StringBuffer(super.toString());
            
            sb.append(", threshold=")
              .append(getVolumeThreshold());
            
            return sb.toString();
            }
        
        /**
         * Adjust the speaker's volume theshold based on the publisher's
        * backlog.
        * 
        * @param cPacketsBacklog  the backlog on the publisher queue, may be
        * <=0 to indicate no backlog
         */
        public void tuneVolumeThreshold(int cPacketsBacklog)
            {
            if (isVolumeTunable())
                {
                int nVolume = getVolumeThreshold();
                if (cPacketsBacklog > 0)
                    {
                    // publisher is not keeping up with incomming packets
                    // encourage handoff to speaker if enabled
            
                    if (nVolume > 0)
                        {
                        // amplify the effect of the backlog in determining adjustment
                        nVolume -= Math.max(1, Math.min(cPacketsBacklog << 4, nVolume >>> 2));
                        setVolumeThreshold(nVolume);
                        }
                    }
                else if (getSynchronousSendCount() >= nVolume)
                    {
                    // publisher is keeping up with the incoming packets, but it was throttled
                    // by the volume threshold, increase threshold
                    nVolume += Math.max(1, nVolume);
                    setVolumeThreshold(nVolume);
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker$BundlingQueue$Iterator
        
        /**
         * Iterator of a snapshot of the List object that backs the Queue.
         * Supports remove(). Uses the Queue as the monitor if any
         * synchronization is required.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Iterator
                extends    com.tangosol.coherence.component.util.Queue.Iterator
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Iterator()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker.BundlingQueue.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketSpeaker$BundlingQueue$Iterator".replace('/', '.'));
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
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker$BundlingQueue$TargetQueue
        
        /**
         * Child queue implementation; it is based on DualQueue but avoids all
         * notification during add as there are no threads that block on the
         * child queue.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class TargetQueue
                extends    com.tangosol.coherence.component.util.queue.concurrentQueue.balancedQueue.BundlingQueue.TargetQueue
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
                __mapChildren.put("Iterator", PacketSpeaker.BundlingQueue.TargetQueue.Iterator.get_CLASS());
                }
            
            // Default constructor
            public TargetQueue()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public TargetQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setElementList(new com.tangosol.util.RecyclingLinkedList());
                    setHeadElementList(new com.tangosol.util.RecyclingLinkedList());
                    setHeadLock(new java.lang.Object());
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
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker.BundlingQueue.TargetQueue();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketSpeaker$BundlingQueue$TargetQueue".replace('/', '.'));
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
            /**
             * Attempt to bundling two objects.
            * 
            * @param oSrc the source object
            * @param oDst the destination object
            * 
            * @return true if oSrc was bundled into oDst
             */
            protected boolean bundle(Object oSrc, Object oDst)
                {
                // import Component.Net.PacketBundle;
                
                return ((PacketBundle) oDst).append((PacketBundle) oSrc);
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker$BundlingQueue$TargetQueue$Iterator
            
            /**
             * Iterator of a snapshot of the List object that backs the Queue.
             * Supports remove(). Uses the Queue as the monitor if any
             * synchronization is required.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.queue.concurrentQueue.BalancedQueue.TargetQueue.Iterator
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Iterator()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker.BundlingQueue.TargetQueue.Iterator();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketSpeaker$BundlingQueue$TargetQueue$Iterator".replace('/', '.'));
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
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker$InQueue
    
    /**
     * The DualQueue is optimized for the producer consumer use case.
     * 
     * Producers work on the tail of the queue, consumers operate on the head
     * of the queue.  The two portions of the queue are maintained as seperate
     * lists, and protected by seperate locks.
     * 
     * When a consumer looks at the head of the queue, if it is empty, the head
     * and tail will be swaped.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InQueue
            extends    com.tangosol.coherence.component.util.queue.concurrentQueue.DualQueue
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
            __mapChildren.put("Iterator", PacketSpeaker.InQueue.Iterator.get_CLASS());
            }
        
        // Default constructor
        public InQueue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setBatchSize(1);
                setElementList(new com.tangosol.util.RecyclingLinkedList());
                setHeadElementList(new com.tangosol.util.RecyclingLinkedList());
                setHeadLock(new java.lang.Object());
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
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker.InQueue();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketSpeaker$InQueue".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker$InQueue$Iterator
        
        /**
         * Iterator of a snapshot of the List object that backs the Queue.
         * Supports remove(). Uses the Queue as the monitor if any
         * synchronization is required.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Iterator
                extends    com.tangosol.coherence.component.util.Queue.Iterator
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Iterator()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker.InQueue.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketSpeaker$InQueue$Iterator".replace('/', '.'));
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
            }
        }
    }

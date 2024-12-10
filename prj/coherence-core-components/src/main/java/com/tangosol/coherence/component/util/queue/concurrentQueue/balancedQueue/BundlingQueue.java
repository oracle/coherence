
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.balancedQueue.BundlingQueue

package com.tangosol.coherence.component.util.queue.concurrentQueue.balancedQueue;

import com.tangosol.coherence.component.util.Queue;
import com.tangosol.internal.util.MovingAverage;
import java.util.Map;

/**
 * A Queue which multiplexes and bundles enqueued items onto one of many
 * internal queues based on a "target" property of the enqueued item.  Items
 * enqueued with the same target will maintain FIFO ordering, but may be
 * re-ordered with respect to items with for different targets.
 * 
 * The abstract implementation must be extended with implementations for
 * bundle(Object, Object).
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class BundlingQueue
        extends    com.tangosol.coherence.component.util.queue.concurrentQueue.BalancedQueue
    {
    // ---- Fields declarations ----
    
    /**
     * Property BundleAverage
     *
     * The moving average for the set of recently dequeued items.
     */
    private transient com.tangosol.internal.util.MovingAverage __m_BundleAverage;
    
    /**
     * Property BundlingAggression
     *
     * The bundling aggression. The higher the aggression the closer a bundle
     * must be to the target bundling utilization before it will be allowed to
     * be dequeued.  A level of 0 will bundle only what is currently available,
     * and never wait before dequeuing.
     */
    private transient double __m_BundlingAggression;
    
    /**
     * Property BundlingEnabled
     *
     * Flag indicating if bundling is enabled.  Generally only used for testing
     * purposes.
     */
    private transient boolean __m_BundlingEnabled;
    
    /**
     * Property DeferralThresholdNanos
     *
     * The maximum number of microseconds to defer dequeing the entry at the
     * head of the queue to encourage bundling.
     */
    private transient long __m_DeferralThresholdNanos;
    
    /**
     * Property DeferralThresholdReads
     *
     * The maximum number of total queue reads to defer dequeing the entry at
     * the head of the queue to encourage bundling.  The value is updated
     * dynamically to approximate the DeferralThresholdMicros setting.
     */
    private transient long __m_DeferralThresholdReads;
    
    /**
     * Property ReadAttemptCount
     *
     * The number of internal reads performed.  This is used as a rough clock
     * for sub-milli idle timeouts.
     */
    private transient long __m_ReadAttemptCount;
    
    /**
     * Property StatsBundled
     *
     * The number of objects which were bundled prior to dequeueing.
     */
    private transient long __m_StatsBundled;
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
        __mapChildren.put("Iterator", com.tangosol.coherence.component.util.Queue.Iterator.get_CLASS());
        __mapChildren.put("TargetQueue", BundlingQueue.TargetQueue.get_CLASS());
        }
    
    // Initializing constructor
    public BundlingQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
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
            clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/balancedQueue/BundlingQueue".replace('/', '.'));
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
    
    // Declared at the super level
    /**
     * Getter for property AtomicFlushState.<p>
    * The AtomicLong used to maintain the FlushState.  See getFlushState() and
    * setFlushState() helper methods.
     */
    public java.util.concurrent.atomic.AtomicInteger getAtomicFlushState()
        {
        return super.getAtomicFlushState();
        }
    
    // Accessor for the property "BundleAverage"
    /**
     * Getter for property BundleAverage.<p>
    * The moving average for the set of recently dequeued items.
     */
    public com.tangosol.internal.util.MovingAverage getBundleAverage()
        {
        return __m_BundleAverage;
        }
    
    // Accessor for the property "BundlingAggression"
    /**
     * Getter for property BundlingAggression.<p>
    * The bundling aggression. The higher the aggression the closer a bundle
    * must be to the target bundling utilization before it will be allowed to
    * be dequeued.  A level of 0 will bundle only what is currently available,
    * and never wait before dequeuing.
     */
    public double getBundlingAggression()
        {
        return __m_BundlingAggression;
        }
    
    // Accessor for the property "DeferralThresholdNanos"
    /**
     * Getter for property DeferralThresholdNanos.<p>
    * The maximum number of microseconds to defer dequeing the entry at the
    * head of the queue to encourage bundling.
     */
    public long getDeferralThresholdNanos()
        {
        return __m_DeferralThresholdNanos;
        }
    
    // Accessor for the property "DeferralThresholdReads"
    /**
     * Getter for property DeferralThresholdReads.<p>
    * The maximum number of total queue reads to defer dequeing the entry at
    * the head of the queue to encourage bundling.  The value is updated
    * dynamically to approximate the DeferralThresholdMicros setting.
     */
    public long getDeferralThresholdReads()
        {
        return __m_DeferralThresholdReads;
        }
    
    // Accessor for the property "ReadAttemptCount"
    /**
     * Getter for property ReadAttemptCount.<p>
    * The number of internal reads performed.  This is used as a rough clock
    * for sub-milli idle timeouts.
     */
    public long getReadAttemptCount()
        {
        return __m_ReadAttemptCount;
        }
    
    // Accessor for the property "StatsBundled"
    /**
     * Getter for property StatsBundled.<p>
    * The number of objects which were bundled prior to dequeueing.
     */
    public long getStatsBundled()
        {
        return __m_StatsBundled;
        }
    
    // Declared at the super level
    /**
     * Instantiate a new target queue.
     */
    protected com.tangosol.coherence.component.util.Queue instantiateTargetQueue()
        {
        return isBundlingEnabled() ? (BundlingQueue.TargetQueue) _newChild("TargetQueue")
                                   : super.instantiateTargetQueue();
        }
    
    // Accessor for the property "BundlingEnabled"
    /**
     * Getter for property BundlingEnabled.<p>
    * Flag indicating if bundling is enabled.  Generally only used for testing
    * purposes.
     */
    public boolean isBundlingEnabled()
        {
        return __m_BundlingEnabled;
        }
    
    // Declared at the super level
    /**
     * Called when a target queue has returned null from removeNoWait(),
    * indicating that it is empty.  The queue may become non-empty at any
    * point.
    * 
    * For the BundlingQueue onEmptyTarget() indicates that the target queue is
    * either empty or is deferring the release its entries.
     */
    public void onEmptyTarget(Object oTarget, com.tangosol.coherence.component.util.Queue queueTarget)
        {
        // Bundled queues may appear empty (removeNoWait() == null) when they are not
        // Perform a quick check before calling super, which will synchronize before
        // checking.
        if (queueTarget.isEmpty())
            {
            super.onEmptyTarget(oTarget, queueTarget);
            }
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
        // import com.tangosol.internal.util.MovingAverage;
        
        setBundleAverage(new MovingAverage(1000, 10));
        
        super.onInit();
        }
    
    /**
     * Reset statistics.
     */
    public void resetStats()
        {
        setStatsBundled(0L);
        }
    
    // Declared at the super level
    /**
     * Remove the specified queue for the specified target, so long as it is
    * empty.
     */
    protected void safeRemoveTargetQueue(Object oTarget)
        {
        // import Component.Util.Queue;
        // import java.util.Map;
        
        boolean fRemoved = false;
        
        // protect from add/addHead
        synchronized (getLock(oTarget))
            {
            Map   mapTarget   = getTargetMap();
            Queue queueTarget = (Queue) mapTarget.get(oTarget);
        
            if (queueTarget instanceof BundlingQueue.TargetQueue)
                {
                // protect from temporary removal in TargetQueue.removeNoWait
                synchronized (((BundlingQueue.TargetQueue) queueTarget).getHeadLock())
                    {
                    if (queueTarget.isEmpty())
                        {
                        mapTarget.remove(oTarget);
                        fRemoved = true;
                        }
                    }
                }
            else if (queueTarget != null && queueTarget.isEmpty())
                {
                // bundling is disabled
                mapTarget.remove(oTarget);
                fRemoved = true;
                }
            }
        
        if (fRemoved)
            {
            refreshEntries();
            }
        }
    
    // Accessor for the property "BundleAverage"
    /**
     * Setter for property BundleAverage.<p>
    * The moving average for the set of recently dequeued items.
     */
    protected void setBundleAverage(com.tangosol.internal.util.MovingAverage average)
        {
        __m_BundleAverage = average;
        }
    
    // Accessor for the property "BundlingAggression"
    /**
     * Setter for property BundlingAggression.<p>
    * The bundling aggression. The higher the aggression the closer a bundle
    * must be to the target bundling utilization before it will be allowed to
    * be dequeued.  A level of 0 will bundle only what is currently available,
    * and never wait before dequeuing.
     */
    public void setBundlingAggression(double dAggression)
        {
        __m_BundlingAggression = (Math.max(0.0, dAggression));
        }
    
    // Accessor for the property "BundlingEnabled"
    /**
     * Setter for property BundlingEnabled.<p>
    * Flag indicating if bundling is enabled.  Generally only used for testing
    * purposes.
     */
    public void setBundlingEnabled(boolean fEnabled)
        {
        __m_BundlingEnabled = fEnabled;
        }
    
    // Accessor for the property "DeferralThresholdNanos"
    /**
     * Setter for property DeferralThresholdNanos.<p>
    * The maximum number of microseconds to defer dequeing the entry at the
    * head of the queue to encourage bundling.
     */
    public void setDeferralThresholdNanos(long cNanos)
        {
        __m_DeferralThresholdNanos = cNanos;
        }
    
    // Accessor for the property "DeferralThresholdReads"
    /**
     * Setter for property DeferralThresholdReads.<p>
    * The maximum number of total queue reads to defer dequeing the entry at
    * the head of the queue to encourage bundling.  The value is updated
    * dynamically to approximate the DeferralThresholdMicros setting.
     */
    public void setDeferralThresholdReads(long cReads)
        {
        __m_DeferralThresholdReads = cReads;
        }
    
    // Accessor for the property "ReadAttemptCount"
    /**
     * Setter for property ReadAttemptCount.<p>
    * The number of internal reads performed.  This is used as a rough clock
    * for sub-milli idle timeouts.
     */
    public void setReadAttemptCount(long cAttempts)
        {
        __m_ReadAttemptCount = cAttempts;
        }
    
    // Accessor for the property "StatsBundled"
    /**
     * Setter for property StatsBundled.<p>
    * The number of objects which were bundled prior to dequeueing.
     */
    public void setStatsBundled(long cBundled)
        {
        __m_StatsBundled = cBundled;
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuilder sb = new StringBuilder(super.toString());
        
        sb.append(", bundled=")
          .append(getStatsBundled())
          .append(", average/bundle=")
          .append(getBundleAverage().getDoubleAverage())
          .append(", stddev=")
          .append(getBundleAverage().getStandardDeviation());
        
        return sb.toString();
        }

    // ---- class: com.tangosol.coherence.component.util.queue.concurrentQueue.balancedQueue.BundlingQueue$TargetQueue
    
    /**
     * Child queue implementation; it is based on DualQueue but avoids all
     * notification during add as there are no threads that block on the child
     * queue.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static abstract class TargetQueue
            extends    com.tangosol.coherence.component.util.queue.concurrentQueue.BalancedQueue.TargetQueue
        {
        // ---- Fields declarations ----
        
        /**
         * Property DeferralReadStamp
         *
         * The value of the parent queue's ReadAttemptCount when this queue
         * entered the deferred state.  This is used to approximate
         * sub-millisecond timeouts.
         */
        private transient long __m_DeferralReadStamp;
        
        /**
         * Property DeferralTimeStamp
         *
         * The time at which this queue entered the deferred state, or 0 if the
         * queue is not currently deferred.
         */
        private transient long __m_DeferralTimeStamp;
        
        /**
         * Property HeadBundles
         *
         * The number of entries bundled in to the entry at the head of the
         * queue.
         */
        private int __m_HeadBundles;
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
            __mapChildren.put("Iterator", com.tangosol.coherence.component.util.queue.concurrentQueue.BalancedQueue.TargetQueue.Iterator.get_CLASS());
            }
        
        // Initializing constructor
        public TargetQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
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
                clz = Class.forName("com.tangosol.coherence/component/util/queue/concurrentQueue/balancedQueue/BundlingQueue$TargetQueue".replace('/', '.'));
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
            return false;
            }
        
        // Declared at the super level
        /**
         * Getter for property AtomicFlushState.<p>
        * The AtomicLong used to maintain the FlushState.  See getFlushState()
        * and setFlushState() helper methods.
         */
        public java.util.concurrent.atomic.AtomicInteger getAtomicFlushState()
            {
            return super.getAtomicFlushState();
            }
        
        // Accessor for the property "DeferralReadStamp"
        /**
         * Getter for property DeferralReadStamp.<p>
        * The value of the parent queue's ReadAttemptCount when this queue
        * entered the deferred state.  This is used to approximate
        * sub-millisecond timeouts.
         */
        public long getDeferralReadStamp()
            {
            return __m_DeferralReadStamp;
            }
        
        // Accessor for the property "DeferralTimeStamp"
        /**
         * Getter for property DeferralTimeStamp.<p>
        * The time at which this queue entered the deferred state, or 0 if the
        * queue is not currently deferred.
         */
        public long getDeferralTimeStamp()
            {
            return __m_DeferralTimeStamp;
            }
        
        // Accessor for the property "HeadBundles"
        /**
         * Getter for property HeadBundles.<p>
        * The number of entries bundled in to the entry at the head of the
        * queue.
         */
        public int getHeadBundles()
            {
            return __m_HeadBundles;
            }
        
        /**
         * Return true if the bundle should be deferred.
         */
        public boolean isBundleDeferrable(Object oBundle)
            {
            // import com.tangosol.internal.util.MovingAverage;
            
            if (isDeferralTimeout())
                {
                return false;
                }
            
            // timer has not expired, defer based on average bundle size
            BundlingQueue queueParent = (BundlingQueue) get_Parent();
            MovingAverage average     = queueParent.getBundleAverage();
            double        dAggression = queueParent.getBundlingAggression();
            int           nAverage    = average.getAverage();
            
            return getHeadBundles() < (dAggression == 0.0 ? nAverage
                    : nAverage + dAggression * average.getStandardDeviation());
            }
        
        // Accessor for the property "DeferralTimeout"
        /**
         * Return true iff there is a deferred packet and its deferral timeout
        * has expired.
         */
        public boolean isDeferralTimeout()
            {
            BundlingQueue queueParent     = (BundlingQueue) get_Parent();
            long          cNanosThreshold = queueParent.getDeferralThresholdNanos();
            long          ldtDeferral     = getDeferralTimeStamp();
            
            if (cNanosThreshold == 0L)
                {
                // instant timeout, never defer
                return true;
                }
            
            if (ldtDeferral == 0L)
                {
                // the queue is not currently deferring
                return false;
                }
            
            final long MILLI = 1000000L; // nanos per milli
            
            // use sub-millisecond timer based on read rate
            // TODO: Consider using System.nanoTime() for 1.5
            long cNanos         = Math.abs(System.currentTimeMillis()
                                    - getDeferralTimeStamp()) * MILLI;
            long cReadAttempts  = queueParent.getReadAttemptCount();
            long cReads         = Math.abs(cReadAttempts - getDeferralReadStamp());
            long nReadThreshold = queueParent.getDeferralThresholdReads();
            
            if (cNanos >= cNanosThreshold + MILLI)
                {
                // we hit the idle timeout, readjust the read timeout; timeout + 1ms is
                // used as we know the minimum clock res is 1ms, and we want to ensure that
                // at least 1ms has passed when we estimate the time
                long cReadsOptimal = (cReads * cNanosThreshold) / cNanos;
                if (cReadsOptimal > nReadThreshold)
                    {
                    queueParent.setDeferralThresholdReads(cReadsOptimal);
                    }
                return true;
                }
            else if (nReadThreshold > 0 && cReads > nReadThreshold)
                {
                // we hit the idle read threshold
                if (cReadAttempts % 8192L == 0L)
                    {
                    // periodically reset the overread threshold
                    queueParent.setDeferralThresholdReads(0L);
                    }
                return true;
                }
            
            return false;
            }
        
        // Declared at the super level
        /**
         * Removes and returns the first element from the front of this Queue.
        * If the Queue is empty, no element is returned.
        * 
        * The blocking equivalent of this method is "remove".
        * 
        * @return the first element in the front of this Queue or null if the
        * Queue is empty
        * 
        * @see #remove
         */
        public Object removeNoWait()
            {
            BundlingQueue queueParent = (BundlingQueue) get_Parent();
            long          lReadCount  = queueParent.getReadAttemptCount() + 1L;
            
            // update read counter (sub-milli timer)
            queueParent.setReadAttemptCount(lReadCount);
            
            int    cPassBundles = 0;
            Object oHead;
            synchronized (getHeadLock()) // needed for atomic peek/remove/addHead
                {
                oHead = super.removeNoWait();
                if (oHead == null)
                    {
                    return null;
                    }
            
                // bundle as many entries as possible, selecting approach based on likelihood of bundling
                int    cHeadBundles = getHeadBundles();
                Object oNext;
                if (queueParent.getBundleAverage().getAverage() > cHeadBundles + 3)
                    {
                    // optimized for consecutive bundleable packets
                    for (oNext = super.removeNoWait(); oNext != null && bundle(oNext, oHead);
                         oNext = super.removeNoWait())
                        {
                        // oNext was bundled into oFirst
                        ++cPassBundles;        
                        }
                    if (oNext != null)
                        {
                        // oNext was not bundled
                        addHead(oNext);
                        }
                    }
                else
                    {
                    // optimized for consecutive non-bundleable packets
                    for (oNext = peekNoWait(); oNext != null && bundle(oNext, oHead);
                         oNext = peekNoWait())
                        {
                        // oNext was bundled into oHead
                        super.removeNoWait();
                        ++cPassBundles;
                        }
                    }
            
                cHeadBundles += cPassBundles;
            
                // check if we should defer the head entry
                if (oNext == null && isBundleDeferrable(oHead))
                    {
                    // defer oHead dequeue
                    if (getDeferralTimeStamp() == 0L)
                        {
                        // first deferral of this bundle, record deferral time
                        setDeferralTimeStamp(System.currentTimeMillis());
                        setDeferralReadStamp(lReadCount);
                        }
            
                    setHeadBundles(cHeadBundles);
                    addHead(oHead);
                    oHead = null;
                    }
                else
                    {
                    // allow oHead to be dequeued by not nulling it out;
                    // reset head trackers
                    setDeferralTimeStamp(0L);
                    setDeferralReadStamp(0L);
                    setHeadBundles(0);
            
                    // include the sample even for an empty bundle
                    queueParent.getBundleAverage().addSample(cHeadBundles);
                    }
                }
            
            // update parent state
            if (cPassBundles > 0)
                {
                queueParent.getElementCounter().addAndGet(-cPassBundles);
                queueParent.setStatsBundled(queueParent.getStatsBundled() + cPassBundles);
                }
            
            return oHead;
            }
        
        // Accessor for the property "DeferralReadStamp"
        /**
         * Setter for property DeferralReadStamp.<p>
        * The value of the parent queue's ReadAttemptCount when this queue
        * entered the deferred state.  This is used to approximate
        * sub-millisecond timeouts.
         */
        public void setDeferralReadStamp(long lReadStamp)
            {
            __m_DeferralReadStamp = lReadStamp;
            }
        
        // Accessor for the property "DeferralTimeStamp"
        /**
         * Setter for property DeferralTimeStamp.<p>
        * The time at which this queue entered the deferred state, or 0 if the
        * queue is not currently deferred.
         */
        protected void setDeferralTimeStamp(long ldtAddLast)
            {
            __m_DeferralTimeStamp = ldtAddLast;
            }
        
        // Accessor for the property "HeadBundles"
        /**
         * Setter for property HeadBundles.<p>
        * The number of entries bundled in to the entry at the head of the
        * queue.
         */
        public void setHeadBundles(int pHeadBundles)
            {
            __m_HeadBundles = pHeadBundles;
            }
        
        // Declared at the super level
        public String toString()
            {
            StringBuilder sb = new StringBuilder(super.toString());
            
            sb.append(", head bundles=")
              .append(getHeadBundles());
            
            long ldtDef = getDeferralTimeStamp();
            if (ldtDef != 0L)
                {
                sb.append(", deferred=")
                  .append(System.currentTimeMillis() - ldtDef)
                  .append("ms");
                }
            
            return sb.toString();
            }
        }
    }

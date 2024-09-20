/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static java.util.Arrays.stream;

/**
* An implementation of SegmentedHashMap that also implements the ConcurrentMap
* interface.
* <p>
* See {@link com.tangosol.util.ConcurrentMap}
*
* @since Coherence 3.5
* @author rhl 2008.12.01
*/
public class SegmentedConcurrentMap
        extends SegmentedHashMap
        implements ConcurrentMap, java.util.concurrent.ConcurrentMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SegmentedConcurrentMap()
        {
        super();
        }

    /**
    * Construct a SegmentedConcurrentMap with the default settings and the
    * specified ContentionObserver
    *
    * @param contentionObserver  the ContentionObserver
    */
    public SegmentedConcurrentMap(ContentionObserver contentionObserver)
        {
        super();

        setContentionObserver(contentionObserver);
        }

    /**
    * Construct a SegmentedConcurrentMap using the specified settings.
    *
    * @param cInitialBuckets  the initial number of hash buckets, 0 &lt; n
    * @param flLoadFactor     the acceptable load factor before resizing
    *                         occurs, 0 &lt; n, such that a load factor of
    *                         1.0 causes resizing when the number of entries
    *                         exceeds the number of buckets
    * @param flGrowthRate     the rate of bucket growth when a resize occurs,
    *                         0 &lt; n, such that a growth rate of 1.0 will
    *                         double the number of buckets:
    *                         bucketcount = bucketcount * (1 + growthrate)
    */
    public SegmentedConcurrentMap(int cInitialBuckets, float flLoadFactor,
                                 float flGrowthRate)
        {
        this(cInitialBuckets, flLoadFactor, flGrowthRate, null);
        }

    /**
    * Construct a thread-safe hash map using the specified settings.
    *
    * @param cInitialBuckets     the initial number of hash buckets, 0 &lt; n
    * @param flLoadFactor        the acceptable load factor before resizing
    *                            occurs, 0 &lt; n, such that a load factor of
    *                            1.0 causes resizing when the number of entries
    *                            exceeds the number of buckets
    * @param flGrowthRate        the rate of bucket growth when a resize occurs,
    *                            0 &lt; n, such that a growth rate of 1.0 will
    *                            double the number of buckets:
    *                            bucketcount = bucketcount * (1 + growthrate)
    * @param contentionObserver  the ContentionObserver
    */
    public SegmentedConcurrentMap(int cInitialBuckets, float flLoadFactor,
                                  float flGrowthRate,
                                  ContentionObserver contentionObserver)
        {
        super(cInitialBuckets, flLoadFactor, flGrowthRate);

        setContentionObserver(contentionObserver);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the registered action for lock().
    *
    * @return the registered action for lock()
    */
    protected LockAction getLockAction()
        {
        return m_actionLock;
        }

    /**
    * Specify the action for lock().
    *
    * @param action  the action for lock()
    */
    protected void setLockAction(LockAction action)
        {
        m_actionLock = action;
        }

    /**
    * Return the registered action for unlock().
    *
    * @return the registered action for unlock()
    */
    protected UnlockAction getUnlockAction()
        {
        return m_actionUnlock;
        }

    /**
    * Specify the action for unlock().
    *
    * @param action  the action for unlock()
    */
    protected void setUnlockAction(UnlockAction action)
        {
        m_actionUnlock = action;
        }

    /**
    * Return the registered action for size().
    *
    * @return the registered action for size()
    */
    protected SizeAction getSizeAction()
        {
        return m_actionSize;
        }

    /**
    * Specify the action for size().
    *
    * @param action  the action for size()
    */
    protected void setSizeAction(SizeAction action)
        {
        m_actionSize = action;
        }

    /**
    * Return the registered action for conditional remove.
    *
    * @return the registered action for conditional remove
    */
    protected ConditionalRemoveAction getConditionalRemoveAction()
        {
        return m_actionConditionalRemove;
        }

    /**
    * Specify the action for conditional remove.
    *
    * @param action  the action for conditional remove
    */
    protected void setConditionalRemoveAction(ConditionalRemoveAction action)
        {
        m_actionConditionalRemove = action;
        }

    /**
     * Specify the action for truncate.
     *
     * @param action  the action for truncate
     *
     * @since 12.2.1.4.21
     */
    protected void setTruncateAction(TruncateAction action)
        {
        m_actionTruncate = action;
        }

    /**
     * Specify the action for dump held locks.
     *
     * @param action  the action for dump held locks
     *
     * @since 12.2.1.4.21
     */
    protected void setDumpHeldLocksAction(DumpHeldLocksAction action)
        {
        m_actionDumpHeldLocks = action;
        }

    /**
    * Return the ContentionObserver for this SegmentedConcurrentMap.
    *
    * @return the ContentionObserver
    */
    public ContentionObserver getContentionObserver()
        {
        return m_contentionObserver;
        }

    /**
    * Set the ContentionObserver for this SegmentedConcurrentMap.
    *
    * @param contentionObserver  the contentionObserver
    */
    protected void setContentionObserver(ContentionObserver contentionObserver)
        {
        m_contentionObserver = contentionObserver;
        }


    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void clear()
        {
        // invoke the remove action on all entries
        invokeOnAllKeys(/*oContext*/ null, /*fLock*/ true, getRemoveAction());
        }

    /**
    * {@inheritDoc}
    */
    public int size()
        {
        SizeAction               actionSize = getSizeAction();
        SegmentedHashMap.Entry[] aeBucket;
        Object                   oContext;
        do
            {
            aeBucket = getStableBucketArray();
            oContext = actionSize.instantiateContext(/*fEmptyCheck*/ false);
            invokeOnAllKeys(/*oContext*/ oContext, /*fLock*/ false, actionSize);
            }
        while (aeBucket != getStableBucketArray());
        return actionSize.size(oContext);
        }

    /**
    * {@inheritDoc}
    */
    public boolean isEmpty()
        {
        SizeAction               actionSize = getSizeAction();
        SegmentedHashMap.Entry[] aeBucket;
        Object                   oContext;
        do
            {
            aeBucket = getStableBucketArray();
            oContext = actionSize.instantiateContext(/*fEmptyCheck*/ true);
            invokeOnAllKeys(/*oContext*/ oContext, /*fLock*/ false, actionSize);
            if (actionSize.size(oContext) > 0)
                {
                return false;
                }
            }
        while (aeBucket != getStableBucketArray());
        return true;
        }


    // ----- java.util.concurrent.ConcurrentMap interface -------------------

    /**
    * {@inheritDoc}
    */
    public Object putIfAbsent(Object oKey, Object oValue)
        {
        Object oOrig = putInternal(oKey, oValue, /*fOnlyIfAbsent*/ true);
        return oOrig == NO_VALUE ? null : oOrig;
        }

    /**
    * {@inheritDoc}
    */
    public boolean replace(Object oKey, Object oValueOld, Object oValueNew)
        {
        LockableEntry entry = (LockableEntry) getEntryInternal(oKey, /*fSynthetic*/false);
        return entry != null && entry.casValueInternal(oValueOld, oValueNew);
        }

    /**
    * {@inheritDoc}
    */
    public Object replace(Object oKey, Object oValue)
        {
        LockableEntry entry = (LockableEntry) getEntryInternal(oKey, /*fSynthetic*/false);
        if (entry != null)
            {
            Object oCurrent;
            while ((oCurrent = entry.getValueInternal()) != NO_VALUE)
                {
                if (entry.casValueInternal(oCurrent, oValue))
                    {
                    // replacement was successful
                    return oCurrent;
                    }
                }
            }
        // entry was removed or did not exist
        return null;
        }

    /**
    * {@inheritDoc}
    */
    public boolean remove(Object oKey, Object oValue)
        {
        Object oValueOld = removeInternal(oKey, getConditionalRemoveAction(), oValue);
        return oValueOld != NO_VALUE;
        }


    // ----- ConcurrentMap interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean lock(Object oKey)
        {
        return lock(oKey, 0);
        }

    /**
    * {@inheritDoc}
    */
    public boolean lock(Object oKey, long cWait)
        {
        if (oKey == LOCK_ALL)
            {
            return m_gateLockAll.close(cWait);
            }

        if (!m_gateLockAll.enter(cWait))
            {
            return false;
            }

        // Lock the segment for oKey and find the Entry for it.
        //
        // Either:
        //   * No entry exists             - create a new entry and lock it
        //   * An entry exists (unlocked)  - lock the entry
        //   * An entry exists (locked)    - release the segment lock and wait
        //                                   for the entry-lock to be released
        Thread  thdCurrent = Thread.currentThread();
        boolean fSuccess   = false;
        try
            {
            while (true)
                {
                // try to lock
                Object oResult = invokeOnKey(oKey,
                                             thdCurrent,
                                             /*fLock*/ true,
                                             getLockAction());
                if (oResult == NO_VALUE)
                    {
                    // we locked the entry; stay entered in the lock-all gate
                    //
                    // Note: we may (or may not) have created a new entry in the
                    //       "lock" call.  Failure to resize could dramatically
                    //       degrade performance; luckily, the check is not expensive.
                    ensureLoadFactor(getSegmentForKey(oKey));
                    return fSuccess = true;
                    }

                // we've found the LockableEntry, but it is locked by somebody else

                if (cWait == 0)
                    {
                    // non-blocking lock() should return immediately
                    return false;
                    }

                // wait for the lock to be released
                cWait = ((LockableEntry) oResult)
                            .waitForNotify(cWait, getContentionObserver());
                }
            }
        finally
            {
            if (!fSuccess)
                {
                m_gateLockAll.exit();
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean unlock(Object oKey)
        {
        if (oKey == LOCK_ALL)
            {
            try
                {
                m_gateLockAll.open();
                return true;
                }
            catch (IllegalMonitorStateException e)
                {
                return false;
                }
            }

        Object  oResult   = invokeOnKey(oKey,
                                        Thread.currentThread(),
                                        /*fLock*/ true,
                                        getUnlockAction());
        boolean fReleased = ((Boolean) oResult).booleanValue();

        if (fReleased)
            {
            try
                {
                m_gateLockAll.exit();
                }
            catch (IllegalStateException e)
                {
                }
            }
        return fReleased;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void initializeActions()
        {
        super.initializeActions();

        /* lock() action */
        setLockAction(instantiateLockAction());

        /* unlock() action */
        setUnlockAction(instantiateUnlockAction());

        /* size() action */
        setSizeAction(instantiateSizeAction());

        /* conditional remove action */
        setConditionalRemoveAction(instantiateConditionalRemoveAction());

        /* truncate action */
        setTruncateAction(instantiateTruncateAction());

        /* dumpHeldLocks action */
        setDumpHeldLocksAction(instantiateDumpHeldLocksAction());
        }

    /**
     * Truncate operation for control map.
     *
     * @since 12.2.1.4.21
     */
    public void truncate()
        {
        // invoke the truncate action over all LockableEntries, interrupt each thread that is owned by a lock and unlock the lock.
        // no locking is used by invokeOnALlKeys since this is reacting to an asynchronous truncate of backing map,
        // all pending operations need to be interrupted immediately.
        invokeOnAllKeys(/*oContext*/ null, /*fLock*/ false, new TruncateAction());

        // reset map. Only synthetic contended, unlocked LockableEntries left after clear.
        clear();
        }

    /**
     * Debug aid to dump threads holding locks.
     *
     * @since 12.2.1.4.21
     */
    public void dumpHeldLocks()
        {
        invokeOnAllKeys(/*oContext*/ null, /*fLock*/ false, new DumpHeldLocksAction());
        }

    // ----- inner class: RemoveAction --------------------------------------

    /**
    * Factory for RemoveAction
    */
    protected SegmentedHashMap.RemoveAction instantiateRemoveAction()
        {
        return new RemoveAction();
        }

    /**
    * Action support for remove().  The action performs a logical locked
    * remove, and is expected to run while holding the segment-lock for the
    * specified key.  If the Entry corresponding to the specified key is still
    * necessary in the map (e.g. for representing a lock state), the Entry may
    * not be physically removed, but rather converted to be synthetic.
    * <p>
    * The context object for a RemoveAction is unused.
    * <p>
    * The result of invoking a RemoveAction is the previous value associated
    * with the specified key, or <tt>NO_VALUE</tt> if no mapping for the key
    * exists in the map.  Note that a synthetic Entry does not represent a
    * key-value mapping, so <tt>NO_VALUE</tt> is returned if a matching
    * synthetic Entry is found.
    */
    protected class RemoveAction
            extends SegmentedHashMap.RemoveAction
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object                   oKey,
                                  Object                   oContext,
                                  SegmentedHashMap.Entry[] aeBucket,
                                  int                      nBucket,
                                  SegmentedHashMap.Entry   entryPrev,
                                  SegmentedHashMap.Entry   entryCur)
            {
            LockableEntry entry = (LockableEntry) entryCur;
            if (entry.isLocked() || entry.isContended())
                {
                // The Entry object is still needed for locking; make it
                // synthetic instead of physically removing it.
                return entryCur.setValueInternal(NO_VALUE);
                }
            else
                {
                return super.invokeFound(oKey, oContext, aeBucket,
                                         nBucket, entryPrev, entryCur);
                }
            }
        }


    // ----- inner class: ConditionalRemoveAction ---------------------------

    /**
    * Factory for ConditionalRemoveAction
    *
    * @return a ConditionalRemoveAction
    */
    protected ConditionalRemoveAction instantiateConditionalRemoveAction()
        {
        return new ConditionalRemoveAction();
        }

    /**
    * Action support for a conditional remove().  The action performs a logical
    * locked remove if the entry is currently mapped to the assumed value, and
    * is expected to run while holding the segment-lock for the specified key.
    * If the Entry corresponding to the specified key is still necessary in the
    * map (e.g. for representing a lock state), the Entry may not be physically
    * removed, but rather converted to be synthetic.
    * <p>
    * The context object for a ConditionalRemoveAction is the assumed associated value.
    * <p>
    * The result of invoking a ConditionalRemoveAction is the previous value
    * associated with the specified key if it is successfully removed, or
    * <tt>NO_VALUE</tt> if the key is not mapped to the assumed value.  Note
    * that a synthetic Entry does not represent a key-value mapping, so
    * <tt>NO_VALUE</tt> is returned if a matching synthetic Entry is found.
    */
    protected class ConditionalRemoveAction
            extends SegmentedHashMap.RemoveAction
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object                   oKey,
                                  Object                   oContext,
                                  SegmentedHashMap.Entry[] aeBucket,
                                  int                      nBucket,
                                  SegmentedHashMap.Entry   entryPrev,
                                  SegmentedHashMap.Entry   entryCur)
            {
            // only perform the remove if the assumed mapping exists, otherwise
            LockableEntry entry = (LockableEntry) entryCur;
            if (oContext == NO_VALUE)
                {
                // conditional remove of any valid mapping
                Object oCurrent;
                while ((oCurrent = entry.getValueInternal()) != NO_VALUE)
                    {
                    if (entry.casValueInternal(oCurrent, NO_VALUE))
                        {
                        return super.invokeFound(
                            oKey, oContext, aeBucket, nBucket, entryPrev, entryCur);
                        }
                    }
                }
            else
                {
                // oContext is the assumed value
                if (entry.casValueInternal(oContext, NO_VALUE))
                    {
                    super.invokeFound(
                        oKey, oContext, aeBucket, nBucket, entryPrev, entryCur);
                    return oContext;
                    }
                }
            return NO_VALUE;
            }
        }


    // ----- inner class: LockAction ----------------------------------------

    /**
    * Factory for LockAction
    *
    * @return a LockAction
    */
    protected LockAction instantiateLockAction()
        {
        return new LockAction();
        }

    /**
    * Action support for lock().  This action attempts to lock the specified
    * key in this map, and is expected to run while holding the segment-lock
    * for the specified key.
    * <p>
    * The context object for a LockAction is the prospective lock-holder.
    * <p>
    * The result of invoking a LockAction is <tt>NO_VALUE</tt> if the key is
    * successfully locked, or the Entry object corresponding to the specified
    * key if the key could not be successfully locked.
    */
    protected class LockAction
            implements EntryAction
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object                   oKey,
                                  Object                   oContext,
                                  SegmentedHashMap.Entry[] aeBucket,
                                  int                      nBucket,
                                  SegmentedHashMap.Entry   entryPrev,
                                  SegmentedHashMap.Entry   entryCur)
            {
            // A LockableEntry was found for this key
            Object        oHolder = oContext;
            LockableEntry entry   = (LockableEntry) entryCur;
            if (!entry.isLocked() || entry.m_oLockHolder == oHolder)
                {
                // Either the entry is not locked or is already locked by this
                // lock-holder
                entry.lock(oHolder);
                return NO_VALUE;
                }
            return entry;
            }

        /**
        * {@inheritDoc}
        */
        public Object invokeNotFound(Object                   oKey,
                                     Object                   oContext,
                                     SegmentedHashMap.Entry[] aeBucket,
                                     int                      nBucket)
            {
            // No LockableEntry was found for this key; since we hold the
            // segment lock, we are now free to insert a new LockableEntry and
            // lock it.
            //
            // We can lock this without synchronization because we have held
            // the segment lock since it was added.
            ((LockableEntry) getInsertAction().invokeNotFound(
                oKey, NO_VALUE, aeBucket, nBucket)).lock(oContext);

            return NO_VALUE;
            }
        }


    // ----- inner class: UnlockAction --------------------------------------

    /**
    * Factory for UnlockAction
    *
    * @return an UnlockAction
    */
    protected UnlockAction instantiateUnlockAction()
        {
        return new UnlockAction();
        }

    /**
    * Action support for unlock().  This action attempts to lock the specified
    * key in this map, and is expected to run while holding the segment-lock
    * for the specified key.
    * <p>
    * The context object for an UnlockAction is the lock-holder that is
    * releasing the lock.
    * <p>
    * The result of invoking an UnlockAction is <tt>Boolean.TRUE</tt> if the
    * key is successfully unlocked, or <tt>Boolean.FALSE</tt> otherwise.
    */
    protected class UnlockAction
            extends EntryActionAdapter
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object                   oKey,
                                  Object                   oContext,
                                  SegmentedHashMap.Entry[] aeBucket,
                                  int                      nBucket,
                                  SegmentedHashMap.Entry   entryPrev,
                                  SegmentedHashMap.Entry   entryCur)
            {
            LockableEntry entry = (LockableEntry) entryCur;
            if (!entry.isLocked() || entry.m_oLockHolder != oContext)
                {
                // Entry exists, but is not locked or is held by a different
                // lock-holder
                return Boolean.FALSE;
                }

            if (entry.unlock())
                {
                // if the Entry was completely unlocked, we need to check to
                // see if we have waiters.
                synchronized (entry)
                    {
                    if (entry.isContended())
                        {
                        // notify a contending thread
                        entry.notify();
                        return Boolean.TRUE;
                        }
                    }

                if (entry.isSynthetic())
                    {
                    // If the entry is synthetic, unlocked, and uncontended,
                    // then physically remove it.
                    getRemoveAction().invokeFound(oKey,
                                                  oContext,
                                                  aeBucket,
                                                  nBucket,
                                                  entryPrev,
                                                  entry);
                    }
                }

            return Boolean.TRUE;
            }

        /**
        * {@inheritDoc}
        */
        public Object invokeNotFound(Object                   oKey,
                                     Object                   oContext,
                                     SegmentedHashMap.Entry[] aeBucket,
                                     int                      nBucket)
            {
            // Not locked
            return Boolean.FALSE;
            }
        }


    // ----- inner class: SizeAction ----------------------------------------

    /**
    * Factory for SizeAction
    *
    * @return a SizeAction
    */
    protected SizeAction instantiateSizeAction()
        {
        return new SizeAction();
        }

    /**
    * Action support for size().  The action is invoked on each Entry in the
    * map to determine the number of key-value mappings contained in the map.
    * SizeAction is not required to run while holding any segment-locks.
    * <p>
    * The context object for a SizeAction is an opaque context created by
    * <tt>instantiateContext</tt>.
    * <p>
    * The result of invoking a SizeAction is the number of key-value mappings
    * found in the map.
    */
    protected static class SizeAction
            extends EntryActionAdapter
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object                   oKey,
                                  Object                   oContext,
                                  SegmentedHashMap.Entry[] aeBucket,
                                  int                      nBucket,
                                  SegmentedHashMap.Entry   entryPrev,
                                  SegmentedHashMap.Entry   entryCur)
            {
            if (!entryCur.isSynthetic())
                {
                // increment the running count
                ++((SizeContext) oContext).m_cEntries;
                }
            return NO_VALUE;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isComplete(Object oContext)
            {
            // If we are only interested in whether or not the map is empty,
            // stop as soon as the first map entry is found.
            SizeContext sizeContext = (SizeContext) oContext;
            return sizeContext.m_fEmptyCheck && sizeContext.m_cEntries > 0;
            }

        /**
        * Return the number of Entry objects found while applying this action.
        *
        * @param oContext  the action context
        *
        * @return the number of Entry objects found
        */
        public int size(Object oContext)
            {
            return ((SizeContext) oContext).m_cEntries;
            }

        /**
        * Instantiate a context appropriate for applying SizeAction to count
        * the number of entries in the map.
        *
        * @param fEmptyCheck  if true, only test for emptiness
        *
        * @return a context to use with a SizeAction
        */
        public Object instantiateContext(boolean fEmptyCheck)
            {
            SizeContext oContext = new SizeContext();
            oContext.m_fEmptyCheck = fEmptyCheck;
            return oContext;
            }

        /**
        * Context for SizeAction.
        */
        private static class SizeContext
            {
            /**
            * Are we only interested in if the map is empty?
            */
            private boolean m_fEmptyCheck;

            /**
            * The number of map entries found.
            */
            private int     m_cEntries;
            }
        }

    // ----- inner class: TruncateAction -------------------------------------

    /**
     * Factory for TruncateAction
     *
     * @return a TruncateAction
     *
     * @since 12.2.1.4.21
     */
    protected TruncateAction instantiateTruncateAction()
        {
        return new TruncateAction();
        }

    /**
     * Action support for truncate() for a NearCache key/events control map.
     * This operation supports interrupting {@link LockableEntry#getLockHolder() lock holder thread} and unlocking {@link LockableEntry} <code>entryCur</code>.
     *
     * @since 12.2.1.4.21
     */
    protected class TruncateAction
            extends EntryActionAdapter
        {
        @Override
        public Object invokeFound(Object oKey,
                                  Object oContext,
                                  SegmentedHashMap.Entry[] aeBucket,
                                  int nBucket,
                                  SegmentedHashMap.Entry entryPrev,
                                  SegmentedHashMap.Entry entryCur)
            {
            LockableEntry entry      = (LockableEntry) entryCur;
            Object        lockHolder = entry.getLockHolder();

            if (lockHolder != null && lockHolder instanceof Thread)
                {
                try
                    {
                    Thread thread = (Thread) lockHolder;

                    thread.interrupt();
                    Logger.fine("TruncateAction: interrupted lock holder thread: " + thread.getName() + " holding lock on key: " + entry.getKey() +
                                " LockableEntry@" + Integer.toHexString(System.identityHashCode(entry)));

                    // the lock holder is interrupted - release the lock
                    //
                    // Note: the order is important; once we clear the lockholder,
                    //       concurrent threads may proceed to lock as we do not
                    //       hold the segment lock here
                    entry.m_cLock       = 0;
                    entry.m_oLockHolder = null;

                    synchronized (entry)
                        {
                        if (entry.isContended())
                            {
                            // notify a contending thread
                            entry.notify();
                            }
                        }
                    return Boolean.TRUE;
                    }
                catch (Throwable t)
                    {
                    // ignore
                    }
                }
            return Boolean.FALSE;
            }

        @Override
        public Object invokeNotFound(Object oKey,
                                     Object oContext,
                                     SegmentedHashMap.Entry[] aeBucket,
                                     int nBucket)
            {
            return Boolean.FALSE;
            }
        }

    // ----- inner class: LocksDumpAction ------------------------------

    /**
     * Factory for LocksDumpAction
     *
     * @return a LocksDumpAction
     *
     * @since 12.2.1.4.21
     */
    protected DumpHeldLocksAction instantiateDumpHeldLocksAction()
        {
        return new DumpHeldLocksAction();
        }

    /**
     * Action support for dumping held locks for a NearCache key/events control map.
     *
     * @since 12.2.1.4.21
     */
    protected class DumpHeldLocksAction
            extends EntryActionAdapter
        {
        @Override
        public Object invokeFound(Object oKey,
                                  Object oContext,
                                  SegmentedHashMap.Entry[] aeBucket,
                                  int nBucket,
                                  SegmentedHashMap.Entry entryPrev,
                                  SegmentedHashMap.Entry entryCur)
            {
            LockableEntry entry = (LockableEntry) entryCur;
            Object lockHolder = entry.getLockHolder();

            if (lockHolder != null && lockHolder instanceof Thread)
                {
                try
                    {
                    Thread thread = (Thread) lockHolder;

                    Logger.info("SegmentedConcurrentMap.dump: " + "LockableEntry@" + Integer.toHexString(System.identityHashCode(entry)) +
                                " key=" + entry.getKey() + " lock count=" + entry.m_cLock +
                                " owner: [" + thread + "] contend:" + entry.m_cContend + " isThreadAlive: " + isAlive(thread));
                    return Boolean.TRUE;
                    }
                catch (Throwable t)
                    {
                    // ignore
                    }
                }
            return Boolean.FALSE;
            }

        @Override
        public Object invokeNotFound(Object oKey,
                                     Object oContext,
                                     SegmentedHashMap.Entry[] aeBucket,
                                     int nBucket)
            {
            return Boolean.FALSE;
            }
        }

    // ----- inner class: LockableEntry -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected SegmentedHashMap.Entry instantiateEntry(Object oKey,
                                                      Object oValue,
                                                      int nHash)
        {
        return new LockableEntry(oKey, oValue, nHash);
        }

    /**
    * LockableEntry is an Entry that supports locking.
    *
    * See {@link com.tangosol.util.ConcurrentMap}
    */
    public class LockableEntry
            extends SegmentedHashMap.Entry
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct a LockableEntry for the given entry.
        *
        * @param oKey    key with which the specified value is to be associated
        * @param oValue  value to be associated with the specified key
        * @param nHash   the hashCode for the specified key
        */
        protected LockableEntry(Object oKey, Object oValue, int nHash)
            {
            super(oKey, oValue, nHash);
            }

        // ----- accessors --------------------------------------------------

        /**
        * Return the holder of this lockable entry, or null if this entry is not
        * locked.
        *
        * @return the lock holder, or null
        */
        public Object getLockHolder()
            {
            return m_oLockHolder;
            }

        /**
         * Return the {@link SegmentedConcurrentMap} containing this {@link LockableEntry Entry}.
         *
         * @return {@link SegmentedConcurrentMap} containing this {@link LockableEntry Entry}
         */
        public SegmentedConcurrentMap getSource()
            {
            return SegmentedConcurrentMap.this;
            }

        /**
        * Is there contention (a thread waiting) to lock this Entry?
        *
        * @return true iff another thread is contending for this Entry
        */
        public boolean isContended()
            {
            return m_cContend > 0;
            }

        // ----- SegmentedHashMap.Entry methods -----------------------------

        /**
        * {@inheritDoc}
        */
        protected Object setValueInternal(Object oValue)
            {
            Object                      oValueOld;
            AtomicReferenceFieldUpdater atomicUpdater = getAtomicUpdaterValue();
            do
                {
                oValueOld = m_atomicUpdaterValue.get(this);
                }
            while (!atomicUpdater.compareAndSet(this, oValueOld, oValue));
            return oValueOld;
            }

        /**
        * {@inheritDoc}
        */
        protected boolean isSynthetic()
            {
            return m_oValue == SegmentedHashMap.NO_VALUE;
            }

        // ----- LockableEntry methods --------------------------------------

        /**
        * Wait for this LockableEntry to be notified that it has been freed by
        * the previous lock-holder.  If the lock is being held by a thread and
        * that thread has died, release the lock.
        * <p>
        * Ensure the provided {@link ContentionObserver} is called before and
        * after this thread's wait for the lock-holder to release the lock.
        *
        * @param cWait     the number of milliseconds to wait for notification
        *                  to obtain a lock; pass zero to return immediately;
        *                  pass -1 to block the calling thread until the lock
        *                  obtained could be
        * @param observer  a ContentionObserver, or null, to be invoked both
        *                  before and after waiting for the lock-holder to
        *                  release the lock
        *
        * @return updated wait time
        */
        protected synchronized long waitForNotify(long cWait, ContentionObserver observer)
            {
            Thread thdCurrent = Thread.currentThread();
            try
                {
                ++m_cContend;
                if (observer != null)
                    {
                    observer.onContend(thdCurrent, this);
                    }

                while (cWait != 0 && isLocked())
                    {
                    cWait = waitForNotify(cWait);
                    }
                }
            finally
                {
                --m_cContend;
                if (observer != null)
                    {
                    observer.onUncontend(thdCurrent, this);
                    }
                }
            return cWait;
            }

        /**
        * Wait for this LockableEntry to be notified that it has been freed by
        * the previous lock-holder.  If the lock is being held by a thread and
        * that thread has died, release the lock.
        * <p>
        * Note: caller of this method is expected to hold a synchronization
        *       monitor for this LockableEntry object while making this call.
        *
        * @param cWait  the number of milliseconds to wait for notification to
        *               obtain a lock; pass zero to return immediately; pass
        *               -1 to block the calling thread until the lock could be
        *               obtained
        *
        * @return updated wait time
        */
        protected long waitForNotify(long cWait)
            {
            long lTime = Base.getSafeTimeMillis();
            try
                {
                // In case of thread death of the lock holder, do not wait
                // forever, because thread death becomes an implicit unlock,
                // and this thread needs to then wake up and take the lock
                long cMaxWait = 1000L;
                long cMillis =
                        (cWait <= 0L || cWait > cMaxWait) ? cMaxWait : cWait;

                Blocking.wait(this, cMillis);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e, "Lock request interrupted");
                }

            if (cWait >= 0)
                {
                // reduce the waiting time by the elapsed amount
                cWait -= Base.getSafeTimeMillis() - lTime;
                cWait = Math.max(0, cWait);
                }

            // If the lock-holder is a thread, check that it is alive
            Object oHolder = m_oLockHolder;
            if (oHolder instanceof Thread && !((Thread) oHolder).isAlive())
                {
                // the holder is dead - release the lock
                //
                // Note: the order is important; once we clear the lockholder,
                //       concurrent threads may proceed to lock as we do not
                //       hold the segment lock here
                m_cLock       = 0;
                m_oLockHolder = null;
                }

            return cWait;
            }

        /**
        * Lock this entry for the specified lock holder.
        * <p>
        * Note: caller of this method is expected to have locked the segment
        *       for this Entry object
        *
        * @param oHolder  the holder of this lock
        */
        protected void lock(Object oHolder)
            {
            m_oLockHolder = oHolder;
            m_cLock++;
            }

        /**
        * Unlock this entry.
        * <p>
        * Note: caller of this method is expected to have locked the segment
        *       for this Entry object
        *
        * @return true iff the Entry is completely unlocked
        */
        protected boolean unlock()
            {
            if (--m_cLock == 0)
                {
                m_oLockHolder = null;
                return true;
                }
            return false;
            }

        /**
        * Set the value of this entry to the specified value iff the current
        * value matches the assumed value.
        *
        * @param oValueAssume  the assumed value
        * @param oValue        the new value
        *
        * @return true iff the value changed
        */
        protected boolean casValueInternal(Object oValueAssume, Object oValue)
            {
            return getAtomicUpdaterValue().compareAndSet(this, oValueAssume, oValue);
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return super.toString() +
                (isLocked() ? (", locked by " + m_oLockHolder) : "");
            }

        /**
        * Is this entry Locked?
        *
        * @return true iff this entry is locked
        */
        protected boolean isLocked()
            {
            return m_oLockHolder != null;
            }

        /**
        * Return an AtomicReferenceFieldUpdater to use to update the value of
        * the Entry.
        * <p>
        * Note: this must be delared here to work around access protection.
        */
        private AtomicReferenceFieldUpdater getAtomicUpdaterValue()
            {
            AtomicReferenceFieldUpdater atomicUpdater = m_atomicUpdaterValue;
            if (atomicUpdater == null)
                {
                atomicUpdater = AtomicReferenceFieldUpdater.newUpdater(
                    SegmentedHashMap.Entry.class, Object.class, "m_oValue");
                m_atomicUpdaterValue = atomicUpdater;
                }
            return atomicUpdater;
            }

        // ----- data members -----------------------------------------------

        /**
        * The lock holder object.
        */
        protected volatile Object m_oLockHolder = null;

        /**
        * The lock count (number of times the "lock()" was called by the
        * locking thread).
        */
        protected volatile short m_cLock;

        /**
        * The number of threads that are waiting to lock this Entry.
        */
        protected volatile short m_cContend;
        }


    // ----- inner interface: ContentionObserver ----------------------------

    /**
    * ContentionObserver is used to observe the contention lock-related actions
    * performed on the concurrent map.
    */
    public interface ContentionObserver
        {
        /**
        * Called when the specified lock holder begins contending for the
        * specified LockableEntry.
        *
        * @param oContender  the contending lock holder
        * @param entry       the entry being contended for
        */
        public void onContend(Object oContender, LockableEntry entry);

        /**
        * Called when the specified lock holder stops contending for the
        * specified LockableEntry.
        *
        * @param oContender  the previously contending lock holder
        * @param entry       the entry no longer being contended for
        */
        public void onUncontend(Object oContender, LockableEntry entry);
        }

    /**
     * For diagnostic use only, detect Daemon pool thread is no longer active.
     * This method is only used in dumpHeldLocks() to automate checking if a LockableEntry is pointing
     * to a DaemonThread pool that is in waiting state.  This method does not account
     * for any other ThreadPool implementation except Coherences.
     *
     * @param t  the thread
     * @return true iff the stack trace does not include Daemon.wait().
     *
     * @since 12.2.1.4.21
     */
    static public boolean isAlive(Thread t)
        {
        if (!t.isAlive())
            {
            return false;
            }

        StackTraceElement[] arElement = t.getStackTrace();

        if (!stream(arElement).anyMatch((e)-> e.getClassName().endsWith(".Daemon") && e.getMethodName().compareTo("run") == 0))
            {
            // not a Daemon Thread
            return true;
            }

        return !stream(arElement).anyMatch((e)-> e.getClassName().endsWith(".Daemon") && e.getMethodName().compareTo("onWait") == 0);
        }

    // ----- data members ---------------------------------------------------

    /**
    * AtomicUpdater for the entry value.
    */
    protected static AtomicReferenceFieldUpdater m_atomicUpdaterValue;

    /**
    * The Gate controlling LOCK_ALL access for this map.
    */
    protected Gate m_gateLockAll = new WrapperReentrantGate();

    /**
    * The action for lock() support.
    */
    protected LockAction m_actionLock;

    /**
    * The action for unlock() support.
    */
    protected UnlockAction m_actionUnlock;

    /**
    * The singleton action for size support.
    */
    protected SizeAction m_actionSize;

    /**
    * The singleton action for conditional remove.
    */
    protected ConditionalRemoveAction m_actionConditionalRemove;

    /**
     * The singleton action for truncate.
     *
     * @since 12.2.1.4.21
     */
    protected TruncateAction m_actionTruncate;

    /**
     * The singleton action dump held locks.
     *
     * @since 12.2.1.4.21
     */
    protected DumpHeldLocksAction m_actionDumpHeldLocks;

    /**
    * The ContentionObserver; may be null.
    */
    protected ContentionObserver m_contentionObserver;
    }

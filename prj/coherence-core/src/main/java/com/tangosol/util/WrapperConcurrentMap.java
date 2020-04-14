/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.oracle.coherence.common.base.Blocking;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* A simple implementation of ConcurrentMap interface built as a
* wrapper around any Map implementation. As a subclass of
* WrapperObservableMap, it naturally implements the ObservableMap
* interface and provides an implementation of CacheStatistics interface.
*
* @author gg 2002.04.02
*/
public class WrapperConcurrentMap<K, V>
        extends WrapperObservableMap<K, V>
        implements ConcurrentMap<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ConcurrentMap wrapper based on the specified map with
    * locking enforced for put, remove and clear operations.
    * <p>
    * <b>Note:</b> it is assumed that while the WrapperConcurrentMap exists,
    * there is no direct manipulation with the content of the wrapped map.
    *
    * @param map the Map that will be wrapped by this WrapperConcurrentMap
    */
    public WrapperConcurrentMap(Map<K, V> map)
        {
        this(map, true, -1L);
        }

    /**
    * Construct a ConcurrentMap wrapper based on the specified map.
    * <p>
    * <b>Note:</b> it is assumed that while the WrapperConcurrentMap exists,
    * there is no direct manipulation with the content of the wrapped map.
    *
    * @param map              the Map that will be wrapped by this
    *                         WrapperConcurrentMap
    * @param fEnforceLocking  if true the locking is enforced for put, remove
    *                         and clear operations; otherwise a client is
    *                         responsible for calling lock and unlock explicitly
    * @param cWaitMillis      if locking enforcement is required then this
    *                         parameter specifies the number of milliseconds to
    *                         continue trying to obtain a lock; pass -1 to block
    *                         the calling thread until the lock could be obtained
    */
    public WrapperConcurrentMap(Map<K, V> map, boolean fEnforceLocking,
            long cWaitMillis)
        {
        super(map);

        m_fEnforceLocking = fEnforceLocking;
        m_cWaitMillis     = cWaitMillis;
        }

    // ----- ConcurrentMap interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean lock(Object oKey, long cWait)
        {
        Map<K, V> map = getMap();
        if (map instanceof ConcurrentMap)
            {
            return ((ConcurrentMap<K, V>) map).lock(oKey, cWait);
            }

        SafeHashMap mapLock = m_mapLock;
        Gate        gateMap = m_gateMap;

        if (oKey == LOCK_ALL)
            {
            return gateMap.close(cWait);
            }

        if (!gateMap.enter(cWait))
            {
            return false;
            }

        boolean fSuccess = false;
        Lock    lock     = null;
        try
            {
            while (true)
                {
                synchronized (mapLock)
                    {
                    lock = (Lock) mapLock.get(oKey);
                    if (lock == null)
                        {
                        lock = instantiateLock(oKey);
                        lock.assign(0); // this will succeed without blocking
                        mapLock.put(oKey, lock);
                        return fSuccess = true;
                        }
                    else
                        {
                        // perform a quick, non-blocking check to see if the
                        // current thread already owns the lock
                        if (lock.isOwnedByCaller())
                            {
                            lock.assign(0); // this will succeed without blocking
                            return fSuccess = true;
                            }
                        }
                    }

                synchronized (lock)
                    {
                    // make sure the lock didn't just get removed
                    if (lock == mapLock.get(oKey))
                        {
                        return fSuccess = lock.assign(cWait);
                        }
                    }
                }
            }
        finally
            {
            if (!fSuccess)
                {
                if (lock != null && lock.isDiscardable())
                    {
                    mapLock.remove(oKey);
                    }
                gateMap.exit();
                }
            }
        }

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
    public boolean unlock(Object oKey)
        {
        Map<K, V> map = getMap();
        if (map instanceof ConcurrentMap)
            {
            return ((ConcurrentMap<K, V>) map).unlock(oKey);
            }

        SafeHashMap mapLock = m_mapLock;
        Gate        gateMap = m_gateMap;

        if (oKey == LOCK_ALL)
            {
            try
                {
                gateMap.open();
                return true;
                }
            catch (IllegalMonitorStateException e)
                {
                return false;
                }
            }

        boolean fReleased = true;
        boolean fExitGate = false;
        while (true)
            {
            Lock lock = (Lock) mapLock.get(oKey);
            if (lock == null)
                {
                break;
                }

            synchronized (lock)
                {
                if (mapLock.get(oKey) == lock)
                    {
                    fExitGate = !lock.isDirty();
                    fReleased = lock.release();
                    if (lock.isDiscardable())
                        {
                        mapLock.remove(oKey);
                        }
                    break;
                    }
                }
            }

        if (fExitGate)
            {
            // if we found the key in the lock-map, and it is not
            // dirty (owned by another thread) then we should exit the
            // thread-gate
            try
                {
                gateMap.exit();
                }
            catch (IllegalStateException e)
                {
                }
            }

        return fReleased;
        }

    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * If lock enforcement is required an attempt will be made to lock the
    * entire map using the {@link ConcurrentMap#LOCK_ALL} object.
    * <p>
    * <b>Note:</b> if this operation fails due to a
    * ConcurrentModificationException, then any subset of the current
    * mappings could still remain in the map.
    *
    * @throws ConcurrentModificationException if any entry is locked
    *         by another thread
    */
    @Override
    public void clear()
        {
        boolean fForceLock = isLockingEnforced();

        if (!fForceLock || lock(LOCK_ALL, getWaitMillis()))
            {
            try
                {
                super.clear();
                }
            finally
                {
                if (fForceLock)
                    {
                    unlock(LOCK_ALL);
                    }
                }
            }
        else
            {
            throw new ConcurrentModificationException("clear");
            }
        }

    /**
    * {@inheritDoc}
    *
    * @throws ConcurrentModificationException if the entry is locked
    *         by another thread
    */
    @Override
    public V put(K oKey, V oValue)
        {
        boolean fForceLock = isLockingEnforced();

        if (!fForceLock || lock(oKey, getWaitMillis()))
            {
            try
                {
                return super.put(oKey, oValue);
                }
            finally
                {
                if (fForceLock)
                    {
                    unlock(oKey);
                    }
                }
            }
        else
            {
            throw new ConcurrentModificationException("(thread="
                    + Thread.currentThread() + ") " + getLockDescription(oKey));
            }
        }

    /**
    * {@inheritDoc}
    *
    * @throws ConcurrentModificationException if the entry is locked
    *         by another thread
    */
    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        boolean fForceLock = isLockingEnforced();

        if (fForceLock)
            {
            Set setLocked = new HashSet();
            try
                {
                for (Iterator iter = map.keySet().iterator(); iter.hasNext();)
                    {
                    Object oKey = iter.next();
                    if (lock(oKey, getWaitMillis()))
                        {
                        setLocked.add(oKey);
                        }
                    else
                        {
                        throw new ConcurrentModificationException("(thread="
                                + Thread.currentThread() + ") "
                                + getLockDescription(oKey));
                        }
                    }

                super.putAll(map);
                }
            finally
                {
                for (Iterator iter = setLocked.iterator(); iter.hasNext();)
                    {
                    unlock(iter.next());
                    }
                }
            }
        else
            {
            super.putAll(map);
            }
        }

    /**
    * {@inheritDoc}
    *
    * @throws ConcurrentModificationException if the entry is locked
    *         by another thread
    */
    @Override
    public V remove(Object oKey)
        {
        boolean fForceLock = isLockingEnforced();

        if (!fForceLock || lock(oKey, getWaitMillis()))
            {
            try
                {
                return super.remove(oKey);
                }
            finally
                {
                if (fForceLock)
                    {
                    unlock(oKey);
                    }
                }
            }
        else
            {
            throw new ConcurrentModificationException("(thread="
                    + Thread.currentThread() + ") " + getLockDescription(oKey));
            }
        }

    // ----- AbstractKeySetBasedMap methods ---------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected boolean isInternalKeySetIteratorMutable()
        {
        // this wrapper needs to do the mutations itself if it is
        // responsible for doing operation-level locking
        return super.isInternalKeySetIteratorMutable() && !isLockingEnforced();
        }

    /**
    * {@inheritDoc}
    *
    * @throws ConcurrentModificationException if the entry is locked
    *         by another thread
    */
    @Override
    protected boolean removeBlind(Object oKey)
        {
        boolean fForceLock = isLockingEnforced();

        if (!fForceLock || lock(oKey, getWaitMillis()))
            {
            try
                {
                return super.removeBlind(oKey);
                }
            finally
                {
                if (fForceLock)
                    {
                    unlock(oKey);
                    }
                }
            }
        else
            {
            throw new ConcurrentModificationException("(thread="
                    + Thread.currentThread() + ") " + getLockDescription(oKey));
            }
        }

    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public String toString()
        {
        return "WrapperConcurrentMap {" + getDescription() + "}";
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the flag indicating whether or not the locking is enforced for
    * put, remove and clear operations.
    *
    * @return true if locking is enforced; false otherwise
    */
    public boolean isLockingEnforced()
        {
        return m_fEnforceLocking;
        }

    /**
    * Set the flag indicating whether or not the locking is enforced for
    * put, remove and clear operations.
    *
    * @param fEnforce  pass true to enforce locking; false otherwise
    */
    public void setLockingEnforced(boolean fEnforce)
        {
        m_fEnforceLocking = fEnforce;
        }

    /**
    * Return the number of milliseconds to continue trying to obtain a lock
    * in case when the locking is enforced.
    *
    * @return the wait time in milliseconds
    */
    public long getWaitMillis()
        {
        return m_cWaitMillis;
        }

    /**
    * Specify the number of milliseconds to continue trying to obtain a lock
    * in case when the locking is enforced.
    *
    * @param cWaitMillis  the wait time in milliseconds
    */
    public void setWaitMillis(long cWaitMillis)
        {
        m_cWaitMillis = cWaitMillis;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected String getDescription()
        {
        return super.getDescription() + ", LockingEnforced="
                + isLockingEnforced() + ", WaitMillis=" + getWaitMillis()
                + ", ThreadGate=" + m_gateMap + ", Locks={" + m_mapLock + "}";
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Make a human-readable description of the information kept about the
    * passed key.
    *
    * @param oKey  the key
    *
    * @return the information known about the passed key
    */
    public String getLockDescription(Object oKey)
        {
        return "key=\"" + oKey + "\", lock=" + m_mapLock.get(oKey);
        }

    // ----- inner class: Lock ---------------------------------------------

    /**
    * Factory pattern.
    *
    * @param oKey  the key
    *
    * @return a new instance of the Lock class (or a subclass thereof)
    */
    protected Lock instantiateLock(Object oKey)
        {
        return new Lock();
        }

    /**
    * A lock object.
    */
    protected static class Lock extends Base
        {
        /**
        * Construct a new Lock object.
        */
        protected Lock()
            {
            }

        /**
        * Assign the ownership of this Lock to the calling thread.
        * <p>
        * Note: caller of this method is expected to hold a synchronization
        * monitor for the Lock object while making this call.
        *
        * @param cWait the number of milliseconds to continue trying to obtain
        *              a lock; pass zero to return immediately; pass -1 to block
        *              the calling thread until the lock could be obtained
        * @return true if lock was successful; false otherwise
        */
        protected boolean assign(long cWait)
            {
            while (isDirty())
                {
                if (cWait == 0)
                    {
                    return false;
                    }
                cWait = waitForNotify(cWait);
                }

            int cLock = m_cLock + 1;
            if (cLock == 1)
                {
                m_thread = Thread.currentThread();
                }
            else if (cLock == Short.MAX_VALUE)
                {
                throw new RuntimeException("Lock count overflow: " + this);
                }
            m_cLock = (short) cLock;

            return true;
            }

        /**
        * Wait for a Lock release notification.
        * <p>
        * Note: caller of this method is expected to hold a synchronization
        * monitor for the Lock object while making this call.
        *
        * @param cWait the number of milliseconds to continue waiting;
        *              pass -1 to block the calling thread indefinitely
        *
        * @return updated wait time.
        */
        protected long waitForNotify(long cWait)
            {
            long lTime = getSafeTimeMillis();
            try
                {
                m_cBlock++;

                // in case of thread death of the lock holder, do not wait forever,
                // because thread death becomes an implicit unlock, and this thread
                // needs to then wake up and take the lock
                final long cMaxWait = 1000L;
                long cMillis = (cWait <= 0L || cWait > cMaxWait) ? cMaxWait
                        : cWait;

                Blocking.wait(this, cMillis);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw ensureRuntimeException(e, "Lock request interrupted");
                }
            finally
                {
                m_cBlock--;
                }

            if (cWait > 0)
                {
                // reduce the waiting time by the elapsed amount
                cWait -= getSafeTimeMillis() - lTime;
                cWait = Math.max(0, cWait);
                }

            return cWait;
            }

        /**
        * Release this Lock.
        * <p>
        * Note: caller of this method is expected to hold a synchronization
        * monitor for the Lock object while making this call.
        *
        * @return true if unlock is successful; false if the entry remained locked
        */
        protected boolean release()
            {
            if (isDirty())
                {
                return false;
                }

            int cLock = m_cLock - 1;

            if (cLock == 0)
                {
                m_thread = null;
                }
            else if (cLock < 0)
                {
                cLock = 0;
                }
            m_cLock = (short) cLock;

            if (cLock == 0)
                {
                if (m_cBlock > 0)
                    {
                    notify();
                    }
                return true;
                }
            else
                {
                return false;
                }
            }

        /**
        * Checks whether or not this Lock object is held by another thread.
        * <p>
        * Note: caller of this method is expected to hold a synchronization
        * monitor for the Lock object while making this call.
        *
        * @return true if the Lock is held by another thread; false otherwise
        */
        protected boolean isDirty()
            {
            Thread threadHolder = m_thread;
            Thread threadCurrent = Thread.currentThread();

            if (threadHolder != null && threadHolder != threadCurrent)
                {
                // make sure that the holder thread is still alive
                if (threadHolder.isAlive())
                    {
                    return true;
                    }

                // the holder is dead - release the lock
                m_thread = null;
                m_cLock = 0;

                if (m_cBlock > 0)
                    {
                    notify();
                    }
                }
            return false;
            }

        /**
        * Checks whether or not this Lock object is held by the calling thread.
        * <p>
        * Note: unlike other methods of this class, the caller of this method
        * is <i>not</i> required to hold a synchronization monitor for the Lock
        * object while making this call.
        *
        * @return true if the Lock is held by the calling thread; false
        *         otherwise
        */
        protected boolean isOwnedByCaller()
            {
            return m_thread == Thread.currentThread();
            }

        /**
        * Checks whether or not this Lock object is discardable.
        * <p>
        * Note: caller of this method is expected to hold a synchronization
        * monitor for the Lock object while making this call.
        *
        * @return true if the Lock is discardable; false otherwise
        */
        protected boolean isDiscardable()
            {
            return m_cLock == 0 && m_cBlock == 0;
            }

        /**
        * Return the Thread object holding this Lock.
        *
        * @return the Thread object holding this Lock.
        */
        protected Thread getLockThread()
            {
            return m_thread;
            }

        /**
        * Return the lock count.
        *
        * @return the lock count
        */
        protected int getLockCount()
            {
            return m_cLock;
            }

        /**
        * Return the blocked threads count.
        *
        * @return the blocked threads count
        */
        protected int getBlockCount()
            {
            return m_cBlock;
            }

        /**
        * Return a human readable description of the Lock type.
        *
        * @return a human readable description of the Lock type
        */
        protected String getLockTypeDescription()
            {
            return "Lock";
            }

        /**
        * Return a human readable description of the Lock.
        *
        * @return a human readable description of the Lock
        */
        public String toString()
            {
            return getLockTypeDescription() + "[" + m_thread + ", cnt="
                    + m_cLock + ", block=" + m_cBlock + ']';
            }

        /**
        * The Thread object holding a lock for this entry.
        */
        private Thread m_thread;

        /**
        * The lock count (number of times the "assign" was called by
        * the locking thread).
        */
        private short  m_cLock;

        /**
        * The number of threads waiting on this Lock to be released.
        */
        private short  m_cBlock;
        }

    // ----- data fields ----------------------------------------------------

    /**
    * Flag indicating whether or not the locking is enforced for put, remove
    * and clear operations.
    */
    protected boolean           m_fEnforceLocking;

    /**
    * The number of milliseconds to continue trying to obtain a lock
    * in case when the locking is enforced.
    */
    protected long              m_cWaitMillis;

    /**
    * The map containing all the locks.
    */
    protected final SafeHashMap m_mapLock = new SafeHashMap();

    /**
    * The ThreadGate object for the entire map.
    */
    protected final Gate        m_gateMap = new WrapperReentrantGate();
    }

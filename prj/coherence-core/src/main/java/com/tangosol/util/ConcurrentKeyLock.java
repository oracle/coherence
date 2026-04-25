/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@link ConcurrentMap} implementation backed by a {@link ConcurrentHashMap}
 * that provides per-key reentrant locking without intrinsic monitors.
 * <p>
 * The map operations are delegated to an internal {@link ConcurrentHashMap};
 * the resource-control-map role only relies on the lock API.
 *
 * @param <K>  the key type
 * @param <V>  the value type
 *
 * @author Aleks Seovic  2026.04.10
 * @since 26.04
 */
public class ConcurrentKeyLock<K, V>
        extends AbstractMap<K, V>
        implements ConcurrentMap<K, V>
    {
    // ----- constructors -------------------------------------------------

    /**
     * Construct an unlocked {@link ConcurrentKeyLock}.
     */
    public ConcurrentKeyLock()
        {
        this(null);
        }

    /**
     * Construct an unlocked {@link ConcurrentKeyLock}.
     *
     * @param observer  the optional contention observer
     */
    public ConcurrentKeyLock(ContentionObserver<K> observer)
        {
        f_observer = observer;
        }


    // ----- ConcurrentMap interface --------------------------------------

    @Override
    public boolean lock(Object oKey)
        {
        return lock(oKey, 0L);
        }

    @Override
    @SuppressWarnings("unchecked")
    public boolean lock(Object oKey, long cWait)
        {
        if (LOCK_ALL == oKey)
            {
            return lockAll(cWait);
            }

        K    key      = (K) oKey;
        long ldtLimit = cWait > 0L ? System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(cWait) : 0L;

        if (!acquireReadLock(cWait, ldtLimit))
            {
            return false;
            }

        LockEntry entry     = retainLockEntry(key);
        boolean   fAcquired = false;

        try
            {
            if (entry.lock.tryLock())
                {
                fAcquired = true;
                return true;
                }

            if (cWait == 0L)
                {
                return false;
                }

            long cRemaining = remainingNanos(cWait, ldtLimit);
            if (cWait > 0L && cRemaining <= 0L)
                {
                return false;
                }

            Thread    thread = Thread.currentThread();
            LockState state  = new LockState(key, entry);

            entry.waiters.incrementAndGet();
            try
                {
                ContentionObserver<K> observer = f_observer;
                if (observer != null)
                    {
                    observer.onContend(thread, state);
                    }

                if (cWait < 0L)
                    {
                    entry.lock.lockInterruptibly();
                    fAcquired = true;
                    return true;
                    }

                cRemaining = remainingNanos(cWait, ldtLimit);
                if (cRemaining <= 0L)
                    {
                    return false;
                    }

                fAcquired = entry.lock.tryLock(cRemaining, TimeUnit.NANOSECONDS);
                return fAcquired;
                }
            finally
                {
                ContentionObserver<K> observer = f_observer;
                if (observer != null)
                    {
                    observer.onUncontend(thread, state);
                    }

                entry.waiters.decrementAndGet();
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e, "Lock request interrupted");
            }
        finally
            {
            if (!fAcquired)
                {
                releaseLockEntry(key, entry);
                releaseReadLock();
                }
            }
        }

    @Override
    @SuppressWarnings("unchecked")
    public boolean unlock(Object oKey)
        {
        if (LOCK_ALL == oKey)
            {
            return unlockAll();
            }

        K         key   = (K) oKey;
        LockEntry entry = f_mapLocks.get(key);
        if (entry == null || !entry.lock.isHeldByCurrentThread())
            {
            return false;
            }

        entry.lock.unlock();
        releaseLockEntry(key, entry);
        releaseReadLock();

        return true;
        }


    // ----- Map interface -------------------------------------------------

    @Override
    public Set<Entry<K, V>> entrySet()
        {
        return f_mapValues.entrySet();
        }

    @Override
    public V put(K key, V value)
        {
        return f_mapValues.put(key, value);
        }

    @Override
    public V get(Object key)
        {
        return f_mapValues.get(key);
        }

    @Override
    public V remove(Object key)
        {
        return f_mapValues.remove(key);
        }

    @Override
    public boolean containsKey(Object key)
        {
        return f_mapValues.containsKey(key);
        }

    @Override
    public boolean containsValue(Object value)
        {
        return f_mapValues.containsValue(value);
        }

    @Override
    public int size()
        {
        return f_mapValues.size();
        }

    @Override
    public boolean isEmpty()
        {
        return f_mapValues.isEmpty();
        }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        f_mapValues.putAll(map);
        }

    @Override
    public void clear()
        {
        f_mapValues.clear();
        }


    // ----- accessors -----------------------------------------------------

    /**
     * Return the number of live lock entries.
     *
     * @return the number of live lock entries
     */
    int getLockEntryCount()
        {
        return f_mapLocks.size();
        }


    // ----- helper methods ------------------------------------------------

    /**
     * Attempt to acquire the global write lock.
     *
     * @param cWait  the wait timeout in millis
     *
     * @return {@code true} if the write lock was acquired
     *
     * @implNote Callers must not attempt to upgrade from a held per-key lock
     *           to {@link ConcurrentMap#LOCK_ALL} on the same thread; the
     *           underlying {@link ReentrantReadWriteLock} does not support
     *           read-to-write upgrades.
     */
    protected boolean lockAll(long cWait)
        {
        try
            {
            if (cWait == 0L)
                {
                return f_lockAll.writeLock().tryLock();
                }

            if (cWait < 0L)
                {
                f_lockAll.writeLock().lockInterruptibly();
                return true;
                }

            return f_lockAll.writeLock().tryLock(cWait, TimeUnit.MILLISECONDS);
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e, "Lock request interrupted");
            }
        }

    /**
     * Release the global write lock.
     *
     * @return {@code true} if the current thread held the global write lock
     */
    protected boolean unlockAll()
        {
        if (!f_lockAll.isWriteLockedByCurrentThread())
            {
            return false;
            }

        f_lockAll.writeLock().unlock();
        return true;
        }

    /**
     * Acquire the global read lock.
     *
     * @param cWait     the wait timeout in millis
     * @param ldtLimit  the absolute deadline in nanos if timed
     *
     * @return {@code true} if the read lock was acquired
     */
    protected boolean acquireReadLock(long cWait, long ldtLimit)
        {
        ReadLockHold hold = f_tloReadLockHold.get();
        if (hold.count > 0)
            {
            ++hold.count;
            return true;
            }

        try
            {
            boolean fLocked;
            if (cWait == 0L)
                {
                fLocked = f_lockAll.readLock().tryLock();
                }
            else if (cWait < 0L)
                {
                f_lockAll.readLock().lockInterruptibly();
                fLocked = true;
                }
            else
                {
                long cRemaining = remainingNanos(cWait, ldtLimit);
                fLocked = cRemaining > 0L
                        && f_lockAll.readLock().tryLock(cRemaining, TimeUnit.NANOSECONDS);
                }

            if (fLocked)
                {
                hold.count = 1;
                }
            else
                {
                f_tloReadLockHold.remove();
                }
            return fLocked;
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw Base.ensureRuntimeException(e, "Lock request interrupted");
            }
        }

    /**
     * Release one per-key-lock hold against the global read lock.
     */
    protected void releaseReadLock()
        {
        ReadLockHold hold = f_tloReadLockHold.get();
        long         cHold = hold.count;

        if (cHold <= 0L)
            {
            f_tloReadLockHold.remove();
            throw new IllegalMonitorStateException("Read lock not held by current thread");
            }

        if (cHold == 1L)
            {
            f_tloReadLockHold.remove();
            f_lockAll.readLock().unlock();
            }
        else
            {
            hold.count = cHold - 1L;
            }
        }

    /**
     * Return the remaining time in nanos.
     *
     * @param cWait     the original wait in millis
     * @param ldtLimit  the absolute deadline in nanos
     *
     * @return the remaining time in nanos
     */
    protected long remainingNanos(long cWait, long ldtLimit)
        {
        return cWait < 0L ? Long.MAX_VALUE : ldtLimit - System.nanoTime();
        }

    /**
     * Retain the lock entry for the specified key.
     *
     * @param key  the key
     *
     * @return the retained lock entry
     */
    protected LockEntry retainLockEntry(K key)
        {
        return f_mapLocks.compute(key, (ignored, entry) ->
            {
            LockEntry entryNew = entry == null ? new LockEntry() : entry;
            entryNew.refs.incrementAndGet();
            return entryNew;
            });
        }

    /**
     * Release the retained reference for the specified key.
     *
     * @param key    the key
     * @param entry  the retained lock entry
     */
    protected void releaseLockEntry(K key, LockEntry entry)
        {
        f_mapLocks.computeIfPresent(key, (ignored, entryCur) ->
            {
            if (entryCur != entry)
                {
                return entryCur;
                }

            return entryCur.refs.decrementAndGet() == 0 ? null : entryCur;
            });
        }


    // ----- inner class: LockState ---------------------------------------

    /**
     * Live view of a contended key lock.
     */
    public class LockState
        {
        /**
         * Construct the lock state.
         *
         * @param key    the key
         * @param entry  the lock entry
         */
        protected LockState(K key, LockEntry entry)
            {
            f_key   = key;
            f_entry = entry;
            }

        /**
         * Return the contended key.
         *
         * @return the contended key
         */
        public K getKey()
            {
            return f_key;
            }

        /**
         * Return the current lock holder thread.
         *
         * @return the current lock holder thread, or {@code null}
         */
        public Thread getLockHolder()
            {
            return f_entry.lock.getOwnerThread();
            }

        /**
         * Return {@code true} iff some thread is contending for the key.
         *
         * @return {@code true} iff some thread is contending for the key
         */
        public boolean isContended()
            {
            return f_entry.waiters.get() > 0;
            }

        /**
         * Return the source map.
         *
         * @return the source map
         */
        public ConcurrentKeyLock<K, V> getSource()
            {
            return ConcurrentKeyLock.this;
            }

        @Override
        public String toString()
            {
            return "LockState{key=" + f_key + ", holder=" + getLockHolder()
                    + ", contended=" + isContended() + '}';
            }

        /**
         * The contended key.
         */
        private final K f_key;

        /**
         * The live lock entry.
         */
        private final LockEntry f_entry;
        }


    // ----- inner interface: ContentionObserver --------------------------

    /**
     * Observe contention on key locks.
     *
     * @param <K>  the key type
     */
    public interface ContentionObserver<K>
        {
        /**
         * Called when the specified thread begins contending for a key lock.
         *
         * @param thread  the contending thread
         * @param state   the live contention state
         */
        void onContend(Thread thread, ConcurrentKeyLock<K, ?>.LockState state);

        /**
         * Called when the specified thread stops contending for a key lock.
         *
         * @param thread  the contending thread
         * @param state   the live contention state
         */
        void onUncontend(Thread thread, ConcurrentKeyLock<K, ?>.LockState state);
        }


    // ----- inner class: LockEntry ---------------------------------------

    /**
     * State for a single key lock.
     */
    protected static class LockEntry
        {
        /**
         * The key lock.
         */
        protected final OwnedReentrantLock lock = new OwnedReentrantLock();

        /**
         * The number of retained references to the lock entry.
         */
        protected final AtomicInteger refs = new AtomicInteger();

        /**
         * The number of contending threads.
         */
        protected final AtomicInteger waiters = new AtomicInteger();
        }


    // ----- inner class: ReadLockHold ---------------------------------------

    /**
     * The current thread's global read-lock hold count.
     */
    protected static class ReadLockHold
        {
        /**
         * The number of per-key locks held by the current thread.
         */
        protected long count;
        }


    // ----- inner class: OwnedReentrantLock ------------------------------

    /**
     * A {@link ReentrantLock} that exposes its owner thread.
     */
    protected static class OwnedReentrantLock
            extends ReentrantLock
        {
        /**
         * Return the owner thread.
         *
         * @return the owner thread, or {@code null}
         */
        protected Thread getOwnerThread()
            {
            return getOwner();
            }
        }


    // ----- data members --------------------------------------------------

    /**
     * Map data delegated to an internal {@link ConcurrentHashMap}.
     */
    protected final ConcurrentHashMap<K, V> f_mapValues = new ConcurrentHashMap<>();

    /**
     * Per-key lock entries.
     */
    protected final ConcurrentHashMap<K, LockEntry> f_mapLocks = new ConcurrentHashMap<>();

    /**
     * Global read/write lock supporting {@link ConcurrentMap#LOCK_ALL}.
     */
    protected final ReentrantReadWriteLock f_lockAll = new ReentrantReadWriteLock();

    /**
     * Per-thread read-lock accounting for held key locks.
     */
    protected final ThreadLocal<ReadLockHold> f_tloReadLockHold = ThreadLocal.withInitial(ReadLockHold::new);

    /**
     * Optional contention observer.
     */
    protected final ContentionObserver<K> f_observer;
    }

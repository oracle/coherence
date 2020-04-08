/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.util.AutoLock.Sentry;


/**
 * An abstract base class for thread-safe {@link LongArray}s which are protected by lock(s).
 *
 * @author mf 2014.10.01
 */
public abstract class AbstractSafeLongArray<V>
        implements LongArray<V>
    {
    @Override
    public V get(long lIndex)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().get(lIndex);
            }
        }

    @Override
    public long floorIndex(long lIndex)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().floorIndex(lIndex);
            }
        }

    @Override
    public V floor(long lIndex)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().floor(lIndex);
            }
        }

    @Override
    public long ceilingIndex(long lIndex)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().ceilingIndex(lIndex);
            }
        }

    @Override
    public V ceiling(long lIndex)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().ceiling(lIndex);
            }
        }

    @Override
    public V set(long lIndex, V oValue)
        {
        try (Sentry<LongArray<V>> sentry = acquireWriteLock())
            {
            return sentry.getResource().set(lIndex, oValue);
            }
        }

    @Override
    public long add(V oValue)
        {
        try (Sentry<LongArray<V>> sentry = acquireWriteLock())
            {
            return sentry.getResource().add(oValue);
            }
        }

    @Override
    public boolean exists(long lIndex)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().exists(lIndex);
            }
        }

    @Override
    public V remove(long lIndex)
        {
        try (Sentry<LongArray<V>> sentry = acquireWriteLock())
            {
            return sentry.getResource().remove(lIndex);
            }
        }

    @Override
    public void remove(long lIndexFrom, long lIndexTo)
        {
        try (Sentry<LongArray<V>> sentry = acquireWriteLock())
            {
            sentry.getResource().remove(lIndexFrom, lIndexTo);
            }
        }

    @Override
    public boolean contains(V oValue)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().contains(oValue);
            }
        }

    @Override
    public void clear()
        {
        try (Sentry<LongArray<V>> sentry = acquireWriteLock())
            {
            sentry.getResource().clear();
            }
        }

    @Override
    public boolean isEmpty()
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().isEmpty();
            }
        }

    @Override
    public int getSize()
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().getSize();
            }
        }

    @Override
    public long getFirstIndex()
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().getFirstIndex();
            }
        }

    @Override
    public long getLastIndex()
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().getLastIndex();
            }
        }

    @Override
    public long indexOf(V oValue)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().indexOf(oValue);
            }
        }

    @Override
    public long indexOf(V oValue, long lIndex)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().indexOf(oValue, lIndex);
            }
        }

    @Override
    public long lastIndexOf(V oValue)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().lastIndexOf(oValue);
            }
        }

    @Override
    public long lastIndexOf(V oValue, long lIndex)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().lastIndexOf(oValue, lIndex);
            }
        }

    @Override
    public Iterator<V> iterator()
        {
        return instantiateSafeIterator(/*fForward*/ true, Long.MIN_VALUE);
        }

    @Override
    public Iterator<V> iterator(long lIndex)
        {
        return instantiateSafeIterator(/*fForward*/ true, lIndex);
        }

    @Override
    public Iterator<V> reverseIterator()
        {
        return instantiateSafeIterator(/*fForward*/ false, Long.MAX_VALUE);
        }

    @Override
    public Iterator<V> reverseIterator(long lIndex)
        {
        return instantiateSafeIterator(/*fForward*/ false, lIndex);
        }


    // ----- Object interface -----------------------------------------------

    @Override
    public String toString()
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return sentry.getResource().toString();
            }
        }

    @Override
    abstract public AbstractSafeLongArray<V> clone();



    // ----- AbstractSafeLongArray ------------------------------------------

    /**
     * Acquire the read lock.
     *
     * @return the lock sentry
     */
    protected abstract Sentry<LongArray<V>> acquireReadLock();

    /**
     * Acquire the write lock.
     *
     * @return the lock sentry
     */
    protected abstract Sentry<LongArray<V>> acquireWriteLock();


    // ----- inner class: SafeIterator -------------------------------------

    /**
     * Instantiate a SafeIterator around the specified delegate iterator.
     *
     * @param fForward    true if a forward iterator is to be returned
     * @param lIndexFrom  the start index
     *
     * @return the safe iterator
     */
    protected SafeIterator instantiateSafeIterator(boolean fForward, long lIndexFrom)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return new SafeIterator(sentry.getResource(), fForward, lIndexFrom);
            }
        }

    /**
     * A lock based LongArray Iterator.  Note the implementation is thread-safe so long as
     * the wrapped unsafe LongArray supports stable iteration when accessed from a single thread.
     * Specifically that performing an add or remove operation directly against the LongArray
     * while being iterated does not corrupt the state of either the LongArray or the Iterator.
     *
     * An alternate approach would be to maintain a write counter and to refresh the delegate iterator
     * each time the write counter changes, the refresh would be cheap as it could use iterator(m_lastIndex).
     */
    protected class SafeIterator
            implements Iterator<V>
        {
        protected SafeIterator(LongArray<V> delegate, boolean fForward, long lIndexFrom)
            {
            f_delegate = fForward
                 ? lIndexFrom == Long.MIN_VALUE
                     ? delegate.iterator()
                     : delegate.iterator(lIndexFrom)
                 : lIndexFrom == Long.MAX_VALUE
                     ? delegate.reverseIterator()
                     : delegate.reverseIterator(lIndexFrom);
            }

        @Override
        public boolean hasNext()
            {
            try (Sentry sentry = acquireReadLock())
                {
                return f_delegate.hasNext();
                }
            }

        @Override
        public V next()
            {
            V    value;
            long lIndex;
            try (Sentry sentry = acquireReadLock())
                {
                value  = f_delegate.next();
                lIndex = f_delegate.getIndex();
                }

            m_valueLast  = value;
            m_lIndexLast = lIndex;

            return value;
            }

        @Override
        public long getIndex()
            {
            // no need to take lock, use cached index
            ensureValid();
            return m_lIndexLast;
            }

        @Override
        public V getValue()
            {
            // no need to take lock, use cached value
            ensureValid();
            return (V) m_valueLast;
            }

        @Override
        public V setValue(V oValue)
            {
            try (Sentry sentry = acquireWriteLock())
                {
                V value = f_delegate.setValue(oValue);
                m_valueLast = oValue;
                return value;
                }
            }

        @Override
        public void remove()
            {
            try (Sentry sentry = acquireWriteLock())
                {
                f_delegate.remove();
                }
            m_valueLast = NO_VALUE;
            }

        /**
         * Ensure that the cached value/index are valid.
         */
        protected void ensureValid()
            {
            if (m_valueLast == NO_VALUE)
                {
                throw new IllegalStateException("missing call to next()");
                }
            }

        /**
         * The delegate iterator.
         */
        protected final Iterator<V> f_delegate;

        /**
         * The last value returned from the iterator.
         */
        protected Object m_valueLast = NO_VALUE;

        /**
         * The index associated with the last returned value
         */
        protected long m_lIndexLast;
        }

    /**
     * A value guaranteed to never be in the array.
     */
    protected static final Object NO_VALUE = new Object();
    }
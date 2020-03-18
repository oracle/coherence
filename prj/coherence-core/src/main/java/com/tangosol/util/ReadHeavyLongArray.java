/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.util.AutoLock;
import com.oracle.coherence.common.util.AutoLock.Sentry;
import com.oracle.coherence.common.util.NullLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A thread-safe LongArray implementation for read heavy workloads but which is also efficient with frequent and/or
 * bursty writes.
 * <p>
 * This implementation provides thread-safety via a combination of locking and copy-on-write. Unlike the
 * {@link CopyOnWriteLongArray} it will in general allow for many mutations within a single copy.  When
 * the read to write ratio is high enough (think thousands of reads per write) the read locks will be elided allowing
 * for maximum performance.
 * <p>
 * For workloads with a read to write ratio close to 1:1 or where writes are more common then reads
 * the {@link SafeLongArray} may be more appropriate.
 *
 * @author mf 2014.10.02
 */
public class ReadHeavyLongArray<V>
    extends AbstractSafeLongArray<V>
    {
    /**
     * Construct a ReadHeavyLongArray.
     */
    public ReadHeavyLongArray()
        {
        this(new SparseArray<V>());
        }

    /**
     * Construct a ReadHeavyLongArray around the specified delegate.
     *
     * @param delegate  the delegate long array, it is not safe to externally access this array
     */
    public ReadHeavyLongArray(LongArray<V> delegate)
        {
        // start in read/write mode
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        m_laDelegate  = delegate;
        f_lockReadRaw = lock.readLock();
        m_lockRead    = new AutoLock<>(f_lockReadRaw, delegate);
        f_lockWrite   = new AutoLock<LongArray<V>>(lock.writeLock())
            {
            @Override
            public LongArray<V> getResource()
                {
                return m_laDelegate; // always return updated resource, never a snapshot
                }
            };
        }


    // ----- Object interface -----------------------------------------------

    @Override
    public ReadHeavyLongArray<V> clone()
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return new ReadHeavyLongArray<>(sentry.getResource().clone());
            }
        }


    // ----- AbstractSafeLongArray interface --------------------------------

    // Note: it is not recommended that public lock accessors be exposed as the usage is quite specific,
    // namely while holding the lock all access to the LongArray must be from the lock's getResource() method
    // any access to this LongArray even while holding the lock may not be safe as often times the getResource()
    // actually refers to a snapshot.

    @Override
    protected Sentry<LongArray<V>> acquireReadLock()
        {
        long cReadTrigger = m_cReadTrigger;
        if (++m_cReads >= cReadTrigger) // dirty increment, but crossing the trigger limit is just a hint
            {
            try (Sentry<LongArray<V>> sentry = f_lockWrite.acquire())
                {
                if (cReadTrigger == m_cReadTrigger) // only promote to lock-free if the read trigger is stable, avoids multiple threads promoting
                    {
                    // transition to lock-free reads on existing delegate array; next writer will switch back
                    // users of this NullLock will always read this now read-only version which makes it safe
                    // for there to be a concurrent switch back to read-write mode.
                    m_lockRead     = new NullLock<>(sentry.getResource());
                    m_cReadTrigger = Long.MAX_VALUE;
                    }
                }
            }
        return m_lockRead.acquire();
        }

    @Override
    protected Sentry<LongArray<V>> acquireWriteLock()
        {
        Sentry<LongArray<V>> sentry   = f_lockWrite.acquire();
        LongArray<V> delegate = sentry.getResource();

        if (m_lockRead instanceof NullLock)
            {
            // transition from lock-free reads; to locked reads after making a copy in case there are concurrent readers now
            delegate     = delegate.clone();
            m_laDelegate = delegate; // ensures that even this write locks's sentry will return the new clone
            m_lockRead   = new AutoLock<>(f_lockReadRaw, delegate);
            }
        // else; already using lock-based readers for this delegate

        // each time we do a write we need to push out the read trigger
        m_cReadTrigger = m_cReads + delegate.getSize() * CLONE_COST_MULTIPLIER;

        return sentry;
        }

    @Override
    protected SafeIterator instantiateSafeIterator(final boolean fForward, final long lIndexFrom)
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            final LongArray<V> laDelegateStart = sentry.getResource();
            return new SafeIterator(laDelegateStart, fForward, lIndexFrom)
                {
                @Override
                public V setValue(V oValue)
                    {
                    V valueOld;
                    try (Sentry<LongArray<V>> sentry = acquireWriteLock())
                        {
                        LongArray<V> laDelegateCurr = sentry.getResource();
                        if (laDelegateStart == laDelegateCurr)
                            {
                            valueOld = f_delegate.setValue(oValue);
                            }
                        else
                            {
                            // we're iterating an old version, perform operation against the current delegate array
                            valueOld =laDelegateCurr.set(getIndex(), oValue);
                            }
                        }

                    m_valueLast = oValue;
                    return valueOld;
                    }

                @Override
                public void remove()
                    {
                    try (Sentry<LongArray<V>> sentry = acquireWriteLock())
                        {
                        LongArray<V> laDelegateCurr = sentry.getResource();
                        if (laDelegateStart == laDelegateCurr)
                            {
                            f_delegate.remove();
                            }
                        else
                            {
                            // we're iterating an old version, perform operation against the current delegate array
                            laDelegateCurr.remove(getIndex());
                            }
                        m_valueLast = NO_VALUE;
                        }
                    }
                };
            }
        }


    // ----- constants ------------------------------------------------------

    /**
     * The assumed relative cost of cloning an element in the LongArray relative to doing a LongArray.get().
     *
     * This value ultimately dictates how many read operations must be performed against a read/write array
     * before it is considered worth it to switch to read-only mode.  Once in read-only mode any writes will
     * force a potentially very expensive copy.
     */
    private static final int CLONE_COST_MULTIPLIER = 1000;


    // ----- data members ---------------------------------------------------

    /**
     * The delegate array, only to be accessed through the write AutoLock.
     */
    private LongArray<V> m_laDelegate;

    /**
     * The write lock, delegating to the one in the super class.
     */
    protected final AutoLock<LongArray<V>> f_lockWrite;

    /**
     * The raw read lock.
     */
    protected final Lock f_lockReadRaw;

    /**
     * The mutable read lock.
     */
    protected volatile AutoLock<LongArray<V>> m_lockRead;

    /**
     * The (dirty) total number of reads.
     */
    protected long m_cReads;

    /**
     * The read count at which to trigger a switch to a lock-free delegate.
     */
    protected long m_cReadTrigger;
    }

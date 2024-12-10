/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.util.AutoLock;
import com.oracle.coherence.common.util.AutoLock.Sentry;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe variant of {@link LongArray} in which is protected by a ReentrantLock.
 *
 * This implementation is suitable for workloads where the read to write ratio is close to 1:1, or where writes are
 * more common then reads.  For read heavy workloads the {@link ReadHeavyLongArray}
 * is likely to be a better choice.
 *
 * @author mf 2014.10.01
 */
public class SafeLongArray<V>
        extends AbstractSafeLongArray<V>
    {
    /**
     * Construct a new SafeLongArray.
     */
    public SafeLongArray()
        {
        this(new SparseArray<V>());
        }

    /**
     * Construct a new SafeLongArray around the specified delegate.
     *
     * @param laDelegate  the delegate long array, it is not safe to externally access this array
     */
    public SafeLongArray(LongArray<V> laDelegate)
        {
        f_lock = new AutoLock<>(new ReentrantLock(), laDelegate);
        }


    // ----- SafeLongArray interface ---------------------------------------

    /**
     * Return the lock used to protect this LongArray, with the unsafe delegate as its
     * {@link AutoLock#getResource() resource}.
     * <p>
     * Explicit usage of this lock is only necessary when performing multiple sequential operations against the
     * LongArray which need to be "atomic".
     *
     * @return the lock
     */
    public AutoLock<LongArray<V>> getLock()
        {
        return f_lock;
        }


    // ----- Object interface ----------------------------------------------

    @Override
    public SafeLongArray<V> clone()
        {
        try (Sentry<LongArray<V>> sentry = acquireReadLock())
            {
            return new SafeLongArray<>(sentry.getResource().clone());
            }
        }


    // ----- AbstractSafeLongArray ------------------------------------------

    // Note: it is up to the derived impls to decide if these locks can be publicly
    // exposed, it is not safe to assume that is allowable here, see ReadHeavyLongArray

    @Override
    protected Sentry<LongArray<V>> acquireReadLock()
        {
        return f_lock.acquire();
        }

    @Override
    protected Sentry<LongArray<V>> acquireWriteLock()
        {
        return f_lock.acquire();
        }


    // ----- data members --------------------------------------------------

    /**
     * The lock to hold during read and write operations.
     */
    protected final AutoLock<LongArray<V>> f_lock;
    }
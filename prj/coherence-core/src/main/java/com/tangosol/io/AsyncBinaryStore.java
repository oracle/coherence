/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Daemon;
import com.tangosol.util.SegmentedConcurrentMap;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.function.Consumer;

/**
* An AsyncBinaryStore is a BinaryStore wrapper that performs the "O" (output)
* portion of its I/O asynchronously on a daemon thread. The output portion
* consists of store and erase processing.
* <p>
* Since the "O" portion is passed along to the wrapped BinaryStore on a
* separate thread, only read operations are blocking, thus the BinaryStore
* operations on a whole appear much faster. As such, it is somewhat analogous
* to a write-behind cache.
* <p>
* If an operation fails on the daemon thread, all further operations will
* occur synchronously, so that exceptions will propagate successfully up. It
* is assumed that once one exception occurs, the underlying BinaryStore is
* in a state that will cause more exceptions to occur. Even when an exception
* occurs on the daemon thread, that write-behind data will not be "lost"
* because it will still be available in the internal data structures that
* keeps track of pending writes.
*
* @since Coherence 2.5
* @author cp 2004.06.18
*/
public class AsyncBinaryStore
        extends Base
        implements BinaryStore
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an AsyncBinaryStore. An AsyncBinaryStor wrap other
    * BinaryStore objects to provide asynchronous write operations.
    * <p>
    * The new AsyncBinaryStore will queue a maximum of <tt>4194304</tt> bytes
    * (4 MB) before blocking.
    *
    * @param store  the BinaryStore to wrap
    */
    public AsyncBinaryStore(BinaryStore store)
        {
        this(store, DEFAULT_LIMIT);
        }

    /**
    * Construct an AsyncBinaryStore. An AsyncBinaryStor wrap other
    * BinaryStore objects to provide asynchronous write operations.
    *
    * @param store  the BinaryStore to wrap
    * @param cbMax  the maximum number of bytes to queue before blocking
    */
    public AsyncBinaryStore(BinaryStore store, int cbMax)
        {
        m_store = store;
        m_cbMax = cbMax;

        ensureQueueDaemon();
        }


    // ----- BinaryStore interface ------------------------------------------

    /**
    * Return the value associated with the specified key, or null if the
    * key does not have an associated value in the underlying store.
    *
    * @param binKey  key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or
    *         <tt>null</tt> if no value is available for that key
    */
    public Binary load(Binary binKey)
        {
        Binary binValue;

        ConcurrentMap mapPending = getPendingMap();
        while (!mapPending.lock(binKey, WAIT_FOREVER)) {}
        try
            {
            // load is performed first against any pending write
            binValue = (Binary) mapPending.get(binKey);
            if (binValue == DELETED)
                {
                // it is pending an erase operation
                binValue = null;
                }
            else if (binValue != null)
                {
                // it is pending a store operation
                }
            else
                {
                // make sure that this BinaryStore is still open
                BinaryStore store = getBinaryStore();
                if (store == null)
                    {
                    throw new IllegalStateException("BinaryStore has been closed");
                    }

                // go to the underlying store
                binValue = store.load(binKey);
                }
            }
        finally
            {
            mapPending.unlock(binKey);
            }

        return binValue;
        }

    /**
    * Store the specified value under the specific key in the underlying
    * store. This method is intended to support both key/value creation
    * and value update for a specific key.
    *
    * @param binKey    key to store the value under
    * @param binValue  value to be stored
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void store(Binary binKey, Binary binValue)
        {
        ConcurrentMap mapPending = getPendingMap();
        while (!mapPending.lock(binKey, WAIT_FOREVER)) {}
        try
            {
            // make sure that this BinaryStore is still open
            BinaryStore store = getBinaryStore();
            if (store == null)
                {
                throw new IllegalStateException("BinaryStore has been closed");
                }

            // pend the value
            Binary binOldValue = (Binary) mapPending.put(binKey, binValue);

            // update the amount of bytes that are pending being written
            int cbNew = binKey.length() + binValue.length();
            int cbOld = binOldValue == null ? 0 : (binKey.length() + binOldValue.length());
            updateQueuedSize(cbNew - cbOld);

            QueueDaemon daemon = getQueueDaemon();
            if (daemon != null && isAsync() && getQueuedSize() < getQueuedLimit())
                {
                // let the daemon do the work
                daemon.scheduleWork();
                m_cAsyncWrite++;
                }
            else
                {
                // perform the operation synchronously
                if (binValue == DELETED)
                    {
                    store.erase(binKey);
                    }
                else
                    {
                    store.store(binKey, binValue);
                    }

                // pending write is done
                mapPending.remove(binKey);
                updateQueuedSize(-cbNew);
                m_cSyncWrite++;
                }
            }
        finally
            {
            mapPending.unlock(binKey);
            }
        }

    /**
    * Remove the specified key from the underlying store if present.
    *
    * @param binKey key whose mapping is to be removed from the map
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void erase(Binary binKey)
        {
        store(binKey, DELETED);
        }

    /**
    * Remove all data from the underlying store.
    *
    * @throws UnsupportedOperationException  if this implementation or the
    *         underlying store is read-only
    */
    public void eraseAll()
        {
        ConcurrentMap mapPending = getPendingMap();
        while (!mapPending.lock(LOCK_ALL, WAIT_FOREVER)) {}
        try
            {
            // erase the underlying BinaryStore
            BinaryStore store = getBinaryStore();

            if (store == null)
                {
                throw new IllegalStateException("BinaryStore has been closed");
                }
            store.eraseAll();

            // discard all pending writes
            mapPending.clear();

            // re-init the total pending byte count
            updateQueuedSize(-getQueuedSize());
            }
        finally
            {
            mapPending.unlock(LOCK_ALL);
            }

        }

    /**
    * Iterate all keys in the underlying store.
    *
    * @return a read-only iterator of the keys in the underlying store
    *
    * @throws UnsupportedOperationException  if the underlying store is not
    *         iterable
    */
    public Iterator keys()
        {
        ConcurrentMap mapPending = getPendingMap();
        while (!mapPending.lock(LOCK_ALL, WAIT_FOREVER)) {}
        try
            {
            // start with the keys from the underlying BinaryStore
            BinaryStore store = getBinaryStore();

            if (store == null)
                {
                throw new IllegalStateException("BinaryStore has been closed");
                }

            Iterator    iter  = store.keys();

            // check if there is anything pending that could change the keys
            if (!mapPending.isEmpty())
                {
                // dump all of the keys from the underlying BinaryStore into
                // a temporary set
                Set setKeys = new HashSet();
                while (iter.hasNext())
                    {
                    setKeys.add(iter.next());
                    }

                // add all pending "stores" and remove all pending "erases"
                for (iter = mapPending.entrySet().iterator(); iter.hasNext(); )
                    {
                    Map.Entry entry    = (Map.Entry) iter.next();
                    Binary    binKey   = (Binary) entry.getKey();
                    Binary    binValue = (Binary) entry.getValue();
                    if (binValue == DELETED)
                        {
                        setKeys.remove(binKey);
                        }
                    else
                        {
                        setKeys.add(binKey);
                        }
                    }

                // return the result
                iter = setKeys.iterator();
                }

            return iter;
            }
        finally
            {
            mapPending.unlock(LOCK_ALL);
            }
        }


    // ----- lifecycle ------------------------------------------------------

    /**
    * Close the store.
    */
    public void close()
        {
        internalClose(null);
        }

    /**
    * Close the store.
    * The wrapped store is closed either with the optional <code>onClose</code> consumer or
    * by directly calling <code>close</code> on wrapped store when <code>onClose</code>
    * consumer is null.
    *
    * @param onClose  optional close consumer to close wrapped {@link BinaryStore}
    */
    protected void internalClose(Consumer<? super BinaryStore> onClose)
        {
        BinaryStore store = getBinaryStore();
        if (store == null)
            {
            // already closed
            return;
            }

        ConcurrentMap mapPending = getPendingMap();
        while (!mapPending.lock(LOCK_ALL, WAIT_FOREVER)) {}

        try
            {
            synchronized (this)
                {
                store = getBinaryStore();
                if (store == null)
                    {
                    // closed during wait
                    return;
                    }

                // close the underlying store
                try
                    {
                    if (onClose == null)
                        {
                        ClassHelper.invoke(store, "close", ClassHelper.VOID);
                        }
                    else
                        {
                        onClose.accept(store);
                        }
                    }
                catch (Throwable e) {}

                // release the underlying store and any pending writes
                forceSync();
                setBinaryStore(null);
                mapPending.clear();
                updateQueuedSize(-getQueuedSize());

                // wake up the daemon (to shut it down)
                QueueDaemon daemon = getQueueDaemon();
                if (daemon != null)
                    {
                    daemon.wakeNow();
                    }
                }
            }
        finally
            {
            mapPending.unlock(LOCK_ALL);
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the BinaryStore that this AsyncBinaryStore wraps. The wrapped
    * BinaryStore is also referred to as the "underlying" BinaryStore. All
    * I/O operations are delegated to the underlying BinaryStore; some write
    * operations are performed asynchronously on the QueueDaemon thread.
    *
    * @return the underlying BinaryStore
    */
    public BinaryStore getBinaryStore()
        {
        return m_store;
        }

    /**
    * Specify the underlying BinaryStore. When the AsyncBinaryStore is
    * closed, it should clear out its reference to the underlying BinaryStore
    * to indicate that it no longer is open.
    *
    * @param store  the underlying BinaryStore
    */
    protected synchronized void setBinaryStore(BinaryStore store)
        {
        m_store = store;
        }

    /**
    * Determine the size, in bytes, of the data that can be queued to be
    * written asynchronously by the QueueDaemon thread. Once the limit is
    * exceeded, operations will be forced to be synchronous to avoid running
    * out of memory or getting too far behind on the I/O operations.
    *
    * @return the number of bytes allowed to be queued to be written
    */
    public int getQueuedLimit()
        {
        return m_cbMax;
        }

    /**
    * Determine the current number of bytes that are pending being written.
    *
    * @return the number of bytes currently queued to be written
    */
    public int getQueuedSize()
        {
        return m_cbPending;
        }

    /**
    * Update the number of bytes that are pending to be written.
    *
    * @param cb  the number of bytes that the queue length changed by
    */
    protected synchronized void updateQueuedSize(int cb)
        {
        m_cbPending += cb;
        }

    /**
    * Determine if the AsyncBinaryStore is operating in an asynchronous
    * manner.
    *
    * @return true if the AsyncBinaryStore is still operating in an async
    *         mode
    */
    public boolean isAsync()
        {
        return m_fAsync;
        }

    /**
    * Indicate the future write operations must be synchronous.
    */
    public synchronized void forceSync()
        {
        m_fAsync = false;
        }

    /**
    * Obtain the map that contains all of the pending store and erase data.
    * The key of the map is a Binary key to pass to the underlying store.
    * The corresponding value will be {@link #DELETED} to indicate an erase
    * operation, otherwise it will be a Binary value.
    *
    * @return the ConcurrentMap that keeps track of all the pending writes
    */
    protected ConcurrentMap getPendingMap()
        {
        return m_mapPending;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human readable description of the AsyncBinaryStore.
    *
    * @return a String description of the AsyncBinaryStore
    */
    public String toString()
        {
        BinaryStore store = getBinaryStore();
        return "AsyncBinaryStore{"
            + (store == null ? "Closed" : store.toString())
            + ", async writes="  + m_cAsyncWrite
            + ", sync writes="   + m_cSyncWrite
            + ", effectiveness=" + (m_cSyncWrite == 0 ? 1.0f :
                 (float) (1.0 - (double) m_cSyncWrite/(double) (m_cSyncWrite + m_cAsyncWrite)))
            + '}';
        }

    /**
    * Perform cleanup during garbage collection.
    */
    protected void finalize()
        {
        close();
        }


    // ----- inner class: QueueDaemon ---------------------------------------

    /**
    * @return the daemon that manages the write-behind queue
    */
    protected QueueDaemon getQueueDaemon()
        {
        return m_daemon;
        }

    /**
    * @param daemon  the daemon that manages the write-behind queue
    */
    protected void setQueueDaemon(QueueDaemon daemon)
        {
        m_daemon = daemon;
        }

    /**
    * Obtain the QueueDaemon, if one already exists; otherwise, create and
    * start one.
    *
    * @return the daemon that manages the write-behind queue
    */
    protected synchronized QueueDaemon ensureQueueDaemon()
        {
        QueueDaemon daemon = getQueueDaemon();

        if (daemon == null)
            {
            daemon = instantiateQueueDaemon();
            daemon.start();
            setQueueDaemon(daemon);
            }

        return daemon;
        }

    /**
    * Factory method: Instantiate a QueueDaemon.
    *
    * @return a new QueueDaemon, but not started
    */
    protected QueueDaemon instantiateQueueDaemon()
        {
        return new QueueDaemon();
        }

    /**
    * A daemon that processes queued writes.
    */
    protected class QueueDaemon
            extends Daemon
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a queue daemon to process pending writes.
        */
        public QueueDaemon()
            {
            super("AsyncBinaryStore["
                    + ClassHelper.getSimpleName(AsyncBinaryStore.this.getBinaryStore().getClass())
                    + "]");
            }

        // ----- notifications ------------------------------------------

        /**
        * Notify the daemon that there is work to be done.
        */
        public synchronized void scheduleWork()
            {
            // only need to wake up the daemon if it has gone "dormant"
            if (isDormant())
                {
                notifyAll();
                }
            }

        /**
        * Force the daemon to wake up immediately.
        */
        public synchronized void wakeNow()
            {
            notifyAll();
            }

        // ----- internal: processing -----------------------------------

        /**
        * Determine if the daemon thread should continue processing or should
        * shut itself down. The daemon thread will continue processing as
        * long as there is an underlying BinaryStore to write pending items
        * to and the write-behind mode has not explicitly been disabled.
        *
        * @return true if the daemon should shut itself down
        */
        public boolean isDone()
            {
            return AsyncBinaryStore.this.getBinaryStore() == null
                || !AsyncBinaryStore.this.isAsync();
            }

        /**
        * This method is invoked on the daemon thread and performs the daemon
        * processing until the thread stops.
        */
        public void run()
            {
            try
                {
                ConcurrentMap mapPending = AsyncBinaryStore.this.getPendingMap();
                while (!isDone())
                    {
                    // iterate the pending work to do, and process it
                    boolean fNoWork = true;
                    try
                        {
                        for (Iterator iter = mapPending.entrySet().iterator(); iter.hasNext(); )
                            {
                            fNoWork = false;

                            Map.Entry entry    = (Map.Entry) iter.next();
                            Binary    binKey   = (Binary) entry.getKey();
                            Binary    binValue = (Binary) entry.getValue();

                            processPending(binKey, binValue);
                            }
                        }
                    catch (ConcurrentModificationException e)
                        {
                        fNoWork = false;
                        }

                    // make sure that this "background" thread doesn't run
                    // "full throttle"
                    synchronized (this)
                        {
                        if (mapPending.isEmpty())
                            {
                            if (fNoWork)
                                {
                                takeVacation();
                                }
                            else
                                {
                                takeNap();
                                }
                            }
                        else
                            {
                            takeBreak();
                            }
                        }
                    }
                }
            catch (Throwable e)
                {
                if (!isDone())
                    {
                    err("An exception occurred in the AsyncBinaryStore QueueDaemon:");
                    err(e);
                    err("(The daemon is terminating.)");
                    }
                }
            finally
                {
                AsyncBinaryStore.this.setQueueDaemon(null);
                AsyncBinaryStore.this.forceSync();
                }
            }

        /**
        * Store the specified value under the specific key in the underlying
        * store, or if the value is {@link AsyncBinaryStore#DELETED} then erase
        * the value from the underlying store.
        *
        * @param binKey    key to store the value under
        * @param binValue  value to be stored
        */
        protected void processPending(Binary binKey, Binary binValue)
            {
            ConcurrentMap mapPending = AsyncBinaryStore.this.getPendingMap();
            if (mapPending.lock(binKey))
                {
                try
                    {
                    if (mapPending.containsKey(binKey))
                        {
                        // make sure that this BinaryStore is still open
                        BinaryStore store = AsyncBinaryStore.this.getBinaryStore();
                        if (store != null)
                            {
                            // perform the operation
                            if (binValue == AsyncBinaryStore.DELETED)
                                {
                                store.erase(binKey);
                                }
                            else
                                {
                                store.store(binKey, binValue);
                                }
                            }

                        // discard the pending write
                        mapPending.remove(binKey);

                        // update the amount of bytes that are pending being written
                        int cbWrite = binKey.length() + binValue.length();
                        AsyncBinaryStore.this.updateQueuedSize(-cbWrite);
                        }
                    }
                finally
                    {
                    mapPending.unlock(binKey);
                    }
                }
            }

        // ----- internal: sleeping -----------------------------------------

        /**
        * Take a short break before plowing into the pending work again.
        * This mode is used when there is already more work to do.
        */
        protected synchronized void takeBreak()
            {
            try
                {
                Blocking.wait(this, 32L);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            }

        /**
        * Take a nap before checking for more pending work. This mode is
        * purposefully relaxed to let some work queue up before trying to
        * process it.
        */
        protected synchronized void takeNap()
            {
            try
                {
                Blocking.wait(this, 256L);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            }

        /**
        * Go on an extended vacation until there is any pending work. This
        * is called a "dormant" mode in which this thread will go to sleep
        * indefinitely and require another thread to wake it up should any
        * work need to be done.
        */
        protected synchronized void takeVacation()
            {
            setDormant(true);
            try
                {
                Blocking.wait(this);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            finally
                {
                setDormant(false);
                }
            }

        // ----- accessors ----------------------------------------------

        /**
        * Determine whether the daemon is dormant or not.
        *
        * @return true if the daemon is dormant, which means that the
        *         daemon must be woken up in order to do any work
        */
        protected boolean isDormant()
            {
            return m_fDormant;
            }

        /**
        * Specify whether the daemon is dormant or not.
        *
        * @param fDormant  pass true if the daemon is going dormant, or false
        *                  if the daemon is waking up from a dormant state
        */
        protected synchronized void setDormant(boolean fDormant)
            {
            m_fDormant = fDormant;
            }

        // ----- data members -------------------------------------------

        /**
        * True when the daemon is in a state that requires waking for it to
        * process anything.
        */
        private boolean m_fDormant;
        }


    // ----- constants ------------------------------------------------------

    /**
    * A special token that is used to signify a queued erase operation.
    */
    protected static final Binary DELETED = new Binary();

    /**
    * Default size limit for write-queued data.
    */
    protected static final int DEFAULT_LIMIT = 4 * 1024 * 1024;

    /**
    * Special key to indicate that all keys should be locked.
    */
    protected static final Object LOCK_ALL = ConcurrentMap.LOCK_ALL;

    /**
    * Special wait time to indicate that a lock should be blocked on until
    * it becomes available.
    */
    protected static final long WAIT_FOREVER = -1L;


    // ----- data members ---------------------------------------------------

    /**
    * The wrapped BinaryStore.
    */
    private volatile BinaryStore m_store;

    /**
    * The "write-behind" queue (which is not actually a queue, since it
    * doesn't matter what order things are written in).
    */
    private ConcurrentMap m_mapPending = new SegmentedConcurrentMap();

    /**
    * The maximum size in bytes of pending writes that can be queued for
    * asynchronous processing. This helps to avoid out-of-memory conditions.
    */
    private int m_cbMax;

    /**
    * The current size in bytes of pending writes. This is the sum of the
    * size of the keys and values that have not been written yet.
    */
    private int m_cbPending;

    /**
    * The "write-behind" daemon.
    */
    private QueueDaemon m_daemon;

    /**
    * True as long as asynchronous operations are allowed against the store.
    */
    private boolean m_fAsync = true;

    /**
    * The number of write operations performed in async mode.
    */
    private volatile long m_cAsyncWrite;

    /**
    * The number of write operations performed in sync mode.
    */
    private volatile long m_cSyncWrite;
    }

/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import com.oracle.coherence.common.base.Associated;
import com.oracle.coherence.common.base.ConcurrentNotifier;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * A concurrent implementation of an AssociationPile.
 *
 * @param <T>  the pile type
 * @param <A>  the association type
 *
 * @author mf 2018.04.10
 */
public class ConcurrentAssociationPile<T, A>
    extends ConcurrentNotifier
    implements AssociationPile<T>
    {
    @Override
    public boolean add(T value)
        {
        f_cValues.increment();

        A key = getAssociation(value);
        if (key == null)
            {
            return addAvailable(value);
            }
        else if (key == ASSOCIATION_ALL)
            {
            return handleAddAll(value);
            }
        else
            {
            try (Sentry sentry = f_gateAll.enter())
                {
                CloseableQueue<Node<T>> queue = m_lPosNextAll == Long.MAX_VALUE
                        ? f_mapQueueDeferred.putIfAbsent(key, EMPTY_QUEUE)
                        : EMPTY_QUEUE;

                return queue == null
                        ? addAvailable(value) // common path
                        : handleAssociatedAdd(key, value, queue);
                }
            }
        }

    @Override
    public T poll()
        {
        // nodes could be in either available queue, we have to check both (at least over time) or we could let one starve
        // note, that polling from an empty SkipList is quite cheap.  We could try to be extra fair by peeking into
        // both and polling from the one with the older Node.  This would be extra expense which isn't required for
        // correctness, and SkipList also doesn't have a peek operation.

        boolean fPriority = (++m_cPolls & 1) == 0;
        Node<T> node      = null;
        T       value;

        if (fPriority)
            {
            node  = f_queueAvailablePriority.pollFirst();
            value = node == null ? f_queueAvailable.poll() : node.getValue();
            }
        else
            {
            value = f_queueAvailable.poll();
            if (value == null)
                {
                node  = f_queueAvailablePriority.pollFirst();
                value = node == null ? null : node.getValue();
                }
            }

        if (value != null)
            {
            f_cValues.decrement();
            }

        return value;
        }

    @Override
    public void release(T value)
        {
        A key = getAssociation(value);
        if (key == null)
            {
            // we don't have any cleanup work for unassociated
            }
        else if (key == ASSOCIATION_ALL)
            {
            handleReleaseAll();
            }
        else // common path
            {
            // release of an association must either move the next associated node to available or remove the empty queue
            try (Sentry sentry = f_gateAll.enter())
                {
                Node<T> node = f_mapQueueDeferred.remove(key, EMPTY_QUEUE)
                    ? null // common path
                    : pollOrRemoveAssociation(key);

                if (m_lPosNextAll < Long.MAX_VALUE) // must be checked even if node is null
                    {
                    handlePostReleaseWithPendingAll(node);
                    }
                else if (node != null)
                    {
                    addAvailable(node);
                    }
                // else; common path
                }
            catch (IllegalArgumentException e)
                {
                throw new IllegalArgumentException("while releasing " + value + " of " + value.getClass(), e);
                }
            }
        }

    @Override
    public int size()
        {
        return Math.max(0, f_cValues.intValue());
        }

    @Override
    public boolean isAvailable()
        {
        return !f_queueAvailable.isEmpty() || !f_queueAvailablePriority.isEmpty();
        }

    // ---- MultiWaiterMultiNotifier interface ------------------------------

    @Override
    protected boolean isReady()
        {
        return isAvailable();
        }

    // ---- Object interface ------------------------------------------------

    @Override
    public String toString()
        {
        return getClass().getCanonicalName()
                + " size=" + size()
                + ", available=" + isAvailable()
                + ", associations=" + f_mapQueueDeferred.size();
        }


    // ---- ConcurrentAssociationPile methods -------------------------------

    /**
     * Return a Node for the specified value.
     *
     * @param value      the value
     * @param lPosition  the value's position within the pile
     *
     * @return the node
     */
    protected Node<T> makeNode(T value, long lPosition)
        {
        return new SimpleNode<>(value, lPosition);
        }

    /**
     * Return the association for the specified value.
     *
     * @param value the value
     *
     * @return the association
     */
    protected A getAssociation(T value)
        {
        return value instanceof Associated
                ? ((Associated<A>) value).getAssociatedKey()
                : null;
        }

    /**
     * Add the specified node to the available set.
     *
     * @param node  the available node
     *
     * @return true
     */
    protected boolean addAvailable(Node<T> node)
        {
        // adding to a SkipList while O(log(N)) still has quite a bit of bookkeeping overhead that we'd
        // prefer to avoid where possible.  Note we aren't required to be perfectly accurate here as
        // the available queues never contain multiple nodes with the same association so there is only
        // a fairness ordering to consider not a correctness ordering.
        long lPos = node.getPosition();
        if (m_lPosAvailableLast - lPos > MAX_UNFAIRNESS_VARIANCE)
            {
            // this node is much older then the last add, push through the strict queue
            f_queueAvailablePriority.add(node);
            }
        else
            {
            m_lPosAvailableLast = lPos;
            f_queueAvailable.add(node.getValue());
            }

        signal(); // free if there are no threads waiting

        return true;
        }

    /**
     * Add a value to the available queue.
     *
     * @param value  the value
     *
     * @return true
     */
    protected boolean addAvailable(T value)
        {
        m_lPosAvailableLast = ++m_cAdds;
        f_queueAvailable.add(value);

        signal(); // free if there are no threads waiting

        return true;
        }

    /**
     * Handle the add of an ASSOCIATION_ALL node.
     *
     * @param valueAll  the ASSOCIATION_ALL associated value
     *
     * @return true
     */
    protected boolean handleAddAll(T valueAll)
        {
        try (Sentry sentry = f_gateAll.close()) // block concurrent associated adds
            {
            long lPos = nextPosition(); // must occur under write lock to ensure ordering

            if (m_lPosNextAll == Long.MAX_VALUE)
                {
                // there are no pending ALLs
                if (f_mapQueueDeferred.isEmpty())
                    {
                    // there are also no pending associations
                    m_lPosNextAll = lPos;
                    addAvailable(valueAll);
                    }
                else
                    {
                    // ensure all already available (or polled) associations get handled first, to be here means there are outstanding nodes
                    // pending release
                    m_lPosNextAll = lPos;
                    f_queueDeferredAlls.add(makeNode(valueAll, lPos));
                    f_cAssociatedPendingRelease.set(f_mapQueueDeferred.size()); // there must be this many associations pending release (semi-expensive but very rare)
                    }
                }
            else // there are already ALL nodes, we can only come after them
                {
                f_queueDeferredAlls.add(makeNode(valueAll, lPos));
                }
            }

        return true;
        }

    /**
     * Handle the release of an ASSOCIATION_ALL value.
     */
    protected void handleReleaseAll()
        {
        // attempt to drain the ALL queue. It can be drained up until we find a new ALL, if we fully
        // drain it without finding an ALL we can return to normal operations.
        // We use a high level lock here to prevent endless churn on the queue where other threads add
        // to it as we drain it, this ultimately helps us got back to our desired non-ALL state faster.

        try (Sentry sentry = f_gateAll.close())
            {
            m_lPosNextAll = Long.MAX_VALUE; // assume there will be no more ALLs

            int cAssociatedPendingRelease = 0;
            for (Node<T> node = f_queueDeferredAlls.pollFirst(); node != null; node = f_queueDeferredAlls.pollFirst())
                {
                if (getAssociation(node.getValue()) == ASSOCIATION_ALL)
                    {
                    m_lPosNextAll = node.getPosition();
                    if (cAssociatedPendingRelease == 0)
                        {
                        addAvailable(node);
                        }
                    else
                        {
                        // we'd already made other associated nodes available so we'll have
                        // to wait on the ALL node, re-insert it (ordered insertion)
                        f_queueDeferredAlls.add(node);
                        f_cAssociatedPendingRelease.set(cAssociatedPendingRelease);
                        }

                    break; // either way we're done once we've see a new ALL
                    }

                addAvailable(node);
                ++cAssociatedPendingRelease;
                // we can continue to add more until we hit an ASSOCATION_ALL
                }
            }
        }

    /**
     * Handle the add of an associated node.
     *
     * @param key    the association
     * @param value  the value
     * @param queue  the associated queue (or null)
     *
     * @return true
     */
    protected boolean handleAssociatedAdd(A key, T value, CloseableQueue<Node<T>> queue)
        {
        Node<T> node   = makeNode(value, nextPosition()); // inc under lock ensures it is never less than a concurrently added ALL
        boolean fNoAll = m_lPosNextAll == Long.MAX_VALUE; // stable since we hold read lock
        boolean fAdded = fNoAll && queue.add(node);

        while (!fAdded)
            {
            queue  = f_mapQueueDeferred.putIfAbsent(key, EMPTY_QUEUE);
            fAdded = queue == null
                    ? fNoAll
                        ? addAvailable(value) // common path; direct add to available
                        : f_queueDeferredAlls.add(node)
                    : queue == EMPTY_QUEUE
                        ? f_mapQueueDeferred.replace(key, EMPTY_QUEUE, new CloseableQueue<>(node)) // defer via promotion to inflated queue
                        : queue.add(node); // defer to existing real queue
            }

        return true;
        }

    /**
     * Compute a position for an new element.
     *
     * @return the position
     */
    protected long nextPosition()
        {
        // the position must be > the f_nPosCounter, and should be > m_cAdds; also take this as an opportunity to
        // bring the two close together, but f_nPosCounter can never go backwards
        long lPosCurr = f_nPosCounter.get();
        long lPosNext = Math.max(lPosCurr, m_cAdds) + 1;

        while (!f_nPosCounter.compareAndSet(lPosCurr, lPosNext))
            {
            lPosCurr = f_nPosCounter.get();
            lPosNext = Math.max(lPosCurr, m_cAdds) + 1;
            }
        m_cAdds = lPosNext;

        return lPosNext;
        }

    /**
     * Handle the post release of an associated node while there is an ALL pending.
     *
     * @param nodeNext  the next deferred node associated with the released node
     */
    protected void handlePostReleaseWithPendingAll(Node<T> nodeNext)
        {
        if (nodeNext == null ||
           (nodeNext.getPosition() > m_lPosNextAll && f_queueDeferredAlls.add(nodeNext)))
           {
           // the node either doesn't exist or comes after the pending ALL
           // (in which case we've deferred it through the ALL queue above)
           // decrement the pending count so we can make the ALL available
           if (f_cAssociatedPendingRelease.decrementAndGet() == 0)
               {
               addAvailable(f_queueDeferredAlls.pollFirst());
               }
           }
        else
           {
           // this node is still in front of the pending ALL
           addAvailable(nodeNext);
           }
        }

    /**
     * Poll the next node for the specified association, or remove the association's deferred queue if it is empty.
     *
     * @param key  the association to poll or remove
     *
     * @return the next node or null, which indicates the queue has also been closed and removed
     */
    protected Node<T> pollOrRemoveAssociation(A key)
        {
        CloseableQueue<Node<T>> queue = f_mapQueueDeferred.get(key);

        if (queue == null)
            {
            // Either the user error or we have an illegal state in the pile.  This should not be ignored
            // as either condition could just as easily cause pile corruption and allow two associated tasks
            // to be concurrently pending release which would violate the contract of the pile.
            throw new IllegalArgumentException("association " + key + " of " + key.getClass()
                    + " is not currently pending release in the pile");
            }

        Node<T> node  = queue.poll();
        if (node == null)
            {
            try (Sentry sentry = queue.f_gate.close())
                {
                node = queue.poll();
                if (node == null)
                    {
                    queue.close();
                    f_mapQueueDeferred.remove(key);
                    }
                }
            }

        return node;
        }


    // ---- inner interface: Node -------------------------------------------

    /**
     * Node is a thin wrapper around the pile's value object.
     *
     * For use cases where a the added value type is well known it may implement Node itself, and avoid
     * creating Node garbage objects, though in such cases it is critical that such values are never exist
     * in two piles at the same time.
     *
     * @param <T>  the value type
     */
    protected interface Node<T>
        extends Comparable<Node<T>>
        {
        /**
         * Return the node's position within the pile.
         *
         * @return the position
         */
        public long getPosition();

        /**
         * Return the nodes value.
         *
         * @return the value
         */
        public T getValue();

        @Override
        default int compareTo(Node<T> o)
            {
            return Long.compare(getPosition(), o.getPosition());
            }
        }

    // ---- inner class: SimpleNode -----------------------------------------

    /**
     * A simple implementation of the Node interface.
     *
     * @param <T> the value type
     */
    protected static class SimpleNode<T>
        implements Node<T>
        {
        /**
         * Construct a SimpleNode with the specified value and position.
         *
         * @param value      the value
         * @param lPosition  the position
         */
        public SimpleNode(T value, long lPosition)
            {
            f_value     = value;
            m_lPosition = lPosition;
            }

        @Override
        public long getPosition()
            {
            return m_lPosition;
            }

        @Override
        public T getValue()
            {
            return f_value;
            }

        // ----- data members -------------------------------------------

        /**
         * The value.
         */
        protected final T f_value;

        /**
         * The position.
         */
        protected long m_lPosition;
        }

    // ----- inner class: CloseableQueue ------------------------------------

    /**
     * A Queue implementation which is also closeable and supports a read write lock.
     *
     * @param <E>  the element type
     */
    protected static class CloseableQueue<E>
        extends ConcurrentLinkedQueue<E>
        implements AutoCloseable
        {
        public CloseableQueue()
            {
            }

        public CloseableQueue(E initial)
            {
            this();
            super.add(initial); // super to avoid needless read lock
            }

        /**
         * {@inheritDoc}
         *
         * @return true if added; false if the queue was closed
         */
        @Override
        public boolean add(E e)
            {
            try (Sentry sentry = f_gate.enter())
                {
                return isOpen() && super.add(e);
                }
            }

        // ---- Closeable interface -----------------------------------------

        /**
         * Close the queue preventing futher additions.  Note the caller should hold the write lock.
         */
        @Override
        public void close()
            {
            m_fClosed = true;
            }

        /**
         * Return true iff the queue has not been closed.
         *
         * @return true iff the queue has not been closed.
         */
        public boolean isOpen()
            {
            return !m_fClosed;
            }

        // ---- Object interface ----------------------------------------

        @Override
        public boolean equals(Object obj)
            {
            return this == obj;
            }

        @Override
        public int hashCode()
            {
            return System.identityHashCode(this);
            }

        // ---- data members --------------------------------------------

        /**
         * True if the queue has been closed.
         */
        protected boolean m_fClosed;

        /**
         * The gate protecting closing the queue.
         */
        protected final ThreadGate.NonReentrant f_gate = new ThreadGate.NonReentrant();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The monotonically increasing position counter, used for potentially contending associated adds.
     */
    protected final AtomicLong f_nPosCounter = new AtomicLong();

    /**
     * (Dirty) number of adds to the pile, used to estimate the position of "uncontended" adds.
     */
    protected long m_cAdds;

    /**
     * The size of the pile.
     */
    protected final LongAdder f_cValues = new LongAdder();

    /**
     * The number of values with non-null associations which are either pending release, i.e. available or polled.
     *
     * This is only maintained when there is a pending ALL node.
     */
    protected final AtomicLong f_cAssociatedPendingRelease = new AtomicLong();

    /**
     * A "queue" (ordered set) of nodes available to be polled.  A SkipList is superior to a tree based set here
     * not just because it is concurrent, but also because unlike a tree based set there is no rebalancing
     * to do, which is especially important as most adds to this queue occur in sort order which would trigger
     * constant rebalancing in a tree based map.
     */
    protected final ConcurrentSkipListSet<Node<T>> f_queueAvailablePriority = new ConcurrentSkipListSet<>();

    /**
     * Another queue of available values to be polled.  Unlike f_queueAvailablePriority, this queue is ordered
     * by the time nodes were made available rather then their position.  See {@link #addAvailable} and {@link #poll}
     * to see how the two available queues interact.
     */
    protected final ConcurrentLinkedQueue<T> f_queueAvailable = new ConcurrentLinkedQueue<>();

    /**
     * (Dirty) Count of the number of times poll has been called.
     */
    protected int m_cPolls;

    /**
     * (Dirty) The estimated position of the last Node added to the available queue.
     */
    protected long m_lPosAvailableLast;

    /**
     * A map of associations to queues of their currently deferred nodes.
     *
     * CloseableQueue is just insertion ordered (rather then strict position ordering).  Normally insertion and positional
     * ordering are the same, the only time when may not be is when two nodes with the same association are concurrently
     * added, and then there is a race to determine the queue order.  In such a case there is no correct ordering
     * anyway, and insertion ordering is cheaper to maintain.
     */
    protected final ConcurrentHashMap<A, CloseableQueue<Node<T>>> f_mapQueueDeferred = new ConcurrentHashMap<>();

    /**
     * The "queue" (ordered set) of nodes with values entangled with {@link #ASSOCIATION_ALL}, this includes any
     * younger association heads which become ready while there is an ASSOCIATION_ALL in the pile.
     *
     * A SkipList rather then Queue is used her as nodes may not be added in their position order.  This can happen
     * when there are multiple ALLs as well as associations from f_mapQueue which sit between those two in age.
     */
    protected final ConcurrentSkipListSet<Node<T>> f_queueDeferredAlls = new ConcurrentSkipListSet<>();

    /**
     * The position of the next ASSOCIATION_ALL node, or Long.MAX_VALUE if there is none in the pile.
     */
    protected long m_lPosNextAll = Long.MAX_VALUE;

    /**
     * ThreadGate governing access to the ASSOCIATION_ALL related portions of the pile.
     */
    protected ThreadGate.NonReentrant f_gateAll = new ThreadGate.NonReentrant();

    // ----- constants ------------------------------------------------------

    /**
     * The maximum unorderedness of polls.
     */
    protected static final int MAX_UNFAIRNESS_VARIANCE = Integer.parseInt(
            System.getProperty(ConcurrentAssociationPile.class.getCanonicalName() + ".maxVariance",
                    String.valueOf(Runtime.getRuntime().availableProcessors() * 4)));

    /**
     * A type-safe permanently empty marker queue.
     */
    protected final CloseableQueue<Node<T>> EMPTY_QUEUE = EMPTY_QUEUE_UNTYPED;

    /**
     * A permanently empty (and closed) marker queue.
     */
    protected final static CloseableQueue EMPTY_QUEUE_UNTYPED = new CloseableQueue()
        {
        @Override
        public boolean add(Object o)
            {
            return false;
            }

        @Override
        public int size()
            {
            return 0;
            }

        @Override
        public boolean isOpen()
            {
            return false;
            }
        };
    }

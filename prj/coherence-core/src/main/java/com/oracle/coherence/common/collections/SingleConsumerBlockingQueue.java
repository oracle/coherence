/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;

import com.oracle.coherence.common.base.Collector;
import com.oracle.coherence.common.base.Notifier;
import com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * SingleConsumerBlockingQueue is a BlockingQueue implementation which supports multiple producers but only a single
 * blocking consumer. The restriction of only allowing a single blocking consumer allows for potentially significant
 * performance optimizations. This queue does not support <tt>null</tt> elements.
 *
 * @param <V> the element type
 *
 * @author mf  2014.01.09
 */
public class SingleConsumerBlockingQueue<V>
    extends ConcurrentLinkedQueue<V>
    implements BlockingQueue<V>
    {
    // ----- SingleConsumerBlockingQueue interface --------------------------

    /**
     * Return a Collector accessor for this BlockingQueue.  This allows for
     * insertion without notification.  When using this style insertion the
     * caller must ultimately {@link Collector#flush flush} the collector to
     * ensure notification.
     *
     * @return the collector
     */
    public Collector<V> getCollector()
        {
        return f_collector;
        }

    /**
     * Return a cooperative Collector accessor for this BlockingQueue.  This allows
     * for insertion without notification.  When using this style insertion the
     * caller must ultimately {@link Collector#flush flush} the queue via this
     * collector or via an explicit call to {@link SingleWaiterCooperativeNotifier#flush}
     * to ensure notification.  The primary benefit of using the cooperative collector
     * approach versus the {@link #getCollector()} is the cooperative collector flushing
     * can be done by a single call to the static {@link SingleWaiterCooperativeNotifier#flush}
     * method.
     *
     * @return the collector
     */
    public Collector<V> getCooperativeCollector()
        {
        return f_collectorCooperative;
        }

    // ----- BlockingQueue interface ----------------------------------------

    @Override
    public boolean add(V value)
        {
        if (value == null)
            {
            throw new NullPointerException();
            }

        super.add(value);
        f_collector.flush();
        return true;
        }

    @Override
    public boolean offer(V value)
        {
        if (value == null)
            {
            throw new NullPointerException();
            }

        super.offer(value);
        f_collector.flush();
        return true;
        }

    @Override
    public void put(V value)
            throws InterruptedException
        {
        add(value);
        }

    @Override
    public boolean offer(V e, long timeout, TimeUnit unit)
            throws InterruptedException
        {
        return add(e); // since we don't have a size constraint add cannot fail so we can ignore timeout
        }

    @Override
    public V take()
            throws InterruptedException
        {
        V value;
        while ((value = poll()) == null)
            {
            f_notifier.await();
            }
        return value;
        }

    @Override
    public V poll(long timeout, TimeUnit unit)
            throws InterruptedException
        {
        V e = poll();
        if (e == null)
            {
            f_notifier.await(unit.toMillis(timeout));
            e = poll();
            }
        return e;
        }

    @Override
    public int remainingCapacity()
        {
        return Integer.MAX_VALUE;
        }

    @Override
    public int drainTo(Collection<? super V> c)
        {
        if (c == this)
            {
            throw new IllegalArgumentException();
            }

        int cV = 0;
        for (V value; (value = poll()) != null; ++cV)
            {
            c.add(value);
            }

        return cV;
        }

    @Override
    public int drainTo(Collection<? super V> c, int maxElements)
        {
        if (c == this)
            {
            throw new IllegalArgumentException();
            }

        int cV = 0;
        for (V value; cV < maxElements && (value = poll()) != null; ++cV)
            {
            c.add(value);
            }

        return cV;
        }

    @Override
    public boolean addAll(Collection<? extends V> c)
        {
        if (c.contains(null))
            {
            throw new NullPointerException();
            }

        super.addAll(c);
        f_collector.flush();
        return false;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The notifier used to signal waiting threads when entries are added.
     */
    protected final Notifier f_notifier = new SingleWaiterCooperativeNotifier();

    /**
     * The internal Collector for this Queue, allowing for add without signal.
     */
    protected final Collector<V> f_collector = new Collector<V>()
        {
        @Override
        public void add(V value)
            {
            SingleConsumerBlockingQueue.super.offer(value);
            }

        @Override
        public void flush()
            {
            f_notifier.signal();
            SingleWaiterCooperativeNotifier.flush();
            }
        };

    /**
     * The internal Cooperative Collector for this Queue, allowing for external flushing via the SWCN.
     */
    protected final Collector<V> f_collectorCooperative = new Collector<V>()
        {
        @Override
        public void add(V value)
            {
            SingleConsumerBlockingQueue.super.offer(value);
            f_notifier.signal();
            }

        @Override
        public void flush()
            {
            // Note: as per the contract of getCooperativeCollector, this method cannot contain more
            // then a call to SWCN.flush().  More specifically a call to SWCN.flush() needs to be
            // sufficient to be called in place of this method.
            SingleWaiterCooperativeNotifier.flush();
            }
        };
    }

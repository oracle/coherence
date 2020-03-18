/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus.util;

import com.oracle.coherence.common.base.Pollable;
import com.oracle.coherence.common.net.exabus.Event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * A Queue based PollingEventCollector implementation.
 * <p>
 * This implementation will internally queue all {@link #add added} events, and return them only when they are
 * {@link #poll polled} for.
 * </p>
 *
 * @author mf 02.18.2013
 */
public class QueueingEventCollector
    extends AbstractPollingEventCollector
    implements Pollable<Event>
    {
    // ----- QueueingEventCollector interface --------------------------------

    @Override
    public Event poll(long timeout, TimeUnit unit)
            throws InterruptedException
        {
        BusProcessor processor = m_busProcessor;
        if (processor == null)
            {
            return f_queue.poll(timeout, unit);
            }
        else
            {
            Event event = f_queue.poll();
            if (event == null)
                {
                processor.poll(timeout, unit);
                event = f_queue.poll();
                }
            return event;
            }
        }

    @Override
    public Event poll()
        {
        Event        event     = f_queue.poll();
        BusProcessor processor = m_busProcessor;
        if (processor == null)
            {
            return event;
            }
        else if (event == null)
            {
            try
                {
                processor.poll(0, TimeUnit.MILLISECONDS);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            event = f_queue.poll();
            }

        return event;
        }

    // ----- Collector interface --------------------------------------------

    @Override
    public void add(Event event)
        {
        f_queue.add(event);
        }

    /**
     * In the case of a {@link #bind bound} collector, the BusProcessor will be
     * {@link BusProcessor#cancel() canceled} as part
     * of this operation.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void flush()
        {
        BusProcessor processor = m_busProcessor;
        if (processor != null)
            {
            // in case the bus also emits async events (outside of while polling is blocked), it would
            // be best to terminate the poll to allow the events to be processed asap
            processor.cancel();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Queue of polled events, for non-PollBus based implementations
     */
    protected final BlockingQueue<Event> f_queue = new LinkedBlockingQueue<Event>();
    }

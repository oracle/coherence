/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus.util;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Hasher;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.exabus.Bus;
import com.oracle.coherence.common.net.exabus.Depot;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.Event;
import com.oracle.coherence.common.net.exabus.MemoryBus;
import com.oracle.coherence.common.net.exabus.MessageBus;


import com.oracle.coherence.common.internal.util.Histogram;
import com.oracle.coherence.common.internal.util.ScaledHistogram;

import com.oracle.coherence.common.net.SSLSettings;

import com.oracle.coherence.common.base.Collector;
import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.base.Factory;
import com.oracle.coherence.common.base.Notifier;
import com.oracle.coherence.common.base.Pollable;
import com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier;

import com.oracle.coherence.common.collections.SingleConsumerBlockingQueue;

import com.oracle.coherence.common.internal.Platform;
import com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver;

import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.BufferManagers;
import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.io.BufferSequenceInputStream;
import com.oracle.coherence.common.io.BufferSequenceOutputStream;
import com.oracle.coherence.common.io.Buffers;
import com.oracle.coherence.common.io.MultiBufferSequence;
import com.oracle.coherence.common.io.SingleBufferSequence;

import com.oracle.coherence.common.net.SocketSettings;
import com.oracle.coherence.common.net.exabus.spi.Driver;

import com.oracle.coherence.common.util.Bandwidth;
import com.oracle.coherence.common.util.Bandwidth.Rate;
import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.MemorySize;

import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketOptions;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * MessageBusTest is an application for testing the performance characteristics
 * of MessageBus implementations and the network on which they operate.
 *
 * @author mf 2010.12.14
 */
public class MessageBusTest
    {
    // ----- inner class: Receipt -------------------------------------------

    /**
     * Receipt object used in the test.
     */
    public static class Receipt
        implements Disposable
        {
        public Receipt(long ldtNanos, Disposable garbage)
            {
            m_ldtNanos = ldtNanos;
            m_garbage  = garbage;
            }

        public long getTimestampNanos()
            {
            return m_ldtNanos;
            }

        @Override
        public void dispose()
            {
            Disposable garbage = m_garbage;
            if (garbage != null)
                {
                m_garbage = null;
                garbage.dispose();
                }
            }

        // ----- data members ------------------------------------------

        long m_ldtNanos;
        Disposable m_garbage;
        }

    // ----- inner class: StampedEvent --------------------------------------

    /**
     * StampedEvent adds a nano-resolution timestamp to events at the time
     * of construction.
     */
    public static class StampedEvent
            implements Event
        {
        public StampedEvent(Event evt)
            {
            m_evt = evt;
            m_ldtNanos = System.nanoTime();
            }

        @Override
        public Type getType()
            {
            return m_evt.getType();
            }

        @Override
        public EndPoint getEndPoint()
            {
            return m_evt.getEndPoint();
            }

        @Override
        public Object getContent()
            {
            return m_evt.getContent();
            }

        @Override
        public Object dispose(boolean fTakeContent)
            {
            return m_evt.dispose(fTakeContent);
            }

        @Override
        public void dispose()
            {
            m_evt.dispose();
            }

        public long getTimestampNanos()
            {
            return m_ldtNanos;
            }

        // ----- data members ------------------------------------------

        protected final Event m_evt;
        protected long m_ldtNanos;
        }

    // ----- inner class: EventProcessor ------------------------------------

    /**
     * EventProcessor is the basis for a thread which will handle bus event
     * streams.
     */
    public static class EventProcessor
            extends Thread
            implements Runnable
        {
        // ----- constructors -------------------------------------------

        /**
         * Construct an EventProcessor.
         *
         * @param bus             the Bus for this processor
         * @param setPeer         the peers to transmit to
         * @param setReady        the set that is ready to be transmitted to
         * @param aTransmitter    the associated transmitters for this bus
         * @param cbsIn           the target inbound data rate
         * @param nFlushOn        the number of response messages to send before flushing
         * @param fBacklogGlobal  an AtomicInteger to serve as a flag; non-zero value
         *                        if the bus has declared a global backlog
         */
        public EventProcessor(Bus bus, Set<EndPoint> setPeer, Set<EndPoint> setReady,
                              Transmitter[] aTransmitter, long cbsIn, int nFlushOn, AtomicInteger fBacklogGlobal)
            {
            super("EventProcessor(" + bus.getLocalEndPoint() + ")");

            m_bus            = bus;
            m_busMsg         = bus instanceof MessageBus ? (MessageBus) bus : null;
            f_fBacklogGlobal = fBacklogGlobal;
            m_setPeer        = setPeer;
            m_setReady       = setReady;
            m_cbsIn          = cbsIn;
            m_nFlushOn       = nFlushOn;
            m_aTransmitter   = aTransmitter;
            m_abChunk        = new byte[s_cbChunk];
            }

        // ----- EventProcessor interface -------------------------------

        /**
         * Return the Bus used by this processor.
         *
         * @return  the bus
         */
        public Bus getBus()
            {
            return m_bus;
            }

        /**
         * Return the MessageBus used by this processor if any.
         *
         * @return  the message bus
         */
        public MessageBus getMessageBus()
            {
            return m_busMsg;
            }

        /**
         * Return the set of peers this processor is configured to transmit
         * to
         *
         * @return the peer set.
         */
        public Set<EndPoint> getPeers()
            {
            return m_setPeer;
            }

        /**
         * Return the number of bytes received.
         *
         * @return the number of bytes received
         */
        public long getBytesIn()
            {
            return m_cbIn;
            }

        /**
         * Return the number of bytes sent.
         *
         * @return the number of bytes sent
         */
        public long getBytesOut()
            {
            return m_cbOut;
            }

        /**
         * Return the number of messages received.
         *
         * @return the number of messages received
         */
        public long getMessagesIn()
            {
            return m_cMsgIn;
            }

        /**
         * Return the number of Messages sent.
         *
         * @return the number of Messages sent
         */
        public long getMessagesOut()
            {
            return m_cMsgOut;
            }

        /**
         * Return the number of receipt samples
         *
         * @return the number of receipt samples
         */
        public long getReceiptSamples()
            {
            return m_cReceiptTimings;
            }

        /**
         * Return the cumulative receipts time.
         *
         * @return the cumulative receipts time.
         */
        public long getReceiptNanos()
            {
            return m_cReceiptsNanos;
            }

        /**
         * Return the number of returned receipts.
         *
         * @return the number of returned receipts
         */
        public long getReceiptsIn()
            {
            return m_cReceiptsIn;
            }

        /**
         * Return the number of received responses.
         *
         * @return the number of received responses
         */
        public long getResponsesIn()
            {
            return m_cResponseIn;
            }

        /**
         * Return the cumulative response time.
         *
         * @return the cumulative response time.
         */
        public long getResponseNanos()
            {
            return m_cResponseNanos;
            }

        /**
         * Return they latency histogram.
         *
         * @return the latency Histogram
         */
        public Histogram getResponseLatencyHistogram()
            {
            return f_histLatency;
            }

        /**
         * Return the number of local backlog events received.
         *
         * @return the number of local backlog events received
         */
        public long getLocalBacklogEvents()
            {
            return m_cBacklogEventsLocal;
            }

        /**
         * Return the total duration of local backlogs.
         *
         * @return the local backlog duration
         */
        public long getLocalBacklogMillis()
            {
            return m_cBacklogMillisLocal;
            }

        /**
         * Return the number of remote backlog events received.
         *
         * @return the number of remote backlog events received
         */
        public long getRemoteBacklogEvents()
            {
            return m_cBacklogEventsRemote;
            }

        /**
         * Return the number of connections.
         *
         * @return the connection count
         */
        public long getConnectionCount()
            {
            return m_cConnections;
            }

        /**
         * Handle an inbound message.
         *
         * @param event  the event
         *
         * @return true if a flush is required
         *
         * @throws IOException if an IO error occurs
         */
        protected boolean onMessage(Event event)
                throws IOException
            {
            BufferSequence bufseq = (BufferSequence) event.getContent();

            DataInput in = new BufferSequenceInputStream(bufseq);

            // format:
            // 4B transmitter ID
            // 1B msg type
            // 8B timestamp (or zero)
            // 8B payload size
            // payload
            long cbMessage = bufseq.getLength();

            m_cbIn += cbMessage;

            long    cMsgIn    = m_cMsgIn++;
            int     nId       = in.readInt();
            boolean fResp     = in.readBoolean();
            long    ldtNanos  = in.readLong();
            long    cbPayload = in.readLong();

            if (MSG_HEADER_SIZE + cbPayload != cbMessage)
                {
                s_cErrors.incrementAndGet();
                System.err.println("unexpecteded message size " + cbMessage + " rather then " +
                        (MSG_HEADER_SIZE + cbPayload) + ", msg=" + Buffers.toString(bufseq) + " from " + event);
                throw new IllegalStateException("unexpected message size in "
                        + event);
                }

            if (s_fVerbose && (cMsgIn % 10000) == 0)
                {
                // update thread-name to show movement
                Thread thread = Thread.currentThread();
                String sName  = thread.getName();
                int    of     = sName.lastIndexOf('#');

                thread.setName(sName.substring(
                        0, of == -1 ? sName.length() : of) +
                        "#" + cMsgIn);
                }

            // TODO: validate message ordering, this could be a bit problematic
            // since the test allows multiple threads to send to the same
            // peer over the same local bus, so we'd need to improve the
            // test's message header

            // TODO: optionally verify contents to account for
            int cbChunk = s_cbChunk;
            switch (cbChunk)
                {
                default:
                    for (; cbPayload >= cbChunk; cbPayload -= cbChunk)
                        {
                        in.readFully(m_abChunk);
                        }
                    // fall through

                case 8:
                    for (; cbPayload >= 8; cbPayload -= 8)
                        {
                        in.readLong();
                        }
                    // fall through

                case 4:
                    for (; cbPayload >= 4; cbPayload -= 4)
                        {
                        in.readInt();
                        }
                    // fall through

                case 2:
                    for (; cbPayload >= 2; cbPayload -= 2)
                        {
                        in.readShort();
                        }
                    // fall through

                case 1:
                    for (; cbPayload >= 1; cbPayload -= 1)
                        {
                        in.readByte();
                        }
                    // fall through

                case 0:
                    // read nothing
                    break;
                }

            EndPoint epResponse = null;
            boolean  fFlush     = false;

            if (fResp)
                {
                // this is an response
                if (s_fRelay)
                    {
                    // server receiving response to relayed request
                    if (s_fBlock)
                        {
                        RelayResponse resp = s_mapRelayResponse.remove(nId);
                        epResponse = resp.peer;
                        nId        = resp.nId;
                        }
                    // else; response was sent immediately during relay
                    }
                else // client awaiting response
                    {
                    if (ldtNanos > 0)
                        {
                        ++m_cResponseIn;
                        long cNanos = System.nanoTime() - ldtNanos;
                        m_cResponseNanos += cNanos;
                        f_histLatency.addSample((int) (cNanos / 1000));

                        if (cNanos > m_cResponseNanosMax)
                            {
                            m_cResponseNanosMax = cNanos;
                            }
                        if (cNanos < m_cResponseNanosMin)
                            {
                            m_cResponseNanosMin = cNanos;
                            }
                        }

                    Transmitter tx = m_aTransmitter[nId & 0x0FFFF]; // upper 16 bits are just a request counter for the thread

                    if (s_fBlock)
                        {
                        tx.signalResult(nId >>> 16);
                        }
                    }
                }
            else if (s_fRelay && !m_setPeer.contains(event.getEndPoint()))
                {
                // relay message to a configured peer

                Iterator<EndPoint> iterRelay = m_iterPeerRelay;
                EndPoint           epRelay;
                if (iterRelay == null || !iterRelay.hasNext())
                    {
                    iterRelay = m_iterPeerRelay = m_setPeer.iterator();
                    }
                epRelay = iterRelay.next();

                if (ldtNanos != 0)
                    {
                    if (s_fBlock)
                        {
                        // defer response like coherence backups
                        RelayResponse resp = new RelayResponse(event.getEndPoint(), nId);
                        nId = s_atomicRelayId.incrementAndGet();
                        s_mapRelayResponse.put(nId, resp);
                        }
                    else
                        {
                        // relay plus immediate response (like async backups)
                        epResponse = event.getEndPoint();
                        }
                    }

                BufferSequence seqRelay = getMessage(nId, /*fResp*/ false, ldtNanos);

                ++m_cMsgOut;
                m_cbOut += seqRelay.getLength();

                // TODO: consider option to send reply upon delivery confirmation?
                getMessageBus().send(epRelay, seqRelay, s_fReceipts ? new Receipt(0, seqRelay) : null);
                fFlush = true;
                }
            else if (ldtNanos != 0)
                {
                // this is a non-relay request
                epResponse = event.getEndPoint();
                }

            if (epResponse != null)
                {
                // send a response message
                if (s_fPrompt)
                    {
                    System.out.println("Press ENTER to send messge to " + event.getEndPoint());
                    System.in.read();
                    }

                BufferSequence seqresponse = getMessage(nId, /*fResp*/ true, ldtNanos);

                ++m_cMsgOut;
                m_cbOut += seqresponse.getLength();

                getMessageBus().send(epResponse, seqresponse, s_fReceipts ? new Receipt(0, seqresponse) : null);
                fFlush = true;
                }

            return fFlush;
            }

        /**
         * Process an event
         *
         * @param event  the event to process
         *
         * @return true if a flush is required
         */
        public boolean onEvent(Event event)
            {
            try
                {
                Bus           bus             = m_bus;
                EndPoint      bindEp          = bus.getLocalEndPoint();
                Set<EndPoint> setPeer         = m_setPeer;
                Set<EndPoint> setReady        = m_setReady;
                boolean       fFlush          = false;

                EndPoint ep = event.getEndPoint();

                switch (event.getType())
                    {
                    case OPEN:
                    case CLOSE:
                        System.err.println(event);
                        break;

                    case CONNECT:
                        if (!s_fSingleUseConnection)
                            {
                            System.err.println(event + " on " + bindEp);
                            }
                        ++m_cConnections;
                        // fall-through to BACKLOG_NORMAL

                    case BACKLOG_NORMAL:
                        if (s_fFlowControl || event.getType() == Event.Type.CONNECT)
                            {
                            if (ep == null)
                                {
                                synchronized (f_fBacklogGlobal)
                                    {
                                    if (!f_fBacklogGlobal.compareAndSet(1, 0))
                                        {
                                        System.err.println("received out of order event " + event + " on " + bindEp);
                                        s_cErrors.incrementAndGet();
                                        }
                                    f_fBacklogGlobal.notifyAll();
                                    }
                                }
                            else if (ep == bindEp)
                                {
                                long ldtStart = m_ldtBacklogLocalStart;
                                if (ldtStart == 0)
                                    {
                                    System.err.println("received out of order event " + event + " on " + bindEp);
                                    s_cErrors.incrementAndGet();
                                    }
                                else
                                    {
                                    m_cBacklogMillisLocal += System.currentTimeMillis() - ldtStart;
                                    m_ldtBacklogLocalStart = 0;
                                    }
                                }
                            else if (setPeer.contains(ep))
                                {
                                boolean fAdded;
                                if (setReady.isEmpty())
                                    {
                                    synchronized (setReady)
                                        {
                                        fAdded = setReady.add(ep);
                                        setReady.notifyAll();
                                        }
                                    }
                                else
                                    {
                                    fAdded = setReady.add(ep);
                                    }

                                if (!fAdded)
                                    {
                                    System.err.println("received out of order event " + event + " on " + bindEp);
                                    s_cErrors.incrementAndGet();
                                    }
                                }
                            }
                        break;

                    case BACKLOG_EXCESSIVE:
                        if (s_fFlowControl)
                            {
                            if (ep == null)
                                {
                                ++m_cBacklogEventsRemote;
                                if (!f_fBacklogGlobal.compareAndSet(0, 1))
                                    {
                                    // event appear to be out of sequence
                                    System.err.println("received out of order event " + event + " on " + bindEp);
                                    s_cErrors.incrementAndGet();
                                    }
                                }
                            else if (bindEp == ep)
                                {
                                ++m_cBacklogEventsLocal;
                                if (m_ldtBacklogLocalStart != 0)
                                    {
                                    System.err.println("received out of order event " + event + " on " + bindEp);
                                    s_cErrors.incrementAndGet();
                                    }
                                m_ldtBacklogLocalStart = System.currentTimeMillis();
                                }
                            else
                                {
                                ++m_cBacklogEventsRemote;
                                if (!setReady.remove(ep) && setPeer.contains(ep))
                                    {
                                    // event appear to be out of sequence
                                    System.err.println("received out of order event " + event + " on " + bindEp);
                                    s_cErrors.incrementAndGet();
                                    }
                                // else; not a peer we're sending to; ordering not tracked by test
                                }
                            }

                        break;

                    case DISCONNECT:
                        if (!s_fSingleUseConnection)
                            {
                            System.err.println(event + " on " + bindEp);
                            Throwable t = (Throwable) event.getContent();
                            if (t != null && (!(t instanceof IOException) || s_fVerbose))
                                {
                                t.printStackTrace(System.err);
                                }
                            }
                        bus.release(ep);
                        break;

                    case RELEASE:
                        if (!s_fSingleUseConnection)
                            {
                            System.err.println(event + " on " + bindEp);
                            }
                        --m_cConnections;
                        synchronized (setReady)
                            {
                            setReady.remove(ep);
                            setReady.notifyAll();
                            }
                        break;

                    case MESSAGE:
                        fFlush = onMessage(event);
                        break;

                    case RECEIPT:
                        Receipt receipt      = (Receipt) event.getContent();
                        long    ldtNanosSent = receipt.getTimestampNanos();
                        ++m_cReceiptsIn;
                        if (ldtNanosSent != 0)
                            {
                            long cNanos = (s_fPollingCollector ? System.nanoTime() : ((StampedEvent) event).getTimestampNanos()) - ldtNanosSent;
                            m_cReceiptsNanos += cNanos;
                            ++m_cReceiptTimings;

                            if (m_busMsg == null)
                                {
                                f_histLatency.addSample((int) (cNanos / 1000L));
                                }
                            }

                        receipt.dispose();
                        break;

                    default:
                        System.err.println(event + " on " + bindEp);
                        break;
                    }

                event.dispose();
                return fFlush;
                }
            catch (Throwable e)
                {
                s_cErrors.incrementAndGet();
                System.err.println("fatal error after receiving " + m_cMsgIn + " messages");
                e.printStackTrace(System.err);
                throw new IllegalStateException(e);
                }
            }

        /**
         * Add an event for the processor to handle.
         *
         * @param event  the event to process
         *
         * @return  true if a flush is required
         */
        public boolean add(Event event)
            {
            BlockingQueue<Event> queue = m_queue;
            if (queue == null)
                {
                // This synchronization should be mostly uncontended. Contention is only possible
                // if we are running in reentrant mode, have multiple peers sending to us, and
                // they bus impl uses different threads to deliver their events, and the
                // DemultiplexingCollector happens to hash them to the same EventProcessor. The
                // level of contention can be roughly controlled via -rxThreads, setting to a
                // large negative value. Note that the default is such a value.

                // Note: we break the event stream up into events related to transmit and receive
                // to allow this independent streams to be processed concurrently, this is important
                // for buses which may emit these events from different threads, as we don't want
                // the test to add unecessary contention
                switch (event.getType())
                    {
                    case BACKLOG_EXCESSIVE:
                    case BACKLOG_NORMAL:
                        if (event.getEndPoint() == m_bus.getLocalEndPoint())
                            {
                            synchronized (f_syncRxEvents)
                                {
                                return onEvent(event);
                                }
                            }
                        // else; fall through

                    case RECEIPT:
                        synchronized (f_syncTxEvents)
                            {
                            return onEvent(event);
                            }

                    case MESSAGE:
                        synchronized (f_syncRxEvents)
                            {
                            return onEvent(event);
                            }

                    default:
                        synchronized (f_syncTxEvents)
                            {
                            synchronized (f_syncRxEvents)
                                {
                                return onEvent(event);
                                }
                            }
                    }
                }
            else
                {
                queue.add(event);
                return false;
                }
            }


        // ----- Runnable interface -------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
            {
            Bus                        bus       = m_bus;
            final BlockingQueue<Event> queue     = m_queue;
            int                  nFlushOn        = m_nFlushOn;
            int                  cFlushEvent     = 0;
            long                 cbsInTarget     = m_cbsIn;
            long                 cbEval          = cbsInTarget / 8;
            long                 ldtLast         = 0;
            long                 cbInLast        = 0;
            long                 cThrottleMillis = 1;
            int                  nThrottle       = 1000; // random
            Pollable<Event>      poll            = s_fPollingCollector
                    ? (QueueingEventCollector) bus.getEventCollector()
                    : new Pollable<Event>()
                    {
                    @Override
                    public Event poll()
                        {
                        return queue.poll();
                        }

                    @Override
                    public Event poll(long timeout, TimeUnit unit)
                            throws InterruptedException
                        {
                        return timeout == Long.MAX_VALUE
                                ? queue.take()
                                : queue.poll(timeout, unit);
                        }
                    };

            try
                {
                for (int i = 0; ; ++i)
                    {
                    Event event = poll.poll();
                    while (event == null)
                        {
                        if (cFlushEvent > 0)
                            {
                            cFlushEvent = 0;
                            bus.flush();
                            }
                        SingleWaiterCooperativeNotifier.flush();

                        event = poll.poll(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                        }

                    if (onEvent(event) && ++cFlushEvent > nFlushOn)
                        {
                        cFlushEvent = 0;
                        bus.flush();
                        }

                    if (cbsInTarget > 0)
                        {
                        // throttle the input rate; this allows simulating
                        // a transmitter which outpaces the receiver, we want
                        // to see that the transmitter will detect our backlog
                        // and throttle itself accordingly
                        if ((i % nThrottle) == 0)
                            {
                            if (cFlushEvent > 0)
                                {
                                cFlushEvent = 0;
                                bus.flush();
                                }
                            Blocking.sleep(cThrottleMillis);
                            }
                        long cbIn    = m_cbIn;
                        long cbDelta = cbIn - cbInLast;

                        if (cbInLast == 0 && ldtLast == 0)
                            {
                            // start the clock
                            ldtLast = System.currentTimeMillis();
                            }
                        else if (cbDelta > cbEval)
                            {
                            long   ldtNow  = System.currentTimeMillis();
                            long   cMillis = Math.max(1, ldtNow - ldtLast);
                            double cbs     = (cbDelta * 1000) / cMillis;
                            double dfl     = cbsInTarget / cbs;

                            nThrottle = Math.max(1, (int) Math.round(nThrottle * dfl));
                            int nThrottleNew = (int) Math.round(nThrottle * dfl);
                            if (nThrottleNew == 0)
                                {
                                nThrottleNew = 1;
                                ++cThrottleMillis; // go slower still
                                }
                            else if (nThrottleNew == nThrottle)
                                {
                                if (dfl > 1.01)
                                    {
                                    ++cThrottleMillis;
                                    }
                                else if (dfl < 0.09)
                                    {
                                    --cThrottleMillis;
                                    }
                                }

                            cbInLast = cbIn;
                            ldtLast  = ldtNow;
                            i        = 0;
                            }
                        }

                    }
                }
            catch (Exception e)
                {
                throw new IllegalStateException(e);
                }
            }

        // ----- Thread interface ---------------------------------------

        @Override
        public void start()
            {
            m_queue = new SingleConsumerBlockingQueue<Event>();

            super.start();
            }


        /**
         * Marker class for profiling.
         */
        public static class TxEventSynchronizer
            {
            };

        /**
         * Marker class for profiling.
         */
        public static class RxEventSynchronizer
            {
            };

        public static class RelayResponse
            {
            public RelayResponse(EndPoint peer, int nId)
                {
                this.peer = peer;
                this.nId  = nId;
                }

            EndPoint peer;
            int      nId;
            }

        // ----- data members -------------------------------------------

        /**
         * The Bus for this processor.
         */
        protected final Bus m_bus;

        protected final MessageBus m_busMsg;

        /**
         * Monitor protecting processing of transmit related events.
         */
        protected final Object f_syncTxEvents = new TxEventSynchronizer();

        /**
         * Monitor protecting processing of receive related events.
         */
        protected final Object f_syncRxEvents = new RxEventSynchronizer();

        /**
         * AtomicInteger that serves as a flag; non-zero if the bus has
         * declared a global backlog.
         */
        protected final AtomicInteger f_fBacklogGlobal;

        /**
         * The event queue, or null for reentrant processing.
         */
        protected BlockingQueue<Event> m_queue;

        /**
         * Set of peer's to transmit to.
         */
        protected final Set<EndPoint> m_setPeer;

        /**
         * Iterator over the peer set for use as a relay
         */
        protected Iterator<EndPoint> m_iterPeerRelay;

        /**
         * Used by a relay to identify which client a response needs to be sent to.
         */
        protected final static Map<Integer, RelayResponse> s_mapRelayResponse = new ConcurrentHashMap<Integer, RelayResponse>();

        /**
         * Relay message ID generator.
         */
        protected final static AtomicInteger s_atomicRelayId = new AtomicInteger();

        /**
         * Subset of m_setPeer that ready to be transmitted to, i.e. connected
         * and not backlogged.
         */
        protected final Set<EndPoint> m_setReady;

        /**
         * The target inbound data rate.
         */
        protected final long m_cbsIn;

        /**
         * The number of messages to send before flushing.
         */
        protected final int m_nFlushOn;

        /**
         * The number of transmit threads to run.
         */
        protected final Transmitter[] m_aTransmitter;

        /**
         * The number of open connections.
         */
        protected long m_cConnections;

        /**
         * The number of bytes read.
         */
        protected long m_cbIn;

        /**
         * The number of received messages.
         */
        protected long m_cMsgIn;

        /**
         * The total number of received responses.
         */
        protected long m_cResponseIn;

        /**
         * The maximum response time.
         */
        protected long m_cResponseNanosMax = -1;

        /**
         * The minimum response time.
         */
        protected long m_cResponseNanosMin = Long.MAX_VALUE;

        /**
         * Histogram of all latencies.
         */
        protected final Histogram f_histLatency = makeLatencyHistogram();
        /**
         * The total RTT time for all receied responses.
         */
        protected long m_cResponseNanos;

        /**
         * The total number of received timed receipts.
         */
        protected long m_cReceiptTimings;

        /**
         * The total RTT time for all received receipts.
         */
        protected long m_cReceiptsNanos;

        /**
         * The total number of returned receipts.
         */
        protected long m_cReceiptsIn;

        /**
         * The number of bytes written.
         */
        protected long m_cbOut;

        /**
         * The number of sent messages.
         */
        protected long m_cMsgOut;

        /**
         * The total number of local backlog events received.
         */
        protected long m_cBacklogEventsLocal;

        /**
         * The time at which the current local backlog condition started, or zero if there is none.
         */
        protected long m_ldtBacklogLocalStart;

        /**
         * The total number of milliseconds for which the EventProcessor was
         * locally backlogged.
         */
        protected long m_cBacklogMillisLocal;

        /**
         * The total number of remote backlog events received.
         */
        protected long m_cBacklogEventsRemote;

        /**
         * to be used for chunked reads, we don't reuse s_abChunk in order to avoid introducing
         * false memory contention
         */
        protected final byte[] m_abChunk;
        }

    // ----- inner class: DemultiplexingCollector ---------------------------

    public static class DemultiplexingCollector implements Collector<Event>
        {
        // ----- constructors -------------------------------------------

        /**
         * Construct a DemultiplexingCollector which dispatches to the
         * specified queues.
         *
         * @param bus           the bus associated with all the processors
         * @param aProcessor    the queues to dispatch other events to
         */
        public DemultiplexingCollector(Bus bus, EventProcessor[] aProcessor)
            {
            m_bus = bus;
            m_aProcessor = aProcessor;
            m_acbReceived = new AtomicLong[aProcessor.length];

            for (int i = 0, c = m_acbReceived.length; i < c; ++i)
                {
                m_acbReceived[i] = new AtomicLong();
                }
            }

        // ----- Collector interface ------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void add(Event event)
            {
            EndPoint pointSrc = event.getEndPoint();
            int nHash = pointSrc == null
                    ? 0
                    : pointSrc.hashCode();

            switch (event.getType())
                {
                case RECEIPT:
                    if (((Receipt) event.getContent()).getTimestampNanos() != 0)
                        {
                        // the event contains a timestamped receipt, insert the
                        // corresponding "end" receipt here as it is the first point
                        // at which the application code sees the response.
                        event = new StampedEvent(event);
                        }
                    break;

                case MESSAGE:
                    m_acbReceived[Hasher.mod(nHash, m_acbReceived.length)]
                            .addAndGet(((BufferSequence) event.getContent()).getLength());
                    break;

                default:
                    break;
                }

            if (m_aProcessor[Hasher.mod(nHash, m_aProcessor.length)].add(event))
                {
                m_fFlush = true;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush()
            {
            if (m_fFlush)
                {
                m_fFlush = false;
                m_bus.flush();
                }

            SingleWaiterCooperativeNotifier.flush();
            }

        // ----- helpers ------------------------------------------------

        /**
         * Return the total number of received bytes.
         *
         * @return the total number of received bytes
         */
        public long getReceivedBytes()
            {
            long cb = 0;
            for (int i = 0, c = m_acbReceived.length; i < c; ++i)
                {
                cb += m_acbReceived[i].get();
                }
            return cb;
            }


        // ----- data members -------------------------------------------

        /**
         * The Bus associated with the EventProcessors.
         */
        protected final Bus m_bus;

        /**
         * The array of processors to dispatch to.
         */
        protected final EventProcessor[] m_aProcessor;

        /**
         * Array containing received message size totals.
         */
        protected final AtomicLong[] m_acbReceived;


        /**
         * True if the collector requires a bus.flush to be performed.
         */
        protected boolean m_fFlush;
        }


    // ----- inner class: Transmitter ---------------------------------------

    /**
     * A Transmitter is reponsible for sending messages on a bus to a series
     * of peers.
     */
    public static class Transmitter extends Thread
        {
        // ----- constructors -------------------------------------------

        /**
         * Construct a Transmitter.
         *
         * @param bus             the associated bus
         * @param nId             the transmitter id
         * @param setReady        the peers to send to
         * @param nFlushOn        the number of messages to send before flushing
         * @param fBacklogGlobal  an AtomicInteger to serve as a flag; non-zero value
         *                        if the bus has declared a global backlog
         * @param cbBacklog       the maximum tx backlog, or -1 for no limit
         *
         * @throws IOException if an IO error occurs
         */
        public Transmitter(
                Bus bus, int nId, Set<EndPoint> setReady, int nFlushOn, AtomicInteger fBacklogGlobal, long cbBacklog)
                throws IOException
            {
            super("Transmitter(" + bus.getLocalEndPoint() + ")");

            f_bus = bus;
            f_bufSeqCached = s_fCached
                    ? MessageBusTest.getMessage(nId, /*fResp*/ false, 0)
                    : null;
            f_aBufCached = s_fCached
                    ? f_bufSeqCached.getBuffers()
                    : null;
            f_fBacklogGlobal = fBacklogGlobal;
            f_nId = nId;
            f_setReady = setReady;
            f_nFlushOn = nFlushOn;
            f_cbTxMaxBacklog = cbBacklog;
            }

        // ----- Transmitter interface ----------------------------------

        private volatile Object   m_oResult;
        private final    Notifier f_notifier = new SingleWaiterCooperativeNotifier();

        public void signalResult(Object oResult)
            {
            long cPending = f_cPendingResponses.decrementAndGet();
            if (cPending == 0)
                {
                m_oResult = oResult;
                f_notifier.signal();
                }
            else if (cPending < 0)
                {
                throw new IllegalStateException();
                }
            }

        public Object awaitResult()
                throws InterruptedException
            {
            Object oResult = m_oResult;
            if (oResult == null)
                {
                if (AWAIT_SPIN_NANOS > 0)
                    {
                    for (long ldtEnd = System.nanoTime() + AWAIT_SPIN_NANOS;
                         oResult == null && System.nanoTime() < ldtEnd;
                         oResult = m_oResult) {}
                    }

                for (; oResult == null; oResult = m_oResult)
                    {
                    f_notifier.await();
                    }
                }

            m_oResult = null;
            return oResult;
            }

        /**
         * Reset the transmit rate.
         *
         * Note this may only be reset before starting the thread.
         *
         * @param cbs  the data rate
         */
        public void setTransmitRate(long cbs)
            {
            f_cbs = cbs;
            }

        /**
         * Return the number of bytes sent.
         *
         * @return the number of bytes sent
         */
        public long getBytesOut()
            {
            return m_cbOut;
            }

        /**
         * Return the number of Messages sent.
         *
         * @return the number of Messages sent
         */
        public long getMessagesOut()
            {
            return m_cMsgOut;
            }

        /**
         * Return the total number of milliseconds for which the transmitter
         * was blocked.
         *
         * @return the total blocked time
         */
        public long getRemoteBacklogMillis()
            {
            long ldtBacklogStart = m_ldtBacklogStart;
            long cMillisBacklog = m_cMillisBacklog;

            return ldtBacklogStart == 0 || m_cbOut == 0
                    ? cMillisBacklog
                    : cMillisBacklog + (System.currentTimeMillis() - ldtBacklogStart);
            }

        public class BacklogTrackingReceipt
                extends Receipt
            {
            public BacklogTrackingReceipt(long ldtNanos, Disposable garbage)
                {
                super(ldtNanos, garbage);
                }

            @Override
            public void dispose()
                {
                super.dispose();
                if (f_cbTxMaxBacklog != -1)
                    {
                    long cbBacklogPre = f_cbTxBacklog.getAndAdd(-s_cbMsgAvg);
                    long cbThreshold  = f_cbTxMaxBacklog / 3;
                    if (cbBacklogPre > cbThreshold && cbBacklogPre - s_cbMsgAvg < f_cbTxMaxBacklog)
                        {
                        f_notifierBacklog.signal();
                        }
                    }
                }
            }

        // ----- Runnable interface -------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
            {
            try
                {
                Bus bus = f_bus;

                MessageBus busMsg = null;
                MemoryBus busMem = null;

                if (bus instanceof MessageBus)
                    {
                    busMsg = (MessageBus) bus;
                    }
                else if (bus instanceof MemoryBus)
                    {
                    busMem = (MemoryBus) bus;
                    }

                Set<EndPoint> setReady = f_setReady;
                long cbsTarget = f_cbs;
                long cbEval = cbsTarget / 8;
                boolean fBlock = s_fBlock;
                long nFlushOn = Math.max(fBlock
                        ? 1
                        : 0, f_nFlushOn);
                AtomicInteger cPending = f_cPendingResponses;
                long lSeq = 0;

                long cThrottleMillis = 1;
                long nThrottle = Math.max(1, ((int) cbEval / (s_cbMsgMax - (s_cbMsgMax - s_cbMsgMin) / 2)) / 8);
                long ldtLast = System.currentTimeMillis();
                long cbThis = 0;
                long cSendThrottle = 0;
                long cMsgOut = 0;

                Iterator<EndPoint> iterPeer = setReady.iterator();

                while (true)
                    {
                    if (f_fBacklogGlobal.get() == 1)
                        {
                        long ldtStartWait = m_ldtBacklogStart = System.currentTimeMillis();
                        synchronized (f_fBacklogGlobal)
                            {
                            while (f_fBacklogGlobal.get() == 1)
                                {
                                Blocking.wait(f_fBacklogGlobal);
                                }
                            }
                        if (cbThis > 0)
                            {
                            m_cMillisBacklog += System.currentTimeMillis() - ldtStartWait;
                            }
                        }

                    if (!iterPeer.hasNext()) // iterator exhausted
                        {
                        iterPeer = setReady.iterator();

                        if (!iterPeer.hasNext())
                            {
                            // all members in backlog
                            bus.flush();
                            synchronized (setReady)
                                {
                                for (iterPeer = setReady.iterator(); !iterPeer.hasNext(); iterPeer = setReady
                                        .iterator())
                                    {
                                    long ldtStartWait = m_ldtBacklogStart = System.currentTimeMillis();
                                    Blocking.wait(setReady);
                                    m_ldtBacklogStart = 0;
                                    if (cbThis > 0)
                                        {
                                        m_cMillisBacklog += System.currentTimeMillis() - ldtStartWait;
                                        }
                                    }
                                }
                            }
                        }

                    // create and send message
                    BufferSequence bufseq = null;
                    try
                        {
                        if (fBlock)
                            {
                            cPending.incrementAndGet();
                            }

                        EndPoint peer = iterPeer.next();

                        if (s_fPrompt)
                            {
                            System.out.println("Press ENTER to send messge to " + peer);
                            System.in.read();
                            }

                        // determine if we will take a latency measurement
                        long ldtNanos = s_nLatencyFreq != 0 && (++lSeq % s_nLatencyFreq == 0)
                                ? System.nanoTime() // latency measurement
                                : fBlock
                                    ? -1 // blocking request
                                    : 0; // streaming with no latency measurement

                        bufseq = getMessage(ldtNanos);
                        long cb = bufseq.getLength();

                        Receipt receipt = s_fReceipts
                                ? new BacklogTrackingReceipt(Math.max(0, ldtNanos), bufseq == f_bufSeqCached ? null : bufseq)
                                : null;

                        if (busMsg != null)
                            {
                            busMsg.send(peer, bufseq, receipt);
                            }
                        else // RDMA test
                            {
                            if (s_fBlock)
                                {
                                // on MemoryBus use receipts as responses when in blocking mode
                                final Disposable garbage = receipt.m_garbage;
                                receipt.m_garbage = new Disposable()
                                    {
                                    @Override
                                    public void dispose()
                                        {
                                        signalResult(0);

                                        if (garbage != null)
                                            {
                                            garbage.dispose();
                                            }
                                        }
                                    };
                                }

                            long cbPeer = busMem.getCapacity(peer);
                            long lOffset = Hasher.mod(s_rand.nextLong(), (cbPeer - bufseq.getLength()));
                            switch ((int) busMem.getCapacity(null))
                                {
                                case 0:
                                    {
                                    if (cbPeer == 0) // test signaling
                                        {
                                        cb = 8;
                                        busMem.signal(peer, 1234, receipt);
                                        }
                                    else // test RDMA writes
                                        {
                                        busMem.write(peer, lOffset, bufseq, receipt);
                                        }
                                    break;
                                    }

                                case 8: // test ATOMICs
                                    {
                                    cb = 16;
                                    busMem.getAndAdd(peer, 0, 1, /*result*/ null, receipt);
                                    break;
                                    }

                                default: // test RDMA reads: TODO: read into hosted memory?
                                    {
                                    busMem.read(peer, lOffset, bufseq, receipt);
                                    break;
                                    }
                                }
                            }

                        cMsgOut = m_cMsgOut++;
                        cbThis += cb;
                        m_cbOut += cb;

                        if (f_cbTxMaxBacklog != -1 && f_cbTxBacklog.addAndGet(s_cbMsgAvg) > f_cbTxMaxBacklog)
                            {
                            bus.flush();
                            f_notifierBacklog.await();
                            }

                        // periodic flush and block
                        if (nFlushOn != 0 && (cMsgOut % nFlushOn) == 0)
                            {
                            bus.flush();

                            if (fBlock)
                                {
                                // wait for response(s)
                                int nSeq = (Integer) awaitResult();
                                int nExp = (int) ((m_cMsgOut - 1) & 0x0FFFF);
                                if (nSeq != 0 && nExp != nSeq)
                                    {
                                    throw new IllegalStateException("unexpected response id " + nSeq + " expected " + nExp);
                                    }
                                }
                            }

                        if (s_fSingleUseConnection)
                            {
                            bus.disconnect(peer);
                            synchronized (setReady)
                                {
                                // wait for release to be processed
                                while (setReady.contains(peer))
                                    {
                                    setReady.wait();
                                    }
                                }
                            bus.connect(peer);
                            }
                        }
                    catch (IllegalArgumentException e)
                        {
                        // should be from a concurrent release; not an error
                        if (fBlock)
                            {
                            cPending.decrementAndGet();
                            }

                        if (bufseq != null && bufseq != f_bufSeqCached)
                            {
                            bufseq.dispose();
                            }
                        }

                    // transmit throttle
                    if (cbsTarget > 0 && (++cSendThrottle % nThrottle) == 0)
                        {
                        bus.flush();
                        Blocking.sleep(cThrottleMillis);

                        if (cbThis >= cbEval)
                            {
                            // evaluate data rate against the target and adjust
                            long ldtNow = System.currentTimeMillis();
                            long cMillis = ldtNow - ldtLast;
                            if (cMillis == 0)
                                {
                                ++nThrottle;
                                }
                            else
                                {
                                double cbs = (cbThis * 1000) / cMillis;
                                double dfl = cbsTarget / cbs;
                                int nThrottleNew = (int) Math.round(nThrottle * dfl);
                                if (nThrottleNew == 0)
                                    {
                                    nThrottleNew = 1;
                                    ++cThrottleMillis; // go slower still
                                    }
                                else if (nThrottleNew == nThrottle)
                                    {
                                    if (dfl > 1.01)
                                        {
                                        ++cThrottleMillis;
                                        }
                                    else if (dfl < 0.09)
                                        {
                                        --cThrottleMillis;
                                        }
                                    }
                                nThrottle = nThrottleNew;
                                }

                            cSendThrottle = 0;
                            cbThis = 0;
                            ldtLast = ldtNow;
                            }
                        }

                    // simple stats updating in thread name
                    if (s_fVerbose && (cMsgOut % 10000) == 0)
                        {
                        // update thread-name to show movement
                        Thread thread = Thread.currentThread();
                        String sName = thread.getName();
                        int of = sName.lastIndexOf('#');

                        thread.setName(sName.substring(0, of == -1
                                ? sName.length()
                                : of) +
                                "#" + cMsgOut);
                        }
                    }
                }
            catch (Throwable e)
                {
                s_cErrors.incrementAndGet();
                System.err.println("fatal error after sending " + m_cMsgOut + " messages");
                throw new IllegalStateException(e);
                }
            }

        // ----- helpers ------------------------------------------------

        /**
         * Construct a message.
         *
         * @param ldtSent  the send time if a latency measurement is required, or 0 for none
         *
         * @return the message
         *
         * @throws IOException if an IO error occurs
         */
        public BufferSequence getMessage(long ldtSent)
                throws IOException
            {
            if (s_fCached)
                {
                if (s_fBlock)
                    {
                    // since we're blocking and asking for a new message the old one is free for re-use; just
                    // overwrite the timestamp
                    // Note that reusing the same bufseq would produce ByteBuffer.duplicates which are expensive to
                    // GC, so instead we produce new sequences of the now unused buffers
                    for (ByteBuffer buf : f_aBufCached)
                        {
                        buf.position(0);
                        }
                    f_aBufCached[0].putLong(5, ldtSent);
                    return f_aBufCached.length == 1
                            ? new SingleBufferSequence(null, f_aBufCached[0])
                            : new MultiBufferSequence(null, f_aBufCached);
                    }
                else if (ldtSent == 0)
                    {
                    // non-blocking cached request in always non-tied
                    return f_bufSeqCached;
                    }
                // else; timed non-blocking request, not cached; fall through
                }

            // upper 16 bits of id are a message counter, lower are a thread id
            return MessageBusTest.getMessage(((int) (m_cMsgOut << 16)) | f_nId, /*fResp*/ false, ldtSent);
            }

        // ----- data members -------------------------------------------

        /**
         * The associated Bus.
         */
        protected final Bus f_bus;

        /**
         * The cached message to use for untimed sends
         */
        protected final BufferSequence f_bufSeqCached;

        /**
         * The buffers in the cached sequence
         */
        protected final ByteBuffer[] f_aBufCached;

        /**
         * Non-zero if the bus has declared a global backlog.
         */
        protected final AtomicInteger f_fBacklogGlobal;

        /**
         * The transmitter ID.
         */
        protected final int f_nId;

        /**
         * The EndPoints to send to.
         */
        protected final Set<EndPoint> f_setReady;

        /**
         * The target data rate.
         */
        protected long f_cbs;

        /**
         * The number of messages to send before flushing.
         */
        protected final int f_nFlushOn;

        /**
         * The maximum allowed transmit backlog, or -1 for no limit.
         */
        protected final long f_cbTxMaxBacklog;

        /**
         * The current backlog.
         */
        protected final AtomicLong f_cbTxBacklog = new AtomicLong();

        /**
         * Backlog notifier.
         */
        protected final Notifier f_notifierBacklog = new SingleWaiterCooperativeNotifier();

        /**
         * The number of pending responses.
         */
        protected final AtomicInteger f_cPendingResponses = new AtomicInteger();

        /**
         * The number of bytes written.
         */
        protected long m_cbOut;

        /**
         * The number of sent messages.
         */
        protected long m_cMsgOut;

        /**
         * The number of milliseconds the transmitter was blocked waiting
         * for peers.
         */
        protected long m_cMillisBacklog;

        /**
         * The start time of the current backlog, or 0 if none is active
         */
        protected volatile long m_ldtBacklogStart;

        protected final long s_cbMsgAvg = s_cbMsgMin + (s_cbMsgMax - s_cbMsgMin) / 2;
        }

    // ----- inner class: SkipStream ----------------------------------------

    /**
     * SkipStream in an OutputStream with the ability to skip a number of
     * bytes.  This provides a cheap way to write large multi-buffer messages
     * without serialization overhead.
     */
    public static class SkipStream
            extends BufferSequenceOutputStream
        {
        // ----- constructors -------------------------------------------

        /**
         * Construct a SkipStream
         *
         * @param manager  the manager to allocate the buffers from
         */
        public SkipStream(BufferManager manager)
            {
            super(manager);
            }

        /**
         * Construct a SkipStream
         *
         * @param manager  the manager to allocate the buffers from
         * @param cb       the estimated size of the stream
         */
        public SkipStream(BufferManager manager, long cb)
            {
            super(manager, cb);
            }

        // ----- SkipStream interface -----------------------------------

        /**
         * Skip over the specified number of output bytes.
         *
         * @param lcb  the number of bytes to skip over.
         *
         * @throws IOException on an I/O error
         */
        public void skip(long lcb)
                throws IOException
            {
            while (lcb > 0)
                {
                ByteBuffer buf = ensureSpace(lcb);
                int cb = (int) Math.min(lcb, buf.remaining());
                buf.position(buf.position() + cb);
                lcb -= cb;
                }
            }
        }


    /**
     * EchoBus is a simple MessageBus implementation which echos all messages back to itself.
     */
    public static class EchoBus
            implements MessageBus
        {
        public EchoBus(EndPoint pointSelf)
            {
            m_pointSelf = pointSelf;
            }

        // ---- MessageBus interface ----------------------------------------

        @Override
        public void send(final EndPoint peer, BufferSequence bufseq, final Object receipt)
            {
            final Collector<Event> collector = getEventCollector();
            collector.add(new SimpleEvent(Event.Type.MESSAGE, peer, bufseq)
                {
                @Override
                public Object dispose(boolean fTakeContent)
                    {
                    if (receipt != null)
                        {
                        // echo bus isn't done with source message until it has been disposed
                        collector.add(new SimpleEvent(Event.Type.RECEIPT, peer, receipt));
                        }
                    return super.dispose(fTakeContent);
                    }
                });
            }

        @Override
        public EndPoint getLocalEndPoint()
            {
            return m_pointSelf;
            }

        @Override
        public void open()
            {
            getEventCollector().add(new SimpleEvent(Event.Type.OPEN, getLocalEndPoint()));
            flush();
            }

        @Override
        public void close()
            {
            getEventCollector().add(new SimpleEvent(Event.Type.CLOSE, getLocalEndPoint()));
            flush();
            }

        @Override
        public void connect(EndPoint peer)
            {
            getEventCollector().add(new SimpleEvent(Event.Type.CONNECT, peer));
            flush();
            }

        @Override
        public void disconnect(EndPoint peer)
            {
            getEventCollector().add(new SimpleEvent(Event.Type.DISCONNECT, peer));
            flush();
            }

        @Override
        public void release(EndPoint peer)
            {
            getEventCollector().add(new SimpleEvent(Event.Type.RELEASE, peer));
            flush();
            }

        @Override
        public void flush()
            {
            getEventCollector().flush();
            }

        @Override
        public void setEventCollector(Collector<Event> collector)
            {
            m_collector = collector;
            }

        @Override
        public Collector<Event> getEventCollector()
            {
            return m_collector;
            }

        public static class EchoDriver
                implements Driver
            {
            @Override
            public void setDepot(Depot depot)
                {
                m_depot = depot;
                }

            @Override
            public Depot getDepot()
                {
                return m_depot;
                }

            @Override
            public EndPoint resolveEndPoint(String sName)
                {
                if (sName == null || !sName.equals("echo"))
                    {
                    return null;
                    }

                return new EndPoint()
                {
                @Override
                public String getCanonicalName()
                    {
                    return "echo";
                    }

                @Override
                public String toString()
                    {
                    return getCanonicalName();
                    }
                };
                }

            @Override
            public boolean isSupported(EndPoint point)
                {
                return point != null && point.getCanonicalName().equals("echo");
                }

            @Override
            public Bus createBus(EndPoint pointLocal)
                {
                if (isSupported(pointLocal))
                    {
                    return new EchoBus(pointLocal);
                    }
                throw new IllegalArgumentException("unsupported");
                }

            // ----- data members ---------------------------------------

            protected Depot m_depot;
            }
        // ----- data members -----------------------------------------------

        protected EndPoint         m_pointSelf;
        protected Collector<Event> m_collector;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a newly configured Histogram for measuring latencies.  Samples added to this histogram must be
     * measured in microseconds.
     *
     * @return the histogram
     */
    public static Histogram makeLatencyHistogram()
        {
        return new ScaledHistogram(10*1000000) // 10s ceiling
                .setFormatter((v) -> new Duration(v, Duration.Magnitude.MICRO).toString());
        }

    /**
     * Construct a message.
     *
     * @param nId      the requester id
     * @param fResp    true for response
     * @param ldtSent  the send time if a latency measurement is required, or 0 for none
     *
     * @return the message
     *
     * @throws IOException if an IO error occurs
     */
    public static BufferSequence getMessage(int nId, boolean fResp, long ldtSent)
            throws IOException
        {
        long cb = s_cbMsgMin;
        long cbDelta = s_cbMsgMax - s_cbMsgMin;
        if (cbDelta > 0)
            {
            cb += s_rand.nextLong() % cbDelta;
            }
        cb = Math.max(cb, MSG_HEADER_SIZE);

        SkipStream out = null;
        do
            {
            try
                {
                out = new SkipStream(s_manager, cb); // TODO: optionally don't supply a hint
                }
            catch (OutOfMemoryError e)
                {
                System.err.println(Thread.currentThread().getName() + " handling error: " + e);
                System.err.println(s_manager);
                s_cErrors.incrementAndGet();
                Thread.yield();
                }
            }
        while (out == null);

        // create message
        // format:
        // 4B transmitter ID
        // 1B msg type
        // 8B timestamp (or zero)
        // 8B payload size
        // payload
        long cbPayload = cb - MSG_HEADER_SIZE;

        out.writeInt(nId);
        out.writeBoolean(fResp);
        out.writeLong(ldtSent);
        out.writeLong(cbPayload);

        int cbChunk = s_cbChunk;
        switch (cbChunk)
            {
            default:
                for (; cbPayload >= cbChunk; cbPayload -= cbChunk)
                    {
                    out.write(s_abChunk);
                    }
                // fall through

            case 8:
                for (; cbPayload >= 8; cbPayload -= 8)
                    {
                    out.writeLong(0);
                    }
                // fall through

            case 4:
                for (; cbPayload >= 4; cbPayload -= 4)
                    {
                    out.writeInt(0);
                    }
                // fall through

            case 2:
                for (; cbPayload >= 2; cbPayload -= 2)
                    {
                    out.writeShort(0);
                    }
                // fall through

            case 1:
                for (; cbPayload >= 1; cbPayload -= 1)
                    {
                    out.write(0);
                    }
                break;

            case 0:
                // skip remainder
                out.skip(cbPayload);
                break;
            }

        return out.toBufferSequence();
        }


    /**
     * Configure a SocketBusDriver from the provided propeties.
     *
     * @param sPrefix  the property prefix
     * @param props    the properties
     * @param deps     the Dependencies object to populate
     *
     * @return the configured Dependencies
     *
     * @throws Exception on an error
     */
    public static SocketBusDriver.DefaultDependencies applyDriverProperties(
            String sPrefix, Properties props, SocketBusDriver.DefaultDependencies deps)
            throws Exception
        {
        deps.setBufferManager(s_manager);

        // SocketOptions
        SocketSettings sockOpts = new SocketSettings(SocketBusDriver.DefaultDependencies.DEFAULT_OPTIONS);
        String sName = sPrefix + "socket.rxbuffer";
        if (props.containsKey(sName))
            {
            sockOpts.set(SocketOptions.SO_RCVBUF, (int) new MemorySize(props.getProperty(sName)).getByteCount());
            }
        sName = sPrefix + "socket.txbuffer";
        if (props.containsKey(sName))
            {
            sockOpts.set(SocketOptions.SO_SNDBUF, (int) new MemorySize(props.getProperty(sName)).getByteCount());
            }
        sName = sPrefix + "socket.nodelay";
        if (props.containsKey(sName))
            {
            sockOpts.set(SocketOptions.TCP_NODELAY, Boolean.parseBoolean(props.getProperty(sName)));
            }
        sName = sPrefix + "socket.linger";
        if (props.containsKey(sName))
            {
            sockOpts.set(SocketOptions.SO_LINGER, Boolean.parseBoolean(props.getProperty(sName)));
            }
        deps.setSocketOptions(sockOpts);

        return deps;
        }


    /**
     * Parse the supplied properties object into a SimpleDepot.Dependencies
     * object.
     * <p>
     * This allows for advanced bus properties to be configured.
     *
     * @param sPrefix  the property prefix
     * @param props    the properties
     *
     * @return the Dependencies object
     *
     * @throws Exception on error
     */
    public static SimpleDepot.Dependencies parseDependencies(String sPrefix, final Properties props)
            throws Exception
        {
        SimpleDepot.DefaultDependencies depsDepot = new SimpleDepot.DefaultDependencies();

        String sSslKeystore = sPrefix + "ssl.keystore";
        if (props.containsKey(sSslKeystore))
            {
            // SSL options
            String sKeystore = props.getProperty(sSslKeystore);
            String sPassword = props.getProperty(sPrefix + "ssl.password", "password");
            SSLContext ctx = SSLContext.getInstance("TLS");
            KeyManagerFactory keymanager = KeyManagerFactory.getInstance("SunX509");
            TrustManagerFactory trustmanager = TrustManagerFactory.getInstance("SunX509");
            KeyStore keystore = KeyStore.getInstance("JKS");
            char[] achPassword = sPassword.toCharArray();

            keystore.load(new URL("file:" + sKeystore).openStream(), achPassword);
            keymanager.init(keystore, achPassword);
            trustmanager.init(keystore);

            ctx.init(keymanager.getKeyManagers(), trustmanager.getTrustManagers(), new SecureRandom());

            SSLSettings sslSettings = new SSLSettings().setSSLContext(ctx);
            String sClientAuth = props.getProperty(sPrefix + "ssl.clientauth", "none").toLowerCase();
            switch (sClientAuth)
                {
                case "wanted":
                    sslSettings.setClientAuth(SSLSocketProvider.ClientAuthMode.wanted);
                    break;
                case "required":
                case "true":
                    sslSettings.setClientAuth(SSLSocketProvider.ClientAuthMode.required);
                    break;
                case "none":
                case "false":
                default:
                    sslSettings.setClientAuth(SSLSocketProvider.ClientAuthMode.none);
                    break;
                }

            depsDepot.setSSLSettings(sslSettings);
            }

        Map<String, Driver> mapDriver = new HashMap<>(depsDepot.getDrivers());

        for (Map.Entry<String, Driver> entry : mapDriver.entrySet())
            {
            Driver driver = entry.getValue();
            if (driver instanceof SocketBusDriver)
                {
                SocketBusDriver sbDriver = (SocketBusDriver) driver;
                entry.setValue(new SocketBusDriver(applyDriverProperties(
                        sPrefix, props,
                        new SocketBusDriver.DefaultDependencies(sbDriver.getDependencies()))));
                }
            }

        mapDriver.put("EchoBus", new EchoBus.EchoDriver());

        depsDepot.setDrivers(mapDriver);

        return depsDepot;
        }

    /**
     * Parse command line argments into key value pairs.
     *
     * @param asArg  the command line arguments
     *
     * @return the key value map
     */
    public static Map<String, String> parseArgs(String[] asArg)
        {
        Map<String, String> mapArgs = new HashMap<String, String>();

        for (int i = 0, c = asArg.length; i < c; ++i)
            {
            String arg = asArg[i];
            if (arg.startsWith("-"))
                {
                String sKey = arg;
                String sVal = null;
                for (; i + 1 < c; ++i)
                    {
                    arg = asArg[i + 1];
                    if (arg.startsWith("-"))
                        {
                        try
                            {
                            Integer.valueOf(arg);
                            }
                        catch (NumberFormatException e)
                            {
                            break; // new key
                            }
                        }

                    sVal = sVal == null ? arg : sVal + " " + arg;
                    }
                mapArgs.put(sKey, sVal == null ? "true" : sVal);
                }
            else
                {
                throw new IllegalArgumentException("unepxected paramter " + arg);
                }
            }

        return mapArgs;
        }

    /**
     * Parse a string containing a space seperated list of EndPoint names.
     * <p>
     * This method supports range based endpoints for EndPoints which end in
     * <tt>:port</tt>.  The range can be specified using <tt>..port</tt>,
     * allowing the specification of a range such as:
     * <tt>http://localhost:80..89</tt> to result in ten addresses.
     *
     * @param depot  the Depot to use in resolving the names
     * @param sEps   the EndPoint name string
     *
     * @return a List resolved EndPoints
     */
    public static List<EndPoint> parseEndPoints(Depot depot, String sEps)
        {
        List<EndPoint> listEp = new ArrayList<EndPoint>();
        for (StringTokenizer tok = new StringTokenizer(sEps); tok.hasMoreElements(); )
            {
            String sTok = tok.nextToken();
            int of = sTok.indexOf("..");

            if (of == -1)
                {
                listEp.add(depot.resolveEndPoint(sTok));
                }
            else // range based endpoint, assumes name:port formatting
                {
                String sName = sTok.substring(0, of);
                int ofPort = Math.max(sName.lastIndexOf('.'), sName.lastIndexOf(':'));
                String sPrefix = sName.substring(0, ofPort + 1);
                int nPort = Integer.parseInt(sName.substring(ofPort + 1));
                int nPortEnd = Integer.parseInt(sTok.substring(of + 2));

                if (nPort < nPortEnd)
                    {
                    for (; nPort <= nPortEnd; ++nPort)
                        {
                        listEp.add(depot.resolveEndPoint(sPrefix + nPort));
                        }
                    }
                else
                    {
                    for (; nPort >= nPortEnd; --nPort)
                        {
                        listEp.add(depot.resolveEndPoint(sPrefix + nPort));
                        }
                    }
                }
            }
        return listEp;
        }

    public static void printHelp(PrintStream out)
        {
        out.println("MessageBusTest parameters:");
        out.println("\t-bind               list of one or more local EndPoints to create");
        out.println("\t-peer               list of one or more remote EndPoints to send to");
        out.println("\t-rxThreads          number of receive threads per bound EndPoint (negative for reentrant)");
        out.println("\t-txThreads          number of transmit threads per bound EndPoint");
        out.println("\t-msgSize            range of message sizes to send, expressed as min[..max]");
        out.println("\t-chunkSize          defines the number of bytes to process as a single unit, i.e. 1 for byte, 8 for long, 0 to disable");
        out.println("\t-cached             re-use message objects where possible, reducing buffer manager overhead");
        out.println("\t-txRate             target outbound data rate");
        out.println("\t-txMaxBacklog       the maximum backlog the test should produce per tx thread");
        out.println("\t-rxRate             target inbound data rate");
        out.println("\t-flushFreq          number of messages to send before flushing, or 0 for auto");
        out.println("\t-latencyFreq        number of messages to send before sampling latency");
        out.println("\t-noReceipts         specified if receipts should not be used, relies on GC to reclaim messages");
        out.println("\t-manager            buffer manager to utilize (net, direct, heap)");
        out.println("\t-polite             if specified this instance will not start sending until connected to");
        out.println("\t-depotFactory       the fully qualified class name of the Factory to use to obtain the Depot");
        out.println("\t-reportInterval     the report interval");
        out.println("\t-polite             if specified this instance will not start sending until connected to");
        out.println("\t-block              if specified a transmit thread will block while awaiting a response, optional value of spin duration");
        out.println("\t-relay              if specified then the process will relay any received messages to one of its peers");
        out.println("\t-ignoreFlowControl  if flow control events are to be ignored, use -txMaxBacklog to prevent OutOfMemory");
        out.println("\t-poll               is specified PollingEventCollector will be utilized");
        out.println("\t-prompt             if specified the user will be prompted before each send");
        out.println("\t-tabular            if specified the output will be in tabular format");
        out.println("\t-warmup             time duration or message count which will be discarded for warmup");
        out.println("\t-single             if specified an outgoing connection will emit just one message, then reconnect");
        out.println("\t-verbose            to enable verbose debugging output");
        }

    /**
     * Run the MessageBusTest application.
     *
     * @param asArg  the program arguments
     *
     * @throws IOException if an IO error occurs
     */
    public static void main(String[] asArg)
            throws Exception
        {
        Map<String, String> mapArgs = parseArgs(asArg);

        String sEpLocal = mapArgs.remove("-bind");
        String sEpPeer = mapArgs.remove("-peer");
        String sRxThreads = mapArgs.remove("-rxThreads");
        String sTxThreads = mapArgs.remove("-txThreads");
        String sMsgSize = mapArgs.remove("-msgSize");
        String sChunkSize = mapArgs.remove("-chunkSize");
        String sCached = mapArgs.remove("-cached");
        String sFlushFreq = mapArgs.remove("-flushFreq");
        String sTxRate = mapArgs.remove("-txRate");
        String sTxMaxBacklog = mapArgs.remove("-txMaxBacklog");
        String sRxRate = mapArgs.remove("-rxRate");
        String sLatFreq = mapArgs.remove("-latencyFreq");
        String sNoReceipt = mapArgs.remove("-noReceipts");
        String sManager = mapArgs.remove("-manager");
        String sVerbose = mapArgs.remove("-verbose");
        String sPolite = mapArgs.remove("-polite");
        String sBlock = mapArgs.remove("-block");
        String sRelay = mapArgs.remove("-relay");
        String sIgnoreFC = mapArgs.remove("-ignoreFlowControl");
        String sTabular = mapArgs.remove("-tabular");
        String sFactoryDepot = mapArgs.remove("-depotFactory");
        String sReportInterval = mapArgs.remove("-reportInterval");
        String sPollingColl = mapArgs.remove("-poll");
        String sPrompt = mapArgs.remove("-prompt");
        String sWarmup = mapArgs.remove("-warmup");
        String sSingle = mapArgs.remove("-single");

        if (sTxRate != null && Character.isDigit(sTxRate.charAt(sTxRate.length() - 1)))
            {
            sTxRate += "MBps";
            }
        if (sRxRate != null && Character.isDigit(sRxRate.charAt(sRxRate.length() - 1)))
            {
            sRxRate += "MBps";
            }

        if (sReportInterval == null)
            {
            sReportInterval = "5s";
            }
        else if (Character.isDigit(sReportInterval.charAt(sReportInterval.length() - 1)))
            {
            sReportInterval += "s";
            }

        s_fVerbose = sVerbose != null;
        s_fFlowControl = sIgnoreFC == null;
        s_fCached = sCached != null;
        s_fPollingCollector = sPollingColl != null;
        s_fPrompt = sPrompt != null;
        s_fSingleUseConnection = sSingle != null;

        long cReportMillis = new Duration(sReportInterval).as(Duration.Magnitude.MILLI);
        boolean fPolite = sPolite != null && sPolite.equals("true");
        boolean fBlock = sBlock != null && !sBlock.equals("false");
        boolean fRelay = sRelay != null && !sRelay.equals("false");
        boolean fTabular = sTabular != null && sTabular.equals("true");
        int cRxThreads = Math.abs(sRxThreads == null ? 1 : Integer.parseInt(sRxThreads));
        int cTxThreads = Math.abs(sTxThreads == null ? fRelay ? 0 : 1 : Integer.parseInt(sTxThreads));
        boolean fReentrant = sRxThreads != null && sRxThreads.startsWith("-");
        long cbsOut = sTxRate == null ? -1 : new Bandwidth(sTxRate).as(Rate.BYTES);
        long cbsIn = sRxRate == null ? -1 : new Bandwidth(sRxRate).as(Rate.BYTES);
        long cbMaxBacklog = sTxMaxBacklog == null ? -1 : new MemorySize(sTxMaxBacklog).getByteCount();
        long cMsgWarmup = 0;
        long cMillisWarmup = 0;


        if (sWarmup != null)
            {
            try
                {
                cMsgWarmup = Integer.parseInt(sWarmup);
                }
            catch (Exception e)
                {
                cMillisWarmup = new Duration(sWarmup).as(Duration.Magnitude.MILLI);
                }
            }

        if (fBlock && !(sBlock.equals("true")))
            {
            AWAIT_SPIN_NANOS = new Duration(sBlock).getNanos();
            }
        s_fBlock = fBlock;

        if (fRelay)
            {
            if (cTxThreads != 0)
                {
                System.err.println("-relay and -txThreads cannot both be specified");
                System.exit(1);
                }
            }
        s_fRelay = fRelay;

        // select default thread counts
        if (sRxThreads == null || cRxThreads == 0)
            {
            // no rx threads specified, default to reentrant mode on all available cores
            // note that because we are reentrant the actual number of threads is up to the bus
            // impl; this is only selecting how many can execute concurrently
            cRxThreads = Platform.getPlatform().getFairShareProcessors() * 17;
            fReentrant = true;
            }

        if (s_fPollingCollector && cRxThreads != 1)
            {
            System.err.println("\nWARNING: polling collector generally requires -rxThreads 1\n");
            }

        if (fReentrant && cbsIn != -1)
            {
            if (sRxThreads == null)
                {
                // user asked for rx throttling and didn't specify rxThreads, thus didn't ask for reentrancy; disable
                fReentrant = false;
                cRxThreads = 1;
                }
            else
                {
                System.err.println("inbound throttling (-rxRate) not available with reentrant processing (-rxThreads <= 0)");
                System.exit(1);
                }
            }

        if (sLatFreq == null)
            {
            s_nLatencyFreq = 100;
            }
        else
            {
            s_nLatencyFreq = Integer.parseInt(sLatFreq);
            }

        s_fReceipts = sNoReceipt == null || !sNoReceipt.equals("true");
        if (!s_fReceipts && cbMaxBacklog != -1)
            {
            throw new IllegalArgumentException("-txMaxBacklog requires receipts");
            }

        if (sManager == null || sManager.equals("net"))
            {
            s_manager = BufferManagers.getNetworkDirectManager();
            }
        else if (sManager.equals("direct"))
            {
            s_manager = BufferManagers.getDirectManager();
            }
        else if (sManager.equals("heap"))
            {
            s_manager = BufferManagers.getHeapManager();
            }
        else
            {
            throw new IllegalArgumentException("unknown heap manager: " + sManager);
            }

        long cbMin, cbMax, cbAvg;
        if (sMsgSize == null)
            {
            cbMin = cbMax = cbAvg = 4096;
            }
        else
            {
            int ofMsgDelim = sMsgSize.indexOf("..");
            if (ofMsgDelim == -1)
                {
                // fixed message size
                cbMin = cbMax = cbAvg = new MemorySize(sMsgSize).getByteCount();
                }
            else if (s_fCached)
                {
                System.err.println("-cached does not support variable sized messaging");
                throw new IllegalArgumentException();
                }
            else
                {
                // range based
                cbMin = new MemorySize(sMsgSize.substring(0, ofMsgDelim)).getByteCount();
                cbMax = new MemorySize(sMsgSize.substring(ofMsgDelim + 2)).getByteCount();

                if (cbMax < cbMin)
                    {
                    // wrong order, swap them
                    long n = cbMax;
                    cbMax = cbMin;
                    cbMin = n;
                    }

                cbAvg = cbMax - ((cbMax - cbMin) / 2);
                }
            }

        if (cbMin < MSG_HEADER_SIZE)
            {
            System.out.println("increasing minimum message size to " + MSG_HEADER_SIZE + " bytes to satisfy test requirements\n");
            cbMin = MSG_HEADER_SIZE;
            }
        if (cbMax < MSG_HEADER_SIZE)
            {
            cbMax = MSG_HEADER_SIZE;
            }
        if (cbAvg < MSG_HEADER_SIZE)
            {
            cbAvg = MSG_HEADER_SIZE;
            }

        s_cbMsgMin = cbMin;
        s_cbMsgMax = cbMax;

        int cbChunk = 0;
        if (sChunkSize != null)
            {
            cbChunk = (int) new MemorySize(sChunkSize).getByteCount();
            }
        s_cbChunk = (int) Math.min(cbMin, cbChunk);
        s_abChunk = new byte[cbChunk];

        int nFlushOn = sFlushFreq == null ? 0 : Integer.parseInt(sFlushFreq);

        if (!mapArgs.isEmpty())
            {
            System.err.println("unknown parameter " + mapArgs.keySet().iterator().next());
            System.err.println();
            printHelp(System.err);
            System.exit(1);
            }

        // create local busses
        Depot depot;
        if (sFactoryDepot == null)
            {
            depot = new SimpleDepot(parseDependencies("depot.", System.getProperties()));
            }
        else
            {
            depot = ((Factory<Depot>) Class.forName(sFactoryDepot).newInstance()).create();
            }

        // resolve peer EndPoints
        EndPoint[] aPeer = new EndPoint[0];
        if (sEpPeer != null)
            {
            aPeer = parseEndPoints(depot, sEpPeer).toArray(aPeer);
            }

        List<Bus> listBus = new ArrayList<Bus>();

        if (sEpLocal == null)
            {
            if (aPeer.length == 0)
                {
                listBus.add(depot.createMessageBus(null));
                }
            else
                {
                // attempt to compute a local endpoint from remote endpoint
                String sPeer = aPeer[0].getCanonicalName();
                if (sPeer.contains(":"))
                    {
                    listBus.add(depot.createMessageBus(
                            depot.resolveEndPoint(sPeer.substring(0, sPeer.indexOf(':')) + "://0.0.0.0:0")));
                    }
                }
            }
        else
            {
            for (EndPoint epBind : parseEndPoints(depot, sEpLocal))
                {
                try
                    {
                    listBus.add(depot.createMessageBus(epBind));
                    }
                catch (Exception e)
                    {
                    try
                        {
                        // "hidden" MemoryBus test mode
                        listBus.add(depot.createMemoryBus(epBind));
                        }
                    catch (Exception e2)
                        {
                        e2.printStackTrace();
                        throw e;
                        }
                    }
                }
            }

        // bring each bus up
        int                          cBus         = listBus.size();
        Set<DemultiplexingCollector> setDemuxer   = new HashSet<DemultiplexingCollector>();
        EventProcessor[]             aProcessor   = new EventProcessor[cBus * cRxThreads];
        Transmitter[]                aTransmitter = new Transmitter[cBus * cTxThreads];
        long                         cbsInProc    = cbsIn  == -1 ? cbsIn  : Math.max(1, cbsIn  / aProcessor.length);
        int                          iProc        = 0;
        int                          iTrans       = 0;

        for (Bus bus : listBus)
            {
            AtomicInteger    fBacklogLocal = new AtomicInteger();
            Set<EndPoint>    setReady      = Collections.newSetFromMap(new ConcurrentHashMap<EndPoint, Boolean>());
            EndPoint         boundEp       = bus.getLocalEndPoint();
            EventProcessor[] aProc         = new EventProcessor[cRxThreads];
            Transmitter[]    aTrans        = new Transmitter[cTxThreads];

            // select peers to operate against
            Set<EndPoint> setPeer = new HashSet<EndPoint>();
            for (EndPoint peer : aPeer)
                {
                if (!peer.equals(boundEp))
                    {
                    setPeer.add(peer);
                    }
                }
            setPeer = Collections.unmodifiableSet(setPeer);
            if (setPeer.isEmpty())
                {
                Transmitter[] aTransNew = new Transmitter[aTransmitter.length - cTxThreads];
                System.arraycopy(aTransmitter, 0, aTransNew, 0, aTransNew.length);
                aTransmitter = aTransNew;
                aTrans       = null;
                }
            else
                {
                // construct transmitters
                for (int i = 0, c = aTrans.length; i < c; ++i)
                    {
                    aTransmitter[iTrans++] = aTrans[i] = new Transmitter(bus, i, setReady, nFlushOn, fBacklogLocal, cbMaxBacklog);
                    }
                }

            // construct processors
            for (int i = 0; i < cRxThreads; ++i)
                {
                aProcessor[iProc++] = aProc[i] = new EventProcessor(bus, setPeer, setReady, aTrans, cbsInProc, nFlushOn, fBacklogLocal);
                }

            if (s_fPollingCollector)
                {
                bus.setEventCollector(new QueueingEventCollector());
                }
            else
                {
                DemultiplexingCollector collector = new DemultiplexingCollector(bus, aProc);
                setDemuxer.add(collector);
                bus.setEventCollector(collector);
                }
            bus.open();

            if (!fPolite)
                {
                // TODO: wait for open events
                // establish connections
                for (EndPoint peer : setPeer)
                    {
                    bus.connect(peer);
                    }
                }
            }

        // start processors
        if (!fReentrant)
            {
            for (EventProcessor proc : aProcessor)
                {
                proc.start();
                }
            }

        // start transmitters; they will wait for connections before sending
        long cbsOutTrans = cbsOut == -1 ? cbsOut : Math.max(1, cbsOut / aTransmitter.length);
        for (Transmitter trans : aTransmitter)
            {
            trans.setTransmitRate(cbsOutTrans);
            trans.start();
            }

        // stats logging

        // add a blank line printout during shutdown
        Runtime.getRuntime().addShutdownHook(new Thread()
            {
            public void run()
                {
                System.out.println();
                }
            });

        class Stats
            {
            public Stats(long ldt)
                {
                this.ldt = ldt;
                }

            long ldt;
            long cMsgIn;
            long cMsgOut;
            long cbIn;
            long cbOut;
            long cbInCollected;
            long cReceipts;
            long cReceiptSamples;
            long cReceiptNanos;
            long cResponses;
            long cResponseNanos;
            long cResponseNanosMin = Long.MAX_VALUE;
            long cResponseNanosMax = -1;
            long cBacklogLocal;
            long cMillisBacklogLocal;
            long cBacklogRemote;
            long cMillisBacklogRemote;
            long cbInPendingLife;
            long cbOutPendingLife;
            long cErrors;
            long cConnections;
            Histogram histLatency = makeLatencyHistogram();
            }

        if (fTabular)
            {
            System.out.println(
                    "msg/s in\t" +
                    "bytes/s in\t" +
                    "msg/s out\t" +
                    "bytes/s out\t" +
                    "avg receipt latency nanos\t" +
                    "min response latency nanos\t" +
                    "avg response latency nanos\t" +
                    "effective latency nanos\t" +
                    "max response latency nanos\t" +
                    "in backlog percentage\t" +
                    "in backlog events\t" +
                    "in backlog bytes\t" +
                    "out backlog percentage\t" +
                    "out backlog events\t" +
                    "out backlog bytes\t" +
                    "connections\t" +
                    "errors");
            }

        long ldtWarmStart = 0;
        Stats statsWarm   = null; // base stats after warmup has completed
        Stats statsPrev   = null; // stats from prev cycle
        Stats stats       = null; // stats from this cycle

        for (int iReport = 0; ; ++iReport)
            {
            Blocking.sleep(statsWarm == null ? 10 : cReportMillis);

            stats = new Stats(System.currentTimeMillis());

            for (EventProcessor proc : aProcessor)
                {
                stats.cReceipts            += proc.getReceiptsIn();
                stats.cReceiptSamples      += proc.getReceiptSamples();
                stats.cReceiptNanos        += proc.getReceiptNanos();
                stats.cResponses           += proc.getResponsesIn();
                stats.cResponseNanos       += proc.getResponseNanos();
                stats.histLatency.addSamples(proc.getResponseLatencyHistogram());
                stats.cResponseNanosMax     = Math.max(proc.m_cResponseNanosMax, stats.cResponseNanosMax);
                proc.m_cResponseNanosMax    = -1;
                stats.cResponseNanosMin     = Math.min(proc.m_cResponseNanosMin, stats.cResponseNanosMin);
                proc.m_cResponseNanosMin    = Long.MAX_VALUE;
                stats.cMsgIn               += proc.getMessagesIn();
                stats.cMsgOut              += proc.getMessagesOut();
                stats.cbIn                 += proc.getBytesIn();
                stats.cbOut                += proc.getBytesOut();
                stats.cBacklogLocal        += proc.getLocalBacklogEvents();
                stats.cMillisBacklogLocal  += proc.getLocalBacklogMillis();
                stats.cBacklogRemote       += proc.getRemoteBacklogEvents();
                stats.cConnections         += proc.getConnectionCount();
                }

            for (Transmitter trans : aTransmitter)
                {
                stats.cMsgOut              += trans.getMessagesOut();
                stats.cbOut                += trans.getBytesOut();
                stats.cMillisBacklogRemote += trans.getRemoteBacklogMillis();
                }

            if (setDemuxer != null)
                {
                for (DemultiplexingCollector collector : setDemuxer)
                    {
                    stats.cbInCollected += collector.getReceivedBytes();
                    }
                }

            stats.cErrors = s_cErrors.get();

            if (statsWarm == null)
                {
                if (stats.cConnections > 0 && Math.max(stats.cMsgIn, stats.cMsgOut) > cMsgWarmup)
                    {
                    if (ldtWarmStart == 0)
                        {
                        ldtWarmStart = stats.ldt;
                        }

                    if (stats.ldt - ldtWarmStart >= cMillisWarmup)
                        {
                        // we've completed warmup, note that the duration between ldtWarmStart and stastWarm.ldt is
                        // questionable, but also never used
                        statsPrev = statsWarm = stats;
                        }

                    iReport = 0;
                    }
                continue;
                }
            else if (stats.cConnections == 0)
                {
                statsWarm    = null;
                ldtWarmStart = 0;
                continue;
                }

            for (Stats statsComp : new Stats[]{statsPrev, statsWarm})
                {
                long cDeltaMillis              = stats.ldt                  - statsComp.ldt;
                long cMsgInDelta               = stats.cMsgIn               - statsComp.cMsgIn;
                long cMsgOutDelta              = stats.cMsgOut              - statsComp.cMsgOut;
                long cbInDelta                 = stats.cbIn                 - statsComp.cbIn;
                long cbOutDelta                = stats.cbOut                - statsComp.cbOut;
                long cReceiptDelta             = stats.cReceiptSamples      - statsComp.cReceiptSamples;
                long cReceiptNanosDelta        = stats.cReceiptNanos        - statsComp.cReceiptNanos;
                long cResponseDelta            = stats.cResponses           - statsComp.cResponses;
                long cResponseNanosDelta       = stats.cResponseNanos       - statsComp.cResponseNanos;
                long cBacklogLocalDelta        = stats.cBacklogLocal        - statsComp.cBacklogLocal;
                long cMillisBacklogLocalDelta  = stats.cMillisBacklogLocal  - statsComp.cMillisBacklogLocal;
                long cBacklogRemoteDelta       = stats.cBacklogRemote       - statsComp.cBacklogRemote;
                long cMillisBacklogRemoteDelta = stats.cMillisBacklogRemote - statsComp.cMillisBacklogRemote;
                long cErrorsDelta              = stats.cErrors              - statsComp.cErrors;

                long cbOutPending              = (stats.cMsgOut - stats.cReceipts) * cbAvg;
                long cbInPending               = s_fPollingCollector ? -1   : stats.cbInCollected - stats.cbIn;

                double dflSeconds = cDeltaMillis / 1000.0;

                long MSGsIn    = Math.round(cMsgInDelta         / dflSeconds);
                long MSGsOut   = Math.round(cMsgOutDelta        / dflSeconds);
                long BLsLocal  = Math.round(cBacklogLocalDelta  / dflSeconds);
                long BLsRemote = Math.round(cBacklogRemoteDelta / dflSeconds);

                long lPctBacklogLocal       = (100 * cMillisBacklogLocalDelta) / (cDeltaMillis * cBus);
                long lPctBacklogRemote      = aTransmitter.length == 0
                        ? -1 : (100 * cMillisBacklogRemoteDelta) / (cDeltaMillis * aTransmitter.length);
                long cResponseNanosDeltaEff = (long) (cResponseNanosDelta * (1.0 / (1.0 - lPctBacklogRemote / 100.0)));

                if (statsComp == statsWarm)
                    {
                    // for lifetime we compute the average cost
                    stats.cbOutPendingLife = statsPrev.cbOutPendingLife + cbOutPending;
                    cbOutPending = stats.cbOutPendingLife / (iReport + 1);

                    stats.cbInPendingLife  = statsPrev.cbInPendingLife + cbInPending;
                    cbInPending            = stats.cbInPendingLife / (iReport + 1);

                    // carry min/max across iterations
                    stats.cConnections      = Math.max(stats.cConnections,      statsPrev.cConnections);
                    stats.cResponseNanosMin = Math.min(stats.cResponseNanosMin, statsPrev.cResponseNanosMin);
                    stats.cResponseNanosMax = Math.max(stats.cResponseNanosMax, statsPrev.cResponseNanosMax);
                    }

                if (fTabular)
                    {
                    System.out.println("" +
                            MSGsIn + '\t' +
                            cbInDelta / dflSeconds + '\t' +
                            MSGsOut + '\t' +
                            cbOutDelta / dflSeconds + '\t' +
                            (cReceiptDelta  == 0 ? -1 : cReceiptNanosDelta     / cReceiptDelta) + '\t' +
                            (cResponseDelta == 0 ? -1 : stats.cResponseNanosMin) + '\t' +
                            (cResponseDelta == 0 ? -1 : cResponseNanosDelta    / cResponseDelta) + '\t' +
                            (cResponseDelta == 0 ? -1 : cResponseNanosDeltaEff / cResponseDelta) + '\t' +
                            (cResponseDelta == 0 ? -1 : stats.cResponseNanosMax) + '\t' +
                            lPctBacklogLocal + '\t' +
                            BLsLocal + '\t' +
                            cbInPending + '\t' +
                            lPctBacklogRemote + '\t' +
                            BLsRemote + '\t' +
                            cbOutPending + '\t' +
                            stats.cConnections + '\t' +
                            cErrorsDelta);
                    break; // skip over lifetime step
                    }
                else
                    {
                    System.out.println((statsComp == statsPrev ? "now:  " : "life: ") +
                            "throughput(" +
                            "out "  + MSGsOut + "msg/s " + new Bandwidth(8 * cbOutDelta / dflSeconds, Rate.BITS) +
                            ", in " + MSGsIn  + "msg/s " + new Bandwidth(8 * cbInDelta  / dflSeconds, Rate.BITS) + "), " +
                            "latency(" +
                            "response" + (cResponseDelta == 0 ? " n/a"
                            : "(avg " + new Duration(cResponseNanosDelta / cResponseDelta) + ", effective " +  new Duration(cResponseNanosDeltaEff / cResponseDelta) + ", min " + new Duration(stats.cResponseNanosMin) + ", max " + new Duration(stats.cResponseNanosMax) + ")") +
                            ", receipt "   + (cReceiptDelta  == 0 ? "n/a" : new Duration(cReceiptNanosDelta     / cReceiptDelta)) + "), " +
                            "backlog(" +
                            "out "  + (lPctBacklogRemote < 0 ? "n/a " : lPctBacklogRemote + "% ") + BLsRemote + "/s " + (s_fReceipts ? new MemorySize(cbOutPending) : "n/a") +
                            ", in " + lPctBacklogLocal  + "% " + BLsLocal  + "/s " + (cbInPending < 0 ? "n/a" : new MemorySize(cbInPending)) + "), " +
                            "connections " + stats.cConnections +
                            ", errors " + cErrorsDelta);

                    if (s_fVerbose && stats.histLatency.getSampleCount() > 0)
                        {
                        System.out.println("\tlatency detail: " + stats.histLatency.compare(statsComp.histLatency));
                        }
                    }
                }

            if (s_fVerbose)
                {
                for (Bus bus : listBus)
                    {
                    System.out.println("bus:  " + bus);
                    }
                System.out.println("mgr:  " + s_manager);
                RuntimeMXBean beanRuntime = ManagementFactory.getRuntimeMXBean();
                System.out.print("jvm:  " + beanRuntime.getSpecVersion() + " " + beanRuntime.getVmVersion() + " ");
                for (String s : beanRuntime.getInputArguments())
                    {
                    System.out.print(s + " ");
                    }
                System.out.print("\ncmd:  ");
                for (String s : asArg)
                    {
                    System.out.print(s + " ");
                    }
                System.out.println();
                System.out.println("time: " + new Date() + "/" + new Duration(System.currentTimeMillis() - statsWarm.ldt, Duration.Magnitude.MILLI));
                }

            if (!fTabular)
                {
                System.out.println();
                }

            statsPrev = stats;
            }
        }

    // ---- constants -------------------------------------------------------

    /**
     * The message header size used in the test.
     */
    public static final int MSG_HEADER_SIZE = 21;


    // ---- static data members ---------------------------------------------

    /**
     * The BufferManager to use in the test.
     */
    protected static BufferManager s_manager;

    /**
     * Flag for verbose logging.
     */
    protected static boolean s_fVerbose;

    /**
     * True if user should be prompted before sending.
     */
    protected static boolean s_fPrompt;

    /**
     * Flag for using cached messages
     */
    protected static boolean s_fCached;

    /**
     * Flag for using polling collector
     */
    protected static boolean s_fPollingCollector;

    /**
     * The frequency (in messages), at which latency will be sampled.
     */
    public static int s_nLatencyFreq = 100;

    /**
     * True if receipts should be used.
     */
    public static boolean s_fReceipts = true;

    /**
     * The minimum message size.
     */
    public static long s_cbMsgMin;

    /**
     * The maximum message size
     */
    public static long s_cbMsgMax;

    /**
     * The unit size to process at.
     */
    public static int s_cbChunk;

    /**
     * To be used for chunked writes.
     */
    public static byte[] s_abChunk;

    /**
     * How long to spin waiting for results
     */
    protected static long AWAIT_SPIN_NANOS;

    /**
     * Switch govering if flow control events should be respected.
     */
    public static boolean s_fFlowControl = true;

    /**
     * True if this instance is to act as a message relay
     */
    public static boolean s_fRelay = false;

    /**
     * True if this instance is in blocking mode.
     */
    public static boolean s_fBlock = false;

    /**
     * True to use each connection for just one message.
     */
    public static boolean s_fSingleUseConnection;

    /**
     * Count of the number of errors encountered during the test.
     */
    public static AtomicLong s_cErrors = new AtomicLong();

    /**
     * Shared randomizer.
     */
    public static Random s_rand = new Random();
    }

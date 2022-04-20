/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.util;

import com.oracle.coherence.common.util.MemorySize;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Collector;
import com.oracle.coherence.common.base.Holder;
import com.oracle.coherence.common.base.SimpleHolder;
import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.BufferManagers;
import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.io.BufferSequenceInputStream;
import com.oracle.coherence.common.io.BufferSequenceOutputStream;
import com.oracle.coherence.common.io.Buffers;
import com.oracle.coherence.common.io.SingleBufferSequence;
import com.oracle.coherence.common.net.exabus.Depot;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.Event;
import com.oracle.coherence.common.net.exabus.MemoryBus;
import com.oracle.coherence.common.net.exabus.util.SimpleDepot;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Created with IntelliJ IDEA. User: falcom Date: 10/25/13 Time: 7:24 PM To change this template use File | Settings |
 * File Templates.
 */
public class MemoryBusTest
    {
    public static void main(String[] asArg)
            throws Exception
        {
        Depot depot = SimpleDepot.getInstance();
        EndPoint epServer = asArg.length < 1 ? null : depot.resolveEndPoint(asArg[0]);
        EndPoint epClient = asArg.length < 2 ? null : depot.resolveEndPoint(asArg[1]);
        MemoryBus busServer = depot.createMemoryBus(epServer);
        busServer.setEventCollector(new Collector<Event>()
            {
            @Override
            public void add(Event value)
                {
                System.out.println(value);
                Object oContent = value.getContent();
                if (oContent instanceof Throwable)
                    {
                    ((Throwable) oContent).printStackTrace();
                    }
                }

            @Override
            public void flush()
                {
                //To change body of implemented methods use File | Settings | File Templates.
                }
            });

        BufferManager mgr = BufferManagers.getNetworkDirectManager();

        BufferSequence bufSeqLocal = Buffers.allocateDirect(mgr, 10 * 1024 * 1024);
        busServer.setBufferSequence(bufSeqLocal);
        busServer.open();

        System.out.println(new MemorySize(bufSeqLocal.getLength()) + "/" + bufSeqLocal.getBufferCount());

        MemoryBus busClient = depot.createMemoryBus(epClient);
        busClient.setEventCollector(new Collector<Event>()
        {
        @Override
        public void add(Event value)
            {
            System.out.println(value);
            Object oContent = value.getContent();
            if (oContent instanceof Throwable)
                {
                ((Throwable) oContent).printStackTrace();
                }
            else if (oContent != null && value.getType() == Event.Type.RECEIPT)
                {
                synchronized (oContent)
                    {
                    if (oContent instanceof Holder)
                        {
                        ((Holder) oContent).set(null);
                        }
                    oContent.notify();
                    }
                }
            }

        @Override
        public void flush()
            {
            //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        busClient.open();
        busClient.connect(busServer.getLocalEndPoint());

        while (busClient.getCapacity(busServer.getLocalEndPoint()) <= 0)
            {
            System.out.println("server capacity " + busClient.getCapacity(busServer.getLocalEndPoint()));
            Blocking.sleep(1000);
            }

        System.out.println("server capacity " + new MemorySize(busClient.getCapacity(busServer.getLocalEndPoint())));


        System.out.println("RDMA writes");
        for (int i = 0; i < 8; ++i)
            {
            ByteBuffer bufWrite = mgr.acquire(8).order(ByteOrder.nativeOrder());
            bufWrite.putInt(i).flip();

            BufferSequence bufseqWrite = new SingleBufferSequence(mgr, bufWrite);
            Holder<BufferSequence> holder = new SimpleHolder<BufferSequence>(bufseqWrite);

            busClient.write(busServer.getLocalEndPoint(), i * 8, bufseqWrite, holder);
            busClient.flush();

            synchronized (holder)
                {
                while (holder.get() != null)
                    {
                    Blocking.wait(holder);
                    }
                }
            }

        System.out.println("memory = " + Buffers.toString(bufSeqLocal, true, 64));

        busClient.signal(busServer.getLocalEndPoint(), 123, null);

        System.out.println("RDMA reads");
        BufferSequence bufSeqRead = new SingleBufferSequence(mgr, mgr.acquire(8).order(ByteOrder.nativeOrder()));
        for (int i = 0; i < 8; ++i)
            {
            Holder<BufferSequence> holder = new SimpleHolder<BufferSequence>(bufSeqRead);
            busClient.read(busServer.getLocalEndPoint(), i * 8, bufSeqRead, holder);
            busClient.flush();

            synchronized (holder)
                {
                while (holder.get() != null)
                    {
                    Blocking.wait(holder);
                    }
                }

            System.out.println(Buffers.toString(bufSeqRead));
            }

        busClient.signal(busServer.getLocalEndPoint(), 456, null);

        System.out.println("memory = " + Buffers.toString(bufSeqLocal, true, 64));

        System.out.println("CAS");
        AtomicLong lResult = new AtomicLong();
        synchronized (lResult)
            {
            busClient.compareAndSwap(busServer.getLocalEndPoint(), 8, 1, 42, lResult, lResult);
            busClient.flush();
            Blocking.wait(lResult);
            }
        System.out.println("lResult = " + lResult);
        System.out.println("memory = " + Buffers.toString(bufSeqLocal, true, 64));

        busClient.signal(busServer.getLocalEndPoint(), 789, null);

        System.out.println("ADD");
        synchronized (lResult)
            {
            busClient.getAndAdd(busServer.getLocalEndPoint(), 8, 1, lResult, lResult);
            busClient.flush();
            Blocking.wait(lResult);
            }
        System.out.println("lResult = " + lResult);
        System.out.println("memory = " + Buffers.toString(bufSeqLocal, true, 64));

        Random rand = new Random();
        BufferSequenceOutputStream out = new BufferSequenceOutputStream(mgr);
        for (int i = 0; i < bufSeqLocal.getLength(); ++i)
            {
            out.write(rand.nextInt());
            }
        BufferSequence bufSeqTmp = out.toBufferSequence();

        System.out.println("large write of " + bufSeqTmp.getBufferCount() + " buffers");

        Holder<BufferSequence> holderBufSeq = new SimpleHolder<BufferSequence>(bufSeqTmp);
        busClient.write(busServer.getLocalEndPoint(), 0, bufSeqTmp, holderBufSeq);
        busClient.flush();

        synchronized (holderBufSeq)
            {
            while (holderBufSeq.get() != null)
                {
                Blocking.wait(holderBufSeq);
                }
            }

        if (Buffers.equals(bufSeqTmp, bufSeqLocal))
            {
            System.out.println("large multi buffer write equality worked");
            }
        else
            {
            System.out.println("!!! large multi buffer write equality failed");
            BufferSequenceInputStream inLocal = new BufferSequenceInputStream(bufSeqLocal);
            BufferSequenceInputStream inTmp   = new BufferSequenceInputStream(bufSeqTmp);

            int cDiff = 0;
            for (long i = 0, c = bufSeqTmp.getLength(); i < c && cDiff < 10; ++i)
                {
                byte bLocal = inLocal.readByte();
                byte bTmp   = inTmp.readByte();

                if (bLocal != bTmp)
                    {
                    System.out.println("byte " + i + " differs " + bLocal + " != " + bTmp);
                    ++cDiff;
                    }
                }
            }

        System.out.println("deep RDMA read");
        synchronized (holderBufSeq)
            {
            holderBufSeq.set(bufSeqRead);
            busClient.read(busServer.getLocalEndPoint(), 1024*100, bufSeqRead, holderBufSeq);
            busClient.flush();

            while (holderBufSeq.get() != null)
                {
                Blocking.wait(holderBufSeq);
                }
            }
        long lRead = bufSeqRead.getBuffer(0).order(ByteOrder.nativeOrder()).getLong();
        System.out.println("lResult = " + lRead);

        System.out.println("deep atomic read");
        lResult = new AtomicLong();
        synchronized (lResult)
            {
            busClient.getAndAdd(busServer.getLocalEndPoint(), 1024*100, 0, lResult, lResult);
            busClient.flush();
            Blocking.wait(lResult);
            }
        System.out.println("lResult = " + lResult);

        System.out.println("atomic == deep rdma read " + (lRead == lResult.get()));

        busClient.close();
        busServer.close();
        }
    }

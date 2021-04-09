/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.messaging.Response;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.DistinctValues;

import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;

import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ConditionalRemove;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.util.Map.Entry;

import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

/**
 * The Protocol used by the Console and Runner.
 *
 * @author jh  2007.02.12
 */
public class RunnerProtocol
        extends AbstractProtocol
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    private RunnerProtocol()
        {
        super();
        }

    // ----- Protocol interface ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getName()
        {
        return "RunnerProtocol";
        }

    // ----- MessageFactory interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    protected Message instantiateMessage(int nType)
        {
        switch (nType)
            {
            case RunnerResponse.TYPE_ID :
                return new RunnerResponse();

            case ClearRequest.TYPE_ID :
                return new ClearRequest();

            case LoadRequest.TYPE_ID :
                return new LoadRequest();

            case IndexRequest.TYPE_ID :
                return new IndexRequest();

            case PutMessage.TYPE_ID :
                return new PutMessage();

            case PutMessage2Serv.TYPE_ID :
                return new PutMessage2Serv();

            case PutMixedMessage.TYPE_ID :
                return new PutMixedMessage();

            case PutMixedContentValueMessage.TYPE_ID :
                return new PutMixedContentValueMessage();

            case PutMixedContentComplexValueMessage.TYPE_ID :
                return new PutMixedContentComplexValueMessage();

            case GetMessage.TYPE_ID :
                return new GetMessage();

            case Get2ServMessage.TYPE_ID :
                return new Get2ServMessage();

            case RunMessage.TYPE_ID :
                return new RunMessage();

            case BenchMessage.TYPE_ID :
                return new BenchMessage();

            case QueryMessage.TYPE_ID :
                return new QueryMessage();

            case DistinctMessage.TYPE_ID :
                return new DistinctMessage();

            case TestResultMessage.TYPE_ID :
                return new TestResultMessage();

            case SampleRequest.TYPE_ID :
                return new SampleRequest();

            default :
                return super.instantiateMessage(nType);
            }
        }

    /**
     * Base class for all test messages.
     */
    public abstract static class AbstractTestMessage<M extends AbstractTestMessage>
            extends AbstractMessage
            implements RemoteCallable<TestResult>, Cloneable
        {
        // ----- RemoteCallable interface -------------------------------

        @Override
        public TestResult call() throws Exception
            {
            TestResult result = run(null);

            if (result != null)
                {
                System.err.println("Class:                 " + getClass().getName());
                System.err.println("Duration:              " + result.getDuration() + "ms");
                System.err.println("Successful Operations: " + result.getSuccessCount());
                System.err.println("Failed Operations:     " + result.getFailureCount());
                System.err.println("Total Rate:            " + result.getRate() + "ops");
                System.err.println("Total Throughput:      " + toBandwidthString(result.getThroughput(), false));
                System.err.println("Latency:               " + result.getLatency());
                }

            return result;
            }

        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public void run()
            {
            Channel           channel    = getChannel();
            MessageFactory    msgFactory = channel.getMessageFactory();
            TestResultMessage msg        = (TestResultMessage) msgFactory.createMessage(TestResultMessage.TYPE_ID);
            TestResult        result     = run(channel);

            msg.setTestResult(result);

            channel.send(msg);
            }


        public TestResult run(Channel channel)
            {
            TestResult result = new TestResult();

            // adjust the thread count, if there are more threads than
            // required to perform the job
            int          cJobSize     = getJobSize();
            int          cThreadCount = Math.min(cJobSize, getThreadCount());
            TestThread[] aThread      = new TestThread[cThreadCount];

            try
                {
                NamedCache cache       = CacheFactory.getCache(getCacheName());
                int        cIterations = getIterationCount();
                int        nStartKey   = getStartKey();

                System.out.println("**** About to run " + getClass().getSimpleName() + " jobSize=" + cJobSize
                                           + " threadCount=" + cThreadCount + " iterations=" + cIterations
                                           + " start=" + nStartKey);

                // sub-divide the job equally among all threads
                int cJobDiv = Math.max(cJobSize / cThreadCount, 1);

                for (int i = 0; i < cThreadCount; ++i)
                    {
                    // calculate the job size for this thread
                    int             cJobSizeCurrent = (i + 1 == cThreadCount) ? cJobSize : cJobDiv;
                    TestThread.Task task            = instantiateTask(cache, nStartKey, cJobSizeCurrent);
                    TestThread      thread          = new TestThread(task, cIterations);

                    thread.start();

                    aThread[i] = thread;
                    cJobSize   -= cJobSizeCurrent;
                    nStartKey  += cJobSizeCurrent;
                    }

                // associate the threads with the channel if we have one
                if (channel != null)
                    {
                    channel.setAttribute(ATTR_TEST_THREADS, aThread);
                    }

                // start the test
                result.start();

                for (int i = 0; i < cThreadCount; ++i)
                    {
                    aThread[i].execute();
                    }

                // wait for the test to complete
                for (int i = 0; i < cThreadCount; ++i)
                    {
                    TestResult resultThread = aThread[i].waitForResult(0L);
                    result.add(resultThread);
                    }

                result.stop();


                for (int i = 0; i < cThreadCount; ++i)
                    {
                    TestResult resultThread = aThread[i].getResult();
                    System.out.println("ThreadResult[" + i + "] " + resultThread);
                    }
                }
            catch (Exception e)
                {
                // interrupt the test threads, if need be
                for (int i = 0; i < cThreadCount; ++i)
                    {
                    Thread thread = aThread[i];

                    if (thread == null)
                        {
                        break;
                        }

                    thread.interrupt();
                    }

                log(e);
                result = null;
                }
            finally
                {
                if (channel != null)
                    {
                    channel.removeAttribute(ATTR_TEST_THREADS);
                    }
                }

            for (TestThread thread : aThread)
                {
                System.out.println(thread.getResult());
                }

            System.out.println("Totals: " + result);

            return result;
            }

        public M getMessageForClient(int nClient, int cClientCount)
            {
            return (M) this;
            }

        protected M splitMessage(int nClient, int cClientCount, M message)
            {
            int cJob     = getJobSize();
            int cJobDiv  = Math.max(cJob / (Math.max(cClientCount, 1)), 1);
            int nStart   = message.getStartKey();

            for (int i=0; i<nClient; i++)
                {
                cJob -= cJobDiv;
                nStart += cJobDiv;
                }

            if (cJob <= 0)
                {
                return null;
                }

            return (M) message.withJobSize(cJobDiv).withStartKey(nStart);
            }

        protected abstract M copy();


        @SuppressWarnings("unchecked")
        protected M copy(M message)
            {
            return (M) message.withCacheName(m_sName)
                    .withIterationCount(m_cIter)
                    .withThreadCount(m_cThread)
                    .withStartKey(m_nStart)
                    .withJobSize(m_cJob)
                    .withBatchSize(m_cBatch);
            }

        @Override
        public String toString()
            {
            return getClass().getSimpleName() + "(" +
                    "cacheName='" + m_sName + '\'' +
                    ", threads=" + m_cThread +
                    ", start=" + m_nStart +
                    ", size=" + m_cJob +
                    ", iterations=" + m_cIter +
                    ", batch=" + m_cBatch +
                    ')';
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sName   = in.readString(0);
            m_cIter   = in.readInt(1);
            m_cThread = in.readInt(2);
            m_nStart  = in.readInt(3);
            m_cJob    = in.readInt(4);
            m_cBatch  = in.readInt(5);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sName);
            out.writeInt(1, m_cIter);
            out.writeInt(2, m_cThread);
            out.writeInt(3, m_nStart);
            out.writeInt(4, m_cJob);
            out.writeInt(5, m_cBatch);
            }

        // ----- internal methods ---------------------------------------

        /**
         * Construct the Task executed by this Message.
         *
         * @param cache   the target NamedCache
         * @param nStart  the starting key
         * @param cJob    the job size
         *
         * @return the new test Task
         */
        protected abstract TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob);

        // ----- accessors ----------------------------------------------

        /**
         * The name of the target NamedCache.
         *
         * @return the name of the target NamedCache
         */
        public String getCacheName()
            {
            return m_sName;
            }

        /**
         * Configure the name of the target NamedCache
         *
         * @param sName  the name of the target NamedCache
         */
        public M withCacheName(String sName)
            {
            assert sName != null;
            m_sName = sName;

            return (M) this;
            }

        /**
         * Return the number of iterations to perform.
         *
         * @return the number of iterations to perform
         */
        public int getIterationCount()
            {
            return m_cIter;
            }

        /**
         * Set the number of iterations to perform.
         *
         * @param cIter  the number of iterations to perform
         */
        public M withIterationCount(int cIter)
            {
            assert cIter > 0;
            m_cIter = cIter;

            return (M) this;
            }

        /**
         * Return the number of threads used to execute the test.
         *
         * @return the number of threads used to execute the test
         */
        public int getThreadCount()
            {
            return m_cThread;
            }

        /**
         * Set the number of threads used to execute the test.
         *
         * @param cThread  the number of threads used to execute the test
         */
        public M withThreadCount(int cThread)
            {
            assert cThread > 0;
            m_cThread = cThread;

            return (M) this;
            }

        /**
         * Return the start key.
         *
         * @return the start key
         */
        public int getStartKey()
            {
            return m_nStart;
            }

        /**
         * Set the start key.
         *
         * @param nStart  the start key
         */
        public M withStartKey(int nStart)
            {
            m_nStart = nStart;

            return (M) this;
            }

        /**
         * Return the job size.
         *
         * @return the job size
         */
        public int getJobSize()
            {
            return m_cJob;
            }

        /**
         * Set the job size.
         *
         * @param cJob  the job size
         */
        public M withJobSize(int cJob)
            {
            assert cJob > 0;
            m_cJob = cJob;

            return (M) this;
            }

        /**
         * Return the batch size.
         *
         * @return the batch size
         */
        public int getBatchSize()
            {
            return m_cBatch;
            }

        /**
         * Set the batch size.
         *
         * @param cBatch  the batch size
         */
        public M withBatchSize(int cBatch)
            {
            assert cBatch > 0;
            m_cBatch = cBatch;

            return (M) this;
            }

        // ----- data members -------------------------------------------

        /**
         * The name of the NamedCache.
         */
        private String m_sName;

        /**
         * The number of iterations.
         */
        private int m_cIter;

        /**
         * The number of threads.
         */
        private int m_cThread;

        /**
         * The start key.
         */
        private int m_nStart;

        /**
         * The job size.
         */
        private int m_cJob;

        /**
         * The batch size.
         */
        private int m_cBatch;
        }

    /**
     * Message to benchmark a NamedCache.
     */
    public static class BenchMessage
            extends AbstractTestMessage<BenchMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected BenchMessage copy()
            {
            BenchMessage message = new BenchMessage()
                    .withType(m_nType)
                    .withPercentPut(m_nPctPut)
                    .withPercentGet(m_nPctGet)
                    .withPercentRemove(m_nPctRemove);

            return copy(message);
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_nType      = in.readInt(6);
            m_nPctGet    = in.readInt(7);
            m_nPctPut    = in.readInt(8);
            m_nPctRemove = in.readInt(9);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(6, m_nType);
            out.writeInt(7, m_nPctGet);
            out.writeInt(8, m_nPctPut);
            out.writeInt(9, m_nPctRemove);
            }

        // ----- PutTask inner class ------------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            PofContext ctx = (PofContext) cache.getCacheService().getSerializer();

            Object     oValue;
            int        cbValue;

            try
                {
                oValue  = ctx.getClass(getType()).newInstance();
                cbValue = ExternalizableHelper.toBinary(oValue, ctx).length();
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }

            return new BenchTask(cache, nStart, cJob, getBatchSize(), getPercentGet(), getPercentPut(),
                                 getPercentRemove(), oValue, cbValue);
            }

        // ----- accessors ----------------------------------------------

        /**
         * Return the POF type ID of the objects to populate the cache with.
         *
         * @return the POF type ID
         */
        public int getType()
            {
            return m_nType;
            }

        /**
         * Set the POF type ID of the objects to populate the cache with.
         *
         * @param nType  the POF type ID
         */
        public BenchMessage withType(int nType)
            {
            assert nType >= 0;
            m_nType = nType;

            return this;
            }

        /**
         * Return the percentage [0-100] of operations that will be gets.
         *
         * @return the percentage of gets
         */
        public int getPercentGet()
            {
            return m_nPctGet;
            }

        /**
         * Set the percentage [0-100] of operations that will be gets.
         *
         * @param nPct  the percentage of gets
         */
        public BenchMessage withPercentGet(int nPct)
            {
            assert nPct >= 0 && nPct <= 100;
            m_nPctGet = nPct;

            return this;
            }

        /**
         * Return the percentage [0-100] of operations that will be puts.
         *
         * @return the percentage of puts
         */
        public int getPercentPut()
            {
            return m_nPctPut;
            }

        /**
         * Set the percentage [0-100] of operations that will be puts.
         *
         * @param nPct  the percentage of puts
         */
        public BenchMessage withPercentPut(int nPct)
            {
            assert nPct >= 0 && nPct <= 100;
            m_nPctPut = nPct;

            return this;
            }

        /**
         * Return the percentage [0-100] of operations that will be removes.
         *
         * @return the percentage of removes
         */
        public int getPercentRemove()
            {
            return m_nPctRemove;
            }

        /**
         * Set the percentage [0-100] of operations that will be removes.
         *
         * @param nPct  the percentage of removes
         */
        public BenchMessage withPercentRemove(int nPct)
            {
            assert nPct >= 0 && nPct <= 100;
            m_nPctRemove = nPct;

            return this;
            }

        // ----- data members -------------------------------------------

        /**
         * Task implementation that performs a benchmark.
         */
        protected static class BenchTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new BenchTask that will perform cJob operations against
             * the given cache, starting at key nStart, in batches of cBatch.
             *
             * @param cache       the target cache
             * @param nStart      the starting key
             * @param cJob        the number of operations to perform
             * @param cBatch      the batch size
             * @param nPctGet     the percentage of operations that will be gets
             * @param nPctPut     the percentage of operations that will be puts
             * @param nPctRemove  the percentage of operations that will be removes
             * @param oValue      a value to use for operations that require one
             * @param cbValue     the serialized size of the value
             */
            protected BenchTask(NamedCache cache, int nStart, int cJob, int cBatch, int nPctGet, int nPctPut,
                                int nPctRemove, Object oValue, int cbValue)
                {
                assert cache != null;
                assert cJob > 0;
                assert cBatch > 0;
                assert nPctGet >= 0;
                assert nPctPut >= 0;
                assert nPctRemove >= 0;
                assert oValue != null;
                assert cbValue > 0;

                assert nPctGet + nPctPut + nPctRemove == 100;

                m_cache   = cache;
                m_nStart  = nStart;
                m_cJob    = cJob;
                m_cBatch  = cBatch;
                m_nPctGet = nPctGet;
                m_nPctPut = nPctPut;
                m_oValue  = oValue;
                m_cbValue = cbValue;
                m_col     = new ArrayList(cBatch);
                m_map     = new HashMap(cBatch);
                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                NamedCache           cache     = m_cache;
                int                  nStart    = m_nStart;
                int                  cJob      = m_cJob;
                int                  cBatch    = m_cBatch;
                int                  nRngGet   = m_nPctGet;
                int                  nRngPut   = nRngGet + m_nPctPut;
                Object               oValue    = m_oValue;
                int                  cbValue   = m_cbValue;
                Collection           col       = m_col;
                Map                  map       = m_map;

                final int            OP_GET    = 0;
                final int            OP_PUT    = 1;
                final int            OP_REMOVE = 2;

                final EntryProcessor EP_PUT    = new ConditionalPut(AlwaysFilter.INSTANCE, oValue);
                final EntryProcessor EP_REMOVE = new ConditionalRemove(AlwaysFilter.INSTANCE, true);

                for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                    {
                    int c = j - i;

                    if (cBatch > c)
                        {
                        cBatch = c;
                        }

                    int n  = getRandom().nextInt(100) + 1;
                    int op = n <= nRngGet
                             ? OP_GET
                             : n <= nRngPut
                               ? OP_PUT
                               : OP_REMOVE;

                    switch (op)
                        {
                        case OP_GET :
                        case OP_REMOVE :
                            col.clear();

                            for (int x = i, z = i + cBatch; x < z; ++x)
                                {
                                col.add(x);
                                }

                            break;

                        case OP_PUT :
                            map.clear();

                            for (int x = i, z = i + cBatch; x < z; ++x)
                                {
                                map.put(x, oValue);
                                }

                            break;
                        }

                    long ldtStart = System.currentTimeMillis();

                    try
                        {
                        switch (op)
                            {
                            case OP_GET :
                                int cGet = cache.getAll(col).size();

                                result.incByteCount(cGet * cbValue);
                                break;

                            case OP_PUT :
                                if (getRandom().nextBoolean())
                                    {
                                    cache.putAll(map);
                                    }
                                else
                                    {
                                    cache.invokeAll(map.keySet(), EP_PUT);
                                    }

                                result.incByteCount(cBatch * cbValue);
                                break;

                            case OP_REMOVE :
                                int cRemove = cBatch - cache.invokeAll(col, EP_REMOVE).size();

                                result.incByteCount(cRemove * cbValue);
                                break;
                            }

                        result.incSuccessCount(1);
                        }
                    catch (RuntimeException e)
                        {
                        log(e);
                        result.incFailureCount(1);
                        }
                    finally
                        {
                        result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                        }
                    }
                }
            // ----- data members -----------------------------------

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The start key.
             */
            private final int m_nStart;

            /**
             * The job size.
             */
            private final int m_cJob;

            /**
             * The batch size.
             */
            private final int m_cBatch;

            /**
             * The percentage of operations that will be gets.
             */
            private final int m_nPctGet;

            /**
             * The percentage of operations that will be gets.
             */
            private final int m_nPctPut;

            /**
             * A new value object.
             */
            private final Object m_oValue;

            /**
             * The size of the value in bytes.
             */
            private final int m_cbValue;

            /**
             * The bulk operation collection.
             */
            private final Collection m_col;

            /**
             * The bulk operation map.
             */
            private final Map m_map;
            }

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 1000;

        /**
         * The POF type ID of the objects to populate the cache with.
         */
        private int m_nType;

        /**
         * The percentage [0-100] of operations that will be gets.
         */
        private int m_nPctGet;

        /**
         * The percentage [0-100] of operations that will be puts.
         */
        private int m_nPctPut;

        /**
         * The percentage [0-100] of operations that will be removes.
         */
        private int m_nPctRemove;
        // ----- constants ----------------------------------------------
        }

    /**
     * Request to clear a NamedCache.
     */
    public static class ClearRequest
            extends AbstractRequest<Boolean>
        {
        public ClearRequest()
            {
            }

        public ClearRequest(String sCacheName)
            {
            m_sName = sCacheName;
            }

        // ----- AbstractRequest methods --------------------------------

        /**
         * {@inheritDoc}
         */
        protected Response instantiateResponse(MessageFactory factory)
            {
            return (Response) factory.createMessage(RunnerResponse.TYPE_ID);
            }

        /**
         * {@inheritDoc}
         */
        public void process(Response response)
            {
            NamedCache cache = CacheFactory.getCache(getCacheName());

            cache.clear();

            response.setResult(Boolean.TRUE);
            }

        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_sName = in.readString(1);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeString(1, m_sName);
            }

        // ----- accessors ----------------------------------------------

        /**
         * The name of the target NamedCache.
         *
         * @return the name of the target NamedCache
         */
        public String getCacheName()
            {
            return m_sName;
            }

        /**
         * Configure the name of the target NamedCache
         *
         * @param sName  the name of the target NamedCache
         */
        public ClearRequest withCacheName(String sName)
            {
            assert sName != null;
            m_sName = sName;

            return this;
            }
        // ----- data members -------------------------------------------

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 1;

        /**
         * The name of the NamedCache to clear.
         */
        private String m_sName;
        // ----- constants ----------------------------------------------
        }

    /**
     * Message to determine the number of distinct values extracted from
     * entries in a NamedCache.
     */
    public static class DistinctMessage
            extends AbstractTestMessage<DistinctMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected DistinctMessage copy()
            {
            DistinctMessage message = new DistinctMessage()
                    .withExtractor(m_sExtractor);

            return copy(message);
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_sExtractor = in.readString(6);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeString(6, m_sExtractor);
            }

        // ----- DistinctTask inner class -------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            return new DistinctTask(cache, getExtractor());
            }

        // ----- accessors ----------------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getBatchSize()
            {
            return 1;
            }

        /**
         * {@inheritDoc}
         */
        public int getJobSize()
            {
            return getThreadCount();
            }

        /**
         * Return the ValueExtractor object that is used to extract an
         * Object from a value stored in the cache.
         *
         * @return the ValueExtractor
         */
        public ValueExtractor getExtractor()
            {
            return new ReflectionExtractor(m_sExtractor);
            }

        /**
         * Configure the ValueExtractor object that is used to extract an
         * Object from a value stored in the cache.
         *
         * @param sExtractor  the ValueExtractor
         */
        public DistinctMessage withExtractor(String sExtractor)
            {
            assert sExtractor != null;
            this.m_sExtractor = sExtractor;

            return this;
            }

        // ----- data members -------------------------------------------

        /**
         * Task implementation that determines the number of distinct values
         * extracted from entries in a NamedCache.
         */
        protected static class DistinctTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new PutTask that will determine the number of distinct
             * values extracted from entries in the given cache.
             *
             * @param cache      the target cache
             * @param extractor  the ValueExtractor that is used to extract an
             *                   Object from a value stored in the cache
             */
            protected DistinctTask(NamedCache cache, ValueExtractor extractor)
                {
                assert cache != null;
                assert extractor != null;

                m_cache     = cache;
                m_extractor = extractor;
                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                long ldtStart = System.currentTimeMillis();

                try
                    {
                    m_cache.aggregate(AlwaysFilter.INSTANCE, new DistinctValues(m_extractor));

                    result.incSuccessCount(1);
                    }
                catch (RuntimeException e)
                    {
                    log(e);
                    result.incFailureCount(1);
                    }
                finally
                    {
                    result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                    }
                }
            // ----- data members -----------------------------------

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The ValueExtractor object that is used to extract an object
             * from a value stored in the cache.
             */
            private final ValueExtractor m_extractor;
            }

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 1002;

        /**
         * The ValueExtractor object that is used to extract an object from a
         * value stored in the cache.
         */
        private String m_sExtractor;

        // ----- constants ----------------------------------------------
        }

    /**
     * Message to access a two NamedCaches.
     */
    public static class Get2ServMessage
            extends AbstractTestMessage<Get2ServMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected Get2ServMessage copy()
            {
            return copy(new Get2ServMessage());
            }

        @Override
        public Get2ServMessage getMessageForClient(int nClient, int cClientCount)
            {
            return splitMessage(nClient, cClientCount, copy());
            }

        // ----- GetTask inner class ------------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            return new Get2ServTask(cache, nStart, cJob, getBatchSize());
            }

        /**
         * Task implementation that performs a bulk get.
         */
        protected static class Get2ServTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new GetTask that will retrieve cJob values from the
             * given cache, starting at key nStart, in batches of cBatch.
             *
             * @param cache    the target cache
             * @param nStart   the starting key
             * @param cJob     the number of values to access
             * @param cBatch   the batch size
             */
            protected Get2ServTask(NamedCache cache, int nStart, int cJob, int cBatch)
                {
                assert cache != null;
                assert cJob > 0;
                assert cBatch > 0;

                m_cache  = cache;
                m_nStart = nStart;
                m_cJob   = cJob;
                m_cBatch = cBatch;
                m_col    = new ArrayList(cBatch);
                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                NamedCache cache  = m_cache;
                NamedCache cache1 = CacheFactory.getCache("dist1-test");
                NamedCache cache2 = CacheFactory.getCache("dist2-test");
                int        nStart = m_nStart;
                int        cJob   = m_cJob;
                int        cBatch = m_cBatch;
                Collection col    = m_col;

                for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                    {
                    col.clear();

                    int c = j - i;

                    if (cBatch > c)
                        {
                        cBatch = c;
                        }

                    for (int x = i, z = i + cBatch; x < z; ++x)
                        {
                        col.add(x);
                        }

                    long ldtStart = System.currentTimeMillis();

                    try
                        {
                        // Map map = cache.getAll(col);
                        Map map1 = cache1.getAll(col);
                        Map map2 = cache2.getAll(col);

                        result.incSuccessCount(1);

                        for (Iterator iter = map1.values().iterator(); iter.hasNext(); )
                            {
                            result.incByteCount(((byte[]) iter.next()).length);
                            }

                        for (Iterator iter = map2.values().iterator(); iter.hasNext(); )
                            {
                            result.incByteCount(((byte[]) iter.next()).length);
                            }
                        }
                    catch (RuntimeException e)
                        {
                        log(e);
                        result.incFailureCount(1);
                        }
                    finally
                        {
                        result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                        }
                    }
                }
            // ----- data members -----------------------------------

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The start key.
             */
            private final int m_nStart;

            /**
             * The job size.
             */
            private final int m_cJob;

            /**
             * The batch size.
             */
            private final int m_cBatch;

            /**
             * The getAll() collection.
             */
            private final Collection m_col;
            }
        // ----- constants ----------------------------------------------

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 31;
        }

    /**
     * Message to access a NamedCache.
     */
    public static class GetMessage
            extends AbstractTestMessage<GetMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected GetMessage copy()
            {
            return copy(new GetMessage());
            }

        @Override
        public GetMessage getMessageForClient(int nClient, int cClientCount)
            {
            return splitMessage(nClient, cClientCount, copy());
            }

        // ----- GetTask inner class ------------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            return new GetTask(cache, nStart, cJob, getBatchSize());
            }

        /**
         * Task implementation that performs a bulk get.
         */
        protected static class GetTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new GetTask that will retrieve cJob values from the
             * given cache, starting at key nStart, in batches of cBatch.
             *
             * @param cache    the target cache
             * @param nStart   the starting key
             * @param cJob     the number of values to access
             * @param cBatch   the batch size
             */
            protected GetTask(NamedCache cache, int nStart, int cJob, int cBatch)
                {
                assert cache != null;
                assert cJob > 0;
                assert cBatch > 0;

                m_cache  = cache;
                m_nStart = nStart;
                m_cJob   = cJob;
                m_cBatch = cBatch;
                m_col    = new ArrayList(cBatch);
                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                NamedCache cache  = m_cache;
                int        nStart = m_nStart;
                int        cJob   = m_cJob;
                int        cBatch = m_cBatch;
                Collection col    = m_col;

                for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                    {
                    col.clear();

                    int c = j - i;

                    if (cBatch > c)
                        {
                        cBatch = c;
                        }

                    for (int x = i, z = i + cBatch; x < z; ++x)
                        {
                        col.add(x);
                        }

                    long ldtStart = System.currentTimeMillis();

                    try
                        {
                        Map map = cache.getAll(col);

                        result.incSuccessCount(1);

                        for (Iterator iter = map.values().iterator(); iter.hasNext(); )
                            {
                            result.incByteCount(((byte[]) iter.next()).length);
                            }
                        }
                    catch (RuntimeException e)
                        {
                        log(e);
                        result.incFailureCount(1);
                        }
                    finally
                        {
                        result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                        }
                    }
                }
            // ----- data members -----------------------------------

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The start key.
             */
            private final int m_nStart;

            /**
             * The job size.
             */
            private final int m_cJob;

            /**
             * The batch size.
             */
            private final int m_cBatch;

            /**
             * The getAll() collection.
             */
            private final Collection m_col;
            }
        // ----- constants ----------------------------------------------

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 3;
        }

    /**
     * Request to add/remove an index to/from a NamedCache.
     */
    public static class IndexRequest
            extends AbstractRequest<Boolean>
        {
        // ----- AbstractRequest methods --------------------------------

        /**
         * {@inheritDoc}
         */
        protected Response instantiateResponse(MessageFactory factory)
            {
            return (Response) factory.createMessage(RunnerResponse.TYPE_ID);
            }

        /**
         * {@inheritDoc}
         */
        public void process(Response response)
            {
            NamedCache cache = CacheFactory.getCache(getCacheName());

            if (isAdd())
                {
                cache.addIndex(getExtractor(), false, null);
                }
            else
                {
                cache.removeIndex(getExtractor());
                }

            response.setResult(Boolean.TRUE);
            }

        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_sName     = in.readString(1);
            m_sExtractor = in.readString(2);
            m_fAdd      = in.readBoolean(3);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeString(1, m_sName);
            out.writeString(2, m_sExtractor);
            out.writeBoolean(3, m_fAdd);
            }

        // ----- accessors ----------------------------------------------

        /**
         * The name of the target NamedCache.
         *
         * @return the name of the target NamedCache
         */
        public String getCacheName()
            {
            return m_sName;
            }

        /**
         * Configure the name of the target NamedCache
         *
         * @param sName  the name of the target NamedCache
         */
        public IndexRequest withCacheName(String sName)
            {
            assert sName != null;
            m_sName = sName;

            return this;
            }

        /**
         * Return the ValueExtractor object that is used to extract an
         * indexable Object from a value stored in the cache.
         *
         * @return the ValueExtractor
         */
        public ValueExtractor getExtractor()
            {
            return new ReflectionExtractor(m_sExtractor);
            }

        /**
         * Configure the ValueExtractor object that is used to extract an
         * indexable Object from a value stored in the cache.
         *
         * @param extractor  the ValueExtractor
         */
        public IndexRequest withExtractor(String extractor)
            {
            assert extractor != null;
            m_sExtractor = extractor;

            return this;
            }

        /**
         * Return true iff an index should be added; otherwise, the index will
         * be removed.
         *
         * @return true iff an index should be added
         */
        public boolean isAdd()
            {
            return m_fAdd;
            }

        /**
         * Configure whether or not to add or remove an index.
         *
         * @param fAdd  true iff an index should be added; otherwise, the
         *              index will be removed
         */
        public IndexRequest add(boolean fAdd)
            {
            m_fAdd = fAdd;

            return this;
            }
        // ----- data members -------------------------------------------

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 101;

        /**
         * The name of the NamedCache to index.
         */
        private String m_sName;

        /**
         * The ValueExtractor object that is used to extract an indexable
         * Object from a value stored in the cache.
         */
        private String m_sExtractor;

        /**
         * True iff an index should be added; otherwise, the index will be
         * removed.
         */
        private boolean m_fAdd;
        // ----- constants ----------------------------------------------
        }

    /**
     * Request to populate a NamedCache.
     */
    public static class LoadRequest
            extends AbstractRequest<Boolean>
        {
        // ----- AbstractRequest methods --------------------------------

        /**
         * {@inheritDoc}
         */
        protected Response instantiateResponse(MessageFactory factory)
            {
            return (Response) factory.createMessage(RunnerResponse.TYPE_ID);
            }

        /**
         * {@inheritDoc}
         */
        public void process(Response response)
            {
            NamedCache cache  = CacheFactory.getCache(getCacheName());

            int        nStart = getStartKey();
            int        cJob   = getJobSize();
            int        cBatch = getBatchSize();
            int        nType  = getType();

            // create a value object
            Object oValue;

            try
                {
                oValue = ((PofContext) cache.getCacheService().getSerializer()).getClass(nType).newInstance();
                }
            catch (Exception e)
                {
                log(e);
                response.setFailure(true);
                response.setResult(e);

                return;
                }

            Map map = new HashMap(cBatch);

            for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                {
                map.clear();

                int c = j - i;

                if (cBatch > c)
                    {
                    cBatch = c;
                    }

                for (int x = i, z = i + cBatch; x < z; ++x)
                    {
                    map.put(x, oValue);
                    }

                cache.putAll(map);
                }

            response.setResult(Boolean.TRUE);
            }

        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_sName  = in.readString(1);
            m_nStart = in.readInt(2);
            m_cJob   = in.readInt(3);
            m_cBatch = in.readInt(4);
            m_nType  = in.readInt(5);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeString(1, m_sName);
            out.writeInt(2, m_nStart);
            out.writeInt(3, m_cJob);
            out.writeInt(4, m_cBatch);
            out.writeInt(5, m_nType);
            }

        // ----- accessors ----------------------------------------------

        /**
         * The name of the target NamedCache.
         *
         * @return the name of the target NamedCache
         */
        public String getCacheName()
            {
            return m_sName;
            }

        /**
         * Configure the name of the target NamedCache
         *
         * @param sName  the name of the target NamedCache
         */
        public LoadRequest withCacheName(String sName)
            {
            assert sName != null;
            m_sName = sName;

            return this;
            }

        /**
         * Return the start key.
         *
         * @return the start key
         */
        public int getStartKey()
            {
            return m_nStart;
            }

        /**
         * Set the start key.
         *
         * @param nStart  the start key
         */
        public LoadRequest withStartKey(int nStart)
            {
            m_nStart = nStart;

            return this;
            }

        /**
         * Return the job size.
         *
         * @return the job size
         */
        public int getJobSize()
            {
            return m_cJob;
            }

        /**
         * Set the job size.
         *
         * @param cJob  the job size
         */
        public LoadRequest withJobSize(int cJob)
            {
            assert cJob > 0;
            m_cJob = cJob;

            return this;
            }

        /**
         * Return the batch size.
         *
         * @return the batch size
         */
        public int getBatchSize()
            {
            return m_cBatch;
            }

        /**
         * Set the batch size.
         *
         * @param cBatch  the batch size
         */
        public LoadRequest withBatchSize(int cBatch)
            {
            assert cBatch > 0;
            m_cBatch = cBatch;

            return this;
            }

        /**
         * Return the POF type ID of the objects to populate the cache with.
         *
         * @return the POF type ID
         */
        public int getType()
            {
            return m_nType;
            }

        /**
         * Set the POF type ID of the objects to populate the cache with.
         *
         * @param nType  the POF type ID
         */
        public LoadRequest withType(int nType)
            {
            assert nType >= 0;
            m_nType = nType;

            return this;
            }

        // ----- data members -------------------------------------------

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 100;

        /**
         * The name of the NamedCache to populate.
         */
        private String m_sName;

        /**
         * The start key.
         */
        private int m_nStart;

        /**
         * The job size.
         */
        private int m_cJob;

        /**
         * The batch size.
         */
        private int m_cBatch;

        /**
         * The POF type ID of the objects to populate the cache with.
         */
        private int m_nType;
        // ----- constants ----------------------------------------------
        }

    /**
     * Message to update a NamedCache.
     */
    public static class PutMessage
            extends AbstractTestMessage<PutMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected PutMessage copy()
            {
            PutMessage message = new PutMessage()
                    .withValueSize(m_cbValue)
                    .withStopOnError(m_fStopOnError);

            return copy(message);
            }

        @Override
        public PutMessage getMessageForClient(int nClient, int cClientCount)
            {
            return splitMessage(nClient, cClientCount, copy());
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_cbValue      = in.readInt(6);
            m_fStopOnError = in.readBoolean(7);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(6, m_cbValue);
            out.writeBoolean(7, m_fStopOnError);
            }

        // ----- PutTask inner class ------------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            byte[] ab = m_abValue;

            if (ab == null)
                {
                ab = m_abValue = new byte[getValueSize()];
                }

            return new PutTask(cache, nStart, cJob, getBatchSize(), ab, isStopOnError());
            }

        // ----- accessors ----------------------------------------------

        /**
         * Return the size of each value in bytes.
         *
         * @return the size of each value in bytes
         */
        public int getValueSize()
            {
            return m_cbValue;
            }

        /**
         * Set the size of each value in bytes.
         *
         * @param cbValue  the size of each value in bytes
         */
        public PutMessage withValueSize(int cbValue)
            {
            assert cbValue >= 0;
            m_cbValue = cbValue;

            return this;
            }

        public boolean isStopOnError()
            {
            return m_fStopOnError;
            }

        public PutMessage withStopOnError(boolean fStopOnError)
            {
            m_fStopOnError = fStopOnError;

            return this;
            }

        // ----- inner class: PutMessage.PutTask ----------------------------

        /**
         * Task implementation that performs a bulk put.
         */
        protected static class PutTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new PutTask that will update cJob values in the
             * given cache, starting at key nStart, in batches of cBatch.
             *
             * @param cache         the target cache
             * @param nStartKey        the starting key
             * @param cJobSize          the number of values to update
             * @param cBatchSize        the batch size
             * @param abValue       the new value
             * @param fStopOnError  the flag indicating that the task should
             *                      stop on an error condition
             */
            protected PutTask(NamedCache cache, int nStartKey, int cJobSize, int cBatchSize, byte[] abValue, boolean fStopOnError)
                {
                assert cache != null;
                assert cJobSize > 0;
                assert cBatchSize > 0;
                assert abValue != null;

                m_cache        = cache;
                m_nStartKey    = nStartKey;
                m_cJobSize     = cJobSize;
                m_cBatchSize   = cBatchSize;
                m_abValue      = abValue;
                m_map          = new HashMap(cBatchSize);
                m_fStopOnError = fStopOnError;
                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                NamedCache  cache   = m_cache;
                int         nStart  = m_nStartKey;
                int         cJob    = m_cJobSize;
                int         cBatch  = m_cBatchSize;
                byte[]      abValue = m_abValue;
                Map         map     = m_map;
                RandomRange rr      = new RandomRange();
                byte[]      tempArray;

                for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                    {
                    map.clear();

                    int c = j - i;

                    if (cBatch > c)
                        {
                        cBatch = c;
                        }

                    for (int x = i, z = i + cBatch; x < z; ++x)
                        {
                        tempArray = new byte[m_abValue.length];
                        // wheel.nextBytes(tempArray);
                        Arrays.fill(tempArray, (byte) rr.getRandomSize(1000));
                        map.put(x, tempArray);
                        }

                    long ldtStart = System.currentTimeMillis();

                    try
                        {
                        cache.putAll(map);

                        result.incSuccessCount(1);
                        result.incByteCount(cBatch * abValue.length);
                        }
                    catch (RuntimeException e)
                        {
                        log(e);
                        result.incFailureCount(1);
                        if (m_fStopOnError)
                            {
                            return;
                            }
                        }
                    finally
                        {
                        result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                        }
                    }
                }
            // ----- data members -----------------------------------

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The start key.
             */
            private final int m_nStartKey;

            /**
             * The job size.
             */
            private final int m_cJobSize;

            /**
             * The batch size.
             */
            private final int m_cBatchSize;

            /**
             * The new value.
             */
            private final byte[] m_abValue;

            /**
             * The putAll() map.
             */
            private final Map m_map;

            /**
             * Flag indicating that the task should stop
             * on an error.
             */
            private boolean m_fStopOnError = false;
            }

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 2;

        /**
         * The size of each value in bytes.
         */
        private int m_cbValue;

        /**
         * Flag indicating that the task should stop
         * on an error.
         */
        private boolean m_fStopOnError = false;

        /**
         * The cached value.
         */
        private byte[] m_abValue;
        }

    /**
     * Message to update a NamedCache.
     */
    public static class PutMessage2Serv
            extends AbstractTestMessage<PutMessage2Serv>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected PutMessage2Serv copy()
            {
            PutMessage2Serv message = new PutMessage2Serv()
                    .withValueSize(m_cbValue);

            return copy(message);
            }

        @Override
        public PutMessage2Serv getMessageForClient(int nClient, int cClientCount)
            {
            return splitMessage(nClient, cClientCount, copy());
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_cbValue = in.readInt(6);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(6, m_cbValue);
            }

        // ----- PutTask2Serv inner class ------------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            byte[] ab = m_abValue;

            if (ab == null)
                {
                ab = m_abValue = new byte[getValueSize()];
                }

            return new PutTask2Serv(cache, nStart, cJob, getBatchSize(), ab);
            }

        // ----- accessors ----------------------------------------------

        /**
         * Return the size of each value in bytes.
         *
         * @return the size of each value in bytes
         */
        public int getValueSize()
            {
            return m_cbValue;
            }

        /**
         * Set the size of each value in bytes.
         *
         * @param cbValue  the size of each value in bytes
         */
        public PutMessage2Serv withValueSize(int cbValue)
            {
            assert cbValue >= 0;
            m_cbValue = cbValue;

            return this;
            }

        // ----- data members -------------------------------------------

        /**
         * Task implementation that performs a bulk put.
         */
        protected static class PutTask2Serv
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new PutTask2Serv that will update cJob values in the
             * given cache, starting at key nStart, in batches of cBatch.
             *
             * @param cache    the target cache
             * @param nStart   the starting key
             * @param cJob     the number of values to update
             * @param cBatch   the batch size
             * @param abValue  the new value
             */
            protected PutTask2Serv(NamedCache cache, int nStart, int cJob, int cBatch, byte[] abValue)
                {
                assert cache != null;
                assert cJob > 0;
                assert cBatch > 0;
                assert abValue != null;

                m_cache   = cache;
                m_nStart  = nStart;
                m_cJob    = cJob;
                m_cBatch  = cBatch;
                m_abValue = abValue;
                m_map     = new HashMap(cBatch);
                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                NamedCache  cache   = m_cache;
                NamedCache  cache1  = CacheFactory.getCache("dist1-test");
                NamedCache  cache2  = CacheFactory.getCache("dist2-test");
                int         nStart  = m_nStart;
                int         cJob    = m_cJob;
                int         cBatch  = m_cBatch;
                byte[]      abValue = m_abValue;
                Map         map     = m_map;
                RandomRange rr      = new RandomRange();
                byte[]      tempArray;

                for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                    {
                    map.clear();

                    int c = j - i;

                    if (cBatch > c)
                        {
                        cBatch = c;
                        }

                    for (int x = i, z = i + cBatch; x < z; ++x)
                        {
                        tempArray = new byte[m_abValue.length];
                        // wheel.nextBytes(tempArray);
                        Arrays.fill(tempArray, (byte) rr.getRandomSize(1000));
                        map.put(x, tempArray);
                        }

                    long ldtStart = System.currentTimeMillis();

                    try
                        {
                        // cache.putAll(map);
                        cache1.putAll(map);
                        cache2.putAll(map);

                        result.incSuccessCount(1);
                        result.incByteCount(cBatch * abValue.length);
                        }
                    catch (RuntimeException e)
                        {
                        log(e);
                        result.incFailureCount(1);
                        }
                    finally
                        {
                        result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                        }
                    }
                }
            // ----- data members -----------------------------------

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The start key.
             */
            private final int m_nStart;

            /**
             * The job size.
             */
            private final int m_cJob;

            /**
             * The batch size.
             */
            private final int m_cBatch;

            /**
             * The new value.
             */
            private final byte[] m_abValue;

            /**
             * The putAll() map.
             */
            private final Map m_map;
            }

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 22;

        /**
         * The size of each value in bytes.
         */
        private int m_cbValue;

        /**
         * The cached value.
         */
        private byte[] m_abValue;
        // ----- constants ----------------------------------------------
        }

    public static class PutMixedContentComplexValueMessage
            extends AbstractTestMessage<PutMixedContentComplexValueMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected PutMixedContentComplexValueMessage copy()
            {
            PutMixedContentComplexValueMessage message = new PutMixedContentComplexValueMessage()
                    .withValueSize(m_cbValue)
                    .withPercentContentValueChange(m_percentContentValueChange)
                    .withM_number_param(m_number_param);

            return copy(message);
            }

        @Override
        public PutMixedContentComplexValueMessage getMessageForClient(int nClient, int cClientCount)
            {
            return splitMessage(nClient, cClientCount, copy());
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_cbValue                   = in.readInt(6);
            m_percentContentValueChange = in.readInt(7);
            withM_number_param(in.readInt(8));
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(6, m_cbValue);
            out.writeInt(7, m_percentContentValueChange);
            out.writeInt(8, getM_number_param());

            }

        // ----- PutMixedContentValueTask inner class ------------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            byte[] ab = m_abValue;

            if (ab == null)
                {
                ab = m_abValue = new byte[getValueSize()];
                }

            // try {                FW.o("\n\n before = " + cache.getCacheName());            } catch (IOException e) {            }
            return new PutMixedContentComplexValueTask(cache, nStart, cJob, getBatchSize(), ab,
                    getPercentContentValueChange(), getM_number_param());
            }

        public int getPercentContentValueChange()
            {
            return m_percentContentValueChange;
            }

        public PutMixedContentComplexValueMessage withPercentContentValueChange(int m_percentContentValueChange)
            {
            this.m_percentContentValueChange = m_percentContentValueChange;

            return this;
            }

        /**
         * @return the m_number_param
         */
        public int getM_number_param()
            {
            return m_number_param;
            }

        /**
         * @param m_number_param the m_number_param to set
         */
        public PutMixedContentComplexValueMessage withM_number_param(int m_number_param)
            {
            this.m_number_param = m_number_param;

            return this;
            }

        // ----- accessors ----------------------------------------------

        /**
         * Return the size of each value in bytes.
         *
         * @return the size of each value in bytes
         */
        public int getValueSize()
            {
            return m_cbValue;
            }

        /**
         * Set the size of each value in bytes.
         *
         * @param cbValue  the size of each value in bytes
         */
        public PutMixedContentComplexValueMessage withValueSize(int cbValue)
            {
            assert cbValue >= 0;
            m_cbValue = cbValue;

            return this;
            }
        // ----- data members -------------------------------------------

        /**
         * Task implementation that performs a bulk put with mixed entry sizes.
         */
        protected static class PutMixedContentComplexValueTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new PutMixedTask that will update cJob values in the
             * given cache, starting at key nStart, in batches of cBatch.
             *
             * @param cache    the target cache
             * @param nStart   the starting key
             * @param cJob     the number of values to update
             * @param cBatch   the batch size
             * @param abValue  the new value
             * @param percentSize the percentage of value changes in bytes
             */
            protected PutMixedContentComplexValueTask(NamedCache cache, int nStart, int cJob, int cBatch,
                    byte[] abValue, int percentSize, int no_param)
                {
                assert cache != null;
                assert cJob > 0;
                assert cBatch > 0;
                assert abValue != null;
                assert percentSize >= 0;
                assert no_param >= 0;

                m_cache       = cache;
                m_nStart      = nStart;
                m_cJob        = cJob;
                m_cBatch      = cBatch;
                m_abValue     = abValue;
                m_percentSize = percentSize;
                m_no_param    = no_param;
                m_map         = new HashMap(cBatch);

                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                NamedCache cache   = m_cache;
                int        nStart  = m_nStart;
                int        cJob    = m_cJob;
                int        cBatch  = m_cBatch;
                byte[]     abValue = m_abValue;
                int        param   = m_no_param;
                Map        map     = m_map;

                for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                    {
                    long batchTotalBytes = 0;

                    map.clear();

                    int c = j - i;

                    if (cBatch > c)
                        {
                        cBatch = c;
                        }

                    // /start
                    RandomRange      generator        = new RandomRange();
                    int              percent          = m_percentSize;    // % percent of value change (0 to 100))
                    int              nestedObjectSize = abValue.length;
                    TestValue tv               = null;
                    ComplexTestValue ctv              = null;

                    for (int x = i, z = i + cBatch; x < z; ++x)
                        {
                        ctv = new ComplexTestValue();
                        tv  = new TestValue();

                        Collection col = new ArrayList();
                        int        per = param;

                        // if number of nested object size is  more than 1 ...work on ComplexTestValue Object's nested objects properties
                        if (nestedObjectSize > 0)
                            {
                            for (int len = 1; len <= nestedObjectSize * percent / 100; len++)
                                {
                                tv = new TestValue();
                                tv.setStreet1(generator.random_Loc_PercntString(tv.getStreet1(), per));
                                tv.setStreet2(generator.random_Loc_PercntString(tv.getStreet2(), per));
                                tv.setCity(generator.random_Loc_PercntString(tv.getCity(), per));
                                tv.setState(generator.random_Loc_PercntString(tv.getState(), per));
                                tv.setZipCode(generator.random_Loc_PercntString(tv.getZipCode(), per));
                                tv.setCountry(generator.random_Loc_PercntString(tv.getCountry(), per));
                                col.add(tv);
                                }

                            for (int count = 1; count <= nestedObjectSize - (nestedObjectSize * percent / 100); count++)
                                {
                                col.add(new TestValue());
                                }

                            ctv.setAddressCollection(col);

                            }    // try {       FW.o(tv.toString()+"\n"); } catch (IOException e) {    }
                        // if number of nested object size is  zero  ...work on ComplexTestValue Object properties itself
                        else
                            {
                            ctv.setStreet1(generator.random_Loc_PercntString(tv.getStreet1(), per));
                            ctv.setCity(generator.random_Loc_PercntString(tv.getCity(), per));
                            ctv.setState(generator.random_Loc_PercntString(tv.getState(), per));
                            ctv.setZipCode(generator.random_Loc_PercntString(tv.getZipCode(), per));
                            ctv.setCountry(generator.random_Loc_PercntString(tv.getCountry(), per));

                            }

                        map.put(x, ctv);
                        batchTotalBytes += tv.toString().length() - 5;
                        }

//                  /end
                    long ldtStart = System.currentTimeMillis();

                    try
                        {
                        cache.putAll(map);

                        result.incSuccessCount(1);
                        result.incByteCount(batchTotalBytes);
                        }
                    catch (RuntimeException e)
                        {
                        result.incFailureCount(1);
                        e.printStackTrace();
                        }
                    finally
                        {
                        result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                        }
                    }
                }
            // ----- data members -----------------------------------

            public int getPercentSize()
                {
                return m_percentSize;
                }

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The start key.
             */
            private final int m_nStart;

            /**
             * The job size.
             */
            private final int m_cJob;

            /**
             * The batch size.
             */
            private final int m_cBatch;

            /**
             * The new value.
             */
            private final byte[] m_abValue;

            /**
             * The new value for the percentage of value changes in bytes.
             */
            private final int m_percentSize;

            /**
             * The number of parameter that need to be changed in a Complex object.
             */
            private final int m_no_param;

            /**
             * The putAll() map.
             */
            private final Map m_map;
            }

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 9;

        /**
         * The size of each value in bytes.
         */
        private int m_cbValue;

        /**
         * The cached value.
         */
        private byte[] m_abValue;

        /**
         * The Percent Content Value Change.
         */
        private int m_percentContentValueChange;

        /**
         * The number of parameter that need to be changed in a Complex object.
         */
        private int m_number_param;
        // ----- constants ----------------------------------------------
        }

    /**
     * Message to update a NamedCache with values of mixed values and sizes.
     */
    public static class PutMixedContentValueMessage
            extends AbstractTestMessage<PutMixedContentValueMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected PutMixedContentValueMessage copy()
            {
            PutMixedContentValueMessage message = new PutMixedContentValueMessage()
                    .withValueSize(m_cbValue)
                    .withPercentContentValueChange(m_percentContentValueChange);

            return copy(message);
            }

        @Override
        public PutMixedContentValueMessage getMessageForClient(int nClient, int cClientCount)
            {
            return splitMessage(nClient, cClientCount, copy());
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_cbValue                   = in.readInt(6);
            m_percentContentValueChange = in.readInt(7);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(6, m_cbValue);
            out.writeInt(7, m_percentContentValueChange);
            }

        // ----- PutMixedContentValueTask inner class ------------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            byte[] ab = m_abValue;

            if (ab == null)
                {
                ab = m_abValue = new byte[getValueSize()];
                }

            return new PutMixedContentValueTask(cache, nStart, cJob, getBatchSize(), ab,
                    getPercentContentValueChange());
            }

        public int getPercentContentValueChange()
            {
            return m_percentContentValueChange;
            }

        public PutMixedContentValueMessage withPercentContentValueChange(int m_percentContentValueChange)
            {
            this.m_percentContentValueChange = m_percentContentValueChange;

            return this;
            }

        // ----- accessors ----------------------------------------------

        /**
         * Return the size of each value in bytes.
         *
         * @return the size of each value in bytes
         */
        public int getValueSize()
            {
            return m_cbValue;
            }

        /**
         * Set the size of each value in bytes.
         *
         * @param cbValue  the size of each value in bytes
         */
        public PutMixedContentValueMessage withValueSize(int cbValue)
            {
            assert cbValue >= 0;
            m_cbValue = cbValue;

            return this;
            }

        // ----- data members -------------------------------------------

        /**
         * Task implementation that performs a bulk put with mixed entry sizes.
         */
        protected static class PutMixedContentValueTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new PutMixedTask that will update cJob values in the
             * given cache, starting at key nStart, in batches of cBatch.
             *
             * @param cache    the target cache
             * @param nStart   the starting key
             * @param cJob     the number of values to update
             * @param cBatch   the batch size
             * @param abValue  the new value
             * @param percentSize the percentage of value changes in bytes
             */
            protected PutMixedContentValueTask(NamedCache cache, int nStart, int cJob, int cBatch, byte[] abValue,
                                               int percentSize)
                {
                assert cache != null;
                assert cJob > 0;
                assert cBatch > 0;
                assert abValue != null;
                assert percentSize >= 0;

                m_cache       = cache;
                m_nStart      = nStart;
                m_cJob        = cJob;
                m_cBatch      = cBatch;
                m_abValue     = abValue;
                m_percentSize = percentSize;
                m_map         = new HashMap(cBatch);

                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                Random     wheel   = new Random();
                NamedCache cache   = m_cache;
                int        nStart  = m_nStart;
                int        cJob    = m_cJob;
                int        cBatch  = m_cBatch;
                byte[]     abValue = m_abValue;
                Map        map     = m_map;

                for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                    {
                    long batchTotalBytes = 0;

                    map.clear();

                    int c = j - i;

                    if (cBatch > c)
                        {
                        cBatch = c;
                        }

                    RandomRange rr     = new RandomRange();
                    int         length = abValue.length * m_percentSize / 100;
                    byte[]      tempArray;

                    for (int x = i, z = i + cBatch; x < z; ++x)
                        {
                        batchTotalBytes += abValue.length;
                        // try {       FW.o("\n\n before = "+Arrays.toString(sourceArray)); } catch (IOException e) {    }
                        tempArray = new byte[length];
                        // wheel.nextBytes(tempArray);
                        Arrays.fill(tempArray, (byte) rr.getRandomSize(1000));    // (int)(range * generator.nextDouble());
                        // try {     FW.o("\n after ="+Arrays.toString(sourceArray));    } catch (IOException e) {   }
                        map.put(x, tempArray);
                        }

                    long ldtStart = System.currentTimeMillis();

                    try
                        {
                        cache.putAll(map);

                        result.incSuccessCount(1);
                        result.incByteCount(batchTotalBytes);
                        }
                    catch (RuntimeException e)
                        {
                        result.incFailureCount(1);
                        e.printStackTrace();
                        }
                    finally
                        {
                        result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                        }
                    }
                }
            // ----- data members -----------------------------------

            public int getPercentSize()
                {
                return m_percentSize;
                }

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The start key.
             */
            private final int m_nStart;

            /**
             * The job size.
             */
            private final int m_cJob;

            /**
             * The batch size.
             */
            private final int m_cBatch;

            /**
             * The new value.
             */
            private final byte[] m_abValue;

            /**
             * The new value for the percentage of value changes in bytes.
             */
            private final int m_percentSize;

            /**
             * The putAll() map.
             */
            private final Map m_map;
            }

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 8;

        /**
         * The size of each value in bytes.
         */
        private int m_cbValue;

        /**
         * The cached value.
         */
        private byte[] m_abValue;

        /**
         * The Percent Content Value Change.
         */
        private int m_percentContentValueChange;
        // ----- constants ----------------------------------------------
        }

    /**
     * Message to update a NamedCache with values of mixed size.
     */
    public static class PutMixedMessage
            extends AbstractTestMessage<PutMixedMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected PutMixedMessage copy()
            {
            PutMixedMessage message = new PutMixedMessage()
                    .withValueSize(m_cbValue);

            return copy(message);
            }

        @Override
        public PutMixedMessage getMessageForClient(int nClient, int cClientCount)
            {
            return splitMessage(nClient, cClientCount, copy());
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_cbValue = in.readInt(6);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(6, m_cbValue);
            }

        // ----- PutMixedTask inner class ------------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            byte[] ab = m_abValue;

            if (ab == null)
                {
                ab = m_abValue = new byte[getValueSize()];
                }

            return new PutMixedTask(cache, nStart, cJob, getBatchSize(), ab);
            }

        // ----- accessors ----------------------------------------------

        /**
         * Return the size of each value in bytes.
         *
         * @return the size of each value in bytes
         */
        public int getValueSize()
            {
            return m_cbValue;
            }

        /**
         * Set the size of each value in bytes.
         *
         * @param cbValue  the size of each value in bytes
         */
        public PutMixedMessage withValueSize(int cbValue)
            {
            assert cbValue >= 0;
            m_cbValue = cbValue;

            return this;
            }

        // ----- data members -------------------------------------------

        /**
         * Task implementation that performs a bulk put with mixed entry sizes.
         */
        protected static class PutMixedTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new PutMixedTask that will update cJob values in the
             * given cache, starting at key nStart, in batches of cBatch.
             *
             * @param cache    the target cache
             * @param nStart   the starting key
             * @param cJob     the number of values to update
             * @param cBatch   the batch size
             * @param abValue  the new value
             */
            protected PutMixedTask(NamedCache cache, int nStart, int cJob, int cBatch, byte[] abValue)
                {
                assert cache != null;
                assert cJob > 0;
                assert cBatch > 0;
                assert abValue != null;

                m_cache   = cache;
                m_nStart  = nStart;
                m_cJob    = cJob;
                m_cBatch  = cBatch;
                m_abValue = abValue;
                m_map     = new HashMap(cBatch);
                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                NamedCache  cache   = m_cache;
                int         nStart  = m_nStart;
                int         cJob    = m_cJob;
                int         cBatch  = m_cBatch;
                byte[]      abValue = m_abValue;
                Map         map     = m_map;
                int         avgSize = m_abValue.length;
                RandomRange rRange  = new RandomRange(avgSize + (avgSize / 2), avgSize - (avgSize / 2));

                for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                    {
                    long batchTotalBytes = 0;

                    map.clear();

                    int c = j - i;

                    if (cBatch > c)
                        {
                        cBatch = c;
                        }

                    /* Use the specified value size as the average size, and vary the size between size +/- 1/2 size */
                    for (int x = i, z = i + cBatch; x < z; ++x)
                        {
                        abValue         = new byte[rRange.getRandomSize()];
                        batchTotalBytes += abValue.length;
                        map.put(x, abValue);
                        }

                    long ldtStart = System.currentTimeMillis();

                    try
                        {
                        cache.putAll(map);

                        result.incSuccessCount(1);
                        result.incByteCount(batchTotalBytes);
                        }
                    catch (RuntimeException e)
                        {
                        result.incFailureCount(1);
                        e.printStackTrace();
                        }
                    finally
                        {
                        result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                        }
                    }
                }
            // ----- data members -----------------------------------

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The start key.
             */
            private final int m_nStart;

            /**
             * The job size.
             */
            private final int m_cJob;

            /**
             * The batch size.
             */
            private final int m_cBatch;

            /**
             * The new value.
             */
            private final byte[] m_abValue;

            /**
             * The putAll() map.
             */
            private final Map m_map;
            }

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 7;

        /**
         * The size of each value in bytes.
         */
        private int m_cbValue;

        /**
         * The cached value.
         */
        private byte[] m_abValue;
        // ----- constants ----------------------------------------------
        }

    /**
     * Message to query a NamedCache.
     */
    public static class QueryMessage
            extends AbstractTestMessage<QueryMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected QueryMessage copy()
            {
            QueryMessage message = new QueryMessage()
                    .withExtractor(m_sExtractor)
                    .withValue(m_oValue);

            return copy(message);
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_sExtractor = in.readString(6);
            m_oValue     = in.readObject(7);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeObject(6, m_sExtractor);
            out.writeObject(7, m_oValue);
            }

        // ----- QueryTask inner class ----------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            Serializer ser = cache.getCacheService().getSerializer();

            // calculate the typical size of an entry in the cache
            int cbEntry;

            try
                {
                Object oKey   = cache.keySet().iterator().next();
                Object oValue = cache.get(oKey);

                cbEntry = ExternalizableHelper.toBinary(oKey, ser).length();
                cbEntry += ExternalizableHelper.toBinary(oValue, ser).length();
                }
            catch (NoSuchElementException e)
                {
                cbEntry = 0;
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }

            return new QueryTask(cache, getExtractor(), getValue(), cbEntry);
            }

        // ----- accessors ----------------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getBatchSize()
            {
            return 1;
            }

        /**
         * {@inheritDoc}
         */
        public int getJobSize()
            {
            return getThreadCount();
            }

        /**
         * Return the ValueExtractor object that is used to extract an
         * Object from a value stored in the cache.
         *
         * @return the ValueExtractor
         */
        public ValueExtractor getExtractor()
            {
            return new ReflectionExtractor(m_sExtractor);
            }

        /**
         * Configure the ValueExtractor object that is used to extract an
         * Object from a value stored in the cache.
         *
         * @param sExtractor  the ValueExtractor
         */
        public QueryMessage withExtractor(String sExtractor)
            {
            assert sExtractor != null;
            m_sExtractor = sExtractor;

            return this;
            }

        /**
         * Return the value to compare with an extracted value.
         *
         * @return the value to compare with an extracted value
         */
        public Object getValue()
            {
            return m_oValue;
            }

        /**
         * Configure the value to compare with an extracted value.
         *
         * @param oValue  the value to compare with an extraced value
         */
        public QueryMessage withValue(Object oValue)
            {
            m_oValue = oValue;

            return this;
            }

        // ----- data members -------------------------------------------

        /**
         * Task implementation that performs a query.
         */
        protected static class QueryTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new QueryTask that will query the given cache.
             *
             * @param cache      the target cache
             * @param extractor  the ValueExtractor that is used to extract an
             *                   Object from a value stored in the cache
             * @param oValue     the value to compare with an extracted value
             * @param cbEntry    the typical size of an entry in the cache
             */
            protected QueryTask(NamedCache cache, ValueExtractor extractor, Object oValue, int cbEntry)
                {
                assert cache != null;
                assert extractor != null;
                assert cbEntry > 0;

                m_cache     = cache;
                m_extractor = extractor;
                m_oValue    = oValue;
                m_cbEntry   = cbEntry;
                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                int  cbEntry  = m_cbEntry;
                long ldtStart = System.currentTimeMillis();

                try
                    {
                    Set set = m_cache.entrySet(new EqualsFilter(m_extractor, m_oValue));

                    for (Iterator iter = set.iterator(); iter.hasNext(); )
                        {
                        Entry entry = (Entry) iter.next();

                        entry.getKey();
                        entry.getValue();

                        result.incByteCount(cbEntry);
                        }

                    result.incSuccessCount(1);
                    }
                catch (RuntimeException e)
                    {
                    log(e);
                    result.incFailureCount(1);
                    }
                finally
                    {
                    result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                    }
                }
            // ----- data members -----------------------------------

            /**
             * The target NamedCache.
             */
            private final NamedCache m_cache;

            /**
             * The ValueExtractor object that is used to extract an object
             * from a value stored in the cache.
             */
            private final ValueExtractor m_extractor;

            /**
             * The value to compare with an extracted value.
             */
            private final Object m_oValue;

            /**
             * The typical size of an entry in the cache.
             */
            private final int m_cbEntry;
            }

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 1001;

        /**
         * The ValueExtractor object that is used to extract an object from a
         * value stored in the cache.
         */
        private String m_sExtractor;

        /**
         * The value to compare with an extracted value.
         */
        private Object m_oValue;
        // ----- constants ----------------------------------------------
        }

    /**
     * Message to simulate an arbitrary cache test.
     */
    public static class RunMessage
            extends AbstractTestMessage<RunMessage>
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        @Override
        protected RunMessage copy()
            {
            RunMessage message = new RunMessage()
                    .withCost(m_cb)
                    .withLatency(m_cMillis);

            return copy(message);
            }

        @Override
        public RunMessage getMessageForClient(int nClient, int cClientCount)
            {
            return splitMessage(nClient, cClientCount, copy());
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_cb      = in.readInt(6);
            m_cMillis = in.readInt(7);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(6, m_cb);
            out.writeInt(7, m_cMillis);
            }

        // ----- RunTask inner class ------------------------------------

        /**
         * {@inheritDoc}
         */
        protected TestThread.Task instantiateTask(NamedCache cache, int nStart, int cJob)
            {
            return new RunTask(nStart, cJob, getBatchSize(), getCost(), getLatency());
            }

        // ----- accessors ----------------------------------------------

        /**
         * Return the cost of each operation in bytes.
         *
         * @return the cost of each operation in bytes
         */
        public int getCost()
            {
            return m_cb;
            }

        /**
         * Set the cost of each operation, in bytes.
         *
         * @param cb  the cost of each operation, in bytes
         */
        public RunMessage withCost(int cb)
            {
            assert cb >= 0;
            m_cb = cb;

            return this;
            }

        /**
         * Return the latency of each batch in milliseconds.
         *
         * @return the latency of each batch in milliseconds
         */
        public int getLatency()
            {
            return m_cMillis;
            }

        /**
         * Set the latency of each batch, in milliseconds.
         *
         * @param cMillis  the latency of each batch, in milliseconds
         */
        public RunMessage withLatency(int cMillis)
            {
            assert cMillis >= 0;
            m_cMillis = cMillis;

            return this;
            }

        // ----- data members -------------------------------------------

        /**
         * Task implementation that simulates an arbitrary cache test.
         */
        protected static class RunTask
                extends Base
                implements TestThread.Task
            {
            // ----- constructors -----------------------------------

            /**
             * Create a new RunTask that will simulate cJob operations on a
             * cache, starting at key nStart, in batches of cBatch.
             *
             * @param nStart   the starting key
             * @param cJob     the number of values to update
             * @param cBatch   the batch size
             * @param cb       the cost of each operation, in bytes
             * @param cMillis  the latency of each batch, in milliseconds
             */
            protected RunTask(int nStart, int cJob, int cBatch, int cb, int cMillis)
                {
                assert cJob > 0;
                assert cBatch > 0;
                assert cb >= 0;
                assert cMillis >= 0;

                m_nStart  = nStart;
                m_cJob    = cJob;
                m_cBatch  = cBatch;
                m_cb      = cb;
                m_cMillis = cMillis;
                }

            // ----- Task interface ---------------------------------

            /**
             * {@inheritDoc}
             */
            public void run(TestResult result)
                {
                int nStart  = m_nStart;
                int cJob    = m_cJob;
                int cBatch  = m_cBatch;
                int cb      = m_cb;
                int cMillis = m_cMillis;

                for (int i = nStart, j = nStart + cJob; i < j; i += cBatch)
                    {
                    int c = j - i;

                    if (cBatch > c)
                        {
                        cBatch = c;
                        }

                    long ldtStart = System.currentTimeMillis();

                    try
                        {
                        if (cMillis > 0)
                            {
                            Thread.sleep(cMillis);
                            }

                        result.incSuccessCount(1);
                        result.incByteCount(cBatch * cb);
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();

                        throw Base.ensureRuntimeException(e);
                        }
                    catch (RuntimeException e)
                        {
                        log(e);
                        result.incFailureCount(1);
                        }
                    finally
                        {
                        result.getLatency().addSample(System.currentTimeMillis() - ldtStart);
                        }
                    }
                }
            // ----- data members -----------------------------------

            /**
             * The start key.
             */
            private final int m_nStart;

            /**
             * The job size.
             */
            private final int m_cJob;

            /**
             * The batch size.
             */
            private final int m_cBatch;

            /**
             * The cost of each operation, in bytes.
             */
            private final int m_cb;

            /**
             * The latency of each batch, in milliseconds.
             */
            private final int m_cMillis;
            }

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 4;

        /**
         * The cost of each operation, in bytes.
         */
        private int m_cb;

        /**
         * The latency of each batch, in milliseconds.
         */
        private int m_cMillis;
        // ----- constants ----------------------------------------------
        }

    // ----- Message classes ------------------------------------------------

    /**
     * Generic Response implementation.
     */
    public static class RunnerResponse
            extends AbstractResponse
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        /**
         * {@inheritDoc}
         */
        public void run()
            {
            }
        // ----- constants ----------------------------------------------

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 0;
        }

    /**
     * Request to return the current test results.
     */
    public static class SampleRequest
            extends AbstractRequest<TestResult>
        {
        // ----- AbstractRequest methods --------------------------------

        /**
         * {@inheritDoc}
         */
        protected Response instantiateResponse(MessageFactory factory)
            {
            return (Response) factory.createMessage(RunnerResponse.TYPE_ID);
            }

        /**
         * {@inheritDoc}
         */
        public void process(Response response)
            {
            Channel      channel = getChannel();
            TestThread[] aThread = (TestThread[]) channel.getAttribute(ATTR_TEST_THREADS);

            if (aThread != null)
                {
                int        cThread   = aThread.length;
                Collection colResult = new ArrayList(cThread);

                for (int i = 0; i < cThread; ++i)
                    {
                    TestResult result = aThread[i].getResult();

                    try
                        {
                        result = (TestResult) result.clone();
                        result.stop();
                        colResult.add(result);
                        }
                    catch (CloneNotSupportedException e)
                        {
                        // ignore
                        }
                    }

                response.setResult(colResult);
                }
            }

        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }
        // ----- constants ----------------------------------------------

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 6;
        }
    // ----- constants ------------------------------------------------------

    /**
     * Message used to send a TestResult from a Runner to a Console.
     */
    public static class TestResultMessage
            extends AbstractMessage
        {
        // ----- Message interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public int getTypeId()
            {
            return TYPE_ID;
            }

        /**
         * {@inheritDoc}
         */
        public void run()
            {
            Console     receiver = (Console) getChannel().getReceiver();

            TestMonitor monitor  = receiver.getTestMonitor();

            if (monitor != null)
                {
                TestResult result = getTestResult();

                out();
                out(result);
                monitor.notify(result);
                }
            }

        // ----- PortableObject interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_result = (TestResult) in.readObject(0);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_result);
            }

        // ----- accessors ----------------------------------------------

        /**
         * The TestResult.
         *
         * @return the TestResult
         */
        public TestResult getTestResult()
            {
            return m_result;
            }

        /**
         * Set the TestResult.
         *
         * @param result  the TestResult
         */
        public void setTestResult(TestResult result)
            {
            assert result != null;
            m_result = result;
            }
        // ----- data members -------------------------------------------

        /**
         * The type identifier of this Message class.
         */
        public static final int TYPE_ID = 5;

        /**
         * The TestResult.
         */
        private TestResult m_result;
        // ----- constants ----------------------------------------------
        }

    /**
     * The singleton AgentProtocol instance.
     */
    public static final RunnerProtocol INSTANCE = new RunnerProtocol();

    /**
     * The Channel attribute used to store the current TestThread array.
     */
    public static final String ATTR_TEST_THREADS = "TestThreadArray";
    }

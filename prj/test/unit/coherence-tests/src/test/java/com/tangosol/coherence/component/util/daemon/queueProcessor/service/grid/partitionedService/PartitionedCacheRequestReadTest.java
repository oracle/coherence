/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService;

import com.tangosol.io.ReadBuffer;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Regression tests for deferred request deserialization failures.
 *
 * @author Aleks Seovic  2026.09.23
 * @since 26.10
 */
public class PartitionedCacheRequestReadTest
    {
    @Test
    public void shouldStopReadingAggregateFilterRequestAfterFilterFailure()
            throws IOException
        {
        assertStopsReading(new FailingAggregateFilterRequest(EXPECTED));
        }

    @Test
    public void shouldStopReadingContinuousAggregationRequestAfterFilterFailure()
            throws IOException
        {
        assertStopsReading(new FailingContinuousAggregationRequest(EXPECTED));
        }

    @Test
    public void shouldStopReadingInvokeFilterRequestAfterFilterFailure()
            throws IOException
        {
        assertStopsReading(new FailingInvokeFilterRequest(EXPECTED));
        }

    @Test
    public void shouldStopReadingQueryRequestAfterFilterFailure()
            throws IOException
        {
        assertStopsReading(new FailingQueryRequest(EXPECTED));
        }

    @Test
    public void shouldStopReadingPartitionedQueryRequestAfterFilterFailure()
            throws IOException
        {
        assertStopsReading(new FailingPartitionedQueryRequest(EXPECTED));
        }

    @Test
    public void shouldStopReadingIndexRequestAfterExtractorFailure()
            throws IOException
        {
        assertStopsReading(new FailingIndexRequest(EXPECTED));
        }

    @Test
    public void shouldStopReadingListenerRequestAfterFilterFailure()
            throws IOException
        {
        assertStopsReading(new FailingListenerRequest(EXPECTED));
        }

    private static void assertStopsReading(FailingRead request)
            throws IOException
        {
        request.readRequest(mock(ReadBuffer.BufferInput.class));

        assertSame(EXPECTED, request.getRequest().getReadException());
        }

    private interface FailingRead
        {
        void readRequest(ReadBuffer.BufferInput input)
                throws IOException;

        com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest getRequest();
        }

    private static class FailingAggregateFilterRequest
            extends PartitionedCache.AggregateFilterRequest
            implements FailingRead
        {
        FailingAggregateFilterRequest(RuntimeException expected)
            {
            m_expected = expected;
            }

        @Override
        public Object readObject(ReadBuffer.BufferInput input)
            {
            throw m_expected;
            }

        @Override
        protected void readTracing(ReadBuffer.BufferInput input)
            {
            fail("must not read tracing after filter deserialization fails");
            }

        @Override
        public void readRequest(ReadBuffer.BufferInput input)
                throws IOException
            {
            read(input);
            }

        @Override
        public com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest getRequest()
            {
            return this;
            }

        private final RuntimeException m_expected;
        }

    private static class FailingInvokeFilterRequest
            extends PartitionedCache.InvokeFilterRequest
            implements FailingRead
        {
        FailingInvokeFilterRequest(RuntimeException expected)
            {
            m_expected = expected;
            }

        @Override
        public Object readObject(ReadBuffer.BufferInput input)
            {
            throw m_expected;
            }

        @Override
        protected void readTracing(ReadBuffer.BufferInput input)
            {
            fail("must not read tracing after filter deserialization fails");
            }

        @Override
        public void readRequest(ReadBuffer.BufferInput input)
                throws IOException
            {
            read(input);
            }

        @Override
        public com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest getRequest()
            {
            return this;
            }

        private final RuntimeException m_expected;
        }

    private static class FailingContinuousAggregationRequest
            extends PartitionedCache.ContinuousAggregationRequest
            implements FailingRead
        {
        FailingContinuousAggregationRequest(RuntimeException expected)
            {
            m_expected = expected;
            }

        @Override
        public Object readObject(ReadBuffer.BufferInput input)
            {
            throw m_expected;
            }

        @Override
        protected void readTracing(ReadBuffer.BufferInput input)
            {
            fail("must not read tracing after filter deserialization fails");
            }

        @Override
        public void readRequest(ReadBuffer.BufferInput input)
                throws IOException
            {
            read(input);
            }

        @Override
        public com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest getRequest()
            {
            return this;
            }

        private final RuntimeException m_expected;
        }

    private static class FailingQueryRequest
            extends PartitionedCache.QueryRequest
            implements FailingRead
        {
        FailingQueryRequest(RuntimeException expected)
            {
            m_expected = expected;
            }

        @Override
        public Object readObject(ReadBuffer.BufferInput input)
            {
            throw m_expected;
            }

        @Override
        protected void readTracing(ReadBuffer.BufferInput input)
            {
            fail("must not read tracing after filter deserialization fails");
            }

        @Override
        public void readRequest(ReadBuffer.BufferInput input)
                throws IOException
            {
            read(input);
            }

        @Override
        public com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest getRequest()
            {
            return this;
            }

        private final RuntimeException m_expected;
        }

    private static class FailingPartitionedQueryRequest
            extends PartitionedCache.PartitionedQueryRequest
            implements FailingRead
        {
        FailingPartitionedQueryRequest(RuntimeException expected)
            {
            m_expected = expected;
            }

        @Override
        public Object readObject(ReadBuffer.BufferInput input)
            {
            throw m_expected;
            }

        @Override
        protected void readTracing(ReadBuffer.BufferInput input)
            {
            fail("must not read tracing after filter deserialization fails");
            }

        @Override
        public void readRequest(ReadBuffer.BufferInput input)
                throws IOException
            {
            read(input);
            }

        @Override
        public com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest getRequest()
            {
            return this;
            }

        private final RuntimeException m_expected;
        }

    private static class FailingIndexRequest
            extends PartitionedCache.IndexRequest
            implements FailingRead
        {
        FailingIndexRequest(RuntimeException expected)
            {
            m_expected = expected;
            }

        @Override
        public Object readObject(ReadBuffer.BufferInput input)
            {
            throw m_expected;
            }

        @Override
        protected void readTracing(ReadBuffer.BufferInput input)
            {
            fail("must not read tracing after extractor deserialization fails");
            }

        @Override
        public void readRequest(ReadBuffer.BufferInput input)
                throws IOException
            {
            read(input);
            }

        @Override
        public com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest getRequest()
            {
            return this;
            }

        private final RuntimeException m_expected;
        }

    private static class FailingListenerRequest
            extends PartitionedCache.ListenerRequest
            implements FailingRead
        {
        FailingListenerRequest(RuntimeException expected)
            {
            m_expected = expected;
            }

        @Override
        public Object readObject(ReadBuffer.BufferInput input)
            {
            throw m_expected;
            }

        @Override
        protected void readTracing(ReadBuffer.BufferInput input)
            {
            fail("must not read tracing after filter deserialization fails");
            }

        @Override
        public void readRequest(ReadBuffer.BufferInput input)
                throws IOException
            {
            read(input);
            }

        @Override
        public com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest getRequest()
            {
            return this;
            }

        private final RuntimeException m_expected;
        }

    private static final RuntimeException EXPECTED = new SecurityException("lambda-target-not-allowed");
    }

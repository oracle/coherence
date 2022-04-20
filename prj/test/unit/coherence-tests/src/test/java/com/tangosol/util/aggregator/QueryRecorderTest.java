/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.Filter;
import com.tangosol.util.QueryRecord;
import com.tangosol.util.SimpleQueryRecord;
import com.tangosol.util.filter.GreaterFilter;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * QueryRecorder unit tests.
 *
 * @author tb 2011.06.01
 */
public class QueryRecorderTest
    {
    /**
    * Test getType.
    */
    @Test
    public void testGetType()
        {
        QueryRecorder recorder = new QueryRecorder(QueryRecorder.RecordType.TRACE);
        assertEquals(QueryRecorder.RecordType.TRACE, recorder.getType());

        recorder = new QueryRecorder(QueryRecorder.RecordType.EXPLAIN);
        assertEquals(QueryRecorder.RecordType.EXPLAIN, recorder.getType());
        }

    /**
    * Test aggregate.
    */
    @Test
    public void testAggregate()
        {
        QueryRecorder recorder = new QueryRecorder(QueryRecorder.RecordType.TRACE);

        try
            {
            recorder.aggregate(new HashSet());
            fail("Expected UnsupportedOperationException");
            }
        catch (UnsupportedOperationException e)
            {
            // expected
            }
        }

    /**
    * Test aggregateResults.
    */
    @Test
    public void testAggregation()
        {
        QueryRecorder recorder = new QueryRecorder(QueryRecorder.RecordType.TRACE);

        Filter filter1 = new GreaterFilter("getFoo", 10);

        recorder.combine(instantiateResult(new int[] {0}, QueryRecorder.RecordType.EXPLAIN, new Filter[] {filter1}));
        recorder.combine(instantiateResult(new int[] {1}, QueryRecorder.RecordType.EXPLAIN, new Filter[] {filter1}));
        recorder.combine(instantiateResult(new int[] {2}, QueryRecorder.RecordType.EXPLAIN, new Filter[] {filter1}));

        QueryRecord record = recorder.finalizeResult();

        assertNotNull(record);
        assertEquals(1, record.getResults().size());
        }

    /**
     * Test serialization.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testSerialization()
            throws Exception
        {

        ByteArrayOutputStream stream = new ByteArrayOutputStream(1000);

        QueryRecorder recorder = new QueryRecorder(QueryRecorder.RecordType.TRACE);
        assertEquals(QueryRecorder.RecordType.TRACE, recorder.getType());

        recorder.writeExternal(new DataOutputStream(stream));

        QueryRecorder recorderNew = new QueryRecorder();

        recorderNew.readExternal(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())));

        assertEquals(QueryRecorder.RecordType.TRACE, recorderNew.getType());

        stream = new ByteArrayOutputStream(1000);

        recorder = new QueryRecorder(QueryRecorder.RecordType.EXPLAIN);
        assertEquals(QueryRecorder.RecordType.EXPLAIN, recorder.getType());

        recorder.writeExternal(new DataOutputStream(stream));

        recorderNew = new QueryRecorder();

        recorderNew.readExternal(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())));

        assertEquals(QueryRecorder.RecordType.EXPLAIN, recorderNew.getType());
        }

    /**
     * Test POF serialization.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testPofSerialization()
            throws Exception
        {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1000);

        byte[]           ab     = new byte[1000];
        WriteBuffer buffer = new ByteArrayWriteBuffer(ab);
        ConfigurablePofContext ctx = new ConfigurablePofContext("com/tangosol/io/pof/include-pof-config.xml");

        QueryRecorder recorder = new QueryRecorder(QueryRecorder.RecordType.TRACE);
        assertEquals(QueryRecorder.RecordType.TRACE, recorder.getType());

        PofBufferWriter pofBufferWriter = new PofBufferWriter(buffer.getBufferOutput(), ctx);

        pofBufferWriter.writeObject(0, recorder);

        PofBufferReader pofBufferReader = new PofBufferReader(new ByteArrayReadBuffer(ab).getBufferInput(), ctx);

        QueryRecorder recorderNew = (QueryRecorder) pofBufferReader.readObject(0);

        assertEquals(QueryRecorder.RecordType.TRACE, recorderNew.getType());


        recorder = new QueryRecorder(QueryRecorder.RecordType.EXPLAIN);
        assertEquals(QueryRecorder.RecordType.EXPLAIN, recorder.getType());

        pofBufferWriter = new PofBufferWriter(buffer.getBufferOutput(), ctx);

        pofBufferWriter.writeObject(0, recorder);

        pofBufferReader = new PofBufferReader(new ByteArrayReadBuffer(ab).getBufferInput(), ctx);

        recorderNew = (QueryRecorder) pofBufferReader.readObject(0);

        assertEquals(QueryRecorder.RecordType.EXPLAIN, recorderNew.getType());
        }


    // ----- helper methods -------------------------------------------------

    private static SimpleQueryRecord.PartialResult instantiateResult(
            int[] aPartition, QueryRecorder.RecordType type, Filter[] aFilter)
        {
        PartitionSet partitions = new PartitionSet(100);

        for (int i = 0; i < aPartition.length; i++)
            {
            partitions.add(aPartition[i]);
            }

        final SimpleQueryRecord.PartialResult result =
                new SimpleQueryRecord.PartialResult(partitions);

        for (int i = 0; i < aFilter.length; i++)
            {
            Filter filter = aFilter[i];
            switch (type)
                {
                case EXPLAIN:
                    result.instantiateExplainStep(filter);
                    break;
                case TRACE:
                    result.instantiateTraceStep(filter);
                    break;
                }
            }
        return result;
        }
    }

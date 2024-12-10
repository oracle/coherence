/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.aggregator.QueryRecorder;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


/**
 * SimpleQueryRecord, SimpleQueryRecord.PartialResult and
 * SimpleQueryRecord.PartialResult.Step unit tests.
 *
 * @author tb 2011.06.01
 */
public class SimpleQueryRecordTest
    {
    // ----- QueryRecord tests ----------------------------------------------

    /**
     * Test getType.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testGetType()
            throws Exception
        {
        SimpleQueryRecord record = new SimpleQueryRecord(QueryRecorder.RecordType.EXPLAIN, new HashSet());
        assertEquals(QueryRecorder.RecordType.EXPLAIN, record.getType());

        record = new SimpleQueryRecord(QueryRecorder.RecordType.TRACE, new HashSet());
        assertEquals(QueryRecorder.RecordType.TRACE, record.getType());
        }

    /**
     * Test getResults.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testGetResults()
            throws Exception
        {
        List<SimpleQueryRecord.PartialResult> listResults = new LinkedList<SimpleQueryRecord.PartialResult>();

        Filter filter1 = new GreaterFilter("getFoo", 10);

        listResults.add(instantiateResult(new int[]{0}, QueryRecorder.RecordType.EXPLAIN, new Filter[]{filter1}));
        listResults.add(instantiateResult(new int[]{1}, QueryRecorder.RecordType.EXPLAIN, new Filter[]{filter1}));
        listResults.add(instantiateResult(new int[]{2}, QueryRecorder.RecordType.EXPLAIN, new Filter[]{filter1}));

        SimpleQueryRecord record = new SimpleQueryRecord(QueryRecorder.RecordType.EXPLAIN, listResults);

        List<? extends QueryRecord.PartialResult> listRecordedResults = record.getResults();

        assertEquals(1, listRecordedResults.size());
        assertEquals(1, listRecordedResults.get(0).getSteps().size());

        PartitionSet parts = listRecordedResults.get(0).getPartitions();

        assertTrue (parts.contains(0));
        assertTrue (parts.contains(1));
        assertTrue(parts.contains(2));
        assertFalse(parts.contains(3));

        final Filter filter2 = new GreaterFilter("getFoo", 11);

        listResults.add(instantiateResult(new int[]{3}, QueryRecorder.RecordType.EXPLAIN, new Filter[]{filter2}));

        record = new SimpleQueryRecord(QueryRecorder.RecordType.EXPLAIN, listResults);

        listRecordedResults = record.getResults();

        assertEquals(2, listRecordedResults.size());
        assertEquals(1, listRecordedResults.get(0).getSteps().size());
        assertEquals(1, listRecordedResults.get(1).getSteps().size());

        parts = listRecordedResults.get(0).getPartitions();

        assertTrue (parts.contains(0));
        assertTrue (parts.contains(1));
        assertTrue (parts.contains(2));

        parts = listRecordedResults.get(1).getPartitions();

        assertTrue(parts.contains(3));
        }


    // ----- QueryRecord.PartialResult --------------------------------------

    /**
     * Test PartialResult.getSteps.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testResultGetSteps()
            throws Exception
        {
        Filter filter1 = new GreaterFilter("getFoo", 10);
        Filter filter2 = new GreaterFilter("getFoo", 11);
        Filter filter3 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult result =
                instantiateResult(new int[]{0}, QueryRecorder.RecordType.EXPLAIN, new Filter[]{filter1, filter2, filter3});

        assertEquals(3, result.getSteps().size());
        }

    /**
     * Test PartialResult.getPartitions.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testResultGetPartitions()
            throws Exception
        {
        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult result =
                instantiateResult(new int[]{0, 1, 3}, QueryRecorder.RecordType.EXPLAIN, new Filter[]{filter1});

        PartitionSet parts = result.getPartitions();

        assertTrue(parts.contains(0));
        assertTrue (parts.contains(1));
        assertFalse(parts.contains(2));
        assertTrue(parts.contains(3));
        }

    /**
     * Test PartialResult serialization.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testResultSerialization()
            throws Exception
        {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1000);

        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult result =
                instantiateResult(new int[]{0, 1, 3}, QueryRecorder.RecordType.EXPLAIN, new Filter[]{filter1});

        result.writeExternal(new DataOutputStream(stream));

        SimpleQueryRecord.PartialResult resultNew = new SimpleQueryRecord.PartialResult();

        resultNew.readExternal(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())));

        assertTrue(result.isMatching(resultNew));
        assertTrue(resultNew.isMatching(result));
        assertEquals(result.getPartitions(), resultNew.getPartitions());
        }

    /**
     * Test PartialResult POF serialization.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testResultPofSerialization()
            throws Exception
        {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1000);

        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult result =
                instantiateResult(new int[]{0, 1, 3}, QueryRecorder.RecordType.EXPLAIN, new Filter[]{filter1});

        byte[]           ab     = new byte[1000];
        WriteBuffer      buffer = new ByteArrayWriteBuffer(ab);
        ConfigurablePofContext ctx = new ConfigurablePofContext("com/tangosol/io/pof/include-pof-config.xml");

        PofBufferWriter pofBufferWriter = new PofBufferWriter(buffer.getBufferOutput(), ctx);

        pofBufferWriter.writeObject(0, result);

        PofBufferReader pofBufferReader = new PofBufferReader(new ByteArrayReadBuffer(ab).getBufferInput(), ctx);

        SimpleQueryRecord.PartialResult resultNew = (SimpleQueryRecord.PartialResult) pofBufferReader.readObject(0);

        assertTrue(result.isMatching(resultNew));
        assertTrue(resultNew.isMatching(result));
        assertEquals(result.getPartitions(), resultNew.getPartitions());
        }


    // ----- QueryRecord.PartialResult.Step ---------------------------------

    /**
     * Test Step copy constructor.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testStepCopyConstructor()
            throws Exception
        {
        Filter filter1 = new GreaterFilter("getFoo", 10);
        SimpleQueryRecord.PartialResult.Step step = new SimpleQueryRecord.PartialResult.Step(filter1);

        step.m_cMillis = 1000L;
        step.m_nEfficiency = 100;
        step.m_nSizeIn = 9999;
        step.m_nSizeOut = 9998;

        SimpleQueryRecord.PartialResult.Step newStep = new SimpleQueryRecord.PartialResult.Step(step);

        assertEquals(step.m_sFilter, newStep.m_sFilter);
        assertEquals(step.m_cMillis, newStep.m_cMillis);
        assertEquals(step.m_nEfficiency, newStep.m_nEfficiency);
        assertEquals(step.m_nSizeIn, newStep.m_nSizeIn);
        assertEquals(step.m_nSizeOut, newStep.m_nSizeOut);
        }

    /**
     * Test Step getSteps().
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testStepGetSteps()
            throws Exception
        {
        Filter filter1 = new GreaterFilter("getFoo", 10);
        Filter filter2 = new GreaterFilter("getFoo", 11);
        Filter filter3 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult result =
                instantiateResult(new int[]{0}, QueryRecorder.RecordType.EXPLAIN, new Filter[]{filter1});

        SimpleQueryRecord.PartialResult.ExplainStep explainStep = (SimpleQueryRecord.PartialResult.ExplainStep) result.getSteps().get(0);

        explainStep.ensureStep(filter2);
        explainStep.ensureStep(filter3);

        assertEquals(2, explainStep.getSteps().size());

        result = instantiateResult(new int[]{0}, QueryRecorder.RecordType.TRACE, new Filter[]{filter1});

        SimpleQueryRecord.PartialResult.TraceStep traceStep = (SimpleQueryRecord.PartialResult.TraceStep) result.getSteps().get(0);

        traceStep.ensureStep(filter2);
        traceStep.ensureStep(filter3);

        assertEquals(2, traceStep.getSteps().size());
        }

    /**
     * Test Step getFilterDescription().
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testStepGetFilterDescription()
            throws Exception
        {
        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult.Step step = new SimpleQueryRecord.PartialResult.Step(filter1);

        assertEquals(filter1.toString(), step.getFilterDescription());
        }

    /**
     * Test Step getEfficiency().
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testStepGetEfficiency()
            throws Exception
        {
        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult.Step step = new SimpleQueryRecord.PartialResult.Step(filter1);

        step.m_nEfficiency = 99;

        assertEquals(99, step.getEfficiency());
        }

    /**
     * Test Step getPreFilterKeySetSize().
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testStepGetPreFilterKeySetSize()
            throws Exception
        {
        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult.Step step = new SimpleQueryRecord.PartialResult.Step(filter1);

        step.m_nSizeIn = 98;

        assertEquals(98, step.getPreFilterKeySetSize());
        }

    /**
     * Test Step getPostFilterKeySetSize().
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testStepGetPostFilterKeySetSize()
            throws Exception
        {
        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult.Step step = new SimpleQueryRecord.PartialResult.Step(filter1);

        step.m_nSizeOut = 97;

        assertEquals(97, step.getPostFilterKeySetSize());
        }

    /**
     * Test Step getDuration().
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void getDuration()
            throws Exception
        {
        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult.Step step = new SimpleQueryRecord.PartialResult.Step(filter1);

        step.m_cMillis = 96L;

        assertEquals(96L, step.getDuration());
        }

    /**
     * Test Step serialization.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testStepSerialization()
            throws Exception
        {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1000);

        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult.Step step = new SimpleQueryRecord.PartialResult.Step(filter1);

        step.writeExternal(new DataOutputStream(stream));

        SimpleQueryRecord.PartialResult.Step stepNew = new SimpleQueryRecord.PartialResult.Step();

        stepNew.readExternal(new DataInputStream(new ByteArrayInputStream(stream.toByteArray())));

        assertTrue(step.isMatching(stepNew));
        assertTrue(stepNew.isMatching(step));
        }

    /**
     * Test PartialResult POF serialization.
     *
     * @throws Exception  rethrow any exception to be caught by test framework
     */
    @Test
    public void testStepPofSerialization()
            throws Exception
        {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(1000);

        Filter filter1 = new GreaterFilter("getFoo", 10);

        SimpleQueryRecord.PartialResult.Step step = new SimpleQueryRecord.PartialResult.Step(filter1);

        byte[]           ab     = new byte[1000];
        WriteBuffer      buffer = new ByteArrayWriteBuffer(ab);
        ConfigurablePofContext ctx = new ConfigurablePofContext("com/tangosol/io/pof/include-pof-config.xml");

        PofBufferWriter pofBufferWriter = new PofBufferWriter(buffer.getBufferOutput(), ctx);

        pofBufferWriter.writeObject(0, step);

        PofBufferReader pofBufferReader = new PofBufferReader(new ByteArrayReadBuffer(ab).getBufferInput(), ctx);

        SimpleQueryRecord.PartialResult.Step stepNew = (SimpleQueryRecord.PartialResult.Step) pofBufferReader.readObject(0);

        assertTrue(step.isMatching(stepNew));
        assertTrue(stepNew.isMatching(step));
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

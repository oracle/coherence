/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import com.tangosol.coherence.performance.AbstractPerformanceTests;
import com.tangosol.coherence.performance.PsrPerformanceEnvironment;
import com.tangosol.util.extractor.ReflectionExtractor;
import org.junit.Test;

/**
 * @author jk 2015.12.09
 */
public class AggregateWithoutIndexTest
        extends AbstractPerformanceTests
    {
    // ----- constructors ---------------------------------------------------

    public AggregateWithoutIndexTest(String description, PsrPerformanceEnvironment environment)
        {
        super(description, environment
                .withClusterMemberRunners()
                .withPofEnabled(true)
                .withConsole(false));
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldRunAggregatorTest() throws Exception
        {
        ReflectionExtractor extractor  = new ReflectionExtractor("getCity");
        String              sCacheName = "dist-test";

        f_environment.getCluster().getCache(sCacheName).clear();

        RunnerProtocol.LoadRequest loadRequest = new RunnerProtocol.LoadRequest()
                .withCacheName(sCacheName)
                .withStartKey(1)
                .withJobSize(10000)
                .withBatchSize(1000)
                .withType(1000);

        f_environment.submitToSingleClient(loadRequest);

        RunnerProtocol.IndexRequest indexRequest = new RunnerProtocol.IndexRequest()
                .withCacheName(sCacheName)
                .withExtractor(extractor.getMethodName())
                .add(false);

        f_environment.submitToSingleClient(indexRequest);

        RunnerProtocol.DistinctMessage distinctMessage = new RunnerProtocol.DistinctMessage()
                .withCacheName(sCacheName)
                .withThreadCount(1)
                .withExtractor(extractor.getMethodName());

        f_environment.submitToAllClients(distinctMessage.withIterationCount(10));
        f_environment.submitToAllClients(distinctMessage.withIterationCount(2300));

        TestResult result = submitTest("AggregateWithoutIndexTest", distinctMessage.withIterationCount(4600));

        processResults(result);
        }
    }

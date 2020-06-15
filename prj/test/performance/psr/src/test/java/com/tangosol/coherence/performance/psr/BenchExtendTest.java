/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import com.tangosol.coherence.performance.AbstractPerformanceTests;
import com.tangosol.coherence.performance.PsrPerformanceEnvironment;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author jk 2015.12.09
 */
public class BenchExtendTest
        extends AbstractPerformanceTests
    {
    // ----- constructors ---------------------------------------------------

    public BenchExtendTest(String description, PsrPerformanceEnvironment environment)
        {
        super(description, environment
                .withExtendClientRunners()
                .withPofEnabled(true)
                .withConsole(false));
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldRunBenchTest() throws Exception
        {
        int cDefault = 100000;
        int cSize    = Integer.getInteger("test.bench.extend.count", 100000);

        if (cSize <= 0)
            {
            cSize = cDefault;
            }

        f_environment.getCluster().getCache("extend-test").clear();

        RunnerProtocol.LoadRequest loadRequest = new RunnerProtocol.LoadRequest()
                .withCacheName("extend-test")
                .withStartKey(1)
                .withJobSize(10000)
                .withBatchSize(1000)
                .withType(1000);

        f_environment.submitToSingleClient(loadRequest);

        RunnerProtocol.BenchMessage benchMessage = new RunnerProtocol.BenchMessage()
                .withCacheName("extend-test")
                .withThreadCount(1)
                .withStartKey(1)
                .withJobSize(cSize)
                .withBatchSize(100)
                .withType(1000)
                .withPercentPut(60)
                .withPercentGet(30)
                .withPercentRemove(10);

        f_environment.submitToAllClients(benchMessage.withIterationCount(500));

        TestResult result = submitTest("BenchExtendTest", benchMessage.withIterationCount(1000), 20, TimeUnit.MINUTES);

        processResults(result);
        }
    }

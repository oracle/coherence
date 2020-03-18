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

/**
 * @author jk 2015.12.09
 */
public class BenchDistTest
        extends AbstractPerformanceTests
    {
    // ----- constructors ---------------------------------------------------

    public BenchDistTest(String description, PsrPerformanceEnvironment environment)
        {
        super(description, environment);

        environment.withClientConfiguration("server-cache-config-one-worker-thread.xml")
                .withClusterMemberRunners()
                .withPofEnabled(true)
                .withConsole(false);
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldRunBenchTest() throws Exception
        {
        int cDefault = 100000;
        int cSize    = Integer.getInteger("test.bench.dist.count", cDefault);

        if (cSize <= 0)
            {
            cSize = cDefault;
            }

        f_environment.getCluster().getCache("trans").clear();

        RunnerProtocol.LoadRequest loadRequest = new RunnerProtocol.LoadRequest()
                .withCacheName("trans")
                .withStartKey(1)
                .withJobSize(10000)
                .withBatchSize(1000)
                .withType(1000);

        f_environment.submitToSingleClient(loadRequest);

        RunnerProtocol.BenchMessage benchMessage = new RunnerProtocol.BenchMessage()
                .withCacheName("trans")
                .withThreadCount(1)
                .withStartKey(1)
                .withJobSize(cSize)
                .withBatchSize(100)
                .withType(1000)
                .withPercentPut(60)
                .withPercentGet(30)
                .withPercentRemove(10);

        f_environment.submitToAllClients(benchMessage.withIterationCount(60));

        TestResult result = submitTest("BenchDistTest", benchMessage.withIterationCount(120));

        processResults(result);
        }
    }

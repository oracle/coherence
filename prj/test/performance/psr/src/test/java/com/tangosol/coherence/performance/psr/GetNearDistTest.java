/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.tangosol.coherence.performance.AbstractPerformanceTests;
import com.tangosol.coherence.performance.PsrPerformanceEnvironment;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author jk 2015.12.09
 */
public class GetNearDistTest
        extends AbstractPerformanceTests
    {
    // ----- constructors ---------------------------------------------------

    public GetNearDistTest(String description, PsrPerformanceEnvironment environment)
        {
        super(description, environment
                .withClusterMemberRunners()
                .withConsole(false));
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldRunGetsTest() throws Exception
        {
        int cGetsDefault = 750000;
        int cGets        = Integer.getInteger("test.get.count", cGetsDefault);

        if (cGets <= 0)
            {
            cGets = cGetsDefault;
            }

        int              cPuts      = cGets * 2;
        String           sCacheName = "near-dist";
        CoherenceCluster cluster    = f_environment.getCluster();

        cluster.getCache(sCacheName).clear();

        RunnerProtocol.PutMessage putMessage = new RunnerProtocol.PutMessage()
                .withCacheName(sCacheName)
                .withIterationCount(1)
                .withThreadCount(5)
                .withStartKey(1)
                .withJobSize(cPuts)
                .withBatchSize(1)
                .withValueSize(1024);

        f_environment.submitToAllClients(putMessage, 60, TimeUnit.MINUTES);
        f_environment.submitToAllClients(putMessage, 60, TimeUnit.MINUTES);

        RunnerProtocol.GetMessage getMessage = new RunnerProtocol.GetMessage()
                .withCacheName(sCacheName)
                .withThreadCount(5)
                .withStartKey(1)
                .withJobSize(cGets)
                .withBatchSize(50);

        f_environment.submitToAllClients(getMessage.withIterationCount(3200), 60, TimeUnit.MINUTES);

        TestResult result = submitTest("GetNearDistTest", getMessage.withIterationCount(6400), 60, TimeUnit.MINUTES);

        processResults(result);
        }
    }

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

/**
 * @author jk 2015.12.09
 */
public class GetExtendTest
        extends AbstractPerformanceTests
    {
    // ----- constructors ---------------------------------------------------

    public GetExtendTest(String description, PsrPerformanceEnvironment environment)
        {
        super(description, environment
                .withExtendClientRunners()
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
        String           sCacheName = "extend-test";
        CoherenceCluster cluster    = f_environment.getCluster();

        cluster.getCache(sCacheName).clear();

        RunnerProtocol.PutMessage putMessage = new RunnerProtocol.PutMessage()
                .withCacheName(sCacheName)
                .withIterationCount(4)
                .withThreadCount(5)
                .withStartKey(1)
                .withJobSize(cGets)
                .withBatchSize(1)
                .withValueSize(1024);

        f_environment.submitToAllClients(putMessage);

        RunnerProtocol.GetMessage getMessage = new RunnerProtocol.GetMessage()
                .withCacheName(sCacheName)
                .withThreadCount(5)
                .withStartKey(1)
                .withJobSize(cGets)
                .withBatchSize(1);

        f_environment.submitToAllClients(getMessage.withIterationCount(4));

        TestResult result = submitTest("GetExtendTest", getMessage.withIterationCount(8));

        processResults(result);
        }
    }

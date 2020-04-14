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
public class PutNearExtendTest
        extends AbstractPerformanceTests
    {
    // ----- constructors ---------------------------------------------------

    public PutNearExtendTest(String description, PsrPerformanceEnvironment environment)
        {
        super(description, environment.withExtendClientRunners());
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldRunPutsTest() throws Exception
        {
        String           sCacheName   = "near-extend";
        CoherenceCluster cluster      = f_environment.getCluster();
        int              cPutsDefault = 1000000;
        int              cPutsInit    = Integer.getInteger("test.put.count", cPutsDefault);

        if (cPutsInit <= 0)
            {
            cPutsInit = cPutsDefault;
            }

        int cPutsTest = cPutsInit / 2;

        cluster.getCache(sCacheName).clear();

        RunnerProtocol.PutMessage putMessage = new RunnerProtocol.PutMessage()
                .withCacheName(sCacheName)
                .withIterationCount(1)
                .withThreadCount(5)
                .withStartKey(1)
                .withJobSize(cPutsInit)
                .withBatchSize(1)
                .withValueSize(1024);

        f_environment.submitToAllClients(putMessage);
        f_environment.submitToAllClients(putMessage);

        RunnerProtocol.GetMessage getMessage = new RunnerProtocol.GetMessage()
                .withCacheName(sCacheName)
                .withIterationCount(1)
                .withThreadCount(5)
                .withStartKey(1)
                .withJobSize(cPutsTest)
                .withBatchSize(1);

        f_environment.submitToAllClients(getMessage);
        f_environment.submitToAllClients(putMessage.withIterationCount(3));

        TestResult result = submitTest("PutNearExtendTest", putMessage.withJobSize(cPutsTest).withIterationCount(6));

        processResults(result);
        }
    }

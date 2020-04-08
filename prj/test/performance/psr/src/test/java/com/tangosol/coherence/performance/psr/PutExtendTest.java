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
public class PutExtendTest
        extends AbstractPerformanceTests
    {
    // ----- constructors ---------------------------------------------------

    public PutExtendTest(String description, PsrPerformanceEnvironment environment)
        {
        super(description, environment
                .withExtendClientRunners()
                .withConsole(false));
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldRunPutsTest() throws Exception
        {
        String           sCacheName   = "extend-test";
        CoherenceCluster cluster      = f_environment.getCluster();
        int              cPutsDefault = 1000000;
        int              cPuts        = Integer.getInteger("test.put.count", cPutsDefault);

        if (cPuts <= 0)
            {
            cPuts = cPutsDefault;
            }

        cluster.getCache(sCacheName).clear();

        RunnerProtocol.PutMessage putMessage = new RunnerProtocol.PutMessage()
                .withCacheName(sCacheName)
                .withThreadCount(5)
                .withStartKey(1)
                .withJobSize(cPuts)
                .withBatchSize(50)
                .withValueSize(1024);

        f_environment.submitToAllClients(putMessage.withIterationCount(15));

        TestResult result = submitTest("PutExtendTest", putMessage.withIterationCount(25));

        processResults(result);
        }
    }

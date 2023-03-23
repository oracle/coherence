/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.cluster;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.net.CacheFactory;
import org.junit.Test;

import com.oracle.coherence.testing.SystemPropertyResource;

import static com.oracle.bedrock.testsupport.deferred.Eventually.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * Test the DefaultServiceFailurePolicy configuring exit-process and timeout for gracefully exiting JVM process.
 *
 * @author jf 2022.12.12
 */
public class DefaultServiceFailurePolicyTest
    {
    @Test
    public void testSystemPropertyForShutdownTimeout()
        {
        String sShutDownTimeout   = "12m";
        long   ldtShutdownTimeout = new Duration(sShutDownTimeout).as(Duration.Magnitude.MILLI);

        try (SystemPropertyResource r1 = new SystemPropertyResource("coherence.shutdown.timeout", sShutDownTimeout);
             SystemPropertyResource r2 = new SystemPropertyResource("java.net.preferIPv4Stack", "true"))
            {
            DefaultServiceFailurePolicy policy = new DefaultServiceFailurePolicy(DefaultServiceFailurePolicy.POLICY_EXIT_PROCESS);

            assertThat(policy.getPolicyType(), is(DefaultServiceFailurePolicy.POLICY_EXIT_PROCESS));
            assertThat(DefaultServiceFailurePolicy.getShutdownTimeout(CacheFactory.ensureCluster()), is(ldtShutdownTimeout));
            }
        }
    }

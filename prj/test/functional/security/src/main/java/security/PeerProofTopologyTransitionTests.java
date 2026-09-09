/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Real-cluster monotonic topology-transition coverage for peer-proof readiness.
 *
 * @author Aleks Seovic  2026.07.21
 */
public class PeerProofTopologyTransitionTests
    {
    @BeforeClass
    public static void startCluster()
        {
        StorageAccessAuthorizerRouteTests._startup();
        }

    @AfterClass
    public static void stopCluster()
        {
        StorageAccessAuthorizerRouteTests.stopServer();
        }

    @Test
    public void shouldAbortAfterServiceAndOwnershipAbaTransitions() throws Exception
        {
        StorageAccessAuthorizerRouteTests
                .assertProductionReadinessAbortsAfterServiceAndOwnershipAbaTransitions();
        }
    }

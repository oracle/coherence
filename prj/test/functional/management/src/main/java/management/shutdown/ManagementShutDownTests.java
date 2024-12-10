/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management.shutdown;

import com.tangosol.net.DefaultCacheServer;
import org.junit.BeforeClass;

/**
 * Tests the shutdown endpoints provided via Management over REST and verifies that
 * the respective services are being restarted.
 *
 * @author gh 2022.12.13
 */
public class ManagementShutDownTests
        extends BaseManagementShutdownTests
    {
    @BeforeClass
    public static void _startup()
        {
        startTestCluster(DefaultCacheServer.class, CLUSTER_NAME);
        }
    }

/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.tangosol.net.DefaultCacheServer;
import org.junit.BeforeClass;

/**
 * Run the ManagementInfoResource tests using read-only management.
 *
 * @author tam 2022.09.19
 */
public class ReadOnlyManagementInfoResourceTests
        extends BaseManagementInfoResourceTests
    {
    @BeforeClass
    public static void _startup()
        {
        setReadOnly(true);
        startTestCluster(DefaultCacheServer.class, CLUSTER_NAME);
        }
    }

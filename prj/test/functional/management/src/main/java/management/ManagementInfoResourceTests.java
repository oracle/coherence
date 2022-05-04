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
 * MBeanResourceTest tests the ManagementInfoResource.
 *
 * <p>
 * In general, if we only want to assert that an attribute value is set
 * (not the default -1), but not what the value is, then use asserts
 * similar to the following:
 *
 * assertThat(((Number) mapResponse.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));
 * assertThat(Long.parseLong(mapResponse.get("requestTotalCount").toString()), greaterThanOrEqualTo(0L));
 *
 * @author hr 2016.07.21
 * @author sr 2017.08.24
 */
public class ManagementInfoResourceTests
        extends BaseManagementInfoResourceTests
    {
    @BeforeClass
    public static void _startup()
        {
        startTestCluster(DefaultCacheServer.class, CLUSTER_NAME);
        }
    }

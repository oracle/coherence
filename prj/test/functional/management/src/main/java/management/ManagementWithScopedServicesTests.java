/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.tangosol.internal.net.management.HttpHelper;
import com.tangosol.net.DefaultCacheServer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Test Management over REST when service names are scoped.
 */
public class ManagementWithScopedServicesTests
        extends BaseManagementInfoResourceTests
    {
    @BeforeClass
    public static void _startup()
        {
        startTestCluster(DefaultCacheServer.class, CLUSTER_NAME, SystemProperty.of("test.scope.name", SCOPE));
        }

    @Test
    @Override
    public void testServiceStartAndStop()
        {
        // skipped as this is actually broken for scoped services
        // due to a bug in the Service MBean start() method.
        }

    @Test
    public void testServiceMemberStartAndStop()
        {
        // skipped as this is actually broken for scoped services
        // due to a bug in the Service MBean start() method.
        }

    // ----- helper methods -------------------------------------------------

    @Override
    protected String getScopedServiceName(String sName)
        {
        // don't scope the management proxy
        if (HttpHelper.getServiceName().equals(sName))
            {
            return sName;
            }
        return SCOPE + ":" + sName;
        }

    @Override
    protected String getQuotedScopedServiceName(String sName)
        {
        // don't scope the management proxy
        if (HttpHelper.getServiceName().equals(sName))
            {
            return sName;
            }
        return '"' + getScopedServiceName(sName) + '"';
        }


    // ----- data members ---------------------------------------------------

    public static final String SCOPE = "Test Scope Name";
    }

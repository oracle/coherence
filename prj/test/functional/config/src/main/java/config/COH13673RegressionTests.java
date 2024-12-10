/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Service;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * COH-13673 Regression Test - ensure that setting thread-count-max to 0 does not result in a validation error.
 * Proxy, Integration and Partitioned Service Dependencies default thread-count-min to 1 to indicate autosizing.
 * User setting thread-count-max to 0 overrides this default and sets thread-count-min to legal value of 0.
 *
 * @author jf 2015.07.28
 */
public class COH13673RegressionTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public COH13673RegressionTests()
        {
        super(FILE_CFG_CACHE);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }

    @Test
    public void loadDistributedServiceThreadCountMaxZeroTest()
        {
        ConfigurableCacheFactory ccf = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE, null);

        Service svc = ccf.ensureService("working-distributed-cache-service");
        assertNotNull(svc);
        }


    @Test
    public void loadProxyServiceThreadCountMaxZeroTest()
        {
        ConfigurableCacheFactory ccf = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE, null);

        Service svc = ccf.ensureService("MyProxyService");
        assertNotNull(svc);
        }

    @Test
    public void loadInvocationServiceThreadCountMaxZeroTest()
        {
        ConfigurableCacheFactory ccf = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE, null);

        Service svc = ccf.ensureService("my-invocation-service");
        assertNotNull(svc);
        }


    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static final String FILE_CFG_CACHE = "COH13673-cache-config.xml";
    }

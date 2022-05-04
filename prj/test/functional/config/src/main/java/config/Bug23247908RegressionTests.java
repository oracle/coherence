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

import static org.junit.Assert.assertNotNull;

/**
 * Regression tests for Bug 23247908 - Missing Injectable page-duration in PagedExternalScheme.
 *
 * @author tam  2016.05.09
 */
public class Bug23247908RegressionTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Bug23247908RegressionTests()
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

    // ----- test methods ---------------------------------------------------

    @Test
    public void regressionTest()
        {
        ConfigurableCacheFactory ccf = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE, null);

        // if we throw exception then this will fail the test and this will indicate
        // a regression for Bug 23247908
        Service svc = ccf.ensureService("test-scheme-service");
        assertNotNull(svc);
        }

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static final String FILE_CFG_CACHE = "Bug23247908-config.xml";
    }

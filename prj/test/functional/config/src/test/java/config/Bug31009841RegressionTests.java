/*
 * Copyright (c) 2000, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package config;

import com.tangosol.config.ConfigurationException;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import common.AbstractFunctionalTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression tests for Bug 23247908 .
 *
 * @author jf  2020.03.09
 */
public class Bug31009841RegressionTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Bug31009841RegressionTests()
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
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void regressionTest()
        {
        ConfigurableCacheFactory ccf = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE, null);

        DefaultCacheServer server = new DefaultCacheServer(ccf);
        try
            {
            server.startServices();
            fail("must throw ConfigurationException for missing scheme");
            }
        catch (ConfigurationException e)
            {
            assertTrue(e.getMessage().contains("Scheme definition missing for scheme test-scheme"));
            }
        }

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static final String FILE_CFG_CACHE = "Bug31009841-config.xml";
    }

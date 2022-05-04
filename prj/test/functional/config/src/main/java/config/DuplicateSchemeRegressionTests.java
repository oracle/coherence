/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.util.WrapperException;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression tests for Enh 33033443 : improve exception logging for duplicate caching scheme defined in cache config
 *
 * @author jf  2021.06.22
 */
public class DuplicateSchemeRegressionTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public DuplicateSchemeRegressionTests()
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

    @Test(expected = IllegalArgumentException.class)
    public void regressionTestFor33033443()
            throws Throwable
        {
        try
            {
            ConfigurableCacheFactory ccf = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(FILE_CFG_CACHE, null);

            fail("should have thrown IllegalArgumentException duplicate scheme definition");
            }
        catch (WrapperException e)
            {
            assertTrue(e.getCause() instanceof IllegalArgumentException);

            String sMessage = e.getCause().getMessage();
            System.out.println(sMessage);

            assertTrue(sMessage, sMessage.contains("duplicate"));
            assertTrue(sMessage, sMessage.contains("my-dist-scheme"));

            throw e.getCause();
            }
        }

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static final String FILE_CFG_CACHE = "duplicate-scheme-cache-config.xml";
    }

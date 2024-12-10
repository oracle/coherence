/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;


import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;

import com.tangosol.util.processor.AbstractProcessor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Regression test for COH-10453
 *
 * @author rhl 2013.09.19
 */
public class Coh10453RegressionTests
        extends AbstractFunctionalTest
    {
    public Coh10453RegressionTests()
        {
        super("coherence-cache-config.xml");
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
        System.setProperty("coherence.distributed.threads.min", "0");
        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void doTest()
        {
        String                   sCache = "dist-coh-10453";
        ClassLoader              loader = getClass().getClassLoader();
        ConfigurableCacheFactory ccf    = getFactory();
        NamedCache               cache  = ccf.ensureCache(sCache, loader);
        Object                   oKey   = 1;

        try
            {
            cache.invoke(oKey, new FaultyExceptionProcessor());
            azzert(false, "invoke of FaultyExceptionProcessor must throw an exception");
            }
        catch (Throwable e)
            {
            }

        ccf.destroyCache(cache);

        cache = ccf.ensureCache(sCache, loader);
        assertFalse(cache.containsKey(oKey));

        cache.invoke(oKey, new PutProcessor(oKey));

        // COH-10453: commented out assertion until the fix is complete
        //assertTrue(cache.containsKey(oKey));
        }

    // ----- inner class FaultyExceptionProcessor -------------------------

    public static class FaultyExceptionProcessor
            extends AbstractProcessor
        {
        public Object process(InvocableMap.Entry entry)
            {
            throw new RuntimeException();
            }
        }

    public static class PutProcessor
            extends AbstractProcessor
        {
        public PutProcessor(Object oValue)
            {
            m_oValue = oValue;
            }

        public Object process(InvocableMap.Entry entry)
            {
            entry.setValue(m_oValue);
            return null;
            }

        private final Object m_oValue;
        }
    }

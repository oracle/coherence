/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.processor.AbstractProcessor;

import static org.junit.Assert.assertTrue;

public class BinaryEntryExpiryTest
        extends AbstractFunctionalTest
    {
    /**
     * Default constructor.
     */
    public BinaryEntryExpiryTest()
        {
        super(FILE_CFG_CACHE);
        }

    @After
    public void cleanup()
        {
        stopAllApplications();
        CacheFactory.shutdown();
        }

    // ----- test listeners -------------------------------------------------

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
    public void testBinaryEntryExpiry()
        {
        NamedCache cache = getNamedCache("dist-test");
        AbstractProcessor processor = new TestProcessor();

        long lExpiry = 3456000000l;
        cache.put(1, 1, lExpiry);
        cache.put(2, 2, lExpiry);

        //warm up
        cache.invoke(1, processor);
        cache.invoke(2, processor);

        Object result = cache.invoke(1, processor);
        assertTrue((lExpiry - (long) result) < 1000l);

        lExpiry = 60000l;
        cache.put(1, 1, lExpiry);
        result = cache.invoke(1, processor);
        assertTrue((lExpiry - (long) result) < 500l);

        lExpiry = 432000000l;
        cache.put(1, 1, lExpiry);
        result = cache.invoke(1, processor);
        assertTrue((lExpiry - (long) result) < 1000l);
        }


    static class TestProcessor extends AbstractProcessor
        {
        @Override
        public Object process(InvocableMap.Entry entry)
            {
            long lExpiry = ((BinaryEntry) entry).getExpiry();
            return lExpiry;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_CFG_CACHE = "server-cache-config.xml";
    }

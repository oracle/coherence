/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package events;


import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.NamedCache;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import common.AbstractFunctionalTest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.core.Is.is;

/**
 * Tests for {@link CacheLifecycleEvent}.
 *
 * @author bbc 2015-09-15
 */
public class CacheLifecycleEventTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Default Constructor.
    */
    public CacheLifecycleEventTests()
        {
        super(CFG_FILE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");
        System.setProperty("tangosol.coherence.management", "all");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Test that we can modify an insert event.
     */
    @Test
    public void testEvents()
            throws InterruptedException
        {
        NamedCache result = getNamedCache("result");
        NamedCache cache  = getNamedCache("cacheLife");

        Eventually.assertThat(invoking(result).get("created"), is("cacheLife"));

        cache.put("k1", "v1");
        cache.put("k2", "v2");

        cache.truncate();
        Eventually.assertThat(invoking(result).get("truncated"), is("cacheLife"));
        result.remove("truncated");

        cache.put("k1", "v1");
        cache.put("k2", "v2");

        // test that uem event is raised for second time cache truncate
        cache.truncate();
        Eventually.assertThat(invoking(result).get("truncated"), is("cacheLife"));

        cache.put("k1", "v1");
        cache.put("k2", "v2");

        cache.destroy();
        Eventually.assertThat(invoking(result).get("destroyed"), is("cacheLife"));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Cache config file to use for these tests.
     */
    public static String CFG_FILE = "server-cache-config.xml";
    }
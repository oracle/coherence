/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.NamedCache;
import com.tangosol.net.InvocationService;
import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.Filter;
import com.tangosol.util.filter.InFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.ExampleAddress;
import com.oracle.coherence.testing.FilterFactory;
import com.oracle.coherence.testing.TestContact;

import java.util.Collection;
import java.util.HashSet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;


/**
 * Coherence test for the ContinuousQueryCache query
 * with constructed filters
 *
 * @author par  2013.7.22
 * 
 * @since Coherence 3.7.1.10
 */
public class CQCTests
        extends AbstractFunctionalTest
    {

    //----- constructors -----------------------------------------------------

    public CQCTests() 
        {
        super(FILE_CLIENT_CFG_CACHE);
        }

    //----- test lifecycle ---------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("CQCTestServer", "extend", FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("CQCTestServer");
        }

    /**
     * Test the serialization/deserialization of InFilter,
     * when used in a query.
     */
    @Test
    public void testCoh10013()
        {
        // put data items into inner cache to generate events
        NamedCache testCache = getNamedCache("dist-extend-direct");
        testCache.clear();

        TestContact t1 = new TestContact("John", "Loehe", 
                new ExampleAddress("675 Beacon St.", "", "Dthaba", "SC", "91666", "USA"));
        testCache.put("1", t1);

        TestContact t2 = new TestContact("John", "Sydqtiinz", 
                new ExampleAddress("336 Beacon St.", "", "Wltowuixs", "MA", "00595", "USA"));
        testCache.put("2", t2);

        InvocationService service = (InvocationService)
                getFactory().ensureService("ExtendTcpInvocationService");

        // Find all contacts who live in Massachusetts - direct cache access
        Collection results = testCache.keySet
                (FilterFactory.createFilter("homeAddress.state = 'MA'", service));

        // Assert we got one result
        assertEquals(1, results.size());
        TestContact obj = (TestContact) testCache.get(results.iterator().next());
        assertEquals(0, obj.compareTo(t2));


        // Query with an InFilter created on the client
        HashSet values = new HashSet();
        values.add("Loehe");
        Filter inFil = new InFilter(new ReflectionExtractor("getLastName"), values);
        ContinuousQueryCache cqc1 = new ContinuousQueryCache(testCache, inFil, true);
        results = cqc1.keySet();

        // Assert we got one result and it's the one we expect
        assertEquals(1, results.size());
        obj = (TestContact) cqc1.get(results.iterator().next());
        assertEquals(0, obj.compareTo(t1));

        // Query with an InFilter created on the cache
        Filter filter = FilterFactory.createFilter("lastName in ('Loehe')", service);
        ContinuousQueryCache cqc2 = new ContinuousQueryCache(testCache, filter, false);
        results = cqc2.keySet();
 
        // Assert we got one result and it's the one we expect
        assertEquals(1, results.size());
        obj = (TestContact) cqc2.get(results.iterator().next());
        assertEquals(0, obj.compareTo(t1));
        }

    //----- constants --------------------------------------------------------

    /**
     * The file name of the default client cache configuration file used by 
     * this test.
     */
    public static String FILE_CLIENT_CFG_CACHE = "client-cache-config-coh10013.xml";

    /**
     * The file name of the cache configuration file used by cache
     * servers launched by this test; no proxy configured.
     */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config-coh10013.xml";

    }

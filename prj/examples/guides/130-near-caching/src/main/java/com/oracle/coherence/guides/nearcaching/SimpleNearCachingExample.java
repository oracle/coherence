/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.nearcaching;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.util.Base;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import java.util.Set;


/**
 * Near caching example using a size limited and optionally expiring front cache.
 *
 * @author Tim Middleton  2021.04.12
 */
public class SimpleNearCachingExample {

    /**
     * Coherence MBean server.
     */
    private MBeanServer mbs;

    /**
     * Cache name to use for the example.
     */
    private final String cacheName;

    /**
     * Attributes to get for front Cache MBean.
     */
    private final String[] ATTRS_FRONT = new String[] {
            "TotalGets", "TotalPuts", "CacheHits", "Size",
            "HitProbability", "AverageMissMillis", "CachePrunes"};

    /**
     * Attributes for get for StorageManager MBean.
     */
    private final String[] ATTRS_STORAGE = new String[] {
            "ListenerRegistrations", "InsertCount"};

    // tag::construct[]
    /**
     * Construct the example.
     *
     * @param cacheName cache name 
     * @param invalidationStrategy invalidation strategy to use
     */
    public SimpleNearCachingExample(String cacheName, String invalidationStrategy) {
        this.cacheName = cacheName;
        if (invalidationStrategy != null) {
            System.setProperty("test.invalidation.strategy", invalidationStrategy);
        }
        System.setProperty("coherence.management.refresh.expiry", "1s");
        System.setProperty("coherence.management", "all");
    }
    // end::construct[]

    /**
     * Entry point to run example from IDE.
     *
     * @param args arguments
     */
    public static void main(String[] args) throws Exception {
        SimpleNearCachingExample example = new SimpleNearCachingExample("size-cache-1", "all");
        example.runExample();
    }

    // tag::runExample[]
    /**
     * Run the example.
     */
    public void runExample() throws Exception {
        final int MAX = 100;

        // Create the Coherence instance from the configuration
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                           .withSession(SessionConfiguration.create("near-cache-config.xml"))
                           .build();
        Coherence coherence = Coherence.clusterMember(cfg);
        coherence.start().join();

        // retrieve a session
        Session session = coherence.getSession();
        
        NamedMap<Integer, String> map = session.getMap(cacheName);
        map.clear();
        
        Logger.info("Running test with cache " + cacheName);

        // sleep so we don't get distribution messages intertwined with test output
        Base.sleep(5000L);

        // fill the map with MAX values <1>
        putValues(map, 0, MAX);

        // execute two times to see the difference in access times and MBeans once the
        // near cache is populated on the first iteration
        for (int j = 1; j <= 2; j++) {

            // issue MAX get operations and get the total time taken
            long start = System.nanoTime();
            getValues(map, 0, MAX);  // <2>

            long duration = (System.nanoTime() - start);
            Logger.info("Iteration #" + j + " Total time for gets "
                        + String.format("%.3f", duration / 1_000_000f) + "ms");

            // Wait for some time for the JMX stats to catch up
            Base.sleep(3000L); // <3>

            logJMXNearCacheStats(); // <4>
        }

        // issue 10 more puts
        putValues(map, MAX, 10); // <5>

        // issue 10 more gets and the high-units will be hit and cache pruning will happen when using size cache
        getValues(map, MAX, 10);
        Logger.info("After extra 10 values put and get");

        logJMXNearCacheStats(); // <6>
        logJMXStorageStats();
    }
    // end::runExample[]

    /**
     * Put values into the {@link NamedMap}.
     *
     * @param map   {@link NamedMap} to add data to
     * @param start start key
     * @param count number of entries to add
     */
    private void putValues(NamedMap<Integer, String> map, int start, int count) {
        for (int i = start; i < start + count; i++) {
            map.put(i, "value-" + i);
        }
    }

    /**
     * Issue a get for entries in a {@link NamedMap}
     *
     * @param map   {@link NamedMap} to issue get
     * @param start start key
     * @param count number of entries to get
     */
    private void getValues(NamedMap<Integer, String> map, int start, int count) {
        for (int i = start; i < start + count; i++) {
            String value = map.get(i);
        }
    }

    /**
     * Returns the {@link MBeanServer}.
     *
     * @return the {@link MBeanServer}
     */
    protected MBeanServer getMBeanServer() {
        if (mbs == null) {
            mbs = MBeanHelper.findMBeanServer();
        }
        return mbs;
    }

    /**
     * Log attributes from the front Cache MBean.
     *
     * @throws Exception if any JMX errors
     */
    protected void logJMXNearCacheStats() throws Exception {
        logJMXStats("Coherence:type=Cache,tier=front,name=" + cacheName + ",*", ATTRS_FRONT);
    }

    /**
     * Log attributes from the StorageManager MBean.
     *
     * @throws Exception if any JMX errors
     */
    protected void logJMXStorageStats() throws Exception {
        logJMXStats("Coherence:type=StorageManager,cache=" + cacheName + ",*", ATTRS_STORAGE);
    }

    /**
     * Generic method to support retrieving JMX attributes.
     *
     * @param query the query to run
     * @param attrs the attributes to extract
     *
     * @throws Exception if any JMX errors
     */
    private void logJMXStats(String query, String[] attrs) throws Exception {
        MBeanServer     mbs         = getMBeanServer();
        Set<ObjectName> objectNames = mbs.queryNames(new ObjectName(query), null);
        for (ObjectName on : objectNames) {
            Logger.info(on.toString());
            mbs.getAttributes(on, attrs)
               .forEach(a -> Logger.info("Name: " + ((Attribute) a).getName()
                                       + ", value=" + ((Attribute) a).getValue()));
        }
    }
}

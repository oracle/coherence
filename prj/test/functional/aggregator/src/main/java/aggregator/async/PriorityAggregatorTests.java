/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package aggregator.async;


import aggregator.AbstractEntryAggregatorTests;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.PriorityAggregator;

import data.Trade;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * A collection of functional tests for the various async
 * {@link PriorityAggregator} implementations that use the
 * "Priority-test" cache.
 * @author bb  2015.04.06
 *
 * @see InvocableMap
 */
public class PriorityAggregatorTests
        extends AbstractAsyncEntryAggregatorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public PriorityAggregatorTests()
        {
        super("PriorityAggregator-test");
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

        AbstractEntryAggregatorTests._startup();
        }

    // ----- AbstractEntryAggregatorTests methods ---------------------------


    /**
    * Run the test timeout test.
    */
    @Test
    public void testRequestTimeout()
        {
        doTestRequestTimeout(CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("request-timeout-config.xml", null));
        }

    /**
     * We are testing PriorityAggregator.setRequestTimeout. The cache is
     * configurated with a request-timeout set to 250 millis seconds and
     * programatically we set the requestTimeout to 500 milliseconds and
     * expecting the test aggregator successfully when sleeping for 300
     * milliseconds.
     *
     * @param factory  the cache factory
     */
     protected void doTestRequestTimeout(ConfigurableCacheFactory factory)
         {
         setFactory(factory);

         NamedCache cache  = getNamedCache();
         Trade.fillRandom(cache, 100);

         PriorityAggregator priorityAggregator = new PriorityAggregator(new SlowAggregator("getPrice"));
         priorityAggregator.setRequestTimeoutMillis(500L);
         try
             {
             cache.aggregate((Filter) null, priorityAggregator);
             }
         catch(com.tangosol.net.RequestTimeoutException exception)
             {
             fail("Test failed with RequestTimeoutException");
             }
         }
    /**
    * Helper aggregator to test the request-timeout.
    */
    public static class SlowAggregator extends DoubleAverage
        {
        public SlowAggregator()
            {
            super();
            }

        public SlowAggregator(String sMethod)
            {
            super(sMethod);
            }

        @Override
        protected Object finalizeResult(boolean fFinal)
            {
            if (!fFinal)
                {
                Base.sleep(50L);
                }
            return super.finalizeResult(fFinal);
            }
        }
    }

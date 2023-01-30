/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.Threads;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.AbstractProcessor;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestNCDListener;

import data.Person;

import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Coherence test for the ContinuousQueryCache receiving events
 * after restarting the distributed service.
 *
 * @author par  2013.2.28
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
        for (int i = 0; i < CLUSTER_SIZE; i++)
            {
            CoherenceClusterMember member = startCacheServer("CQCTestServer-" + i, "cache", FILE_SERVER_CFG_CACHE);
            members.add(member);
            }

        for (int i = 0; i < CLUSTER_SIZE; i++)
            {
            CoherenceClusterMember member = members.get(i);
            Eventually.assertThat(invoking(member).isServiceRunning("Cluster"), is(true), within(2, TimeUnit.MINUTES));
            Eventually.assertThat(invoking(member).isOperational(), is(true), within(2, TimeUnit.MINUTES));
            }

        ensureRunningService("CQCTestServer-0", "CQCTestService");
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        for (int i = 0; i < CLUSTER_SIZE; i++)
            {
            stopCacheServer("CQCTestServer-" + i);
            }
        }

    @After
    public void reset()
        {
        if (m_queryCache != null)
            {
            m_queryCache.release();
            }
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test the behavior of proxy returning events.
     * COH-8145 reports the CQC doesn't receive events after
     * the proxy is restarted.
     * COH-8470 reports that the CQC resynchronizes multiple
     * times, giving double or triple events.
     *
     * Put a known number of data items into the inner cache,
     * then count the number of events the listener receives
     * after restarting the proxy.
     */
    @Test
    public void testEvents()
        {
        // put data items into inner cache to generate events
        NamedCache<String, Integer>                    testCache = getAndPopulateNamedCache("dist-test");
        TestCQCListener                                listener  = new TestCQCListener(SOME_DATA);
        ContinuousQueryCache<String, Integer, Integer> theCQC    = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE,
                listener));

        // check that the listener received the correct number of events
        // and the CQC has set its state to STATE_SYNCHRONIZED
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(SOME_DATA));

        // suspend service
        getFactory().ensureService("CQCTestService").shutdown();

        // verify CQC has received disconnect event before restarting service
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));

        // reset the listener
        listener.resetActualTotal();

        // restart the service
        getFactory().ensureService("CQCTestService").start();

        // ping the CQC for non-existing key to make it realize the cache needs restart
        theCQC.get("junkKey");

        // check that the listener received the correct number of events after restart
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(SOME_DATA));
        }

    /**
     * Test the case where an Invoke inserts an entry into the cache
     * to ensure that the event contains the value being inserted.
     */
    @Test
    public void testInvokeEvents()
        {
        // put data items into inner cache to generate events
        NamedCache<String, String>                   testCache   = getNamedCache("dist-test", String.class, String.class);
        GetValueListener                             listener    = new GetValueListener();
        GetValueListener                             CQCListener = new GetValueListener();
        ContinuousQueryCache<String, String, String> theCQC      = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE,
                CQCListener));

        String sValue = "myValue";

        testCache.addMapListener(listener);
        //noinspection unchecked
        testCache.invoke("myKey", new InsertProcessor(sValue));

        // check that the listener received the correct value
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getValue(), is(sValue));
        Eventually.assertThat(invoking(CQCListener).getValue(), is(sValue));

        // suspend service
        getFactory().ensureService("CQCTestService").shutdown();

        // verify CQC has received disconnect event before restarting service
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));
        }

    /**
     * Test to validate truncate.
     */
    @Test
    public void testTruncate()
        {
        NamedCache<String, Integer>                    testCache = getAndPopulateNamedCache("dist-test");
        TestCQCListener                                listener  = new TestCQCListener(SOME_DATA);
        ContinuousQueryCache<String, Integer, Integer> theCQC    = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE,
                listener));

        // check that the listener received the correct number of events
        // and the CQC has set its state to STATE_SYNCHRONIZED
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(SOME_DATA));

        listener.resetActualTotal();

        theCQC.truncate();
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(0));
        Eventually.assertThat(invoking(theCQC).isEmpty(), is(true));
        assertThat(theCQC.isActive(), is(true));
        }

    /**
     * Ensure truncate is a no-op on the {@link ContinuousQueryCache}-side if it is {@code read-only}.
     */
    @Test(expected = IllegalStateException.class)
    public void testTruncateReadOnly()
        {
        NamedCache<String, Integer>                    testCache = getAndPopulateNamedCache("dist-test");
        TestCQCListener                                listener  = new TestCQCListener(SOME_DATA);
        ContinuousQueryCache<String, Integer, Integer> theCQC    = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE,
                listener));

        // check that the listener received the correct number of events
        // and the CQC has set its state to STATE_SYNCHRONIZED
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(SOME_DATA));

        listener.resetActualTotal();

        theCQC.setReadOnly(true);
        theCQC.truncate();
        }

    /**
     * Ensure truncate is properly honored if invoked against the back cache, even if the {@link ContinuousQueryCache}
     * is marked {@code read-only}.
     */
    @Test
    public void testTruncateReadOnlyBackCacheTruncation()
        {
        NamedCache<String, Integer>                    testCache = getAndPopulateNamedCache("dist-test");
        TestCQCListener                                listener  = new TestCQCListener(SOME_DATA);
        ContinuousQueryCache<String, Integer, Integer> theCQC    = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE,
                listener));

        // check that the listener received the correct number of events
        // and the CQC has set its state to STATE_SYNCHRONIZED
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(SOME_DATA));

        listener.resetActualTotal();

        theCQC.setReadOnly(true);
        theCQC.getCache().truncate();
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(0));
        Eventually.assertThat(invoking(theCQC).isEmpty(), is(true));
        assertThat(theCQC.isActive(), is(true));
        }

    /**
     * Ensure an {@link UnsupportedOperationException} is thrown if the backing cache doesn't support truncation
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testTruncateNotSupported()
        {
        NamedCache<String, Integer>                    testCache = getAndPopulateNamedCache("repl-test");
        TestCQCListener                                listener  = new TestCQCListener(SOME_DATA);
        ContinuousQueryCache<String, Integer, Integer> theCQC    = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE,
                listener));

        // check that the listener received the correct number of events
        // and the CQC has set its state to STATE_SYNCHRONIZED
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(SOME_DATA));

        theCQC.truncate();
        }

    /**
     * Ensure {@link NamedCacheDeactivationListener} are properly unregistered when requested.
     */
    @Test
    public void testDeactivationListenerDeregistration()
        {
        NamedCache<String, Integer>                    testCache            = getAndPopulateNamedCache("dist-test");
        TestNCDListener                                deactivationListener = new TestNCDListener();
        TestCQCListener                                listener             = new TestCQCListener(SOME_DATA);
        ContinuousQueryCache<String, Integer, Integer> theCQC               = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE,
                listener));

        //noinspection unchecked
        theCQC.addMapListener(deactivationListener);

        // check that the listener received the correct number of events
        // and the CQC has set its state to STATE_SYNCHRONIZED
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(SOME_DATA));

        //noinspection unchecked
        theCQC.removeMapListener(deactivationListener);

        theCQC.truncate();
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(theCQC).isEmpty(), is(true));
        assertThat(theCQC.isActive(), is(true));

        MapEvent evt = deactivationListener.waitForEvent();
        assertThat("Deactivation listener deregistration failed", evt, is(nullValue()));
        }

    /**
     * Validate {@code release} puts the {@link ContinuousQueryCache} into a disconnected state.
     * Ensure the {@link ContinuousQueryCache} can properly resynchronize after a local release.
     */
    @Test
    public void testRelease()
        {
        NamedCache<String, Integer>                    testCache = getAndPopulateNamedCache("dist-test");
        ContinuousQueryCache<String, Integer, Integer> theCQC    = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE));

        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));

        assertThat(theCQC.isActive(), is(true));
        theCQC.release();
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));
        assertThat(theCQC.isActive(), is(false));

        // trigger CQC to re-synchronize
        theCQC.get("someKey");
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        assertThat(theCQC.isActive(), is(true));
        }

    /**
     * Validate {@code destroy} puts the {@link ContinuousQueryCache} into a disconnected state.
     * Ensure the {@link ContinuousQueryCache} can properly resynchronize after a local release.
     */
    @Test
    public void testDestroy()
        {
        NamedCache<String, Integer>                    testCache = getAndPopulateNamedCache("dist-test");
        ContinuousQueryCache<String, Integer, Integer> theCQC    = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE));

        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));

        assertThat(theCQC.isActive(), is(true));
        theCQC.destroy();
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));
        assertThat(theCQC.isActive(), is(false));

        // trigger CQC to re-synchronize
        theCQC.get("someKey");
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        assertThat(theCQC.isActive(), is(true));
        }

    /**
     * Validate the name of the {@link ContinuousQueryCache} with and without a {@link java.util.function.Supplier}
     * for the cache name.
     */
    @Test
    public void testCacheNameSupplier()
        {
        NamedCache<String, Integer>                    testCache = getAndPopulateNamedCache("dist-test");
        ContinuousQueryCache<String, Integer, Integer> theCQC    = setCQC(new ContinuousQueryCache<>(
                testCache,
                AlwaysFilter.INSTANCE));

        assertThat(theCQC.getCacheName(), is("ContinuousQueryCache{Cache=dist-test, Filter=AlwaysFilter, Transformer=null}"));

        theCQC.setCacheName("dist-test");

        assertThat(theCQC.getCacheName(), is("dist-test"));

        theCQC.setCacheName(null);

        assertThat(theCQC.getCacheName(), is("ContinuousQueryCache{Cache=dist-test, Filter=AlwaysFilter, Transformer=null}"));
        }

    @Test
    public void testCQCWithSupplierWithCluster()
        {
        final String cacheName = "dist-test";

        // put data items into inner cache to generate events
        NamedCache<String, Integer> cqcTestCache = getNamedCache(cacheName, String.class, Integer.class);
        cqcTestCache.clear();

        TestCQCListener      listener = new TestCQCListener(SOME_DATA);
        ContinuousQueryCache theCQC   = setCQC(new ContinuousQueryCache<>(
                () -> getNamedCache(cacheName),
                AlwaysFilter.INSTANCE,
                true,
                listener,
                new IdentityExtractor<>(),
                null));

        CoherenceClusterMember member0 = members.get(0);
        for (int i = 0; i < SOME_DATA; i++)
            {
            //noinspection unchecked
            member0.getCache(cacheName).put("TestKey" + i, i);
            }

        // check that the listener received the correct number of events
        // and the CQC has set its state to STATE_SYNCHRONIZED
        Eventually.assertThat(invoking(theCQC).getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        Eventually.assertThat(invoking(listener).getActualTotal(), is(SOME_DATA));

        assertEquals(5, (int) theCQC.get("TestKey" + 5));

        // reset the listener
        listener.resetActualTotal();

        // destroy the underlying cache.
        CoherenceClusterMember member1 = members.get(1);

        member1.getCache(cacheName).destroy();
        Eventually.assertThat(invoking(theCQC).isActive(), is(false), within(1, TimeUnit.MINUTES));


        // Now update a set of keys that existed in the cache before it was destroyed.
        // Also, add a few new keys.
        for (int i = SOME_DATA / 2; i < SOME_DATA + (SOME_DATA / 2); i++)
            {
            //noinspection unchecked
            member1.getCache(cacheName).put("TestKey" + i, 2 * i);
            }

        Object obj = theCQC.get("TestKey" + 0);
        assertNull("Was expecting the cache to be reset.", obj);

        // 0 to SOME_DATA / 2 must not exist.
        for (int i = 0; i < SOME_DATA / 2; i++)
            {
            obj = theCQC.get("TestKey" + i);
            assertNull("Was expecting " + obj + "to be null.", obj);
            }

        // SOME_DATA / 2 to SOME_DATA + (SOME_DATA / 2) must be 2 * i.
        for (int i = SOME_DATA / 2; i < SOME_DATA + (SOME_DATA / 2); i++)
            {
            obj = theCQC.get("TestKey" + i);
            assertEquals(obj, 2 * i);
            }
        }

    /**
     * Unit test to validate CQC reconnect interval state transitions.
     */
    @Test
    public void testReconnectInterval()
        {
        Supplier<NamedCache<Integer, Person>> supplier = () ->
            {
            NamedCache<Integer, Person> cacheBase = getNamedCache("dist-test", Integer.class, Person.class);
            cacheBase.put(1, new Person("111-11-1111", "Homer", "Simpson", 1945, null, new String[0]));
            cacheBase.put(2, new Person("222-22-2222", "Marge", "Simpson", 1950, null, new String[0]));
            cacheBase.put(3, new Person("333-33-3333", "Bart",  "Simpson", 1985, null, new String[0]));
            cacheBase.put(4, new Person("444-44-4444", "Lisa",  "Simpson", 1987, null, new String[0]));

            return cacheBase;
            };

        Person fifthWheel = new Person("555-55-5555", "Sideshow", "Bob", 1969, null, new String[0]);

        ContinuousQueryCache<Integer, Person, Person> cacheCQC =
                setCQC(new ContinuousQueryCache<>(supplier, AlwaysFilter.INSTANCE));

        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));

        cacheCQC.setReconnectInterval(3000);
        cacheCQC.getCache().destroy();

        Eventually.assertThat(invoking(cacheCQC).getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));

        assertThat(cacheCQC.get(1), is(new Person("111-11-1111", "Homer", "Simpson", 1945, null, new String[0])));
        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));

        cacheCQC.put(5, fifthWheel); // remote operation

        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));
        assertThat(cacheCQC.get(5), is(nullValue())); // CQC is disconnected; won't see the new value
        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));

        sleep(4000);

        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));
        assertThat(cacheCQC.get(5), is(fifthWheel));
        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        }

    /**
     * Unit test to validate CQC reconnect interval state transitions when the reconnect interval is zero.
     */
    @Test
    public void testReconnectIntervalSetAtZero()
        {
        NamedCache<Integer, Person> cacheBase = getNamedCache("dist-test", Integer.class, Person.class);
        cacheBase.put(1, new Person("111-11-1111", "Homer", "Simpson", 1945, null, new String[0]));
        cacheBase.put(2, new Person("222-22-2222", "Marge", "Simpson", 1950, null, new String[0]));
        cacheBase.put(3, new Person("333-33-3333", "Bart",  "Simpson", 1985, null, new String[0]));
        cacheBase.put(4, new Person("444-44-4444", "Lisa",  "Simpson", 1987, null, new String[0]));

        TestContinuousQueryCache<Integer, Person, Person> cacheCQC =
                new TestContinuousQueryCache<>(cacheBase, AlwaysFilter.INSTANCE, true);
        setCQC(cacheCQC);

        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));

        cacheCQC.changeState(ContinuousQueryCache.STATE_DISCONNECTED);
        cacheCQC.lockState();

        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));

        try
            {
            cacheCQC.get(1);
            fail("Expected an exception to be thrown");
            }
        catch (Exception expected)
            {
            Logger.info("This error is expected: " + expected.toString());
            }

        cacheCQC.unlockState();

        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_DISCONNECTED));
        assertThat(cacheCQC.get(1), is(new Person("111-11-1111", "Homer", "Simpson", 1945, null, new String[0])));
        assertThat(cacheCQC.getState(), is(ContinuousQueryCache.STATE_SYNCHRONIZED));
        }

    @Test
    public void testViewServiceRestart()
        {
        // Note: service is not set to autostart thus this is a single server test

        ContinuousQueryCache<Integer, String, String> cacheView = (ContinuousQueryCache) getNamedCache("view-foo");
        try
            {
            for (int i = 0; i < 100; ++i)
                {
                cacheView.put(i, "value " + i);
                }

            causeServiceDisruption(cacheView.getCache());

            Eventually.assertDeferred(cacheView::getState, is(ContinuousQueryCache.STATE_DISCONNECTED));

            cacheView.size(); // cause the service restart

            String sReturn = cacheView.put(0, "value new");
            assertEquals("value 0", sReturn);
            }
        finally
            {
            cacheView.destroy();
            }
        }

    /**
     * Test case for https://jira.oraclecorp.com/jira/browse/COH-27085.
     * CQC clients deadlock between ObservableHashMap and
     * ContinuousQueryCache.ensureSynchronized
     */
    @Test
    public void testCoh27085() throws Exception
        {
        int                     nAttempts = 1000;
        NamedCache              raw       = getNamedCache("dist-test-deadlock");
        ExecutorService         service   = Executors.newFixedThreadPool(2);
        AtomicInteger           t1Counter = new AtomicInteger();
        AtomicInteger           t2Counter = new AtomicInteger();
        AtomicReference<String> t1Name    = new AtomicReference<>();
        AtomicReference<String> t2Name    = new AtomicReference<>();

        Thread.sleep(3000);

        service.submit(() ->
                       {
                       String sName = Thread.currentThread().getName();
                       t1Name.set(sName);
                       Logger.info(String.format("Starting thread [%s] which will create CQC instances", sName));
                       while (t1Counter.incrementAndGet() < nAttempts)
                           {
                           try
                               {
                               new ContinuousQueryCache(raw);
                               }
                           catch (IllegalStateException ignored)
                               {
                               continue;
                               }
                           catch (Throwable t)
                               {
                               Logger.err(t);
                               }
                           Thread.yield();
                           }
                       Logger.info(String.format("Thread [%s] complete", sName));
                       });

        service.submit(() ->
                       {
                       String sName = Thread.currentThread().getName();
                       t2Name.set(sName);
                       Logger.info(String.format("Starting thread [%s] which will call truncate on distributed cache", sName));
                       while (t2Counter.incrementAndGet() < nAttempts)
                           {
                           raw.truncate();
                           Thread.yield();
                           }
                       Logger.info(String.format("Thread [%s] complete", sName));
                       });

        try
            {
            Eventually.assertDeferred(t1Counter::get, is(nAttempts));
            Eventually.assertDeferred(t2Counter::get, is(nAttempts));
            }
        catch (Throwable t)
            {
            String sMsg = "Deadlock detected!  Look for threads %s and %s in the following thread dump:";
            Logger.err(String.format(sMsg, t1Name.get(), t2Name.get()));
            System.out.println(Threads.getThreadDump(Threads.LockAnalysis.FULL));
            throw t;
            }
        finally
            {
            service.shutdownNow();
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Stop the inner service.
     *
     * @param cache  the cache hosted by the service to stop
     */
    protected void causeServiceDisruption(NamedCache cache)
        {
        CacheService serviceSafe = cache.getCacheService();
        try
            {
            Method       methRunningService = serviceSafe.getClass().getMethod("getRunningService");
            CacheService serviceInternal    = (CacheService) methRunningService.invoke(serviceSafe);

            serviceInternal.stop();
            }
        catch (NoSuchMethodException e)
            {
            fail("Unexpected service: " + serviceSafe);
            }
        catch (IllegalAccessException | InvocationTargetException e)
            {
            fail("Failed to call getRunningService on: " + serviceSafe);
            }
        }

    // ----- helper methods ----------------------------------------------------

    /**
     * Store the provided CQC as an instance variable of the test so that it can be released
     * after the test completes so that any registered listeners are properly removed from the
     * shared cache.
     *
     * @param cqc   the {@link ContinuousQueryCache}
     * @param <K>   the key type
     * @param <VF>  the type of the front value
     * @param <VB>  the type of the back value
     *
     * @return the provided {@link ContinuousQueryCache}, unaltered
     */
    protected <K, VF, VB> ContinuousQueryCache<K, VF, VB> setCQC(ContinuousQueryCache<K, VF, VB> cqc)
        {
        m_queryCache = cqc;
        return cqc;
        }

    /**
     * Obtain and populate the {@code dist-test} named cache.
     *
     * @return the populated {@link NamedCache}, {@code dist-test}
     */
    protected NamedCache<String, Integer> getAndPopulateNamedCache(String sCacheName)
        {
        NamedCache<String, Integer> testCache = getNamedCache(sCacheName, String.class, Integer.class);
        testCache.clear();
        for (int i = 0; i < SOME_DATA; i++)
            {
            testCache.put("TestKey" + i, i);
            }
        return testCache;
        }

    // ----- inner class: TestContinuousQueryCache -----------------------------

    static class TestContinuousQueryCache<K, V_BACK, K_FRONT>
            extends ContinuousQueryCache<K, V_BACK, K_FRONT>
        {

        public TestContinuousQueryCache(NamedCache<K, V_BACK> cache, Filter filter, boolean fCacheValues)
            {
            super(cache, filter, fCacheValues);
            }

        @Override
        protected void changeState(int nState)
            {
            if (m_fLockState)
                {
                return;
                }
            super.changeState(nState);
            }

        protected void lockState()
            {
            m_fLockState = true;
            }

        protected void unlockState()
            {
            m_fLockState = false;
            }

        // ----- data members -----------------------------------------------

        private boolean m_fLockState;
        }

    // ----- inner class: TestCQCListener --------------------------------------

    /**
     * MapListener that continuously receives events from the cache.
     */
    @SuppressWarnings("unused")
    public static class TestCQCListener implements MapListener<String, Integer>
        {
        // ----- constructors -----------------------------------------------

        public TestCQCListener(int count)
            {
            this.m_cCount         = count;
            this.m_cActualInserts = 0;
            this.m_cActualUpdates = 0;
            this.m_cActualDeletes = 0;
            }

        // ----- MapListener methods ----------------------------------------

        public void entryUpdated(MapEvent evt)
            {
            m_cActualUpdates++;
            }

        public void entryInserted(MapEvent evt)
            {
            m_cActualInserts++;
            }

        public void entryDeleted(MapEvent evt)
            {
            m_cActualDeletes++;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Signal listener has received all the events.
         *
         * @return  true if has received all events, false if not.
         */
        public boolean isFinished()
            {
            return getActualTotal() >= m_cCount;
            }

        /**
         * Number of insert events listener actually received.
         *
         * @return  number of event received
         */
        public int getActualInserts()
            {
            return m_cActualInserts;
            }

        /**
         * Number of update events listener actually received.
         *
         * @return  number of event received
         */
        public int getActualUpdates()
            {
            return m_cActualUpdates;
            }

        /**
         * Number of delete events listener actually received.
         *
         * @return  number of event received
         */
        public int getActualDeletes()
            {
            return m_cActualDeletes;
            }

        /**
         * Total number of events listener actually received.
         *
         * @return  number of event received
         */
        public int getActualTotal()
            {
            return m_cActualInserts + m_cActualUpdates + m_cActualDeletes;
            }

        /**
         * Reset the number of events received.
         */
        public void resetActualTotal()
            {
            m_cActualUpdates = 0;
            m_cActualInserts = 0;
            m_cActualDeletes = 0;
            }

        // ----- data members -----------------------------------------------

        /**
         * Number of insert events actually received
         */
        protected int m_cActualInserts;

        /**
         * Number of update events actually received
         */
        protected int m_cActualUpdates;

        /**
         * Number of delete events actually received
         */
        protected int m_cActualDeletes;

        /**
         * Number of events listener is expected to receive
         */
        protected int m_cCount;
        }

    // ----- inner class: InsertProcessor

    public static class InsertProcessor
            extends AbstractProcessor
            implements PortableObject
        {
        @SuppressWarnings("unused")
        public InsertProcessor()
            {
            this("defaultValue");
            }

        public InsertProcessor(String sValue)
            {
            m_sValue = sValue;
            }

        // ----- EntryProcessor interface ---------------------------------

        /**
         * {@inheritDoc}
         */
        public Object process(InvocableMap.Entry entry)
            {
            String sValue = (String) entry.getValue();

            // only update when it doesn't exist or it's different
            if (!entry.isPresent() || !sValue.equals(m_sValue))
                {
                //noinspection unchecked
                entry.setValue(m_sValue);
                }

            return null;
            }

        // ----- PortableObject interface ---------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sValue = in.readString(0);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sValue);
            }

        // ----- data members ---------------------------------------------

        /**
         * Invoked through invokeAll.
         */
        private String m_sValue;
        }

    /**
     * MapListener that verifies the data received in the event.
     *
     */
    @SuppressWarnings("unused")
    public static class GetValueListener implements MapListener<String, String>
        {
        // ----- constructors -----------------------------------------------

        public GetValueListener()
            {
            }

        // ----- MapListener methods ----------------------------------------

        public void entryUpdated(MapEvent evt)
            {
            }

        public void entryInserted(MapEvent evt)
            {
            if (evt.getNewValue() != null)
                {
                m_sValue = (String) evt.getNewValue();
                }
            }

        public void entryDeleted(MapEvent evt)
            {
            }

        /**
         * Get the value of the event received.
         *
         * @return  value of event received
         */
        public String getValue()
            {
            return m_sValue;
            }

        // ----- data members -----------------------------------------------

        /**
         * Number of insert events actually received
         */
        private String m_sValue;
        }

    //----- constants --------------------------------------------------------

    /**
     * Number of data items to put in cache; should generate same number of events.
     */
    public static int SOME_DATA = 100;

    /**
     * The file name of the default client cache configuration file used by
     * this test.
     */
    public static String FILE_CLIENT_CFG_CACHE = "server-cache-config.xml";

    /**
     * The file name of the cache configuration file used by cache
     * servers launched by this test; no proxy configured.
     */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config.xml";

    /**
     * Cluster size.
     */
    protected static final int CLUSTER_SIZE = 3;

    /**
     * List of members in the cluster.
     */
    protected static ArrayList<CoherenceClusterMember> members = new ArrayList<>();

    /**
     * The current {@link ContinuousQueryCache}.
     */
    protected ContinuousQueryCache m_queryCache;
    }

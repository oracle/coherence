/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package net;


import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.listener.SimpleMapListener;
import com.tangosol.util.processor.ExtractorProcessor;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.Person;

import java.io.File;
import java.io.IOException;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;


public class ServiceTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.role", "test,client");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void testRestartOnEventDispatcher()
        {
        Properties properties = new Properties();
        properties.setProperty("coherence.role", "test,server");

        startCacheServer("testRestartOnEventDispatcher-1", "net", null, properties);
        try
            {
            NamedCache<Object, Object> cacheFoo = CacheFactory.getCache("foo");
            NamedCache<Object, Object> cacheBar = CacheFactory.getCache("bar");

            AtomicInteger atomicSuccess = new AtomicInteger();
            cacheFoo.addMapListener(new SimpleMapListener<>().addDeleteHandler(event ->
            {
            try
                {
                cacheBar.put(event.getKey(), event.getOldValue());
                atomicSuccess.set(2);
                }
            catch (RuntimeException e)
                {
                atomicSuccess.set(1);
                }
            }));

            cacheFoo.put(1, 1);
            cacheFoo.remove(1);

            assertThat(cacheFoo.isEmpty(), is(true));
            Eventually.assertDeferred(() -> this.dereference(atomicSuccess), is(2));

            // clear the 'rippled' change and the success status
            cacheBar.remove(1);
            atomicSuccess.set(0);

            // prime the cache
            cacheFoo.put(1, 2);

            // cause the service to stop
            ((SafeCacheService) cacheBar.getCacheService()).getRunningService()
                    .stop();

            // instigate the remove that the MapListener will be invoked
            cacheFoo.remove(1);

            assertThat(cacheFoo.isEmpty(), is(true));
            Eventually.assertDeferred(() -> this.dereference(atomicSuccess), greaterThan(0));

            switch (atomicSuccess.get())
                {
                case 2:    // success
                    break;
                case 1:    // failed
                default:
                    fail("Failed to use a restarted cache");
                }
            }
        finally
            {
            stopCacheServer("testRestartOnEventDispatcher-1");
            CacheFactory.shutdown();
            }
        }

    @Test
    public void testCacheServiceClassLoader() throws Exception
        {
        // This test cannot run with modules enabled
        Assume.assumeFalse("Cannot run this test with Java module path",
                           System.getProperties().containsKey("jdk.module.path"));

        try
            {
            CoherenceClusterMember server = startCacheServer("testCacheServiceClassLoader-1", "net", null, null, true,
                    createServerClassPath());

            NamedCache<Integer, Person> cachePpl = CacheFactory.getCache("resetCL");

            cachePpl.put(1, new Person("435-34-8278", "Vladamir", "Putin", 1957, "mrs putin", new String[0]));

            try
                {
                cachePpl.invoke(1, new ExtractorProcessor<>(ValueExtractor.identity()));
                fail("Storage server should be unable to deserialize");
                }
            catch (RuntimeException e)
                {
                Base.log("Expected exception  " + e.getMessage());
               }

            String sLocation = getLocation(Person.class);

            server.invoke(() ->
            {
            CacheService service = CacheFactory.getCache("resetCL").getCacheService();

            ClassLoader loaderNew = new URLClassLoader(new URL[]{new File(sLocation).toURI().toURL()});
            service.setContextClassLoader(loaderNew);
            Class<?> c1 = loaderNew.loadClass("data.Person");
            return null;
            });

            Person p = cachePpl.invoke(1, new ExtractorProcessor<>(ValueExtractor.identity()));
            assertThat(p, is(notNullValue()));
            }
        catch (URISyntaxException e)
            {
            fail("Got exception while manipulate classpath " + e.getMessage());
            }
        finally
            {
            stopCacheServer("testCacheServiceClassLoader-1", false);
            CacheFactory.shutdown();
            }
        }

    private static String getLocation(Class<?> clz) throws URISyntaxException
        {
        ProtectionDomain domain = clz.getProtectionDomain();
        CodeSource       source = domain == null ? null : domain.getCodeSource();
        URL              url    = source == null ? null : source.getLocation();

        return url == null ? null : Paths.get(url.toURI()).toFile().toString();
        }

    private static String createServerClassPath() throws IOException
        {
        return ClassPath.automatic().excluding(".*coherence-testing-data.*").toString();
        }

    private int dereference(AtomicInteger ref)
        {
        return ref.get();
        }
    }

/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package net;


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
import common.AbstractFunctionalTest;

import data.Person;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

            assertTrue(cacheFoo.isEmpty());
            Eventually.assertThat(invoking(this).dereference(atomicSuccess), is(2));

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

            assertTrue(cacheFoo.isEmpty());
            Eventually.assertThat(invoking(this).dereference(atomicSuccess), greaterThan(0));

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
    public void testCacheServiceClassLoader()
        {
        try
            {
            CoherenceClusterMember server = startCacheServer("testCacheServiceClassLoader-1", "net", null, null, true,
                    createSansClassPath(Person.class));

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
            assertNotNull(p);
            }
        catch (URISyntaxException e)
            {
            fail("Got exception while manipulate classpath " + e.getMessage());
            }
        finally
            {
            stopCacheServer("testCacheServiceClassLoader-1");
            CacheFactory.shutdown();
            }
        }

    protected static String getLocation(Class clz) throws URISyntaxException
        {
        ProtectionDomain domain = clz.getProtectionDomain();
        CodeSource       source = domain == null ? null : domain.getCodeSource();
        URL              url    = source == null ? null : source.getLocation();

        return url == null ? null : Paths.get(url.toURI()).toFile().toString();
        }

    protected static String createSansClassPath(Class clz) throws URISyntaxException
        {
        String sFile      = getLocation(clz);
        String sClassPath = System.getProperty("java.class.path", "");

        if (sFile != null)
            {
            if (sClassPath.indexOf(sFile) == -1)
                {
                // TODO: temporary fix to accommodate windows System32 and system32 being same for a file.
                // handle possible windows differences between drive letter in path and case.
                if (sClassPath.contains("system32") && sFile.contains("System32"))
                    {
                    sFile = sFile.replace("System32", "system32");

                    if (!sClassPath.contains(sFile))
                        {
                        azzertFailed("can not find class file location=" + sFile + " in classpath " + sClassPath);
                        return sClassPath;
                        }
                    }
                }

            sClassPath = sClassPath.replace(sFile, "");
            Assert.assertEquals(sClassPath.indexOf(sFile), -1);

            // if exist, remove consecutive classpath separators from classpath.
            StringBuilder sb = new StringBuilder(2);
            sb.append(File.pathSeparator).append(File.pathSeparator);

            sClassPath = sClassPath.replace(sb.toString(), File.pathSeparator);
            }

        return sClassPath;
        }


    public int dereference(AtomicInteger ref)
        {
        return ref.get();
        }
    }

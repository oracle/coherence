/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import com.oracle.bedrock.runtime.coherence.options.LocalHost;

import com.tangosol.io.ObjectStreamFactory;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.InvocableMap.EntryProcessor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class OptionalDeserializationTest
    {

    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setup()
        {
        System.setProperty("coherence.cacheconfig", "coh27975-cache-config.xml");
        System.setProperty("coherence.wka", LocalHost.loopback().getAddress());
        System.setProperty("java.net.preferIPv4Stack", "true");
        }

    @After
    public void teardown()
        {
        System.clearProperty("coherence.cacheconfig");
        System.clearProperty("coherence.wka");
        System.clearProperty("java.net.preferIPv4Stack");
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testDeserializationOptionalType()
            throws Exception
        {
        NamedCache cache = CacheFactory.getCache("MY_CACHE");
        cache.put("key", "value");

        ObjectStreamFactory defaultStreamFactory = ExternalizableHelper.getObjectStreamFactory();
        ExternalizableHelper.setObjectStreamFactory(new TestObjectStreamFactory(defaultStreamFactory));

        Thread appThread = new Thread(() -> {
            try
                {
                Class<?> appClass = appLoader.loadClass(CacheUsage.class.getName());
                appClass.asSubclass(Runnable.class).newInstance().run();    
                }
            catch (Throwable e)
                {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                }
        });

        appThread.setContextClassLoader(appLoader);
        appThread.start();
        appThread.join();

        // assert that CacheUsage thread is not interrupted due to CNFE as mentioned in COH-27975
        assertFalse(appThread.isInterrupted());
        }

    // ----- inner class: MyResultClassLoader ------------------------------

    static class MyResultClassLoader
            extends ClassLoader
        {
        public MyResultClassLoader(String name)
            {
            super(name, Thread.currentThread().getContextClassLoader());
            }
        }

    // ----- inner class: CacheUsage  ---------------------------------------

    static class CacheUsage
            implements Runnable
        {
        @Override
        public void run()
            {
            System.setProperty("coherence.cacheconfig", "coh27975-cache-config.xml");
            String cacheName = "MY_CACHE";

            NamedCache<Object, Object> cache  = CacheFactory.getCache(cacheName);
            Optional<MyResult>         result = (Optional<MyResult>) cache.invoke("key", new ReturnOptionalResult<>());

            assertNotNull(result);
            assertTrue(Thread.currentThread().getContextClassLoader() instanceof MyResultClassLoader);

            MyResult myResult = result.get();
            assertNotNull(myResult);
            assertNotNull(myResult.getClass().getClassLoader());
            assertEquals(myResult.getClass().getClassLoader().getName(), OptionalDeserializationTest.appLoader.getName());
            }
        }

    // ----- inner class: MyResult  -----------------------------------------

    static class MyResult
            implements Serializable
        {
        }


    // ----- inner class: ReturnOptionalResult  -----------------------------

    static class ReturnOptionalResult<K, V> 
            implements EntryProcessor<K, V, Object>, EntryAggregator<K, V, Object>
        {
        @Override
        public Object process(InvocableMap.Entry<K, V> entry)
            {
            MyResult result = new MyResult();
            assertEquals(result.getClass().getClassLoader(), Thread.currentThread().getContextClassLoader());
            return Optional.of(result);
            }

        @Override
        public Object aggregate(Set setEntries)
            {
            MyResult result = new MyResult();
            assertEquals(result.getClass().getClassLoader(), Thread.currentThread().getContextClassLoader());
            return Optional.of(result);
            }
        }

    // ----- inner class: TestObjectInputStream  ----------------------------

    static class TestObjectInputStream
            extends ObjectInputStream
        {
        public TestObjectInputStream(InputStream in, ClassLoader loader)
                 throws IOException
            {
            super(in);
            this.loader = loader;
            }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass osc)
                throws IOException, ClassNotFoundException
            {
            return Class.forName(osc.getName(), false, loader);
            }

        private final ClassLoader loader;
        }

    // ----- inner class: TestObjectStreamFactory  --------------------------

    static class TestObjectStreamFactory
            implements ObjectStreamFactory
        {
        public TestObjectStreamFactory(ObjectStreamFactory delegate)
            {
            this.delegate = delegate;
            }

        @Override
        public ObjectInput getObjectInput(DataInput in, ClassLoader loader, boolean fForceNew)
                throws IOException
            {
            return new TestObjectInputStream((InputStream) in, loader);
            }

        @Override
        public ObjectOutput getObjectOutput(DataOutput out)
                throws IOException
            {
            return delegate.getObjectOutput(out);
            }

        private final ObjectStreamFactory delegate;
        }

     // ----- constants -----------------------------------------------------

    private static final MyResultClassLoader appLoader = new MyResultClassLoader("app");

    }
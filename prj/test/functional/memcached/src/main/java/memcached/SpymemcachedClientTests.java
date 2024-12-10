/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package memcached;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import net.spy.memcached.internal.CheckedOperationTimeoutException;
import net.spy.memcached.internal.OperationFuture;

import net.spy.memcached.ops.Operation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;


public class SpymemcachedClientTests extends AbstractFunctionalTest
    {

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup() throws Exception
        {
        String hostName = LocalPlatform.get().getLoopbackAddress().getHostAddress();

        System.setProperty("memcached.hostname", hostName);

        Properties props = new Properties();
        props.setProperty("coherence.override","memcached-coherence-override.xml");
        props.setProperty("memcached.hostname", hostName);
        props.setProperty("tangosol.log.level", "9");

        CoherenceClusterMember member = startCacheServer("MemcachedTests", "memcached", "memcached-cache-config.xml", props);
        Eventually.assertThat(invoking(member).getClusterSize(), is(2));

        AuthDescriptor ad = new AuthDescriptor(new String[] { "PLAIN" }, new PlainCallbackHandler("username",
            "password"));

        // Get a memcached client connected to several servers with the binary protocol
        s_client = new MemcachedClient(new ConnectionFactoryBuilder().setProtocol(Protocol.BINARY)
                .setAuthDescriptor(ad).setAuthWaitTime(5000).build(),
                AddrUtil.getAddresses(hostName + ":11211"));
        System.out.println("Memcached client = " + s_client);

        Eventually.assertThat(invoking(s_client).getAvailableServers().size(), is(not(0)));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("MemcachedTests");
        }

    @Test
    public void testShared()
            throws Exception
        {
        MemcachedClient client = s_client;
        String key = "sharedkey";
        String data = "sharedvalue";
        if (!client.set(key, 0, data).get())
            {
            throw new Exception("testShared failed to set value");
            }
        String dataRead = (String) client.get(key);
        assertEquals(data, dataRead);
        }

    @Test
    public void testPut()
            throws Exception
        {
        MemcachedClient client = s_client;
        int i = 0;
        OperationFuture<Boolean> putFuture = null;

        while (i < 100)
            {
            putFuture = client.set("putkey", 0, String.valueOf(i++));
            }

        try
            {
            Boolean result = putFuture.get(30, TimeUnit.SECONDS);
            assertThat("Final put failed", result, is(true));
            }
        catch (CheckedOperationTimeoutException e)
            {
            e.printStackTrace();
            for (Operation op : e.getOperations())
                {
                System.err.println("Failing Operation: " + op);
                }
            fail("Caught CheckedOperationTimeoutException");
            }
        }

    @Test
    public void testAdd()
            throws Exception
        {
        MemcachedClient client = s_client;
        client.delete("addkey");
        OperationFuture<Boolean> addFuture = client.add("addkey", 0, "add");
        OperationFuture<Boolean> addFuture2 = client.add("addkey", 0, "replace");
        if (!addFuture.get())
            {
            throw new Exception("add failed");
            }
        if (addFuture2.get()) //should fail
            {
            throw new Exception("add failed. replaced existing key");
            }
        }

    @Test
    public void testGetBulk()
            throws Exception
        {
        MemcachedClient client = s_client;
        List keys = new ArrayList();
        for (int i = 0; i < 10; i++)
            {
            client.add("bulkkey" + i, 0, "bulkkey" + i);
            keys.add("bulkkey" + i);
            }
        Map<String, Object> map = client.getBulk(keys);
        for (Map.Entry<String, Object> entry : map.entrySet())
            {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            assertEquals(key, value);
            }
        }

    @Test
    public void testCas()
            throws Exception
        {
        MemcachedClient client = s_client;
        String key = "caskey";
        client.set(key, 0, "casvalue");
        CASValue<Object> casValue = client.gets(key);
        assertEquals(casValue.getValue(), "casvalue");
        assertEquals(casValue.getCas(), 1l);
        CASResponse response = client.cas(key, casValue.getCas(), "casvalue2");
        if (!response.equals(CASResponse.OK))
            {
            throw new Exception("getCas failed. Failed to update value");
            }
        response = client.cas(key, 100, "casvalueN");
        if (response.equals(CASResponse.OK))
            {
            throw new Exception("getCas failed. Updated value even with wrong CAS");
            }
        casValue = client.gets(key);
        response = client.cas(key, casValue.getCas(), 2, "casvalue3", client.getTranscoder());
        if (!response.equals(CASResponse.OK))
            {
            throw new Exception("getCas failed. Failed to update value the second time");
            }
        Thread.sleep(2000);
        String dataRead = (String) client.get(key);
        assertNull(dataRead);
        }

    @Test
    public void testDelete()
            throws Exception
        {
        MemcachedClient client = s_client;
        String key = "deletekey";
        OperationFuture<Boolean> putFuture = client.set(key, 0, "value");
        if (!putFuture.get())
            {
            throw new Exception("set failed in testDelete");
            }
        OperationFuture<Boolean> delFuture = client.delete(key);
        if (!delFuture.get())
            {
            throw new Exception("delete failed");
            }
        String value = (String) client.get(key);
        assertNull(value);
        }

    @Test
    public void testTimeout()
            throws Exception
        {
        MemcachedClient client = s_client;
        OperationFuture<Boolean> putFuture = client.set("timeoutkey", 2, "value");
        if (!putFuture.get())
            {
            throw new Exception("set failed");
            }
        Thread.sleep(2 * 1000);
        String dataRead = (String) client.get("timeoutkey");
        assertNull(dataRead);
        }

    @Test
    public void testReplace()
            throws Exception
        {
        MemcachedClient client = s_client;
        OperationFuture<Boolean> putFuture = client.set("replacekey", 0, "set");
        OperationFuture<Boolean> replaceFuture = client.replace("replacekey", 0, "replace");
        if (!replaceFuture.get())
            {
            throw new Exception("replace failed to update");
            }
        replaceFuture = client.replace("key2", 0, "replace");
        if (replaceFuture.get()) //should fail
            {
            throw new Exception("replace failed. Inserted new key");
            }
        }

    @Test
    public void testIncrement()
            throws Exception
        {
        MemcachedClient client = s_client;
        String key = "incrKey";
        client.delete(key);
        long lValue = client.incr(key, 10, 100);
        assertEquals(lValue, 100);
        lValue = client.incr(key, 10);
        assertEquals(lValue, 110);
        client.set(key, 0, String.valueOf(200));
        lValue = client.incr(key, 10);
        assertEquals(lValue, 210);
        lValue = client.incr(key, Long.MAX_VALUE);
        long expectedVal = Long.MIN_VALUE + 209;
        assertEquals(lValue, expectedVal);
        }

    @Test
    public void testDecrement()
            throws Exception
        {
        MemcachedClient client = s_client;
        String key = "decrKey";
        client.delete(key);
        long lValue = client.decr(key, 10, 100);
        assertEquals(lValue, 100);
        lValue = client.decr(key, 10);
        assertEquals(lValue, 90);
        client.set(key, 0, String.valueOf(200));
        lValue = client.decr(key, 10);
        assertEquals(lValue, 190);
        lValue = client.decr(key, 200);
        assertEquals(lValue, 0);
        }

    @Test
    public void testAppendPrepend()
            throws Exception
        {
        MemcachedClient client = s_client;
        String key = "updatekey";
        client.set(key, 0, "key");
        CASValue<Object> casValue = client.gets(key);
        client.append(casValue.getCas(), key, "append");
        casValue = client.gets(key);
        assertEquals(casValue.getValue(), "keyappend");
        client.prepend(casValue.getCas(), key, "prepend");
        casValue = client.gets(key);
        assertEquals(casValue.getValue(), "prependkeyappend");
        }

    protected static MemcachedClient s_client;
    }

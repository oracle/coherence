/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package memcached;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.coherence.memcached.server.MemcachedHelper;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.CachedData;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import net.spy.memcached.compat.SpyObject;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.transcoders.Transcoder;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.net.NamedCache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import common.AbstractFunctionalTest;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;


public class SpymemcachedPofClientTests extends AbstractFunctionalTest
    {
    public SpymemcachedPofClientTests() throws Exception
        {
        super("memcached-cache-config.xml");
        }

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
        props.setProperty("tangosol.coherence.override","memcached-coherence-override.xml");
        props.setProperty("memcached.hostname", hostName);

        CoherenceClusterMember member = startCacheServer("MemcachedPoFTests", "memcached", "memcached-cache-config.xml", props);
        Eventually.assertThat(invoking(member).getClusterSize(), is(2));

        AuthDescriptor ad = new AuthDescriptor(new String[] { "PLAIN" }, new PlainCallbackHandler("username",
            "password"));

        // Get a memcached client connected to several servers with the binary protocol
        s_client = new MemcachedClient(new ConnectionFactoryBuilder().setProtocol(Protocol.BINARY)
                .setAuthDescriptor(ad).setAuthWaitTime(5000).build(),
                AddrUtil.getAddresses("localhost:11212"));

        System.out.println("Memcached client = " + s_client);

        Eventually.assertThat(invoking(s_client).getAvailableServers().size(), is(not(0)));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("MemcachedPoFTests");
        }

    @Test
    public void testPof()
            throws Exception
        {
        MemcachedClient client = s_client;
        String key = "pofKey";
        PofUser user = new PofUser("memcached", 1);
        PofTranscoder<PofUser> tc = new PofTranscoder("memcached-pof-config.xml");

        if (!client.set(key, 0, user, tc).get())
            {
            throw new Exception("testPof failed to set value");
            }

        // get it using Coherence client
        NamedCache cache    = getNamedCache("memcache");
        PofUser    readUser = (PofUser) cache.get(key);

        if (readUser != null)
            {
            assertEquals(readUser.getName(), "memcached");
            assertEquals(readUser.getAge(), 1);
            }
        PofUser couUser = new PofUser("coherence", 2);
        cache.put("coherenceclient", couUser);

        // read pof user set by coherence client using memcached client
        PofUser cohUser = client.get("coherenceclient", tc);
        if (cohUser != null)
            {
            assertEquals(cohUser.getName(), "coherence");
            assertEquals(cohUser.getAge(), 2);
            }
        }

    @Test
    public void shouldRejectDangerousPassThroughSet()
            throws Exception
        {
        MemcachedClient client = s_client;
        String          key    = "dangerousPofValue";

        getNamedCache("memcache").remove(key);

        OperationFuture<Boolean> future = client.set(key, 0,
                new byte[] {(byte) ExternalizableHelper.FMT_OBJ_SER}, new RawTranscoder());
        boolean fStored = false;
        try
            {
            fStored = future.get(30, TimeUnit.SECONDS);
            }
        catch (Exception expected)
            {
            // failed server-side validation is acceptable; the value must not be stored
            }

        assertFalse(fStored);
        assertFalse(getNamedCache("memcache").containsKey(key));
        }

    @Test
    public void shouldRejectLegacyDangerousStoredValueOnCoherenceGet()
        {
        NamedCache cache = getNamedCache("memcache");
        String     key   = "legacyDangerousPofValue";

        MaterializationSentinel.reset();
        Binary binValue  = ExternalizableHelper.toBinary(new MaterializationSentinel());
        Binary binStored = MemcachedHelper.decorateBinary(binValue, 0, 1);

        assertEquals(ExternalizableHelper.FMT_OBJ_SER, binValue.byteAt(0) & 0xFF);
        putBinary(cache, key, binStored);
        try
            {
            assertThrows(RuntimeException.class, () -> cache.get(key));
            assertFalse(MaterializationSentinel.wasMaterialized());
            }
        finally
            {
            removeBinary(cache, key);
            }
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected static void putBinary(NamedCache cache, Object key, Binary binValue)
        {
        cache.invoke(key, new PutBinaryProcessor(binValue));
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected static void removeBinary(NamedCache cache, Object key)
        {
        cache.invoke(key, RemoveBinaryProcessor.INSTANCE);
        }

    protected static MemcachedClient s_client;

    public static class PutBinaryProcessor
            extends AbstractProcessor
            implements PortableObject
        {
        public PutBinaryProcessor()
            {
            }

        public PutBinaryProcessor(Binary binValue)
            {
            m_binValue = binValue;
            }

        public Object process(InvocableMap.Entry entry)
            {
            ((BinaryEntry) entry).updateBinaryValue(m_binValue);
            return null;
            }

        public void readExternal(PofReader in)
                throws IOException
            {
            m_binValue = (Binary) in.readObject(0);
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_binValue);
            }

        private Binary m_binValue;
        }

    public static class RemoveBinaryProcessor
            extends AbstractProcessor
            implements PortableObject
        {
        public Object process(InvocableMap.Entry entry)
            {
            ((BinaryEntry) entry).updateBinaryValue(null);
            return null;
            }

        public void readExternal(PofReader in)
            {
            }

        public void writeExternal(PofWriter out)
            {
            }

        public static final RemoveBinaryProcessor INSTANCE = new RemoveBinaryProcessor();
        }

    public static class MaterializationSentinel
            implements Serializable
        {
        public static void reset()
            {
            s_fMaterialized = false;
            }

        public static boolean wasMaterialized()
            {
            return s_fMaterialized;
            }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            s_fMaterialized = true;
            in.defaultReadObject();
            }

        private static boolean s_fMaterialized;
        }

    public static class RawTranscoder
            extends SpyObject
            implements Transcoder<byte[]>
        {
        public boolean asyncDecode(CachedData data)
            {
            return false;
            }

        public byte[] decode(CachedData data)
            {
            return data.getData();
            }

        public CachedData encode(byte[] abValue)
            {
            return new CachedData(0, abValue, CachedData.MAX_SIZE);
            }

        public int getMaxSize()
            {
            return CachedData.MAX_SIZE;
            }
        }
    }

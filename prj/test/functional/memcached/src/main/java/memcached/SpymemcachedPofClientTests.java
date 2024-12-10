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
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.net.NamedCache;

import java.util.Properties;

import com.oracle.coherence.testing.AbstractFunctionalTest;

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
        props.setProperty("coherence.override","memcached-coherence-override.xml");
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

    protected static MemcachedClient s_client;
    }

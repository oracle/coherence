/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SystemConfigTests
    {
    @After
    public void cleanup()
        {
        Coherence.closeAll();
        System.clearProperty("coherence.serializer");
        System.clearProperty("coherence.system.serializer");
        }

    @Test
    public void shouldUseSpecificSystemSerializer() throws Exception
        {
        System.setProperty("coherence.serializer", "pof");
        System.setProperty("coherence.system.serializer", "java");

        Coherence coherence = Coherence.clusterMember()
                .start()
                .get(5, TimeUnit.MINUTES);

        Session          sessionSys = coherence.getSession(Coherence.SYSTEM_SESSION);
        NamedCache<?, ?> cacheSys   = sessionSys.getCache("sys$config-test");
        CacheService     serviceSys = cacheSys.getCacheService();

        Session          session = coherence.getSession();
        NamedCache<?, ?> cache   = session.getCache("foo");
        CacheService     service = cache.getCacheService();

        assertThat(serviceSys.getSerializer(), is(instanceOf(DefaultSerializer.class)));
        assertThat(service.getSerializer(), is(instanceOf(ConfigurablePofContext.class)));
        }

    @Test
    public void shouldUseCoherenceSerializer() throws Exception
        {
        System.setProperty("coherence.serializer", "pof");
        System.clearProperty("coherence.system.serializer");

        Coherence coherence = Coherence.clusterMember()
                .start()
                .get(5, TimeUnit.MINUTES);

        Session          sessionSys = coherence.getSession(Coherence.SYSTEM_SESSION);
        NamedCache<?, ?> cacheSys   = sessionSys.getCache("sys$config-test");
        CacheService     serviceSys = cacheSys.getCacheService();

        Session          session = coherence.getSession();
        NamedCache<?, ?> cache   = session.getCache("foo");
        CacheService     service = cache.getCacheService();

        assertThat(serviceSys.getSerializer(), is(instanceOf(ConfigurablePofContext.class)));
        assertThat(service.getSerializer(), is(instanceOf(ConfigurablePofContext.class)));
        }


    @Test
    public void shouldUseDefaultSerializer() throws Exception
        {
        System.clearProperty("coherence.serializer");
        System.clearProperty("coherence.system.serializer");

        Coherence coherence = Coherence.clusterMember()
                .start()
                .get(5, TimeUnit.MINUTES);

        Session          sessionSys = coherence.getSession(Coherence.SYSTEM_SESSION);
        NamedCache<?, ?> cacheSys   = sessionSys.getCache("sys$config-test");
        CacheService     serviceSys = cacheSys.getCacheService();

        Session          session = coherence.getSession();
        NamedCache<?, ?> cache   = session.getCache("foo");
        CacheService     service = cache.getCacheService();

        assertThat(serviceSys.getSerializer(), is(instanceOf(DefaultSerializer.class)));
        assertThat(service.getSerializer(), is(instanceOf(DefaultSerializer.class)));
        }

    }

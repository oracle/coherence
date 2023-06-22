/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;

import com.oracle.bedrock.runtime.coherence.callables.GetClusterSize;
import com.oracle.bedrock.runtime.coherence.callables.GetLocalMemberId;
import com.oracle.bedrock.runtime.coherence.callables.SessionExists;

import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;

import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.AbstractTest;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CoherenceSessionIT
        extends AbstractTest
    {
    @Test
    public void shouldGetDefaultSession()
        {
        Platform platform = getPlatform();

        try (CoherenceCacheServer server = platform.launch(CoherenceCacheServer.class,
                                                           ClusterPort.automatic(),
                                                           LocalHost.only(),
                                                           WellKnownAddress.loopback(),
                                                           m_testLogs))
            {
            Eventually.assertThat(server, new GetLocalMemberId(), is(1));
            Eventually.assertThat(server, new GetClusterSize(), is(1));
            Eventually.assertThat(server, new SessionExists(Coherence.DEFAULT_NAME), is(true));

            Session session = server.getSession();
            assertThat(session, is(notNullValue()));
            assertThat(session.isActive(), is(true));
            assertThat(session.getScopeName(), is(Coherence.DEFAULT_SCOPE));
            }
        }

    @Test
    public void shouldGetCacheFromDefaultSession()
        {
        Platform platform = getPlatform();

        try (CoherenceCacheServer server = platform.launch(CoherenceCacheServer.class,
                                                           ClusterPort.automatic(),
                                                           LocalHost.only(),
                                                           WellKnownAddress.loopback(),
                                                           m_testLogs))
            {
            Eventually.assertThat(server, new GetLocalMemberId(), is(1));
            Eventually.assertThat(server, new GetClusterSize(), is(1));
            Eventually.assertThat(server, new SessionExists(Coherence.DEFAULT_NAME), is(true));

            Session session = server.getSession();
            assertThat(session, is(notNullValue()));

            NamedCache<String, String> sessionCache = session.getCache("test");
            NamedCache<String, String> cache = server.getCache("test");

            assertThat(sessionCache, is(notNullValue()));
            assertThat(cache, is(notNullValue()));

            sessionCache.put("key-1", "value-1");
            assertThat(cache.get("key-1"), is("value-1"));
            }
        }

    @Test
    public void shouldGetSystemSession()
        {
        Platform platform = getPlatform();

        try (CoherenceCacheServer server = platform.launch(CoherenceCacheServer.class,
                                                           ClusterPort.automatic(),
                                                           LocalHost.only(),
                                                           WellKnownAddress.loopback(),
                                                           m_testLogs))
            {
            Eventually.assertThat(server, new GetLocalMemberId(), is(1));
            Eventually.assertThat(server, new GetClusterSize(), is(1));
            Eventually.assertThat(server, new SessionExists(Coherence.SYSTEM_SESSION), is(true));

            Session session = server.getSession(Coherence.SYSTEM_SESSION);
            assertThat(session, is(notNullValue()));
            assertThat(session.isActive(), is(true));
            assertThat(session.getScopeName(), is(Coherence.SYSTEM_SESSION));
            }
        }


    @Test
    public void shouldGetCacheFromSystemSession()
        {
        Platform platform = getPlatform();

        try (CoherenceCacheServer server = platform.launch(CoherenceCacheServer.class,
                                                           ClusterPort.automatic(),
                                                           LocalHost.only(),
                                                           WellKnownAddress.loopback(),
                                                           m_testLogs))
            {
            Eventually.assertThat(server, new GetLocalMemberId(), is(1));
            Eventually.assertThat(server, new GetClusterSize(), is(1));
            Eventually.assertThat(server, new SessionExists(Coherence.SYSTEM_SESSION), is(true));
            Eventually.assertThat(server, new SessionExists(Coherence.DEFAULT_NAME), is(true));

            Session session = server.getSession(Coherence.SYSTEM_SESSION);
            assertThat(session, is(notNullValue()));

            NamedCache<String, String> sessionCache = session.getCache("sys$config-test");
            NamedCache<String, String> cache = server.getCache(Coherence.SYSTEM_SESSION, "sys$config-test");

            assertThat(sessionCache, is(notNullValue()));
            assertThat(cache, is(notNullValue()));

            sessionCache.put("key-1", "value-1");
            assertThat(cache.get("key-1"), is("value-1"));

            NamedCache<String, String> defaultCache = server.getCache("sys$config-test");
            assertThat(defaultCache.get("key-1"), is(nullValue()));
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtains the {@link Platform} to use when realizing applications.
     */
    public Platform getPlatform()
        {
        return LocalPlatform.get();
        }

    // ----- data members ---------------------------------------------------

    /**
     * JUnit 5 extension to write Coherence logs to target/test-output/
     */
    @RegisterExtension
    static final TestLogsExtension m_testLogs = new TestLogsExtension(CoherenceSessionIT.class);
    }

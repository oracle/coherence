/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.io.Serializer;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.grpc.GrpcChannelDependencies;
import com.tangosol.net.grpc.GrpcDependencies;
import org.junit.jupiter.api.AfterAll;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class BaseLocalDefaultCacheConfigGrpcIT
        extends AbstractGrpcClientIT
    {
    static void runCluster(SessionConfiguration... aCfgSession) throws Exception
        {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.override", "coherence-json-override.xml");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.cacheconfig", "coherence-cache-config.xml");
        System.setProperty("coherence.extend.port", String.valueOf(PORTS.next()));

        String sGrpcPort = String.valueOf(PORTS.next());
        System.setProperty(GrpcDependencies.PROP_PORT, sGrpcPort);
        System.setProperty(GrpcChannelDependencies.PROP_DEFAULT_CHANNEL_PORT, sGrpcPort);
        System.setProperty("coherence.grpc.address", "127.0.0.1");
        System.setProperty("coherence.grpc.port", sGrpcPort);

        CoherenceConfiguration.Builder cfgBuilder = CoherenceConfiguration.builder()
                .withSession(SessionConfiguration.defaultSession());

        for (SessionConfiguration cfgSession : aCfgSession)
            {
            cfgBuilder.withSession(cfgSession);
            }

        Set<String> setSessionName = new HashSet<>();
        Set<String> setSerializer  = serializers().map(a -> String.valueOf(a.get()[0]))
                .collect(Collectors.toSet());

        for (String sSerializer : setSerializer)
            {
            String sName = sessionNameFromSerializerName(sSerializer);
            SessionConfiguration cfg = SessionConfiguration.builder()
                    .named(sName)
                    .withScopeName(sName)
                    .withMode(Coherence.Mode.GrpcFixed)
                    .withParameter("coherence.serializer", sSerializer)
                    .withParameter("coherence.profile", "thin")
                    .withParameter("coherence.proxy.enabled", "false")
                    .build();

            setSessionName.add(sName);
            cfgBuilder.withSession(cfg);
            }

        Coherence coherence = Coherence.clusterMember(cfgBuilder.build()).start().get(5, TimeUnit.MINUTES);

        Eventually.assertDeferred(IsGrpcProxyRunning::locally, is(true));

        for (String sName : setSessionName)
            {
            Session session = coherence.getSession(sName);
            SESSIONS.put(sName, session);
            }
        s_defaultSession = coherence.getSession();
        }

    @AfterAll
    static void shutdownCoherence()
        {
        Coherence.closeAll();
        }

    @Override
    protected <K, V> NamedCache<K, V> createClient(String sCacheName, String sSerializerName, Serializer serializer)
        {
        String sName = sessionNameFromSerializerName(sSerializerName);
        Session session = SESSIONS.get(sName);
        assertThat(session, is(notNullValue()));
        return session.getCache(sCacheName);
        }

    protected static String sessionNameFromSerializerName(String sSerializerName)
        {
        return sSerializerName.isEmpty() ? "default" : sSerializerName;
        }

    @Override
    protected <K, V> NamedCache<K, V> ensureCache(String sName, ClassLoader loader)
        {
        return s_defaultSession.getCache(sName);
        }

    protected static void closeSilent(AutoCloseable closeable)
        {
        try
            {
            closeable.close();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }
        }

    // ----- data members ---------------------------------------------------

    static final Map<String, Session> SESSIONS = new HashMap<>();

    static Session s_defaultSession;

    static final String CLUSTER_NAME = "LocalDefaultCacheConfigGrpcIT";

    static final LocalPlatform PLATFORM = LocalPlatform.get();

    static final AvailablePortIterator PORTS = PLATFORM.getAvailablePorts();
    }

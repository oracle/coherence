/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.bedrock.deferred.Deferred;
import com.oracle.bedrock.deferred.PermanentlyUnavailableException;
import com.oracle.bedrock.deferred.TemporarilyUnavailableException;
import com.oracle.bedrock.options.LaunchLogging;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GrpcClientFailoverTests
    {
    @BeforeAll
    static void setup() throws Exception
        {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");

        SessionConfiguration sessionConfiguration = SessionConfiguration.builder()
                .withParameter("coherence.profile", "thin")
                .withParameter("coherence.client", "grpc")
                .build();

        CoherenceConfiguration coherenceConfiguration = CoherenceConfiguration.builder()
                .withSessions(sessionConfiguration)
                .build();

        s_coherence = Coherence.client(coherenceConfiguration).start().get(5, TimeUnit.MINUTES);
        }

    @Test
    public void shouldFailOverOnProxyRestart() throws Exception
        {
        Assumptions.assumeFalse(isHelidon(), () -> "Fail-over tests are currently skipped when using the Helidon client");
        
        CoherenceClusterBuilder builder = new CoherenceClusterBuilder()
                .include(3, CoherenceClusterMember.class, LocalStorage.enabled(),
                        ClusterName.of(CLUSTER_NAME),
                        WellKnownAddress.loopback(),
                        LocalHost.only(),
                        LaunchLogging.disabled(),
                        CacheConfig.of("coherence-cache-config.xml"),
                        DisplayName.of("storage"),
                        StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                        RoleName.of("storage"),
                        Logging.atFinest(),
                        s_testLogs);

        try (CoherenceCluster cluster = builder.build())
            {
            Session                  session = s_coherence.getSession();
            NamedMap<String, String> map     = session.getMap("test");
            String                   sKey    = "key-1";
            String                   sValue  = "value-1";

            map.put(sKey, sValue);
            assertThat(map.get(sKey), is(sValue));

            RetryingCacheGet<String, String> retryingCacheGet = new RetryingCacheGet<>(map, sKey);

//            try (Concurrent.Assertion assertion = Concurrently.assertThat(retryingCacheGet, is(sValue)))
//                {
//                // wait for at least one call to the assertion
//                retryingCacheGet.awaitFirstCall();

                cluster.relaunch();

//                assertion.check();
//                }

            assertThat(map.get(sKey), is(sValue));
            }
        }

    protected boolean isHelidon()
        {
        try
            {
            Class.forName("io.helidon.webclient.grpc.GrpcClient");
            return true;
            }
        catch (ClassNotFoundException ignored)
            {
            }
        return false;
        }

    // ----- RetryingCacheGet -----------------------------------------------

    static class RetryingCacheGet<K, V>
            implements Deferred<V>
        {
        public RetryingCacheGet(NamedMap<K, V> map, K key)
            {
            f_map = map;
            f_key = key;
            }

        @Override
        public V get() throws TemporarilyUnavailableException, PermanentlyUnavailableException
            {
            while (true)
                {
                try
                    {
                    V oValue;
                    //try (RetryGrpcCall ignored = RetryGrpcCall.maxAttempts(5))
                    //    {
                    oValue = f_map.get(f_key);
                    f_latch.countDown();
                    //    }
                    return oValue;
                    }
                catch (Exception e)
                    {
                    Logger.err("Error in assertion check", e);
                    throw new RuntimeException(e);
                    }
                }
            }

        public boolean awaitFirstCall() throws InterruptedException
            {
            return awaitFirstCall(1, TimeUnit.MINUTES);
            }

        public boolean awaitFirstCall(long timeout, TimeUnit units) throws InterruptedException
            {
            return f_latch.await(timeout, units);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link NamedMap} to get from.
         */
        private final NamedMap<K, V> f_map;

        /**
         * The key to get.
         */
        private final K f_key;

        /**
         * A {@link CountDownLatch} to allow code to wait for the first call.
         */
        private final CountDownLatch f_latch = new CountDownLatch(1);
        }

    // ----- data members ---------------------------------------------------

    public static final String CLUSTER_NAME = "GrpcClientFailoverTests";

    @RegisterExtension
    static TestLogsExtension s_testLogs = new TestLogsExtension(GrpcClientFailoverTests.class);

    static Coherence s_coherence;
    }

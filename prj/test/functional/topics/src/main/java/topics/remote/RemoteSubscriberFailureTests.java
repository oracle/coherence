/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.remote;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.util.invoke.Lambdas;
import org.junit.ClassRule;

public class RemoteSubscriberFailureTests
    {


    // ----- data members ---------------------------------------------------

    static public int STORAGE_MEMBER_COUNT = 3;

    static public int PROXY_MEMBER_COUNT = 2;

    public static final String CACHE_CONFIG_FILE = "coherence-cache-config.xml";

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(RemoteSubscriberFailureTests.class);

    @ClassRule
    public static CoherenceClusterResource cluster =
        new CoherenceClusterResource()
            .with(ClusterName.of(RemoteSubscriberFailureTests.class.getSimpleName() + "Cluster"),
                  CacheConfig.of(CACHE_CONFIG_FILE),
                  Logging.atMax(),
                  SystemProperty.of("coherence.serializer", "java"),
                  SystemProperty.of("coherence.management", "all"),
                  SystemProperty.of("coherence.management.remote", "true"),
                  SystemProperty.of("coherence.management.refresh.expiry", "1ms"),
                  LocalHost.only(),
                  WellKnownAddress.of("127.0.0.1"),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)))
            .include(STORAGE_MEMBER_COUNT, CoherenceClusterMember.class,
                     RoleName.of("storage"),
                     DisplayName.of("storage"),
                     SystemProperty.of("coherence.proxy.enabled", false),
                     s_testLogs.builder(),
                     LocalStorage.enabled())
            .include(PROXY_MEMBER_COUNT, CoherenceClusterMember.class,
                     RoleName.of("proxy"),
                     DisplayName.of("proxy"),
                     SystemProperty.of("coherence.proxy.enabled", true),
                     s_testLogs.builder(),
                     LocalStorage.disabled());

    }

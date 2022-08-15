/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.guides.preload.cachestore.IsCacheStoreEnabled;
import com.oracle.coherence.guides.preload.cachestore.SimpleController;
import com.oracle.coherence.guides.preload.db.HsqldbRunner;
import com.oracle.coherence.guides.preload.model.Customer;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PreloaderWithCacheStoreIT
        extends AbstractPreloadIT
    {
    @BeforeAll
    public static void setupCaches()
        {
        CoherenceCluster cluster = clusterRunner.getCluster();
        assertThat(cluster.isSafe(), is(true));

        ConfigurableCacheFactory ccf = clusterRunner.createSession(SessionBuilders.storageDisabledMember());
        Session session = new ConfigurableCacheFactorySession(ccf, Classes.getContextClassLoader());

        customersCache = session.getCache("customers");
        }

    @BeforeEach
    public void resetData() throws SQLException
        {
        customersCache.clear();
        createTables(connection);
        }

    @Test
    public void shouldLoadCustomersToCacheButNotWriteThrough() throws Exception
        {
        // turn off the customers cache store
        SimpleController.disableCacheStores(customersCache);
        Eventually.assertDeferred(() -> IsCacheStoreEnabled.isDisabled(customersCache), is(true));

        Map<Integer, Customer> customers = createTestCustomers();
        customersCache.putAll(customers);

        // turn the customers cache store on
        SimpleController.enableCacheStores(customersCache);
        Eventually.assertDeferred(() -> IsCacheStoreEnabled.isEnabled(customersCache), is(true));

        // the customers table in the DB should be empty as we disabled cache stores
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from customers");)
            {
            assertThat(resultSet.next(), is(true));
            assertThat(resultSet.getInt(1), is(0));
            }
        }

    // ----- data members ---------------------------------------------------

    private static NamedCache<Integer, Customer> customersCache;

    @RegisterExtension
    static final TestLogsExtension testLogs = new TestLogsExtension(PreloaderWithCacheStoreIT.class);

    /**
     * A JUnit 5 extension that runs a Coherence cluster.
     */
    @RegisterExtension
    static final CoherenceClusterExtension clusterRunner = new CoherenceClusterExtension()
            .with(WellKnownAddress.of("127.0.0.1"),
                    ClusterName.of("preload-test"),
                    SystemProperty.of("jdbc.url", hsqldbRunner.getJdbcURL()),
                    CacheConfig.of("controllable-cachestore-cache-config.xml"),
                    IPv4Preferred.yes(),
                    LocalHost.only())
            .include(3, CoherenceClusterMember.class,
                     LocalStorage.enabled(),
                     testLogs,
                     RoleName.of("storage"),
                     DisplayName.of("storage"));
    }

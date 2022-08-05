/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.guides.preload.db.HsqldbRunner;
import com.oracle.coherence.guides.preload.loaders.CustomerBinaryJdbcLoader;
import com.oracle.coherence.guides.preload.loaders.CustomerJdbcLoader;
import com.oracle.coherence.guides.preload.loaders.OrderJdbcLoader;
import com.oracle.coherence.guides.preload.model.Customer;
import com.oracle.coherence.guides.preload.model.Order;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SimplePreloaderIT
        extends AbstractPreloadIT
    {
    @BeforeAll
    public static void setupCaches()
        {
        CoherenceCluster cluster = clusterRunner.getCluster();
        assertThat(cluster.isSafe(), is(true));

        ConfigurableCacheFactory ccf = clusterRunner.createSession(SessionBuilders.storageDisabledMember());
        session = new ConfigurableCacheFactorySession(ccf, Classes.getContextClassLoader());

        customersCache = session.getCache("customers");
        ordersCache = session.getCache("orders");
        }

    @BeforeEach
    public void resetData() throws SQLException
        {
        customersCache.clear();
        ordersCache.clear();
        createTables(connection);
        }

    @Test
    public void shouldLoadCustomers()
        {
        Map<Integer, Customer> customers = loadCustomersToDB();

        CustomerJdbcLoader loader = new CustomerJdbcLoader(connection, session, 10);
        loader.run();

        assertThat(customersCache.size(), is(customers.size()));
        for (Map.Entry<Integer, Customer> entry : customers.entrySet())
            {
            Customer expected = entry.getValue();
            Customer customer = customersCache.get(entry.getKey());
            assertThat(customer, is(notNullValue()));
            assertThat(customer.getId(), is(expected.getId()));
            assertThat(customer.getName(), is(expected.getName()));
            assertThat(customer.getAddress(), is(expected.getAddress()));
            assertThat(customer.getCreditLimit(), is(expected.getCreditLimit()));
            }
        }

    @Test
    public void shouldLoadCustomersUsingBinaryLoader()
        {
        Map<Integer, Customer> customers = loadCustomersToDB();

        CustomerBinaryJdbcLoader loader = new CustomerBinaryJdbcLoader(connection, session, 10);
        loader.run();

        assertThat(customersCache.size(), is(customers.size()));
        for (Map.Entry<Integer, Customer> entry : customers.entrySet())
            {
            Customer expected = entry.getValue();
            Customer customer = customersCache.get(entry.getKey());
            assertThat(customer, is(notNullValue()));
            assertThat(customer.getId(), is(expected.getId()));
            assertThat(customer.getName(), is(expected.getName()));
            assertThat(customer.getAddress(), is(expected.getAddress()));
            assertThat(customer.getCreditLimit(), is(expected.getCreditLimit()));
            }
        }

    @Test
    public void shouldLoadOrders()
        {
        Map<Integer, Order> orders = loadOrdersToDB();

        OrderJdbcLoader loader = new OrderJdbcLoader(connection, session, 10);
        loader.run();

        assertThat(ordersCache.size(), is(orders.size()));
        for (Map.Entry<Integer, Order> entry : orders.entrySet())
            {
            Order expected = entry.getValue();
            Order order = ordersCache.get(entry.getKey());
            assertThat(order, is(notNullValue()));
            assertThat(order.getId(), is(expected.getId()));
            assertThat(order.getCustomerId(), is(expected.getCustomerId()));
            assertThat(order.getItemId(), is(expected.getItemId()));
            assertThat(order.getItemPrice(), is(expected.getItemPrice()));
            assertThat(order.getQuantity(), is(expected.getQuantity()));
            }
        }

    @Test
    public void shouldRunPreLoadApplication()
        {
        Map<Integer, Customer> customers = loadCustomersToDB();
        Map<Integer, Order> orders = loadOrdersToDB();

        LocalPlatform platform = LocalPlatform.get();

        try (CoherenceClusterMember preloader = platform.launch(CoherenceClusterMember.class,
                ClassName.of(PreloadApplication.class),
                WellKnownAddress.of("127.0.0.1"),
                ClusterName.of("preload-test"),
                SystemProperty.of("jdbc.url", hsqldbRunner.getJdbcURL()),
                IPv4Preferred.yes(),
                LocalHost.only(),
                LocalStorage.disabled(),
                RoleName.of("preloader"),
                                                                testLogs,
                DisplayName.of("preload")))
            {
            preloader.waitFor(Timeout.after(5, TimeUnit.MINUTES));

            assertThat(customersCache.size(), is(customers.size()));
            assertThat(ordersCache.size(), is(orders.size()));
            }

        }

    // ----- data members ---------------------------------------------------

    private static Session session;

    private static NamedCache<Integer, Customer> customersCache;

    private static NamedCache<Integer, Order> ordersCache;

    /**
     * A JUnit 5 extension that runs a Coherence cluster.
     */
    @RegisterExtension
    static final CoherenceClusterExtension clusterRunner = new CoherenceClusterExtension()
            .with(WellKnownAddress.of("127.0.0.1"),
                    ClusterName.of("preload-test"),
                    IPv4Preferred.yes(),
                    LocalHost.only())
            .include(3, CoherenceClusterMember.class,
                     LocalStorage.enabled(),
                     testLogs,
                     RoleName.of("storage"),
                     DisplayName.of("storage"));
    }

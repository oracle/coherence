/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.cachestore;

import com.oracle.coherence.guides.preload.model.Customer;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SimpleControllerIT
    {
    @BeforeAll
    static void startCoherence() throws Exception
        {
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.cluster", "SimpleControllerIT");

        SessionConfiguration sessionConfiguration = SessionConfiguration.create("controllable-cachestore-cache-config.xml");
        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                .withSession(sessionConfiguration)
                .build();

        coherence = Coherence.clusterMember(configuration).start().get(5, TimeUnit.MINUTES);
        }

    @AfterAll
    static void shutdownCoherence()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldControlCustomersCacheStore() throws Exception
        {
        Session session = coherence.getSession();
        NamedMap<Integer, Customer> customersMap = session.getMap("customers");

        assertThat(IsCacheStoreEnabled.isEnabled(customersMap), is(true));
        SimpleController.setEnabled(customersMap, false);
        assertThat(IsCacheStoreEnabled.isEnabled(customersMap), is(false));
        SimpleController.setEnabled(customersMap, true);
        assertThat(IsCacheStoreEnabled.isEnabled(customersMap), is(true));
        }

    protected static Coherence coherence;
    }

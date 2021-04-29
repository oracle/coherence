/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.graphql;


import com.oracle.coherence.tutorials.graphql.model.Customer;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import io.helidon.microprofile.cdi.Main;

import io.helidon.microprofile.tests.junit5.HelidonTest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Integration test for the GraphQL initial example.
 *
 * @author Tim Middleton 2020.01.29
 */
@HelidonTest
public class GraphQLInitialIT {

    @BeforeAll
    public static void startup() {
        Main.main(new String[0]);
    }

    @AfterAll
    public static void shutdown() {
        Main.shutdown();
    }

    @Test
    public void testCache() {
        Coherence coherence = Coherence.getInstance();
        if (coherence == null) {
            Coherence.clusterMember().start().join();
            coherence = Coherence.getInstance();
        }
        Session session = coherence.getSession();
        
        NamedMap<Integer, Customer> customers = session.getMap("customers");
        NamedMap<Integer, Customer> orders    = session.getMap("orders");
        assertEquals(4, customers.size());
        assertEquals(5, orders.size());
    }
}

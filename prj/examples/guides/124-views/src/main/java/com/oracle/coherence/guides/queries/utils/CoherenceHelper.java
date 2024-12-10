/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.queries.utils;

import com.oracle.coherence.guides.queries.model.Country;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import java.util.concurrent.CompletableFuture;

/**
 * Helper class to start Coherence and to populate demo data.
 *
 * @author Gunnar Hillert  2022.02.25
 */
public abstract class CoherenceHelper {

    // # tag::bootstrap[]
    public static void startCoherence() {
        Coherence coherence = Coherence.clusterMember(); // <1>
        CompletableFuture<Coherence> future = coherence.start(); // <2>
        future.join(); // <3>

        Session session = coherence.getSession(); // <4>
        NamedCache<String, Country> countries = session.getCache("countries"); // <5>

        countries.put("de", new Country("Germany", "Berlin", 83.2)); // <6>
        countries.put("fr", new Country("France", "Paris", 67.4));
        countries.put("ua", new Country("Ukraine", "Kyiv", 41.2));
        countries.put("co", new Country("Colombia", "Bogotá", 50.4));
        countries.put("au", new Country("Australia", "Canberra", 26));
    }
    // # end::bootstrap[]
}

/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.entryprocessors.utils;

import com.tangosol.net.Coherence;

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
    }
    // # end::bootstrap[]
}

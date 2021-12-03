/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.net.Coherence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Tests for {@link AsyncRemoteAtomicLong} using POF.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class AsyncRemoteAtomicLongPofTest
        extends AsyncAtomicLongTest
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void startUp()
        {
        Coherence.clusterMember().start().join();
        System.setProperty("coherence.serializer", "pof");
        System.setProperty("coherence.pof.config", "coherence-concurrent-pof-config.xml");
        }

    @AfterAll
    static void shutDown()
        {
        Coherence.closeAll();
        System.clearProperty("coherence.serializer");
        System.clearProperty("coherence.pof.config");
        }

    // ----- AsyncAtomicLongTest --------------------------------------------

    @Override
    protected AsyncAtomicLong asyncValue()
        {
        return Atomics.remoteAtomicLong("value").async();
        }
    }

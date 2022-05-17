/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicLong;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicLong;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.tangosol.net.Coherence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Tests for {@link AsyncRemoteAtomicLong}.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class AsyncRemoteAtomicLongTest
        extends AsyncAtomicLongTest
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void startUp()
        {
        Coherence.clusterMember().start().join();
        }

    @AfterAll
    static void shutDown()
        {
        Coherence.closeAll();
        }

    // ----- AsyncAtomicLongTest --------------------------------------------

    @Override
    protected AsyncAtomicLong asyncValue()
        {
        return Atomics.remoteAtomicLong("value").async();
        }
    }

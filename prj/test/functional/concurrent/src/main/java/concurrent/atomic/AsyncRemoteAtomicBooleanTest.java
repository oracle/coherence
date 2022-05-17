/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.tangosol.net.Coherence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Tests for {@link AsyncRemoteAtomicBoolean}.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class AsyncRemoteAtomicBooleanTest
        extends AsyncAtomicBooleanTest
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

    // ----- AsyncAtomicBooleanTest methods ---------------------------------

    @Override
    protected AsyncAtomicBoolean asyncValue()
        {
        return Atomics.remoteAtomicBoolean("value").async();
        }
    }

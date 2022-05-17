/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicMarkableReference;
import com.tangosol.net.Coherence;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Tests for {@link RemoteAtomicMarkableReference}.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public class RemoteAtomicMarkableReferenceTest
        extends AtomicMarkableReferenceTest
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

    // ----- AtomicMarkableReferenceTest methods ----------------------------

    @Override
    protected AtomicMarkableReference<String> value()
        {
        return Atomics.remoteAtomicMarkableReference("value");
        }
    }

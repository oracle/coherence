/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.LocalAtomicMarkableReference;

/**
 * Tests for {@link LocalAtomicMarkableReference}.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public class LocalAtomicMarkableReferenceTest
        extends AtomicMarkableReferenceTest
    {
    // ----- AtomicMarkableReferenceTest methods ----------------------------

    @Override
    protected AtomicMarkableReference<String> value()
        {
        return Atomics.localAtomicMarkableReference("value");
        }
    }

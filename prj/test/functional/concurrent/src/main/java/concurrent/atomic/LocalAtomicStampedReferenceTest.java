/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.LocalAtomicStampedReference;

/**
 * Tests for {@link LocalAtomicStampedReference}.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public class LocalAtomicStampedReferenceTest
        extends AtomicStampedReferenceTest
    {
    // ----- AtomicStampedReferenceTest methods -----------------------------

    @Override
    protected AtomicStampedReference<String> value()
        {
        return Atomics.localAtomicStampedReference("value");
        }
    }

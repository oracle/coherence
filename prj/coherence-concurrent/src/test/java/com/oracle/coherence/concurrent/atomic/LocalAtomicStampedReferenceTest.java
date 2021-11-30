/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

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

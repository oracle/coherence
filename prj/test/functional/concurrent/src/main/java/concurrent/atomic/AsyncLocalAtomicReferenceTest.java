/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicReference;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.LocalAtomicReference;

/**
 * Tests for {@link AsyncLocalAtomicReference}.
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
public class AsyncLocalAtomicReferenceTest
        extends AsyncAtomicReferenceTest
    {
    // ----- AsyncAtomicReferenceTest methods -------------------------------

    @Override
    protected AsyncAtomicReference<String> asyncValue()
        {
        LocalAtomicReference<String> ref = Atomics.localAtomicReference("value");
        return ref.async();
        }
    }

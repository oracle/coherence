/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicInteger;
import com.oracle.coherence.concurrent.atomic.Atomics;

/**
 * Tests for {@link AsyncLocalAtomicInteger}.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class AsyncLocalAtomicIntegerTest
        extends AsyncAtomicIntegerTest
    {
    // ----- AsyncAtomicIntegerTest methods ---------------------------------

    @Override
    protected AsyncAtomicInteger asyncValue()
        {
        return Atomics.localAtomicInteger("value").async();
        }
    }

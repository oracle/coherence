/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.Atomics;

/**
 * Tests for {@link AsyncLocalAtomicBoolean}.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class AsyncLocalAtomicBooleanTest
        extends AsyncAtomicBooleanTest
    {
    // ----- AsyncAtomicBooleanTest methods ---------------------------------

    @Override
    protected AsyncAtomicBoolean asyncValue()
        {
        return Atomics.localAtomicBoolean("value").async();
        }
    }

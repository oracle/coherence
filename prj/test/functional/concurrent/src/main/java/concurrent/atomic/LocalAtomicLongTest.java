/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AtomicLong;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.LocalAtomicLong;

/**
 * Tests for {@link LocalAtomicLong}.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class LocalAtomicLongTest
        extends AtomicLongTest
    {
    // ----- AtomicLongTest methods -----------------------------------------

    @Override
    protected AtomicLong value()
        {
        return Atomics.localAtomicLong("value");
        }
    }

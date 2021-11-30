/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

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

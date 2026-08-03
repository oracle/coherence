/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

/**
 * Test support for {@link CoherenceMode}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public final class CoherenceModeTestSupport
    {
    /**
     * Reset the memoized mode.
     */
    public static void reset()
        {
        CoherenceMode.resetForTesting();
        }

    private CoherenceModeTestSupport()
        {
        }
    }

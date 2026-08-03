/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle.security.type;

import com.tangosol.util.function.Remote;

/**
 * Non-recursive type fixture.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
@Remote.Allowed(recursive = false)
public class NonRecursiveAllowedType
    {
    /**
     * Nested type fixture.
     */
    public static class Nested
        {
        }
    }

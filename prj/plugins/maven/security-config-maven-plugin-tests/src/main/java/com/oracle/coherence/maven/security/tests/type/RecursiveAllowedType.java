/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.security.tests.type;

import com.tangosol.util.function.Remote;

/**
 * Recursive type fixture.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
@Remote.Allowed
public class RecursiveAllowedType
    {
    /**
     * Nested type fixture.
     */
    public static class Nested
        {
        }
    }

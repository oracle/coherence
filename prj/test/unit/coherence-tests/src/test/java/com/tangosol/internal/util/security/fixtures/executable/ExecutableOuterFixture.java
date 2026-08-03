/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security.fixtures.executable;

import com.tangosol.util.function.Remote;

/**
 * Nested executable security-config generator fixture.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public class ExecutableOuterFixture
    {
    @Remote.Executable
    public static class Nested
        {
        }
    }

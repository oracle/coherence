/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle.security.executable;

import com.tangosol.util.function.Remote;

/**
 * Allowed and executable Gradle plugin fixture.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
@Remote.Allowed
@Remote.Executable
public class AllowedExecutableThing
    {
    }

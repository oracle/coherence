/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.security.tests.lambdatarget;

import com.tangosol.util.function.Remote;

/**
 * Abstract executable fixture.
 *
 * @author Aleks Seovic  2026.05.02
 * @since 26.04
 */
@Remote.Executable
public abstract class AbstractExecutableThing
    {
    /**
     * Execute the fixture.
     */
    public abstract void run();
    }

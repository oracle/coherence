/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.security.tests.lambdatarget;

import com.tangosol.util.function.Remote;

import java.io.Serializable;

/**
 * Local SAM lambda target fixture.
 *
 * @author Aleks Seovic  2026.05.02
 * @since 26.07
 */
@Remote.Executable
@FunctionalInterface
public interface LocalLambdaTargetThing
        extends Serializable
    {
    /**
     * Apply this function.
     *
     * @param value  the input value
     *
     * @return the output value
     */
    Object apply(Object value);
    }

/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle.security.lambdatarget;

import com.tangosol.util.function.Remote;

/**
 * Fixture intentionally missing {@code java.io.Serializable}.
 *
 * @author Aleks Seovic  2026.05.02
 * @since 26.04
 */
@Remote.Executable
@FunctionalInterface
public interface NonSerializableLambdaTargetThing
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

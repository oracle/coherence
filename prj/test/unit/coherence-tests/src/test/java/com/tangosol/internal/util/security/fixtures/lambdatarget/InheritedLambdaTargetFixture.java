/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security.fixtures.lambdatarget;

import com.tangosol.util.function.Remote;

/**
 * Lambda-target fixture whose SAM is inherited from a JDK functional interface.
 *
 * @author Aleks Seovic  2026.05.02
 * @since 26.07
 */
@Remote.Executable
@FunctionalInterface
public interface InheritedLambdaTargetFixture<T, R>
        extends java.util.function.Function<T, R>, java.io.Serializable
    {
    }

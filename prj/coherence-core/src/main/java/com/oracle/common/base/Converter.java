/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;


import java.util.function.Function;


/**
 * Provide for "pluggable" object conversions.
 *
 * @param <F> the from type
 * @param <T> the to type
 *
 * @author pm 2000.04.25
 * @deprecated use {@link com.oracle.coherence.common.base.Converter} instead
 */
@Deprecated
@FunctionalInterface
public interface Converter<F, T>
        extends com.oracle.coherence.common.base.Converter<F, T>
    {
    }

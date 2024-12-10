/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


import java.util.function.Function;


/**
 * Provide for "pluggable" object conversions.
 *
 * @param <F> the from type
 * @param <T> the to type
 *
 * @author pm 2000.04.25
 */
@FunctionalInterface
public interface Converter<F, T>
        extends Function<F, T>
    {
    /**
     * Convert the passed object to another object.
     *
     * @param value  the object to convert
     *
     * @return the converted form of the passed object
     *
     * @throws IllegalArgumentException describes a conversion error
     */
    public T convert(F value);

    @Override
    public default T apply(F value)
        {
        return convert(value);
        }
    }

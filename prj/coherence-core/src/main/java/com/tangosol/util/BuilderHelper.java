/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.ParameterResolver;

/**
 * Provides a collection of helper methods for working with {@link Builder}s.
 *
 * @author bo 2012.10.26
 */
public class BuilderHelper
    {
    /**
     * Creates a {@link Builder} that returns a specified object, for each
     * invocation of {@link Builder#realize()}.
     *
     * @param <T>  the type of object to realize
     * @param t    the instance to return
     *
     * @return  a {@link Builder} implementation that returns the specified
     *          instance when {@link Builder#realize()} is invoked
     */
    public static <T> Builder<T> using(final T t)
        {
        return new Builder<T>()
            {
            @Override
            public T realize()
                {
                return t;
                }
            };
        }

    /**
     * Adapts a {@link ParameterizedBuilder} into a {@link Builder}.
     *
     * @param <T>       the type of object to realize
     * @param bldr      the {@link ParameterizedBuilder}
     * @param resolver  the {@link ParameterResolver} for the builder
     * @param loader    the {@link ClassLoader}
     * @param list      (optional) the {@link ParameterList}
     *
     * @return a {@link Builder} that will realize an instance using the
     *         specified {@link ParameterizedBuilder}
     */
    public static <T> Builder<T> using(final ParameterizedBuilder<T> bldr, final ParameterResolver resolver,
                                       final ClassLoader loader, final ParameterList list)
        {
        return new Builder<T>()
            {
            @Override
            public T realize()
                {
                return bldr.realize(resolver, loader, list);
                }
            };
        }
    }

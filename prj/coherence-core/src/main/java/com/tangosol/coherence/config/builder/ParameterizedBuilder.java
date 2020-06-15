/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

/**
 * A {@link ParameterizedBuilder} is an implementation of the classic Builder Pattern that utilizes a
 * {@link ParameterResolver} to resolve any required runtime {@link Parameter}s necessary for realizing an object.
 * <p>
 * {@link ParameterizedBuilder}s are typically used to:
 * <p>
 * 1. encapsulate the ability to dynamically configure the mechanism to realize/create/construct/resolve objects of a
 * required type at runtime, typically based on some externally defined and parameterized configuration.
 * <p>
 * 2. allow developers to postpone the creation of required objects at runtime, thus allowing lazy initialization of
 * system components.
 *
 * @author bo  2011.06.23
 * @since Coherence 12.1.2
 */
public interface ParameterizedBuilder<T>
    {
    /**
     * Realizes (creates if necessary) an instance of a object of type T, using the provided {@link ParameterResolver}
     * to resolve values any referenced {@link Parameter}s.
     *
     * @param resolver        the {@link ParameterResolver} for resolving named {@link Parameter}s
     * @param loader          the {@link ClassLoader} for loading any necessary classes and if <code>null</code> the
     *                        {@link ClassLoader} used to load the builder will be used instead
     * @param listParameters  an optional {@link ParameterList} (may be <code>null</code>) to be used for realizing the
     *                        instance, eg: used as constructor parameters
     *
     * @return an instance of T
     */
    public T realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters);

    /**
     * WARNING: Do not use this interface.  It is no longer used internally and this
     * deprecated interface will be removed in the future.
     * <p>
     * A deprecated interface that {@link ParameterizedBuilder}s may implement
     * to provide runtime type information about the type of objects that may be built.
     *
     * @since 12.1.3
     * @see ParameterizedBuilderHelper#realizes(ParameterizedBuilder, Class, ParameterResolver, ClassLoader)
     */
    @Deprecated
    public interface ReflectionSupport
        {
        /**
         * Determines if the {@link ParameterizedBuilder} will realize an instance of the specified class (without
         * requiring the builder to actually realize an object).
         * <p>
         * This method is synonymous with the Java keyword <code>instanceof</code> but allows dynamic runtime type
         * querying of the types of objects a builder may realize.
         *
         * @param clzClass  the expected type
         * @param resolver  the {@link ParameterResolver} to use for resolving necessary {@link Parameter}s
         * @param loader    the {@link ClassLoader} for loading any necessary classes and if <code>null</code> the
         *                  {@link ClassLoader} used to load the builder will be used instead
         *
         * @return <code>true</code> if the {@link ParameterizedBuilder} will realize an instance of the class,
         *         <code>false</code> otherwise
         */
        public boolean realizes(Class<?> clzClass, ParameterResolver resolver, ClassLoader loader);
        }
    }

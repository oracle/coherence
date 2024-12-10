/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCollection;
import com.tangosol.net.ValueTypeAssertion;

import java.util.Map;

/**
 * A {@link NamedCollectionBuilder} realizes {@link NamedCollection}s.
 *
 * @author jk 2015.06.27
 * @since Coherence 14.1.1
 */
public interface NamedCollectionBuilder<C extends NamedCollection>
    {
    /**
     * Realizes a {@link NamedCollection} (possibly "ensuring it") based on the state
     * of the builder, the provided {@link ParameterResolver} and {@link MapBuilder}
     * dependencies.
     * <p>
     * The {@link MapBuilder} dependencies are required to satisfy the requirement
     * when realizing a {@link NamedCollection} additionally involves realizing one
     * or more internal {@link Map}s.
     *
     * @param typeConstraint  type constraint assertion for elements of this {@link NamedCollection}
     * @param resolver        the ParameterResolver
     * @param dependencies    the {@link MapBuilder} dependencies
     *
     * @param <E>  the element type of {@link NamedCollection}
     *
     * @return a {@link NamedCollection}
     */
    public  <E> C realize(
            ValueTypeAssertion<E> typeConstraint,
            ParameterResolver resolver, MapBuilder.Dependencies dependencies);

    /**
     * Determines whether this {@link NamedCollectionBuilder} can realize a
     * {@link NamedCollection} of the specified type.
     *
     * @param type  the {@link Class} of the type to verify
     * @param <T>   the type of the class to verify
     *
     * @return true if this builder can realize a {@link NamedCollection} of the
     *         specified type.
     */
    public <T extends NamedCollection> boolean realizes(Class<T> type);
    }

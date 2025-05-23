/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedCollectionBuilder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCollection;

import com.tangosol.net.topic.TopicDependencies;

/**
 * The {@link TopicScheme} class is responsible for building a fully
 * configured instance of a topic.
 *
 * @author jk 2015.06.27
 * @since Coherence 14.1.1
 */
public interface TopicScheme<C extends NamedCollection, S>
        extends NamedCollectionBuilder<C>, ServiceScheme
    {
    /**
     * Obtain a configured topic service.
     *
     * @param resolver  the {@link ParameterResolver} to use to resolve the service parameters
     * @param deps      the dependencies to use to configure the service
     *
     * @return  a configured topic service
     */
    public S ensureConfiguredService(ParameterResolver resolver, MapBuilder.Dependencies deps);

    /**
     * Create a {@link TopicDependencies} based on the values contained in this scheme.
     *
     * @param resolver  the {@link ParameterResolver} to use to resolve configuration values
     * @param loader    the {@link ClassLoader} to use
     *
     * @return  a {@link TopicDependencies} based on the values contained in this scheme
     */
    public TopicDependencies createConfiguration(ParameterResolver resolver, ClassLoader loader);
    }

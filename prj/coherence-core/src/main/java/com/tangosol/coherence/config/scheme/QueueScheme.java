/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedCollectionBuilder;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedQueue;

/**
 * The {@link QueueScheme} class is responsible for building a fully
 * configured instance of a queue.
 */
@SuppressWarnings("rawtypes")
public interface QueueScheme<C extends NamedQueue, S>
        extends NamedCollectionBuilder<C>, ServiceScheme
    {
    /**
     * Obtain a configured queue service.
     *
     * @param resolver  the {@link ParameterResolver} to use to resolve the service parameters
     * @param deps      the dependencies to use to configure the service
     *
     * @return  a configured queue service
     */
    public S ensureConfiguredService(ParameterResolver resolver, MapBuilder.Dependencies deps);
    }

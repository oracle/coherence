/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.tangosol.coherence.config.scheme.NamedTopicScheme;
import com.tangosol.coherence.config.scheme.PagedTopicScheme;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

/**
 * A base class for topic backing map managers.
 *
 * @param <D>  the type of the {@link TopicDependencies} used
 * @param <S>  the type of the {@link NamedTopicScheme} defining the topic
 *
 * @author Jonathan Knight 2002.09.10
 * @since 22.09
 */
public abstract class TopicBackingMapManager<D extends TopicDependencies, S extends NamedTopicScheme>
        extends ExtensibleConfigurableCacheFactory.Manager
    {
    /**
     * Create a {@link TopicBackingMapManager}.
     *
     * @param eccf  the owning {@link ExtensibleConfigurableCacheFactory}
     */
    protected TopicBackingMapManager(ExtensibleConfigurableCacheFactory eccf)
        {
        super(eccf);
        }

    /**
     * Find the {@link NamedTopicScheme} that defines a topic.
     *
     * @param sName  the name of the topic
     *
     * @return the {@link NamedTopicScheme} that defines the topic
     */
    public abstract S findTopicScheme(String sName);

    /**
     * Get the {@link TopicDependencies} for a topic.
     *
     * @param sName  the name of the topic
     *
     * @return the {@link TopicDependencies} for the topic
     */
    public abstract D getTopicDependencies(String sName);
    }

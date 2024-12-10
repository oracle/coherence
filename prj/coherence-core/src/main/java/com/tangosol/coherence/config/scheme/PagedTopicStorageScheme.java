/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCache;

import java.util.Map;

/**
 * A scheme that builds the inner scheme of the backing map scheme of a topic.
 *
 * @author jk 2015.05.29
 * @since Coherence 14.1.1
 */
public class PagedTopicStorageScheme
        extends WrapperCachingScheme
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicStorageScheme}.
     *
     * @param schemeStorage  the {@link CachingScheme} defining the storage scheme for the topic
     * @param topicScheme    the {@link PagedTopicScheme} defining the topic
     */
    public PagedTopicStorageScheme(CachingScheme schemeStorage, PagedTopicScheme topicScheme)
        {
        super(schemeStorage);
        f_schemeTopic = topicScheme;
        }

    // ----- CachingScheme methods ------------------------------------------

    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        f_schemeTopic.ensureConfiguredService(resolver, dependencies);

        return super.realizeMap(resolver, dependencies);
        }

    @Override
    public NamedCache realizeCache(ParameterResolver resolver, Dependencies dependencies)
        {
        f_schemeTopic.ensureConfiguredService(resolver, dependencies);

        return super.realizeCache(resolver, dependencies);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link PagedTopicStorageScheme} defining the topic configuration.
     */
    private final PagedTopicScheme f_schemeTopic;
    }

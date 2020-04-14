/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.util.ArrayList;

/**
 * A {@link ChainedResourceResolver} is a {@link ResourceResolver} that
 * chains together one or more other {@link ResourceResolver}s, those of which
 * will be queried (in the order in which they are added to the {@link ChainedResourceResolver})
 * when attempting to resolve a resource.
 *
 * @author bo 2012.09.21
 * @since Coherence 12.1.2
 */
public class ChainedResourceResolver
        implements ResourceResolver
    {
    /**
     * Constructs a {@link ChainedResourceResolver}.
     *
     * @param resourceResolvers  the {@link ResourceResolver}s to query
     */
    public ChainedResourceResolver(ResourceResolver... resourceResolvers)
        {
        m_aResourceResolvers = new ArrayList<ResourceResolver>();

        for (ResourceResolver resolver : resourceResolvers)
            {
            m_aResourceResolvers.add(resolver);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R getResource(Class<R> clsResource)
        {
        for (ResourceResolver resolver : m_aResourceResolvers)
            {
            R resource = resolver.getResource(clsResource);

            if (resource != null)
                {
                return resource;
                }
            }

        return null;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> R getResource(Class<R> clsResource, String sResourceName)
        {
        for (ResourceResolver resolver : m_aResourceResolvers)
            {
            R resource = resolver.getResource(clsResource, sResourceName);

            if (resource != null)
                {
                return resource;
                }
            }

        return null;
        }

    /**
     * The list of {@link ResourceResolver}s to query in order
     * when attempting to resolve a resource
     */
    private ArrayList<ResourceResolver> m_aResourceResolvers;
    }

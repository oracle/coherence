/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.rest.config.DirectQuery;
import com.tangosol.coherence.rest.config.QueryConfig;
import com.tangosol.coherence.rest.config.ResourceConfig;

import com.tangosol.coherence.rest.util.StaticContent;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * An alternate {@link ResourceConfig} implementation that supports pass-through
 * access to all the caches defined by the cache configuration.
 *
 * @author as  2015.09.07
 * @since 12.2.1
 */
@Path("/")
public class PassThroughRootResource
        extends DefaultRootResource
    {
    /**
     * Returns a resource representing a single named cache.
     *
     * @param sName   resource name
     *
     * @return resource representing a single named cache
     */
    @Override
    @Path("{name}")
    public CacheResource getCacheResource(@PathParam("name") String sName)
        {
        ResourceConfig configResource = m_config.getResources().get(sName);
        if (configResource == null)
            {
            // register pass-through resource for the specified cache name
            configResource = new ResourceConfig();
            configResource.setCacheName(sName);
            configResource.setKeyClass(String.class);
            configResource.setValueClass(StaticContent.class);
            configResource.setQueryConfig(new QueryConfig().setDirectQuery(new DirectQuery(Integer.MAX_VALUE)));
            configResource.setMaxResults(Integer.MAX_VALUE);

            m_config.getResources().put(sName, configResource);
            Logger.info("Configured pass-through resource for cache: " + sName);
            }

        return instantiateCacheResourceInternal(configResource);
        }
    }

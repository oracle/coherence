/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.rest.config.QueryConfig;
import com.tangosol.coherence.rest.config.ResourceConfig;
import com.tangosol.coherence.rest.config.RestConfig;

import com.tangosol.coherence.rest.server.InjectionBinder;

import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import javax.inject.Inject;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.glassfish.hk2.api.ServiceLocator;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

/**
 * Default root resource implementation.
 * <p>
 * This class can be replaced by a custom implementation if the user wants to
 * have more control over the mapping of URLs to caches.
 * <p>
 * This implementation simply maps the first path element of the URL to a
 * resource with the same name (as defined in <tt>coherence-rest-config.xml</tt>)
 * and returns a {@link CacheResource} wrapper around it.
 * <p>
 * If the cache with a specified name does not exist, a 404 (Not Found)
 * status code will be returned in the response.
 *
 * @author as  2011.06.03
 */
@Path("/")
public class DefaultRootResource
    {
    /**
     * Returns a resource representing single named cache.
     *
     * @param sName  resource name
     *
     * @return resource representing single named cache
     */
    @Path("{name}")
    public CacheResource getCacheResource(@PathParam("name") String sName)
        {
        ResourceConfig configResource = m_config.getResources().get(sName);
        if (configResource == null)
            {
            throw new NotFoundException("There is no resource configured by that name.");
            }

        return instantiateCacheResourceInternal(configResource);
        }

    /**
     * Create an instance of {@link CacheResource} for the specified resource
     * configuration.
     * <p>
     * This is an internal method and is not intended to be overridden by the
     * users. The users should override {@link #instantiateCacheResource}
     * method instead.
     *
     * @param configResource  the resource configuration
     *
     * @return a fully configured cache resource
     */
    protected CacheResource instantiateCacheResourceInternal(ResourceConfig configResource)
        {
        try
            {
            NamedCache cache = m_session.getCache(configResource.getCacheName(),
                                                  withoutTypeChecking());
            return InjectionBinder.inject(
                    instantiateCacheResource(cache,
                                            configResource.getKeyClass(),
                                            configResource.getValueClass(),
                                            configResource.getKeyConverter(),
                                            configResource.getQueryConfig(),
                                            configResource.getMaxResults()),
                    m_serviceLocator);
            }
        catch (Exception e)
            {
            Logger.err(e);
            throw new NotFoundException(e.getMessage());
            }
        }

    /**
     * Create an instance of {@link CacheResource}.
     *
     * @param cache         cache to create a resource for
     * @param clzKey        key class of the cached entries
     * @param clzValue      value class of the cached entries
     * @param keyConverter  key converter to use
     * @param queryConfig   query configuration for this resource
     * @param cMaxResults   max size of result set for this resource
     *
     * @return a cache resource
     */
    protected CacheResource instantiateCacheResource(NamedCache cache, Class clzKey, Class clzValue,
            KeyConverter keyConverter, QueryConfig queryConfig, int cMaxResults)
        {
        return new CacheResource(cache, clzKey, clzValue, keyConverter, queryConfig, cMaxResults);
        }

    // ---- data members ----------------------------------------------------

    /**
     * Coherence session.
     */
    @Inject
    protected Session m_session;

    /**
     * REST configuration.
     */
    @Inject
    protected RestConfig m_config;

    /*
     * Service locator.
     */
    @Inject
    protected ServiceLocator m_serviceLocator;
    }

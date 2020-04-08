/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.util.Enumeration;


/**
* CacheService implementation that delegates to a wrapped CacheService
* instance.
*
* @author jh  2010.03.17
*/
public class WrapperCacheService
        extends WrapperService
        implements CacheService
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new WrapperCacheService that delegates to the given
    * CacheService instance.
    *
    * @param service the CacheService to wrap
    */
    public WrapperCacheService(CacheService service)
        {
        super(service);
        }


    // ----- CacheService interface -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public BackingMapManager getBackingMapManager()
        {
        return getCacheService().getBackingMapManager();
        }

    /**
    * {@inheritDoc}
    */
    public void setBackingMapManager(BackingMapManager manager)
        {
        getCacheService().setBackingMapManager(manager);
        }

    /**
    * {@inheritDoc}
    */
    public NamedCache ensureCache(String sName, ClassLoader loader)
        {
        return getCacheService().ensureCache(sName, loader);
        }

    /**
    * {@inheritDoc}
    */
    public Enumeration getCacheNames()
        {
        return getCacheService().getCacheNames();
        }

    /**
    * {@inheritDoc}
    */
    public void releaseCache(NamedCache map)
        {
        getCacheService().releaseCache(map);
        }

    /**
    * {@inheritDoc}
    */
    public void destroyCache(NamedCache map)
        {
        getCacheService().destroyCache(map);
        }

    /**
    * {@inheritDoc}
    */
    public void setDependencies(ServiceDependencies deps)
        {
        getCacheService().setDependencies(deps);
        }

    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "WrapperCacheService{" + getCacheService() + '}';
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the wrapped CacheService.
    *
    * @return  the wrapped CacheService
    */
    public CacheService getCacheService()
        {
        return (CacheService) getService();
        }
    }

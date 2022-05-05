/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.WrapperCacheService;


/**
* Example WrapperCacheService that demonstrates how entitlements can be
* applied to a wrapped CacheService using the Subject passed from the
* client via Coherence*Extend. This implementation only allows clients with
* a specified Principal name to access the wrapped CacheService.
*
* @author jh  2010.04.06
*/
public class EntitledCacheService
        extends WrapperCacheService
    {
    /**
    * Create a new EntitledCacheService.
    *
    * @param service     the wrapped CacheService
    * @param sPrincipal  the name of the Principal that is allowed to access
    *                    the wrapped CacheService
    */
    public EntitledCacheService(CacheService service, String sPrincipal)
        {
        super(service);

        if (sPrincipal == null || sPrincipal.length() == 0)
            {
            throw new IllegalArgumentException("Principal required");
            }
        m_sPrincipal = sPrincipal;
        }


    // ----- CacheService interface -----------------------------------------


    /**
    * {@inheritDoc}
    */
    public NamedCache ensureCache(String sName, ClassLoader loader)
        {
        return new EntitledNamedCache(super.ensureCache(sName, loader),
                getPrincipalName());
        }

    /**
    * {@inheritDoc}
    */
    public void releaseCache(NamedCache map)
        {
        if (map instanceof EntitledNamedCache)
            {
            map = ((EntitledNamedCache) map).getNamedCache();
            }
        super.releaseCache(map);
        }

    /**
    * {@inheritDoc}
    */
    public void destroyCache(NamedCache map)
        {
        if (map instanceof EntitledNamedCache)
            {
            map = ((EntitledNamedCache) map).getNamedCache();
            }
        super.destroyCache(map);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the name of the Principal that is allowed to access the wrapped
    * service.
    *
    * @return the name of the Principal
    */
    public String getPrincipalName()
        {
        return m_sPrincipal;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of the principal that is allowed to access the wrapped
    * service.
    */
    private String m_sPrincipal;
    }

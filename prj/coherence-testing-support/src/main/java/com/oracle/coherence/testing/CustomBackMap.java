/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.util.Base;

import java.util.concurrent.atomic.AtomicInteger;


/**
* A custom back map that wraps WrapperNamedCache class used by the
* NearCacheTests to verify fix of PrimingListener problem (Bug 32415967)
* in near cache.
*
* @author hr, lh  2021.01.27
*/
public class CustomBackMap<K, V>
        extends WrapperNamedCache
    {
    // ----- constructors ---------------------------------------------------

    public CustomBackMap(String sName, String sService)
        {
        super(sName);
        m_service = (((CacheService) CacheFactory.getService(sService)));
        }

    @Override
    public NamedCache<K, V> getMap()
        {
        NamedCache<K, V> map = m_cache;
        if (map == null)
            {
            synchronized (this)
                {
                if (m_cache == null)
                    {
                    map = m_cache = m_service.ensureCache(getName(), getContextClassLoader());
                    }
                }
            }
        return map;
        }

    @Override
    public Object get(Object oKey)
        {
        // If this was called, then we encountered bug 32415967
        Object ret = super.get(oKey);
        f_atomicCounter.incrementAndGet();
        log("get(Object) oKey=" + oKey + ", m_count=" + f_atomicCounter.get() + ", value=" + ret);
        return ret;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public String toString()
        {
        return new String("CustomBackMap(" + f_atomicCounter + ')');
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (o instanceof CustomBackMap)
            {
            CustomBackMap that = (CustomBackMap) o;
            return Base.equals(f_atomicCounter, that.f_atomicCounter);
            }
        return false;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public int hashCode()
        {
        Object o = f_atomicCounter;
        return o == null ? 0: o.hashCode();
        }

    // ----- data members ---------------------------------------------------

    protected volatile NamedCache<K, V> m_cache;

    /**
    * An object.
    */
    public final AtomicInteger f_atomicCounter = new AtomicInteger();
    }

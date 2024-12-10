/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;

import com.tangosol.net.cache.ContinuousQueryCache;

import java.util.Objects;

public class ViewMBeanImpl
        implements ViewMBean
    {
    // ----- constructors -----------------------------------------------

    /**
     * Constructs a {@code ViewMBeanImpl}
     * @param cache the cache
     */
    public ViewMBeanImpl(ContinuousQueryCache cache)
        {
        f_cache = cache;
        }

    // ----- ViewMBean interface ----------------------------------------

    public String getViewName()
        {
        return f_cache.getCacheName();
        }

    public boolean isReadOnly()
        {
        return f_cache.isReadOnly();
        }

    public boolean isTransformed()
        {
        return f_cache.isTransformed();
        }

    public String getFilter()
        {
        return Objects.toString(f_cache.getFilter(), null);
        }

    public String getTransformer()
        {
        return Objects.toString(f_cache.getTransformer(), null);
        }

    public long getReconnectInterval()
        {
        return f_cache.getReconnectInterval();
        }

    public boolean isCacheValues()
        {
        return f_cache.isCacheValues();
        }

    public long getSize()
        {
        return f_cache.size();
        }

    // ----- object methods -------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        ViewMBeanImpl viewMBean = (ViewMBeanImpl) o;

        if (!getViewName().equals(viewMBean.getViewName()))
            {
            return false;
            }
        if (!f_cache.getCacheService().equals(viewMBean.f_cache.getCacheService()))
            {
            return false;
            }
        return f_cache.equals(viewMBean.f_cache);
        }

    public int hashCode()
        {
        int result = getViewName().hashCode();
        result = 31 * result + f_cache.getCacheService().hashCode();
        result = 31 * result + f_cache.hashCode();
        return result;
        }

    public String toString()
        {
        return "ViewMBeanImpl{" +
               "f_cache=" + f_cache +
               '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The cache instance.
     */
    protected final ContinuousQueryCache f_cache;
    }

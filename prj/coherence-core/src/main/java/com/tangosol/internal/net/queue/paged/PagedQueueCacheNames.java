/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.NamedMap;

public enum PagedQueueCacheNames
    {
    /**
     * The buckets cache name.
     */
    Buckets("$buckets"),
    /**
     * The elements cache name.
     */
    Elements(""),
    /**
     * The versions cache name.
     */
    Version("$versions"),
    /**
     * The info cache name.
     */
    Info("$info"),
    ;

    PagedQueueCacheNames(String sSuffix)
        {
        this.m_sSuffix = sSuffix;
        }

    public String suffix()
        {
        return m_sSuffix;
        }

    public String getCacheName(NamedMap<?, ?> map)
        {
        return getCacheName(map.getName());
        }

    public String getCacheName(BackingMapContext ctx)
        {
        return getCacheName(ctx.getCacheName());
        }

    public String getCacheName(String sCacheName)
        {
        if (sCacheName.endsWith(Buckets.m_sSuffix))
            {
            return sCacheName.substring(0, sCacheName.length() - Buckets.m_sSuffix.length()) + m_sSuffix;
            }
        if (sCacheName.endsWith(Version.m_sSuffix))
            {
            return sCacheName.substring(0, sCacheName.length() - Version.m_sSuffix.length()) + m_sSuffix;
            }
        if (sCacheName.endsWith(Info.m_sSuffix))
            {
            return sCacheName.substring(0, sCacheName.length() - Info.m_sSuffix.length()) + m_sSuffix;
            }
        return sCacheName + m_sSuffix;
        }

    // ----- data members ---------------------------------------------------

    private final String m_sSuffix;

    /**
     * The suffix appended to the buckets cache name to get the name of the
     * cache used to store the queue elements.
     */
    public static final String BUCKETS_CACHE_NAME_SUFFIX = "$buckets";

    /**
     * The suffix appended to the buckets cache name to get the name of the
     * cache used to store the queue elements.
     */
    public static final String ELEMENTS_CACHE_NAME_SUFFIX = "$elements";

    /**
     * The suffix appended to the bucket's cache name to get the name of the
     * cache used to store the Bucket version details.
     */
    public static final String VERSION_CACHE_NAME_SUFFIX = "$versions";

    /**
     * The suffix appended to the bucket's cache name to get the name of the
     * cache used to store the Bucket lock details.
     */
    public static final String LOCKS_CACHE_NAME_SUFFIX = "$locks";

    }

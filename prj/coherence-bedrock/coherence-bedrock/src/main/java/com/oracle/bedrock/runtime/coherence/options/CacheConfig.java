/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.options;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

public class CacheConfig
        implements Profile, Option
    {
    /**
     * The tangosol.coherence.cacheconfig property.
     */
    public static final String PROPERTY = "coherence.cacheconfig";

    /**
     * The cache config uri.
     */
    private String uri;


    /**
     * Constructs a {@link CacheConfig} for the specified uri.
     *
     * @param uri the name
     */
    private CacheConfig(String uri)
        {
        if (uri == null)
            {
            throw new NullPointerException("CacheConfig must not be null");
            }
        else
            {
            this.uri = uri;
            }
        }


    /**
     * Obtains the uri of the {@link CacheConfig}.
     *
     * @return the uri of the {@link CacheConfig}
     */
    public String getUri()
        {
        return uri;
        }


    /**
     * Obtains a {@link CacheConfig} for a specified uri.
     *
     * @param uri the uri of the {@link CacheConfig}
     * @return a {@link CacheConfig} for the specified uri
     */
    public static CacheConfig of(String uri)
        {
        return new CacheConfig(uri);
        }


    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

        if (systemProperties != null)
            {
            optionsByType.add(SystemProperty.of(PROPERTY, uri));
            }
        }


    @Override
    public void onLaunched(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        }


    @Override
    public void onClosing(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        }


    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (!(o instanceof CacheConfig))
            {
            return false;
            }

        CacheConfig executable = (CacheConfig) o;

        return uri.equals(executable.uri);

        }


    @Override
    public int hashCode()
        {
        return uri.hashCode();
        }

    @Override
    public String toString()
        {
        return "CacheConfig(" +
                "uri='" + uri + '\'' +
                ')';
        }
    }

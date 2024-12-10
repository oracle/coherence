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

import java.util.UUID;

public class SiteName
        implements Profile, Option
    {
    /**
     * The tangosol.coherence.site property.
     */
    public static final String PROPERTY = "coherence.site";

    /**
     * The site name of an {@link CoherenceClusterMember}.
     */
    private String name;


    /**
     * Constructs a {@link SiteName} for the specified name.
     *
     * @param name the name
     */
    private SiteName(String name)
        {
        if (name == null)
            {
            this.name = UUID.randomUUID().toString();
            }
        else
            {
            this.name = name;
            }
        }


    /**
     * Obtains the name of the {@link SiteName}.
     *
     * @return the name of the {@link SiteName}
     */
    public String get()
        {
        return name;
        }


    /**
     * Obtains a {@link SiteName} for a specified name.
     *
     * @param name the name of the {@link SiteName}
     * @return a {@link SiteName} for the specified name
     */
    public static SiteName of(String name)
        {
        return new SiteName(name);
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
            optionsByType.add(SystemProperty.of(PROPERTY, name));
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

        if (!(o instanceof SiteName))
            {
            return false;
            }

        SiteName executable = (SiteName) o;

        return name.equals(executable.name);

        }


    @Override
    public int hashCode()
        {
        return name.hashCode();
        }


    @Override
    public String toString()
        {
        return "SiteName('" + name + "')";
        }
    }

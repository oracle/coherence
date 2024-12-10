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

/**
 * An option to set the rack name property.
 *
 * @author Jonathan Knight  2022.05.25
 * @since 22.06
 */
public class RackName
        implements Profile, Option
    {
    /**
     * The tangosol.coherence.rack property.
     */
    public static final String PROPERTY = "coherence.rack";

    /**
     * The rack name of an {@link CoherenceClusterMember}.
     */
    private final String name;


    /**
     * Constructs a {@link RackName} for the specified name.
     *
     * @param name the name
     */
    private RackName(String name)
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
     * Obtains the name of the {@link RackName}.
     *
     * @return the name of the {@link RackName}
     */
    public String get()
        {
        return name;
        }


    /**
     * Obtains a {@link RackName} for a specified name.
     *
     * @param name the name of the {@link RackName}
     * @return a {@link RackName} for the specified name
     */
    public static RackName of(String name)
        {
        return new RackName(name);
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

        if (!(o instanceof RackName))
            {
            return false;
            }

        RackName executable = (RackName) o;

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
        return "RackName('" + name + "')";
        }
    }

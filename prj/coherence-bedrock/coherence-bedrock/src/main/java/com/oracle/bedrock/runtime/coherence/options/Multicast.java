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

public class Multicast
        implements Profile, Option
    {
    /**
     * The tangosol.coherence.ttl property.
     */
    public static final String PROPERTY = "coherence.ttl";

    /**
     * The multicast ttl value {@link CoherenceClusterMember}.
     */
    private int value;


    /**
     * Constructs a {@link Multicast} for the specified value.
     *
     * @param value the value
     */
    private Multicast(int value)
        {
        this.value = value;
        }


    /**
     * Obtains a {@link Multicast} for a specified value.
     *
     * @param value the ttl for the {@link Multicast}
     * @return a {@link Multicast} for the specified name
     */
    public static Multicast ttl(int value)
        {
        return new Multicast(value);
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
            optionsByType.add(SystemProperty.of(PROPERTY, value));
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

        if (!(o instanceof Multicast))
            {
            return false;
            }

        Multicast that = (Multicast) o;

        return value == that.value;

        }


    @Override
    public int hashCode()
        {
        return value;
        }


    @Override
    public String toString()
        {
        return "Multicast(" + value + ')';
        }
    }

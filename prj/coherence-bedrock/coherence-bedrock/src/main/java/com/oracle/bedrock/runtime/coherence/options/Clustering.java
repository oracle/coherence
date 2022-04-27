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

public class Clustering
        implements Profile, Option
    {
    /**
     * The tangosol.coherence.tcmp.enabled property.
     */
    public static final String PROPERTY = "coherence.tcmp.enabled";

    /**
     * Is local storage enabled?
     */
    private boolean enabled;


    /**
     * Constructs a {@link Clustering} for the specified value.
     *
     * @param enabled is local storage enabled?
     */
    private Clustering(boolean enabled)
        {
        this.enabled = enabled;
        }


    /**
     * Determines if {@link Clustering} is enabled.
     *
     * @return <code>true</code> if {@link Clustering} is enabled,
     * <code>false</code> otherwise
     */
    public boolean isEnabled()
        {
        return enabled;
        }


    /**
     * Obtains a {@link Clustering} for a specified value.
     *
     * @param enabled is local storage enabled?
     * @return a {@link Clustering} for the specified value
     */
    public static Clustering enabled(boolean enabled)
        {
        return new Clustering(enabled);
        }


    /**
     * Obtains a {@link Clustering} that is enabled.
     *
     * @return a {@link Clustering} that is enabled
     */
    public static Clustering enabled()
        {
        return new Clustering(true);
        }


    /**
     * Obtains a {@link Clustering} that is disabled.
     *
     * @return a {@link Clustering} that is disabled
     */
    public static Clustering disabled()
        {
        return new Clustering(false);
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
            optionsByType.add(SystemProperty.of(PROPERTY, enabled));
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

        if (!(o instanceof Clustering))
            {
            return false;
            }

        Clustering that = (Clustering) o;

        return enabled == that.enabled;

        }


    @Override
    public int hashCode()
        {
        return (enabled ? 1 : 0);
        }

    @Override
    public String toString()
        {
        return "Clustering(" +
                PROPERTY + "=" + enabled +
                ')';
        }
    }

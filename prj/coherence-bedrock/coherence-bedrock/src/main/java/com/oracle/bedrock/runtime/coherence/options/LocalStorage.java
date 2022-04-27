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

public class LocalStorage
        implements Profile, Option
    {
    /**
     * The tangosol.coherence.distributed.localstorage property.
     */
    public static final String PROPERTY = "coherence.distributed.localstorage";

    /**
     * Is local storage enabled?
     */
    private boolean enabled;


    /**
     * Constructs a {@link LocalStorage} for the specified value.
     *
     * @param enabled is local storage enabled?
     */
    private LocalStorage(boolean enabled)
        {
        this.enabled = enabled;
        }


    /**
     * Determines if {@link LocalStorage} is enabled.
     *
     * @return <code>true</code> if {@link LocalStorage} is enabled,
     * <code>false</code> otherwise
     */
    public boolean isEnabled()
        {
        return enabled;
        }


    /**
     * Obtains a {@link LocalStorage} for a specified value.
     *
     * @param enabled is local storage enabled?
     * @return a {@link LocalStorage} for the specified value
     */
    public static LocalStorage enabled(boolean enabled)
        {
        return new LocalStorage(enabled);
        }


    /**
     * Obtains a {@link LocalStorage} that is enabled.
     *
     * @return a {@link LocalStorage} that is enabled
     */
    public static LocalStorage enabled()
        {
        return new LocalStorage(true);
        }


    /**
     * Obtains a {@link LocalStorage} that is disabled.
     *
     * @return a {@link LocalStorage} that is disabled
     */
    public static LocalStorage disabled()
        {
        return new LocalStorage(false);
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

        if (!(o instanceof LocalStorage))
            {
            return false;
            }

        LocalStorage that = (LocalStorage) o;

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
        return "LocalStorage(" +
                "enabled=" + enabled +
                ')';
        }
    }

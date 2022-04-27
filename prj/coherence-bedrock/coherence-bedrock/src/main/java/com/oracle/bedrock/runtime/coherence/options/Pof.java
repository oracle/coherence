/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.options;

import com.oracle.bedrock.ComposableOption;
import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

public class Pof
        implements Profile, ComposableOption<Pof>
    {
    /**
     * The tangosol.pof.config property.
     */
    public static final String PROPERTY_CONFIG = "tangosol.pof.config";

    /**
     * The tangosol.pof.enabled property.
     */
    public static final String PROPERTY_ENABLED = "tangosol.pof.enabled";

    /**
     * The configuration uri (null if not set).
     */
    private String configUri;

    /**
     * Is POF enabled (null if not set).
     */
    private Boolean enabled;


    /**
     * Constructs a {@link Pof} given the specified parameters.
     *
     * @param configUri the pof configuration (null means use default)
     * @param enabled   is pof enabled (null means use default)
     */
    private Pof(
            String configUri,
            Boolean enabled)
        {
        this.configUri = configUri;
        this.enabled = enabled;
        }


    /**
     * Obtains a {@link Pof} for the specified config uri.
     *
     * @param configUri the uri for the {@link Pof} configuration
     * @return a {@link Pof} for the specified config uri
     */
    public static Pof config(String configUri)
        {
        return new Pof(configUri, null);
        }


    /**
     * Obtains a {@link Pof} based on the specified parameter.
     *
     * @param enabled is {@link Pof} to be enabled?
     * @return a {@link Pof} based on the specified parameter
     */
    public static Pof enabled(boolean enabled)
        {
        return new Pof(null, enabled);
        }


    /**
     * Obtains a {@link Pof} that is disabled.
     *
     * @return a disabled {@link Pof}
     */
    public static Pof disabled()
        {
        return new Pof(null, false);
        }


    /**
     * Obtains a {@link Pof} that is enabled.
     *
     * @return a disabled {@link Pof}
     */
    public static Pof enabled()
        {
        return new Pof(null, true);
        }


    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

        if (systemProperties != null && configUri != null)
            {
            optionsByType.add(SystemProperty.of(PROPERTY_CONFIG, configUri));

            // when a configuration is defined, we automatically enabled pof
            optionsByType.add(SystemProperty.of(PROPERTY_ENABLED, true));
            }

        if (systemProperties != null && enabled != null)
            {
            optionsByType.add(SystemProperty.of(PROPERTY_ENABLED, enabled));
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
    public Pof compose(Pof other)
        {
        return new Pof(this.configUri == null ? other.configUri : this.configUri,
                       this.enabled == null ? other.enabled : this.enabled);
        }


    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (!(o instanceof Pof))
            {
            return false;
            }

        Pof logging = (Pof) o;

        if (configUri != null ? !configUri.equals(logging.configUri) : logging.configUri != null)
            {
            return false;
            }

        return enabled != null ? enabled.equals(logging.enabled) : logging.enabled == null;

        }


    @Override
    public int hashCode()
        {
        int result = configUri != null ? configUri.hashCode() : 0;

        result = 31 * result + (enabled != null ? enabled.hashCode() : 0);

        return result;
        }

    @Override
    public String toString()
        {
        return "Pof(" +
                "configUri='" + configUri + '\'' +
                ", enabled=" + enabled +
                ')';
        }
    }

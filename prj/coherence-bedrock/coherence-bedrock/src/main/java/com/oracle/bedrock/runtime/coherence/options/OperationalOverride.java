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

public class OperationalOverride
        implements Profile, Option
    {
    /**
     * The tangosol.coherence.override property.
     */
    public static final String PROPERTY = "coherence.override";

    /**
     * The operational override uri.
     */
    private String uri;


    /**
     * Constructs a {@link OperationalOverride} for the specified uri.
     *
     * @param uri the uri
     */
    private OperationalOverride(String uri)
        {
        if (uri == null)
            {
            throw new NullPointerException("OperationalOverride must not be null");
            }
        else
            {
            this.uri = uri;
            }
        }


    /**
     * Obtains the uri of the {@link OperationalOverride}.
     *
     * @return the uri of the {@link OperationalOverride}
     */
    public String getUri()
        {
        return uri;
        }


    /**
     * Obtains a {@link OperationalOverride} for a specified uri.
     *
     * @param uri the uri of the {@link OperationalOverride}
     * @return a {@link OperationalOverride} for the specified uri
     */
    public static OperationalOverride of(String uri)
        {
        return new OperationalOverride(uri);
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

        if (!(o instanceof OperationalOverride))
            {
            return false;
            }

        OperationalOverride executable = (OperationalOverride) o;

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
        return "OperationalOverride('" + uri + "')";
        }
    }

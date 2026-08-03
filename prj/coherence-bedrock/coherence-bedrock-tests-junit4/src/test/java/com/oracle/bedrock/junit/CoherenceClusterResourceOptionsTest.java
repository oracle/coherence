/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests default option inheritance for {@link CoherenceClusterResource}.
 *
 * @author as  2026.05.02
 * @since 26.07
 */
public class CoherenceClusterResourceOptionsTest
    {
    @Test
    public void shouldInheritCommandLineCoherenceMode()
        {
        String sMode = System.getProperty(COHERENCE_MODE_PROPERTY);
        try
            {
            System.setProperty(COHERENCE_MODE_PROPERTY, "dev");

            CoherenceClusterResource resource = apply(new CoherenceClusterResource());

            assertThat(systemProperty(resource, COHERENCE_MODE_PROPERTY), is("dev"));
            }
        finally
            {
            restoreProperty(COHERENCE_MODE_PROPERTY, sMode);
            }
        }

    @Test
    public void shouldPreserveExplicitCoherenceMode()
        {
        String sMode = System.getProperty(COHERENCE_MODE_PROPERTY);
        try
            {
            System.setProperty(COHERENCE_MODE_PROPERTY, "dev");

            CoherenceClusterResource resource = apply(new CoherenceClusterResource()
                    .with(SystemProperty.of(COHERENCE_MODE_PROPERTY, "prod")));

            assertThat(systemProperty(resource, COHERENCE_MODE_PROPERTY), is("prod"));
            }
        finally
            {
            restoreProperty(COHERENCE_MODE_PROPERTY, sMode);
            }
        }

    @Test
    public void shouldUseCommandLineClusterNameAsDefault()
        {
        String sClusterName = System.getProperty(ClusterName.PROPERTY);
        try
            {
            System.setProperty(ClusterName.PROPERTY, "command-line-cluster");

            CoherenceClusterResource resource = apply(new CoherenceClusterResource());

            assertThat(clusterName(resource), is("command-line-cluster"));
            }
        finally
            {
            restoreProperty(ClusterName.PROPERTY, sClusterName);
            }
        }

    @Test
    public void shouldInheritCommandLineOptionsWhenStartedImperatively()
        {
        String sMode        = System.getProperty(COHERENCE_MODE_PROPERTY);
        String sClusterName = System.getProperty(ClusterName.PROPERTY);
        try
            {
            System.setProperty(COHERENCE_MODE_PROPERTY, "dev");
            System.setProperty(ClusterName.PROPERTY, "command-line-cluster");

            CoherenceClusterResource resource = new CoherenceClusterResource();
            resource.inheritInvocationProperties(null);

            assertThat(systemProperty(resource, COHERENCE_MODE_PROPERTY), is("dev"));
            assertThat(clusterName(resource), is("command-line-cluster"));
            }
        finally
            {
            restoreProperty(COHERENCE_MODE_PROPERTY, sMode);
            restoreProperty(ClusterName.PROPERTY, sClusterName);
            }
        }

    @Test
    public void shouldPreserveExplicitClusterName()
        {
        String sClusterName = System.getProperty(ClusterName.PROPERTY);
        try
            {
            System.setProperty(ClusterName.PROPERTY, "command-line-cluster");

            CoherenceClusterResource resource = apply(new CoherenceClusterResource()
                    .with(ClusterName.of("explicit-cluster")));

            assertThat(clusterName(resource), is("explicit-cluster"));
            }
        finally
            {
            restoreProperty(ClusterName.PROPERTY, sClusterName);
            }
        }

    private static CoherenceClusterResource apply(CoherenceClusterResource resource)
        {
        resource.apply(new Statement()
            {
            @Override
            public void evaluate()
                {
                }
            }, Description.createTestDescription(CoherenceClusterResourceOptionsTest.class, "test"));
        return resource;
        }

    private static String clusterName(CoherenceClusterResource resource)
        {
        return OptionsByType.of(resource.getCommonOptions()).get(ClusterName.class).getName();
        }

    private static String systemProperty(CoherenceClusterResource resource, String sName)
        {
        SystemProperties properties = OptionsByType.of(resource.getCommonOptions()).get(SystemProperties.class);
        SystemProperty   property   = properties.get(sName);

        return property == null ? null : String.valueOf(property.getValue());
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private static final String COHERENCE_MODE_PROPERTY = "coherence.mode";
    }

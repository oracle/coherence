/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package opentelemetry.core;

import java.util.Properties;

import org.junit.Test;

import tracing.AbstractJmxConfigurationIT;

/**
 * Tests to validate configuration of tracing via JMX.
 *
 * @author rl 9.22.2023
 * @since 24.03
 */
public class JmxConfigurationIT
        extends AbstractJmxConfigurationIT
    {
    // ----- methods from AbstractJmxConfigurationIT ------------------------

    @Override
    protected Properties getDefaultProperties()
        {
        Properties props = super.getDefaultProperties();
        props.setProperty("coherence.wka",     "127.0.0.1");
        props.setProperty("otel.service.name", getClass().getName());

        return props;
        }

    // ----- test methods ---------------------------------------------------

    @Override
    @Test
    public void testDynamicTracingConfigurationOnNode()
        {
        super.testDynamicTracingConfigurationOnNode();
        }

    @Override
    @Test
    public void testDynamicTracingConfigurationClusterWide()
        {
        super.testDynamicTracingConfigurationClusterWide();
        }

    @Override
    @Test
    public void testDynamicTracingConfigurationClusterWideWithRoles()
        {
        super.testDynamicTracingConfigurationClusterWideWithRoles();
        }

    @Test
    @Override
    public void testTracingRatioBounds() throws Exception
        {
        super.testTracingRatioBounds();
        }
    }

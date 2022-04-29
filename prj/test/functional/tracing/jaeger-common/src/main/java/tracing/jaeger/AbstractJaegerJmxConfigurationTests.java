/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tracing.jaeger;

import java.util.Properties;

import org.junit.Test;
import tracing.AbstractJmxConfigurationTests;

/**
 * Run JMX configuration tests with Jaeger enabled.
 */
public abstract class AbstractJaegerJmxConfigurationTests
        extends AbstractJmxConfigurationTests
    {
    // ----- methods from AbstractTracingTest -------------------------------

    @Override
    protected Properties getDefaultProperties()
        {
        Properties props = super.getDefaultProperties();
        props.setProperty(JaegerConfigProperties.SERVICE_NAME.toString(), getClass().getName());
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

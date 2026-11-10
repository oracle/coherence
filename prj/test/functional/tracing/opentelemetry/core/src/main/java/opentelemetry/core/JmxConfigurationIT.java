/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package opentelemetry.core;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
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
    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void startCollector()
        {
        s_collectorServer = TestingUtils.createMockServer(0);
        }

    @AfterClass
    public static void stopCollector()
        {
        if (s_collectorServer != null)
            {
            s_collectorServer.stop();
            s_collectorServer = null;
            }
        }

    // ----- methods from AbstractJmxConfigurationIT ------------------------

    @Override
    protected Properties getDefaultProperties()
        {
        Properties props = super.getDefaultProperties();
        props.setProperty("coherence.wka",     "127.0.0.1");
        props.setProperty("otel.service.name", getClass().getName());

        // a recording tracer requires a span processor with OpenTelemetry 1.67+
        props.setProperty("otel.traces.exporter",        "otlp");
        props.setProperty("otel.exporter.otlp.protocol", "http/protobuf");
        props.setProperty("otel.exporter.otlp.endpoint", "http://127.0.0.1:" + s_collectorServer.port());

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

    // ----- data members ---------------------------------------------------

    private static WireMockServer s_collectorServer;
    }

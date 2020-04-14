/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.metrics.internal;

import org.glassfish.jersey.jackson.JacksonFeature;

import org.glassfish.jersey.message.GZipEncoder;

import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.EncodingFilter;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * An alternate {@link ResourceConfig} implementation that supports
 * Prometheus metrics requests.
 *
 * @author jf  2018.07.25
 * @since 12.2.1.4.0
 */
@ApplicationPath("/metrics")
public class MetricsResourceConfig
        extends ResourceConfig
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public MetricsResourceConfig()
        {
        register(JacksonFeature.class);
        register(MetricsWriter.class);
        EncodingFilter.enableFor(this, GZipEncoder.class);
        register(MetricsResource.class);

        // This allows the type of metrics to be requested by adding a suffix to the url
        // for example http://localhost:8000/metrics.json will return json
        Map<String, MediaType> mediaTypeMappings = new HashMap<>();
        mediaTypeMappings.put("txt", MediaType.TEXT_PLAIN_TYPE);
        mediaTypeMappings.put("json", MediaType.APPLICATION_JSON_TYPE);

        property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypeMappings);
        }
    }

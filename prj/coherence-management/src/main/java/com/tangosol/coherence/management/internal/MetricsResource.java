/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import com.tangosol.internal.metrics.MetricsFormatter;
import com.tangosol.internal.metrics.MetricsHttpHandler;

import javax.annotation.security.PermitAll;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A JAX-RS resource that supports metrics requests for managed Coherence server.
 *
 * @author jk, lsho 2024.07.18
 * @since 14.1.2.0.0
 */
@PermitAll
@Path("coherence/metrics")
public class MetricsResource
        extends MetricsHttpHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a MetricsResource.
     * <p>
     * This constructor will be used by JAX-RS to create the resource instance.
     * <p>
     * The {@code coherence.metrics.legacy.names} system property will be used to
     * determine whether legacy metric names ot Microprofile compatible metric
     * names will be used when publishing Prometheus formatted metrics.
     */
    public MetricsResource()
        {
        super();
        }

    /**
     * Create a MetricsResource.
     *
     * @param format the format to use for metric names and tag keys.
     */
    MetricsResource(Format format)
        {
        super(format);
        }


    // ----- MetricsResource methods ----------------------------------------

    /**
     * Obtain the current Prometheus metrics data for all metrics.
     *
     * @return the current Prometheus metrics data for all metrics
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public MetricsFormatter getPrometheusMetrics(@Context UriInfo uriInfo)
        {
        final MetricPredicate predicate = new MetricPredicate(null, uriInfo.getQueryParameters());
        return getPrometheusMetrics(predicate, useExtendedFormat(uriInfo));
        }

    /**
     * Obtain the current Prometheus metrics data for a metric name.
     * <p>
     * All metrics matching the specified name will be returned.
     * Metrics can be further filtered by specifying query parameters.
     * Each name/value pair in the query parameters is used to match
     * the metric tag values of the metrics returned. Not all of a
     * metrics tags need to be specified, matching is only done on
     * the tags specified in the query parameters, the metric will
     * match even if it has extra tags not specified in the query
     * parameters.
     *
     * @return the current metrics data for a metric name
     */
    @GET
    @Path("{metric}")
    @Produces(MediaType.TEXT_PLAIN)
    public MetricsFormatter getPrometheusMetrics(@PathParam("metric") String sName, @Context UriInfo uriInfo)
        {
        final MetricPredicate predicate = new MetricPredicate(sName, uriInfo.getQueryParameters());
        return getPrometheusMetrics(predicate, useExtendedFormat(uriInfo));
        }

    /**
     * Obtain the current JSON formatted metrics data for all metrics.
     *
     * @return the current JSON formatted metrics data for all metrics
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsFormatter getJsonMetrics(@Context UriInfo uriInfo)
        {
        final MetricPredicate predicate = new MetricPredicate(null, uriInfo.getQueryParameters());
        return getJsonMetrics(predicate, useExtendedFormat(uriInfo));
        }

    /**
     * Obtain the current JSON formatted metrics data for a metric name.
     * <p>
     * All metrics matching the specified name will be returned. Metrics
     * can be further filtered by specifying query parameters.
     * Each name/value pair in the query parameters is used to match the
     * metric tag values of the metrics returned. Not all of a metrics
     * tags need to be specified, matching is only done on the tags specified
     * in the query parameters, the metric will match even if it has extra
     * tags not specified in the query parameters.
     *
     * @return the current metrics data for a metric name
     */
    @GET
    @Path("{metric}")
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsFormatter getJsonMetrics(@PathParam("metric") String sName, @Context UriInfo uriInfo)
        {
        final MetricPredicate predicate = new MetricPredicate(sName, uriInfo.getQueryParameters());
        return getJsonMetrics(predicate, useExtendedFormat(uriInfo));
        }

    // ----- helper methods -------------------------------------------------

    private boolean useExtendedFormat(UriInfo uriInfo)
        {
        return f_fAlwaysUseExtended ||
               "true".equalsIgnoreCase(uriInfo.getQueryParameters().getFirst("extended"));
        }
    }

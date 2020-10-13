/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.metrics.internal;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.metrics.MBeanMetric;

import com.tangosol.util.Base;

import java.io.IOException;
import java.io.Writer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.function.Predicate;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.PermitAll;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;


/**
 * Metrics Rest Resource
 *
 * @author jk  2019.06.24
 * @since 12.2.1.4.0
 */
@Path("/")
@PermitAll
public class MetricsResource
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
        this(defaultFormat());
        }

    /**
     * Create a MetricsResource.
     *
     * @param format the format to use for metric names and tag keys.
     */
    MetricsResource(Format format)
        {
        f_format = format;
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
        return new PrometheusFormatter(useExtendedFormat(uriInfo),
                                       f_format,
                                       getMetrics(predicate));
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
        return new PrometheusFormatter(useExtendedFormat(uriInfo), f_format, getMetrics(predicate));
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
        return new JsonFormatter(useExtendedFormat(uriInfo), getMetrics(predicate));
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
        return new JsonFormatter(useExtendedFormat(uriInfo), getMetrics(predicate));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Determine the metric format to use based on system properties.
     *
     * @return  the metric format to use based on system properties
     */
    static Format defaultFormat()
        {
        if (Config.getBoolean(PROP_USE_LEGACY_NAMES, true))
            {
            return Format.Legacy;
            }
        else if (Config.getBoolean(PROP_USE_MP_NAMES, false))
            {
            return Format.Microprofile;
            }
        return Format.DotDelimited;
        }

    private boolean useExtendedFormat(UriInfo uriInfo)
        {
        return f_fAlwaysUseExtended ||
               "true".equalsIgnoreCase(uriInfo.getQueryParameters().getFirst("extended"));
        }

    private List<MBeanMetric> getMetrics(Predicate<MBeanMetric> predicate)
        {
        try
            {
            Stream<Map.Entry<MBeanMetric.Identifier, MBeanMetric>> stream
                    = DefaultMetricRegistry.getRegistry().stream();

            if (predicate != null)
                {
                stream = stream.filter(e -> predicate.test(e.getValue()));
                }
// ToDo: This was originally so that cluster metrics are only published from the senior member but what if the senior member is not metrics enabled????
//
//            else
//                {
//                // filter out cluster metrics if this is not the senior member
//                int nId     = CacheFactory.ensureCluster().getLocalMember().getId();
//                int nOldest = CacheFactory.ensureCluster().getOldestMember().getId();
//
//                if (nId != nOldest)
//                    {
//                    stream = stream.filter(e -> !e.getKey().getName().startsWith("coherence_cluster_"));
//                    }
//                }

            return stream.map(Map.Entry::getValue)
                         .collect(Collectors.toList());
            }
        catch (Throwable t)
            {
            CacheFactory.log("Exception in MetricsResource.getMetrics()"
                             + Base.printStackTrace(t), Base.LOG_ERR);
            throw t;
            }
        }

    // ----- inner class: MetricPredicate -----------------------------------

    /**
     * A {@link Predicate} that can be used to restrict the metrics returned by a request.
     */
    private class MetricPredicate
            implements Predicate<MBeanMetric>
        {
        /**
         * Create a predicate.
         *
         * @param sName   the value to use to match a metric's name
         * @param mapTags the values to use to match a metrics tags
         */
        private MetricPredicate(String sName, MultivaluedMap<String, String> mapTags)
            {
            f_sName = sName;
            f_mapTags = mapTags.entrySet()
                    .stream()
                    .filter(e -> !e.getKey().equalsIgnoreCase("extended"))
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
            }

        // ----- Predicate methods ------------------------------------------

        @Override
        public boolean test(MBeanMetric metric)
            {
            return hasValue(metric) && nameMatches(metric) && tagsMatch(metric);
            }

        // ----- helper methods ---------------------------------------------

        private boolean hasValue(MBeanMetric metric)
            {
            return metric.getValue() != null;
            }

        private boolean nameMatches(MBeanMetric metric)
            {
            return f_sName == null || metric.getName().startsWith(f_sName);
            }

        private boolean tagsMatch(MBeanMetric metric)
            {
            if (f_mapTags == null || f_mapTags.isEmpty())
                {
                return true;
                }

            Map<String, String> mapTags = metric.getTags();

            for (String sKey : f_mapTags.keySet())
                {
                if (!f_mapTags.get(sKey).equals(mapTags.get(sKey)))
                    {
                    return false;
                    }
                }
            return true;
            }

        // ----- data members -----------------------------------------------

        private final String f_sName;

        private final Map<String, String> f_mapTags;
        }

    // ----- inner class: PrometheusFormatter -------------------------------

    /**
     * A {@link MetricsFormatter} implementation that writes metrics
     * in a Prometheus format.
     */
    static class PrometheusFormatter
            implements MetricsFormatter
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code PrometheusFormatter} instance.
         *
         * @param fExtended    the flag specifying whether to include metric type
         *                     and description into the output
         * @param format       the format to use for metric names and tag keys.
         * @param listMetrics  the list of metrics to write
         */
        PrometheusFormatter(boolean fExtended, Format format, List<MBeanMetric> listMetrics)
            {
            f_fExtended   = fExtended;
            f_format      = format;
            f_listMetrics = listMetrics;
            }

        // ---- MetricsFormatter interface ----------------------------------

        @Override
        public void writeMetrics(Writer writer) throws IOException
            {
            for (MBeanMetric metric : f_listMetrics)
                {
                writeMetric(writer, metric);
                }
            }

        // ----- helper methods ---------------------------------------------

        private void writeMetric(Writer writer, MBeanMetric m) throws IOException
            {
            MBeanMetric.Identifier id     = m.getIdentifier();
            Map<String, String>    mapTag = id.getPrometheusTags();
            String                 sName;

            switch (f_format)
                {
                case Legacy:
                    sName = id.getLegacyName();
                    break;
                case Microprofile:
                    sName = id.getMicroprofileName();
                    break;
                case DotDelimited:
                default:
                    sName = id.getFormattedName().replaceAll("\\.", "_");
                }

            if (f_fExtended)
                {
                writeType(writer, sName);
                writeHelp(writer, sName, m.getDescription());
                }

            writer.append(sName);
            writeTags(writer, mapTag);

            writer.append(' ')
                  .append(m.getValue().toString())
                  .append('\n');
            }

        private void writeType(Writer writer, String sName) throws IOException
            {
            writer.append("# TYPE ").append(sName).append(" gauge \n");
            }

        private void writeHelp(Writer writer, String sName, String sDescription) throws IOException
            {
            if (sDescription != null && sDescription.length() > 0)
                {
                writer.append("# HELP ")
                        .append(sName)
                        .append(' ')
                        .append(sDescription)
                        .append('\n');
                }
            }

        private void writeTags(Writer writer, Map<String, String> mapTags) throws IOException
            {
            if (!mapTags.isEmpty())
                {
                writer.write('{');

                Iterator<Map.Entry<String, String>> iterator = mapTags.entrySet().iterator();
                while (iterator.hasNext())
                    {
                    Map.Entry<String, String> tag = iterator.next();
                    writer.append(tag.getKey())
                          .append("=\"")
                          .append(tag.getValue())
                          .append('"');
                    if (iterator.hasNext())
                        {
                        writer.append(", ");
                        }
                    }

                writer.write('}');
                }
            }

        // ---- data members ------------------------------------------------

        /**
         * The flag specifying whether to include metric type and description
         * into the output.
         */
        private final boolean f_fExtended;

        /**
         * The format the format to use for metric names and tag keys.
         */
        private final Format f_format;

        /**
         * The list of metrics to write.
         */
        private final List<MBeanMetric> f_listMetrics;
        }

    // ----- inner class: JsonFormatter -------------------------------------

    /**
     * A {@link MetricsFormatter} implementation that writes metrics
     * in a JSON format.
     */
    static class JsonFormatter
            implements MetricsFormatter
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code JsonFormatter} instance.
         *
         * @param fExtended  the flag specifying whether to include metric type
         *                   and description into the output
         * @param metrics    the list of metrics to write
         */
        JsonFormatter(boolean fExtended, List<MBeanMetric> metrics)
            {
            f_fExtended = fExtended;
            f_metrics   = metrics;
            }

        // ---- MetricsFormatter interface ----------------------------------

        @Override
        public void writeMetrics(Writer writer) throws IOException
            {
            try (JsonGenerator jsonWriter = new JsonFactory().createGenerator(writer))
                {
                jsonWriter.writeStartArray();
                for (MBeanMetric metric : f_metrics)
                    {
                    writeMetric(jsonWriter, metric);
                    }
                jsonWriter.writeEndArray();
                }
            }

        // ----- helper methods ---------------------------------------------

        private void writeMetric(JsonGenerator writer, MBeanMetric m) throws IOException
            {
            final MBeanMetric.Identifier id = m.getIdentifier();

            writer.writeStartObject();

            writer.writeStringField("name", id.getName());
            writeTags(writer, id.getTags());
            writer.writeStringField("scope", id.getScope().name());
            writer.writeObjectField("value", m.getValue());

            final String sDesc = m.getDescription();
            if (f_fExtended && sDesc != null && sDesc.length() > 0)
                {
                writer.writeStringField("description", sDesc);
                }

            writer.writeEndObject();
            }

        private void writeTags(JsonGenerator writer, Map<String, String> mapTags) throws IOException
            {
            if (!mapTags.isEmpty())
                {
                writer.writeFieldName("tags");
                writer.writeStartObject();
                for (Map.Entry<String, String> tag : mapTags.entrySet())
                    {
                    writer.writeStringField(tag.getKey(), tag.getValue());
                    }
                writer.writeEndObject();
                }
            }

        // ---- data members ------------------------------------------------

        /**
         * The flag specifying whether to include metric type and description
         * into the output.
         */
        private final boolean f_fExtended;

        /**
         * The list of metrics to write.
         */
        private final List<MBeanMetric> f_metrics;
        }

    // ----- inner enum: Format ---------------------------------------------

    /**
     * An enum to represent the format to use for metric names and tag keys.
     */
    enum Format
        {
        /**
         * Names will be dot delimited without a scope, e.g. coherence.cluster.size
         */
        DotDelimited,
        /**
         * Names will be underscore delimited with a scope, e.g. vendor:coherence_cluster_size
         */
        Legacy,
        /**
         * Names will be MP 2.0 compatible with a scope, e.g. vendor_Coherence_Cluster_Size
         */
        Microprofile,
        }

    // ----- constants ------------------------------------------------------

    /**
     * The System property to use to determine whether to always include
     * extended information (type and/or description) when publishing metrics.
     */
    private static final String PROP_EXTENDED = "coherence.metrics.extended";

    /**
     * A flag to determine whether to always include help information when
     * publishing metrics.
     */
    private static final boolean f_fAlwaysUseExtended
            = Boolean.parseBoolean(System.getProperty(PROP_EXTENDED, "false"));

    /**
     * A system property that when true outputs metric names using Coherence legacy
     * format and when false outputs Prometheus metrics with Microprofile 2.0
     * compatible metric names.
     */
    public static final String PROP_USE_LEGACY_NAMES = "coherence.metrics.legacy.names";

    /**
     * A system property that when true outputs metric names as Microprofile 2.0
     * compatible metric names.
     */
    public static final String PROP_USE_MP_NAMES = "coherence.metrics.mp.names";

    // ----- data members ---------------------------------------------------

    /**
     * The format to use for metric names and tag keys.
     */
    private final Format f_format;
    }

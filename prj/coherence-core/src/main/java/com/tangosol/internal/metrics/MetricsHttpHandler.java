/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;


import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.metrics.MBeanMetric;

import com.tangosol.util.SimpleMapEntry;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.net.URI;
import java.net.URLDecoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.function.Predicate;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.zip.GZIPOutputStream;

import static java.util.stream.Collectors.toList;


/**
 * Metrics Rest http endpoint
 *
 * @author jk  2019.06.24
 * @since 12.2.1.4.0
 */
public class MetricsHttpHandler
        implements HttpHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a MetricsResource.
     * <p>
     * This constructor will be used by Coherence to create the resource instance.
     * <p>
     * The {@code coherence.metrics.legacy.names} system property will be used to
     * determine whether legacy metric names ot Microprofile compatible metric
     * names will be used when publishing Prometheus formatted metrics.
     */
    public MetricsHttpHandler()
        {
        this(defaultFormat());
        }

    /**
     * Create a MetricsResource.
     *
     * @param format the format to use for metric names and tag keys.
     */
    protected MetricsHttpHandler(Format format)
        {
        f_format = format;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the {@link Format} being used for metric names.
     *
     * @return the {@link Format} being used for metric names
     */
    public Format getFormat()
        {
        return f_format;
        }

    /**
     * Returns the String being used for the context root.
     *
     * @return the String being used for the context root
     *
     * @since 14.1.2.0.0
     */
    public String getPath()
        {
        return m_sPath;
        }

    /**
     * Setter for path of the context root.
     *
     * @param sPath  the root path
     *
     * @since 14.1.2.0.0
     */
    public void setPath(String sPath)
        {
        m_sPath = sPath;
        }

    // ----- HttpHandler methods --------------------------------------------

    @Override
    public void handle(HttpExchange exchange) throws IOException
        {
        try
            {
            URI                       requestURI   = exchange.getRequestURI();
            Map<String, List<String>> mapQuery     = getQueryParameters(requestURI);
            Headers                   headers      = exchange.getRequestHeaders();
            String                    sSuffix      = null;
            String                    sName        = null;
            List<String>              listExtended = mapQuery.remove("extended");
            boolean                   fExtended    = f_fAlwaysUseExtended 
                                                        || listExtended != null && !listExtended.isEmpty()
                                                        && Boolean.parseBoolean(listExtended.get(0));

            // The path will always start with the context root path, e.g. /metrics, but may have *anything* after that
            // as the JDK http server is not fussy
            String sPath = requestURI.getPath();
            if (sPath.equals(getPath()) || sPath.startsWith(getPath() + "/"))
                {
                // path is valid so far, as it is either the root path or root + "/"...

                // strip any .suffix which can be used to override the accepted media type
                if (sPath.endsWith(".txt"))
                    {
                    sSuffix = ".txt";
                    sPath = sPath.substring(0, sPath.length() - 4);
                    }
                else if (sPath.endsWith(".json"))
                    {
                    sSuffix = ".json";
                    sPath = sPath.substring(0, sPath.length() - 5);
                    }

                // will be 2 or longer, first element is empty string
                // valid length is 2, 3, or 4 if 4 is empty
                String[] asSegment = sPath.split("/");
                if (asSegment.length > 4 || (asSegment.length == 4 && asSegment[3].length() != 0))
                    {
                    // the path is invalid, so send 404
                    send(exchange, 404);
                    return;
                    }

                if (asSegment.length >= 3)
                    {
                    // we have a metric name in the path i.e. /metrics/foo
                    sName = asSegment[2];
                    }
                }
            else
                {
                // the path is invalid, so send 404
                send(exchange, 404);
                return;
                }

            Predicate<MBeanMetric> predicate = createPredicate(sName, mapQuery);
            MetricsFormatter       formatter;

            if (".txt".equals(sSuffix))
                {
                formatter = getPrometheusMetrics(predicate, fExtended);
                }
            else if (".json".equals(sSuffix))
                {
                formatter = getJsonMetrics(predicate, fExtended);
                }
            else
                {
                formatter = getFormatterForAcceptedType(headers, predicate, fExtended);
                }

            if (formatter == null)
                {
                // no valid media types in the "Accept" header or path suffix
                send(exchange, 415);
                return;
                }

            boolean fGzip     = false;
            String  sEncoding = headers.getFirst("Accept-Encoding");
            if (sEncoding != null)
                {
                fGzip = Arrays.stream(sEncoding.split(","))
                              .map(String::trim)
                              .anyMatch("gzip"::equalsIgnoreCase);
                }

            try (OutputStream os = exchange.getResponseBody())
                {
                exchange.getResponseHeaders().set("Content-Type", formatter.getContentType());
                if (fGzip)
                    {
                    sendGZippedMetrics(exchange, os, formatter);
                    }
                else
                    {
                    sendMetrics(exchange, () -> os, formatter);
                    }
                }
            }
        catch (Throwable t)
            {
            Logger.err(t);
            exchange.sendResponseHeaders(500, -1);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns the query parameters present in the URI.
     *
     * @param uri  the {@link URI} to get the query parameters from
     *
     * @return the map of query parameters from the URI, or an empty map if there
     *         were no query parameters
     */
    private Map<String, List<String>> getQueryParameters(URI uri)
        {
        String sQuery = uri.getQuery();
        if (sQuery == null || sQuery.length() == 0)
            {
            return Collections.emptyMap();
            }
        return Arrays.stream(sQuery.split("&"))
                .map(this::splitQueryParameter)
                .filter(e -> e.getValue() != null)
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new,
                                               Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        }

    /**
     * Split the specified key/value query parameter into a {@link Map.Entry}
     * decoding any encoded characters in the query parameter value.
     *
     * @param  sParam  the query parameter to decode
     *
     * @return the query parameter decoded into a {@link Map.Entry}
     */
    private Map.Entry<String, String> splitQueryParameter(String sParam)
        {
        try
            {
            int    nIndex   = sParam.indexOf("=");
            String sKey     = nIndex > 0 ? sParam.substring(0, nIndex) : sParam;
            String sValue   = nIndex > 0 && sParam.length() > nIndex + 1 ? sParam.substring(nIndex + 1) : null;
            String sDecoded = sValue == null ? null : URLDecoder.decode(sValue, "UTF-8");
            return new SimpleMapEntry<>(URLDecoder.decode(sKey, "UTF-8"), sDecoded);
            }
        catch (UnsupportedEncodingException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Returns the {@link MetricsFormatter} matching the media type in any Accept header
     * present in the request.
     *
     * @param headers    the http request headers
     * @param predicate  the optional {@link Predicate} to pass to the {@link MetricsFormatter}
     * @param fExtended  the extended flag to the {@link MetricsFormatter}
     *
     * @return the {@link MetricsFormatter} matching the media type in any Accept header
     *         present in the request or {@code null} if there is no Accept header or
     *         no {@link MetricsFormatter} matches the header
     */
    private MetricsFormatter getFormatterForAcceptedType(Headers                headers,
                                                         Predicate<MBeanMetric> predicate,
                                                         boolean                fExtended)
        {
        List<String> listAccept = headers.get("Accept");
        if (listAccept == null)
            {
            return getPrometheusMetrics(predicate, fExtended);
            }
        else
            {
            for (String sType : listAccept)
                {
                String[] asType = sType.split(",");
                for (String sAccept : asType)
                    {
                    int nIndex = sAccept.indexOf(';');
                    if (nIndex >= 0)
                        {
                        sAccept = sAccept.substring(0, nIndex);
                        }
                    switch (sAccept.trim())
                        {
                        case APPLICATION_JSON:
                            return getJsonMetrics(predicate, fExtended);
                        case TEXT_PLAIN:
                        case WILDCARD:
                            return getPrometheusMetrics(predicate, fExtended);
                        }
                    }
                }
            }
        return null;
        }

    /**
     * Send the metrics response.
     * This method uses a {@link StreamSupplier} to supply the {@link OutputStream} to send the metrics data to.
     * This is because we must send the response headers before sending any output data but if using an output
     * stream such as {@link GZIPOutputStream} this sends dat aas soon as it is constructed. By using a supplier
     * we can delay construction of any stream and hence sending any data until after this method has sent the
     * response headers.
     *
     * @param exchange   the {@link HttpExchange} to send the response to
     * @param supplier   a {@link StreamSupplier} to supply the {@link OutputStream} to send the metrics data to
     * @param formatter  the {@link MetricsFormatter} to format the response
     *
     * @throws IOException if an error occurs sending the response
     */
    private void sendMetrics(HttpExchange exchange, StreamSupplier supplier, MetricsFormatter formatter)
            throws IOException
        {
        exchange.sendResponseHeaders(200, 0);
        try (Writer writer = new OutputStreamWriter(supplier.get()))
            {
            formatter.writeMetrics(writer);
            writer.flush();
            }
        }

    /**
     * Send the metrics response using gzip to compress the metrics data.
     * <p>
     * This method will wrap the specified {@link OutputStream} in a {@link GZIPOutputStream}
     * before sending data.
     *
     * @param exchange   the {@link HttpExchange} to send the response to
     * @param os         the {@link OutputStream} to send the metrics data to
     * @param formatter  the {@link MetricsFormatter} to format the response
     *
     * @throws IOException if an error occurs sending the response
     */
    private void sendGZippedMetrics(HttpExchange exchange, OutputStream os, MetricsFormatter formatter)
            throws IOException
        {
        exchange.getResponseHeaders().set("Content-Encoding", "gzip");
        sendMetrics(exchange, () -> new GZIPOutputStream(os), formatter);
        }

    /**
     * Send a simple http response.
     *
     * @param t      the {@link HttpExchange} to send the response to
     * @param status the response status
     */
    private static void send(HttpExchange t, int status) {
        try {
            t.sendResponseHeaders(status, 0);
            try (OutputStream os = t.getResponseBody()) {
                os.write(EMPTY_BODY);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtain the current Prometheus metrics data.
     * <p>
     * If the {@code sName} argument is {@code null} or an empty string, all metrics will be returned.
     * <p>
     * If the {@code sName} argument is not {@code null} and not empty, all metrics matching the specified
     * name will be returned. Metrics can be further filtered by specifying query parameters.
     * Each name/value pair in the query parameters is used to match the metric tag values of the metrics returned.
     * Not all of a metrics tags need to be specified, matching is only done on the tags specified in the query
     * parameters, the metric will match even if it has extra tags not specified in the query parameters.
     *
     * @return the current Prometheus metrics data
     */
    protected MetricsFormatter getPrometheusMetrics(Predicate<MBeanMetric> predicate, boolean fExtended)
        {
        return new PrometheusFormatter(fExtended, f_format, getMetrics(predicate));
        }

    /**
     * Obtain the current JSON formatted metrics data for all metrics.
     * <p>
     * If the {@code sName} argument is {@code null} or an empty string, all metrics will be returned.
     * <p>
     * If the {@code sName} argument is not {@code null} and not empty, all metrics matching the specified
     * name will be returned. Metrics can be further filtered by specifying query parameters.
     * Each name/value pair in the query parameters is used to match the metric tag values of the metrics returned.
     * Not all of a metrics tags need to be specified, matching is only done on the tags specified in the query
     * parameters, the metric will match even if it has extra tags not specified in the query parameters.
     *
     * @return the current JSON formatted metrics data for all metrics
     */
    protected MetricsFormatter getJsonMetrics(Predicate<MBeanMetric> predicate, boolean fExtended)
        {
        return new JsonFormatter(fExtended, getMetrics(predicate));
        }

    /**
     * Determine the metric format to use based on system properties.
     *
     * @return  the metric format to use based on system properties
     */
    static Format defaultFormat()
        {
        if (Config.getBoolean(PROP_USE_MP_NAMES, false))
            {
            return Format.Microprofile;
            }
        else if (Config.getBoolean(PROP_USE_DOT_NAMES, false))
            {
            return Format.DotDelimited;
            }
        // As of 14.1.2 and 24.09 this property defaults to false, which will remove the
        // "vendor:" prefix from Prometheus metrics and require use of updated Grafana dashboards
        else if (Config.getBoolean(PROP_USE_LEGACY_NAMES, false))
            {
            return Format.Legacy;
            }
        return Format.Default;
        }

    /**
     * Returns the lst of metrics matching the predicate, or all metrics if
     * the predicate is {@code null}.
     *
     * @param predicate  the optional predicate to use to filter the returned metrics
     *
     * @return the lst of metrics matching the predicate, or all metrics if
     *         the predicate is {@code null}
     */
    protected List<MBeanMetric> getMetrics(Predicate<MBeanMetric> predicate)
        {
        try
            {
            Stream<Map.Entry<MBeanMetric.Identifier, MBeanMetric>> stream
                    = DefaultMetricRegistry.getRegistry().stream();

            if (predicate != null)
                {
                stream = stream.filter(e -> predicate.test(e.getValue()));
                }
            return stream.map(Map.Entry::getValue).collect(toList());
            }
        catch (Throwable t)
            {
            Logger.err("Exception in MetricsResource.getMetrics():", t);
            throw t;
            }
        }

    /**
     * Create a {@link MetricPredicate} from a metric name pattern and tags.
     *
     * @param sName    the optional metric name pattern to use in the predicate
     * @param mapTags  the optional tags to use in the predicate
     *
     * @return the {@link MetricPredicate} to use or {@code null} if neither the name
     *         nor the tags are specified
     */
    private MetricPredicate createPredicate(String sName, Map<String, List<String>> mapTags)
        {
        if ((sName == null || sName.isEmpty()) && mapTags.isEmpty())
            {
            return null;
            }
        return new MetricPredicate(sName, mapTags);
        }

    // ----- inner class: MetricPredicate -----------------------------------

    /**
     * A {@link Predicate} that can be used to restrict the metrics returned by a request.
     */
    protected static class MetricPredicate
            implements Predicate<MBeanMetric>
        {
        /**
         * Create a predicate.
         *
         * @param sName   the value to use to match a metric name
         * @param mapTags the values to use to match a metric tags
         */
        public MetricPredicate(String sName, Map<String, List<String>> mapTags)
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

        /**
         * Returns {@code true} if the metric has a non-null value.
         *
         * @param metric  the metric to check
         *
         * @return {@code true} if the metric has a non-null value
         */
        private boolean hasValue(MBeanMetric metric)
            {
            return metric.getValue() != null;
            }

        /**
         * Returns {@code true} if the metric name matches this predicate.
         *
         * @param metric  the metric to check
         *
         * @return {@code true} if the metric name matches this predicate
         */
        private boolean nameMatches(MBeanMetric metric)
            {
            return f_sName == null || metric.getName().startsWith(f_sName);
            }

        /**
         * Returns {@code true} if the metric tags matches this predicate.
         *
         * @param metric  the metric to check
         *
         * @return {@code true} if the metric tags matches this predicate
         */
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
    protected static class PrometheusFormatter
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
        public PrometheusFormatter(boolean fExtended, Format format, List<MBeanMetric> listMetrics)
            {
            f_fExtended   = fExtended;
            f_format      = format;
            f_listMetrics = listMetrics;
            }

        // ---- MetricsFormatter interface ----------------------------------

        @Override
        public String getContentType()
            {
            return TEXT_PLAIN;
            }

        @Override
        public void writeMetrics(Writer writer) throws IOException
            {
            for (MBeanMetric metric : f_listMetrics)
                {
                writeMetric(writer, metric);
                writer.flush();
                }
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Write the metric.
         *
         * @param writer  the {@link Writer} to write to
         * @param metric  the metric to write
         *
         * @throws IOException if an error occurs writing the metric
         */
        private void writeMetric(Writer writer, MBeanMetric metric) throws IOException
            {
            Object oValue = metric.getValue();
            if (oValue != null)
                {
                MBeanMetric.Identifier id     = metric.getIdentifier();
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
                        sName = id.getFormattedName();
                        break;
                    default:
                        sName = id.getFormattedName().replaceAll("\\.", "_");
                    }

                if (f_fExtended)
                    {
                    writeType(writer, sName);
                    writeHelp(writer, sName, metric.getDescription());
                    }

                writer.append(sName);
                writeTags(writer, mapTag);

                writer.append(' ')
                        .append(oValue.toString())
                        .append('\n');
                }
            }

        /**
         * Write the metric type line.
         *
         * @param writer  the {@link Writer} to write to
         * @param sName   the metric name
         *
         * @throws IOException if an error occurs writing the type line
         */
        private void writeType(Writer writer, String sName) throws IOException
            {
            writer.append("# TYPE ").append(sName.trim()).append(" gauge\n");
            }

        /**
         * Write the metric help description.
         *
         * @param writer        the {@link Writer} to write to
         * @param sName         the metric name
         * @param sDescription  the metric help description
         *
         * @throws IOException if an error occurs writing the help description
         */
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

        /**
         * Write the metric tags.
         *
         * @param writer   the {@link Writer} to write the tags to
         * @param mapTags  the metric tags to write
         *
         * @throws IOException if an error occurs writing the tags
         */
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
         * The format to use for metric names and tag keys.
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
    protected static class JsonFormatter
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
        public JsonFormatter(boolean fExtended, List<MBeanMetric> metrics)
            {
            f_fExtended = fExtended;
            f_metrics   = metrics;
            }

        // ---- MetricsFormatter interface ----------------------------------

        @Override
        public String getContentType()
            {
            return APPLICATION_JSON;
            }

        @Override
        public void writeMetrics(Writer writer) throws IOException
            {
            writer.write('[');

            boolean separator = false;
            for (MBeanMetric metric : f_metrics)
                {
                separator = writeMetric(writer, metric, separator) || separator;
                }
            writer.write(']');
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Write the metric as a json object.
         *
         * @param writer     the {@link Writer} to write the json to
         * @param metric     the metric to write
         * @param fSeparator {@code true} to indicate that a comma separator should precede
         *                   the metric json object
         *
         * @throws IOException if an error occurs writing the tags
         */
        private boolean writeMetric(Writer writer, MBeanMetric metric, boolean fSeparator) throws IOException
            {
            Object oValue = metric.getValue();
            if (oValue != null)
                {
                MBeanMetric.Identifier id = metric.getIdentifier();

                if (fSeparator)
                    {
                    writer.write(',');
                    }
                writer.write('{');
                writer.write("\"name\":\"");
                writer.write(id.getName());
                writer.write("\",");

                writeTags(writer, id.getTags());

                writer.write("\"scope\":\"");
                writer.write(id.getScope().name());
                writer.write("\",");
                writer.write("\"value\":");
                if (oValue instanceof Number || oValue instanceof Boolean)
                    {
                    writer.write(String.valueOf(oValue));
                    }
                else
                    {
                    writer.write('"');
                    writer.write(String.valueOf(oValue));
                    writer.write('"');
                    }

                String sDesc = metric.getDescription();
                if (f_fExtended && sDesc != null && sDesc.length() > 0)
                    {
                    writer.write(',');
                    writer.write("\"description\":\"");
                    writer.write(sDesc);
                    writer.write('"');
                    }

                writer.write('}');
                return true;
                }
            return false;
            }

        /**
         * Write the metric tags as a json object.
         *
         * @param writer   the {@link Writer} to write the json to
         * @param mapTags  the metric tags to write
         *
         * @throws IOException if an error occurs writing the tags
         */
        private void writeTags(Writer writer, Map<String, String> mapTags) throws IOException
            {
            if (!mapTags.isEmpty())
                {
                String sTags = mapTags.entrySet().stream()
                        .map(e -> '"' + e.getKey() + "\":\"" + e.getValue() + "\"")
                        .collect(Collectors.joining(","));

                writer.write("\"tags\":{");
                writer.write(sTags);
                writer.write("},");
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

    // ----- inner interface: StreamSupplier --------------------------------

    /**
     * A supplier of {@link OutputStream} instances
     */
    @FunctionalInterface
    private interface StreamSupplier
        {
        /**
         * Return the {@link OutputStream} to use to send a http response.
         * @return the {@link OutputStream} to use to send a http response
         * @throws IOException if there is an error creating the stream
         */
        OutputStream get() throws IOException;
        }

    // ----- inner enum: Format ---------------------------------------------

    /**
     * An enum to represent the format to use for metric names and tag keys.
     */
    public enum Format
        {
        /**
         * Names will the default format without a scope, e.g. coherence_cluster_size
         */
        Default,
        /**
         * Names will be a dot delimited without a scope, e.g. coherence.cluster.size
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
    protected static final String PROP_EXTENDED = "coherence.metrics.extended";

    /**
     * A flag to determine whether to always include help information when
     * publishing metrics.
     */
    protected static final boolean f_fAlwaysUseExtended
            = Boolean.parseBoolean(System.getProperty(PROP_EXTENDED, "false"));

    /**
     * A system property that when true outputs metric names using Coherence legacy
     * format.
     */
    public static final String PROP_USE_LEGACY_NAMES = "coherence.metrics.legacy.names";

    /**
     * A system property that when true outputs metric names as Microprofile 2.0
     * compatible metric names.
     */
    public static final String PROP_USE_MP_NAMES = "coherence.metrics.mp.names";

    /**
     * A system property that when true outputs metric names as dot delimited metric names.
     */
    public static final String PROP_USE_DOT_NAMES = "coherence.metrics.dot.names";

    /**
     * The "Accept" header value for the json media type.
     */
    public static final String APPLICATION_JSON = "application/json";

    /**
     * The "Accept" header value for the text media type.
     */
    public static final String TEXT_PLAIN = "text/plain";

    /**
     * The "Accept" header value for the wild-card media type.
     */
    public static final String WILDCARD = "*/*";

    /**
     * An empty byte array to use as an empty response body.
     */
    private static final byte[] EMPTY_BODY = new byte[0];

    // ----- data members ---------------------------------------------------

    /**
     * The format to use for metric names and tag keys.
     */
    protected Format f_format;

    /**
     * The context root path.
     *
     * @since 14.1.2.0.0
     */
    private String m_sPath;
    }

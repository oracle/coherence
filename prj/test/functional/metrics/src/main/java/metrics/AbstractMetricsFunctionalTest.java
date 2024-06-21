/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.coherence.io.json.JsonSerializer;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared helper methods for metrics end point functional testing.
 *
 * @author jf 2018.07.31
 */
public class AbstractMetricsFunctionalTest extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor
     */
    public AbstractMetricsFunctionalTest()
        {
        }

    /**
     * Constructor that initializes cluster member configuration file.
     *
     * @param cfgfile coherence configuration file
     */
    AbstractMetricsFunctionalTest(String cfgfile)
        {
        super(cfgfile);
        }

    // ---- AbstractMetricsFunctionalTest methods  --------------------------

    /**
     * Get the metrics from localhost at specified <code>port</code>.
     *
     * @param port  metrics http end point port
     *
     * @return a list of representing latest metrics scrape.
     *
     * @throws Exception if the request fails
     */
    List<Map<String,Object>> getMetrics(int port)
            throws Exception
        {
        return getMetrics(port, null, null);
        }

    /**
     * Get the metrics from localhost at specified <code>port</code>.
     *
     * @param port  metrics http end point port
     *
     * @return a list of representing latest metrics scrape.
     *
     * @throws Exception if the request fails
     */
    @SuppressWarnings("unchecked")
    List<Map<String,Object>> getMetrics(int port, String sName, Map<String, String> mapTags)
            throws Exception
        {
        String sData = getMetricsResponse(port, sName, mapTags);

        if (sData.length() > 0)
            {
            return MAPPER.deserialize(sData, List.class);
            }
        return Collections.emptyList();
        }

    /**
     * Return from metrics get response on endpoint on localhost at specified <code>port</code>.
     *
     * @param port  metrics http end point port
     *
     * @return metrics get response
     *
     * @throws Exception if the request fails
     */
    String getMetricsResponse(int port) throws Exception
        {
        return getMetricsResponse(port, null, null);
        }

    /**
     * Return from metrics get response on endpoint on localhost at specified <code>port</code>.
     *
     * @param port  metrics http end point port
     *
     * @return metrics get response
     *
     * @throws Exception if the request fails
     */
    protected String getMetricsResponse(int port, String sName, Map<String, String> mapTags) throws Exception
        {
        String sURL = composeURL(port);

        if (sName != null && sName.length() > 0)
            {
            sURL = sURL + "/" + sName;
            }

        if (mapTags != null && mapTags.size() > 0)
            {
            sURL = sURL + "?" + mapTags.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + encode(e.getValue()))
                    .collect(Collectors.joining("&"));
            }

        HttpURLConnection con = (HttpURLConnection) URI.create(sURL).toURL().openConnection();

        modifyConnection(con);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        StringBuilder sbResponse = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())))
            {
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                {
                sbResponse.append(inputLine);
                }
            }
        return sbResponse.toString();
        }

    protected String composeURL(int port)
        {
        return "http://127.0.0.1:" + port + "/metrics";
        }

    protected void modifyConnection(HttpURLConnection con)
        {
        }

    private String encode(String sValue)
        {
        try
            {
            return URLEncoder.encode(sValue, "UTF-8");
            }
        catch (UnsupportedEncodingException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Post a metrics lookup with Accept-Encoding "gzip" on localhost at specified <code>port</code>.
     *
     * @param port  metrics http end point port
     *
     * @return HttpURLConnection
     *
     * @throws IOException if the request fails
     */
    protected HttpURLConnection getMetricsResponseConnectionWithAcceptEncodingGzip(int port)
        throws IOException
        {
        String            metricsUrl = composeURL(port);
        HttpURLConnection con        = (HttpURLConnection) URI.create(metricsUrl).toURL().openConnection();

        modifyConnection(con);
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept-Encoding", "gzip");

        return con;
        }

    /**
     * Scrapes prometheus end point for value of metric value having
     * metric tag <code>name</code> of <code>cacheName</code>.
     *
     * @param port       prometheus end point port
     * @param metricName prometheus metric name
     * @param tags       only match metric with these tag name/values.
     *
     * @return long value of metric
     *
     * @throws Exception if the request fails
     */
    // Must be public - used in Eventually.assertThat()
    public long getCacheMetric(int port, String metricName, Map<String, String> tags) throws Exception
        {
        List<Map<String, Object>> list = getMetrics(port, metricName, tags);

        if (list.size() > 0)
            {
            Map<String, Object> map    = list.get(0);
            Object              oValue = map.get("value");

            if (oValue instanceof Number)
                {
                return ((Number) oValue).longValue();
                }
            }

        return -1;
        }

    // ----- data members ---------------------------------------------------

    private static final JsonSerializer MAPPER = new JsonSerializer();
    }

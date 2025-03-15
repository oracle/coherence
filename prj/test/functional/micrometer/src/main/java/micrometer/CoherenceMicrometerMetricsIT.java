/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package micrometer;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.micrometer.CoherenceMicrometerMetrics;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;

import com.tangosol.net.metrics.MBeanMetric;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for {@link CoherenceMicrometerMetrics}.
 *
 * @author Jonathan Knight  2020.10.09
 */
public class CoherenceMicrometerMetricsIT
    {
    @BeforeAll
    static void startCoherence()
        {
        s_prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        s_prometheusRegistry.config().onMeterRegistrationFailed((id, err) ->
            {
            Logger.err("Registration of " + id + " failed with registry " + s_prometheusRegistry + ": " + err);
            });

        CoherenceMicrometerMetrics.INSTANCE.bindTo(s_prometheusRegistry);

        DefaultCacheServer.startServerDaemon()
                .waitForServiceStart();
        }

    @Test
    public void shouldHaveAllMetricsWithSamePrometheusNameAsCoherenceMetrics()
        {
        // create and populate a cache so that there will be cache metrics
        NamedCache foo = CacheFactory.getCache("foo");

        for (int i = 0; i < 100; ++i)
            {
            foo.put(i,i);
            }

        SortedSet<String> setExpected = new TreeSet<>();
        SortedSet<String> setActual   = new TreeSet<>();

        Map<MBeanMetric.Identifier, CoherenceMicrometerMetrics.Holder> metrics
                = CoherenceMicrometerMetrics.INSTANCE.getMetrics();

        for (CoherenceMicrometerMetrics.Holder holder : CoherenceMicrometerMetrics.INSTANCE.getMetrics().values())
            {
            // name without scope
            String sName = holder.getIdentifier().getLegacyName().substring(7);
            // sName is changed to conform to the changed naming convention in the micrometer version 1.13.4
            // see https://github.com/micrometer-metrics/micrometer/wiki/1.13-Migration-Guide
            // New PrometheusMeterRegistry from Micrometer 1.13.4 removes the _total suffix
            sName = stripTotalSuffix(sName);
            StringBuilder str = new StringBuilder(sName);
            for (Map.Entry<String, String> entry : holder.getIdentifier().getPrometheusTags().entrySet())
                {
                String sKey = entry.getKey();
                if(!CoherenceMicrometerMetrics.NAME_TAG_EXCLUDES.contains(sKey))
                    {
                    str.append(" ").append(sKey).append("=").append(entry.getValue());
                    }
                }
            setExpected.add(str.toString());
            }

        String   prometheusTextFormat = s_prometheusRegistry.scrape();
        String[] lines                = prometheusTextFormat.split("\n");

        for (String sLine : lines) {
            if (sLine.startsWith("#") || sLine.trim().isEmpty()) {
                continue;
            }
            setActual.add(sampleToString(sLine.trim()));
        }

        Iterator<String> itExpected = setExpected.iterator();
        Iterator<String> itActual   = setActual.iterator();
        while (itExpected.hasNext())
            {
            assertThat(itActual.hasNext(), is(true));
            String sActual   = itActual.next();
            String sExpected = itExpected.next();
            assertThat(sActual, is(sExpected));
            }
        }

    private String sampleToString(String sSampleToString)
        {
        if (sSampleToString == null || sSampleToString.isEmpty())
            {
            return "";
            }

        int    cBraceStart = sSampleToString.indexOf('{');
        String sMetricName;
        String sLabelsPart = "";

        if (cBraceStart != -1)
            {
            sMetricName = sSampleToString.substring(0, cBraceStart);

            int cBraceEnd  = sSampleToString.indexOf('}', cBraceStart);
            if (cBraceEnd != -1 && cBraceEnd > cBraceStart)
                {
                sLabelsPart = sSampleToString.substring(cBraceStart + 1, cBraceEnd);
                }
            }
        else
            {
            int cFirstSpace = sSampleToString.indexOf(' ');
                sMetricName = (cFirstSpace != -1)
                                ? sSampleToString.substring(0, cFirstSpace)
                                : sSampleToString;
            }

        StringBuilder result = new StringBuilder(sMetricName);

        if (!sLabelsPart.isEmpty())
            {
            int cLen = sLabelsPart.length();
            int i    = 0;

            while (i < cLen)
                {
                // Find the key
                int cEqIndex = sLabelsPart.indexOf('=', i);
                if (cEqIndex == -1)
                    {
                    break; // no more key=value pairs
                    }
                String sKey = sLabelsPart.substring(i, cEqIndex).trim();

                // Find the value
                i = cEqIndex + 1;

                char   quoteChar = sLabelsPart.charAt(i);
                int    cValEnd;
                String value;

                if (quoteChar == '\"')
                    {
                    // Quoted value
                    i++;
                    cValEnd = sLabelsPart.indexOf('\"', i);
                    if (cValEnd == -1)
                        {
                        break; // malformed, but we bail out safely
                        }
                    value = sLabelsPart.substring(i, cValEnd);
                    i     = cValEnd + 1;
                    }
                else
                    {
                    // Unquoted value (not expected in Prometheus, but defensive code)
                    cValEnd = sLabelsPart.indexOf(',', i);
                    if (cValEnd == -1)
                        {
                        value = sLabelsPart.substring(i);
                        i     = cLen;
                        }
                    else
                        {
                        value = sLabelsPart.substring(i, cValEnd);
                        i     = cValEnd + 1;
                        }
                    }

                result.append(" ").append(sKey).append("=").append(value);

                // Skip the comma separator if there is one
                if (i < cLen && sLabelsPart.charAt(i) == ',')
                    {
                    i++;
                    }
                }
            }

        return result.toString();
        }

    // Added to conform to the changed naming convention in the micrometer version 1.13.4
    private String stripTotalSuffix(String input)
        {
        String sSuffix = "_total";

        if (input != null && input.endsWith(sSuffix))
            {
            // Remove the suffix by taking a substring from index 0 to the suffix's starting point
            return input.substring(0, input.length() - sSuffix.length());
            }

        // If it doesn't end with "_total", return the string as is
        return input;
        }

    // ----- data members ---------------------------------------------------

    private static PrometheusMeterRegistry s_prometheusRegistry;
    }

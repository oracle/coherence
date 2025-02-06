/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.util.Capture;
import com.tangosol.coherence.metrics.internal.MetricsResource;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import java.net.HttpURLConnection;
import java.net.URI;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class MetricsFormatTests
    extends AbstractMetricsFunctionalTest
    {
    // ----- Parameterized Setup --------------------------------------------

    /**
     * Create the test parameters.
     *
     * @return the test parameters
     */
    @Parameterized.Parameters(name = "Format={0}")
    public static Collection<Object[]> getTestParameters()
        {
        ArrayList<Object[]> parameters = new ArrayList<>();

        // Run Coherence with no metrics format configured - i.e. use whatever is the current default
        parameters.add(new Object[]{"None Specified", "foo", false});
        // Run Coherence with the legacy metrics format configured
        parameters.add(new Object[]{"Legacy",  MetricsResource.PROP_USE_LEGACY_NAMES, true});
        // Run Coherence with the MP metrics format configured
        parameters.add(new Object[]{"MP", MetricsResource.PROP_USE_MP_NAMES, true});
        // Run Coherence with the dot delimited metrics format configured
        parameters.add(new Object[]{"Dot Delimited", "foo", false});

        return parameters;
        }

    // ----- constructors ---------------------------------------------------
    /**
     * Create the test instance.
     *
     * @param sFormat    the descriptive name of the test
     * @param sProperty  the system property to set to configure the metric name format
     * @param fValue     the value to set the system property to
     * @param format     the expected metrics format
     */
    public MetricsFormatTests(String sFormat, String sProperty, boolean fValue)
        {
        super("client-cache-config-metrics.xml");
        f_sFormat        = sFormat;
        f_sProperty      = sProperty;
        f_fPropertyValue = fValue;
        }

    // ----- test -----------------------------------------------------------
    /**
     * Just extracts metrics into the file so their format can be verified by an external tool.
     *
     * @throws Exception
     */
    @Test
    public void shouldHaveCorrectFormat() throws Exception
        {
        LocalPlatform platform = LocalPlatform.get();
        Capture<Integer> port     = new Capture<>(platform.getAvailablePorts());

        String additionalMetricsProperty = "foo";
        if (!f_sFormat.equals("Legacy"))
            {
            additionalMetricsProperty = MetricsResource.PROP_USE_LEGACY_NAMES;
            }
        // start Coherence with metrics configured with the test format
        try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                                                             SystemProperty.of(f_sProperty, f_fPropertyValue),
                                                             SystemProperty.of(additionalMetricsProperty, false),
                                                             SystemProperty.of(MetricsHttpHelper.PROP_METRICS_ENABLED, true),
                                                             SystemProperty.of("coherence.metrics.http.port", port),
                                                             SystemProperty.of("test.persistence.enabled", false),
                                                             SystemProperty.of("coherence.management.extendedmbeanname", true),
                                                             SystemProperty.of(Logging.PROPERTY_LEVEL, "9"),
                                                             IPv4Preferred.yes(),
                                                             LocalHost.only()))
            {
            Eventually.assertDeferred(() -> member.isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));

            String sURL = "http://127.0.0.1:" + port.get() + "/metrics";

            HttpURLConnection con = (HttpURLConnection) URI.create(sURL).toURL().openConnection();
            con.setRequestProperty("Accept", "text/plain");
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            assertThat(responseCode, is(200));

            try (PrintWriter writer = new PrintWriter("target/" + f_sFormat + ".metrics.txt");
                 InputStream in = con.getInputStream())
                {
                BufferedReader isr = new BufferedReader(new InputStreamReader(in));
                String line;
                int counter = 0;
                while ((line = isr.readLine()) != null )
                    {
                    writer.print(line + "\n");
                    // limit example size otherwise verification takes too much time to complete
                    if (counter++ == 110)
                        {
                        break;
                        }
                    }
                writer.print("# EOF");
                }
            }
        }

    // ----- data members ----------------------------------------------------

    /**
     * The text description of the format to set.
     */
    private final String f_sFormat;

    /**
     * The system property to set to configure the name format.
     */
    private final String f_sProperty;

    /**
     * The value of the system property to set to configure the name format.
     */
    private final boolean f_fPropertyValue;
    }

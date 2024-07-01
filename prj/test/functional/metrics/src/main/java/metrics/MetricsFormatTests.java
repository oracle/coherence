/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.util.Capture;
import com.tangosol.internal.metrics.MetricsHttpHandler;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class MetricsFormatTests
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
        parameters.add(new Object[]{"None Specified", "foo", false, MetricsHttpHandler.Format.Default});
        // Run Coherence with the default metrics format configured
        parameters.add(new Object[]{"Default", MetricsHttpHandler.PROP_USE_LEGACY_NAMES, false, MetricsHttpHandler.Format.Default});
        // Run Coherence with the legacy metrics format configured
        parameters.add(new Object[]{"Legacy", MetricsHttpHandler.PROP_USE_LEGACY_NAMES, true, MetricsHttpHandler.Format.Legacy});
        // Run Coherence with the MP metrics format configured
        parameters.add(new Object[]{"MP", MetricsHttpHandler.PROP_USE_MP_NAMES, true, MetricsHttpHandler.Format.Microprofile});
        // Run Coherence with the dot delimited metrics format configured
        parameters.add(new Object[]{"Dot Delimited", MetricsHttpHandler.PROP_USE_DOT_NAMES, true, MetricsHttpHandler.Format.DotDelimited});

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
    public MetricsFormatTests(String sFormat, String sProperty, boolean fValue, MetricsHttpHandler.Format format)
        {
        f_sFormat        = sFormat;
        f_sProperty      = sProperty;
        f_fPropertyValue = fValue;
        f_format         = format;
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void shouldHaveCorrectNameFormat() throws Exception
        {
        LocalPlatform    platform = LocalPlatform.get();
        Capture<Integer> port     = new Capture<>(platform.getAvailablePorts());

        // start Coherence with metrics configured with the test format
        try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                    SystemProperty.of(f_sProperty, f_fPropertyValue),
                    SystemProperty.of(MetricsHttpHelper.PROP_METRICS_ENABLED, true),
                    SystemProperty.of("coherence.metrics.http.port", port),
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

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())))
                {
                String sLine;
                while ((sLine = in.readLine()) != null)
                    {
                    switch (f_format)
                        {
                        case Default:
                            assertThat(sLine, startsWith("coherence_"));
                            break;
                        case Legacy:
                            assertThat(sLine, startsWith("vendor:coherence_"));
                            break;
                        case Microprofile:
                            assertThat(sLine, startsWith("vendor_Coherence_"));
                            break;
                        case DotDelimited:
                            assertThat(sLine, startsWith("coherence."));
                            break;
                        default:
                            fail("Unrecognised metrics format " + f_format);
                        }
                    }
                }
            }
        }

    // ----- data members ---------------------------------------------------

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

    /**
     * The expected metrics format.
     */
    private final MetricsHttpHandler.Format f_format;
    }

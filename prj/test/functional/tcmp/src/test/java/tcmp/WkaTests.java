/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package tcmp;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;
import com.oracle.bedrock.runtime.java.options.JavaHome;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import common.AbstractFunctionalTest;

import org.hamcrest.Matchers;

import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

/**
 * Tests of the Well Know Addresses
 *
 * @author lh 2020.08.27
 */
public class WkaTests
        extends AbstractFunctionalTest
    {
    /**
     * Test that a warning message is issued when WKA includes a port.
     */
    @Test
    public void testWkaWithPort()
        {
        CoherenceClusterMember member = null;

        try
            {
            String                      sServer = "WkaTestServer";
            CapturingApplicationConsole console = new CapturingApplicationConsole();

            out(createMessageHeader() + " >>>>>>> Starting cache server: " + sServer);

            OptionsByType optionsByType = OptionsByType.of(DisplayName.of(sServer));
            optionsByType.add(SystemProperty.of("coherence.override", "wka-port-override.xml"));
            optionsByType.add(Console.of(console));

            String sJavaHome = System.getProperty("server.java.home");
            if (sJavaHome != null)
                {
                optionsByType.add(JavaHome.at(sJavaHome));
                }

            member = LocalPlatform.get().launch(CoherenceClusterMember.class, optionsByType.asArray());

            out(createMessageHeader() + " >>>>>>> Started cache server: " + sServer);

            Eventually.assertThat(invoking(member).getClusterSize(), is(1));
            Eventually.assertThat(invoking(console).getCapturedOutputLines(), Matchers.hasItem(containsString("The use of <socket-address> for the <well-known-addresses> element is deprecated")));
            }
        catch (Exception e)
            {
            throw new RuntimeException("Error starting cache server", e);
            }

        if (member != null)
            {
            member.close();
            }
        }
    }

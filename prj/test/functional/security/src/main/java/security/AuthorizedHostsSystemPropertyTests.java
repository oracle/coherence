/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;
import com.oracle.bedrock.runtime.java.options.JavaHome;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;


/**
 * Functional test for system property coherence.authorized.hosts.
 *
 * @author jf  2025.04.16
 */
public class AuthorizedHostsSystemPropertyTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("java.security.auth.login.config", "login.config");
        }

    @Test
    public void testAccessAllowed() throws Exception
        {
        Properties props = new Properties();

        props.setProperty("coherence.authorized.hosts", "nonexistent.dns.address,baddomain.badhost,127.0.0.1");

        CoherenceClusterMember clusterMember = startCacheServer("AuthorizedHostAddressSystemPropertyTestsAccessAllowed",
                                                                "security", null, props, false);
        try
            {
            Eventually.assertThat(invoking(clusterMember).getClusterSize(), is(1));
            }
        finally
            {
            if (clusterMember != null)
                {
                stopCacheServer("AuthorizedHostAddressSystemPropertyTestsAccessAllowed");
                }
            }
        }

    @Test
    public void testAccessDenied() throws Exception
        {
        boolean                     fFailed       = false;
        CapturingApplicationConsole console       = new CapturingApplicationConsole();
        OptionsByType               optionsByType = OptionsByType.of(DisplayName.of("AuthorizedHostsSystemPropertyTestsAccessDenied"));

        optionsByType.add(SystemProperty.of("coherence.authorized.hosts", "nonexistent.dns.address,baddomain.badhost,127.0.0.2"));
        optionsByType.add(Console.of(console));

        String sJavaHome = System.getProperty("server.java.home");
        if (sJavaHome != null)
            {
            optionsByType.add(JavaHome.at(sJavaHome));
            }

        CoherenceClusterMember member = LocalPlatform.get().launch(CoherenceClusterMember.class, optionsByType.asArray());
        try
            {
            Eventually.assertThat(invoking(console).getCapturedOutputLines(), Matchers.hasItem(containsString("This member is not authorized to join the cluster.")));
            }
        catch (Exception e)
            {
            fFailed = true;
            throw e;
            }
        finally
            {
            if (fFailed)
                {
                System.out.println("Console Output Lines: " + console.getCapturedOutputLines());
                }
            }
        }
    }

/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.nio.file.Files;
import java.nio.file.Path;

import java.io.IOException;

import java.security.PrivilegedAction;

import java.util.Properties;

import javax.security.auth.Subject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Extend identity-token boundary tests.
 *
 * @author OpenAI  2026.05.16
 */
public class IdentityTokenSecurityTests
        extends AbstractFunctionalTest
    {
    public IdentityTokenSecurityTests()
        {
        super("client-cache-config-java-identity.xml");
        }

    @BeforeClass
    public static void _startup()
        {
        try
            {
            s_pathMarker = Files.createTempFile("coherence-malicious-identity-", ".marker");
            Files.deleteIfExists(s_pathMarker);
            }
        catch (IOException e)
            {
            throw new RuntimeException("failed to create malicious identity marker", e);
            }

        System.setProperty("coherence.override", "security-malicious-identity-override.xml");
        System.setProperty("java.security.auth.login.config", "login.config");
        System.setProperty("test.malicious.identity.marker", s_pathMarker.toString());
        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.setProperty("test.server.classname", "security.SubjectCacheServer");
        props.setProperty("test.malicious.identity.marker", s_pathMarker.toString());

        CoherenceClusterMember clusterMember = startCacheServer("IdentityTokenSecurityTests", "security",
                "server-cache-config-java-identity.xml", props);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("TcpProxyService"), is(true));
        }

    @AfterClass
    public static void stopServer() throws Exception
        {
        stopCacheServer("IdentityTokenSecurityTests");
        if (s_pathMarker != null)
            {
            Files.deleteIfExists(s_pathMarker);
            }
        System.clearProperty("test.malicious.identity.marker");
        }

    @Test
    public void shouldRejectMaliciousIdentityTokenBeforeMaterialization() throws Exception
        {
        Subject subject = NameServiceSecurityTests.loginJAAS("manager", "password");

        try
            {
            Subject.doAs(subject, (PrivilegedAction<Object>) () ->
                {
                getFactory().ensureService("TcpProxyService");
                return null;
                });
            fail("expected malicious identity token to be rejected");
            }
        catch (RuntimeException expected)
            {
            // expected
            }

        assertFalse(Files.exists(s_pathMarker));
        }

    private static Path s_pathMarker;
    }

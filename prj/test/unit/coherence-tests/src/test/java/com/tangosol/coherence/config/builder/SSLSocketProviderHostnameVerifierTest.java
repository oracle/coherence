/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.oracle.coherence.testing.SystemPropertyIsolation;
import com.oracle.coherence.testing.util.CoherenceModeHelper;
import com.oracle.coherence.testing.util.SSLSocketProviderBuilderHelper;

import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Rule;
import org.junit.Test;

import java.net.InetAddress;
import java.net.Socket;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for mode-aware SSL hostname verifier defaults.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public class SSLSocketProviderHostnameVerifierTest
    {
    @Test
    public void shouldAllowNullVerifierInLegacy()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            provider(null).ensureSessionValidity(rejectingSession(), peerSocket());
            }
        }

    @Test
    public void shouldRejectNullVerifierInDev()
            throws Exception
        {
        assertNullVerifierRejected(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldRejectNullVerifierInProd()
            throws Exception
        {
        assertNullVerifierRejected(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldPreserveCustomVerifier()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            HostnameVerifier verifier = (sHost, session) -> true;

            SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder builder =
                    new SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder();
            builder.setBuilder((resolver, loader, list) -> verifier);

            assertThat(builder.realize(null, null, null), is(sameInstance(verifier)));
            }
        }

    @Test
    public void shouldAllowActionAllowInLegacy()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            HostnameVerifier verifier = allowActionBuilder(false).realize(null, null, null);

            assertThat(verifier.verify("not-a-cert-name.example.com", rejectingSession()), is(true));
            }
        }

    @Test
    public void shouldRejectExplicitActionAllowInDev()
        {
        assertExplicitAllowRejected(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldRejectExplicitActionAllowInProd()
        {
        assertExplicitAllowRejected(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldTreatSystemPropertyDefaultActionAllowAsDefaultInDev()
        {
        assertSystemPropertyDefaultIsDefaultVerifier(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldTreatSystemPropertyDefaultActionAllowAsDefaultInProd()
        {
        assertSystemPropertyDefaultIsDefaultVerifier(CoherenceModeHelper.securityHardened());
        }

    @Test
    public void shouldResolveHostnameFallbackInBothXmlLoaders()
        {
        System.clearProperty(PROP_HOSTNAME_VERIFICATION);
        for (String sMode : new String[]{"hardened", "compatibility"})
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityMode(sMode))
                {
                for (boolean fLegacy : new boolean[]{false, true})
                    {
                    HostnameVerifier verifier = loadVerifier(hostnameFallback(), fLegacy);
                    assertThat(verifier.verify("not-a-cert-name.example.com", rejectingSession()),
                            is("compatibility".equals(sMode)));
                    }
                }
            }
        }

    @Test
    public void shouldRejectExplicitAllowPropertyInBothXmlLoaders()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            System.setProperty(PROP_HOSTNAME_VERIFICATION, "allow");
            for (boolean fLegacy : new boolean[]{false, true})
                {
                assertThrows(IllegalArgumentException.class, () -> loadVerifier(hostnameFallback(), fLegacy));
                }
            }
        }

    @Test
    public void shouldRejectExplicitAllowXmlInBothXmlLoaders()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            for (boolean fLegacy : new boolean[]{false, true})
                {
                assertThrows(IllegalArgumentException.class, () -> loadVerifier(
                        XmlHelper.loadXml("<hostname-verifier><action>allow</action></hostname-verifier>"), fLegacy));
                }
            }
        }

    @Test
    public void shouldRetainExplicitAllowOverrideAfterLegacyDefaultResolution()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            System.clearProperty(PROP_HOSTNAME_VERIFICATION);
            XmlElement xmlBase = XmlHelper.loadXml("<ssl><hostname-verifier><action system-property=\""
                    + PROP_HOSTNAME_VERIFICATION + "\">allow</action></hostname-verifier></ssl>");
            XmlHelper.replaceSystemProperties(xmlBase, "system-property");
            XmlElement xmlOverride = XmlHelper.loadXml(
                    "<ssl><hostname-verifier><action>allow</action></hostname-verifier></ssl>");
            XmlHelper.replaceSystemProperties(xmlOverride, "system-property");
            XmlHelper.overrideElement(xmlBase, xmlOverride, "id");

            assertThrows(IllegalArgumentException.class,
                    () -> loadVerifier(xmlBase.getElement("hostname-verifier"), false));
            }
        }

    @Test
    public void shouldPreserveExplicitDefaultPropertyInLegacyLoader()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            System.setProperty(PROP_HOSTNAME_VERIFICATION, "default");
            HostnameVerifier verifier = loadVerifier(hostnameFallback(), true);
            assertThat(verifier.verify("not-a-cert-name.example.com", rejectingSession()), is(false));
            }
        }

    @Test
    public void shouldNotTreatOtherPropertyFallbackAsShippedDefault()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            System.clearProperty("test.ssl.hostname.verification");
            XmlElement xml = XmlHelper.loadXml("<hostname-verifier>"
                    + "<action system-property=\"test.ssl.hostname.verification\">allow</action>"
                    + "</hostname-verifier>");
            assertThrows(IllegalArgumentException.class, () -> loadVerifier(xml, true));
            }
        }

    private XmlElement hostnameFallback()
        {
        return XmlHelper.loadXml("<hostname-verifier><action system-property=\""
                + PROP_HOSTNAME_VERIFICATION + "\">allow</action></hostname-verifier>");
        }

    private HostnameVerifier loadVerifier(XmlElement xml, boolean fLegacy)
        {
        if (fLegacy)
            {
            XmlHelper.replaceSystemProperties(xml, "system-property");
            }
        try (DefaultProcessingContext context = SSLSocketProviderBuilderHelper.createDefaultProcessingContext(xml))
            {
            SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder builder =
                    (SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder) context.processDocument(xml);
            return builder.realize(null, null, null);
            }
        }

    @Test
    public void shouldAllowCompletedHandshakeWithoutOptionalClientIdentity()
            throws Exception
        {
        for (String sMode : new String[] {null, "hardened", "compatibility"})
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityMode(sMode))
                {
                for (SSLSocketProvider.ClientAuthMode mode : new SSLSocketProvider.ClientAuthMode[]
                        {SSLSocketProvider.ClientAuthMode.none, SSLSocketProvider.ClientAuthMode.wanted})
                    {
                    provider(null, mode).ensureSessionValidity(anonymousSession(true), peerSocket(), false);
                    }
                }
            }
        }

    @Test
    public void shouldStillRequireServerIdentityOnClient()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            for (SSLSocketProvider.ClientAuthMode mode : SSLSocketProvider.ClientAuthMode.values())
                {
                assertThrows(SSLException.class,
                        () -> provider(null, mode).ensureSessionValidity(anonymousSession(true), peerSocket(), true));
                }
            }
        }

    @Test
    public void shouldStillRequireMandatoryClientIdentity()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(SSLException.class, () -> provider(null, SSLSocketProvider.ClientAuthMode.required)
                    .ensureSessionValidity(anonymousSession(true), peerSocket(), false));
            }
        }

    @Test
    public void shouldNotAcceptIncompleteAnonymousHandshake()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            for (SSLSocketProvider.ClientAuthMode mode : new SSLSocketProvider.ClientAuthMode[]
                    {SSLSocketProvider.ClientAuthMode.none, SSLSocketProvider.ClientAuthMode.wanted})
                {
                assertThrows(SSLException.class,
                        () -> provider(null, mode).ensureSessionValidity(anonymousSession(false), peerSocket(), false));
                }
            }
        }

    @Test
    public void shouldStillVerifyProvidedClientIdentity()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            X509Certificate cert = mock(X509Certificate.class);
            when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=wrong.example.invalid"));
            SSLSession session = mock(SSLSession.class);
            when(session.isValid()).thenReturn(true);
            when(session.getPeerCertificates()).thenReturn(new Certificate[] {cert});

            for (SSLSocketProvider.ClientAuthMode mode : new SSLSocketProvider.ClientAuthMode[]
                    {SSLSocketProvider.ClientAuthMode.none, SSLSocketProvider.ClientAuthMode.wanted})
                {
                assertThrows(SSLException.class,
                        () -> provider(null, mode).ensureSessionValidity(session, peerSocket(), false));
                }
            }
        }

    @Test
    public void shouldPreserveExplicitVerifierForAnonymousClient()
            throws Exception
        {
        for (String sMode : new String[] {"hardened", "compatibility"})
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityMode(sMode))
                {
                for (SSLSocketProvider.ClientAuthMode mode : new SSLSocketProvider.ClientAuthMode[]
                        {SSLSocketProvider.ClientAuthMode.none, SSLSocketProvider.ClientAuthMode.wanted})
                    {
                    HostnameVerifier verifier = (sHost, session) -> false;
                    assertThrows(SSLException.class, () -> provider(verifier, mode)
                            .ensureSessionValidity(anonymousSession(true), peerSocket(), false));
                    }
                }
            }
        }

    private SSLSocketProvider provider(HostnameVerifier verifier, SSLSocketProvider.ClientAuthMode mode)
        {
        return new SSLSocketProvider(new SSLSocketProvider.DefaultDependencies()
                .setHostnameVerifier(verifier).setClientAuth(mode));
        }

    private SSLSession anonymousSession(boolean fValid)
        {
        SSLSession session = rejectingSession();
        when(session.isValid()).thenReturn(fValid);
        return session;
        }

    private void assertNullVerifierRejected(CoherenceModeHelper.ModeScope scope)
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = scope)
            {
            try
                {
                provider(null).ensureSessionValidity(rejectingSession(), peerSocket());
                fail("Expected SSLException");
                }
            catch (SSLException expected)
                {
                // expected
                }
            }
        }

    private void assertExplicitAllowRejected(CoherenceModeHelper.ModeScope scope)
        {
        try (CoherenceModeHelper.ModeScope ignored = scope)
            {
            try
                {
                allowActionBuilder(false).realize(null, null, null);
                fail("Expected IllegalArgumentException");
                }
            catch (IllegalArgumentException expected)
                {
                // expected
                }
            }
        }

    private void assertSystemPropertyDefaultIsDefaultVerifier(CoherenceModeHelper.ModeScope scope)
        {
        try (CoherenceModeHelper.ModeScope ignored = scope)
            {
            HostnameVerifier verifier = allowActionBuilder(true).realize(null, null, null);

            assertThat(verifier, instanceOf(SSLSocketProviderDependenciesBuilder.DefaultHostnameVerifier.class));
            assertThat(verifier.verify("not-a-cert-name.example.com", rejectingSession()), is(false));
            }
        }

    private SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder allowActionBuilder(boolean fDefault)
        {
        SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder builder =
                new SSLSocketProviderDependenciesBuilder.HostnameVerifierBuilder();
        builder.setAction(SSLSocketProviderDependenciesBuilder.ACTION_ALLOW);
        builder.setSystemPropertyDefault(fDefault);
        return builder;
        }

    private SSLSocketProvider provider(HostnameVerifier verifier)
        {
        return new SSLSocketProvider(new SSLSocketProvider.DefaultDependencies().setHostnameVerifier(verifier));
        }

    private Socket peerSocket()
            throws Exception
        {
        Socket socket = mock(Socket.class);
        when(socket.getInetAddress()).thenReturn(InetAddress.getByName("203.0.113.10"));
        return socket;
        }

    private SSLSession rejectingSession()
        {
        try
            {
            SSLSession session = mock(SSLSession.class);
            when(session.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("peer not verified"));
            when(session.getCipherSuite()).thenReturn("TLS_FAKE_WITH_NULL_NULL");
            return session;
            }
        catch (SSLPeerUnverifiedException e)
            {
            throw new IllegalStateException(e);
            }
        }
    private static final String PROP_HOSTNAME_VERIFICATION = "coherence.security.hostname.verification";

    @Rule
    public final SystemPropertyIsolation f_properties = new SystemPropertyIsolation();

    }

/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.InetSocketAddress32;
import com.oracle.coherence.common.net.InetSocketAddressWithHostname;
import com.oracle.coherence.common.net.InetAddresses;
import com.oracle.coherence.common.net.TcpSocketProvider;
import com.oracle.coherence.common.internal.net.ssl.SSLCertUtility;
import com.oracle.coherence.common.internal.net.ssl.SSLSocket;
import com.oracle.coherence.common.internal.net.ssl.SSLSocketChannel;

import com.oracle.coherence.testing.SystemPropertyIsolation;
import com.oracle.coherence.testing.util.CoherenceModeHelper;
import com.oracle.coherence.testing.util.SSLSocketProviderBuilderHelper;

import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Rule;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import java.nio.channels.SocketChannel;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.spi.SelectorProvider;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateParsingException;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;

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

    @Test
    public void shouldMatchIPv4SubjectAlternativeName()
            throws Exception
        {
        HostnameVerifier verifier = SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier(false);
        assertThat(verifier.verify("127.0.0.1", certificateSession("unrelated", List.of(7, "127.0.0.1"))), is(true));
        assertThat(verifier.verify("127.0.0.2", certificateSession("unrelated", List.of(7, "127.0.0.1"))), is(false));
        }

    @Test
    public void shouldMatchIPv6SubjectAlternativeNameByAddress()
            throws Exception
        {
        HostnameVerifier verifier = SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier(false);
        SSLSession session = certificateSession("unrelated", List.of(7, "0:0:0:0:0:0:0:1"));
        for (String sAddress : new String[] {"::1", "[::1]", "0:0:0:0:0:0:0:1", "::1%1"})
            {
            assertThat(sAddress, verifier.verify(sAddress, session), is(true));
            }
        assertThat(verifier.verify("::2", session), is(false));
        assertThat(verifier.verify("127.0.0.1", session), is(false));
        }

    @Test
    public void shouldMatchIPv4MappedIPv6SubjectAlternativeName()
            throws Exception
        {
        HostnameVerifier verifier = SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier(false);
        assertThat(verifier.verify("::ffff:127.0.0.1", certificateSession("unrelated", List.of(7, "127.0.0.1"))), is(true));
        assertThat(verifier.verify("127.0.0.1", certificateSession("unrelated", List.of(7, "::ffff:127.0.0.1"))), is(true));
        assertThat(verifier.verify("::ffff:127.0.0.2", certificateSession("unrelated", List.of(7, "127.0.0.1"))), is(false));
        }

    @Test
    public void shouldRequireIPSANForNumericIdentity()
            throws Exception
        {
        HostnameVerifier verifier = SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier(false);
        assertThat(verifier.verify("127.0.0.1", certificateSession("127.0.0.1")), is(false));
        assertThat(verifier.verify("127.0.0.1", certificateSession("127.0.0.1", List.of(2, "127.0.0.1"))), is(false));
        assertThat(verifier.verify("::1", certificateSession("unrelated", List.of(2, "::1"))), is(false));
        }

    @Test
    public void shouldNotResolveDNSIdentityAgainstIPSAN()
            throws Exception
        {
        HostnameVerifier verifier = SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier(false);
        SSLSession session = certificateSession("unrelated", List.of(2, "localhost"), List.of(7, "127.0.0.1"));
        assertThat(verifier.verify("localhost", session), is(true));
        assertThat(verifier.verify("localhost.localdomain", session), is(false));
        }

    @Test
    public void shouldRejectMalformedIPSAN()
            throws Exception
        {
        HostnameVerifier verifier = SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier(false);
        for (String sSAN : new String[] {"localhost", "256.0.0.1", "127.0.0", "127.0.0.1.example", "bad:address"})
            {
            assertThat(sSAN, verifier.verify("127.0.0.1", certificateSession("unrelated", List.of(7, sSAN))), is(false));
            }
        }

    @Test
    public void shouldRejectUnparseableCertificateSAN()
            throws Exception
        {
        SSLSession session = certificateSession("127.0.0.1", List.of(7, "127.0.0.1"));
        X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
        when(cert.getSubjectAlternativeNames()).thenThrow(new CertificateParsingException("invalid SAN"));
        assertThat(SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier(false)
                .verify("127.0.0.1", session), is(false));
        }

    @Test
    public void shouldOnlyMatchPeerLeafCertificate()
            throws Exception
        {
        SSLSession session = certificateSession("unrelated", List.of(7, "127.0.0.2"));
        Certificate certLeaf = session.getPeerCertificates()[0];
        Certificate certCA = certificateSession("issuer", List.of(7, "127.0.0.1")).getPeerCertificates()[0];
        when(session.getPeerCertificates()).thenReturn(new Certificate[] {certLeaf, certCA});
        assertThat(SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier(false)
                .verify("127.0.0.1", session), is(false));
        }

    @Test
    public void shouldVerifyAcceptedPeerWithoutReverseDNS()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityMode(null))
            {
            SSLSession session = certificateSession("unrelated", List.of(7, "127.0.0.1"));
            for (String sName : new String[] {null, "localhost", "localhost.localdomain", "127.0.0.1"})
                {
                Socket socket = loopbackSocket(sName);
                provider(null, SSLSocketProvider.ClientAuthMode.required)
                        .ensureSessionValidity(session, socket, false);
                assertThrows(SSLException.class, () -> provider(null, SSLSocketProvider.ClientAuthMode.required)
                        .ensureSessionValidity(certificateSession("unrelated", List.of(7, "127.0.0.2")), socket, false));
                }
            }
        }

    @Test
    public void shouldPreserveConfiguredDNSIdentity()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityMode(null))
            {
            Socket socket = loopbackSocket("localhost.localdomain");
            provider(null).ensureSessionValidity(certificateSession("unrelated", List.of(2, "service.example")),
                    socket, true, "service.example");
            assertThrows(SSLException.class, () -> provider(null).ensureSessionValidity(
                    certificateSession("unrelated", List.of(2, "other.example"), List.of(7, "127.0.0.1")),
                    socket, true, "service.example"));
            }
        }

    @Test
    public void shouldRejectCachedDNSInsteadOfAcceptedPeerIPSAN()
            throws Exception
        {
        try (var mode = CoherenceModeHelper.securityMode(null))
            {
            Socket socket = loopbackSocket("cached.example");
            SSLSocketProvider provider = provider(null, SSLSocketProvider.ClientAuthMode.required);
            SSLException error = assertThrows(SSLException.class, () -> provider.ensureSessionValidity(
                    certificateSession("cached.example", List.of(2, "cached.example")), socket, false));
            assertThat(error.getMessage().startsWith("Unacceptable peer"), is(true));
            assertThrows(SSLException.class, () -> provider.ensureSessionValidity(
                    certificateSession("unrelated", List.of(7, "127.0.0.1")), socket, true));
            }
        }

    @Test
    public void shouldPreserveExplicitServerIdentityContext()
            throws Exception
        {
        try (var mode = CoherenceModeHelper.securityMode(null))
            {
            Socket socket = loopbackSocket("cached.example");
            SSLSocketProvider provider = provider(null, SSLSocketProvider.ClientAuthMode.required);
            provider.ensureSessionValidity(certificateSession("unrelated", List.of(2, "service.example")),
                    socket, false, "service.example");
            assertThrows(SSLException.class, () -> provider.ensureSessionValidity(
                    certificateSession("unrelated", List.of(2, "other.example"), List.of(7, "127.0.0.1")),
                    socket, false, "service.example"));
            }
        }

    @Test
    public void shouldRestoreNestedServerIdentityAndPreserveLegacyPolicy()
            throws Exception
        {
        try (var mode = CoherenceModeHelper.securityMode(null))
            {
            Socket outer = loopbackSocket("cached.example");
            Socket inner = loopbackSocket("cached.example");
            SSLSession outerSession = certificateSession("unrelated", List.of(2, "outer.example"));
            SSLSession innerSession = certificateSession("unrelated", List.of(7, "127.0.0.1"));
            SSLSession wrongSession = certificateSession("unrelated", List.of(7, "127.0.0.2"));
            var dependencies = new SSLSocketProvider.DefaultDependencies()
                    .setClientAuth(SSLSocketProvider.ClientAuthMode.required);
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            SSLSocketProvider nested = new SSLSocketProvider(dependencies)
                {
                @Override
                public void ensureSessionValidity(SSLSession session, Socket socket) throws SSLException
                    {
                    calls.incrementAndGet();
                    if (socket == outer)
                        {
                        ensureSessionValidity(innerSession, inner, false);
                        assertThrows(SSLException.class, () -> ensureSessionValidity(wrongSession, inner, false));
                        }
                    super.ensureSessionValidity(session, socket);
                    }
                };
            nested.ensureSessionValidity(outerSession, outer, true, "outer.example");
            assertThat(calls.get(), is(3));
            assertThrows(SSLException.class, () -> nested.ensureSessionValidity(outerSession, outer));

            SSLSocketProvider rejecting = new SSLSocketProvider(dependencies)
                {
                @Override
                public void ensureSessionValidity(SSLSession session, Socket socket) throws SSLException
                    {
                    throw new SSLException("legacy two-argument policy");
                    }
                };
            SSLException error = assertThrows(SSLException.class,
                    () -> rejecting.ensureSessionValidity(innerSession, inner, false));
            assertThat(error.getMessage(), is("legacy two-argument policy"));
            }
        }

    @Test
    public void shouldRetainHostnameInExtendedSocketAddress()
            throws Exception
        {
        InetSocketAddress32 addrNumeric = new InetSocketAddress32("127.0.0.1", 30000);
        addrNumeric.getAddress().getHostName();
        assertThat(SSLSocketProvider.getPeerName(addrNumeric), is("127.0.0.1"));

        InetAddress addrNamed = InetAddress.getByAddress("service.example", new byte[] {127, 0, 0, 1});
        InetSocketAddress32 addrDNS = new InetSocketAddress32(
                new InetSocketAddressWithHostname("service.example", addrNamed, 30000));
        assertThat(SSLSocketProvider.getPeerName(addrDNS), is("service.example"));
        InetSocketAddress32 addrUnresolved = new InetSocketAddress32(InetSocketAddress.createUnresolved("service.example", 30000));
        assertThat(addrUnresolved.isUnresolved(), is(true));
        assertThat(SSLSocketProvider.getPeerName(addrUnresolved), is("service.example"));
        }

    @Test
    public void shouldParseLiteralsWithoutResolvingDNSNames()
        {
        for (String sName : new String[] {null, "", "localhost", "localhost.localdomain", "256.0.0.1",
                "127.0.0.1.example", "localhost%::1", "bad:address", "localhost:1", "xyz::1"})
            {
            assertThat(sName, SSLCertUtility.getIPAddress(sName), is((byte[]) null));
            }
        assertThat(SSLCertUtility.getIPAddress("127.0.0.1"), is(new byte[] {127, 0, 0, 1}));
        assertThat(SSLCertUtility.getIPAddress("::ffff:127.0.0.1"), is(new byte[] {127, 0, 0, 1}));
        }

    @Test
    public void shouldRequireIPSANForAllJavaDecimalIPv4Forms()
            throws Exception
        {
        HostnameVerifier verifier = SSLSocketProviderDependenciesBuilder.createDefaultHostnameVerifier(false);
        for (String sAddress : new String[] {"2130706433", "127.1", "127.0.1", "127.0000.0.1"})
            {
            assertThat(SSLCertUtility.getIPAddress(sAddress), is(InetAddress.getByName(sAddress).getAddress()));
            assertThat(verifier.verify(sAddress, certificateSession("unrelated", List.of(7, "127.0.0.1"))), is(true));
            assertThat(verifier.verify(sAddress, certificateSession(sAddress, List.of(2, sAddress))), is(false));
            assertThat(SSLSocketProvider.getPeerName(new InetSocketAddress32(sAddress, 30000)), is("127.0.0.1"));
            }
        for (String sAddress : new String[] {"4294967296", "256.1", "127.0.65536", "127.0.0.256",
                "127..1", "0x7f000001", "127.0.0.1.", "localhost:1", "[localhost]"})
            {
            assertThat(SSLCertUtility.getIPAddress(sAddress), is((byte[]) null));
            assertThat(verifier.verify(sAddress, certificateSession("unrelated", List.of(2, sAddress))), is(false));
            }
        }

    @Test
    public void shouldNeverResolveFromLiteralParser()
            throws Exception
        {
        InetAddress resolved = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        try (var addresses = mockStatic(InetAddress.class))
            {
            addresses.when(() -> InetAddress.getByName("localhost:1")).thenReturn(resolved);
            assertThat(SSLCertUtility.getIPAddress("localhost:1"), is((byte[]) null));
            assertThat(SSLCertUtility.getIPAddress("::1"), is(new byte[] {0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 1}));
            assertThat(SSLCertUtility.getIPAddress("127.1"), is(new byte[] {127, 0, 0, 1}));
            for (String sName : new String[] {"localhost:1", "xyz::1", "bad:address", "[::1", "::1]",
                    "1:::2", "1::2::3", "192.0.2.1::", "1:2:3:4:5:6:7:8::", "::1%", "::1%a%b"})
                {
                assertThat(sName, SSLCertUtility.getIPAddress(sName), is((byte[]) null));
                }
            addresses.verifyNoInteractions();
            }
        }

    @Test
    public void shouldMatchIPv6ParserToJavaLiteralBytes()
            throws Exception
        {
        for (String sLiteral : new String[] {"::", "::1", "1::", "2001:db8::1234", "fe80::1",
                "1:2:3:4:5:6:7:8", "1:2:3:4:5:6:192.0.2.1", "::ffff:c000:201", "::192.0.2.1"})
            {
            assertThat(sLiteral, SSLCertUtility.getIPAddress(sLiteral), is(InetAddress.getByName(sLiteral).getAddress()));
            }
        }

    @Test
    public void shouldRejectAmbiguousNamesWithoutChangingNumericIntent()
            throws Exception
        {
        InetAddress address = InetAddress.getByAddress("cached.example", new byte[] {127, 0, 0, 1});
        InetSocketAddress ordinary = new InetSocketAddress(address, 30000);
        InetSocketAddress32 converted = new InetSocketAddress32(ordinary);
        for (SocketAddress peer : new SocketAddress[] {ordinary, converted,
                new InetSocketAddress32(converted, 30001), InetAddresses.setPort(ordinary, 30001),
                InetAddresses.setPort(converted, 30001)})
            {
            assertThat(InetAddresses.hasKnownReferenceIdentity(peer), is(false));
            assertThrows(SSLException.class, () -> SSLSocketProvider.getPeerName(peer));
            }
        // the InetAddress overload explicitly requests numeric identity
        assertThat(SSLSocketProvider.getPeerName(new InetSocketAddress32(address, 30000)), is("127.0.0.1"));
        assertThat(SSLSocketProvider.getPeerName(new InetSocketAddress("127.0.0.1", 30000)), is("127.0.0.1"));
        }

    @Test
    public void shouldRejectOrdinaryResolvedDNSAndWrappedCopies()
            throws Exception
        {
        InetSocketAddress ordinary = new InetSocketAddress("localhost", 30000);
        assertThat(ordinary.isUnresolved(), is(false));
        var wrapped = new com.tangosol.net.internal.WrapperSocketAddressProvider(ordinary, 1);
        for (SocketAddress peer : new SocketAddress[] {ordinary, new InetSocketAddress32(ordinary),
                wrapped.getNextAddress()})
            {
            SSLException error = assertThrows(SSLException.class, () -> SSLSocketProvider.getPeerName(peer));
            assertThat(error.getMessage().startsWith("Ambiguous peer identity"), is(true));
            }
        }

    @Test
    public void shouldRetainAmbiguityThroughDefaultPortSubstitution()
            throws Exception
        {
        InetSocketAddress ordinary = new InetSocketAddress("localhost", 0);
        com.tangosol.net.AddressProvider delegate = mock(com.tangosol.net.AddressProvider.class);
        when(delegate.getNextAddress()).thenReturn(ordinary);
        SocketAddress regular = new com.tangosol.net.internal.SubstitutionAddressProvider(delegate, 30000).getNextAddress();
        assertThat(InetAddresses.getPort(regular), is(30000));
        assertThrows(SSLException.class, () -> SSLSocketProvider.getPeerName(regular));
        com.tangosol.net.SocketAddressProvider extended = mock(com.tangosol.net.SocketAddressProvider.class);
        when(extended.getNextAddress()).thenReturn(new InetSocketAddress32(ordinary));
        SocketAddress changed = new com.tangosol.net.internal.SubstitutionSocketAddressProvider(
                extended, ordinary.getAddress(), 30000, -1).getNextAddress();
        assertThat(InetAddresses.getPort(changed), is(30000));
        assertThrows(SSLException.class, () -> SSLSocketProvider.getPeerName(changed));
        }

    @Test
    public void shouldRetainExplicitNamesThroughAddressConversion()
            throws Exception
        {
        InetSocketAddress named = new InetSocketAddressWithHostname("service.example",
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 30000);
        for (SocketAddress address : new SocketAddress[] {named, new InetSocketAddress32(named),
                new InetSocketAddress32(new InetSocketAddress32(named), 30001),
                InetAddresses.setPort(named, 30001), InetAddresses.setPort(new InetSocketAddress32(named), 30001)})
            {
            assertThat(SSLSocketProvider.getPeerName(address), is("service.example"));
            }
        assertThat(SSLSocketProvider.getPeerName(TcpSocketProvider.INSTANCE.resolveAddress("localhost:30000")), is("localhost"));
        var provider = new com.tangosol.net.ConfigurableAddressProvider(
                List.of(new com.tangosol.net.ConfigurableAddressProvider.AddressHolder("localhost", 30000)), true);
        assertThat(SSLSocketProvider.getPeerName(provider.getNextAddress()), is("localhost"));
        var wrapper = new com.tangosol.net.internal.WrapperSocketAddressProvider(named, 1);
        assertThat(SSLSocketProvider.getPeerName(wrapper.getNextAddress()), is("service.example"));
        }

    @Test
    public void shouldPreserveReferenceProvenanceAcrossSerialization()
            throws Exception
        {
        for (InetSocketAddress32 address : new InetSocketAddress32[] {
                new InetSocketAddress32(new InetSocketAddress("localhost", 30000)),
                new InetSocketAddress32(InetSocketAddress.createUnresolved("service.example", 30000)),
                new InetSocketAddress32("127.0.0.1", 30000)})
            {
            var bytes = new java.io.ByteArrayOutputStream();
            try (var output = new java.io.ObjectOutputStream(bytes))
                {
                output.writeObject(address);
                }
            try (var input = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())))
                {
                input.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(
                        "com.oracle.coherence.common.net.InetSocketAddress32;java.net.*;java.lang.*;!*"));
                InetSocketAddress32 restored = (InetSocketAddress32) input.readObject();
                assertThat(restored.hasKnownReferenceIdentity(), is(address.hasKnownReferenceIdentity()));
                if (restored.hasKnownReferenceIdentity())
                    {
                    assertThat(SSLSocketProvider.getPeerName(restored), is(SSLSocketProvider.getPeerName(address)));
                    }
                else
                    {
                    assertThrows(SSLException.class, () -> SSLSocketProvider.getPeerName(restored));
                    }
                }
            }
        }

    @Test
    public void shouldReadLegacyAddressesWithUnknownReferenceIdentity()
            throws Exception
        {
        try (var input = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(
                Base64.getDecoder().decode(LEGACY_ADDRESSES))))
            {
            input.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(
                    "com.oracle.coherence.common.net.InetSocketAddress32;java.net.*;java.lang.*;!*"));
            InetSocketAddress32[] addresses = (InetSocketAddress32[]) input.readObject();
            assertThat(addresses.length, is(4));
            for (int i = 0; i < addresses.length; i++)
                {
                InetSocketAddress32 address = addresses[i];
                assertThat(address.getPort(), is(i == 0 ? 0x12345678 : 30000));
                assertThat(address.isUnresolved(), is(i == 2));
                assertThat(address.getHostString(), is(i == 2 ? "service.example" : "127.0.0.1"));
                if (!address.isUnresolved())
                    {
                    assertThat(address.getAddress().getAddress(), is(new byte[] {127, 0, 0, 1}));
                    }
                assertThat(address.hasKnownReferenceIdentity(), is(false));
                assertThrows(SSLException.class, () -> SSLSocketProvider.getPeerName(address));
                SocketAddress changedPort = InetAddresses.setPort(address, 30001);
                assertThat(InetAddresses.getPort(changedPort), is(30001));
                assertThrows(SSLException.class, () -> SSLSocketProvider.getPeerName(changedPort));
                }
            }
        }

    @Test
    public void shouldRetainKnownIdentityThroughBothSubstitutionProviders()
            throws Exception
        {
        InetSocketAddress named = new InetSocketAddressWithHostname("service.example",
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0);
        com.tangosol.net.AddressProvider delegate = mock(com.tangosol.net.AddressProvider.class);
        when(delegate.getNextAddress()).thenReturn(named);
        SocketAddress regular = new com.tangosol.net.internal.SubstitutionAddressProvider(delegate, 30000).getNextAddress();
        assertThat(InetAddresses.getPort(regular), is(30000));
        assertThat(SSLSocketProvider.getPeerName(regular), is("service.example"));
        com.tangosol.net.SocketAddressProvider extended = mock(com.tangosol.net.SocketAddressProvider.class);
        when(extended.getNextAddress()).thenReturn(new InetSocketAddress32(named));
        SocketAddress changed = new com.tangosol.net.internal.SubstitutionSocketAddressProvider(
                extended, named.getAddress(), 30000, -1).getNextAddress();
        assertThat(InetAddresses.getPort(changed), is(30000));
        assertThat(SSLSocketProvider.getPeerName(changed), is("service.example"));
        }

    @Test
    public void shouldRetainLegacyValidationDispatchAndRestoreNestedIdentity()
            throws Exception
        {
        SSLSession outerSession = certificateSession("unrelated", List.of(2, "outer.example"));
        SSLSession innerSession = certificateSession("unrelated", List.of(2, "inner.example"));
        Socket outer = loopbackSocket("cached.example");
        Socket inner = loopbackSocket("cached.example");
        var dependencies = new SSLSocketProvider.DefaultDependencies().setClientAuth(SSLSocketProvider.ClientAuthMode.none);
        SSLSocketProvider nested = new SSLSocketProvider(dependencies)
            {
            @Override
            public void ensureSessionValidity(SSLSession session, Socket socket, boolean fClientMode)
                    throws SSLException
                {
                if (socket == outer)
                    {
                    ensureSessionValidity(innerSession, inner, true, "inner.example");
                    }
                super.ensureSessionValidity(session, socket, fClientMode);
                }
            };
        nested.ensureSessionValidity(outerSession, outer, true, "outer.example");
        assertThrows(SSLException.class, () -> nested.ensureSessionValidity(outerSession, outer));

        SSLSocketProvider rejectingTwo = new SSLSocketProvider(dependencies)
            {
            @Override
            public void ensureSessionValidity(SSLSession session, Socket socket) throws SSLException
                {
                assertThat(socket, sameInstance(outer));
                throw new SSLException("legacy two-argument policy");
                }
            };
        assertThrows(SSLException.class, () -> rejectingTwo.ensureSessionValidity(outerSession, outer, true, "outer.example"));
        SSLSocketProvider rejectingThree = new SSLSocketProvider(dependencies)
            {
            @Override
            public void ensureSessionValidity(SSLSession session, Socket socket, boolean fClientMode) throws SSLException
                {
                assertThat(socket, sameInstance(outer));
                throw new SSLException("legacy three-argument policy");
                }
            };
        assertThrows(SSLException.class, () -> rejectingThree.ensureSessionValidity(outerSession, outer, true, "outer.example"));
        }

    @Test
    public void shouldRetainDestinationThroughBlockingTLSConnect()
            throws Exception
        {
        for (String sPeer : new String[] {"127.0.0.1", "service.example"})
            {
            SSLSession session = certificateSession("unrelated", List.of("127.0.0.1".equals(sPeer) ? 7 : 2, sPeer));
            SSLContext context = mock(SSLContext.class);
            SSLSocketFactory factory = mock(SSLSocketFactory.class);
            javax.net.ssl.SSLSocket sslSocket = mock(javax.net.ssl.SSLSocket.class);
            Socket socket = loopbackSocket("localhost.localdomain");
            when(context.getSocketFactory()).thenReturn(factory);
            when(factory.createSocket(socket, sPeer, 30000, true)).thenReturn(sslSocket);
            when(sslSocket.getSession()).thenReturn(session);
            SSLSocketProvider provider = new SSLSocketProvider(new SSLSocketProvider.DefaultDependencies().setSSLContext(context)
                        .setClientAuth(SSLSocketProvider.ClientAuthMode.none));
            try (SSLSocket client = new SSLSocket(socket, provider))
                {
                client.connect(InetSocketAddress32.createUnresolved(sPeer, 30000), 1000);
                verify(factory).createSocket(socket, sPeer, 30000, true);
                }
            }
        }

    @Test
    public void shouldIsolateConcurrentValidationIdentities()
            throws Exception
        {
        SSLSession first  = certificateSession("unrelated", List.of(2, "first.example"));
        SSLSession second = certificateSession("unrelated", List.of(2, "second.example"));
        Socket firstSocket  = loopbackSocket("cached.example");
        Socket secondSocket = loopbackSocket("cached.example");
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        HostnameVerifier verifier = (sName, session) ->
            {
            try
                {
                barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                return sName.equals(session == first ? "first.example" : "second.example");
                }
            catch (Exception e)
                {
                throw new IllegalStateException(e);
                }
            };
        SSLSocketProvider provider = provider(verifier);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2))
            {
            var a = executor.submit(() ->
                {
                provider.ensureSessionValidity(first, firstSocket, true, "first.example");
                return null;
                });
            var b = executor.submit(() ->
                {
                provider.ensureSessionValidity(second, secondSocket, true, "second.example");
                return null;
                });
            a.get(7, java.util.concurrent.TimeUnit.SECONDS);
            b.get(7, java.util.concurrent.TimeUnit.SECONDS);
            }
        }

    @Test
    public void shouldRetainDestinationThroughNIOTLSConnect()
            throws Exception
        {
        for (boolean fSocketConnect : new boolean[] {false, true})
            {
            for (String sPeer : new String[] {"127.0.0.1", "service.example"})
                {
                SSLSession session = certificateSession("unrelated", List.of("127.0.0.1".equals(sPeer) ? 7 : 2, sPeer));
                Socket socket = loopbackSocket("localhost.localdomain");
                SocketChannel delegate = mock(SocketChannel.class);
                when(delegate.provider()).thenReturn(SelectorProvider.provider());
                when(delegate.socket()).thenReturn(socket);
                SSLContext context = mock(SSLContext.class);
                SSLEngine engine = mock(SSLEngine.class);
                when(context.createSSLEngine()).thenReturn(engine);
                when(engine.getSession()).thenReturn(session);
                when(engine.getUseClientMode()).thenReturn(true);
                SSLSocketProvider provider = new SSLSocketProvider(new SSLSocketProvider.DefaultDependencies().setSSLContext(context)
                        .setClientAuth(SSLSocketProvider.ClientAuthMode.none));
                try (var client = new SSLSocketChannel(delegate, provider)
                    {
                    public void validate()
                            throws SSLException
                        {
                        validatePeer();
                        }
                    })
                    {
                    SocketAddress address = InetSocketAddress32.createUnresolved(sPeer, 30000);
                    if (fSocketConnect)
                        {
                        doAnswer(invocation ->
                            {
                            client.validate();
                            return null;
                            }).when(socket).connect(address, 1000);
                        client.socket().connect(address, 1000);
                        }
                    else
                        {
                        when(delegate.connect(address)).thenAnswer(invocation ->
                            {
                            client.validate();
                            return false;
                            });
                        client.connect(address);
                        }
                    client.validate();

                    when(delegate.isConnected()).thenReturn(true);
                    SocketAddress other = InetSocketAddress32.createUnresolved("other.example", 30000);
                    when(delegate.connect(other)).thenThrow(new AlreadyConnectedException());
                    assertThrows(AlreadyConnectedException.class, () -> client.connect(other));
                    client.validate();
                    when(delegate.isConnected()).thenReturn(false);
                    when(delegate.isConnectionPending()).thenReturn(true);
                    org.mockito.Mockito.doThrow(new java.nio.channels.ConnectionPendingException()).when(delegate).connect(other);
                    assertThrows(java.nio.channels.ConnectionPendingException.class, () -> client.connect(other));
                    client.validate();
                    }
                }
            }
        }

    @Test
    public void shouldPreserveCustomVerifierWithRetainedIdentity()
            throws Exception
        {
        HostnameVerifier verifier = mock(HostnameVerifier.class);
        SSLSession session = certificateSession("unrelated", List.of(7, "127.0.0.1"));
        when(verifier.verify("service.example", session)).thenReturn(false);
        assertThrows(SSLException.class, () -> provider(verifier)
                .ensureSessionValidity(session, loopbackSocket("localhost.localdomain"), true, "service.example"));
        verify(verifier).verify("service.example", session);
        }

    private Socket loopbackSocket(String sName)
            throws Exception
        {
        Socket socket = mock(Socket.class);
        InetAddress address = InetAddress.getByAddress(sName, new byte[] {127, 0, 0, 1});
        when(socket.getInetAddress()).thenReturn(address);
        when(socket.getRemoteSocketAddress()).thenReturn(new InetSocketAddress(address, 30000));
        when(socket.getPort()).thenReturn(30000);
        return socket;
        }

    private SSLSession certificateSession(String sCommonName, List<?>... aSAN)
            throws Exception
        {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=" + sCommonName));
        when(cert.getIssuerX500Principal()).thenReturn(new X500Principal("CN=test-issuer"));
        when(cert.getSubjectAlternativeNames()).thenReturn(Arrays.asList(aSAN));
        SSLSession session = mock(SSLSession.class);
        when(session.isValid()).thenReturn(true);
        when(session.getPeerCertificates()).thenReturn(new Certificate[] {cert});
        return session;
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

    // serialized by the original InetSocketAddress32 (UID -3271050419013917201)
    private static final String LEGACY_ADDRESSES =
            "rO0ABXVyADZbTGNvbS5vcmFjbGUuY29oZXJlbmNlLmNvbW1vbi5uZXQuSW5ldFNvY2tldEFkZHJlc3MzMjvUr9V5MrT5kgIAAHhw"
             + "AAAABHNyADNjb20ub3JhY2xlLmNvaGVyZW5jZS5jb21tb24ubmV0LkluZXRTb2NrZXRBZGRyZXNzMzLSmuT5hEhl7wIAA0kABHBv"
             + "cnRMAARhZGRydAAWTGphdmEvbmV0L0luZXRBZGRyZXNzO0wACGhvc3RuYW1ldAASTGphdmEvbGFuZy9TdHJpbmc7eHIAFmphdmEu"
             + "bmV0LlNvY2tldEFkZHJlc3NIYfZi9JedagIAAHhwEjRWeHNyABRqYXZhLm5ldC5JbmV0QWRkcmVzcy2bV6+f4+vbAwADSQAHYWRk"
             + "cmVzc0kABmZhbWlseUwACGhvc3ROYW1lcQB+AAR4cH8AAAEAAAACcHhwc3EAfgACAAB1MHNxAH4AB38AAAEAAAACdAAOY2FjaGVk"
             + "LmV4YW1wbGV4cHNxAH4AAgAAdTBwdAAPc2VydmljZS5leGFtcGxlc3EAfgACAAB1MHNxAH4AB38AAAEAAAACcHh0AAkxMjcuMC4w"
             + "LjE=";

    @Rule
    public final SystemPropertyIsolation f_properties = new SystemPropertyIsolation();

    }

/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.grpc.GrpcDiagnosticsPolicy;
import com.tangosol.net.grpc.GrpcTransportSecurity;

import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.SaxParser;
import com.tangosol.run.xml.XmlDocument;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultGrpcAcceptorDependencies}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.04
 */
public class GrpcAcceptorDependenciesTest
    {
    @Test
    public void shouldDefaultAuthMethodToNone()
        {
        DefaultGrpcAcceptorDependencies deps = new DefaultGrpcAcceptorDependencies();

        assertEquals("none", deps.getAuthMethod());
        assertEquals(GrpcDiagnosticsPolicy.CHANNELZ_AUTO, deps.getChannelz());
        assertEquals(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_AUTO, deps.getErrorDisclosure());
        assertEquals(GrpcTransportSecurity.SECURE_TRANSPORT_OPTIONAL, deps.getSecureTransport());
        deps.validate();
        }

    @Test
    public void shouldAcceptBasicAuthMethod()
        {
        DefaultGrpcAcceptorDependencies deps = populate(new DefaultGrpcAcceptorDependencies());
        deps.setAuthMethod(" Basic ");

        assertEquals("basic", deps.getAuthMethod());
        deps.validate();
        }

    @Test
    public void shouldRejectUnsupportedAuthMethod()
        {
        DefaultGrpcAcceptorDependencies deps = populate(new DefaultGrpcAcceptorDependencies());
        deps.setAuthMethod("cert");

        assertThrows(IllegalArgumentException.class, deps::validate);
        }

    @Test
    public void shouldAcceptRequiredSecureTransport()
        {
        DefaultGrpcAcceptorDependencies deps = populate(new DefaultGrpcAcceptorDependencies());
        deps.setSecureTransport(" Required ");

        assertEquals(GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED, deps.getSecureTransport());
        deps.validate();
        }

    @Test
    public void shouldAcceptExplicitDiagnosticsPolicies()
        {
        DefaultGrpcAcceptorDependencies deps = populate(new DefaultGrpcAcceptorDependencies());
        deps.setChannelz(" Disabled ");
        deps.setErrorDisclosure(" Safe ");

        assertEquals(GrpcDiagnosticsPolicy.CHANNELZ_DISABLED, deps.getChannelz());
        assertEquals(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE, deps.getErrorDisclosure());
        deps.validate();
        }

    @Test
    public void shouldRejectUnsupportedChannelz()
        {
        DefaultGrpcAcceptorDependencies deps = populate(new DefaultGrpcAcceptorDependencies());

        assertThrows(IllegalArgumentException.class, () -> deps.setChannelz("on"));
        }

    @Test
    public void shouldRejectUnsupportedErrorDisclosure()
        {
        DefaultGrpcAcceptorDependencies deps = populate(new DefaultGrpcAcceptorDependencies());

        assertThrows(IllegalArgumentException.class, () -> deps.setErrorDisclosure("verbose"));
        }

    @Test
    public void shouldRejectUnsupportedSecureTransport()
        {
        DefaultGrpcAcceptorDependencies deps = populate(new DefaultGrpcAcceptorDependencies());

        assertThrows(IllegalArgumentException.class, () -> deps.setSecureTransport("mandatory"));
        }

    @Test
    public void shouldParseGrpcAuthMethodFromXml()
            throws Exception
        {
        DefaultGrpcAcceptorDependencies deps = fromXml(new SocketProviderFactory(),
                "<acceptor-config>"
              + "  <grpc-acceptor>"
              + "    <auth-method>basic</auth-method>"
              + "    <channelz>disabled</channelz>"
              + "    <error-disclosure>safe</error-disclosure>"
              + "    <secure-transport>required</secure-transport>"
              + "  </grpc-acceptor>"
              + "</acceptor-config>");

        assertEquals("basic", deps.getAuthMethod());
        assertEquals(GrpcDiagnosticsPolicy.CHANNELZ_DISABLED, deps.getChannelz());
        assertEquals(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE, deps.getErrorDisclosure());
        assertEquals(GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED, deps.getSecureTransport());
        }

    @Test
    public void shouldPreserveNamedSslProviderMetadataFromXml()
            throws Exception
        {
        SocketProviderFactory.DefaultDependencies depsFactory = new SocketProviderFactory.DefaultDependencies();
        depsFactory.addNamedSSLDependencies(SocketProviderFactory.Dependencies.ProviderType.SSL.getName(),
                new SSLSocketProvider.DefaultDependencies());
        SocketProviderFactory factory = new SocketProviderFactory(depsFactory);

        DefaultGrpcAcceptorDependencies deps = fromXml(factory,
                "<acceptor-config>"
              + "  <grpc-acceptor>"
              + "    <socket-provider>ssl</socket-provider>"
              + "    <secure-transport>required</secure-transport>"
              + "  </grpc-acceptor>"
              + "</acceptor-config>");

        assertEquals(GrpcTransportSecurity.Transport.TLS, GrpcTransportSecurity.enforce(
                deps.getSocketProviderBuilder(), deps.getSecureTransport()));
        }

    @Test
    public void shouldPreserveInlineSslProviderMetadataFromXml()
            throws Exception
        {
        DefaultGrpcAcceptorDependencies deps = fromXml(new SocketProviderFactory(),
                "<acceptor-config>"
              + "  <grpc-acceptor>"
              + "    <socket-provider><ssl/></socket-provider>"
              + "    <secure-transport>required</secure-transport>"
              + "  </grpc-acceptor>"
              + "</acceptor-config>");

        assertEquals(GrpcTransportSecurity.Transport.TLS, GrpcTransportSecurity.enforce(
                deps.getSocketProviderBuilder(), deps.getSecureTransport()));
        }

    @Test
    public void shouldPreserveGrpcInsecureProviderMetadataFromXml()
            throws Exception
        {
        DefaultGrpcAcceptorDependencies deps = fromXml(new SocketProviderFactory(),
                "<acceptor-config>"
              + "  <grpc-acceptor>"
              + "    <socket-provider>grpc-insecure</socket-provider>"
              + "  </grpc-acceptor>"
              + "</acceptor-config>");

        assertEquals(GrpcTransportSecurity.Transport.INSECURE_EXPLICIT, GrpcTransportSecurity.enforce(
                deps.getSocketProviderBuilder(), deps.getSecureTransport()));
        }

    @Test
    public void shouldRejectRequiredGrpcInsecureProviderFromXml()
            throws Exception
        {
        DefaultGrpcAcceptorDependencies deps = fromXml(new SocketProviderFactory(),
                "<acceptor-config>"
              + "  <grpc-acceptor>"
              + "    <socket-provider>grpc-insecure</socket-provider>"
              + "    <secure-transport>required</secure-transport>"
              + "  </grpc-acceptor>"
              + "</acceptor-config>");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> GrpcTransportSecurity.enforce(deps.getSocketProviderBuilder(), deps.getSecureTransport()));

        assertEquals(GrpcTransportSecurity.SECURE_REQUIRED_MESSAGE, e.getMessage());
        }

    @Test
    public void shouldTreatDefaultProviderFromXmlAsOptionalCleartext()
            throws Exception
        {
        DefaultGrpcAcceptorDependencies deps = fromXml(new SocketProviderFactory(),
                "<acceptor-config>"
              + "  <grpc-acceptor/>"
              + "</acceptor-config>");

        SocketProviderBuilder builder = deps.getSocketProviderBuilder();
        assertEquals(GrpcTransportSecurity.Transport.INSECURE_COMPAT, GrpcTransportSecurity.enforce(
                builder, deps.getSecureTransport()));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> GrpcTransportSecurity.enforce(builder, GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED));

        assertEquals(GrpcTransportSecurity.SECURE_REQUIRED_MESSAGE, e.getMessage());
        }

    @Test
    public void shouldTreatTcpProviderFromXmlAsOptionalCleartext()
            throws Exception
        {
        DefaultGrpcAcceptorDependencies deps = fromXml(new SocketProviderFactory(),
                "<acceptor-config>"
              + "  <grpc-acceptor>"
              + "    <socket-provider>tcp</socket-provider>"
              + "  </grpc-acceptor>"
              + "</acceptor-config>");

        SocketProviderBuilder builder = deps.getSocketProviderBuilder();
        assertEquals(GrpcTransportSecurity.Transport.INSECURE_COMPAT, GrpcTransportSecurity.enforce(
                builder, deps.getSecureTransport()));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> GrpcTransportSecurity.enforce(builder, GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED));

        assertEquals(GrpcTransportSecurity.SECURE_REQUIRED_MESSAGE, e.getMessage());
        }

    @Test
    public void shouldRejectMissingSslDependenciesFromXmlWhenOptional()
            throws Exception
        {
        DefaultGrpcAcceptorDependencies deps = fromXml(new SocketProviderFactory(),
                "<acceptor-config>"
              + "  <grpc-acceptor>"
              + "    <socket-provider>ssl</socket-provider>"
              + "  </grpc-acceptor>"
              + "</acceptor-config>");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> GrpcTransportSecurity.enforce(deps.getSocketProviderBuilder(), deps.getSecureTransport()));

        assertEquals(GrpcTransportSecurity.MISSING_TLS_CREDENTIALS_MESSAGE, e.getMessage());
        }

    @Test
    public void shouldAllowGrpcAuthMethodInCacheConfigSchema()
            throws Exception
        {
        validateCacheConfigSchema("coherence-cache-config.xsd");
        }

    @Test
    public void shouldDeclareGrpcAuthMethodInVersionedWebCacheConfigSchemas()
            throws Exception
        {
        assertVersionedWebSchemas("coherence-xsd/web/coherence-cache-config", "coherence-cache-config.xsd",
                "<xsd:element name=\"grpc-acceptor\"",
                GrpcAcceptorDependenciesTest::assertVersionedWebSchemaDeclaresGrpcAuthMethod);
        }

    @Test
    public void shouldDeclareGrpcSecureTransportInVersionedWebConfigBaseSchemas()
            throws Exception
        {
        assertVersionedWebSchemas("coherence-xsd/web/coherence-config-base", "coherence-config-base.xsd",
                "name=\"grpc-channel-type\"",
                GrpcAcceptorDependenciesTest::assertVersionedWebConfigBaseSchemaDeclaresGrpcDiagnostics);
        }

    private static void validateCacheConfigSchema(String sSchemaLocation)
            throws Exception
        {
        String sXml = "<?xml version=\"1.0\"?>"
                + "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\""
                + " xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config "
                + sSchemaLocation + "\">"
                + "  <caching-scheme-mapping>"
                + "    <cache-mapping>"
                + "      <cache-name>*</cache-name>"
                + "      <scheme-name>grpc-proxy</scheme-name>"
                + "    </cache-mapping>"
                + "  </caching-scheme-mapping>"
                + "  <caching-schemes>"
                + "    <proxy-scheme>"
                + "      <scheme-name>grpc-proxy</scheme-name>"
                + "      <service-name>GrpcProxy</service-name>"
                + "      <acceptor-config>"
                + "        <grpc-acceptor>"
                + "          <auth-method>basic</auth-method>"
                + "          <channelz>disabled</channelz>"
                + "          <error-disclosure>safe</error-disclosure>"
                + "          <secure-transport>required</secure-transport>"
                + "        </grpc-acceptor>"
                + "      </acceptor-config>"
                + "    </proxy-scheme>"
                + "  </caching-schemes>"
                + "</cache-config>";

        XmlDocument xml = new SimpleParser(false).parseXml(sXml);
        new SaxParser().validateXsd(sXml, xml);
        }

    private static void assertVersionedWebSchemaDeclaresGrpcAuthMethod(Path path)
            throws Exception
        {
        String sSchema = Files.readString(path);
        int    iStart  = sSchema.indexOf("<xsd:element name=\"grpc-acceptor\"");
        int    iEnd    = sSchema.indexOf("<xsd:element name=\"grpc-controller\"", iStart);

        if (iStart < 0 || iEnd < 0)
            {
            throw new AssertionError("Cannot locate grpc-acceptor declaration in " + path);
            }

        String sGrpcAcceptor = sSchema.substring(iStart, iEnd);
        if (!sGrpcAcceptor.contains("<xsd:element minOccurs=\"0\" ref=\"auth-method\"/>")
                && !sGrpcAcceptor.contains("<xsd:element ref=\"auth-method\" minOccurs=\"0\" />"))
            {
            throw new AssertionError("Versioned web schema does not declare grpc auth-method in " + path);
            }
        if (!sGrpcAcceptor.contains("<xsd:element minOccurs=\"0\" ref=\"secure-transport\"/>")
                && !sGrpcAcceptor.contains("<xsd:element ref=\"secure-transport\" minOccurs=\"0\" />"))
            {
            throw new AssertionError("Versioned web schema does not declare grpc secure-transport in " + path);
            }
        if (!sGrpcAcceptor.contains("<xsd:element minOccurs=\"0\" ref=\"channelz\"/>")
                && !sGrpcAcceptor.contains("<xsd:element ref=\"channelz\" minOccurs=\"0\" />"))
            {
            throw new AssertionError("Versioned web schema does not declare grpc channelz in " + path);
            }
        if (!sGrpcAcceptor.contains("<xsd:element minOccurs=\"0\" ref=\"error-disclosure\"/>")
                && !sGrpcAcceptor.contains("<xsd:element ref=\"error-disclosure\" minOccurs=\"0\" />"))
            {
            throw new AssertionError("Versioned web schema does not declare grpc error-disclosure in " + path);
            }
        }

    private static void assertVersionedWebConfigBaseSchemaDeclaresGrpcDiagnostics(Path path)
            throws Exception
        {
        String sSchema = Files.readString(path);
        int    iStart  = sSchema.indexOf("name=\"grpc-channel-type\"");
        int    iEnd    = sSchema.indexOf("</xsd:complexType>", iStart);

        if (iStart < 0 || iEnd < 0)
            {
            throw new AssertionError("Cannot locate grpc-channel-type declaration in " + path);
            }

        String sGrpcChannel = sSchema.substring(iStart, iEnd);
        if (!sGrpcChannel.contains("<xsd:element minOccurs=\"0\" ref=\"secure-transport\"/>")
                && !sGrpcChannel.contains("<xsd:element ref=\"secure-transport\" minOccurs=\"0\"/>")
                && !sGrpcChannel.contains("<xsd:element ref=\"secure-transport\" minOccurs=\"0\" />"))
            {
            throw new AssertionError("Versioned web schema does not declare grpc-channel secure-transport in " + path);
            }
        if (!sSchema.contains("<xsd:element name=\"channelz\" type=\"coherence-string-type\">"))
            {
            throw new AssertionError("Versioned web schema does not declare grpc channelz element in " + path);
            }
        if (!sSchema.contains("<xsd:element name=\"error-disclosure\" type=\"coherence-string-type\">"))
            {
            throw new AssertionError("Versioned web schema does not declare grpc error-disclosure element in " + path);
            }
        }

    private static DefaultGrpcAcceptorDependencies fromXml(SocketProviderFactory factory, String sXml)
            throws Exception
        {
        DefaultGrpcAcceptorDependencies deps = new DefaultGrpcAcceptorDependencies();
        XmlDocument                     xml  = new SimpleParser().parseXml(sXml);
        OperationalContext              ctx  = mock(OperationalContext.class);

        when(ctx.getSocketProviderFactory()).thenReturn(factory);
        LegacyXmlGrpcAcceptorHelper.fromXml(xml, deps, ctx, GrpcAcceptorDependenciesTest.class.getClassLoader());
        return deps;
        }

    private static Path findProjectRoot()
        {
        Path path = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (path != null)
            {
            if (Files.isDirectory(path.resolve("coherence-xsd")))
                {
                return path;
                }
            path = path.getParent();
            }
        throw new IllegalStateException("Cannot find project root from " + System.getProperty("user.dir"));
        }

    private static void assertVersionedWebSchemas(String sDirectory, String sFileName, String sMarker,
            SchemaAssertion assertion)
            throws Exception
        {
        Path root = findProjectRoot().resolve(sDirectory);
        try (Stream<Path> paths = Files.list(root))
            {
            Path[] schemas = paths
                    .map(path -> path.resolve(sFileName))
                    .filter(Files::isRegularFile)
                    .toArray(Path[]::new);
            for (Path schema : schemas)
                {
                if (Files.readString(schema).contains(sMarker))
                    {
                    assertion.accept(schema);
                    }
                }
            }
        }

    @FunctionalInterface
    private interface SchemaAssertion
        {
        void accept(Path path) throws Exception;
        }

    private static DefaultGrpcAcceptorDependencies populate(DefaultGrpcAcceptorDependencies deps)
        {
        AcceptorDependenciesTest.populate(deps);
        deps.setController(null);
        return deps;
        }
    }

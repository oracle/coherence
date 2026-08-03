/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.SaxParser;
import com.tangosol.run.xml.XmlDocument;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultGrpcAcceptorDependencies}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
public class GrpcAcceptorDependenciesTest
    {
    @Test
    public void shouldDefaultAuthMethodToNone()
        {
        DefaultGrpcAcceptorDependencies deps = new DefaultGrpcAcceptorDependencies();

        assertEquals("none", deps.getAuthMethod());
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
    public void shouldParseGrpcAuthMethodFromXml()
            throws Exception
        {
        DefaultGrpcAcceptorDependencies deps = new DefaultGrpcAcceptorDependencies();
        XmlDocument xml = new SimpleParser().parseXml(
                "<acceptor-config>"
              + "  <grpc-acceptor>"
              + "    <auth-method>basic</auth-method>"
              + "  </grpc-acceptor>"
              + "</acceptor-config>");

        OperationalContext context = mock(OperationalContext.class);
        when(context.getSocketProviderFactory()).thenReturn(new SocketProviderFactory());

        LegacyXmlGrpcAcceptorHelper.fromXml(xml, deps, context,
                getClass().getClassLoader());

        assertEquals("basic", deps.getAuthMethod());
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
        Path root = findProjectRoot();
        assertVersionedWebSchemas(root.resolve("coherence-xsd/web/coherence-cache-config"),
                "coherence-cache-config.xsd", GrpcAcceptorDependenciesTest::assertVersionedWebSchemaDeclaresGrpcAuthMethod);
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
                + "        </grpc-acceptor>"
                + "      </acceptor-config>"
                + "    </proxy-scheme>"
                + "  </caching-schemes>"
                + "</cache-config>";

        XmlDocument xml = new SimpleParser(false).parseXml(sXml);
        new SaxParser().validateXsd(sXml, xml);
        }

    private static void assertVersionedWebSchemas(Path dir, String sSchemaName, SchemaAssertion assertion)
            throws Exception
        {
        boolean fFound = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, Files::isDirectory))
            {
            for (Path pathVersion : stream)
                {
                Path pathSchema = pathVersion.resolve(sSchemaName);
                if (Files.exists(pathSchema))
                    {
                    fFound |= assertion.assertSchema(pathSchema);
                    }
                }
            }

        if (!fFound)
            {
            throw new AssertionError("Cannot locate versioned web schema " + sSchemaName + " under " + dir);
            }
        }

    private static boolean assertVersionedWebSchemaDeclaresGrpcAuthMethod(Path path)
            throws Exception
        {
        String sSchema = Files.readString(path);
        int    iStart  = sSchema.indexOf("<xsd:element name=\"grpc-acceptor\"");
        int    iEnd    = sSchema.indexOf("<xsd:element name=\"grpc-controller\"", iStart);

        if (iStart < 0 || iEnd < 0)
            {
            return false;
            }

        String sGrpcAcceptor = sSchema.substring(iStart, iEnd);
        if (!sGrpcAcceptor.contains("<xsd:element minOccurs=\"0\" ref=\"auth-method\"/>")
                && !sGrpcAcceptor.contains("<xsd:element ref=\"auth-method\" minOccurs=\"0\" />"))
            {
            throw new AssertionError("Versioned web schema does not declare grpc auth-method in " + path);
            }
        return true;
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

    private static DefaultGrpcAcceptorDependencies populate(DefaultGrpcAcceptorDependencies deps)
        {
        AcceptorDependenciesTest.populate(deps);
        deps.setController(null);
        return deps;
        }

    @FunctionalInterface
    private interface SchemaAssertion
        {
        boolean assertSchema(Path path)
                throws Exception;
        }
    }

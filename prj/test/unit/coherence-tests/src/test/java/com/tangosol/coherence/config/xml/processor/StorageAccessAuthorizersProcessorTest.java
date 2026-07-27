/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;

import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.NamespaceHandler;

import com.tangosol.internal.net.cluster.DefaultClusterDependencies;
import com.tangosol.internal.net.security.StorageAccessAuthorizerBuilder;

import com.tangosol.net.security.AuditingAuthorizer;
import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for storage-authorizer operational policy parsing.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 15.1.2.0
 */
public class StorageAccessAuthorizersProcessorTest
    {
    @Before
    public void init()
        {
        m_deps = new DefaultClusterDependencies();

        DocumentProcessor.DefaultDependencies dependencies =
                new DocumentProcessor.DefaultDependencies(new OperationalConfigNamespaceHandler());
        dependencies.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);

        m_ctxClusterConfig = new DefaultProcessingContext(dependencies, null);

        NamespaceHandler handler = dependencies.getDefaultNamespaceHandler();
        if (handler != null)
            {
            m_ctxClusterConfig.ensureNamespaceHandler("", handler);
            }

        dependencies.setResourceRegistry(new SimpleResourceRegistry());
        m_ctxClusterConfig.addCookie(ParameterizedBuilderRegistry.class, m_deps.getBuilderRegistry());
        m_ctxClusterConfig.addCookie(DefaultClusterDependencies.class, m_deps);
        }

    @Test
    public void shouldDefaultSubjectProofRequiredToFalse()
        {
        processStorageAuthorizers("<storage-authorizers>"
                + "<storage-authorizer id=\"auditing\">"
                + "<class-name>com.tangosol.net.security.AuditingAuthorizer</class-name>"
                + "</storage-authorizer>"
                + "</storage-authorizers>");

        StorageAccessAuthorizerBuilder builder = getBuilder("auditing");

        assertFalse(builder.isSubjectProofRequired());
        assertSame(AuditingAuthorizer.class, builder.realize(null, null, null).getClass());
        }

    @Test
    public void shouldParseExplicitSubjectProofRequired()
        {
        processStorageAuthorizers("<storage-authorizers>"
                + "<storage-authorizer id=\"strict-auditing\" subject-proof-required=\"true\">"
                + "<class-name>com.tangosol.net.security.AuditingAuthorizer</class-name>"
                + "</storage-authorizer>"
                + "</storage-authorizers>");

        assertTrue(getBuilder("strict-auditing").isSubjectProofRequired());
        }

    @Test
    public void shouldValidateSubjectProofRequiredOnStorageAuthorizer()
            throws Exception
        {
        assertSchemaValid("<storage-authorizers>"
                + "<storage-authorizer id=\"strict-auditing\" subject-proof-required=\"true\">"
                + "<class-name>com.tangosol.net.security.AuditingAuthorizer</class-name>"
                + "</storage-authorizer>"
                + "</storage-authorizers>");
        }

    @Test
    public void shouldRejectSubjectProofRequiredOnSerializer()
            throws Exception
        {
        assertSchemaInvalid("<serializers>"
                + "<serializer id=\"java\" subject-proof-required=\"true\">"
                + "<class-name>com.tangosol.io.DefaultSerializer</class-name>"
                + "</serializer>"
                + "</serializers>");
        }

    private void processStorageAuthorizers(String sXml)
        {
        XmlElement xml = XmlHelper.loadXml(sXml);
        new DefaultProcessingContext(m_ctxClusterConfig, xml).processDocument(xml);
        }

    private StorageAccessAuthorizerBuilder getBuilder(String sName)
        {
        ParameterizedBuilder<StorageAccessAuthorizer> builder = m_deps.getBuilderRegistry()
                .getBuilder(StorageAccessAuthorizer.class, sName);

        assertTrue(builder instanceof StorageAccessAuthorizerBuilder);
        return (StorageAccessAuthorizerBuilder) builder;
        }

    private static void assertSchemaValid(String sClusterConfigChild)
            throws Exception
        {
        validateOperationalConfig(sClusterConfigChild);
        }

    private static void assertSchemaInvalid(String sClusterConfigChild)
            throws Exception
        {
        try
            {
            validateOperationalConfig(sClusterConfigChild);
            }
        catch (Exception e)
            {
            return;
            }
        throw new AssertionError("expected operational schema validation to fail");
        }

    private static void validateOperationalConfig(String sClusterConfigChild)
            throws Exception
        {
        Validator validator = getOperationalSchema().newValidator();
        validator.validate(new StreamSource(new StringReader("<?xml version=\"1.0\"?>"
                + "<coherence xmlns=\"http://xmlns.oracle.com/coherence/coherence-operational-config\">"
                + "<cluster-config>"
                + sClusterConfigChild
                + "</cluster-config>"
                + "</coherence>")));
        }

    private static Schema getOperationalSchema()
            throws Exception
        {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return factory.newSchema(locateCoreOperationalXsd().toFile());
        }

    private static Path locateCoreOperationalXsd()
        {
        Path path = Path.of("").toAbsolutePath();
        while (path != null)
            {
            Path xsd = path.resolve("coherence-core/src/main/resources/coherence-operational-config.xsd");
            if (Files.exists(xsd))
                {
                return xsd;
                }
            path = path.getParent();
            }
        throw new IllegalStateException("cannot locate coherence-operational-config.xsd");
        }

    private DefaultClusterDependencies m_deps;
    private DefaultProcessingContext   m_ctxClusterConfig;
    }

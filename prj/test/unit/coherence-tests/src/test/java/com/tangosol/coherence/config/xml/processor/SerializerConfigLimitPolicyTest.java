/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;
import com.tangosol.coherence.config.xml.OperationalConfigNamespaceHandler;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.NamespaceHandler;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.SerializationLimitPolicy;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.SimpleResourceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for serializer limit config parsing.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.04
 */
public class SerializerConfigLimitPolicyTest
    {
    @Before
    public void init()
        {
        CoherenceModeHelper.securityHardened();
        }

    @After
    public void cleanup()
        {
        System.clearProperty("test.serializer.max.elements");
        CoherenceModeHelper.clear();
        }

    @Test
    public void shouldApplyOperationalSerializerLimits()
        {
        XmlElement xml = XmlHelper.loadXml("<serializer id=\"java\">"
                + "<class-name>com.tangosol.io.DefaultSerializer</class-name>"
                + "<limits><max-elements>17</max-elements></limits>"
                + "</serializer>");

        ParameterizedBuilder<Serializer> builder = new SerializerBuilderProcessor()
                .onProcess(operationalContext(), xml);
        Serializer serializer = builder.realize(new NullParameterResolver(), loader(), null);

        assertTrue(serializer instanceof DefaultSerializer);
        assertEquals(Integer.valueOf(17), serializer.getLimitPolicy().getMaxElements());
        assertEquals(Integer.valueOf(SerializationLimitPolicy.DEFAULT_MAX_MAP_ENTRIES),
                serializer.getLimitPolicy().getMaxMapEntries());
        }

    @Test
    public void shouldPreserveCacheSerializerShortForm()
        {
        SerializerFactory factory = new SerializerFactoryProcessor()
                .onProcess(cacheContext(namedSerializers()), XmlHelper.loadXml("<serializer>pof</serializer>"));

        Serializer serializer = factory.createSerializer(loader());

        assertTrue(serializer instanceof ConfigurablePofContext);
        assertEquals(Integer.valueOf(SerializationLimitPolicy.DEFAULT_MAX_ELEMENTS),
                serializer.getLimitPolicy().getMaxElements());
        }

    @Test
    public void shouldApplyCacheInlineSerializerLimits()
        {
        XmlElement xml = XmlHelper.loadXml("<serializer>"
                + "<instance><class-name>com.tangosol.io.DefaultSerializer</class-name></instance>"
                + "<limits><max-map-entries>3</max-map-entries></limits>"
                + "</serializer>");

        SerializerFactory factory = new SerializerFactoryProcessor()
                .onProcess(cacheContext(namedSerializers()), xml);
        Serializer serializer = factory.createSerializer(loader());

        assertTrue(serializer instanceof DefaultSerializer);
        assertEquals(Integer.valueOf(3), serializer.getLimitPolicy().getMaxMapEntries());
        assertEquals(Integer.valueOf(SerializationLimitPolicy.DEFAULT_MAX_ELEMENTS),
                serializer.getLimitPolicy().getMaxElements());
        }

    @Test
    public void shouldApplyCacheNamedReferenceLimits()
        {
        XmlElement xml = XmlHelper.loadXml("<serializer id=\"pof\">"
                + "<limits><max-container-bytes>256m</max-container-bytes></limits>"
                + "</serializer>");

        SerializerFactory factory = new SerializerFactoryProcessor()
                .onProcess(cacheContext(namedSerializers()), xml);
        Serializer serializer = factory.createSerializer(loader());

        assertTrue(serializer instanceof ConfigurablePofContext);
        assertEquals(Long.valueOf(256L * 1024L * 1024L), serializer.getLimitPolicy().getMaxContainerBytes());
        assertEquals(Integer.valueOf(SerializationLimitPolicy.DEFAULT_MAX_ELEMENTS),
                serializer.getLimitPolicy().getMaxElements());
        }

    @Test
    public void shouldApplyCacheLimitSystemPropertyOverrideThroughPreprocessor()
        {
        System.setProperty("test.serializer.max.elements", "19");

        XmlElement xml = XmlHelper.loadXml("<serializer id=\"pof\">"
                + "<limits><max-elements system-property=\"test.serializer.max.elements\">17</max-elements></limits>"
                + "</serializer>");

        SerializerFactory factory = (SerializerFactory) cacheContext(namedSerializers()).processDocument(xml);

        assertNull(xml.getElement("limits").getElement("max-elements").getAttribute("system-property"));
        assertEquals(Integer.valueOf(19),
                factory.createSerializer(loader()).getLimitPolicy().getMaxElements());
        }

    @Test
    public void shouldApplyCacheLimitSystemPropertyFallbackThroughPreprocessor()
        {
        XmlElement xml = XmlHelper.loadXml("<serializer id=\"pof\">"
                + "<limits><max-elements system-property=\"test.serializer.max.elements\">17</max-elements></limits>"
                + "</serializer>");

        SerializerFactory factory = (SerializerFactory) cacheContext(namedSerializers()).processDocument(xml);

        assertNull(xml.getElement("limits").getElement("max-elements").getAttribute("system-property"));
        assertEquals(Integer.valueOf(17),
                factory.createSerializer(loader()).getLimitPolicy().getMaxElements());
        }

    @Test
    public void shouldApplyOperationalLimitSystemPropertyOverrideThroughPreprocessor()
        {
        System.setProperty("test.serializer.max.elements", "23");

        XmlElement xml = XmlHelper.loadXml("<serializer id=\"java\">"
                + "<class-name>com.tangosol.io.DefaultSerializer</class-name>"
                + "<limits><max-elements system-property=\"test.serializer.max.elements\">17</max-elements></limits>"
                + "</serializer>");

        ParameterizedBuilder<Serializer> builder =
                (ParameterizedBuilder<Serializer>) operationalContext().processDocument(xml);
        Serializer serializer = builder.realize(new NullParameterResolver(), loader(), null);

        assertNull(xml.getElement("limits").getElement("max-elements").getAttribute("system-property"));
        assertEquals(Integer.valueOf(23), serializer.getLimitPolicy().getMaxElements());
        }

    @Test
    public void shouldApplyOperationalLimitSystemPropertyFallbackThroughPreprocessor()
        {
        XmlElement xml = XmlHelper.loadXml("<serializer id=\"java\">"
                + "<class-name>com.tangosol.io.DefaultSerializer</class-name>"
                + "<limits><max-elements system-property=\"test.serializer.max.elements\">17</max-elements></limits>"
                + "</serializer>");

        ParameterizedBuilder<Serializer> builder =
                (ParameterizedBuilder<Serializer>) operationalContext().processDocument(xml);
        Serializer serializer = builder.realize(new NullParameterResolver(), loader(), null);

        assertNull(xml.getElement("limits").getElement("max-elements").getAttribute("system-property"));
        assertEquals(Integer.valueOf(17), serializer.getLimitPolicy().getMaxElements());
        }

    @Test
    public void shouldValidateSerializerLimitsInOperationalSchema()
            throws Exception
        {
        Validator validator = operationalSchema().newValidator();

        validator.validate(new StreamSource(new StringReader("<?xml version=\"1.0\"?>"
                + "<coherence xmlns=\"http://xmlns.oracle.com/coherence/coherence-operational-config\">"
                + "<cluster-config><serializers>"
                + "<serializer id=\"java\">"
                + "<class-name>com.tangosol.io.DefaultSerializer</class-name>"
                + "<limits><max-elements>17</max-elements></limits>"
                + "</serializer>"
                + "</serializers></cluster-config></coherence>")));
        }

    @Test
    public void shouldValidateSerializerLimitsInCacheSchema()
            throws Exception
        {
        Validator validator = cacheSchema().newValidator();

        validator.validate(new StreamSource(new StringReader("<?xml version=\"1.0\"?>"
                + "<cache-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\">"
                + "<defaults><serializer id=\"pof\">"
                + "<limits><max-container-bytes system-property=\"my.limit\">256m</max-container-bytes></limits>"
                + "</serializer></defaults>"
                + "</cache-config>")));
        }

    private static DefaultProcessingContext operationalContext()
        {
        return context(new OperationalConfigNamespaceHandler());
        }

    private static DefaultProcessingContext cacheContext(Map<String, SerializerFactory> mapSerializers)
        {
        OperationalContext ctxOperational = mock(OperationalContext.class);
        when(ctxOperational.getSerializerMap()).thenReturn(mapSerializers);

        DefaultProcessingContext context = context(new CacheConfigNamespaceHandler());
        context.addCookie(OperationalContext.class, ctxOperational);
        return context;
        }

    private static DefaultProcessingContext context(NamespaceHandler handler)
        {
        DocumentProcessor.DefaultDependencies dependencies =
                new DocumentProcessor.DefaultDependencies(handler);
        dependencies.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dependencies.setResourceRegistry(new SimpleResourceRegistry());

        DefaultProcessingContext context = new DefaultProcessingContext(dependencies, (XmlElement) null);
        context.ensureNamespaceHandler("", handler);
        return context;
        }

    private static Map<String, SerializerFactory> namedSerializers()
        {
        return Map.of("pof", loader -> new ConfigurablePofContext());
        }

    private static ClassLoader loader()
        {
        return Thread.currentThread().getContextClassLoader();
        }

    private static Schema operationalSchema()
            throws Exception
        {
        return schema("coherence-operational-config.xsd");
        }

    private static Schema cacheSchema()
            throws Exception
        {
        return schema("coherence-cache-config.xsd");
        }

    private static Schema schema(String sName)
            throws Exception
        {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return factory.newSchema(locateCoreResource(sName).toFile());
        }

    private static Path locateCoreResource(String sName)
        {
        Path path = Path.of("").toAbsolutePath();
        while (path != null)
            {
            Path xsd = path.resolve("coherence-core/src/main/resources").resolve(sName);
            if (Files.exists(xsd))
                {
                return xsd;
                }
            path = path.getParent();
            }
        throw new IllegalStateException("cannot locate " + sName);
        }
    }

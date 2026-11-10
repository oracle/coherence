/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema;

import com.oracle.coherence.common.schema.lang.cpp.CppProperty;
import com.oracle.coherence.common.schema.lang.cpp.CppType;
import com.oracle.coherence.common.schema.lang.dotnet.DotNetProperty;
import com.oracle.coherence.common.schema.lang.dotnet.DotNetType;
import com.oracle.coherence.common.schema.lang.java.JavaProperty;
import com.oracle.coherence.common.schema.lang.java.JavaType;

import com.tangosol.io.pof.schema.PofProperty;
import com.tangosol.io.pof.schema.PofType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.EntityResolver2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests schema metadata import and the DOCTYPE rejection policy of
 * {@link XmlSchemaSource}.
 *
 * @author Vaso Putica  2026.11.05
 * @since 26.1.0.0
 */
public class XmlSchemaSourceTest
    {
    // ----- constructor entry points ---------------------------------------

    @Test
    public void shouldImportTypeAndPropertyFromInputStream()
            throws Exception
        {
        assertOrdinaryMetadata(populate(source(SCHEMA)));
        }

    @Test
    public void shouldImportTypeAndPropertyFromFile()
            throws Exception
        {
        File file = writeFile("normal.xml", SCHEMA);
        assertOrdinaryMetadata(populate(new XmlSchemaSource(file)));
        }

    @Test
    public void shouldImportTypeAndPropertyFromFilename()
            throws Exception
        {
        File file = writeFile("normal.xml", SCHEMA);
        assertOrdinaryMetadata(populate(new XmlSchemaSource(file.getAbsolutePath())));
        }

    @Test
    public void shouldImportTypeAndPropertyFromClasspathResource()
            throws Exception
        {
        withClasspathSchema(SCHEMA, source -> assertOrdinaryMetadata(populate(source)));
        }

    @Test
    public void shouldRejectDoctypeFromFile()
            throws Exception
        {
        File file = writeFile("doctype.xml", DOCTYPE + SCHEMA);
        assertDoctypeRejected(new XmlSchemaSource(file));
        }

    @Test
    public void shouldRejectDoctypeFromFilename()
            throws Exception
        {
        File file = writeFile("doctype.xml", DOCTYPE + SCHEMA);
        assertDoctypeRejected(new XmlSchemaSource(file.getAbsolutePath()));
        }

    @Test
    public void shouldRejectDoctypeFromClasspathResource()
            throws Exception
        {
        withClasspathSchema(DOCTYPE + SCHEMA, this::assertDoctypeRejected);
        }

    // ----- DOCTYPE policy -------------------------------------------------

    @Test
    public void shouldRejectExternalGeneralEntityInElementContent()
            throws Exception
        {
        String sXml = "<!DOCTYPE schema [<!ENTITY externalText SYSTEM \""
                + GENERAL_ID + "\">]>\n"
                + SCHEMA.replace("<type ", "&externalText;<type ");

        assertDoctypeRejectedWithoutResolution(sXml);
        }

    @Test
    public void shouldRejectExternalParameterEntity()
            throws Exception
        {
        String sXml = "<!DOCTYPE schema [<!ENTITY % localDtd SYSTEM \""
                + PARAMETER_ID + "\">%localDtd;]>\n" + SCHEMA;

        assertDoctypeRejectedWithoutResolution(sXml);
        }

    @Test
    public void shouldRejectExternalDtdSubset()
            throws Exception
        {
        String sXml = "<!DOCTYPE schema SYSTEM \""
                + SUBSET_ID + "\">\n" + SCHEMA;

        assertDoctypeRejectedWithoutResolution(sXml);
        }

    @Test
    public void shouldRejectHarmlessDoctype()
            throws Exception
        {
        assertDoctypeRejected(source(DOCTYPE + SCHEMA));
        }

    @Test
    public void shouldRejectTinyInternalEntity()
            throws Exception
        {
        String sXml = "<!DOCTYPE schema [<!ENTITY localName \"name\">]>\n"
                + SCHEMA.replace("name=\"name\"", "name=\"&localName;\"");

        assertDoctypeRejected(source(sXml));
        }

    @Test
    public void shouldRejectDtdProvidedAttributeDefaults()
            throws Exception
        {
        String sXml = "<!DOCTYPE schema [<!ATTLIST property type CDATA \"String\">]>\n"
                + SCHEMA.replace(" type=\"String\"", "");

        assertDoctypeRejected(source(sXml));
        }

    // ----- provider observability and fail-closed configuration -----------

    @Test
    public void shouldObserveExternalResolutionWithUnprotectedParser()
            throws Exception
        {
        String sMarker = "harmless resolver marker";
        HarnessState state = new HarnessState(Map.of(
                SUBSET_ID, "<!ENTITY % declarations SYSTEM \"" + PARAMETER_ID + "\">%declarations;",
                PARAMETER_ID, "<!ENTITY externalText SYSTEM \"" + GENERAL_ID + "\">",
                GENERAL_ID, sMarker));

        withProvider(state, () ->
            {
            // the control deliberately uses parser defaults, without production hardening
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            assertSame(RecordingFactory.class, factory.getClass());
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            String sXml = "<!DOCTYPE schema SYSTEM \"" + SUBSET_ID + "\">"
                    + "<schema>&externalText;</schema>";
            try (InputStream input = new ByteArrayInputStream(sXml.getBytes(StandardCharsets.UTF_8)))
                {
                Document document = builder.parse(input);
                assertEquals(sMarker, document.getDocumentElement().getTextContent());
                }

            assertEquals(List.of(SUBSET_ID, PARAMETER_ID, GENERAL_ID), state.m_listResolvedIds);
            assertEquals(3, state.m_cResolverCalls);
            assertEquals(1, state.m_cFactories);
            assertEquals(1, state.m_cBuilders);
            });
        }

    @Test
    public void shouldResolveOnlyKnownInMemoryFixtures()
            throws Exception
        {
        HarnessState state = new HarnessState(Map.of(GENERAL_ID, "marker"));
        EntityResolver2 resolver = new RecordingResolver(state);
        for (InputSource input : List.of(resolver.resolveEntity(null, GENERAL_ID),
                resolver.resolveEntity(null, null, null, GENERAL_ID)))
            {
            try (java.io.Reader reader = input.getCharacterStream())
                {
                assertEquals('m', reader.read());
                assertEquals(GENERAL_ID, input.getSystemId());
                }
            }
        assertThrows(SAXException.class, () -> resolver.resolveEntity(null, "urn:unexpected"));
        assertThrows(SAXException.class, () -> resolver.resolveEntity(null, null, null, "urn:unexpected"));
        assertThrows(SAXException.class, () -> resolver.resolveEntity(null, null));
        assertThrows(SAXException.class, () -> resolver.resolveEntity(null, null, null, null));
        assertEquals(6, state.m_cResolverCalls);
        }

    @Test
    public void shouldPreserveNamespaceAwarenessWithRecordingProvider()
            throws Exception
        {
        Schema schema = new SchemaBuilder().build();
        schema.registerTypeHandler(new DescriptionTypeHandler());
        String sXml = SCHEMA.replace("<type name=\"example.Customer\">",
                "<type name=\"example.Customer\">"
                + "<d:description xmlns:d=\"urn:xml-schema-source-test:description\">marker</d:description>");
        HarnessState state = new HarnessState(Map.of());
        withProvider(state, () ->
            {
            populate(source(sXml), schema);
            assertOrdinaryMetadata(schema);
            assertEquals("marker", schema.getType("example.Customer")
                    .getExtension(DescriptionType.class).m_sDescription);
            assertTrue("The real builder must be namespace aware", state.m_fNamespaceAware);
            assertEquals(1, state.m_cFactories);
            assertEquals(1, state.m_cBuilders);
            assertEquals(0, state.m_cResolverCalls);
            });
        }

    @Test
    public void shouldFailBeforeReadingWhenDoctypeProtectionUnsupported()
            throws Exception
        {
        assertProtectionRequired(DISALLOW_DOCTYPE, true);
        }

    @Test
    public void shouldFailBeforeReadingWhenGeneralEntityProtectionUnsupported()
            throws Exception
        {
        assertProtectionRequired(EXTERNAL_GENERAL_ENTITIES, false);
        }

    @Test
    public void shouldFailBeforeReadingWhenParameterEntityProtectionUnsupported()
            throws Exception
        {
        assertProtectionRequired(EXTERNAL_PARAMETER_ENTITIES, false);
        }

    @Test
    public void shouldFailBeforeReadingWhenExternalDtdLoadingProtectionUnsupported()
            throws Exception
        {
        assertProtectionRequired(LOAD_EXTERNAL_DTD, false);
        }

    @Test
    public void shouldFailBeforeReadingWhenExternalDtdAccessProtectionUnsupported()
            throws Exception
        {
        assertProtectionRequired(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        }

    @Test
    public void shouldRestoreProviderAndStateAfterFailure()
            throws Exception
        {
        String sSaved = System.getProperty(PROVIDER_PROPERTY);
        try
            {
            // exercise both an absent property and a pre-existing provider selection
            String sDefault = DocumentBuilderFactory.newDefaultInstance().getClass().getName();
            for (String sPrevious : new String[] {null, sDefault})
                {
                restoreProviderProperty(sPrevious);
                HarnessState state = new HarnessState(Map.of());
                AssertionError expected = new AssertionError("test-owned scope failure");
                AssertionError actual = assertThrows(AssertionError.class, () -> withProvider(state, () ->
                    {
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    throw expected;
                    }));
                assertSame(expected, actual);
                assertEquals(sPrevious, System.getProperty(PROVIDER_PROPERTY));
                assertNull(s_state);

                // a new scope must not inherit counters or failure injection
                HarnessState fresh = new HarnessState(Map.of());
                withProvider(fresh, () ->
                    {
                    assertEquals(0, fresh.m_cFactories);
                    assertEquals(0, fresh.m_cBuilders);
                    assertEquals(0, fresh.m_cResolverCalls);
                    assertNull(fresh.m_failure);
                    assertTrue(fresh.m_mapSettings.isEmpty());
                    });
                }
            }
        finally
            {
            restoreProviderProperty(sSaved);
            }
        }

    @Test
    public void shouldRejectOverlappingProviderScopes()
            throws Exception
        {
        HarnessState state = new HarnessState(Map.of());
        withProvider(state, () ->
            {
            assertThrows(IllegalStateException.class, () -> withProvider(new HarnessState(Map.of()), () -> {}));
            assertSame(state, s_state);
            assertEquals(RecordingFactory.class.getName(), System.getProperty(PROVIDER_PROPERTY));
            });
        }

    // ----- metadata compatibility -----------------------------------------

    @Test
    public void shouldAutomaticallyImportSchemasFromMultipleClasspathRoots()
            throws Exception
        {
        Path pathFirst  = m_tempFolder.newFolder("first-schema").toPath();
        Path pathSecond = m_tempFolder.newFolder("second-schema").toPath();
        Files.createDirectory(pathFirst.resolve("META-INF"));
        Files.createDirectory(pathSecond.resolve("META-INF"));
        Files.writeString(pathFirst.resolve("META-INF/schema.xml"), SCHEMA, StandardCharsets.UTF_8);
        Files.writeString(pathSecond.resolve("META-INF/schema.xml"),
                "<schema xmlns=\"http://xmlns.oracle.com/coherence/schema\">"
                + "<type name=\"example.Order\"><property name=\"total\" type=\"int\"/></type></schema>",
                StandardCharsets.UTF_8);

        Thread      thread      = Thread.currentThread();
        ClassLoader loaderSaved = thread.getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] {pathFirst.toUri().toURL(), pathSecond.toUri().toURL()}, loaderSaved))
            {
            thread.setContextClassLoader(loader);
            try
                {
                // discover both custom schemas and bundled metadata in one build
                Schema schema = new SchemaBuilder().build();
                assertOrdinaryMetadata(schema);

                ExtensibleType order = schema.getType("example.Order");
                assertNotNull(order);
                assertEquals("example", order.getNamespace());
                assertEquals("Order", order.getName());
                assertFalse(order.isExternal());
                assertEquals(1, order.getProperties().size());

                ExtensibleProperty total = order.getProperty("total");
                assertNotNull(total);
                assertEquals("total", total.getName());
                assertEquals("int", total.getType().getFullName());

                ExtensibleType string = schema.getType("String");
                assertNotNull(string);
                assertTrue(string.isExternal());

                JavaType javaString = string.getExtension(JavaType.class);
                assertNotNull(javaString);
                assertEquals("java.lang.String", javaString.getFullName());
                }
            finally
                {
                thread.setContextClassLoader(loaderSaved);
                }
            }
        }

    @Test
    public void shouldPreserveNamespacedExtensionsAndExternalMetadata()
            throws Exception
        {
        String sXml = "<schema xmlns=\"http://xmlns.oracle.com/coherence/schema\""
                + " xmlns:j=\"http://xmlns.oracle.com/coherence/schema/java\""
                + " xmlns:n=\"http://xmlns.oracle.com/coherence/schema/dotnet\""
                + " xmlns:c=\"http://xmlns.oracle.com/coherence/schema/cpp\""
                + " xmlns:p=\"http://xmlns.oracle.com/coherence/schema/pof\" external=\"true\">"
                + "<type name=\"example.Customer\" p:id=\"40259\" p:version=\"3\">"
                + "<j:type name=\"example.java.Customer\"/>"
                + "<n:type name=\"Example.Customer\"/>"
                + "<c:type name=\"example::Customer\"/>"
                + "<property name=\"name\" type=\"String\" p:since=\"2\" p:order=\"7\">"
                + "<j:property j:type=\"java.lang.String\"/>"
                + "<n:property n:type=\"System.String\"/>"
                + "<c:property c:type=\"coherence::lang::String\"/>"
                + "</property></type></schema>";

        Schema         schema = populate(source(sXml));
        ExtensibleType type   = schema.getType("example.Customer");
        assertNotNull(type);
        assertTrue(type.isExternal());

        ExtensibleProperty property = type.getProperty("name");
        assertNotNull(property);
        assertEquals("String", property.getType().getFullName());
        assertEquals("example.java.Customer", type.getExtension(JavaType.class).getFullName());
        assertEquals("Example.Customer", type.getExtension(DotNetType.class).getFullName());
        assertEquals("example::Customer", type.getExtension(CppType.class).getFullName());
        assertEquals("java.lang.String", property.getExtension(JavaProperty.class).getType().getFullName());
        assertEquals("System.String", property.getExtension(DotNetProperty.class).getType().getFullName());
        assertEquals("coherence::lang::String", property.getExtension(CppProperty.class).getType().getFullName());

        PofType pofType = type.getExtension(PofType.class);
        assertNotNull(pofType);
        assertEquals(40259, pofType.getId());
        assertEquals(3, pofType.getVersion());

        PofProperty pofProperty = property.getExtension(PofProperty.class);
        assertNotNull(pofProperty);
        assertEquals(2, pofProperty.getSince());
        assertEquals(7, pofProperty.getOrder());
        }

    @Test
    public void shouldPreserveEscapesAndCharacterReferences()
            throws Exception
        {
        String sXml = "<schema xmlns=\"http://xmlns.oracle.com/coherence/schema\""
                + " xmlns:d=\"urn:xml-schema-source-test:description\">"
                + "<type name=\"example.Cust&#111;mer\">"
                + "<d:description>A &amp; B &lt; C &gt; D &quot;quoted&quot; &apos;text&apos; &#65;&#x42;</d:description>"
                + "<property name=\"n&#x61;me\" type=\"Str&#105;ng\"/>"
                + "</type></schema>";

        Schema schema = new SchemaBuilder().build();
        schema.registerTypeHandler(new DescriptionTypeHandler());
        populate(source(sXml), schema);

        ExtensibleType type = schema.getType("example.Customer");
        assertNotNull(type);
        assertEquals("example", type.getNamespace());
        assertEquals("Customer", type.getName());
        assertEquals(1, type.getProperties().size());

        ExtensibleProperty property = type.getProperty("name");
        assertNotNull(property);
        assertEquals("String", property.getType().getFullName());

        DescriptionType description = type.getExtension(DescriptionType.class);
        assertNotNull(description);
        assertEquals("A & B < C > D \"quoted\" 'text' AB", description.m_sDescription);
        }

    // ----- helpers --------------------------------------------------------

    private XmlSchemaSource source(String sXml)
        {
        return new XmlSchemaSource(new ByteArrayInputStream(sXml.getBytes(StandardCharsets.UTF_8)));
        }

    private File writeFile(String sName, String sContents)
            throws IOException
        {
        File file = m_tempFolder.newFile(sName);
        Files.write(file.toPath(), sContents.getBytes(StandardCharsets.UTF_8));
        return file;
        }

    private Schema populate(XmlSchemaSource source)
            throws IOException
        {
        // close constructor-owned streams even when parsing fails
        try (InputStream ignored = source.m_inputXml)
            {
            Schema schema = new SchemaBuilder().build();
            source.populateSchema(schema);
            return schema;
            }
        }

    private void populate(XmlSchemaSource source, Schema schema)
            throws IOException
        {
        try (InputStream ignored = source.m_inputXml)
            {
            source.populateSchema(schema);
            }
        }

    private void assertOrdinaryMetadata(Schema schema)
        {
        ExtensibleType type = schema.getType("example.Customer");
        assertNotNull(type);
        assertEquals("example", type.getNamespace());
        assertEquals("Customer", type.getName());
        assertFalse(type.isExternal());
        assertEquals(1, type.getProperties().size());

        ExtensibleProperty property = type.getProperty("name");
        assertNotNull(property);
        assertEquals("name", property.getName());
        assertEquals("String", property.getType().getFullName());
        }

    private void assertDoctypeRejected(XmlSchemaSource source)
            throws IOException
        {
        try (InputStream ignored = source.m_inputXml)
            {
            // keep automatic bundled-schema loading outside the rejection assertion
            Schema schema = new SchemaBuilder().build();
            assertDoctypeRejected(source, schema);
            }
        }

    private void assertDoctypeRejected(XmlSchemaSource source, Schema schema)
        {
        RuntimeException failure = assertThrows(RuntimeException.class, () -> source.populateSchema(schema));
        Throwable cause = failure.getCause();
        while (cause != null && !(cause instanceof SAXParseException))
            {
            cause = cause.getCause();
            }

        assertNotNull("Expected a SAX parse cause for DOCTYPE rejection: " + failure, cause);
        String sMessage = String.valueOf(cause.getMessage()).toLowerCase(Locale.ROOT);
        assertTrue("Expected a DOCTYPE-policy rejection: " + cause,
                sMessage.contains("doctype")
                && (sMessage.contains("disallow") || sMessage.contains("prohibit") || sMessage.contains("forbid")));
        }

    private void assertDoctypeRejectedWithoutResolution(String sXml)
            throws Exception
        {
        // schema construction parses bundled metadata and must precede instrumentation
        Schema schema = new SchemaBuilder().build();
        HarnessState state = new HarnessState(Map.of(
                GENERAL_ID, "harmless external text",
                PARAMETER_ID, "<!ELEMENT schema ANY>",
                SUBSET_ID, "<!ELEMENT schema ANY>"));
        try (InputStream input = new ByteArrayInputStream(sXml.getBytes(StandardCharsets.UTF_8)))
            {
            withProvider(state, () ->
                {
                try
                    {
                    assertDoctypeRejected(new XmlSchemaSource(input), schema);
                    }
                finally
                    {
                    assertEquals("External resolution must not be attempted", 0, state.m_cResolverCalls);
                    }
                assertEquals(1, state.m_cFactories);
                assertEquals(1, state.m_cBuilders);
                });
            }
        }

    private void assertProtectionRequired(String sName, Object value)
            throws Exception
        {
        Schema schema = new SchemaBuilder().build();
        HarnessState state = new HarnessState(Map.of(), sName);
        try (TrackingInputStream input = new TrackingInputStream(SCHEMA))
            {
            withProvider(state, () ->
                {
                RuntimeException failure = assertThrows(RuntimeException.class,
                        () -> new XmlSchemaSource(input).populateSchema(schema));

                assertEquals("Required configuration must be attempted: " + sName,
                        value, state.m_mapSettings.get(sName));
                assertNotNull("Provider must refuse the targeted setting", state.m_failure);
                assertSame("The specific provider failure must be retained", state.m_failure, failure.getCause());
                assertEquals(1, state.m_cFactories);
                // without a builder, parsing cannot begin
                assertEquals("No builder may be requested", 0, state.m_cBuilderRequests);
                assertEquals("No builder may be created", 0, state.m_cBuilders);
                assertEquals("No input read may be attempted", 0, input.m_cReads);
                assertEquals("No input skip may be attempted", 0, input.m_cSkips);
                assertEquals(0, state.m_cResolverCalls);
                });
            }
        }

    /**
     * The module's reuseForks=false isolates test classes and JUnit 4 runs these
     * methods serially. Reject overlapping scopes; unrelated parallel classes
     * must use separate JVMs when selecting a different test launcher.
     */
    private void withProvider(HarnessState state, CheckedAction action)
            throws Exception
        {
        String sSaved;
        synchronized (PROVIDER_LOCK)
            {
            if (s_state != null)
                {
                throw new IllegalStateException("Overlapping JAXP provider scopes");
                }
            sSaved  = System.getProperty(PROVIDER_PROPERTY);
            s_state = state;
            }
        try
            {
            System.setProperty(PROVIDER_PROPERTY, RecordingFactory.class.getName());
            action.run();
            }
        finally
            {
            synchronized (PROVIDER_LOCK)
                {
                try
                    {
                    restoreProviderProperty(sSaved);
                    }
                finally
                    {
                    s_state = null;
                    }
                }
            assertEquals("Restore the exact previous provider property", sSaved, System.getProperty(PROVIDER_PROPERTY));
            assertNull("Restore harness state even after failures", s_state);
            }
        }

    private static void restoreProviderProperty(String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(PROVIDER_PROPERTY);
            }
        else
            {
            System.setProperty(PROVIDER_PROPERTY, sValue);
            }
        }

    private void withClasspathSchema(String sXml, SchemaAssertion assertion)
            throws Exception
        {
        File   directory = m_tempFolder.newFolder();
        String sResource = "xml-schema-source-" + directory.getName() + ".xml";
        File   file      = new File(directory, sResource);
        Files.write(file.toPath(), sXml.getBytes(StandardCharsets.UTF_8));
        assertFalse("The String constructor must take its resource fallback", new File(sResource).exists());

        Thread      thread      = Thread.currentThread();
        ClassLoader loaderSaved = thread.getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {directory.toURI().toURL()}, loaderSaved))
            {
            thread.setContextClassLoader(loader);
            try
                {
                assertion.check(new XmlSchemaSource(sResource));
                }
            finally
                {
                thread.setContextClassLoader(loaderSaved);
                }
            }
        }

    // ----- scoped JAXP instrumentation ------------------------------------

    /**
     * Observes the real JDK parser, forwarding the configuration used by
     * XmlSchemaSource. The production path does not replace the test resolver.
     */
    public static class RecordingFactory
            extends DocumentBuilderFactory
        {
        public RecordingFactory()
            {
            synchronized (PROVIDER_LOCK)
                {
                if (s_state == null || s_state.m_thread != Thread.currentThread())
                    {
                    throw new IllegalStateException("Recording factory requires its owning test scope");
                    }
                m_state = s_state;
                }
            m_delegate = DocumentBuilderFactory.newDefaultInstance();
            m_state.m_cFactories++;
            }

        @Override
        public DocumentBuilder newDocumentBuilder()
                throws ParserConfigurationException
            {
            m_state.m_cBuilderRequests++;
            m_delegate.setNamespaceAware(isNamespaceAware());
            DocumentBuilder builder = m_delegate.newDocumentBuilder();
            m_state.m_cBuilders++;
            m_state.m_fNamespaceAware = builder.isNamespaceAware();
            builder.setEntityResolver(new RecordingResolver(m_state));
            return builder;
            }

        @Override
        public void setAttribute(String sName, Object value)
            {
            m_state.m_mapSettings.put(sName, value);
            if (sName.equals(m_state.m_sRefusedSetting))
                {
                IllegalArgumentException failure = new IllegalArgumentException("Test provider refuses " + sName);
                m_state.m_failure = failure;
                throw failure;
                }
            m_delegate.setAttribute(sName, value);
            }

        @Override
        public Object getAttribute(String sName)
            {
            return m_delegate.getAttribute(sName);
            }

        @Override
        public void setFeature(String sName, boolean fValue)
                throws ParserConfigurationException
            {
            m_state.m_mapSettings.put(sName, fValue);
            if (sName.equals(m_state.m_sRefusedSetting))
                {
                ParserConfigurationException failure =
                        new ParserConfigurationException("Test provider refuses " + sName);
                m_state.m_failure = failure;
                throw failure;
                }
            m_delegate.setFeature(sName, fValue);
            }

        @Override
        public boolean getFeature(String sName)
                throws ParserConfigurationException
            {
            return m_delegate.getFeature(sName);
            }

        @Override
        public void setXIncludeAware(boolean fValue)
            {
            m_delegate.setXIncludeAware(fValue);
            }

        private final DocumentBuilderFactory m_delegate;
        private final HarnessState           m_state;
        }

    /**
     * Counts both resolver APIs and external-subset requests. Only in-memory
     * fixtures are returned; unknown identifiers never fall back to external I/O.
     */
    private static class RecordingResolver
            implements EntityResolver2
        {
        private RecordingResolver(HarnessState state)
            {
            m_state = state;
            }

        @Override
        public InputSource getExternalSubset(String sName, String sBaseUri)
            {
            m_state.m_cResolverCalls++;
            // an implicit subset has no identifier and contributes no declarations
            return new InputSource(new StringReader(""));
            }

        @Override
        public InputSource resolveEntity(String sPublicId, String sSystemId)
                throws SAXException
            {
            m_state.m_cResolverCalls++;
            m_state.m_listResolvedIds.add(sSystemId);
            String sFixture = sSystemId == null ? null : m_state.m_mapFixtures.get(sSystemId);
            if (sFixture == null)
                {
                throw new SAXException("Unexpected external reference: " + sSystemId);
                }
            InputSource source = new InputSource(new StringReader(sFixture));
            source.setPublicId(sPublicId);
            source.setSystemId(sSystemId);
            return source;
            }

        @Override
        public InputSource resolveEntity(String sName, String sPublicId, String sBaseUri, String sSystemId)
                throws SAXException
            {
            return resolveEntity(sPublicId, sSystemId);
            }

        private final HarnessState m_state;
        }

    private static class HarnessState
        {
        private HarnessState(Map<String, String> fixtures)
            {
            this(fixtures, null);
            }

        private HarnessState(Map<String, String> fixtures, String sRefusedSetting)
            {
            m_mapFixtures     = Map.copyOf(fixtures);
            m_sRefusedSetting = sRefusedSetting;
            }

        private final Thread             m_thread = Thread.currentThread();
        private final Map<String, String> m_mapFixtures;
        private final String             m_sRefusedSetting;
        private final Map<String, Object> m_mapSettings     = new HashMap<>();
        private final List<String>       m_listResolvedIds = new ArrayList<>();
        private Exception                m_failure;
        private int                      m_cFactories;
        private int                      m_cBuilderRequests;
        private int                      m_cBuilders;
        private int                      m_cResolverCalls;
        private boolean                  m_fNamespaceAware;
        }

    private static class TrackingInputStream
            extends InputStream
        {
        private TrackingInputStream(String sXml)
            {
            m_delegate = new ByteArrayInputStream(sXml.getBytes(StandardCharsets.UTF_8));
            }

        @Override
        public int read()
            {
            m_cReads++;
            return m_delegate.read();
            }

        @Override
        public int read(byte[] bytes, int of, int cb)
            {
            m_cReads++;
            return m_delegate.read(bytes, of, cb);
            }

        @Override
        public long skip(long cb)
            {
            m_cSkips++;
            return m_delegate.skip(cb);
            }

        @Override
        public void close()
                throws IOException
            {
            m_delegate.close();
            }

        private final ByteArrayInputStream m_delegate;
        private int                        m_cReads;
        private int                        m_cSkips;
        }

    @FunctionalInterface
    private interface CheckedAction
        {
        void run()
                throws Exception;
        }

    // ----- test metadata extension ----------------------------------------

    /**
     * Text metadata keeps XML escapes out of canonical type and property names.
     */
    public static class DescriptionType
            extends AbstractCanonicalType<ExtensibleProperty>
        {
        public DescriptionType(ExtensibleType parent)
            {
            super(parent);
            }

        private String m_sDescription;
        }

    /**
     * Imports text through the existing schema type-handler dispatch.
     */
    private static class DescriptionTypeHandler
            extends AbstractTypeHandler<DescriptionType, Element>
        {
        @Override
        public void importType(DescriptionType type, Element source, Schema schema)
            {
            Element description = (Element) source.getElementsByTagNameNS(
                    "urn:xml-schema-source-test:description", "description").item(0);
            if (description != null)
                {
                type.m_sDescription = description.getTextContent();
                }
            }
        }

    @FunctionalInterface
    private interface SchemaAssertion
        {
        void check(XmlSchemaSource source)
                throws Exception;
        }

    // ----- constants ------------------------------------------------------

    private static final String SCHEMA = "<schema xmlns=\"http://xmlns.oracle.com/coherence/schema\">"
            + "<type name=\"example.Customer\"><property name=\"name\" type=\"String\"/></type></schema>";

    private static final String DOCTYPE = "<!DOCTYPE schema>\n";

    private static final String PROVIDER_PROPERTY = "javax.xml.parsers.DocumentBuilderFactory";
    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private static final String GENERAL_ID   = "urn:xml-schema-source-test:general";
    private static final String PARAMETER_ID = "urn:xml-schema-source-test:parameter";
    private static final String SUBSET_ID    = "urn:xml-schema-source-test:subset";

    private static final Object PROVIDER_LOCK = new Object();

    // ----- data members ---------------------------------------------------

    private static HarnessState s_state;

    @Rule
    public TemporaryFolder m_tempFolder = TemporaryFolder.builder().assureDeletion().build();
    }

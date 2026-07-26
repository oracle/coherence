/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.Result;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.w3c.dom.ls.LSResourceResolver;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

/**
 * Test class for SaxParser.
 *
 * @author pp  2011.02.01
 */
public class SaxParserTest
    {
    @Rule
    public TemporaryFolder m_tempFolder = new TemporaryFolder();

    @Before
    public void setup()
        {
        // snapshot per test because coherence.mode is process-global and these tests mutate it
        m_sOldMode = System.getProperty("coherence.mode");
        }

    @After
    public void cleanup()
        {
        CoherenceModeHelper.restore(m_sOldMode);
        SaxParser.resetForTesting();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Simple test to ensure that parsing and validation works.
     *
     * @throws Exception  if an exception is thrown while parsing
     *
     */
    @Test
    public void testValidateXsd()
            throws Exception
        {
        parseWithRootElement(LOCAL_LOCATION);
        parseWithRootElement(HTTP_LOCATION);
        parseWithRootElement(NO_LOCATION);
        }

    @Test
    public void testValidateXsdWithStrictCatalogUsesLocalResolver()
            throws Exception
        {
        String sOldResolve = System.getProperty("javax.xml.catalog.resolve");

        try
            {
            System.setProperty("javax.xml.catalog.resolve", "strict");
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
                {
                // design/features/security-bugs/plans/rest-01/prompts/06-slice-e-saxparser-xxe-implementation.md closes
                // external schema access, so strict catalog validation must still use bundled local schemas
                parseWithRootElement(LOCAL_LOCATION);
                parseWithRootElement(HTTP_LOCATION);
                }
            }
        finally
            {
            if (sOldResolve == null)
                {
                System.clearProperty("javax.xml.catalog.resolve");
                }
            else
                {
                System.setProperty("javax.xml.catalog.resolve", sOldResolve);
                }
            }
        }

    @Test
    public void testLegacyValidateXsdStillUsesRealParserAndLocalSchemas()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            // design/features/security-bugs/plans/rest-01/prompts/06-slice-e-saxparser-xxe-implementation.md keeps
            // LEGACY compatibility on the real parser path, not only on fake unsupported-protection paths
            parseWithRootElement(LOCAL_LOCATION);
            parseWithRootElement(HTTP_LOCATION);
            }
        }

    /**
     * Test the {@link SaxParser#resolveSchemaSources(java.util.List)} to
     * ensure that HTTP URLs are converted to local resources.
     *
     * @throws Exception
     */
    @Test
    public void testResolveSchemaSources()
            throws Exception
        {
        SaxParser parser = new SaxParser();
        String[]  asUri  = new String[] {"coherence-cache-config.xsd",
                                         "http://xmlns.oracle.com/coherence/coherence-cache-config.xsd"};

        Source[] aSources = parser.resolveSchemaSources(Arrays.asList(asUri));
        assertEquals(2, aSources.length);
        for (int i = 0; i < aSources.length; i++)
            {
            String sUri = aSources[i].getSystemId();
            assertTrue(sUri.endsWith("coherence-cache-config.xsd"));
            assertFalse(sUri.startsWith("http"));
            }
        }

    @Test
    public void testProdValidateXsdDoesNotDereferenceJarHttpSchemaLocation()
            throws Exception
        {
        try (LoopbackRequestCounter counter = new LoopbackRequestCounter();
             CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            IOException e = expectIOException(() -> validateXmlWithSchemaLocation(
                    "jar:http://127.0.0.1:" + counter.getPort() + "/evil.jar!/external.xsd"));
            assertThat(e.getMessage(), containsString("external.xsd"));
            assertEquals(0, counter.getRequestCount());
            }
        }

    @Test
    public void testProdValidateXsdPreservesLocalJarFileSchemaLocation()
            throws Exception
        {
        File fileJar = m_tempFolder.newFile("schemas.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(fileJar)))
            {
            out.putNextEntry(new JarEntry("schema.xsd"));
            out.write(TEST_SCHEMA.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            }

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            validateXmlWithSchemaLocation("jar:" + fileJar.toURI().toURL() + "!/schema.xsd");
            }
        }

    /**
     * Test to ensure that parse exceptions are thrown.
     */
    @Test
    public void testParseException()
            throws Exception
        {
        String sXml = "<?xml version=\"1.0\"?>"
            + LOCAL_LOCATION
            + "  <distributed-scheme>"
            + "  </distributed-scheme>"
            + "  <caching-schemes>"
            + "    <distributed-scheme>"
            + "    </distributed-scheme>"
            + "  </caching-schemes>"
            + "</cache-config>";

        SaxParser    saxParser    = new SaxParser();
        SimpleParser simpleParser = new SimpleParser(false);
        XmlDocument  xml          = simpleParser.parseXml(sXml);

        try
            {
            saxParser.validateXsd(sXml, xml);
            fail("Expected parse error");
            }
        catch (Exception e)
            {
            }
        }

    @Test
    public void testDevAndProdRejectDoctype()
            throws Exception
        {
        assertDevAndProdRejectXml("<!DOCTYPE root [<!ELEMENT root ANY>]><root/>");
        }

    @Test
    public void testDevAndProdRejectExternalGeneralEntity()
            throws Exception
        {
        assertDevAndProdRejectXml("<!DOCTYPE root [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><root>&xxe;</root>");
        }

    @Test
    public void testDevAndProdRejectExternalParameterEntity()
            throws Exception
        {
        assertDevAndProdRejectXml("<!DOCTYPE root [<!ENTITY % xxe SYSTEM \"file:///etc/passwd\">%xxe;]><root/>");
        }

    @Test
    public void testDevFailsClosedWhenParserProtectionUnsupported()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            SAXException e = expectSaxException(
                    () -> SaxParser.configureRequiredParserProtections(new UnsupportedParser()));
            assertThat(e.getMessage(), containsString(FEATURE_DISALLOW_DOCTYPE));
            }
        }

    @Test
    public void testLegacyContinuesWhenParserProtectionUnsupported()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            SaxParser.configureRequiredParserProtections(new UnsupportedParser());
            }
        }

    @Test
    public void testDevFailsClosedWhenSchemaFactoryProtectionUnsupported()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            SAXException e = expectSaxException(
                    () -> SaxParser.configureRequiredSchemaFactoryProtections(new UnsupportedSchemaFactory()));
            assertThat(e.getMessage(), containsString(XML_ACCESS_EXTERNAL_DTD));
            }
        }

    @Test
    public void testDevFailsClosedWhenValidatorProtectionUnsupported()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            SAXException e = expectSaxException(
                    () -> SaxParser.configureRequiredValidatorProtections(new UnsupportedValidator()));
            assertThat(e.getMessage(), containsString(XML_ACCESS_EXTERNAL_DTD));
            }
        }

    @Test
    public void testRequiredProtectionSetAppliesToParserAndValidatorPaths()
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            RecordingParser parser = new RecordingParser();
            RecordingValidator validator = new RecordingValidator();

            SaxParser.configureRequiredParserProtections(parser);
            SaxParser.configureRequiredValidatorProtections(validator);

            assertEquals(Boolean.TRUE, parser.m_mapFeatures.get(FEATURE_DISALLOW_DOCTYPE));
            assertEquals(Boolean.FALSE, parser.m_mapFeatures.get(FEATURE_EXTERNAL_GENERAL_ENTITIES));
            assertEquals(Boolean.FALSE, parser.m_mapFeatures.get(FEATURE_EXTERNAL_PARAMETER_ENTITIES));
            assertEquals(Boolean.FALSE, parser.m_mapFeatures.get(FEATURE_LOAD_EXTERNAL_DTD));
            assertEquals("", validator.m_mapProperties.get(XML_ACCESS_EXTERNAL_DTD));
            assertEquals("", validator.m_mapProperties.get(XML_ACCESS_EXTERNAL_SCHEMA));
            }
        }

    @Test
    public void testParserCacheIsSecurityHardeningAware()
            throws Exception
        {
        Parser parserCompatibility;
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            parserCompatibility = SaxParser.getParser();
            }

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            Parser parserHardened = SaxParser.getParser();
            assertNotSame(parserCompatibility, parserHardened);
            }
        }

    @Test
    public void testValidateXsdBlocksExternalSchemaInDev()
            throws Exception
        {
        File fileSchema = m_tempFolder.newFile("external-schema.xsd");
        Files.writeString(fileSchema.toPath(),
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
                + "targetNamespace=\"urn:test\" elementFormDefault=\"qualified\">"
                + "<xs:include schemaLocation=\"http://127.0.0.1:9/external.xsd\"/>"
                + "<xs:element name=\"root\" type=\"xs:string\"/>"
                + "</xs:schema>");

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            SAXException e = expectSaxException(() -> validateXmlWithSchema(fileSchema));
            assertThat(e.getMessage(), containsString("accessExternalSchema"));
            }
        }

    @Test
    public void testValidateXsdBlocksExternalDtdInDev()
            throws Exception
        {
        File fileSchema = m_tempFolder.newFile("external-dtd.xsd");
        Files.writeString(fileSchema.toPath(),
                "<!DOCTYPE xs:schema SYSTEM \"http://127.0.0.1:9/external.dtd\">"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
                + "targetNamespace=\"urn:test\" elementFormDefault=\"qualified\">"
                + "<xs:element name=\"root\" type=\"xs:string\"/>"
                + "</xs:schema>");

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            SAXException e = expectSaxException(() -> validateXmlWithSchema(fileSchema));
            assertThat(e.getMessage(), containsString("accessExternalDTD"));
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Parse a Coherence configuration XML document with the given
     * root element.  This element contains the XSD schema location
     * which is used to load the XSD for validation.
     *
     * @param sRootElement  the root element used in the test XML doc
     *
     * @throws Exception  if an exception is thrown while parsing
     */
    protected void parseWithRootElement(String sRootElement)
            throws Exception
        {
        String sXml = "<?xml version=\"1.0\"?>"
            + sRootElement
            + "  <caching-scheme-mapping>"
            + "    <cache-mapping>"
            + "      <cache-name>*</cache-name>"
            + "      <scheme-name>partitioned</scheme-name>"
            + "    </cache-mapping>"
            + "  </caching-scheme-mapping>"
            + "  <caching-schemes>"
            + "    <distributed-scheme>"
            + "      <scheme-name>partitioned</scheme-name>"
            + "      <backing-map-scheme>"
            + "        <local-scheme />"
            + "      </backing-map-scheme>"
            + "    </distributed-scheme>"
            + "  </caching-schemes>"
            + "</cache-config>";

        SaxParser    saxParser    = new SaxParser();
        SimpleParser simpleParser = new SimpleParser(false);
        XmlDocument  xml          = simpleParser.parseXml(sXml);

        saxParser.validateXsd(sXml, xml);
        }

    private static void assertDevAndProdRejectXml(String sXml)
            throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            SaxParser.resetForTesting();
            expectSaxException(() -> new SaxParser().parseXml(sXml));
            }

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            SaxParser.resetForTesting();
            expectSaxException(() -> new SaxParser().parseXml(sXml));
            }
        }

    private void validateXmlWithSchema(File fileSchema)
            throws Exception
        {
        validateXmlWithSchemaLocation(fileSchema.getAbsolutePath());
        }

    private void validateXmlWithSchemaLocation(String sSchemaLocation)
            throws Exception
        {
        String sXml = "<root xmlns=\"urn:test\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"urn:test " + sSchemaLocation + "\">value</root>";
        SimpleParser simpleParser = new SimpleParser(false);
        XmlDocument  xml          = simpleParser.parseXml(sXml);

        new SaxParser().validateXsd(sXml, xml);
        }

    private static SAXException expectSaxException(CheckedRunnable runnable)
            throws Exception
        {
        try
            {
            runnable.run();
            fail("Expected SAXException");
            return null;
            }
        catch (SAXException e)
            {
            return e;
            }
        }

    private static IOException expectIOException(CheckedRunnable runnable)
            throws Exception
        {
        try
            {
            runnable.run();
            fail("Expected IOException");
            return null;
            }
        catch (IOException e)
            {
            return e;
            }
        }

    // ----- inner interface: CheckedRunnable -------------------------------

    private interface CheckedRunnable
        {
        void run()
                throws Exception;
        }

    // ----- inner class: UnsupportedParser ---------------------------------

    private static class UnsupportedParser
            implements Parser
        {
        @Override
        public void setLocale(java.util.Locale locale)
            {
            }

        @Override
        public void setEntityResolver(EntityResolver resolver)
            {
            }

        @Override
        public void setDTDHandler(DTDHandler handler)
            {
            }

        @Override
        public void setDocumentHandler(DocumentHandler handler)
            {
            }

        @Override
        public void setErrorHandler(ErrorHandler handler)
            {
            }

        @Override
        public void parse(InputSource input)
            {
            }

        @Override
        public void parse(String sSystemId)
            {
            }
        }

    // ----- inner class: RecordingParser -----------------------------------

    private static class RecordingParser
            extends UnsupportedParser
            implements XMLReader
        {
        @Override
        public boolean getFeature(String sName)
            {
            Boolean FValue = m_mapFeatures.get(sName);
            return FValue != null && FValue;
            }

        @Override
        public void setFeature(String sName, boolean fValue)
            {
            m_mapFeatures.put(sName, fValue);
            }

        @Override
        public Object getProperty(String sName)
            {
            return null;
            }

        @Override
        public void setProperty(String sName, Object oValue)
            {
            }

        @Override
        public void setEntityResolver(EntityResolver resolver)
            {
            }

        @Override
        public EntityResolver getEntityResolver()
            {
            return null;
            }

        @Override
        public void setDTDHandler(DTDHandler handler)
            {
            }

        @Override
        public DTDHandler getDTDHandler()
            {
            return null;
            }

        @Override
        public void setContentHandler(ContentHandler handler)
            {
            }

        @Override
        public ContentHandler getContentHandler()
            {
            return null;
            }

        @Override
        public void setErrorHandler(ErrorHandler handler)
            {
            }

        @Override
        public ErrorHandler getErrorHandler()
            {
            return null;
            }

        @Override
        public void parse(InputSource input)
            {
            }

        @Override
        public void parse(String sSystemId)
            {
            }

        private final Map<String, Boolean> m_mapFeatures = new HashMap<>();
        }

    // ----- inner class: UnsupportedSchemaFactory --------------------------

    private static class UnsupportedSchemaFactory
            extends SchemaFactory
        {
        @Override
        public boolean isSchemaLanguageSupported(String sSchemaLanguage)
            {
            return true;
            }

        @Override
        public void setFeature(String sName, boolean fValue)
            {
            }

        @Override
        public boolean getFeature(String sName)
            {
            return false;
            }

        @Override
        public void setProperty(String sName, Object oValue)
                throws SAXNotRecognizedException
            {
            throw new SAXNotRecognizedException("unsupported property: " + sName);
            }

        @Override
        public Schema newSchema(Source[] aSchemas)
            {
            return null;
            }

        @Override
        public Schema newSchema()
            {
            return null;
            }

        @Override
        public void setErrorHandler(ErrorHandler errorHandler)
            {
            }

        @Override
        public ErrorHandler getErrorHandler()
            {
            return null;
            }

        @Override
        public void setResourceResolver(LSResourceResolver resolver)
            {
            }

        @Override
        public LSResourceResolver getResourceResolver()
            {
            return null;
            }
        }

    // ----- inner class: UnsupportedValidator ------------------------------

    private static class UnsupportedValidator
            extends Validator
        {
        @Override
        public void reset()
            {
            }

        @Override
        public void validate(Source source, Result result)
            {
            }

        @Override
        public void setProperty(String sName, Object oValue)
                throws SAXNotRecognizedException
            {
            throw new SAXNotRecognizedException("unsupported property: " + sName);
            }

        @Override
        public void setErrorHandler(ErrorHandler errorHandler)
            {
            }

        @Override
        public ErrorHandler getErrorHandler()
            {
            return null;
            }

        @Override
        public void setResourceResolver(LSResourceResolver resolver)
            {
            }

        @Override
        public LSResourceResolver getResourceResolver()
            {
            return null;
            }
        }

    // ----- inner class: RecordingValidator --------------------------------

    private static class RecordingValidator
            extends UnsupportedValidator
        {
        @Override
        public void setProperty(String sName, Object oValue)
            {
            m_mapProperties.put(sName, oValue);
            }

        private final Map<String, Object> m_mapProperties = new HashMap<>();
        }

    private static class LoopbackRequestCounter
            implements AutoCloseable
        {
        private LoopbackRequestCounter()
                throws IOException
            {
            f_server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            f_server.setSoTimeout(100);
            f_thread = new Thread(this::acceptRequests, "SaxParserTest-LoopbackRequestCounter");
            f_thread.setDaemon(true);
            f_thread.start();
            }

        private int getPort()
            {
            return f_server.getLocalPort();
            }

        private int getRequestCount()
            {
            return f_cRequests.get();
            }

        @Override
        public void close()
                throws IOException
            {
            f_fClosed.set(true);
            f_server.close();
            try
                {
                f_thread.join(1000);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new IOException(e);
                }
            IOException e = f_error.get();
            if (e != null)
                {
                throw e;
                }
            }

        private void acceptRequests()
            {
            while (!f_fClosed.get())
                {
                try (Socket socket = f_server.accept())
                    {
                    f_cRequests.incrementAndGet();
                    }
                catch (SocketTimeoutException ignored)
                    {
                    }
                catch (IOException e)
                    {
                    if (!f_fClosed.get())
                        {
                        f_error.compareAndSet(null, e);
                        }
                    }
                }
            }

        private final ServerSocket                 f_server;
        private final Thread                       f_thread;
        private final AtomicBoolean                f_fClosed   = new AtomicBoolean();
        private final AtomicInteger                f_cRequests = new AtomicInteger();
        private final AtomicReference<IOException> f_error     = new AtomicReference<>();
        }

    /**
     * Cache configuration root element with HTTP schema location.
     */
    private static final String HTTP_LOCATION =
          "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
          + "xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\" "
          + "xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config "
          + "http://xmlns.oracle.com/coherence/coherence-cache-config/1.0/coherence-cache-config.xsd\">";

    /**
     * Cache configuration root element with local schema location.
     */
    private static final String LOCAL_LOCATION =
          "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
          + "xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\" "
          + "xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config "
          + "coherence-cache-config.xsd\">";

    /**
     * Cache configuration root element with NO schema location.
     */
    private static final String NO_LOCATION =
          "<cache-config xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
          + "xmlns=\"http://xmlns.oracle.com/coherence/coherence-cache-config\" "
          + "xsi:schemaLocation=\"http://xmlns.oracle.com/coherence/coherence-cache-config\">";

    private static final String FEATURE_DISALLOW_DOCTYPE =
            "http://apache.org/xml/features/disallow-doctype-decl";

    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";

    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";

    private static final String FEATURE_LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private static final String XML_ACCESS_EXTERNAL_DTD =
            "http://javax.xml.XMLConstants/property/accessExternalDTD";

    private static final String XML_ACCESS_EXTERNAL_SCHEMA =
            "http://javax.xml.XMLConstants/property/accessExternalSchema";

    private static final String TEST_SCHEMA =
            "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
            + "targetNamespace=\"urn:test\" elementFormDefault=\"qualified\">"
            + "<xs:element name=\"root\" type=\"xs:string\"/>"
            + "</xs:schema>";

    private String m_sOldMode;
    }

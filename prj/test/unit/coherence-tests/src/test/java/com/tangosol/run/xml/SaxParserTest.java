/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;

import java.util.Arrays;

import javax.xml.transform.Source;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for SaxParser.
 *
 * @author pp  2011.02.01
 */
public class SaxParserTest
    {
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
    }

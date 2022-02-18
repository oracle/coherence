/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package xsd;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.XMLConstants;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.SchemaFactory;

import java.util.Arrays;
import java.util.Collection;

/**
* A collection of functional tests that validate the xml using the
* scheme definition files.
*
* @author der 10/020/2011
*/
@RunWith(Parameterized.class)
public class XsdValidationTests
    {
    // ----- constructor ----------------------------------------------------

    /**
     * Run tests using different XML parsers.
     *
     * @param sSaxParserFactoryImplName  canonical classname for SAX Parser Factory impl to test
     * @param sSchemaFactoryImplName     canonical classname for SAX Schema Factory impl to test
     */
    public XsdValidationTests(String sSaxParserFactoryImplName, String sSchemaFactoryImplName)
        {
        System.setProperty("javax.xml.parsers.SAXParserFactory", sSaxParserFactoryImplName);
        System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema", sSchemaFactoryImplName);

        // enables verifying which xml parser implementation is being loaded and from what jar.
        System.setProperty("jaxp.debug", "true");

        SAXParserFactory spf = SAXParserFactory.newInstance();
        assertEquals(sSaxParserFactoryImplName, spf.getClass().getCanonicalName());

        SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        assertEquals(sSchemaFactoryImplName, sf.getClass().getCanonicalName());
        }

    // ----- test lifecycle methods -----------------------------------------

    @Parameterized.Parameters(name = "SaxParserFactoryImpl={0} SchemaFactoryImpl={1}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]
            {
                // default JDK 8 parser, need to explicitly specify to override the service provider-configuration file in test scoped xercesImpl.jar
                {"com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl", "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory"},

                // a xerces implementation to verify that all tests run and tolerate of unrecognized/unsuppported features/properties.
                // Depending on Xerces implementation, it only implements JAXP 1.4 or less
                {"org.apache.xerces.jaxp.SAXParserFactoryImpl", "org.apache.xerces.jaxp.validation.XMLSchemaFactory"}
            });
        }

    // ----- test methods ---------------------------------------------------

   /**
    * Test the system-property attribute for each element with the following files
    * that have been modified to have the system-property added.
    *
    * @throws Exception  if XML does not validate against the XSD
    */
    @Test
    public void testDefaultFiles()
        throws Exception
        {
        XmlValidator.validate("../../../coherence-core/src/main/resources/coherence-cache-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/coherence-pof-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/management-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/tangosol-coherence-override-dev.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/tangosol-coherence-override-eval.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/tangosol-coherence-override-prod.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/tangosol-coherence.xml");
        }

   /**
    * Test the system-property attribute for each element with the following files
    * that have been modified to have the system-property added.
    *
    * @throws Exception  if XML does not validate against the XSD
    */
    @Test
    public void testSystemPropertyAttrib()
        throws Exception
        {
        XmlValidator.validate("system-property-coherence-cache-config.xml");
        XmlValidator.validate("system-property-tangosol-coherence.xml");
        XmlValidator.validate("system-property-tangosol-coherence-override.xml");
        }

    /**
     * Test fix to replicated and optimistic-scheme to enforce no
     * read-write-backing-map-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916Valid()
            throws Exception
        {
        XmlValidator.validate("cache-config-coh-5916-1.xml");
        }

    /**
     * Test invalid use of <partition> element in replicated-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916ReplicatedInvalidPartition()
            throws Exception
        {
        try
            {
            XmlValidator.validate("cache-config-coh-5916-2.xml");
            fail("Failed to throw expected exception in testCacheConfigCoh5916ReplicatedInvalidPartition");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            }
        }

    /**
     * Test invalid use of RWBM in replicated-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916ReplicatedInvalidRWBM()
            throws Exception
        {
        try
            {
            XmlValidator.validate("cache-config-coh-5916-3.xml");
            fail("Failed to throw exception in testCacheConfigCoh5916ReplicatedInvalidRWBM");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            }
        }

    /**
     * Test invalid use of <partition> element in optimistic-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916OptimisticInvalidPartition()
            throws Exception
        {
        try
            {
            XmlValidator.validate("cache-config-coh-5916-4.xml");
            fail("Failed to throw expected exception in testCacheConfigCoh5916OptimisticInvalidPartition");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            }
        }

    /**
     * Test invalid use of RWBM in replicated-scheme (COH-5916).
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testCacheConfigCoh5916OptimisticInvalidRWBM()
            throws Exception
        {
        try
            {
            XmlValidator.validate("cache-config-coh-5916-5.xml");
            fail("Failed to throw expected exception in testCacheConfigCoh5916OptimisticInvalidRWBM");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            }
        }

    /**
     * Test to validate the XML changes done for COH12077 to support password-providers
     *
     * @throws Exception if XML does not validate against the XSD
     */
    @Test
    public void testPasswordProviderXML()
            throws Exception
        {
        XmlValidator.validate("tangosol-coherence-override-password-provider.xml");
        }

    @Test
    public void testXmlValidationMustDenyAccessExternalDTD()
        throws Exception
        {
        Assume.assumeThat(supportJAXP15Property(XMLConstants.ACCESS_EXTERNAL_DTD), is(true));
        try
            {
            XmlValidator.validate("validation_denies_access_to_external_dtd.xml");
            fail("must fail due to accessing external DTD entity");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            System.out.println("handled expected exception: " + e.getMessage());
            }
        }

    @Test
    public void testXmlValidationMustDenyAccessExternalSchema()
        throws Exception
        {
        Assume.assumeThat(supportJAXP15Property(XMLConstants.ACCESS_EXTERNAL_SCHEMA), is(true));
        try
            {
            XmlValidator.validate("validation_denies_access_to_external_schema.xml");
            fail("must fail due to accessing external schema");
            }
        catch (java.io.IOException e)
            {
            // expected exception
            System.out.println("handled expected exception: " + e.getMessage());
            }
        }

    // ----- helpers --------------------------------------------------------

    private static boolean supportJAXP15Property(String property)
        {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try
            {
            sf.setProperty(property, "");
            }
        catch (Exception e)
            {
            System.out.println("Skipping implementation: " + sf.getClass().getCanonicalName());
            return false;
            }
        return true;
        }
    }

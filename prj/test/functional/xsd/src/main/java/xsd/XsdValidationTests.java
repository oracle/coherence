/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package xsd;

import static com.tangosol.util.Base.getContextClassLoader;
import static com.tangosol.util.Base.read;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.oracle.coherence.common.base.Resources;
import com.oracle.coherence.testing.SystemPropertyResource;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.net.CacheFactory;

import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.junit.runners.Parameterized;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.SchemaFactory;

import java.io.File;
import java.io.IOException;

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
     * @param sSecurityMode              explicit security mode, or null for the default
     */
    public XsdValidationTests(String sSaxParserFactoryImplName, String sSchemaFactoryImplName, String sSecurityMode)
        {
        m_xmlParserRule = xmlParserRule(sSaxParserFactoryImplName, sSchemaFactoryImplName, sSecurityMode);
        m_fLegacyParser = sSchemaFactoryImplName.equals("org.apache.xerces.jaxp.validation.XMLSchemaFactory");
        }

    // ----- test lifecycle methods -----------------------------------------

    @Parameterized.Parameters(name = "SaxParserFactoryImpl={0} SchemaFactoryImpl={1} SecurityMode={2}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]
            {
                // select the JDK provider explicitly despite xercesImpl.jar's service registration
                {"com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl", "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory", null},
                {"com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl", "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory", "compatibility"},

                // tolerating unsupported XML protections requires explicit compatibility mode
                {"org.apache.xerces.jaxp.SAXParserFactoryImpl", "org.apache.xerces.jaxp.validation.XMLSchemaFactory", "compatibility"}
            });
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testHardenedModeRequiresXmlProtections()
            throws Exception
        {
        for (String sSecurityMode : new String[] {null, "hardened"})
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityMode(sSecurityMode))
                {
                if (m_fLegacyParser)
                    {
                    IOException error = assertThrows(IOException.class,
                            () -> XmlValidator.validate("cache-config-coh-5916-1.xml"));
                    assertTrue(error.getMessage(), error.getMessage().contains("XML protection"));
                    assertTrue(error.getMessage(), error.getMessage().contains(XMLConstants.ACCESS_EXTERNAL_DTD));
                    }
                else
                    {
                    XmlValidator.validate("cache-config-coh-5916-1.xml");
                    }
                }
            }
        }

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
        XmlValidator.validate("../../../coherence-core/src/main/resources/com/oracle/coherence/defaults/coherence-cache-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/com/oracle/coherence/defaults/grpc-proxy-cache-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/coherence-pof-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/com/oracle/coherence/defaults/management-config.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/com/oracle/coherence/defaults/tangosol-coherence-override-dev.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/com/oracle/coherence/defaults/tangosol-coherence-override-eval.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/com/oracle/coherence/defaults/tangosol-coherence-override-prod.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/tangosol-coherence.xml");
        XmlValidator.validate("../../../coherence-core/src/main/resources/com/oracle/coherence/defaults/grpc-proxy-cache-config.xml");

        XmlValidator.validate("../../../coherence-concurrent/src/main/resources/coherence-concurrent-config.xml");
        XmlValidator.validate("../../../coherence-concurrent/src/main/resources/coherence-concurrent-client-config.xml");
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
        XmlValidator.validate("system-property-federation.xml");
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

    /**
     * Test for Bug 33801919 to make sure XmlHelper.overrideElement()
     * produces correct XML and passes schema validate after the merge
     * with override.  If an element in a list is added from override
     * to base, it is still added to the end of the list.
     *
     * @since 14.1.2.0
     */
    @Test
    public void testOverrideElement()
            throws Exception
        {
        ClassLoader  loader       = getContextClassLoader();
        String       sBaseXml     = new String(read(new File(Resources.findFileOrResource("cluster-config-base.xml", loader).toURI())));
        String       sOverrideXml = new String(read(new File(Resources.findFileOrResource("cluster-config-override.xml", loader).toURI())));

        SimpleParser parser      = new SimpleParser(true);
        XmlDocument  xmlBase     = parser.parseXml(sBaseXml);
        XmlDocument  xmlOverride = parser.parseXml(sOverrideXml);

        final String WKA_PATH = "unicast-listener/well-known-addresses";
        XmlElement   xmlItem  = xmlBase.findElement(WKA_PATH);
        Assert.assertEquals(xmlItem.getElementList().size(), 1);

        XmlHelper.overrideElement(xmlBase, xmlOverride);
        parser.parseXml(xmlBase.toString());

        xmlItem = xmlBase.findElement("multicast-listener/time-to-live");
        Assert.assertEquals(Integer.parseInt((String) xmlItem.getValue()), 3);

        XmlElement xmlItemOverride = xmlOverride.findElement(WKA_PATH);
        xmlItem = xmlBase.findElement(WKA_PATH);
        Assert.assertEquals(xmlItem, xmlItemOverride);
        sBaseXml = xmlBase.toString();
        parser.parseXml(sBaseXml);

        // Test adding to base XML.
        // Element is still added to the end of the list.  So we only test
        // that the element is added.
        sOverrideXml = new String(read(new File(Resources.findFileOrResource("cluster-config-override-add.xml", loader).toURI())));
        xmlOverride  = parser.parseXml(sOverrideXml);

        Assert.assertFalse(sBaseXml.contains("interface"));

        XmlHelper.overrideElement(xmlBase, xmlOverride);
        final String INTERFACE_PATH = "multicast-listener/interface";
        xmlItem = xmlBase.findElement(INTERFACE_PATH);
        xmlItemOverride = xmlOverride.findElement(INTERFACE_PATH);
        Assert.assertEquals(xmlItem, xmlItemOverride);
        }

    /**
     * Test to ensure that the cluster configuration returned by Coherence
     * passes schema validation.
     *
     * @since 14.1.2.0
     */
    @Test
    public void testGetClusterConfig()
            throws Exception
        {
        XmlElement xmlCluster = CacheFactory.getClusterConfig();

        new SimpleParser().parseXml(xmlCluster.toString());
        }

    // ----- helpers --------------------------------------------------------

    static TestRule xmlParserRule(String sSaxParser, String sSchemaFactory, String sSecurityMode)
        {
        return (base, description) -> new Statement()
            {
            @Override
            public void evaluate()
                    throws Throwable
                {
                try (SystemPropertyResource sax = new SystemPropertyResource(
                             "javax.xml.parsers.SAXParserFactory", sSaxParser);
                     SystemPropertyResource schema = new SystemPropertyResource(
                             "javax.xml.validation.SchemaFactory:" + XMLConstants.W3C_XML_SCHEMA_NS_URI, sSchemaFactory);
                     CoherenceModeHelper.ModeScope mode = CoherenceModeHelper.securityMode(sSecurityMode))
                    {
                    assertEquals(sSaxParser, SAXParserFactory.newInstance().getClass().getCanonicalName());
                    assertEquals(sSchemaFactory, SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                            .getClass().getCanonicalName());
                    base.evaluate();
                    }
                }
            };
        }

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

    @Rule
    public final TestRule m_xmlParserRule;

    private final boolean m_fLegacyParser;
    }

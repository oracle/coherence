/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package xsd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.SchemaFactory;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
* A collection of funtional tests that validate the xml used in
* chapter 3 of the Developer's Guide.
*
* @author der 10/020/2011
*/
@RunWith(Parameterized.class)
public class DevGuideXmlTests
    {
    // ----- constructor ----------------------------------------------------

    /**
     * Run tests using different XML parsers.
     *
     * @param sSaxParserFactoryImplName  canonical classname for SAX Parser Factory impl to test
     * @param sSchemaFactoryImplName     canonical classname for SAX Schema Factory impl to test
     */
    public DevGuideXmlTests(String sSaxParserFactoryImplName, String sSchemaFactoryImplName)
        {
        System.setProperty("javax.xml.parsers.SAXParserFactory", sSaxParserFactoryImplName);
        System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema", sSchemaFactoryImplName);

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
    * Test the xml examples in the Developer's Guide chapter 3.
    */
    @Test
    public void testChapter3()
        throws Exception
        {
        XmlValidator.validate("dg-ch3-1-cache-config.xml");
        XmlValidator.validate("dg-ch3-1-override.xml");
        XmlValidator.validate("dg-ch3-1-pof-config.xml");
        XmlValidator.validate("dg-ch3-1-report-group.xml");
        XmlValidator.validate("dg-ch3-2-cache-config.xml");
        XmlValidator.validate("dg-ch3-2-override.xml");
        XmlValidator.validate("dg-ch3-2-pof-config.xml");
        XmlValidator.validate("dg-ch3-3-cluster-config.xml");
        XmlValidator.validate("dg-ch3-3-override.xml");
        XmlValidator.validate("dg-ch3-4-override.xml");
        XmlValidator.validate("dg-ch3-5-override.xml");
        XmlValidator.validate("dg-ch3-6-override-mbeans.xml");
        XmlValidator.validate("dg-ch3-7-override-cluster-name.xml");
        XmlValidator.validate("dg-ch3-8-override-disable-ls.xml");
        XmlValidator.validate("dg-ch3-9-override-custom-mbean.xml");
        XmlValidator.validate("dg-ch3-10-override-cluster-config.xml");
        XmlValidator.validate("dg-ch3-11-override-multicast-listener.xml");
        XmlValidator.validate("dg-ch3-12-override-services.xml");
        XmlValidator.validate("dg-ch3-13-override-service.xml");
        XmlValidator.validate("dg-ch3-14-override-multiple.xml");
        XmlValidator.validate("dg-ch3-15-override-multiple.xml");
        XmlValidator.validate("dg-ch3-16-override-multiple.xml");
        XmlValidator.validate("dg-ch3-17-override-multicast-listener.xml");
        XmlValidator.validate("dg-ch3-18-override-cluster-config.xml");
        }

    /**
     * Test the xml examples in the Developer's Guide chapter 4.
     */
    @Test
    public void testChapter4()
        throws Exception
        {
        XmlValidator.validate("dg-ch4-1-cache-config-hello-example.xml");
        XmlValidator.validate("dg-ch4-1-override.xml");
        }

    /**
     * Test the xml examples in the Developer's Guide chapter 3.
     */
    @Test
    public void testChapter6()
        throws Exception
        {
        XmlValidator.validate("dg-ch6-1-override.xml");
        XmlValidator.validate("dg-ch6-2-override.xml");
        XmlValidator.validate("dg-ch6-3-override.xml");
        XmlValidator.validate("dg-ch6-4-override.xml");
        XmlValidator.validate("dg-ch6-5-override.xml");
        XmlValidator.validate("dg-ch6-6-override.xml");
        XmlValidator.validate("dg-ch6-7-override.xml");
        XmlValidator.validate("dg-ch6-8-override.xml");
        XmlValidator.validate("dg-ch6-9-override.xml");
        XmlValidator.validate("dg-ch6-10-override.xml");
        XmlValidator.validate("dg-ch6-11-override.xml");
        XmlValidator.validate("dg-ch6-12-override.xml");
        XmlValidator.validate("dg-ch6-13-override.xml");
        XmlValidator.validate("dg-ch6-14-override.xml");
        XmlValidator.validate("dg-ch6-15-override.xml");
        XmlValidator.validate("dg-ch6-16-override.xml");
        XmlValidator.validate("dg-ch6-17-override.xml");
        XmlValidator.validate("dg-ch6-18-override.xml");
        XmlValidator.validate("dg-ch6-19-override.xml");
        }

    /**
     * Test the xml examples in the Developer's Guide chapter 8.
     */
    @Test
    public void testChapter8()
        throws Exception
        {
        XmlValidator.validate("dg-ch8-1-cache-config-distributed.xml");
        }

    /**
     * Test the xml examples in the Developer's Guide chapter 3.
     */
    @Test
    public void testChapter9()
        throws Exception
        {
        XmlValidator.validate("dg-ch9-1-override.xml");
        XmlValidator.validate("dg-ch9-2-override.xml");
        XmlValidator.validate("dg-ch9-3-override.xml");
        XmlValidator.validate("dg-ch9-4-override.xml");
        XmlValidator.validate("dg-ch9-5-override.xml");
        XmlValidator.validate("dg-ch9-6-override.xml");
        XmlValidator.validate("dg-ch9-7-override.xml");
        XmlValidator.validate("dg-ch9-8-override.xml");
        XmlValidator.validate("dg-ch9-9-override.xml");
        XmlValidator.validate("dg-ch9-10-override.xml");
        XmlValidator.validate("dg-ch9-11-override.xml");
        XmlValidator.validate("dg-ch9-12-override.xml");
        XmlValidator.validate("dg-ch9-13-override.xml");
        XmlValidator.validate("dg-ch9-14-override.xml");
        XmlValidator.validate("dg-ch9-15-override.xml");
        XmlValidator.validate("dg-ch9-16-override.xml");
        XmlValidator.validate("dg-ch9-17-override.xml");
        XmlValidator.validate("dg-ch9-18-override.xml");
        XmlValidator.validate("dg-ch9-19-override.xml");
        }
    }

/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package xsd;

import org.junit.Test;

/**
* A collection of funtional tests that validate the xml used in
* chapter 3 of the Developer's Guide.
*
* @author der 10/020/2011
*/
public class DevGuideXmlTests
    {
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

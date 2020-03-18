/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import data.pof.PortablePerson;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Tests for XmlJaxbMarshaller.
 *
 * @author as  2011.07.14
 */
public class XmlJaxbMarshallerTest
        extends AbstractMarshallerTest
    {
    protected Marshaller getMarshaller()
        {
        return new JaxbXmlMarshaller(PortablePerson.class);
        }

    @Before
    public void setUp()
        {
        System.setProperty("coherence.rest.xml.allowDTD", "false");
        System.setProperty("coherence.rest.xml.allowExternalEntities", "false");
        }

    @Test
    public void testUnmarshallWithSE()
            throws IOException
        {
        ByteArrayInputStream in = new ByteArrayInputStream(getPersonWithSEValue().getBytes());
        PortablePerson p = (PortablePerson) getMarshaller().unmarshal(in, null);

        PortablePerson expectedPerson = PortablePerson.create();
        expectedPerson.setName("Popeye <the sailor man>");
        assertEquals(expectedPerson, p);
        }

    @Test(expected = IOException.class)
    public void testUnmarshallNoDTD()
            throws IOException
        {
        ByteArrayInputStream in = new ByteArrayInputStream(getDTDValue().getBytes());
        PortablePerson p = (PortablePerson) getMarshaller().unmarshal(in, null);
        assertEquals(m_person, p);
        }

    @Test
    public void testUnmarshallAllowDTD()
            throws IOException
        {
        ByteArrayInputStream in = new ByteArrayInputStream(getDTDValue().getBytes());
        System.setProperty("coherence.rest.xml.allowDTD", "true");
        System.setProperty("coherence.rest.xml.allowExternalEntities", "true");

        PortablePerson p      = (PortablePerson) getMarshaller().unmarshal(in, null);
        PortablePerson person = new PortablePerson();
        person.setName(" Mark5059850598505985059850598505985059850598");
        assertEquals(person, p);
        }

    protected String getExpectedValue()
        {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<person age=\"36\"><dateOfBirth>1974-08-24</dateOfBirth>"
                + "<name>Aleksandar Seovic</name><address><city>Tampa</city>"
                + "<state>FL</state><street>123 Main St</street><zip>12345</zip>"
                + "</address><children>"
                + "<child xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"6\">"
                + "<dateOfBirth>2004-08-14</dateOfBirth><name>Ana Maria Seovic</name><children/></child>"
                + "<child xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"3\">"
                + "<dateOfBirth>2008-12-28</dateOfBirth><name>Novak Seovic</name><children/></child></children>"
                + "<spouse xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"33\">"
                + "<dateOfBirth>1978-02-20</dateOfBirth><name>Marija Seovic</name><children/></spouse></person>";
        }

    protected String getExpectedFragment()
        {
        return "<person age=\"36\"><dateOfBirth>1974-08-24</dateOfBirth>"
                + "<name>Aleksandar Seovic</name><address><city>Tampa</city>"
                + "<state>FL</state><street>123 Main St</street><zip>12345</zip>"
                + "</address><children>"
                + "<child xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"6\">"
                + "<dateOfBirth>2004-08-14</dateOfBirth><name>Ana Maria Seovic</name><children/></child>"
                + "<child xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"3\">"
                + "<dateOfBirth>2008-12-28</dateOfBirth><name>Novak Seovic</name><children/></child></children>"
                + "<spouse xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"33\">"
                + "<dateOfBirth>1978-02-20</dateOfBirth><name>Marija Seovic</name><children/>"
                + "</spouse></person>";
        }

    protected String getPersonWithSEValue()
        {
        return "<person age=\"36\"><dateOfBirth>1974-08-24</dateOfBirth>"
                + "<name>Popeye &lt;the sailor man&gt;</name><address><city>Tampa</city>"
                + "<state>FL</state><street>123 Main St</street><zip>12345</zip>"
                + "</address><children>"
                + "<child xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"6\">"
                + "<dateOfBirth>2004-08-14</dateOfBirth><name>Ana Maria Seovic</name><children/></child>"
                + "<child xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"3\">"
                + "<dateOfBirth>2008-12-28</dateOfBirth><name>Novak Seovic</name><children/></child></children>"
                + "<spouse xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"33\">"
                + "<dateOfBirth>1978-02-20</dateOfBirth><name>Marija Seovic</name><children/>"
                + "</spouse></person>";
        }

    protected String getDTDValue()
        {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<!DOCTYPE foo [<!ENTITY xeec0c210 \"50598\"><!ENTITY xeec0c211 "
                + "\"&xeec0c210;&xeec0c210;\"><!ENTITY xeec0c212 \"&xeec0c211;&xeec0c211;\">"
                + "<!ENTITY xeec0c213 \"&xeec0c212;&xeec0c212;\">]>"
                + "<Person><name> Mark&xeec0c213;</name><age>12</age></Person>";
        }
    }

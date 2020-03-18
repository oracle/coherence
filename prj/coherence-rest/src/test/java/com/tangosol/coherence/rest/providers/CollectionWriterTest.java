/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.providers;

import com.tangosol.coherence.rest.io.MarshallerRegistry;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.io.ByteArrayOutputStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.codehaus.jettison.json.JSONArray;

import org.junit.Test;

import javax.ws.rs.core.MediaType;

import javax.xml.bind.annotation.XmlRootElement;

import static org.junit.Assert.assertEquals;

/**
 * Tests for CollectionWriter class.
 */
public class CollectionWriterTest
    {
    @Test
    public void testJsonCollectionMarshalling()
            throws Exception
        {
        Collection<Letter> letters = Arrays.asList(new Letter("A"), new Letter("B"), new Letter("C"), new Letter("D"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonCollectionWriter w = new JsonCollectionWriter(new MarshallerRegistry());

        w.writeTo(letters, null, null, null, MediaType.APPLICATION_JSON_TYPE, null, out);

        JSONArray array = new JSONArray(out.toString());

        assertEquals(4, array.length());
        assertEquals("A", array.getJSONObject(0).getString("value"));
        assertEquals("D", array.getJSONObject(3).getString("value"));
        }

    @Test
    public void testXmlCollectionMarshalling()
            throws Exception
        {
        Collection<Letter> letters = Arrays.asList(new Letter("A"), new Letter("B"), new Letter("C"), new Letter("D"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlCollectionWriter w = new XmlCollectionWriter(new MarshallerRegistry());

        w.writeTo(letters, null, null, null, MediaType.APPLICATION_XML_TYPE, null, out);

        XmlDocument xmlDoc = XmlHelper.loadXml(out.toString());

        String[] expected = new String[] {"A", "B", "C", "D"};
        int i = 0;
        for (Iterator it = xmlDoc.getElements("letter"); it.hasNext();)
            {
            XmlElement el = (XmlElement) it.next();
            assertEquals(expected[i++], el.getElement("value").getValue());
            }
        }

    // ---- inner class: Letter ---------------------------------------------

    @XmlRootElement
    public static class Letter
        {
        public Letter()
            {
            }

        public Letter(String value)
            {
            this.m_value = value;
            }

        public String getValue()
            {
            return m_value;
            }

        public void setValue(String value)
            {
            m_value = value;
            }

        private String m_value;
        }
    }
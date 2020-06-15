/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.io;

import data.pof.PortablePerson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Abstract base class for various marshaller tests.
 *
 * @author as  2011.07.14
 */
public abstract class AbstractMarshallerTest
    {
    protected abstract Marshaller getMarshaller();
    protected abstract String getExpectedValue();
    protected abstract String getExpectedFragment();

    @Before
    public void setup()
        {
        m_sOutputOld = System.getProperty(Marshaller.FORMAT_OUTPUT);
        System.setProperty(Marshaller.FORMAT_OUTPUT, "false");
        }

    @After
    public void teardown()
        {
        String sOutputOld = m_sOutputOld;
        if (sOutputOld == null)
            {
            System.getProperties().remove(Marshaller.FORMAT_OUTPUT);
            }
        else
            {
            System.setProperty(Marshaller.FORMAT_OUTPUT, sOutputOld);
            }
        }

    @Test
    public void testMarshall()
            throws IOException
        {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        getMarshaller().marshal(m_person, out, null);
        String result = new String(out.toByteArray());

        assertEquals(getExpectedValue(), result);
        }

    @Test
    public void testMarshallAsFragment()
            throws IOException
        {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        getMarshaller().marshalAsFragment(m_person, out, null);
        String result = new String(out.toByteArray());

        assertEquals(getExpectedFragment(), result);
        }

    @Test
    public void testUnmarshall()
            throws IOException
        {
        ByteArrayInputStream in = new ByteArrayInputStream(getExpectedValue().getBytes());
        PortablePerson p = (PortablePerson) getMarshaller().unmarshal(in, null);

        assertEquals(m_person, p);
        }

    protected PortablePerson m_person = PortablePerson.create();
    protected String m_sOutputOld = null;
    }

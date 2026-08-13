/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.discovery;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.management.remote.JMXServiceURL;

import static org.junit.Assert.*;


/**
 * Unit test of the NSLookup class.
 *
 * @author jf  2024.10.07
 */
public class NSLookupTest
    {
    @Test
    public void shouldReportNameWarning()
        {
        for (String sName : NSLookup.VALID_PREDEFINED_LOOKUP_NAMES)
            {
            StringBuffer sbTypo = new StringBuffer(sName);

            sbTypo.setCharAt(0, Character.toUpperCase(sName.charAt(0)));
            sbTypo.setCharAt(1, Character.toLowerCase(sName.charAt(1)));
            sbTypo.setCharAt(2, Character.toUpperCase(sName.charAt(2)));
            assertNotNull("assert predefined lookup name " + sbTypo + " with injected typo results in a warning",
                          NSLookup.validateName(sbTypo.toString()));
            }
        }

    @Test
    public void mustNotReportNameWarning()
        {
        for (String sName : NSLookup.VALID_PREDEFINED_LOOKUP_NAMES)
            {
            assertNull("assert no warning for predefined lookup names", NSLookup.validateName(sName));
            }
        }

    @Test
    public void shouldValidateJmxServiceUrl()
            throws IOException
        {
        assertNotNull(NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://127.0.0.1:9000/jmxrmi")));
        assertNotNull(NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:rmi://127.0.0.1:9000")));
        assertNotNull(NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:iiop://127.0.0.1:9000/jndi/weblogic.management.mbeanservers.runtime")));
        assertNotNull(NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:iiops://127.0.0.1:9000/jndi/weblogic.management.mbeanservers.runtime")));
        assertNotNull(NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:iiop://127.0.0.1:9000/jndi/iiop://127.0.0.1:9000/weblogic.management.mbeanservers.runtime")));
        assertNotNull(NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:iiops://127.0.0.1:9000/jndi/iiops://127.0.0.1:9000/weblogic.management.mbeanservers.runtime")));
        assertNotNull(NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:t3://127.0.0.1:9000/jndi/weblogic.management.mbeanservers.runtime")));
        assertNotNull(NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:t3s://127.0.0.1:9000/jndi/weblogic.management.mbeanservers.runtime")));

        assertThrowsRuntime(() -> NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:rmi://127.0.0.1:9000/stub/abcd")));
        assertThrowsRuntime(() -> NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:rmi:///jndi/ldap://127.0.0.1:1389/jmxrmi")));
        assertThrowsRuntime(() -> NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:rmi:///jndi/ldaps://127.0.0.1:1636/jmxrmi")));
        assertThrowsRuntime(() -> NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:iiop:///jndi/iiop://127.0.0.1:9000/jmxrmi")));
        assertThrowsRuntime(() -> NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:rmi:///jndi/iiops://127.0.0.1:9000/jmxrmi")));
        assertThrowsRuntime(() -> NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:iiop://127.0.0.1:9000/jndi/")));
        assertThrowsRuntime(() -> NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:rmi:///jndi/dns://127.0.0.1/jmxrmi")));
        assertThrowsRuntime(() -> NSLookup.validateJMXServiceURL(new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi:///jmxrmi")));
        }

    @Test
    public void shouldReadNameServiceStringAsUtf8()
            throws IOException
        {
        assertEquals("caf\u00e9", NSLookup.readString(stringResult("caf\u00e9")));
        }

    @Test
    public void shouldRejectMalformedNameServiceStringHeader()
            throws IOException
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream      out   = new DataOutputStream(bytes);

        out.writeInt(NS_LOOKUP_STRING_RESPONSE_HEADER);
        out.writeShort(NS_LOOKUP_STRING_POF_HEADER - 1);
        NSLookup.writePackedInt(out, 1);
        out.write('x');
        out.flush();

        assertThrowsRuntime(() -> NSLookup.readString(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        }

    @Test
    public void shouldRejectOversizedNameServiceStrings()
            throws IOException
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream      out   = new DataOutputStream(bytes);

        out.writeInt(NS_LOOKUP_STRING_RESPONSE_HEADER);
        out.writeShort(NS_LOOKUP_STRING_POF_HEADER);
        NSLookup.writePackedInt(out, NSLookup.MAX_STRING_BYTES + 1);
        out.flush();

        assertThrowsRuntime(() -> NSLookup.readString(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        }

    @Test
    public void shouldRejectNegativeNameServiceStringLengths()
            throws IOException
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream      out   = new DataOutputStream(bytes);

        out.writeInt(NS_LOOKUP_STRING_RESPONSE_HEADER);
        out.writeShort(NS_LOOKUP_STRING_POF_HEADER);
        NSLookup.writePackedInt(out, -1);
        out.flush();

        assertThrowsRuntime(() -> NSLookup.readString(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        }

    @Test
    public void shouldRejectOversizedFrames()
            throws IOException
        {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream      out   = new DataOutputStream(bytes);

        NSLookup.writePackedInt(out, NSLookup.MAX_MESSAGE_BYTES + 1);
        out.flush();

        try
            {
            NSLookup.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
            fail("expected oversized frame rejection");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    private static DataInputStream stringResult(String sValue)
            throws IOException
        {
        byte[]                abValue = sValue.getBytes("UTF-8");
        ByteArrayOutputStream bytes   = new ByteArrayOutputStream();
        DataOutputStream      out     = new DataOutputStream(bytes);

        out.writeInt(NS_LOOKUP_STRING_RESPONSE_HEADER);
        out.writeShort(NS_LOOKUP_STRING_POF_HEADER);
        NSLookup.writePackedInt(out, abValue.length);
        out.write(abValue);
        out.flush();

        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        }

    private static void assertThrowsRuntime(ThrowingRunnable runnable)
        {
        try
            {
            runnable.run();
            fail("expected exception");
            }
        catch (Exception e)
            {
            // expected
            }
        }

    private interface ThrowingRunnable
        {
        void run() throws Exception;
        }

    private static final int NS_LOOKUP_STRING_RESPONSE_HEADER = 0x01004200;

    private static final int NS_LOOKUP_STRING_POF_HEADER = 0x034E;
    }

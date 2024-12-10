/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import java.net.InetSocketAddress;

import java.util.Comparator;
import java.util.Iterator;

import java.util.stream.Stream;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* Unit test of the ConfigurableAddressProvider class.
*
* @author jh,pp  2010.08.23
*/
public class ConfigurableAddressProviderTest
    {
    // ----- tests ----------------------------------------------------------

    /**
    * Assert that address rejection followed by address acceptance will
    * cause the <code>getNextAddress</code> call to start at the beginning
    * of the iteration instead of returning null.
    */
    @Test
    public void testAcceptLast()
        {
        ConfigurableAddressProvider provider = new ConfigurableAddressProvider(
                createConfiguration(2));

        InetSocketAddress addr = provider.getNextAddress();
        assertNotNull(addr);
        provider.reject(null);
        addr = provider.getNextAddress();
        assertNotNull(addr);
        provider.accept();
        addr = provider.getNextAddress();
        assertNotNull(addr);
        }

    /**
    * Assert that iteration of addresses without calling accept will result
    * in <code>null</code> being returned after all addresses have been
    * iterated.
    */
    @Test
    public void testIteration()
        {
        int                         cAddresses = 5;
        ConfigurableAddressProvider provider   = new ConfigurableAddressProvider(
                createConfiguration(cAddresses));

        for (int i = 0; i < cAddresses; i++)
            {
            provider.getNextAddress();
            }

        assertNull("Expected null after iterating all addresses",
                provider.getNextAddress());

        Iterator iterator = provider.iterator();
        try
            {
            iterator.remove();
            fail("Should have thrown UnsupportedOperationException");
            }
        catch (Exception e)
            {
            assertThat(e, is(instanceOf(UnsupportedOperationException.class)));
            }

        while (iterator.hasNext())
            {
            iterator.next();
            }

        assertFalse("Expected false after iterating all addresses",
                iterator.hasNext());
        }

    /**
    * Assert that calling <code>accept</code> after iterating each address
    * will result in the first address being returned again.
    */
    @Test
    public void testIterationWithAccept()
        {
        int                         cAddresses = 5;
        ConfigurableAddressProvider provider   = new ConfigurableAddressProvider(
                createConfiguration(cAddresses));

        InetSocketAddress firstAddr = provider.getNextAddress();
        provider.accept();

        for (int i = 0; i < cAddresses - 1; i++)
            {
            provider.getNextAddress();
            provider.accept();
            }

        assertEquals("Expected first address after iterating all addresses",
                firstAddr, provider.getNextAddress());
        }

    /**
     * Assert that comma separated addresses are correctly parsed.
     */
    @Test
    public void testCommaDelimitedAddresses()
        {
        InetSocketAddress[] expected = new InetSocketAddress[]
                {
                        new InetSocketAddress("127.0.0.1", 81),
                        new InetSocketAddress("127.0.0.2", 99),
                        new InetSocketAddress("127.0.0.3", 99),
                        new InetSocketAddress("127.0.0.4", 99),
                        new InetSocketAddress("127.0.0.5", 0),
                        new InetSocketAddress("127.0.0.6", 0),
                        new InetSocketAddress("127.0.0.7", 0)
                };

        ConfigurableAddressProvider provider = new ConfigurableAddressProvider(createCommaDelimitedConfiguration());
        InetSocketAddress[] actual           = ((Stream<InetSocketAddress>) provider.stream())
                .sorted(Comparator.comparing(InetSocketAddress::toString))
                .toArray(InetSocketAddress[]::new);

        assertArrayEquals(expected, actual);
        }

    // ----- helper methods -------------------------------------------------

    /**
    * Create a ConfigurableAddressProvider XML configuration.
    *
    * @param cAddresses  number of addresses to include in configuration;
    *                    values from 1 to 9 are supported 
    *
    * @return XML configuration containing the number of addresses specified
    */
    protected XmlElement createConfiguration(int cAddresses)
        {
        if (cAddresses < 1 || cAddresses > 9)
            {
            throw new IllegalArgumentException("Supported values are 1 to 9");
            }
        
        StringBuilder builder = new StringBuilder();
        builder.append("<socket-addresses>");
        for (int i = 0; i < cAddresses; i++)
            {
            builder.append("<socket-address><address>localhost</address><port>80").
                    append(i).append("</port></socket-address>");
            }
        builder.append("</socket-addresses>");

        return XmlHelper.loadXml(builder.toString());
        }

    /**
     * Create a ConfigurableAddressProvider XML configuration.
     *
     * @return XML configuration containing comma separated addresses
     */
    protected XmlElement createCommaDelimitedConfiguration()
        {
        StringBuilder builder = new StringBuilder();
        builder.append("<socket-addresses>");
        builder.append("<socket-address><address>127.0.0.1</address><port>81</port></socket-address>");

        builder.append("<socket-address>");
        builder.append("<address>127.0.0.2, 127.0.0.3, 127.0.0.4</address>");
        builder.append("<port>99</port>");
        builder.append("</socket-address>");

        builder.append("<address>127.0.0.5, 127.0.0.6, 127.0.0.7</address>");

        builder.append("</socket-addresses>");
        return XmlHelper.loadXml(builder.toString());
        }
    }

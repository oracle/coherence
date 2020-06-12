/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.net.InetAddress;

/**
 * A {@link Converter} for {@link InetAddress} instances.
 *
 * @author Aleks Seovic  2018.05.31
* @since 20.06
 */
public class InetAddressConverter
        implements Converter<InetAddress>
    {
    // ----- constructors -----------------------------------------------------

    /**
     * Creates a new {@code InetAddressConverter}.
     */
    protected InetAddressConverter()
        {
        }

    // ----- Converter interface ----------------------------------------------

    @Override
    public void serialize(InetAddress address, ObjectWriter writer, Context ctx)
            throws Exception
        {
        writer.beginObject();
        writer.writeString("host", address.getHostName());
        writer.writeString("address", address.getHostAddress());
        writer.endObject();
        }

    @Override
    public InetAddress deserialize(ObjectReader reader, Context ctx)
            throws Exception
        {
        String host = null;
        String address = null;

        reader.beginObject();
        while (reader.hasNext())
            {
            reader.next();
            if ("host".equals(reader.name()))
                {
                host = reader.valueAsString();
                }
            else if ("address".equals(reader.name()))
                {
                address = reader.valueAsString();
                }
            }
        reader.endObject();

        return address == null
               ? host == null ? null : InetAddress.getByName(host)
               : host == null
                 ? InetAddress.getByAddress(toBytes(address))
                 : InetAddress.getByAddress(host, toBytes(address));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Converts a String address into an array of bytes.
     *
     * @param address  the address to convert
     *
     * @return the address as an array of bytes.
     */
    protected static byte[] toBytes(String address)
        {
        return address.indexOf('.') > 0
               ? IPAddressUtil.textToNumericFormatV4(address)
               : IPAddressUtil.textToNumericFormatV6(address);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton instance of {@code InetAddressConverter}.
     */
    public static final InetAddressConverter INSTANCE = new InetAddressConverter();
    }

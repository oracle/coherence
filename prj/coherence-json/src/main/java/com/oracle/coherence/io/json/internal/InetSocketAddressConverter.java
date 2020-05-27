/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;

import com.oracle.coherence.io.json.genson.stream.JsonStreamException;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * A {@link Converter} for {@link InetSocketAddress} instances.
 *
 * @since 14.1.2
 */
public class InetSocketAddressConverter
        implements Converter<InetSocketAddress>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a new {@code InetSocketAddressConverter}.
     */
    protected InetSocketAddressConverter()
        {
        }

    // ----- Converter interface --------------------------------------------

    @Override
    public void serialize(InetSocketAddress address, ObjectWriter writer, Context ctx)
            throws Exception
        {
        writer.beginObject();
        writer.writeName("address");
        InetAddressConverter.INSTANCE.serialize(address.getAddress(), writer, ctx);
        writer.writeNumber("port", address.getPort());
        writer.endObject();
        }

    @Override
    public InetSocketAddress deserialize(ObjectReader reader, Context ctx)
            throws Exception
        {
        InetAddress host = null;
        int         port = 0;

        reader.beginObject();
        while (reader.hasNext())
            {
            reader.next();
            if ("address".equals(reader.name()))
                {
                host = InetAddressConverter.INSTANCE.deserialize(reader, ctx);
                }
            else if ("port".equals(reader.name()))
                {
                port = reader.valueAsInt();
                }
            }
        reader.endObject();

        if (host == null)
            {
            throw new JsonStreamException("Unable to deserialize InetSocketAddress; address missing");
            }

        return new InetSocketAddress(host.getHostAddress(), port);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton instance of {@code InetSocketAddressConverter}.
     */
    public static final InetSocketAddressConverter INSTANCE = new InetSocketAddressConverter();
    }

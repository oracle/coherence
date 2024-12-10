/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * POF serializer for {@code java.net.InetSocketAddress}.
 *
 * @author  mf  2015.06.09
 */
public class InetSocketAddressSerializer
        implements PofSerializer<InetSocketAddress>
    {
    @Override
    public void serialize(PofWriter out, InetSocketAddress addr)
            throws IOException
        {
        out.writeByteArray(0, addr.getAddress().getAddress());
        out.writeInt(1, addr.getPort());
        out.writeRemainder(null);
        }

    @Override
    public InetSocketAddress deserialize(PofReader in)
            throws IOException
        {
        byte[] abAddr = in.readByteArray(0);
        int    nPort  = in.readInt(1);
        in.readRemainder();

        return new InetSocketAddress(InetAddress.getByAddress(abAddr), nPort);
        }
    }
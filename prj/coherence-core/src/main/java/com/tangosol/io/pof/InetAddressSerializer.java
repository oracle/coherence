/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import java.io.IOException;

import java.net.InetAddress;

/**
 * POF serializer for {@code java.net.InetAddress}.
 *
 * @author  mf  2015.06.09
 */
public class InetAddressSerializer
        implements PofSerializer<InetAddress>
    {
    @Override
    public void serialize(PofWriter out, InetAddress addr)
            throws IOException
        {
        out.writeByteArray(0, addr.getAddress());
        out.writeRemainder(null);
        }

    @Override
    public InetAddress deserialize(PofReader in)
            throws IOException
        {
        byte[] abAddr = in.readByteArray(0);
        in.readRemainder();

        return InetAddress.getByAddress(abAddr);
        }
    }
/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import java.io.IOException;

import java.util.OptionalLong;

/**
 * POF serializer for {@code java.util.OptionalLong}.
 *
 * @author as  2014.10.07
 */
public class OptionalLongSerializer
        implements PofSerializer<OptionalLong>
    {
    public void serialize(PofWriter out, OptionalLong value) throws IOException
        {
        boolean fPresent = value.isPresent();
        out.writeBoolean(0, fPresent);
        if (fPresent)
            {
            out.writeLong(1, value.getAsLong());
            }

        out.writeRemainder(null);
        }

    public OptionalLong deserialize(PofReader in) throws IOException
        {
        boolean fPresent = in.readBoolean(0);
        OptionalLong value = fPresent ? OptionalLong.of(in.readLong(1)) : OptionalLong.empty();
        in.readRemainder();
        return value;
        }
    }

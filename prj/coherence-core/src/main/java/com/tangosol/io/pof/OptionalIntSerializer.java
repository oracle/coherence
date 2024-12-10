/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import java.io.IOException;

import java.util.OptionalInt;

/**
 * POF serializer for {@code java.util.OptionalInt}.
 *
 * @author as  2014.10.07
 */
public class OptionalIntSerializer
        implements PofSerializer<OptionalInt>
    {
    public void serialize(PofWriter out, OptionalInt value) throws IOException
        {
        boolean fPresent = value.isPresent();
        out.writeBoolean(0, fPresent);
        if (fPresent)
            {
            out.writeInt(1, value.getAsInt());
            }

        out.writeRemainder(null);
        }

    public OptionalInt deserialize(PofReader in) throws IOException
        {
        boolean fPresent = in.readBoolean(0);
        OptionalInt value = fPresent ? OptionalInt.of(in.readInt(1)) : OptionalInt.empty();
        in.readRemainder();
        return value;
        }
    }

/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import java.io.IOException;

import java.util.OptionalDouble;

/**
 * POF serializer for {@code java.util.OptionalDouble}.
 *
 * @author as  2014.10.07
 */
public class OptionalDoubleSerializer
        implements PofSerializer<OptionalDouble>
    {
    public void serialize(PofWriter out, OptionalDouble value) throws IOException
        {
        boolean fPresent = value.isPresent();
        out.writeBoolean(0, fPresent);
        if (fPresent)
            {
            out.writeDouble(1, value.getAsDouble());
            }

        out.writeRemainder(null);
        }

    public OptionalDouble deserialize(PofReader in) throws IOException
        {
        boolean fPresent = in.readBoolean(0);
        OptionalDouble value = fPresent ? OptionalDouble.of(in.readInt(1)) : OptionalDouble.empty();
        in.readRemainder();
        return value;
        }
    }

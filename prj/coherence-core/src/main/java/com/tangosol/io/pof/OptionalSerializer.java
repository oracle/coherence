/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import java.io.IOException;

import java.util.Optional;

/**
 * POF serializer for {@code java.util.Optional}.
 *
 * @author as  2014.10.07
 */
public class OptionalSerializer implements PofSerializer<Optional>
    {
    public void serialize(PofWriter out, Optional value) throws IOException
        {
        boolean fPresent = value.isPresent();
        out.writeBoolean(0, fPresent);
        if (fPresent)
            {
            out.writeObject(1, value.get());
            }

        out.writeRemainder(null);
        }

    public Optional deserialize(PofReader in) throws IOException
        {
        boolean fPresent = in.readBoolean(0);
        Optional value = fPresent ? Optional.of(in.readObject(1)) : Optional.empty();
        in.readRemainder();
        return value;
        }
    }

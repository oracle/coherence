/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;

import java.io.IOException;
import java.util.UUID;

/**
 * A {@link PofSerializer} for a Java {@link UUID}.
 */
public class UuidSerializer
        implements PofSerializer<UUID>
    {
    @Override
    public void serialize(PofWriter out, UUID uuid) throws IOException
        {
        out.writeLong(0, uuid.getLeastSignificantBits());
        out.writeLong(1, uuid.getMostSignificantBits());
        out.writeRemainder(null);
        }

    @Override
    public UUID deserialize(PofReader in) throws IOException
        {
        long lsb = in.readLong(0);
        long msb = in.readLong(1);
        in.readRemainder();
        return new UUID(msb, lsb);
        }
    }

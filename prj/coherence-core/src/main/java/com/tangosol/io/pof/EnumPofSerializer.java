/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.io.IOException;


/**
* {@link PofSerializer} implementation that can be used to serialize all enum
* values.
*
* @author as  2008.10.24
*/
public class EnumPofSerializer
        implements PofSerializer
    {
    // ---- PofSerializer implementation ------------------------------------

    /**
    * {@inheritDoc}
    */
    public void serialize(PofWriter writer, Object o)
            throws IOException
        {
        // COH-9833 getClass().isEnum() may return false for certain enums
        if (!(o instanceof Enum))
            {
            throw new IllegalArgumentException(
                    "EnumPofSerializer can only be used to serialize enum types.");
            }

        writer.writeString(0, ((Enum) o).name());
        writer.writeRemainder(null);
        }

    /**
    * {@inheritDoc}
    */
    public Object deserialize(PofReader reader)
            throws IOException
        {
        PofContext ctx = reader.getPofContext();
        Class      clz = ctx.getClass(reader.getUserTypeId());

        if (!clz.isEnum())
            {
            throw new IllegalArgumentException(
                    "EnumPofSerializer can only be used to deserialize enum types.");
            }

        Enum enumValue = Enum.valueOf(clz, reader.readString(0));
        reader.registerIdentity(enumValue);
        reader.readRemainder();

        return enumValue;
        }
    }

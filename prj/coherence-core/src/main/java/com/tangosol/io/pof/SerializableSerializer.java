/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;


import com.tangosol.io.DefaultSerializer;
import com.tangosol.util.ExternalizableHelper;
import java.io.IOException;


/**
 * @author Aleksandar Seovic  2014.06.16
 */
public class SerializableSerializer
        implements PofSerializer
    {
    private static final DefaultSerializer DEFAULT_SERIALIZER = new DefaultSerializer();

    public void serialize(PofWriter writer, Object o) throws IOException
        {
        writer.writeBinary(0, ExternalizableHelper.toBinary(o, DEFAULT_SERIALIZER));
        writer.writeRemainder(null);
        }

    public Object deserialize(PofReader reader) throws IOException
        {
        Object o = ExternalizableHelper.fromBinary(reader.readBinary(0), DEFAULT_SERIALIZER);
        reader.readRemainder();
        return o;
        }
    }

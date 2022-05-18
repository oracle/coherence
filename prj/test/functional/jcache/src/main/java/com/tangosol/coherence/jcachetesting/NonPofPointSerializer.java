/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

public class NonPofPointSerializer
        implements PofSerializer
    {
    /**
     * Method description
     *
     * @param pofWriter
     * @param o
     *
     * @throws IOException
     */
    @Override
    public void serialize(PofWriter pofWriter, Object o)
            throws IOException
        {
        NonPofPoint oP = (NonPofPoint) o;

        pofWriter.writeInt(0, oP.x);
        pofWriter.writeInt(1, oP.y);
        pofWriter.writeRemainder(null);
        }

    /**
     * Method description
     *
     * @param pofReader
     *
     * @return
     *
     * @throws IOException
     */
    @Override
    public Object deserialize(PofReader pofReader)
            throws IOException
        {
            int x = pofReader.readInt(0);
            int y = pofReader.readInt(1);
            pofReader.readRemainder();
            return new NonPofPoint(x, y);
        }
    }

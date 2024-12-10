/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;


import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.WriteBuffer.BufferOutput;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
*
* @author coh 2010-09-09
*/
public class PofBufferTest
    {
    @Before
    public void doBefore()
        {
        m_buffer.clear();
        }
    @Test
    public void testBigDecimalNull() throws IOException
        {
        PofBufferWriter writer = new PofBufferWriter(getOutput(), m_ctx);
        WritingPofHandler handler = writeComplexTypeHeader(writer);
        writer.writeInt(0, 0);
        writer.writeBigDecimal(1, null);
        handler.endComplexValue();

        PofReader reader = readComplexTypeHeader();
        Assert.assertEquals(0, reader.readInt(0));
        Assert.assertEquals(null, reader.readBigDecimal(1));
        }

    @Test
    public void testBigIntegerNull() throws IOException
        {
        PofBufferWriter writer = new PofBufferWriter(getOutput(), m_ctx);
        WritingPofHandler handler = writeComplexTypeHeader(writer);
        writer.writeInt(0, 0);
        writer.writeBigInteger(1, null);
        handler.endComplexValue();

        PofReader reader = readComplexTypeHeader();
        Assert.assertEquals(0, reader.readInt(0));
        Assert.assertEquals(null, reader.readBigInteger(1));
        }

    /**
    *
    * @return
    */
    private BufferInput getInput()
        {
        return m_buffer.getReadBuffer().getBufferInput();
        }

    /**
    *
    * @return
    */
    private BufferOutput getOutput()
        {
        return m_buffer.getBufferOutput();
        }

    /**
    *
    * @return
    * @throws IOException
    */
    private PofReader readComplexTypeHeader() throws IOException
        {
        BufferInput in = getInput();
        int type = in.readPackedInt();
        PofReader reader = new PofBufferReader.UserTypeReader(
                    in, m_ctx, type, in.readPackedInt());
        return reader;
        }

    /**
    *
    * @param writer
    * @return
    */
    private WritingPofHandler writeComplexTypeHeader(PofBufferWriter writer)
        {
        WritingPofHandler handler = writer.getPofHandler();
        handler.beginUserType(-1, -1, 1000, 0);
        return handler;
        }

    @BeforeClass
    public static void setUp()
        {
        m_buffer = new ByteArrayWriteBuffer(1024);
        m_ctx    = new ConfigurablePofContext();
        }

    private static ByteArrayWriteBuffer m_buffer;

    private static PofContext m_ctx;
    }

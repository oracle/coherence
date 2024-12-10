/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;


/**
* Unit tests of basic POF primitive value serialization/deserialization.
*
* @author gm 2006.12.21
* @author jh 2006.12.27
*/
public class PofPrimitiveValueTest
        extends AbstractPofTest
    {
    @Test
    public void testPofWriteBoolean()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeBoolean(0, true);
        m_writer.writeBoolean(0, false);
        m_writer.writeBoolean(0, true);
        m_writer.writeBoolean(0, true);

        initPOFReader();
        assertTrue(m_reader.readBoolean(0));
        assertFalse(m_reader.readBoolean(0));
        assertTrue(m_reader.readBoolean(0));

        Object obool = m_reader.readObject(0);
        assertTrue(obool instanceof Boolean);

        Boolean rbool = (Boolean) obool;
        assertTrue(rbool.booleanValue());
        }

    @Test
    public void testPofWriteByte()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeByte(0, (byte) 1);
        m_writer.writeByte(0, (byte) 2);
        m_writer.writeByte(0, (byte) 127);
        m_writer.writeByte(0, (byte) -128);
        m_writer.writeByte(0, (byte) 0);

        initPOFReader();
        assertEquals(m_reader.readByte(0), 1);
        assertEquals(m_reader.readByte(0), 2);
        assertEquals(m_reader.readByte(0), 127);
        assertEquals(m_reader.readByte(0), -128);
        assertEquals(m_reader.readByte(0), 0);
        }

    @Test
    public void testPofWriteChar()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeChar(0, 'g');
        m_writer.writeChar(0, Character.MAX_VALUE);
        m_writer.writeChar(0, 'A');
        m_writer.writeChar(0, Character.MIN_VALUE);
        m_writer.writeChar(0, '%');

        initPOFReader();
        assertEquals(m_reader.readChar(0), 'g');
        assertEquals(m_reader.readChar(0), Character.MAX_VALUE);
        assertEquals(m_reader.readChar(0), 'A');
        assertEquals(m_reader.readChar(0), Character.MIN_VALUE);
        assertEquals(m_reader.readChar(0), '%');
        }

    @Test
    public void testPofWriteIntShort()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeShort(0, (short) 0);
        m_writer.writeShort(0, (short) -1);
        m_writer.writeShort(0, Short.MAX_VALUE);
        m_writer.writeShort(0, (short) 101);
        m_writer.writeShort(0, Short.MIN_VALUE);

        initPOFReader();
        assertEquals(m_reader.readShort(0), 0);
        assertEquals(m_reader.readShort(0), -1);
        assertEquals(m_reader.readShort(0), Short.MAX_VALUE);
        assertEquals(m_reader.readShort(0), 101);
        assertEquals(m_reader.readShort(0), Short.MIN_VALUE);
        }

    @Test
    public void testPofWriteInt()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeInt(0, 0);
        m_writer.writeInt(0, -1);
        m_writer.writeInt(0, Integer.MIN_VALUE);
        m_writer.writeInt(0, 101);
        m_writer.writeInt(0, Integer.MAX_VALUE);

        initPOFReader();
        assertEquals(m_reader.readInt(0), 0);
        assertEquals(m_reader.readInt(0), -1);
        assertEquals(m_reader.readInt(0), Integer.MIN_VALUE);
        assertEquals(m_reader.readInt(0), 101);
        assertEquals(m_reader.readInt(0), Integer.MAX_VALUE);
        }

    @Test
    public void testPofWriteLong()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeLong(0, 0);
        m_writer.writeLong(0, -1);
        m_writer.writeLong(0, Long.MAX_VALUE);
        m_writer.writeLong(0, 101);
        m_writer.writeLong(0, Long.MIN_VALUE);

        initPOFReader();
        assertEquals(m_reader.readLong(0), 0);
        assertEquals(m_reader.readLong(0), -1);
        assertEquals(m_reader.readLong(0), Long.MAX_VALUE);
        assertEquals(m_reader.readLong(0), 101);
        assertEquals(m_reader.readLong(0), Long.MIN_VALUE);
        }

    @Test
    public void testPofWriteFloat()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeFloat(0, 0.0F);
        m_writer.writeFloat(0, -1.0F);
        m_writer.writeFloat(0, Float.MAX_VALUE);
        m_writer.writeFloat(0, 100000F);
        m_writer.writeFloat(0, Float.MIN_VALUE);
        m_writer.writeFloat(0, Float.NEGATIVE_INFINITY);
        m_writer.writeFloat(0, Float.POSITIVE_INFINITY);
        m_writer.writeFloat(0, Float.NaN);

        initPOFReader();
        assertEquals(m_reader.readFloat(0), 0.0F, 0);
        assertEquals(m_reader.readFloat(0), -1.0F, 0);
        assertEquals(m_reader.readFloat(0), Float.MAX_VALUE, 0);
        assertEquals(m_reader.readFloat(0), 100000F, 0);
        assertEquals(m_reader.readFloat(0), Float.MIN_VALUE, 0);
        assertEquals(m_reader.readFloat(0), Float.NEGATIVE_INFINITY, 0);
        assertEquals(m_reader.readFloat(0), Float.POSITIVE_INFINITY, 0);
        // REVIEW
        assertEquals(Float.valueOf(m_reader.readFloat(0)), Float.valueOf(Float.NaN));
        }

    @Test
    public void testPofWriteDouble()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeDouble(0, 0);
        m_writer.writeDouble(0, -1.0);
        m_writer.writeDouble(0, 1.0);
        m_writer.writeDouble(0, Double.MAX_VALUE);
        m_writer.writeDouble(0, 100.0);
        m_writer.writeDouble(0, Double.MIN_VALUE);
        m_writer.writeDouble(0, Double.NEGATIVE_INFINITY);
        m_writer.writeDouble(0, Double.POSITIVE_INFINITY);
        m_writer.writeDouble(0, Double.NaN);

        initPOFReader();
        assertEquals(m_reader.readDouble(0), 0.0, 0);
        assertEquals(m_reader.readDouble(0), -1.0, 0);
        assertEquals(m_reader.readDouble(0), 1.0, 0);
        assertEquals(m_reader.readDouble(0), Double.MAX_VALUE, 0);
        assertEquals(m_reader.readDouble(0), 100.0, 0);
        assertEquals(m_reader.readDouble(0), Double.MIN_VALUE, 0);
        assertEquals(m_reader.readDouble(0), Double.NEGATIVE_INFINITY, 0);
        assertEquals(m_reader.readDouble(0), Double.POSITIVE_INFINITY, 0);
        // REVIEW
        assertEquals(Double.valueOf(m_reader.readDouble(0)), Double.valueOf(Double.NaN));
        }

    @Test
    public void testPofStreamBadAdvanceTo()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeFloat(0, 100.0F);

        initPOFReader();
        try
            {
            assertEquals(m_reader.readFloat(1), 100.0F, 0);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }
    }

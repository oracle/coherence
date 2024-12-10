/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.data;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.io.pof.PofConstants;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.InputStream;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Short;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.net.URL;

import static org.junit.Assert.*;

import com.tangosol.util.Resources;
import org.junit.Test;

/*
 * Test for POF serialization between platforms.  Reads type
 * data files and deserializes the data contained within.
 * Currently reads data files generated on .NET.
 */
public class PofStreamTest
        extends Base
   {

    @Test
    public void testByte()
        {
        PofBufferReader pofReader = initPofBufferReader("Byte");
        try {
            assertEquals(1,         pofReader.readByte(0));
            assertEquals(0,         pofReader.readByte(0));
            assertEquals((byte)200, pofReader.readByte(0));
            assertEquals((byte)255, pofReader.readByte(0));
            }
        catch (IOException ioe)
            {
            fail("Exception reading Byte pof data file");
            }
        }

    @Test
    public void testChar()
        {
        PofBufferReader pofReader = initPofBufferReader("Char");
        try {
            assertEquals('f', pofReader.readChar(0));
            assertEquals('0', pofReader.readChar(0));
            }
        catch (IOException ioe)
            {
            fail("Exception reading Char pof data file");
            }
        }

    @Test
    public void testInt16()
        {
        PofBufferReader pofReader = initPofBufferReader("Int16");
        try {
            assertEquals((short) -1,      pofReader.readShort(0));
            assertEquals((short) 0,       pofReader.readShort(0));
            assertEquals(Short.MAX_VALUE, pofReader.readShort(0));
            }
        catch (IOException ioe)
            {
            fail("Exception reading Int16 pof data file");
            }
        }

    @Test
    public void testInt32()
        {
        PofBufferReader pofReader = initPofBufferReader("Int32");
        try {
            assertEquals((int) 255,         pofReader.readInt(0));
            assertEquals((int) -12345,      pofReader.readInt(0));
            assertEquals(Integer.MAX_VALUE, pofReader.readInt(0));
            }
        catch (IOException ioe)
            {
            fail("Exception reading Int32 pof data file");
            }
        }

    @Test
    public void testInt64()
        {
        PofBufferReader pofReader = initPofBufferReader("Int64");
        try {
            assertEquals((long) -1,      pofReader.readLong(0));
            assertEquals(Long.MAX_VALUE, pofReader.readLong(0));
            }
        catch (IOException ioe)
            {
            fail("Exception reading Int64 pof data file");
            }
        }

    @Test
    public void testInt128()
        {
        PofBufferReader pofReader = initPofBufferReader("Int128");
        try {
            assertEquals(BigInteger.TEN, pofReader.readBigInteger(0));
            assertEquals(new BigInteger("555555555555555555"), pofReader.readBigInteger(0));
            }
        catch (IOException ioe)
            {
            fail("Exception reading Int128 pof data file");
            }
        }

    @Test
    public void testDecimal32()
        {
        PofBufferReader pofReader = initPofBufferReader("Dec32");
        BigInteger nI    = new BigInteger("9999999");
        BigDecimal nBig1 = new BigDecimal("99999");
        BigDecimal nBig2 = new BigDecimal(nI, 0);
        BigDecimal nBig3 = new BigDecimal(nI, 28);

        try {
            assertEquals(nBig1, pofReader.readBigDecimal(0));
            assertEquals(nBig2, pofReader.readBigDecimal(0));
            assertEquals(nBig3, pofReader.readBigDecimal(0));
            }
        catch (IOException ioe)
            {
            fail("Exception reading Dec32 pof data file");
            }
        }

    @Test
    public void testDecimal64()
        {
        PofBufferReader pofReader = initPofBufferReader("Dec64");
        BigInteger nI    = new BigInteger("9999999999999999");
        BigDecimal nBig1 = new BigDecimal("9999999999");
        BigDecimal nBig2 = new BigDecimal(nI, 0);
        BigDecimal nBig3 = new BigDecimal(nI, 28);

        try {
            assertEquals(nBig1, pofReader.readBigDecimal(0));
            assertEquals(nBig2, pofReader.readBigDecimal(0));
            assertEquals(nBig3, pofReader.readBigDecimal(0));
            }
        catch (IOException ioe)
            {
            fail("Exception reading Dec64 pof data file");
            }
        }

    @Test
    public void testDecimal128()
        {
        PofBufferReader pofReader = initPofBufferReader("Dec128");
        BigInteger nI = (new BigInteger(NET_MAX_INTEGER)).add(BigInteger.ONE);

        try {
            assertEquals(new BigDecimal(NET_MAX_DECIMAL), pofReader.readBigDecimal(0));
            assertEquals(new BigDecimal(nI),     pofReader.readBigDecimal(0));
            assertEquals(new BigDecimal(nI, 28), pofReader.readBigDecimal(0));
            }
        catch (IOException ioe)
            {
            fail("Exception reading Dec128 pof data file");
            }
        }

    // --------Utility methods for reading/writing files ------------------------------

    private PofBufferReader initPofBufferReader(String sFileName)
        {
        byte[] ab = null;

        try {
            ab = readFromFile(sFileName);
            }
        catch (FileNotFoundException fnfe)
            {
            fail("FileNotFoundException reading pof data file: " + fnfe.getMessage());
            }
        catch (IOException ioe)
            {
            fail("IOException reading pof data file");
            }

        SimplePofContext    ctx       = new SimplePofContext();
        ByteArrayReadBuffer rb        = new ByteArrayReadBuffer(ab);
        PofBufferReader     pofReader = new PofBufferReader(rb.getBufferInput(), ctx);
        return pofReader;
        }



    private byte[] readFromFile(String sFile)
            throws FileNotFoundException, IOException
        {
        URL         url = Resources.findFileOrResource(m_sFileInDir + sFile + m_sFileExt, null);
        InputStream fis = url.openStream();

        // dump serialized object by byte
        byte[] ab = new byte[fis.available()];

        fis.read(ab);

        fis.close();
        return ab;
        }

    private void writeToFile(byte[] ba, String dataType)
        {
        try {
            FileOutputStream fos = new FileOutputStream(m_sFileOutDir + dataType + m_sFileExt);
            fos.write(ba);
            fos.flush();
            fos.close();
            }
        catch (FileNotFoundException fnfe)
            {
            fail("FileNotFoundException writing Byte pof data file");
            }
        catch (IOException ioe)
            {
            fail("IOException writing Byte pof data file");
            }
        }

    // -------Utility methods for writing data files--------------------------

    private void writeByte()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);

        try {
            pofWriter.writeByte(0, (byte) 1);
            pofWriter.writeByte(0, (byte) 0);
            pofWriter.writeByte(0, (byte) 200);
            pofWriter.writeByte(0, (byte) 255);
            }
        catch (IOException ioe)
            {
            fail("IOException serializing Byte pof data");
            }
        writeToFile(wb.toByteArray(), "Byte");
        }

    private void writeChar()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);

        try {
            pofWriter.writeChar(0, 'f');
            pofWriter.writeChar(0, '0');
            }
        catch (IOException ioe)
            {
            fail("IOException serializing Char pof data");
            }
        writeToFile(wb.toByteArray(), "Char");
        }

    private void writeInt16()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);

        try {
            pofWriter.writeShort(0, (short) -1);
            pofWriter.writeShort(0, (short) 0);
            pofWriter.writeShort(0, Short.MAX_VALUE);
            }
        catch (IOException ioe)
            {
            fail("IOException serializing Int16 pof data");
            }
        writeToFile(wb.toByteArray(), "Int16");
        }

    private void writeInt32()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);

        try {
            pofWriter.writeInt(0, (int) 255);
            pofWriter.writeInt(0, (int) -12345);
            pofWriter.writeInt(0, Integer.MAX_VALUE);
            }
        catch (IOException ioe)
            {
            fail("IOException serializing Int32 pof data");
            }
        writeToFile(wb.toByteArray(), "Int32");
        }

    private void writeInt64()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);

        try {
            pofWriter.writeLong(0, (long) -1);
            pofWriter.writeLong(0, Long.MAX_VALUE);
            }
        catch (IOException ioe)
            {
            fail("IOException serializing Int64 pof data");
            }
        writeToFile(wb.toByteArray(), "Int64");
        }

    private void writeInt128()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);

        try {
            pofWriter.writeBigInteger(0, BigInteger.TEN);
            pofWriter.writeBigInteger(0, new BigInteger("555555555555555555"));
            }
        catch (IOException ioe)
            {
            fail("IOException serializing Int128 pof data");
            }
        writeToFile(wb.toByteArray(), "Int128");
        }

    private void writeDecimal32()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);
        BigInteger           nI        = new BigInteger("9999999");

        try {
            pofWriter.writeBigDecimal(0, new BigDecimal("99999"));
            pofWriter.writeBigDecimal(0, new BigDecimal(nI, 0));
            pofWriter.writeBigDecimal(0, new BigDecimal(nI, 28));
            }
        catch (IOException ioe)
            {
            fail("IOException serializing Dec32 pof data");
            }
        writeToFile(wb.toByteArray(), "Dec32");
        }

    private void writeDecimal64()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);
        BigInteger           nI        = new BigInteger("9999999999999999");

        try {
            pofWriter.writeBigDecimal(0, new BigDecimal("9999999999"));
            pofWriter.writeBigDecimal(0, new BigDecimal(nI, 0));
            pofWriter.writeBigDecimal(0, new BigDecimal(nI, 28));
            }
        catch (IOException ioe)
            {
            fail("IOException serializing Dec64 pof data");
            }
        writeToFile(wb.toByteArray(), "Dec64");
        }

    private void writeDecimal128()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);
        BigInteger           nI        = (new BigInteger(NET_MAX_INTEGER)).add(BigInteger.ONE);

        try {
            pofWriter.writeBigDecimal(0, new BigDecimal(NET_MAX_DECIMAL));
            pofWriter.writeBigDecimal(0, new BigDecimal(nI));
            pofWriter.writeBigDecimal(0, new BigDecimal(nI, 28));
            }
        catch (IOException ioe)
            {
            fail("IOException serializing Dec128 pof data");
            }
        writeToFile(wb.toByteArray(), "Dec128");
        }

    private void writeMaxDecimal128()
        {
        SimplePofContext     ctx       = new SimplePofContext();
        ByteArrayWriteBuffer wb        = new ByteArrayWriteBuffer(1024);
        PofBufferWriter      pofWriter = new PofBufferWriter(wb.getBufferOutput(), ctx);

        try {
            pofWriter.writeBigDecimal(0, new BigDecimal(PofConstants.MAX_DECIMAL128_UNSCALED));
            }
        catch (IOException ioe)
            {
            fail("IOException serializing MaxDec128 pof data");
            }
        writeToFile(wb.toByteArray(), "MaxDec128");
        }

    /*
     * Method for writing data files.  Do this:
     * set COH_HOME=xxxx (example: COH_HOME=d:\coherence\dev\sandbox\Elvis\main)
     * p4 edit the data files in %COH_HOME%\tests\data\java
     *  "java -cp %COH_HOME%\build\lib\coherence.jar;%COH_HOME%\build\tests\classes \
     *                 com.tangosol.io.pof.data.PofStreamTests"
     */
    public static void main(String[] args)
        {
        PofStreamTest pst = new PofStreamTest();
        pst.writeByte();
        pst.writeChar();
        pst.writeInt16();
        pst.writeInt32();
        pst.writeInt64();
        pst.writeInt128();
        pst.writeDecimal32();
        pst.writeDecimal64();
        pst.writeDecimal128();
        pst.writeMaxDecimal128();
        }

    // --------Data members and constants-------------------------------------

    private static String m_sFileInDir  = "data/dotnet/";
    private static String m_sFileOutDir = "src/test/resources/data/java/";

    private static String m_sFileExt = ".data";

    private static String NET_MAX_DECIMAL = "79228162514264337593543950335"; // max for 96 bits
    private static String NET_MAX_INTEGER = "9223372036854775807"; // max for 96 bits
    }


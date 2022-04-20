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

import java.math.BigInteger;
import java.math.BigDecimal;


/**
* Unit tests of various PofHelper POF encoding/decoding methods.
*
* @author gm 2006.12.21
* @author jh 2006.12.27
*/
public class PofHelperEncodingTest
        extends AbstractPofTest
    {
    @Test
    public void testEncodeTinyInt()
        {
        assertEquals(PofConstants.V_INT_1, PofHelper.encodeTinyInt(1));
        assertEquals(PofConstants.V_INT_NEG_1, PofHelper.encodeTinyInt(-1));
        assertEquals(PofConstants.V_INT_13, PofHelper.encodeTinyInt(13));
        }

    @Test
    public void testWriteDate()
            throws IOException
        {
        initPOFWriter();
        PofHelper.writeDate(m_wb.getBufferOutput(), 2006, 8, 10);
        PofHelper.writeDate(m_wb.getBufferOutput(), 2006, 8, 11);

        initPOFReader();
        assertEquals(PofHelper.readRawDate(m_rb.getBufferInput()),
                new RawDate(2006, 8, 11));
        assertEquals(PofHelper.readRawDate(m_rb.getBufferInput()),
                new RawDate(2006, 8, 11));
        }

    @Test
    public void testWriteTime()
            throws IOException
        {
        initPOFWriter();
        PofHelper.writeTime(m_wb.getBufferOutput(), 12, 59, 14, 0, 0, 0, 0);
        PofHelper.writeTime(m_wb.getBufferOutput(), 12, 59, 59, 0, 0, 0, 0);

        initPOFReader();
        assertEquals(PofHelper.readRawTime(m_rb.getBufferInput()),
                new RawTime(12, 59, 59, 0, 0, 0));
        assertEquals(PofHelper.readRawTime(m_rb.getBufferInput()),
                new RawTime(12, 59, 59, 0, 0, 0));
        }

    @Test
    public void testBigInteger()
            throws IOException
        {
        for (int i = -128; i <= -17; ++i)
            {
            BigInteger n = BigInteger.valueOf(i);

            initPOFWriter();
            PofHelper.writeBigInteger(m_wb.getBufferOutput(), n);

            initPOFReader();
            n = PofHelper.readBigInteger(m_rb.getBufferInput());
            assertFalse(String.valueOf(i), n == BigInteger.valueOf(i));
            assertTrue(String.valueOf(i), n.equals(BigInteger.valueOf(i)));
            }

        for (int i = -16; i <= 16; ++i)
            {
            BigInteger n = BigInteger.valueOf(i);

            initPOFWriter();
            PofHelper.writeBigInteger(m_wb.getBufferOutput(), n);

            initPOFReader();
            n = PofHelper.readBigInteger(m_rb.getBufferInput());
            assertTrue(String.valueOf(i), n == BigInteger.valueOf(i));
            }

        for (int i = 17; i <= 128; ++i)
            {
            BigInteger n = BigInteger.valueOf(i);

            initPOFWriter();
            PofHelper.writeBigInteger(m_wb.getBufferOutput(), n);

            initPOFReader();
            n = PofHelper.readBigInteger(m_rb.getBufferInput());
            assertFalse(String.valueOf(i), n == BigInteger.valueOf(i));
            assertTrue(String.valueOf(i), n.equals(BigInteger.valueOf(i)));
            }
        }

    @Test
    public void testBigDecimal()
            throws IOException
        {
        for (int i = -128; i <= -1; ++i)
            {
            BigDecimal dec = BigDecimal.valueOf(i);
            int        cb  = PofHelper.calcDecimalSize(dec);

            initPOFWriter();
            PofHelper.writeBigDecimal(m_wb.getBufferOutput(), dec, cb);

            initPOFReader();
            dec = PofHelper.readBigDecimal(m_rb.getBufferInput(), cb);
            assertFalse(String.valueOf(i), dec == BigDecimal.valueOf(i));
            assertTrue(String.valueOf(i), dec.equals(BigDecimal.valueOf(i)));
            }

        for (int i = 0; i <= 10; ++i)
            {
            BigDecimal dec = BigDecimal.valueOf(i);
            int        cb  = PofHelper.calcDecimalSize(dec);

            initPOFWriter();
            PofHelper.writeBigDecimal(m_wb.getBufferOutput(), dec, cb);

            initPOFReader();
            dec = PofHelper.readBigDecimal(m_rb.getBufferInput(), cb);
            assertTrue(String.valueOf(i), dec == BigDecimal.valueOf(i));
            }

        for (int i = 11; i <= 128; ++i)
            {
            BigDecimal dec = BigDecimal.valueOf(i);
            int        cb  = PofHelper.calcDecimalSize(dec);

            initPOFWriter();
            PofHelper.writeBigDecimal(m_wb.getBufferOutput(), dec, cb);

            initPOFReader();
            dec = PofHelper.readBigDecimal(m_rb.getBufferInput(), cb);
            assertFalse(String.valueOf(i), dec == BigDecimal.valueOf(i));
            assertTrue(String.valueOf(i), dec.equals(BigDecimal.valueOf(i)));
            }
        }
    }

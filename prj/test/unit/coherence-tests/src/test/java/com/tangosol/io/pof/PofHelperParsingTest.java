/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import data.pof.Address;
import data.pof.Person;
import data.pof.PortablePerson;

import org.junit.Test;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;

import static org.junit.Assert.*;


/**
* Unit tests of various PofHelper POF parsing methods.
*
* @author gm 2006.12.21
* @author jh 2006.12.27
*/
public class PofHelperParsingTest
        extends AbstractPofTest
    {
    @Test
    public void testDecodeTinyInt()
        {
        assertEquals(0,  PofHelper.decodeTinyInt(PofConstants.V_INT_0));
        assertEquals(22, PofHelper.decodeTinyInt(PofConstants.V_INT_22));
        assertEquals(-1, PofHelper.decodeTinyInt(PofConstants.V_INT_NEG_1));
        assertEquals(17, PofHelper.decodeTinyInt(PofConstants.V_INT_17));
        }

    @Test
    public void testReadChar()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeChar(0, 'a');
        m_writer.writeChar(0, (char) 0x0080);
        m_writer.writeChar(0, (char) 0x0800);
        m_writer.writeChar(0, (char) 0xff);
        m_writer.writeChar(0, (char) 0xffff);

        initPOFReader();
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_CHAR);
        assertEquals(PofHelper.readChar(m_bi),
                'a');
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_CHAR);
        assertEquals(PofHelper.readChar(m_bi),
                (char) 0x0080);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_CHAR);
        assertEquals(PofHelper.readChar(m_bi),
                (char) 0x0800);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_CHAR);
        assertEquals(PofHelper.readChar(m_bi),
                (char) 0xff);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.V_INT_NEG_1);
        }

    @Test
    public void testReadAsChar()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeChar(0, 'A');
        m_writer.writeChar(0, (char) 0x0080);
        m_writer.writeChar(0, (char) 0x0800);
        m_writer.writeChar(0, (char) 0xff);
        m_writer.writeChar(0, (char) 0xffff);

        initPOFReader();
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_CHAR);
        assertEquals(PofHelper.readAsChar(m_bi, PofConstants.T_OCTET),
                'A');
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_CHAR);
        assertEquals(PofHelper.readAsChar(m_bi, PofConstants.T_CHAR),
                (char) 0x0080);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_CHAR);
        assertEquals(PofHelper.readAsChar(m_bi, PofConstants.T_CHAR),
                (char) 0x0800);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_CHAR);
        assertEquals(PofHelper.readAsChar(m_bi, PofConstants.T_CHAR),
                (char) 0xff);
        assertEquals(PofHelper.readAsChar(m_bi, PofConstants.V_INT_NEG_1),
                (char) 0xffff);
        }

    @Test
    public void testReadAsInt()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeInt(0, 0);
        m_writer.writeInt(0, Integer.MIN_VALUE);
        m_writer.writeInt(0, -1);
        m_writer.writeInt(0, Integer.MAX_VALUE);

        initPOFReader();
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.V_INT_0);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                Integer.MIN_VALUE);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.V_INT_NEG_1);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                Integer.MAX_VALUE);

        initPOFWriter();
        m_writer.writeFloat(0, 1000.123f);
        m_writer.writeDouble(0, 3000.123456);

        initPOFReader();
        assertEquals(m_reader.readInt(0), 1000);
        assertEquals(m_reader.readInt(0), 3000);
        }

    @Test
    public void testReadAsIntException()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeString(0, "invalid value");

        initPOFReader();
        try
            {
            m_reader.readInt(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testReadAsLong()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeLong(0, 0);
        m_writer.writeLong(0, Long.MIN_VALUE);
        m_writer.writeLong(0, -1);
        m_writer.writeLong(0, Long.MAX_VALUE);

        initPOFReader();
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.V_INT_0);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT64);
        assertEquals(PofHelper.readAsFloat(m_bi, PofConstants.T_INT64),
                Long.MIN_VALUE, 0);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.V_INT_NEG_1);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT64);
        assertEquals(PofHelper.readAsFloat(m_bi, PofConstants.T_INT64),
                Long.MAX_VALUE, 0);

        initPOFWriter();
        m_writer.writeLong(0, 1000L);
        m_writer.writeDouble(0, 3000.123456);

        initPOFReader();
        assertEquals(m_reader.readLong(0), 1000, 0.2);
        assertEquals(m_reader.readLong(0), 3000, 0.2);
        }

    @Test
    public void testReadAsFloat()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeFloat(0, 0);
        m_writer.writeFloat(0, Float.MIN_VALUE);
        m_writer.writeFloat(0, -1);
        m_writer.writeFloat(0, Float.MAX_VALUE);

        initPOFReader();
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.V_INT_0);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_FLOAT32);
        assertEquals(PofHelper.readAsFloat(m_bi, PofConstants.T_FLOAT32),
                Float.MIN_VALUE, 0);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.V_INT_NEG_1);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_FLOAT32);
        assertEquals(PofHelper.readAsFloat(m_bi, PofConstants.T_FLOAT32),
                Float.MAX_VALUE, 0);

        initPOFWriter();
        m_writer.writeFloat(0, 1000.123f);
        m_writer.writeDouble(0, 3000.123456);

        initPOFReader();
        assertEquals(m_reader.readFloat(0), 1000, 0.2);
        assertEquals(m_reader.readFloat(0), 3000, 0.2);
        }

    @Test
    public void testReadAsDouble()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeDouble(0, 0);
        m_writer.writeDouble(0, Double.MIN_VALUE);
        m_writer.writeDouble(0, -1);
        m_writer.writeDouble(0, Double.MAX_VALUE);

        initPOFReader();
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.V_INT_0);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_FLOAT64);
        assertEquals(PofHelper.readAsDouble(m_bi, PofConstants.T_FLOAT64),
                Double.MIN_VALUE, 0);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.V_INT_NEG_1);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_FLOAT64);
        assertEquals(PofHelper.readAsDouble(m_bi, PofConstants.T_FLOAT64),
                Double.MAX_VALUE, 0);
        }

    @Test
    public void testReadRawDate()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeRawDate(0, new RawDate(2006, 8, 11));
        m_writer.writeRawDate(0, new RawDate(2006, 8, 12));
        m_writer.writeRawDate(0, new RawDate(2006, 8, 13));

        initPOFReader();
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_DATE);
        assertEquals(PofHelper.readRawDate(m_bi),
                new RawDate(2006, 8, 11));
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_DATE);
        assertEquals(PofHelper.readRawDate(m_bi),
                new RawDate(2006, 8, 12));
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_DATE);
        assertEquals(PofHelper.readRawDate(m_bi),
                new RawDate(2006, 8, 13));
        }

    @Test
    public void testReadRawTime()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeRawTime(0, new RawTime(11, 59, 57, 100, true));
        m_writer.writeRawTime(0, new RawTime(11, 59, 58, 100, true));
        m_writer.writeRawTime(0, new RawTime(11, 59, 59, 100, true));

        initPOFReader();
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_TIME);
        assertEquals(PofHelper.readRawTime(m_bi),
                new RawTime(11, 59, 57, 100, true));
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_TIME);
        assertEquals(PofHelper.readRawTime(m_bi),
                new RawTime(11, 59, 58, 100, true));
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_TIME);
        assertEquals(PofHelper.readRawTime(m_bi),
                new RawTime(11, 59, 59, 100, true));
        }

    @Test
    public void testReadRawDateTime()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeRawDateTime(0, new RawDateTime(
                new RawDate(2006, 8, 11),
                new RawTime(11, 59, 57, 100, true)));
        m_writer.writeRawDateTime(0, new RawDateTime(
                new RawDate(2006, 8, 12),
                new RawTime(11, 59, 58, 100, true)));
        m_writer.writeRawDateTime(0, new RawDateTime(
                new RawDate(2006, 8, 13),
                new RawTime(11, 59, 59, 100, true)));

        initPOFReader();
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_DATETIME);
        assertEquals(PofHelper.readRawDate(m_bi),
                new RawDate(2006, 8, 11));
        assertEquals(PofHelper.readRawTime(m_bi),
                new RawTime(11, 59, 57, 100, true));
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_DATETIME);
        assertEquals(PofHelper.readRawDate(m_bi),
                new RawDate(2006, 8, 12));
        assertEquals(PofHelper.readRawTime(m_bi),
                new RawTime(11, 59, 58, 100, true));
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_DATETIME);
        assertEquals(PofHelper.readRawDate(m_bi),
                new RawDate(2006, 8, 13));
        assertEquals(PofHelper.readRawTime(m_bi),
                new RawTime(11, 59, 59, 100, true));
        }

    @Test
    public void testSkipValue()
            throws IOException
        {
        Map map = new HashMap();
        map.put(1, "t");
        map.put(2, "g");

        initPOFWriter();
        m_writer.writeRawDate(0, new RawDate(2006, 8, 11));
        m_writer.writeRawDateTime(0, new RawDateTime(
                new RawDate(2006, 8, 11),
                new RawTime(11, 59, 57, 100, true)));
        m_writer.writeCharArray(0, new char[]{'g', 't', 's'});
        m_writer.writeObjectArray(0,
                new Object[]{'g', "Gor", 55});
        m_writer.writeObjectArray(0,
                new Object[]{new int[]{1, 2}, new int[]{3, 2, 4}});
        m_writer.writeObject(0, map);
        m_writer.writeByte(0, (byte) 0x00F0);
        m_writer.writeInt(0, 300);

        initPOFReader();
        PofHelper.skipValue(m_bi);
        PofHelper.skipValue(m_bi);
        PofHelper.skipValue(m_bi);
        PofHelper.skipValue(m_bi);
        PofHelper.skipValue(m_bi);
        PofHelper.skipValue(m_bi);
        PofHelper.skipValue(m_bi);

        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                300);
        }

    @Test
    public void testSkipUniformValue()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeInt(0, 100);
        m_writer.writeInt(0, 200);
        m_writer.writeInt(0, 300);

        initPOFReader();
        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                300);

        initPOFWriter();
        RawYearMonthInterval ymi = new RawYearMonthInterval(2, 10);
        m_writer.writeRawYearMonthInterval(0, ymi);
        m_writer.writeInt(0, 101);
        m_writer.writeRawTimeInterval(0,
                new RawTimeInterval(4, 52, 10, 100));
        m_writer.writeInt(0, 201);
        m_writer.writeRawDayTimeInterval(0,
                new RawDayTimeInterval(11, 12, 13, 14, 50));
        m_writer.writeInt(0, 301);
        m_writer.writeFloat(0, 120.34f);
        m_writer.writeInt(0, 401);
        m_writer.writeDouble(0, 1222.22);
        m_writer.writeInt(0, 501);
        m_writer.writeChar(0, 'A');
        m_writer.writeInt(0, 601);

        initPOFReader();
        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        PofHelper.skipUniformValue(m_bi, PofConstants.T_YEAR_MONTH_INTERVAL);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                101);

        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        PofHelper.skipUniformValue(m_bi, PofConstants.T_TIME_INTERVAL);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                201);

        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        PofHelper.skipUniformValue(m_bi, PofConstants.T_DAY_TIME_INTERVAL);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                301);

        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        PofHelper.skipUniformValue(m_bi, PofConstants.T_FLOAT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                401);

        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        PofHelper.skipUniformValue(m_bi, PofConstants.T_FLOAT64);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                501);

        PofHelper.skipUniformValue(m_bi, PofConstants.T_INT32);
        PofHelper.skipUniformValue(m_bi, PofConstants.T_CHAR);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                601);
        }

    @Test
    public void testSkipPackedInts()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeInt(0, 100);
        m_writer.writeInt(0, 200);
        m_writer.writeInt(0, 300);

        initPOFReader();
        PofHelper.skipPackedInts(m_bi, 4);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                300);
        }

    @Test
    public void testSkipUserType()
            throws IOException
        {
        initPOFWriter();
        m_ctx.registerUserType(1000, Address.class, new PortableObjectSerializer(1000));
        m_ctx.registerUserType(1001, PortablePerson.class, new PortableObjectSerializer(1001));

        Address address = new Address("48 Grove St.", "Somerville", "MA", "02144");
        Person  person  = new PortablePerson("Jason", new Date());
        person.setAddress(address);

        m_writer.writeObject(0, person);
        m_writer.writeInt(0, 777);

        initPOFReader();
        m_ctx.registerUserType(1000, Address.class, new PortableObjectSerializer(1000));
        m_ctx.registerUserType(1001, Person.class, new PortableObjectSerializer(1001));

        PofHelper.skipValue(m_bi);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                PofConstants.T_INT32);
        assertEquals(PofHelper.readAsInt(m_bi, PofConstants.T_INT32),
                777);
        }
    }
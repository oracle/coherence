/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ExternalizableLiteSerializer;
import com.tangosol.io.ExternalizableType;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofInputStream;
import com.tangosol.io.pof.PofOutputStream;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import data.Person;

import java.lang.reflect.InvocationTargetException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.Method;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import com.oracle.coherence.testing.CheckJDK;

import static org.junit.Assert.*;

/**
* Unit tests for ExternalizableHelper.
*
* @author cp  2006.01.10
*/
public class ExternalizableHelperTest extends ExternalizableHelper
    {
    // ----- unit tests -----------------------------------------------------

    /**
     * Test UTF-8 conversion.
     */
    @Test
    public void testUtfConversion() throws IOException
        {
        assertUtfConversion("Aleksandar");
        assertUtfConversion("Александар");
        assertUtfConversion("Aleksandar$ñ£");
        assertUtfConversion("ⅯⅭⅯⅬⅩⅩⅠⅤ");
        assertUtfConversion(toBytes(new int[] {0xf0938080, 0xf09f8ebf, 0xf09f8f80, 0xf09f8e89, 0xf09f9294}));

        // make sure we can still handle our proprietary (broken) encoding
        String sUtf = new String(toBytes(new int[] {0xf0938080, 0xf09f8ebf, 0xf09f8f80, 0xf09f8e89, 0xf09f9294}), StandardCharsets.UTF_8);
        Binary bin = ExternalizableHelper.toBinary(sUtf);
        assertEquals(22, bin.length());
        assertEquals(sUtf, ExternalizableHelper.fromBinary(bin));
        }

    private void assertUtfConversion(String s) throws IOException
        {
        assertUtfConversion(s.getBytes(StandardCharsets.UTF_8));
        }

    private void assertUtfConversion(byte[] abUtf8) throws IOException
        {
        String sExpected = new String(abUtf8, StandardCharsets.UTF_8);
        String sActual   = ExternalizableHelper.convertUTF(abUtf8, 0, abUtf8.length, new char[sExpected.length()]);
        System.out.printf("\n%12s = %-12s : utf8 bytes = %d; string length = %d", sExpected, sActual, abUtf8.length, sActual.length());
        assertEquals(sExpected, sActual);
        }

    /**
    * Test POF serialization/deserialization of a java.util.Map.
    */
    @Test
    public void testMapPof() throws IOException
        {
        PofContext ctx = new SimplePofContext();
        WriteBuffer wb = new ByteArrayWriteBuffer(0);

        Map mapTest;
        Map mapDest;

        // test null Map
        wb.clear();
        mapTest = null;
        mapDest = new HashMap();
        writeMap(createPofDataOutput(ctx, wb), mapTest);
        readMap(createPofDataInput(ctx, wb), mapDest, 0, null);
        assertTrue(mapDest.isEmpty());

        // test empty Map
        wb.clear();
        mapTest = new HashMap();
        mapDest = new HashMap();
        writeMap(createPofDataOutput(ctx, wb), mapTest);
        readMap(createPofDataInput(ctx, wb), mapDest, 0, null);
        assertTrue(equals(mapTest, mapDest));

        // test single entry Map
        wb.clear();
        mapTest = new HashMap();
        mapDest = new HashMap();

        mapTest.put("key", "value");

        writeMap(createPofDataOutput(ctx, wb), mapTest);
        readMap(createPofDataInput(ctx, wb), mapDest, 0, null);
        assertTrue(equals(mapTest, mapDest));
        }

    /**
    * Test POF serialization/deserialization of a java.lang.String array.
    */
    @Test
    public void testStringArrayPof() throws IOException
        {
        PofContext ctx = new SimplePofContext();
        WriteBuffer wb = new ByteArrayWriteBuffer(0);

        String[] as;

        // test null array
        wb.clear();
        as = null;
        writeStringArray(createPofDataOutput(ctx, wb), as);
        assertTrue(equals(readStringArray(createPofDataInput(ctx, wb)), null));

        // test empty array
        wb.clear();
        as = new String[0];
        writeStringArray(createPofDataOutput(ctx, wb), as);
        assertTrue(readStringArray(createPofDataInput(ctx, wb)).length == 0);

        // test single element array
        wb.clear();
        as = new String[] { "test" };
        writeStringArray(createPofDataOutput(ctx, wb), as);

        String[] asTemp = readStringArray(createPofDataInput(ctx, wb));
        assertTrue(asTemp.length == 1 && equals(asTemp[0], as[0]));
        }

    /**
    * Test POF serialization/deserialization of a java.sql.Date.
    */
    @Test
    public void testDatePof() throws IOException
        {
        PofContext ctx = new SimplePofContext();
        WriteBuffer wb = new ByteArrayWriteBuffer(0);

        Date date;

        // test null Date
        wb.clear();
        date = null;
        writeDate(createPofDataOutput(ctx, wb), date);
        assertTrue(equals(readDate(createPofDataInput(ctx, wb)), date));

        // test non-null Date
        wb.clear();
        date = new Date(75, 11, 31);
        writeDate(createPofDataOutput(ctx, wb), date);
        assertEquals(date, readDate(createPofDataInput(ctx, wb)));
        }

    /**
    * Test POF serialization/deserialization of a java.sql.Time.
    */
    @Test
    public void testTimePof() throws IOException
        {
        PofContext ctx = new SimplePofContext();
        WriteBuffer wb = new ByteArrayWriteBuffer(0);

        Time time;

        // test null Time
        wb.clear();
        time = null;
        writeTime(createPofDataOutput(ctx, wb), time);
        assertTrue(equals(readTime(createPofDataInput(ctx, wb)), time));

        // test non-null Time
        wb.clear();
        time = new Time(7, 7, 7);
        writeTime(createPofDataOutput(ctx, wb), time);
        assertTrue(equals(readTime(createPofDataInput(ctx, wb)), time));
        }

    /**
    * Test POF serialization/deserialization of a java.sql.Timestamp.
    */
    @Test
    public void testTimestampPof() throws IOException
        {
        PofContext ctx = new SimplePofContext();
        WriteBuffer wb = new ByteArrayWriteBuffer(0);

        Timestamp ts;

        // test null Timestamp
        wb.clear();
        ts = null;
        writeTimestamp(createPofDataOutput(ctx, wb), ts);
        assertTrue(equals(readDate(createPofDataInput(ctx, wb)), ts));

        // test non-null Timestamp
        wb.clear();
        ts = new Timestamp(75, 11, 31, 7, 7, 7, 0);
        writeTimestamp(createPofDataOutput(ctx, wb), ts);
        assertTrue(equals(readTimestamp(createPofDataInput(ctx, wb)), ts));
        }

    /**
    * Test POF serialization/deserialization of an XmlSerialzable.
    */
    @Test
    public void testXmlSerializablePof() throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        WriteBuffer wb = new ByteArrayWriteBuffer(0);

        ctx.registerUserType(0, Person.class, new PortableObjectSerializer(0));

        Person person;

        // test null Person
        wb.clear();
        person = null;
        writeXmlSerializable(createPofDataOutput(ctx, wb), person);
        assertTrue(equals(readXmlSerializable(createPofDataInput(ctx, wb)),
            person));

        // test non-null Person
        wb.clear();
        person = new Person();
        person.setId("111-11-1111");
        person.setFirstName("Jason");
        person.setLastName("Howes");
        writeXmlSerializable(createPofDataOutput(ctx, wb), person);
        assertTrue(equals(readXmlSerializable(createPofDataInput(ctx, wb)),
                person));
        }

    /**
    * Test {@link #decorate(com.tangosol.util.Binary, com.tangosol.util.Binary[])}
    * and {@link #getDecorations(com.tangosol.io.ReadBuffer)}
    * and {@link #decorate(com.tangosol.util.Binary, int, com.tangosol.util.Binary)}
    * and {@link #getDecoration(com.tangosol.util.Binary, int)}
    * and {@link #undecorate(com.tangosol.util.Binary, int)}
    * and {@link #isDecorated(com.tangosol.io.ReadBuffer)}
    * and {@link #getUndecorated(com.tangosol.util.Binary)}.
    */
    @Test
    public void testDecoration()
        {
        assertFalse(isDecorated(null));
        assertFalse(isDecorated(VALUE));

        Binary bin = decorate(VALUE, new Binary[0]);
        assertFalse(isDecorated(bin));
        assertIdentical(VALUE, bin);
        bin = getUndecorated(bin);
        assertFalse(isDecorated(bin));
        assertIdentical(VALUE, bin);

        bin = decorate(null, new Binary[] { VALUE });
        assertFalse(isDecorated(bin));
        assertIdentical(VALUE, bin);
        bin = getUndecorated(bin);
        assertFalse(isDecorated(bin));
        assertIdentical(VALUE, bin);

        bin = decorate(DIFF, new Binary[] { VALUE });
        assertFalse(isDecorated(bin));
        assertIdentical(VALUE, bin);
        bin = getUndecorated(bin);
        assertFalse(isDecorated(bin));
        assertIdentical(VALUE, bin);

        assertFalse(isDecorated(null, 2));
        assertFalse(isDecorated(VALUE, 6));

        for (int i = 1; i < 12; ++i)
            {
            bin = decorate(DIFF, i, VALUE);
            assertTrue(isDecorated(bin, i));
            assertFalse(isDecorated(bin, i+1));
            }

        for (int i = 1; i < 10; ++i)
            {
            bin = decorate(null, i, VALUE);
            assertTrue(isDecorated(bin));
            assertFalse(VALUE.equals(bin));
            assertNull(getUndecorated(bin));
            assertIdentical(VALUE, getDecoration(bin, i));
            }

        for (int i = 1; i < 10; ++i)
            {
            for (int cSize = 10; cSize <= 1000000; cSize *= 10)
                {
                Binary binOrig = toBinary(new byte[cSize]);
                bin = decorate(binOrig, i, VALUE);
                assertTrue(isDecorated(bin));
                assertIdentical(VALUE, getDecoration(bin, i));
                bin = getUndecorated(bin);
                assertIdentical(bin, binOrig);
                }
            }

        bin = decorate(VALUE, DIGITS);
        assertTrue(isDecorated(bin));
        ReadBuffer[] abuf = getDecorations(bin);
        assertTrue(abuf.length == 10);
        for (int i = 0; i < 10; ++i)
            {
            assertIdentical(DIGITS[i], abuf[i].toBinary());
            }
        assertIdentical(DIGITS[0], getUndecorated(bin));

        bin = decorate(bin, 0, DIFF);
        assertTrue(isDecorated(bin));
        bin = decorate(bin, 3, DIFF);
        assertTrue(isDecorated(bin));
        bin = decorate(bin, 7, DIFF);
        assertTrue(isDecorated(bin));
        bin = decorate(bin, 9, DIFF);
        assertTrue(isDecorated(bin));
        abuf = getDecorations(bin);
        assertTrue(abuf.length == 10);
        for (int i = 0; i < 10; ++i)
            {
            switch (i)
                {
                case 0:
                case 3:
                case 7:
                case 9:
                    assertIdentical(DIFF, abuf[i]);
                break;
                default:
                    assertIdentical(DIGITS[i], abuf[i]);
                break;
                }

            assertIdentical(abuf[i], getDecoration(bin, i));
            }

        bin = undecorate(bin, 0);
        bin = undecorate(bin, 3);
        bin = undecorate(bin, 7);
        bin = undecorate(bin, 0);
        abuf = getDecorations(bin);
        assertTrue(abuf.length == 10);
        for (int i = 0; i < 9; ++i)
            {
            switch (i)
                {
                case 0:
                case 3:
                case 7:
                case 9:
                break;

                default:
                    assertIdentical(DIGITS[i], abuf[i]);
                break;
                }

            assertIdentical(abuf[i], getDecoration(bin, i));
            }

        bin = decorate(bin, 0, DIFF);
        bin = decorate(bin, 3, DIFF);
        bin = decorate(bin, 7, DIFF);
        bin = decorate(bin, 9, DIFF);
        abuf = getDecorations(bin);
        assertTrue(abuf.length == 10);
        for (int i = 0; i < 10; ++i)
            {
            switch (i)
                {
                case 0:
                case 3:
                case 7:
                case 9:
                    assertIdentical(DIFF, abuf[i]);
                break;
                default:
                    assertIdentical(DIGITS[i], abuf[i]);
                break;
                }

            assertIdentical(abuf[i], getDecoration(bin, i));
            }

        bin = decorate(VALUE, DECO_EXPIRY, DIGITS[DECO_EXPIRY]);
        if (!equals(fromBinary(bin), fromBinary(VALUE)))
            {
            out("binaries differ:" + "\n  binary 1:\n"
                    + indentString(toHexDump(bin.toByteArray(), 16), "    ")
                    + "\n  binary 2:\n"
                    + indentString(toHexDump(VALUE.toByteArray(), 16), "    "));
            }
        }

    @Test
    public void testRandom()
        {
        for (int i = 0; i < 10000; i++)
            {
            Binary binary = decorateBinaryRandomly(VALUE);
            compare(binary);
            }
        }

    @Test
    public void testEmpty()
        {
        for (int i = 1; i < 1000; i++)
            {
            Binary bin = ExternalizableHelperTest.decorate(VALUE, 0,
                randomizedBinary(0, 0));
            bin = ExternalizableHelperTest.decorate(bin, 3,
                    randomizedBinary(0, 0));
            bin = ExternalizableHelperTest.decorate(bin, 8,
                    randomizedBinary(512, 1024));
            bin = ExternalizableHelperTest.decorate(bin, 15,
                    randomizedBinary(512, 1024));
            bin = ExternalizableHelperTest.decorate(bin, 18,
                    randomizedBinary(0, 0));
            bin = ExternalizableHelperTest.decorate(bin, 19,
                    randomizedBinary(0, 0));
            bin = ExternalizableHelperTest.decorate(bin, 8,
                    randomizedBinary(512, 1024));
            bin = ExternalizableHelperTest.decorate(bin, 25,
                    randomizedBinary(512, 1024));
            }
        }

    @Test
    public void testNull()
        {
        for (int i = 1; i < 1000; i++)
            {
            Binary bin = ExternalizableHelperTest.decorate(null, 0,
                    randomizedBinary(0, 0));
            bin = ExternalizableHelperTest.decorate(bin, random(Long.SIZE),
                    randomizedBinary(0, 0));
            bin = ExternalizableHelperTest.decorate(bin, random(Long.SIZE),
                    randomizedBinary(512, 1024));
            bin = ExternalizableHelperTest.decorate(bin, random(Long.SIZE),
                    randomizedBinary(512, 1024));
            bin = ExternalizableHelperTest.decorate(bin, random(Long.SIZE),
                    randomizedBinary(0, 0));
            bin = ExternalizableHelperTest.decorate(bin, random(Long.SIZE),
                    randomizedBinary(0, 0));
            bin = ExternalizableHelperTest.decorate(bin, random(Long.SIZE),
                    randomizedBinary(512, 1024));
            bin = ExternalizableHelperTest.decorate(bin, random(Long.SIZE),
                    randomizedBinary(512, 1024));
            }
        }

    @Test
    public void testOverwriteValue()
        {
        for (int i = 0; i < 10000; i++)
            {
            Binary val = randomizedBinary(0, 255);
            Binary bin = decorate(VALUE, 0, val);
            Binary diff = getDecoration(bin, 0);
            assertIdentical(val, diff);
            }
        }

    @Test
    public void testRandomCorrectness()
        {
        for (int i = 1; i < 10000; i++)
            {
            Binary[] binArray = generateRandomBinaryArray();
            Binary bin = apply(binArray, VALUE);
            compare(binArray, bin);
            }
        }

    @Test
    public void testInflateAndDeflate()
        {
        Binary bin = VALUE;
        Binary val = randomizedBinary(10, 25);
        bin = decorate(bin, 8, val);
        assertIdentical(val, getDecoration(bin, 8));
        bin = decorate(bin, 3, val);
        assertIdentical(val, getDecoration(bin, 3));
        assertIdentical(val, getDecoration(bin, 8));

        bin = decorate(bin, 0, val);
        assertIdentical(val, getDecoration(bin, 0));
        assertIdentical(val, getDecoration(bin, 3));
        assertIdentical(val, getDecoration(bin, 8));

        bin = decorate(bin, 5, val);
        assertIdentical(val, getDecoration(bin, 5));
        assertIdentical(val, getDecoration(bin, 3));
        assertIdentical(val, getDecoration(bin, 8));
        assertIdentical(val, getDecoration(bin, 0));

        bin = decorate(bin, 8, null);
        assertIdentical(val, getDecoration(bin, 5));
        assertIdentical(val, getDecoration(bin, 3));
        assertNull(getDecoration(bin, 8));
        assertIdentical(val, getDecoration(bin, 0));

        ReadBuffer[] array = getDecorations(bin);
        }

    @Test
    public void testRandomApplyCorrectness()
        {
        for (int i = 1; i < 10000; i++)
            {
            Binary[] binArray = generateRandomBinaryArray();
            Binary bin = applyRandomly(binArray, VALUE);
            compare(binArray, bin);
            }
        }

    /**
     * This method will likely fail if the algorithm is changed, since changing
     * the algorithm will likely change the buffer size returned.
     */
    @Test
    public void testStats()
        {
        new ExternalizableStatsHelper().runTest();
        }

    /**
     * Annotate this method to generate output which reports
     * execution speed for the buffer serialization algorithm.
     */
    @Test
    public void testStatsPerformance()
        {
        new ExternalizableStatsHelper().runPerformanceTest();
        }

    /**
     * Test ObjectInputStream without an ObjectInputFilter.
     */
    @Test
    public void testObjectInputStreamWithoutFilter()
        {
        testObjectInputFilter(false, false, 500000);
        }

    /**
     * Test a filter that disallow testing object to be deserialized.
     */
    @Test
    public void testObjectInputStreamWithShouldFailFilter()
        {
        testObjectInputFilter(true, true, 10);
        }

    /**
     * Test an ObjectInputFilter that allow testing object to be deserialized.
     */
    @Test
    public void testObjectInputStreamWithShouldPassFilter()
        {
        testObjectInputFilter(true, false, 500000);
        }

    /**
     * Test Object Deserialisation Filters.
     */
    public void testObjectInputFilter(boolean fFilter, boolean fFail, int nCount)
        {
        Map<Person.Key, Person> map = new HashMap();
        Person.fillRandom(map, nCount);

        Set<Person> setPersons = new HashSet<>(map.values());
        Set<Person> setRead    = new HashSet<>(map.size());
        Exception   exception  = null;
        String      sFilter    = null;
        long        ldtStart   = -1;

        try
            {
            ByteArrayOutputStream outRaw = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(outRaw);

            ExternalizableHelper.writeCollection(oos, setPersons);
            oos.flush();

            byte[] ab = outRaw.toByteArray();

            ByteArrayInputStream inRaw = new ByteArrayInputStream(ab);
            ObjectInputStream ios = new ObjectInputStream(inRaw);

            sFilter = fFilter
                      ? fFail ? "!data.Person" : "data.Person"
                      : null;
            if (!setObjectInputStreamFilter(ios, sFilter))
                {
                return;
                }
            ldtStart = start();
            ExternalizableHelper.readCollection(ios, setRead, null);
            }
        catch (Exception e)
            {
            exception = e;
            }
        stop(ldtStart, "ObjectInputStream: filter: " + sFilter + " count=" + nCount);

        if (!fFilter || !fFail)
            {
            assertEquals("expected deserialized collection to be equal to serialized collection for ObjectInputFilter=" + sFilter,
                         setPersons.size(), setRead.size());
            }
        else
            {
            assertTrue("expected an InvalidClassException for ObjectInputFilter " + sFilter,
                       exception != null && exception.getCause() instanceof InvalidClassException);
            }
        }

    @Test
    public void testConfigGetSerialFilterFactory()
        {
        CheckJDK.assumeJDKVersionEqualOrGreater(17);
        assertNotNull(ExternalizableHelper.getConfigSerialFilterFactory());
        }

    /**
     * Test ObjectInputStream without an ObjectInputFilter.
     */
    @Test
    public void testExternalizableLiteObjectInputStreamWithoutFilter()
        {
        testExternalizableLiteObjectInputFilter(false, false, 500000);
        }

    /**
     * Test a filter that disallow testing object to be deserialized.
     */
    @Test
    public void testExternalizableLiteObjectInputStreamWithShouldFailFilter()
        {
        testExternalizableLiteObjectInputFilter(true, true, 10);
        }

    /**
     * Test an ObjectInputFilter that allow testing object to be deserialized.
     */
    @Test
    public void testExternalizableLiteObjectInputStreamWithShouldPassFilter()
        {
        testExternalizableLiteObjectInputFilter(true, false, 500000 );
        }

    @Test
    public void testExternalizableType() throws IOException
        {
        Point point = new Point(5, 10);
        ByteArrayWriteBuffer wb = new ByteArrayWriteBuffer(0);

        ExternalizableHelper.writeObject(wb.getBufferOutput(), point);

        ByteArrayReadBuffer inRaw = new ByteArrayReadBuffer(wb.toByteArray());
        ReadBuffer.BufferInput in = inRaw.getBufferInput();

        assertEquals(point, ExternalizableHelper.readObject(in, null));
        }

    /**
     * Test deserialization filter on {@link ReadBuffer.BufferInput}.
     */
    public void testExternalizableLiteObjectInputFilter(boolean fFilter, boolean fFail, int nCount)
        {
        Map<Person.Key, Person> map = new HashMap();
        Person.fillRandom(map, nCount);

        Set<Person> setPersons = new HashSet<>(map.values());
        Set<Person> setRead    = new HashSet<>(map.size());
        Exception   exception = null;
        String      sFilter   = null;
        long        ldtStart  = -1;

        try
            {
            ByteArrayWriteBuffer wb = new ByteArrayWriteBuffer(0);
            ExternalizableHelper.writeCollection(wb.getBufferOutput(), setPersons);

            ByteArrayReadBuffer    inRaw = new ByteArrayReadBuffer(wb.toByteArray());
            ReadBuffer.BufferInput in    = inRaw.getBufferInput();
            if (fFilter)
                {
                sFilter = fFilter
                              ? fFail ? "!data.Person" : "data.Person"
                              : null;
                in.setObjectInputFilter(createObjectInputFilter(sFilter));
                }
            try
                {
                ldtStart = start();
                ExternalizableHelper.readCollection(in, setRead, null);
                }
            catch (EOFException eof)
                {
                }
            }
        catch (Exception e)
            {
            exception = e;
            }
        stop(ldtStart, "ExternalizableLite BufferInput: filter: " + sFilter + " count=" + nCount);
        if (!fFilter || !fFail)
            {
            assertEquals("expected deserialized collection to be equal to serialized collection for ObjectInputFilter=" + sFilter,
                         setPersons.size(), setRead.size());
            }
        else
            {
            assertTrue("expected an InvalidClassException for ObjectInputFilter " + sFilter + " Exception=" + exception,
                       exception != null && exception.getCause() instanceof InvalidClassException);
            }
        }

    /**
     * Set filter for ObjectInputStream.
     *
     * @param sFilter  filter pattern as used by JEP-290
     *
     * @return ture if ObjectInputFilter is supported
     */
    public boolean setObjectInputStreamFilter(ObjectInputStream ois, String sFilter)
        {
        try
            {
            Class<?> clzFilter          = null;
            String   sSetFilterMethod   = null;
            Method   methodSetFilter    = null;

            if ((clzFilter = getClass("java.io.ObjectInputFilter")) != null)
                {
                sSetFilterMethod = "setObjectInputFilter";
                }
            else if ((clzFilter = getClass("sun.misc.ObjectInputFilter")) != null)
                {
                sSetFilterMethod = "setInternalObjectInputFilter";
                }

            if (sSetFilterMethod != null)
                {
                Object filter = sFilter != null  ? createObjectInputFilter(sFilter) : null;

                if (filter != null)
                    {
                    Class clzObjectInputStream = ObjectInputStream.class;

                    methodSetFilter = clzObjectInputStream.getDeclaredMethod(sSetFilterMethod, clzFilter);
                    methodSetFilter.setAccessible(true);
                    methodSetFilter.invoke(ois, filter);
                    out("registered ObjectOutputStream filter: " + ExternalizableHelper.getObjectInputFilter(ois));
                    }

                return true;
                }
            }
        catch (Exception e)
            {
            out(e);
            }

        return false;
        }

    // ----- Inner class: ExternalizableStatsHelper ------------------------------

    public class ExternalizableStatsHelper extends ExternalizableHelper.Stats
        {
        public void runTest()
            {
            Stats stats = new Stats();
            stats.update(1000);
            stats.update(2000);
            stats.update(3000);
            stats.update(4000);
            stats.update(5000);
            WriteBuffer buf = stats.instantiateBuffer(true);
            assertTrue(buf.getCapacity() == 3375);

            stats.update(1024*1024*4);
            buf = stats.instantiateBuffer(true);
            assertTrue(buf.getCapacity() == 1024*1024); // Stats.MAX_ALLOC

            stats.update(1000);
            buf = stats.instantiateBuffer(true);
            assertTrue(buf.getCapacity() == (1000+8));
            }

        /**
         * This method measures the execution time of specific use cases. It
         * can be used to evaluate changes to the underlying algorithm.  The
         * test gathers the average speed of serializing a specific number
         * of buffer requests, which is consistent across all use cases,
         * so the performance can be compared.
         *
         * The use cases:
         *   a) smooth: buffer sizes are all about the same
         *   b) large then small:  a large buffer, followed by small buffers
         *   c) large/small:  large, small, large, small, etc
         *   d) many large, then small: many large buffers, then a small buffer
         *   e) distributed: a series distributed over a range with a consistent
         *      average size
         */
        public void runPerformanceTest()
            {
            System.out.println("ExternalizableHelperTest.runPerformanceTest(): ");
            int aSmooth[] = new int[] {1000, 1500, 1700, 2000, 2500,
                                       2750, 1200, 2100, 3000, 1100,
                                       1800, 2100, 1500, 2000, 1800,
                                       2600, 1300, 1650, 1400, 2800,
                                       2300, 2000, 1900, 1000, 2000};

            int aOneLargeThenSmall[] = new int[] {4000000, 1500, 1700, 2000, 2500,
                                                  2750,    1200, 2100, 3000, 1100,
                                                  1800,    2100, 1500, 2000, 1800,
                                                  2600,    1300, 1650, 1400, 2800,
                                                  2300,    2000, 1900, 1000, 2000};

            int aAlterLargeSmall[] = new int[]
                                              {4000000, 1500,    4500000, 2000,     4000000,
                                               2750,    4500000, 2100,    4000000,  1100,
                                               4500000, 2100,    4000000, 2000,     4500000,
                                               2600,    4000000, 1650,    4500000,  2800,
                                               4500000, 2000,    4000000, 1000,     4500000};

            System.out.println("    smooth average (nanosecs):                "+doTest(aSmooth));
            System.out.println("    one-large-then-small average (nanosecs):  "+doTest(aOneLargeThenSmall));
            System.out.println("    alternate-large-small average (nanosecs): "+doTest(aAlterLargeSmall));
            }
        }

    // ----- helper methods -------------------------------------------------

    private static byte[] toBytes(int[] ai)
        {
        ByteBuffer buf = ByteBuffer.allocate(4 * ai.length);
        for (int n : ai)
            {
            buf.putInt(n);
            }
        return buf.array();
        }

    private long doTest(int[] aData)
        {
        long nAccum = 0;
        long nStart = 0;
        long nEnd   = 0;

        Stats stats = new Stats();

        for (int i = 0; i < aData.length; i++)
            {
            nStart = System.nanoTime();
            stats.update(aData[i]);
            stats.instantiateBuffer(true);
            nEnd = System.nanoTime();
            nAccum = nAccum + (nEnd - nStart);
            }
            return (nAccum / aData.length);
        }

    private void compare(Binary[] expected, Binary actual)
        {
        if (expected.length == 0)
            {
            assertIdentical(VALUE, actual);
            }
        else if (expected.length == 1)
            {
            assertIdentical(expected[0], actual);
            }
        else
            {
            ReadBuffer[] abufDeco = ExternalizableHelper.getDecorations(actual);
            if (expected.length != abufDeco.length)
                {
                assertEquals(expected.length, abufDeco.length);
                }

            for (int i = 0; i < abufDeco.length; i++)
                {
                assertIdentical(expected[i], abufDeco[i]);
                }
            }
        }

    private void compare(Binary actual)
        {
        ReadBuffer[] abufDeco = ExternalizableHelper.getDecorations(actual);

        for (int i = 0; i < abufDeco.length; i++)
            {
            assertIdentical(ExternalizableHelper.getDecoration(actual, i),
                    abufDeco[i]);
            }
        }

    private int random(int i)
        {
        Random rand = getRandomizer();
        return rand.nextInt(i);
        }

    private boolean random()
        {
        Random rand = getRandomizer();
        return rand.nextBoolean();
        }

    private Random getRandomizer()
        {
        if (m_rand == null)
            {
            m_rand = new Random();
            }

        return m_rand;
        }

    Random m_rand;

    private Binary decorateBinaryRandomly(Binary binary)
        {
        int len = random(Long.SIZE);
        for (int i = 0; i < len; i++)
            {
            if (random())
                {
                binary = ExternalizableHelper.decorate(binary, i,
                        randomizedBinary(512, 2048));
                }
            }
        return binary;
        }

    private Binary[] generateRandomBinaryArray()
        {
        int len = random(Long.SIZE);
        Binary[] binaryArray = new Binary[len];
        for (int i = 0; i < len; i++)
            {
            if (random() || i == len - 1)
                {
                binaryArray[i] = randomizedBinary(random(512), random(2048));
                }
            }
        return binaryArray;
        }

    /**
    *
    * @param binArray
    * @param value
    * @return
    */
    private Binary apply(Binary[] binArray, Binary value)
        {
        for (int i = 0; i < binArray.length; i++)
            {
            value = decorate(value, i, binArray[i]);
            }
        return value;
        }

    /**
    *
    * @param binArray
    * @param value
    * @return
    */
    private Binary applyRandomly(Binary[] binArray, Binary value)
        {
        List<Integer> ids = new ArrayList<Integer>(binArray.length);
        for (int i = 0; i < binArray.length; i++)
            {
            ids.add(i);
            }
        java.util.Collections.shuffle(ids);

        for (int id : ids)
            {
            value = decorate(value, id, binArray[id]);
            }
        return value;
        }

    /**
    *
    * @return
    */
    private Binary randomizedBinary(int min, int max)
        {
        min = min == 0 ? 0 : random(min);
        max = max == 0 ? 0 : random(max);

        String string = Base.getRandomString(min, max + min, true);
        return toBinary(string);
        }

    /**
    * Compare two binaries for equality.
    *
    * @param buf1  the first binary
    * @param buf2  the second binary
    *
    * @throws RuntimeException  if they are different
    */
    public static void assertIdentical(ReadBuffer buf1, ReadBuffer buf2)
        {
        if (!equals(buf1, buf2))
            {
            fail("binaries differ:"
                    + "\n  binary 1:\n"
                    + indentString(
                            buf1 == null ? "null" : toHexDump(
                                    buf1.toByteArray(), 16), "    ")
                    + "\n  binary 2:\n"
                    + indentString(
                            buf2 == null ? "null" : toHexDump(
                                    buf2.toByteArray(), 16), "    "));
            }
        }

    /**
    * Helper for displaying binary data, including decorated binaries.
    *
    * @param bin  a Binary object
    */
    public static void showBinary(Binary bin)
        {
        if (bin == null)
            {
            out("binary=null");
            }
        else
            {
            out(toHexDump(bin.toByteArray(), 32));
            if (isDecorated(bin))
                {
                ReadBuffer[] abuf = getDecorations(bin);
                int cbuf = abuf.length;
                out("decorated Binary[" + cbuf + "]");
                for (int i = 0; i < cbuf; ++i)
                    {
                    if (abuf[i] != null)
                        {
                        out("[" + i + "]");
                        showBinary(abuf[i].toBinary());
                        }
                    }
                }
            }
        }

    /**
    * Create a POF DataOutput implementation for the given PofContext and
    * WriteBuffer.
    *
    * @param ctx  the PofContext used to serialize user types
    * @param wb   the WriteBuffer that will store the POF stream
    *
    * @return the POF DataOutput implementation
    */
    public static DataOutput createPofDataOutput(PofContext ctx, WriteBuffer wb)
        {
        return new PofOutputStream(new PofBufferWriter(wb.getBufferOutput(),
                ctx));
        }

    /**
    * Create a POF DataInput implementation for the given PofContext and
    * WriteBuffer.
    *
    * @param ctx  the PofContext used to deserialize user types
    * @param wb   the WriteBuffer that contains a POF stream
    *
    * @return the POF DataInput implementation
    */
    public static DataInput createPofDataInput(PofContext ctx, WriteBuffer wb)
        {
        ReadBuffer rb = wb.getReadBuffer();
        return new PofInputStream(new PofBufferReader(rb.getBufferInput(), ctx));
        }

    /**
     * Create an ObjectInputFilter from filter.
     *
     * @return return an ObjectInputFilter as an Object to enable working with Java version 8
     *
     */
    public static Object createObjectInputFilter(String sFilter)
        {
        Class<?> clzConfig = getClass("java.io.ObjectInputFilter$Config");

        clzConfig = clzConfig == null ? getClass("sun.misc.ObjectInputFilter$Config") : clzConfig;

        if (clzConfig != null)
            {
            try
                {
                Method methodConfigCreateFilter = clzConfig.getDeclaredMethod("createFilter", String.class);
                methodConfigCreateFilter.setAccessible(true);

                return methodConfigCreateFilter.invoke(null, sFilter);
                }
            catch (IllegalAccessException | InvocationTargetException e)
                {
                err("Unable to invoke createFilter on ObjectInputFilter$Config");
                err(e);
                }
            catch (Throwable t)
                {
               err(t);
                }
            }
        return null;
        }

    /**
     * Return start time in millieconds.
     *
     * @return start time in milliseconds
     */
    public static long start()
        {
        return System.currentTimeMillis();
        }

    /**
     * Log a description of an operation being timed and its duration in milliseconds.
     *
     * @param ldtStart      start time in milliseconds of operation being timed
     * @param sDescription  operation description
     */
    public static void stop(long ldtStart, String sDescription)
        {
        long lStop = System.currentTimeMillis();
        long lElapsed = lStop - ldtStart;
        out(sDescription + " (" + lElapsed + "ms)");
        }

    // ----- inner class: PointNotSerializable -------------------------------

    /**
     * Not serializable class without public setters, only public constructor to set object state.
     */
    static public class PointNotSerializable
        {
        public PointNotSerializable(int nX, int nY)
            {
            m_nX = nX;
            m_nY = nY;
            }

        public int getX()
            {
            return m_nX;
            }

        public int getY()
            {
            return m_nY;
            }

        @Override
        public boolean equals(Object o)
            {
            if (o instanceof PointNotSerializable)
                {
                PointNotSerializable p = (PointNotSerializable) o;
                return m_nX == p.getX() && m_nY == p.getY();
                }
            return false;
            }

        @Override
        public int hashCode()
            {
            return m_nX + m_nY;
            }

        @Override
        public String toString()
            {
            return "Point[x=" + m_nX + ", y=" + m_nY + "]";
            }

        private int m_nX;
        private int m_nY;
        }

    // ----- inner class: Point ----------------------------------------------

    /**
     * Add ExternalizableLite to a superclass without public setters.
     */
    @ExternalizableType(serializer = Point.Serializer.class)
    static public class Point
            extends PointNotSerializable
            implements ExternalizableLite
        {
        public Point(int nX, int nY)
            {
            super(nX, nY);
            }

        static public class Serializer
                implements ExternalizableLiteSerializer<Point>
            {
            public Point deserialize(DataInput in) throws IOException
                {
                int nX = in.readInt();
                int nY = in.readInt();

                return new Point(nX, nY);
                }

            public void serialize(DataOutput out, Point value)
                    throws IOException
                {
                out.writeInt(value.getX());
                out.writeInt(value.getY());
                }
            }
        }

    // ----- constants ----------------------- -------------------------------

    /**
    * Some test value for decorating.
    */
    public static final Binary   VALUE       = toBinary("value");

    /**
     * Some test value for decorating.
     */
    public static final Binary   PROD_VALUE  = toBinary("product value");

    /**
     * Some test value for decorating.
     */
    public static final Binary   PROD_VALUE2 = toBinary("product value 2");

    /**
    * Some test value for decorating.
    */
    public static final Binary   DIFF        = toBinary("different");

    /**
    * Some test data for decorating.
    */
    public static final Binary[] DIGITS      = new Binary[] {
        toBinary(Boolean.TRUE), toBinary("one"), toBinary(2),
        toBinary(3.0), toBinary(4L),
        toBinary(5F), toBinary(new java.math.BigInteger("6")),
        toBinary(new java.math.BigDecimal("7")),
        toBinary((short) 8), toBinary("3^2")};
    }

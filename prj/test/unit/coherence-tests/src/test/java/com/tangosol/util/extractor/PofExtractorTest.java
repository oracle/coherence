/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PofContext;

import com.tangosol.io.pof.reflect.SimplePofPath;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import data.pof.Address;
import data.pof.ObjectWithAllTypes;
import data.pof.PofDataUtils;
import data.pof.PortablePerson;

import java.io.IOException;

import java.math.BigInteger;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;


/**
* Unit test of the {@link PofExtractor} implementation.
*
* @author as 02/10/2009
*/
public class PofExtractorTest
    {
    // ----- test methods ---------------------------------------------------

    /**
    * Test of how the PofIndex extractor works with plain Binary objects.
    */
    @Test
    public void extractorTestWithPlainBinary()
            throws IOException
        {
        PortablePerson oPerson = PortablePerson.create();
        Binary binPerson = PofDataUtils.serialize(
                                oPerson, PofDataUtils.MODE_PLAIN);

        extractorTest(new TestBinaryEntry(null, binPerson,
                                          PofDataUtils.getPofContext()));
        }

    /**
    * Test of how the PofIndex extractor works with FMT_EXT Binary objects.
    */
    @Test
    public void extractorTestWithFmtExt()
            throws IOException
        {
        PortablePerson oPerson = PortablePerson.create();
        Binary binPerson = PofDataUtils.serialize(
                                oPerson, PofDataUtils.MODE_FMT_EXT);

        extractorTest(new TestBinaryEntry(null, binPerson,
                                          PofDataUtils.getPofContext()));
        }

    /**
    * Test of how the PofIndex extractor works with integer decorated
    * Binary objects.
    */
    @Test
    public void extractorTestWithIntDecoratedBinary()
            throws IOException
        {
        PortablePerson oPerson = PortablePerson.create();
        Binary binPerson = PofDataUtils.serialize(
                                oPerson, PofDataUtils.MODE_FMT_IDO);

        extractorTest(new TestBinaryEntry(null, binPerson,
                                          PofDataUtils.getPofContext()));
        }

    /**
    * Test of how the PofIndex extractor works with decorated Binary objects.
    */
    @Test
    public void extractorTestWithDecoratedBinary()
            throws IOException
        {
        PortablePerson oPerson = PortablePerson.create();
        Binary binPerson = PofDataUtils.serialize(
                                oPerson, PofDataUtils.MODE_FMT_DECO);

        extractorTest(new TestBinaryEntry(null, binPerson,
                                          PofDataUtils.getPofContext()));
        }

    /**
    * Test of how the PofIndex extractor works in combination with
    * KeyExtractor.
    */
    @Test
    public void keyExtractorTest()
            throws IOException
        {
        PortablePerson oPerson = PortablePerson.create();
        Binary binPerson = PofDataUtils.serialize(
                                oPerson, PofDataUtils.MODE_PLAIN);

        BinaryEntry binEntry = new TestBinaryEntry(binPerson, null,
                                          PofDataUtils.getPofContext());

        //AbstractExtractor ke = new KeyExtractor(new PofExtractor(0));
        //assertEquals(ke.extract(binEntry), "Aleksandar Seovic");

        AbstractExtractor ve = new PofExtractor(null,
                new SimplePofPath(new int[] {PortablePerson.ADDRESS, Address.CITY}),
                PofExtractor.KEY);
        assertEquals("Tampa", ve.extractFromEntry(binEntry));
        }

    /**
    * Test of how the PofIExtractor works with all Pof types.
    */
    @Test
    public void extractorTestPrimitiveTypes() throws Exception
        {
        ObjectWithAllTypes oAllTypes = new ObjectWithAllTypes();

        oAllTypes.init();

        Binary       binAllTypes = PofDataUtils.serialize(oAllTypes,
                PofDataUtils.MODE_PLAIN);

        BinaryEntry  binEntry    = new TestBinaryEntry(null, binAllTypes,
                PofDataUtils.getPofContext());

        PofExtractor extractor = new PofExtractor(boolean.class,
                ObjectWithAllTypes.P_BOOLEAN_FALSE);
        assertEquals(extractor.extractFromEntry(binEntry),
                Boolean.FALSE);

        extractor = new PofExtractor(boolean.class,
                ObjectWithAllTypes.P_BOOLEAN_TRUE);
        assertEquals(extractor.extractFromEntry(binEntry),
                Boolean.TRUE);

        extractor = new PofExtractor(char.class, ObjectWithAllTypes.P_CHAR);
        assertEquals(extractor.extractFromEntry(binEntry), 'a');

        extractor = new PofExtractor(byte.class, ObjectWithAllTypes.P_BYTE_0);
        assertEquals(extractor.extractFromEntry(binEntry), (byte) 0);

        extractor = new PofExtractor(byte.class,
                ObjectWithAllTypes.P_BYTE_22);
        assertEquals(extractor.extractFromEntry(binEntry), (byte) 22);

        extractor = new PofExtractor(byte.class, ObjectWithAllTypes.P_BYTE);
        assertEquals(extractor.extractFromEntry(binEntry), Byte.MAX_VALUE);

        extractor = new PofExtractor(short.class,
                ObjectWithAllTypes.P_SHORT_0);
        assertEquals(extractor.extractFromEntry(binEntry), (short) 0);

        extractor = new PofExtractor(short.class,
                ObjectWithAllTypes.P_SHORT_22);
        assertEquals(extractor.extractFromEntry(binEntry), (short) 22);

        extractor = new PofExtractor(short.class, ObjectWithAllTypes.P_SHORT);
        assertEquals(extractor.extractFromEntry(binEntry), Short.MAX_VALUE);

        extractor = new PofExtractor(int.class, ObjectWithAllTypes.P_INT_0);
        assertEquals(extractor.extractFromEntry(binEntry), 0);

        extractor = new PofExtractor(int.class, ObjectWithAllTypes.P_INT_22);
        assertEquals(extractor.extractFromEntry(binEntry), 22);

        extractor = new PofExtractor(int.class, ObjectWithAllTypes.P_INT);
        assertEquals(extractor.extractFromEntry(binEntry), Integer.MAX_VALUE);

        extractor = new PofExtractor(long.class, ObjectWithAllTypes.P_LONG_0);
        assertEquals(extractor.extractFromEntry(binEntry), 0L);

        extractor = new PofExtractor(long.class,
                ObjectWithAllTypes.P_LONG_22);
        assertEquals(extractor.extractFromEntry(binEntry), 22L);

        extractor = new PofExtractor(long.class, ObjectWithAllTypes.P_LONG);
        assertEquals(extractor.extractFromEntry(binEntry), Long.MAX_VALUE);

        extractor = new PofExtractor(float.class,
                ObjectWithAllTypes.P_FLOAT_0);
        assertEquals(extractor.extractFromEntry(binEntry), (float) 0);

        extractor = new PofExtractor(float.class,
                ObjectWithAllTypes.P_FLOAT_22);
        assertEquals(extractor.extractFromEntry(binEntry), 22F);

        extractor = new PofExtractor(float.class, ObjectWithAllTypes.P_FLOAT);
        assertEquals(extractor.extractFromEntry(binEntry), Float.MAX_VALUE);

        extractor = new PofExtractor(double.class,
                ObjectWithAllTypes.P_DOUBLE_0);
        assertEquals(extractor.extractFromEntry(binEntry), (double) 0);

        extractor = new PofExtractor(double.class,
                ObjectWithAllTypes.P_DOUBLE_22);
        assertEquals(extractor.extractFromEntry(binEntry), 22.0);

        extractor = new PofExtractor(double.class,
                ObjectWithAllTypes.P_DOUBLE);
        assertEquals(extractor.extractFromEntry(binEntry), Double.MAX_VALUE);
        // tests with null Class
        extractor = new PofExtractor(null, ObjectWithAllTypes.P_SHORT);
        assertEquals(extractor.extractFromEntry(binEntry), Short.MAX_VALUE);

        extractor = new PofExtractor(null, ObjectWithAllTypes.P_DOUBLE);
        assertEquals(extractor.extractFromEntry(binEntry), Double.MAX_VALUE);
        }

    /**
    * Test of how the PofIExtractor works with all Pof types.
    */
    @Test
    public void extractorTestWrapperTypes() throws Exception
        {
        ObjectWithAllTypes oAllTypes = new ObjectWithAllTypes();

        oAllTypes.init();

        Binary       binAllTypes = PofDataUtils.serialize(oAllTypes,
                PofDataUtils.MODE_PLAIN);

        BinaryEntry  binEntry    = new TestBinaryEntry(null, binAllTypes,
                PofDataUtils.getPofContext());

        PofExtractor extractor = new PofExtractor(Boolean.class,
                ObjectWithAllTypes.BOOLEAN_FALSE);
        assertEquals(extractor.extractFromEntry(binEntry),
                Boolean.FALSE);

        extractor = new PofExtractor(Boolean.class,
                ObjectWithAllTypes.BOOLEAN_TRUE);
        assertEquals(extractor.extractFromEntry(binEntry),
                Boolean.TRUE);

        extractor = new PofExtractor(Boolean.class,
                ObjectWithAllTypes.BOOLEAN_NULL);
        assertNull(extractor.extractFromEntry(binEntry));

        extractor = new PofExtractor(Character.class,
                ObjectWithAllTypes.CHARACTER);
        assertEquals(extractor.extractFromEntry(binEntry), 'a');

        extractor = new PofExtractor(Character.class,
                ObjectWithAllTypes.CHARACTER_NULL);
        assertNull(extractor.extractFromEntry(binEntry));

        extractor = new PofExtractor(Byte.class, ObjectWithAllTypes.BYTE_0);
        assertEquals(extractor.extractFromEntry(binEntry), (byte) 0);

        extractor = new PofExtractor(Byte.class, ObjectWithAllTypes.BYTE_22);
        assertEquals(extractor.extractFromEntry(binEntry), (byte) 22);

        extractor = new PofExtractor(Byte.class,
                ObjectWithAllTypes.BYTE_NULL);
        assertNull(extractor.extractFromEntry(binEntry));

        extractor = new PofExtractor(Byte.class, ObjectWithAllTypes.BYTE);
        assertEquals(extractor.extractFromEntry(binEntry), Byte.MAX_VALUE);

        extractor = new PofExtractor(Short.class, ObjectWithAllTypes.SHORT_0);
        assertEquals(extractor.extractFromEntry(binEntry), (short) 0);

        extractor = new PofExtractor(Short.class,
                ObjectWithAllTypes.SHORT_22);
        assertEquals(extractor.extractFromEntry(binEntry), (short) 22);

        extractor = new PofExtractor(Short.class,
                ObjectWithAllTypes.SHORT_NULL);
        assertNull(extractor.extractFromEntry(binEntry));

        extractor = new PofExtractor(Short.class, ObjectWithAllTypes.SHORT);
        assertEquals(extractor.extractFromEntry(binEntry), Short.MAX_VALUE);

        extractor = new PofExtractor(Integer.class,
                ObjectWithAllTypes.INTEGER_0);
        assertEquals(extractor.extractFromEntry(binEntry), 0);

        extractor = new PofExtractor(Integer.class,
                ObjectWithAllTypes.INTEGER_22);
        assertEquals(extractor.extractFromEntry(binEntry), 22);

        extractor = new PofExtractor(Integer.class,
                ObjectWithAllTypes.INTEGER_NULL);
        assertNull(extractor.extractFromEntry(binEntry));

        extractor = new PofExtractor(Integer.class,
                ObjectWithAllTypes.INTEGER);
        assertEquals(extractor.extractFromEntry(binEntry), Integer.MAX_VALUE);

        extractor = new PofExtractor(Long.class, ObjectWithAllTypes.LONG_0);
        assertEquals(extractor.extractFromEntry(binEntry), 0L);

        extractor = new PofExtractor(Long.class, ObjectWithAllTypes.LONG_22);
        assertEquals(extractor.extractFromEntry(binEntry), 22L);

        extractor = new PofExtractor(Long.class,
                ObjectWithAllTypes.LONG_NULL);
        assertNull(extractor.extractFromEntry(binEntry));

        extractor = new PofExtractor(Long.class, ObjectWithAllTypes.LONG);
        assertEquals(extractor.extractFromEntry(binEntry), Long.MAX_VALUE);

        extractor = new PofExtractor(Float.class,
                ObjectWithAllTypes.FLOAT_0);
        assertEquals(extractor.extractFromEntry(binEntry), (float) 0);

        extractor = new PofExtractor(Float.class,
                ObjectWithAllTypes.FLOAT_22);
        assertEquals(extractor.extractFromEntry(binEntry), 22F);

        extractor = new PofExtractor(Float.class,
                ObjectWithAllTypes.FLOAT_NULL);
        assertNull(extractor.extractFromEntry(binEntry));

        extractor = new PofExtractor(Float.class, ObjectWithAllTypes.FLOAT);
        assertEquals(extractor.extractFromEntry(binEntry), Float.MAX_VALUE);

        extractor = new PofExtractor(Double.class,
                ObjectWithAllTypes.DOUBLE_0);
        assertEquals(extractor.extractFromEntry(binEntry), (double) 0);

        extractor = new PofExtractor(Double.class,
                ObjectWithAllTypes.DOUBLE_22);
        assertEquals(extractor.extractFromEntry(binEntry), 22.0);

        extractor = new PofExtractor(Double.class,
                ObjectWithAllTypes.DOUBLE_NULL);
        assertNull(extractor.extractFromEntry(binEntry));

        extractor = new PofExtractor(Double.class, ObjectWithAllTypes.DOUBLE);
        assertEquals(extractor.extractFromEntry(binEntry), Double.MAX_VALUE);
        // tests with null Class
        extractor = new PofExtractor(null, ObjectWithAllTypes.SHORT);
        assertEquals(extractor.extractFromEntry(binEntry), Short.MAX_VALUE);

        extractor = new PofExtractor(null, ObjectWithAllTypes.DOUBLE);
        assertEquals(extractor.extractFromEntry(binEntry), Double.MAX_VALUE);
       }

    /**
    * Test of how the PofIExtractor works with all Pof types.
    */
    @Test
    public void extractorTestPrimitveArrays() throws Exception
        {
        ObjectWithAllTypes oAllTypes = new ObjectWithAllTypes();

        oAllTypes.init();

        Binary       binAllTypes = PofDataUtils.serialize(oAllTypes,
                PofDataUtils.MODE_PLAIN);

        BinaryEntry  binEntry    = new TestBinaryEntry(null, binAllTypes,
                PofDataUtils.getPofContext());

        PofExtractor extractor = new PofExtractor(boolean[].class,
                ObjectWithAllTypes.P_BOOLEAN_FALSE_ARRAY);
        assertTrue(Arrays.equals(
                (boolean[]) extractor.extractFromEntry(binEntry),
                new boolean[]{false, false}));

        extractor = new PofExtractor(boolean[].class,
                ObjectWithAllTypes.P_BOOLEAN_TRUE_ARRAY);
        assertTrue(Arrays.equals(
                (boolean[]) extractor.extractFromEntry(binEntry),
                new boolean[]{true, true}));

        extractor = new PofExtractor(long[].class,
                ObjectWithAllTypes.P_LONG_0_ARRAY);
        assertTrue(Arrays.equals(
                (long[]) extractor.extractFromEntry(binEntry),
                new long[]{0, 0}));

        extractor = new PofExtractor(long[].class,
                ObjectWithAllTypes.P_LONG_22_ARRAY);
        assertTrue(Arrays.equals(
                (long[]) extractor.extractFromEntry(binEntry),
                new long[]{22, 22}));

        extractor = new PofExtractor(long[].class,
                ObjectWithAllTypes.P_LONG_ARRAY);
        assertTrue(Arrays.equals(
                (long[]) extractor.extractFromEntry(binEntry),
                new long[]{Long.MAX_VALUE, Long.MAX_VALUE}));
        }

    /**
    * Test of how the PofIExtractor works with all Pof types.
    */
    @Test
    public void extractorTestWrapperArrays()
            throws IOException
        {
        ObjectWithAllTypes oAllTypes = new ObjectWithAllTypes();

        oAllTypes.init();

        Binary       binAllTypes = PofDataUtils.serialize(oAllTypes,
                PofDataUtils.MODE_PLAIN);

        BinaryEntry  binEntry    = new TestBinaryEntry(null, binAllTypes,
                PofDataUtils.getPofContext());

        PofExtractor extractor = new PofExtractor(Boolean[].class,
                ObjectWithAllTypes.BOOLEAN_FALSE_ARRAY);
        assertTrue(Arrays.equals(
                (Object[]) extractor.extractFromEntry(binEntry),
                new Object[]{Boolean.FALSE, Boolean.FALSE}));

        extractor = new PofExtractor(Boolean[].class,
                ObjectWithAllTypes.BOOLEAN_TRUE_ARRAY);
        assertTrue(Arrays.equals(
                (Object[]) extractor.extractFromEntry(binEntry),
                new Object[]{Boolean.TRUE, Boolean.TRUE}));

        extractor = new PofExtractor(Long[].class,
                ObjectWithAllTypes.LONG_0_ARRAY);
        assertEquals((Object[]) extractor.extractFromEntry(binEntry),
        new Object[]{0L, 0L});

        extractor = new PofExtractor(Long[].class,
                ObjectWithAllTypes.LONG_22_ARRAY);
        assertEquals((Object[]) extractor.extractFromEntry(binEntry),
        new Object[]{22L, 22L});

        extractor = new PofExtractor(Long[].class,
                ObjectWithAllTypes.LONG_ARRAY);
        assertEquals((Object[]) extractor.extractFromEntry(binEntry),
        new Object[]{Long.MAX_VALUE, Long.MAX_VALUE});
        }

    /**
    * Test of how the PofIExtractor works with all Pof types.
    */
    @Test
    public void extractorTestOtherTypes()
            throws IOException
        {
        ObjectWithAllTypes oAllTypes = new ObjectWithAllTypes();

        oAllTypes.init();

        Binary binAllTypes = PofDataUtils.serialize(oAllTypes,
                PofDataUtils.MODE_PLAIN);

        BinaryEntry binEntry = new TestBinaryEntry(null, binAllTypes,
                PofDataUtils.getPofContext());

        PofExtractor extractor = new PofExtractor(PortablePerson.class,
                ObjectWithAllTypes.PORTABLE_PERSON);
        PortablePerson person  = (PortablePerson) extractor.
                extractFromEntry(binEntry);
        assertEquals(person.m_sName, oAllTypes.m_PortablePerson.m_sName);
        assertEquals(person.m_dtDOB, oAllTypes.m_PortablePerson.m_dtDOB);

        extractor = new PofExtractor(null, ObjectWithAllTypes.PORTABLE_PERSON);
        person    = (PortablePerson) extractor. extractFromEntry(binEntry);
        assertEquals(person.m_sName, oAllTypes.m_PortablePerson.m_sName);
        assertEquals(person.m_dtDOB, oAllTypes.m_PortablePerson.m_dtDOB);

        extractor = new PofExtractor(BigInteger.class, ObjectWithAllTypes.BIG_INTEGER);
        assertEquals(extractor.extractFromEntry(binEntry), BigInteger.ZERO);

        extractor = new PofExtractor(null, ObjectWithAllTypes.BIG_INTEGER);
        assertEquals(extractor.extractFromEntry(binEntry), BigInteger.ZERO);

        Map map   = oAllTypes.m_Map;
        extractor = new PofExtractor(null, ObjectWithAllTypes.MAP);
        assertEquals(extractor.extractFromEntry(binEntry), map);
        extractor = new PofExtractor(map.getClass(), ObjectWithAllTypes.MAP);
        assertEquals(extractor.extractFromEntry(binEntry), map);

        extractor = new PofExtractor(Date.class, ObjectWithAllTypes.DATE);
        Date dt = (Date) extractor.extractFromEntry(binEntry);
        assertTrue(dt.getYear() == oAllTypes.m_dt.getYear()
            && dt.getMonth() == oAllTypes.m_dt.getMonth() && dt.getDate() == oAllTypes.m_dt.getDate());

        extractor = new PofExtractor(Date.class, ObjectWithAllTypes.DATETIME);
        assertEquals(extractor.extractFromEntry(binEntry), oAllTypes.m_dtTime);

        extractor = new PofExtractor(Date.class, ObjectWithAllTypes.TIMESTAMP);
        dt = (Date) extractor.extractFromEntry(binEntry);
        assertEquals(dt, oAllTypes.m_timeStamp);

        List listString = oAllTypes.m_listString;
        extractor = new PofExtractor(null, ObjectWithAllTypes.STRING_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listString);
        extractor = new PofExtractor(listString.getClass(), ObjectWithAllTypes.STRING_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listString);
        for (int i = 0; i < listString.size(); i++)
            {
            extractor = new PofExtractor(null,
                new SimplePofPath(new int[] {ObjectWithAllTypes.STRING_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listString.get(i));
            extractor = new PofExtractor(String.class,
                new SimplePofPath(new int[] {ObjectWithAllTypes.STRING_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listString.get(i));
            }

        listString = oAllTypes.m_listStringUniform;
        extractor = new PofExtractor(null, ObjectWithAllTypes.STRING_U_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listString);
        extractor = new PofExtractor(listString.getClass(), ObjectWithAllTypes.STRING_U_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listString);
        for (int i = 0; i < listString.size(); i++)
            {
            extractor = new PofExtractor(null,
                new SimplePofPath(new int[] {ObjectWithAllTypes.STRING_U_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listString.get(i));
            extractor = new PofExtractor(String.class,
                new SimplePofPath(new int[] {ObjectWithAllTypes.STRING_U_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listString.get(i));
            }

        List listInteger = oAllTypes.m_listInteger;
        extractor = new PofExtractor(null, ObjectWithAllTypes.INTEGER_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listInteger);
        extractor = new PofExtractor(listInteger.getClass(), ObjectWithAllTypes.INTEGER_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listInteger);
        for (int i = 0; i < listInteger.size(); i++)
            {
            extractor = new PofExtractor(null,
                new SimplePofPath(new int[] {ObjectWithAllTypes.INTEGER_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listInteger.get(i));
            extractor = new PofExtractor(Integer.class,
                new SimplePofPath(new int[] {ObjectWithAllTypes.INTEGER_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listInteger.get(i));
            }

        listInteger = oAllTypes.m_listIntegerUniform;
        extractor = new PofExtractor(null, ObjectWithAllTypes.INTEGER_U_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listInteger);
        extractor = new PofExtractor(listInteger.getClass(), ObjectWithAllTypes.INTEGER_U_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listInteger);
        for (int i = 0; i < listInteger.size(); i++)
            {
            extractor = new PofExtractor(null,
                new SimplePofPath(new int[] {ObjectWithAllTypes.INTEGER_U_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listInteger.get(i));
            extractor = new PofExtractor(Integer.class,
                new SimplePofPath(new int[] {ObjectWithAllTypes.INTEGER_U_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listInteger.get(i));
            }

        List listDouble = oAllTypes.m_listDouble;
        extractor = new PofExtractor(null, ObjectWithAllTypes.DOUBLE_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listDouble);
        extractor = new PofExtractor(listDouble.getClass(), ObjectWithAllTypes.DOUBLE_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listDouble);
        for (int i = 0; i < listDouble.size(); i++)
            {
            extractor = new PofExtractor(null,
                new SimplePofPath(new int[] {ObjectWithAllTypes.DOUBLE_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listDouble.get(i));
            extractor = new PofExtractor(Double.class,
                new SimplePofPath(new int[] {ObjectWithAllTypes.DOUBLE_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listDouble.get(i));
            }

        listDouble = oAllTypes.m_listDoubleUniform;
        extractor = new PofExtractor(null, ObjectWithAllTypes.DOUBLE_U_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listDouble);
        extractor = new PofExtractor(listDouble.getClass(), ObjectWithAllTypes.DOUBLE_U_LIST);
        assertEquals(extractor.extractFromEntry(binEntry), listDouble);
        for (int i = 0; i < listDouble.size(); i++)
            {
            extractor = new PofExtractor(null,
                new SimplePofPath(new int[] {ObjectWithAllTypes.DOUBLE_U_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listDouble.get(i));
            extractor = new PofExtractor(Double.class,
                new SimplePofPath(new int[] {ObjectWithAllTypes.DOUBLE_U_LIST, i}));
            assertEquals(extractor.extractFromEntry(binEntry), listDouble.get(i));
            }
        }

    /**
    * The hashCode of a PofExtractor with a CanonicalName does not match
    * a PofExtractor without a canonicalName, ensure no equality between
    * Extractor with CName and one without it.
    */
    @Test
    public void validateMixedCNameExtractorsNotEqual()
        {
        PofExtractor extractor = new PofExtractor(boolean.class,
                                                  ObjectWithAllTypes.P_BOOLEAN_FALSE, "booleanFalse");

        PofExtractor extractorNoCName = new PofExtractor(boolean.class,
                                                         ObjectWithAllTypes.P_BOOLEAN_FALSE);

        assertNotNull(extractor.getCanonicalName());
        assertNull(extractorNoCName.getCanonicalName());
        assertFalse(extractor.equals(extractorNoCName));
        assertNotEquals(extractor.hashCode(), extractorNoCName.hashCode());
        }

    @Test
    public void validateNonCNamePofExtractors()
        {
        PofExtractor extractor1 = new PofExtractor(boolean.class,
                                                   ObjectWithAllTypes.P_BOOLEAN_FALSE);

        PofExtractor extractor2 = new PofExtractor(boolean.class,
                                                   ObjectWithAllTypes.P_BOOLEAN_FALSE);

        assertNull(extractor1.getCanonicalName());
        assertNull(extractor2.getCanonicalName());

        assertTrue(extractor1.equals(extractor2));
        assertTrue(extractor2.equals(extractor1));
        assertEquals(extractor2.hashCode(), extractor1.hashCode());
        }

    /**
    * Test of PofExtractor with canonical name specified to enable
    * comparison with {@link UniversalExtractor} and {@link com.tangosol.internal.util.invoke.lambda.AbstractRemotableLambda}
    * {@link ValueExtractor}.
    */
    @Test
    public void extractorWithCanonicalNameTest() throws Exception
        {
        ObjectWithAllTypes oAllTypes = new ObjectWithAllTypes();

        oAllTypes.init();

        Binary       binAllTypes = PofDataUtils.serialize(oAllTypes,
                                                          PofDataUtils.MODE_PLAIN);
        BinaryEntry     binEntry = new TestBinaryEntry(null, binAllTypes,
                                                       PofDataUtils.getPofContext());
        PofExtractor   extractor = new PofExtractor(boolean.class,
                                                  ObjectWithAllTypes.P_BOOLEAN_FALSE, "booleanFalse");
        assertEquals(extractor.extractFromEntry(binEntry),
                     Boolean.FALSE);
        assertEquals("booleanFalse", extractor.getCanonicalName());

        PofExtractor extractor2 = new PofExtractor(boolean.class,
                                                   ObjectWithAllTypes.P_BOOLEAN_FALSE, "booleanFalseDifferentName");
        assertEquals(extractor2.extractFromEntry(binEntry),
                     Boolean.FALSE);
        assertEquals("booleanFalseDifferentName", extractor2.getCanonicalName());
        assertFalse(extractor2.equals(extractor));
        assertNotEquals(extractor.hashCode(), extractor2.hashCode());

        PofExtractor extractor3 = new PofExtractor(boolean.class,
                                                   ObjectWithAllTypes.P_BOOLEAN_FALSE, "booleanFalse");
        assertEquals(extractor3.extractFromEntry(binEntry),
                     Boolean.FALSE);
        assertEquals("booleanFalse", extractor3.getCanonicalName());
        assertTrue(extractor.equals(extractor3));
        assertEquals(extractor.hashCode(), extractor3.hashCode());
        assertEquals(extractor3.extractFromEntry(binEntry), extractor.extractFromEntry(binEntry));

        // test equals and hashCode between PofExtractor with canonical name and equivalent ReflectionExtractors.
        UniversalExtractor re = new UniversalExtractor("booleanFalse");

        assertTrue(re.equals(extractor3));
        assertTrue(extractor3.equals(re));
        assertEquals(re.hashCode(), extractor3.hashCode());

        // following should not match since PofExtractor canonical name does not match RE
        assertFalse(re.equals(extractor2));
        assertFalse(extractor2.equals(re));
        assertNotEquals(re.hashCode(), extractor2.hashCode());
        }

    /**
    * Validate equals/hashCode contract when one PofExtractor has canonical name
    * and other one does not.
    */
    @Test
    public void testPofExtractorEqualsHashCodeContract()
        {
        AbstractExtractor ve = new PofExtractor(null,
                                                new SimplePofPath(new int[] {PortablePerson.ADDRESS, Address.CITY}),
                                                PofExtractor.KEY);

        AbstractExtractor veSame = new PofExtractor(null,
                                                    new SimplePofPath(new int[] {PortablePerson.ADDRESS, Address.CITY}),
                                                    PofExtractor.KEY);

        AbstractExtractor vec = new PofExtractor(null,
                                                 new SimplePofPath(new int[] {PortablePerson.ADDRESS, Address.CITY}),
                                                 PofExtractor.KEY,
                                                 "address.city");

        assertTrue(ve.equals(veSame));
        assertEquals(ve.hashCode(), veSame.hashCode());

        assertFalse(ve.equals(vec));
        assertNotEquals(ve.hashCode(), vec.hashCode());
        assertFalse(vec.equals(veSame));
        assertNotEquals(veSame.hashCode(), vec.hashCode());
        }

    // COH-18199 regression test: if intermediate pof navigation path is null, just return null as ChainedExtractor does.
    @Test
    public void verifyPofExtractorShouldNotThrowNullPointerException()
            throws IOException
        {
        PortablePerson oPerson = PortablePerson.create();

        oPerson.setSpouse(null);

        Binary       binPerson = PofDataUtils.serialize(oPerson, PofDataUtils.MODE_PLAIN);
        BinaryEntry  binEntry  = new TestBinaryEntry(null, binPerson, PofDataUtils.getPofContext());
        PofExtractor ve        = new PofExtractor(null, new SimplePofPath(
                new int[] {PortablePerson.SPOUSE, PortablePerson.ADDRESS, Address.CITY}));

        assertEquals(null, ve.extractFromEntry(binEntry));
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Assertions used by all PofExtractor tests.
    *
    * @param binEntry  binary entry to extract values from
    */
    public static void extractorTest(BinaryEntry binEntry)
        {
        PofExtractor ve = new PofExtractor(null, PortablePerson.NAME);
        assertEquals("Aleksandar Seovic", ve.extractFromEntry(binEntry));

        ve = new PofExtractor(null, new SimplePofPath(
                new int[] {PortablePerson.ADDRESS, Address.CITY}));
        assertEquals("Tampa", ve.extractFromEntry(binEntry));
        }


    // ----- BinaryEntry stub -----------------------------------------------

    public static class TestBinaryEntry
            implements BinaryEntry
        {
        // ----- constructor ----------------------------------------------

        public TestBinaryEntry(Binary binKey, Binary binValue, PofContext pofContext)
            {
            m_binKey = binKey;
            m_binValue = binValue;
            m_pofContext = pofContext;
            }

        // ----- BinaryEntry implementation -------------------------------

        public Binary getBinaryKey()
            {
            return m_binKey;
            }

        public Binary getBinaryValue()
            {
            return m_binValue;
            }

        public Serializer getSerializer()
            {
            return m_pofContext;
            }

        public BackingMapManagerContext getContext()
            {
            return null;
            }

        public void updateBinaryValue(Binary binValue)
            {
            m_binValue = binValue;
            }

        public void updateBinaryValue(Binary binValue, boolean fSynthetic)
            {
            updateBinaryValue(binValue);
            }

        public Object getKey()
            {
            return null;
            }

        public Object getValue()
            {
            return null;
            }

        public Object setValue(Object oValue)
            {
            return null;
            }

        public void setValue(Object oValue, boolean fSynthetic)
            {
            }

        public void update(ValueUpdater updater, Object oValue)
            {
            }

        public boolean isPresent()
            {
            return false;
            }

        public boolean isSynthetic()
            {
            return false;
            }

        public void remove(boolean fSynthetic)
            {
            }

        public Object extract(ValueExtractor extractor)
            {
            return null;
            }

        public Object getOriginalValue()
            {
            return null;
            }

        public Binary getOriginalBinaryValue()
            {
            return null;
            }

        public ObservableMap getBackingMap()
            {
            return null;
            }

        public BackingMapContext getBackingMapContext()
            {
            return null;
            }

        public void expire(long cMillis)
            {
            }

        public long getExpiry()
            {
            return CacheMap.EXPIRY_DEFAULT;
            }

        public boolean isReadOnly()
            {
            return true;
            }

        private Binary m_binKey;
        private Binary m_binValue;
        private PofContext m_pofContext;
        }
    }

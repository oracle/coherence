/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


import com.tangosol.io.pof.PofConstants;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Binary;
import com.tangosol.util.CompositeKey;
import com.tangosol.util.LongArray;

import data.pof.ObjectWithAllTypes;
import data.pof.PofDataUtils;
import data.pof.PortablePerson;
import data.pof.PortablePersonReference;
import data.pof.TestValue;

import org.junit.Test;

import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * Tests for the PofValue class.
 *
 * @author as  2009.01.31
 */
public class PofValueTest
        extends PofDataUtils
    {
    // ----- test methodds --------------------------------------------------

    /**
    * Test {@link PofValue(Binary, PofContext)}.
    */
    @Test
    public void testPofValueInitialization()
            throws IOException
        {
        PortablePerson person = PortablePerson.create();

        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            Binary   binPerson = serialize(person, MODE_PLAIN, fRefEnabled);
            PofValue pv        = PofValueParser.parse(binPerson, getPofContext(fRefEnabled));
            assertEquals(pv.getTypeId(), 2);
            assertEquals(pv.getValue(), person);

            if (fRefEnabled)
                {
                break;
                }
            fRefEnabled = true;
            }
        }

    /**
    * Test access to POF values using plain Binary.
    */
    @Test
    public void testPofValueAccessorWithPlainBinary()
            throws IOException
        {
        PortablePerson person    = PortablePerson.create();
        Binary         binPerson = serialize(person, MODE_PLAIN);

        testPofValueAccessor(person, binPerson, false);

        // Test the case where we try to access an object that contains a
        // uniform collection of user defined objects when object reference
        // is enabled.
        try
            {
            binPerson = serialize(person, MODE_PLAIN, true);
            testPofValueAccessor(person, binPerson, true);
            fail("Should've thrown UnsupportedOperationException.");
            }
        catch (UnsupportedOperationException e)
            {
            }
        }

    /**
    * Test access to POF values using FMT_EXT Binary.
    */
    @Test
    public void testPofValueAccessorWithFmtExtBinary()
            throws IOException
        {
        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            PortablePerson person    = fRefEnabled ?
                    PortablePerson.createNoChildren() : PortablePerson.create();
            Binary         binPerson = serialize(person, MODE_FMT_EXT, fRefEnabled);
            testPofValueAccessor(person, binPerson, fRefEnabled);

            if (fRefEnabled)
                {
                break;
                }
            fRefEnabled = true;
            }
        }

    /**
    * Test access to POF values using int decorated Binary.
    */
    @Test
    public void testPofValueAccessorWithIntegerDecoratedObject()
            throws IOException
        {
        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            PortablePerson person    = fRefEnabled ?
                    PortablePerson.createNoChildren() : PortablePerson.create();
            Binary         binPerson = serialize(person, MODE_FMT_IDO, fRefEnabled);
            testPofValueAccessor(person, binPerson, fRefEnabled);

            if (fRefEnabled)
                {
                break;
                }
            fRefEnabled = true;
            }
        }

    /**
    * Test access to POF values using decorated Binary.
    */
    @Test
    public void testPofValueAccessorWithDecoratedBinary()
            throws IOException
        {
        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            PortablePerson person    = fRefEnabled ?
                    PortablePerson.createNoChildren() : PortablePerson.create();
            Binary         binPerson = serialize(person, MODE_FMT_DECO, fRefEnabled);
            testPofValueAccessor(person, binPerson, fRefEnabled);

            if (fRefEnabled)
                {
                break;
                }
            fRefEnabled = true;
            }
        }

    /**
    * Assertions used by all testPofValueAccessor* methods.
    *
    * @param person       object to test
    * @param binPerson    binary serialized object
    * @param fRefEnabled  flag to indicate if object identity/reference is enabled
    */
    public void testPofValueAccessor(PortablePerson person, Binary binPerson, boolean fRefEnabled)
            throws IOException
        {
        PofValue pv = PofValueParser.parse(binPerson, getPofContext(fRefEnabled));
        assertEquals(pv.getChild(1).getValue(), person.getAddress());
        assertEquals(pv.getChild(0).getValue(), person.m_sName);
        assertEquals(((Date) pv.getChild(2).getValue()).getTime(),
                person.m_dtDOB.getTime());

        // test NilPofValue
        PofValue nv = pv.getChild(100);
        assertNotNull(nv);
        assertNull(nv.getValue());

        // test PofNavigationException
        try
            {
            pv.getChild(0).getChild(0);
            fail("Should've thrown PofNavigationException");
            }
        catch (PofNavigationException ignore) {}
        }

    /**
    * Test access to nested POF values.
    */
    @Test
    public void testNestedPofValueAccessor()
            throws IOException
        {
        PortablePerson person = PortablePerson.create();

        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            Binary   binPerson = serialize(person, MODE_PLAIN, fRefEnabled);
            PofValue pv        = PofValueParser.parse(binPerson, getPofContext(fRefEnabled));
            assertEquals(pv.getChild(1).getChild(0).getValue(),
                                person.getAddress().m_sStreet);
            assertEquals(pv.getChild(1).getChild(1).getValue(),
                                person.getAddress().m_sCity);
            assertEquals(pv.getChild(1).getChild(2).getValue(),
                                person.getAddress().m_sState);
            assertEquals(pv.getChild(1).getChild(3).getValue(),
                                person.getAddress().m_sZip);

            if (fRefEnabled)
                {
                break;
                }
            fRefEnabled = true;
            }
        }

    /**
    * Test POF value mutation with plain Binary.
    */
    @Test
    public void testPofValueMutatorWithPlainBinary()
            throws IOException
        {
        PortablePerson person = PortablePerson.create();
        Binary binPerson = serialize(person, MODE_PLAIN);

        testPofValueMutator(person, binPerson);
        }

    /**
    * Test POF value mutation with plain Binary.
    */
    @Test
    public void testPofValueMutatorWithFmtExtBinary()
            throws IOException
        {
        PortablePerson person = PortablePerson.create();
        Binary binPerson = serialize(person, MODE_FMT_EXT);

        testPofValueMutator(person, binPerson);
        }

    /**
    * Test POF value mutation with plain Binary.
    */
    @Test
    public void testPofValueMutatorWithIntDecoratedObject()
            throws IOException
        {
        PortablePerson person = PortablePerson.create();
        Binary binPerson = serialize(person, MODE_FMT_IDO);

        testPofValueMutator(person, binPerson);
        }

    /**
    * Test POF value mutation with decorated Binary.
    */
    @Test
    public void testPofValueMutatorWithDecoratedBinary()
            throws IOException
        {
        PortablePerson person = PortablePerson.create();
        Binary binPerson = serialize(person, MODE_FMT_DECO);

        testPofValueMutator(person, binPerson);
        }

    /**
    * Assertions used by all testPofValueMutator* methods.
    *
    * @param p          object to test
    * @param binPerson  binary serialized object
    */
    public void testPofValueMutator(PortablePerson p, Binary binPerson)
            throws IOException
        {
        PofValue pv            = PofValueParser.parse(binPerson, getPofContext());
        Binary   binUnmodified = pv.applyChanges();
        assertEquals(binUnmodified, binPerson);

        pv.getChild(0).setValue("Seovic Aleksandar");
        assertEquals(pv.getChild(0).getValue(), "Seovic Aleksandar");

        pv.getChild(0).setValue("Marija Seovic");
        pv.getChild(1).getChild(0).setValue("456 Main St");
        pv.getChild(1).getChild(1).setValue("Lutz");
        pv.getChild(1).getChild(3).setValue("33549");
        pv.getChild(2).setValue(new Date(78, 1, 20));
        pv.getChild(3).setValue(new PortablePerson("Aleksandar Seovic", new Date(74, 7, 24)));
        pv.getChild(4).setValue(p.getChildren());
        binPerson = pv.applyChanges();

        PortablePerson p2 = (PortablePerson) deserialize(binPerson);
        assertEquals(p2.m_sName, "Marija Seovic");
        assertEquals(p2.getAddress().m_sStreet, "456 Main St");
        assertEquals(p2.getAddress().m_sCity, "Lutz");
        assertEquals(p2.getAddress().m_sZip, "33549");
        assertEquals(p2.m_dtDOB, new Date(78, 1, 20));
        assertEquals(p2.getSpouse().m_sName, "Aleksandar Seovic");
        assertEquals(p2.getChildren(), p.getChildren());

        pv = PofValueParser.parse(binPerson, getPofContext());
        pv.getChild(0).setValue("Ana Maria Seovic");
        pv.getChild(2).setValue(new Date(104, 7, 14));
        pv.getChild(3).setValue(null);
        pv.getChild(4).setValue(null);
        binPerson = pv.applyChanges();

        PortablePerson p3 = (PortablePerson) deserialize(binPerson);
        assertEquals(p3.m_sName, "Ana Maria Seovic");
        assertEquals(p3.getAddress(), p2.getAddress());
        assertEquals(p3.m_dtDOB, new Date(104, 7, 14));
        assertNull(p3.getSpouse());
        assertNull(p3.getChildren());
        }

    /**
    * Test PofArray.
    */
    @Test
    public void testPofArray()
            throws IOException
        {
        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            TestValue tv  = TestValue.create(fRefEnabled);
            Binary    bin = serialize(tv, MODE_FMT_EXT, fRefEnabled);

            PofValue root = PofValueParser.parse(bin, getPofContext(fRefEnabled));
            PofValue pv   = root.getChild(0);
            assertEquals(((PofArray) pv).getLength(), 4);
            assertEquals(pv.getChild(0).getValue(), Integer.valueOf(1));
            assertEquals(pv.getChild(1).getValue(), "two");
            assertEquals(pv.getChild(2).getValue(), fRefEnabled ? PortablePerson.createNoChildren() : PortablePerson.create());
            assertEquals(pv.getChild(3).getValue(), new Binary(new byte[]{22, 23, 24}));

            try
                {
                pv.getChild(100);
                fail("Should've thrown IndexOutOfBoundsException.");
                }
            catch (IndexOutOfBoundsException ignore)
                {
                }

            if (fRefEnabled)
                {
                break;
                }

            pv.getChild(1).setValue("dva");
            pv.getChild(2).getChild(0).setValue("Novak");
            Binary binModified = root.applyChanges();

            TestValue tvModified = (TestValue) deserialize(binModified);
            assertEquals(tvModified.m_oArray[1], "dva");
            assertEquals(((PortablePerson) tvModified.m_oArray[2]).m_sName,
                                "Novak");

            fRefEnabled = true;
            }
        }

    /**
    * Test PofUniformArray.
    */
    @Test
    public void testPofUniformArray()
            throws IOException
        {
        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            TestValue tv  = TestValue.create(fRefEnabled);
            Binary    bin = serialize(tv, MODE_FMT_EXT, fRefEnabled);

            PofValue root = PofValueParser.parse(bin, getPofContext(fRefEnabled));
            PofValue pv   = root.getChild(1);
            assertEquals(((PofArray) pv).getLength(), 4);
            assertEquals(pv.getChild(0).getValue(), "one");
            assertEquals(pv.getChild(1).getValue(), "two");
            assertEquals(pv.getChild(2).getValue(), "three");
            assertEquals(pv.getChild(3).getValue(), "four");

            try
                {
                pv.getChild(100);
                fail("Should've thrown IndexOutOfBoundsException.");
                }
            catch (IndexOutOfBoundsException ignore)
                {}

            if (fRefEnabled)
                {
                try
                    {
                    root.applyChanges();
                    fail("Should've thrown UnsupportedOperationException.");
                    }
                catch (UnsupportedOperationException e)
                    {
                    }
                }
            else
                {
                pv.getChild(0).setValue("jedan");
                pv.getChild(3).setValue("cetiri");
                Binary binModified = root.applyChanges();

                TestValue tvModified = (TestValue) deserialize(binModified);
                assertEquals(tvModified.m_sArray[0], "jedan");
                assertEquals(tvModified.m_sArray[3], "cetiri");
                }

            if (fRefEnabled)
                {
                break;
                }
            fRefEnabled = true;
            }
        }

    /**
    * Test PofCollection.
    */
    @Test
    public void testPofCollection()
            throws IOException
        {
        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            TestValue tv  = TestValue.create(fRefEnabled);
            Binary    bin = serialize(tv, MODE_FMT_EXT, fRefEnabled);

            PofValue root = PofValueParser.parse(bin, getPofContext(fRefEnabled));
            PofValue pv   = root.getChild(0);
            Object o = pv.getValue();
            if (fRefEnabled)
                {
                pv = pv.getChild(2);
                }
            pv   = root.getChild(2);
            assertEquals(((PofCollection) pv).getLength(), 4);
            assertEquals(pv.getChild(0).getValue(), Integer.valueOf(1));
            assertEquals(pv.getChild(1).getValue(), "two");
            Object person = pv.getChild(2).getValue();
            assertEquals(person, fRefEnabled ? PortablePerson.createNoChildren() : PortablePerson.create());

            try
                {
                pv.getChild(100);
                fail("Should've thrown IndexOutOfBoundsException.");
                }
            catch (IndexOutOfBoundsException ignore)
                {}

            if (fRefEnabled)
                {
                break;
                }

            pv.getChild(1).setValue("dva");
            pv.getChild(2).getChild(0).setValue("Novak");
            assertEquals(pv.getChild(3).getValue(), new Binary(new byte[]{22, 23, 24}));
            Binary binModified = root.applyChanges();

            TestValue tvModified = (TestValue) deserialize(binModified);
            assertEquals(((ArrayList) tvModified.m_col).get(1), "dva");
            assertEquals(((PortablePerson) ((ArrayList) tvModified.m_col).get(2)).m_sName,
                                "Novak");

            root = PofValueParser.parse(bin, getPofContext());
            pv = root.getChild(0);
            person = pv.getChild(2).getValue();

            PofValue pv2     = root.getChild(2);
            Object   address = pv2.getChild(2).getChild(1).getValue();
            assertEquals(address, ((PortablePerson) person).getAddress());

            root = PofValueParser.parse(bin, getPofContext());
            pv = root.getChild(2);
            pv.getChild(2).getChild(0).setValue("John Smith");
            binModified = root.applyChanges();
            tvModified = (TestValue) deserialize(binModified);
            assertEquals(((PortablePerson) ((ArrayList) tvModified.m_col).get(2)).m_sName,
                                "John Smith");
            fRefEnabled = true;
            }
        }

    /**
    * Test PofUniformCollection.
    */
    @Test
    public void testPofUniformCollection()
            throws IOException
        {
        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            TestValue tv   = TestValue.create(fRefEnabled);
            Binary    bin  = serialize(tv, MODE_FMT_EXT, fRefEnabled);

            PofValue  root = PofValueParser.parse(bin, getPofContext(fRefEnabled));
            PofValue  pv   = root.getChild(3);

            assertEquals(((PofArray) pv).getLength(), 4);
            assertEquals(pv.getChild(0).getValue(), "one");
            assertEquals(pv.getChild(1).getValue(), "two");
            assertEquals(pv.getChild(2).getValue(), "three");
            assertEquals(pv.getChild(3).getValue(), "four");

            try
                {
                pv.getChild(100);
                fail("Should've thrown IndexOutOfBoundsException.");
                }
            catch (IndexOutOfBoundsException ignore)
                {}

            if (fRefEnabled)
                {
                break;
                }

            pv.getChild(0).setValue("jedan");
            pv.getChild(3).setValue("cetiri");
            Binary binModified = root.applyChanges();

            TestValue tvModified = (TestValue) deserialize(binModified);
            ArrayList sList = (ArrayList) tvModified.m_colUniform;
            assertEquals(sList.get(0), "jedan");
            assertEquals(sList.get(3), "cetiri");
            fRefEnabled = true;
            }
        }

    /**
    * Test PofSparseArray.
    */
    @Test
    public void testPofSparseArray()
            throws IOException
        {
        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            TestValue tv  = TestValue.create(fRefEnabled);
            Binary    bin = serialize(tv, MODE_FMT_EXT, fRefEnabled);

            PofValue root = PofValueParser.parse(bin, getPofContext(fRefEnabled));
            PofValue pv   = root.getChild(4);

            // Test the case where we try to read a reference id before the object is read.
            // We should get an
            // IOException: missing identity: 2
            // Work around by reading the person object in root.getChild(0) where it first appears
            // so that root.getChild(4).getValue(), which references the person object will have it.
            if (fRefEnabled)
                {
                try
                    {
                    System.out.println(pv.getValue());
                    fail("Should've thrown Exception.");
                    }
                catch (Exception e)
                    {
                    }
                root.getChild(0).getChild(2).getValue();
                }
            System.out.println(pv.getValue());

            assertTrue(pv instanceof PofSparseArray);
            assertEquals(pv.getChild(4).getValue(), Integer.valueOf(4));
            assertEquals(pv.getChild(2).getValue(), "two");
            assertEquals(pv.getChild(5).getValue(), fRefEnabled ? PortablePerson.createNoChildren() : PortablePerson.create());

            if (fRefEnabled)
                {
                break;
                }

            pv.getChild(1).setValue(Integer.valueOf(1));
            pv.getChild(2).setValue("dva");
            pv.getChild(3).setValue("tri");
            pv.getChild(5).getChild(0).setValue("Novak");
            Binary binModified = root.applyChanges();

            TestValue tvModified = (TestValue) deserialize(binModified);
            System.out.println("m_oArray:Person " + tvModified.m_oArray[2]);
            System.out.println(tvModified.m_sparseArray);

            // not using reference, modification of person in m_sparseArray
            // does not affect the person in m_oArray.
            assertEquals(((PortablePerson) tvModified.m_oArray[2]).m_sName,
                                "Aleksandar Seovic");
            assertEquals(tvModified.m_sparseArray.get(1), Integer.valueOf(1));
            assertEquals(tvModified.m_sparseArray.get(2), "dva");
            assertEquals(tvModified.m_sparseArray.get(3), "tri");
            assertEquals(((PortablePerson) tvModified.m_sparseArray.get(5)).m_sName,
                                "Novak");
            fRefEnabled = true;
            }
        }

    /**
    * Test PofUniformSparseArray.
    */
    @Test
    public void testPofUniformSparseArray()
            throws IOException
        {
        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            TestValue tv  = TestValue.create(fRefEnabled);
            Binary    bin = serialize(tv, MODE_FMT_EXT, fRefEnabled);

            PofValue root = PofValueParser.parse(bin, getPofContext(fRefEnabled));
            PofValue pv   = root.getChild(5);
            System.out.println(pv.getValue());

            assertTrue(pv instanceof PofUniformSparseArray);
            assertEquals(pv.getChild(2).getValue(), "two");
            assertEquals(pv.getChild(4).getValue(), "four");

            if (fRefEnabled)
                {
                break;
                }

            pv.getChild(1).setValue("jedan");
            pv.getChild(3).setValue("tri");
            pv.getChild(4).setValue("cetiri");
            pv.getChild(5).setValue("pet");
            Binary binModified = root.applyChanges();

            TestValue tvModified = (TestValue) deserialize(binModified);
            LongArray arr        = tvModified.m_uniformSparseArray;
            System.out.println(arr);
            assertEquals(arr.get(1), "jedan");
            assertEquals(arr.get(2), "two");
            assertEquals(arr.get(3), "tri");
            assertEquals(arr.get(4), "cetiri");
            assertEquals(arr.get(5), "pet");
            fRefEnabled = true;
            }
        }

    /**
    * Test getBooleanArray().
    */
    @Test
    public void testGetBooleanArray()
            throws IOException
        {
        boolean[] af    = new boolean[] {true, false, true, false, true, false};
        Binary    bin   = serialize(af, MODE_PLAIN);
        PofValue  pv    = PofValueParser.parse(bin, getPofContext());
        boolean[] afGet = pv.getBooleanArray();

        assertEquals(afGet.length, af.length);

        for (int i = 0; i < af.length; i++)
            {
            assertTrue(afGet[i] == af[i]);
            }

        try
            {
            pv.getObjectArray();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getByteArray().
    */
    @Test
    public void testGetByteArray()
            throws IOException
        {
        byte[]   ab    = new byte[] {0, -1, 2, -3, 4, -5};
        Binary   bin   = serialize(ab, MODE_PLAIN);
        PofValue pv    = PofValueParser.parse(bin, getPofContext());
        byte[]   abGet = pv.getByteArray();

        assertEquals(abGet.length, ab.length);

        for (int i = 0; i < ab.length; i++)
            {
            assertTrue(abGet[i] == ab[i]);
            }

        try
            {
            pv.getCharArray();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getCharArray().
    */
    @Test
    public void testGetCharArray()
            throws IOException
        {
        char[]   ach    = new char[] {'\u0000', '\u1010', '\u2020', '\u3030'};
        Binary   bin    = serialize(ach, MODE_PLAIN);
        PofValue pv     = PofValueParser.parse(bin, getPofContext());
        char[]   achGet = pv.getCharArray();

        assertEquals(achGet.length, ach.length);

        for (int i = 0; i < ach.length; i++)
            {
            assertTrue(achGet[i] == ach[i]);
            }
        try
            {
            pv.getShortArray();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getShortArray().
    */
    @Test
    public void testGetShortArray()
            throws IOException
        {
        short[]  an    = new short[] {0x0000, 0x0001, 0x0002, 0x0003, 0x0004};
        Binary   bin   = serialize(an, MODE_PLAIN);
        PofValue pv    = PofValueParser.parse(bin, getPofContext());
        short[]  anGet = pv.getShortArray();

        assertEquals(anGet.length, an.length);

        for (int i = 0; i < an.length; i++)
            {
            assertTrue(anGet[i] == an[i]);
            }

        try
            {
            pv.getIntArray();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getIntArray().
    */
    @Test
    public void testGetIntArray()
            throws IOException
        {
        int[]    an    = new int[] {0x0000, 0x0001, 0x0002, 0x0003, 0x0004};
        Binary   bin   = serialize(an, MODE_PLAIN);
        PofValue pv    = PofValueParser.parse(bin, getPofContext());
        int[]    anGet = pv.getIntArray();

        assertEquals(anGet.length, an.length);

        for (int i = 0; i < an.length; i++)
            {
            assertTrue(anGet[i] == an[i]);
            }

        try
            {
            pv.getLongArray();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getLongArray().
    */
    @Test
    public void testGetLongArray()
            throws IOException
        {
        long[]   al    = new long[] {0L, 1L, 2L, 3L, 4L, 5L};
        Binary   bin   = serialize(al, MODE_PLAIN);
        PofValue pv    = PofValueParser.parse(bin, getPofContext());
        long[]   alGet = pv.getLongArray();

        assertEquals(alGet.length, al.length);

        for (int i=0; i<al.length; i++)
            {
            assertTrue(alGet[i] == al[i]);
            }

        try
            {
            pv.getIntArray();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getFloatArray().
    */
    @Test
    public void testGetFloatArray()
            throws IOException
        {
        float[]  afl    = new float[] {0.0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        Binary   bin    = serialize(afl, MODE_PLAIN);
        PofValue pv     = PofValueParser.parse(bin, getPofContext());
        float[]  aflGet = pv.getFloatArray();

        assertEquals(aflGet.length, afl.length);

        for (int i  =0; i < afl.length; i++)
            {
            assertTrue(aflGet[i] == afl[i]);
            }

        try
            {
            pv.getIntArray();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getDoubleArray().
    */
    @Test
    public void testGetDoubleArray()
            throws IOException
        {
        double[] adfl    = new double[] {0.0d, 0.1d, 0.2d, 0.3d, 0.4d, 0.5d};
        Binary   bin     = serialize(adfl, MODE_PLAIN);
        PofValue pv      = PofValueParser.parse(bin, getPofContext());
        double[] adflGet = pv.getDoubleArray();

        assertEquals(adflGet.length, adfl.length);

        for (int i = 0; i < adfl.length; i++)
            {
            assertTrue(adflGet[i] == adfl[i]);
            }

        try
            {
            pv.getIntArray();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getBigInteger().
    */
    @Test
    public void testGetBigInteger()
            throws IOException
        {
        BigInteger n    = new BigInteger("12345678901234567890");
        Binary     bin  = serialize(n, MODE_PLAIN);
        PofValue   pv   = PofValueParser.parse(bin, getPofContext());
        BigInteger nGet = pv.getBigInteger();

        assertEquals(nGet, n);

        try
            {
            pv.getString();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getBigDecimal().
    */
    @Test
    public void testGetBigDecimal()
            throws IOException
        {
        BigDecimal dec    = new BigDecimal("1234567890.0987654321");
        Binary     bin    = serialize(dec, MODE_PLAIN);
        PofValue   pv     = PofValueParser.parse(bin, getPofContext());
        BigDecimal decGet = pv.getBigDecimal();

        assertEquals(decGet, dec);

        try
            {
            pv.getString();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getString().
    */
    @Test
    public void testGetString()
            throws IOException
        {
        String   s    = "qwerty";
        Binary   bin  = serialize(s, MODE_PLAIN);
        PofValue pv   = PofValueParser.parse(bin, getPofContext());
        String   sGet = pv.getString();

        assertEquals(sGet, s);

        try
            {
            pv.getInt();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getDate().
    */
    @Test
    public void testGetDate()
            throws IOException
        {
        Date     date    = new Date();
        Binary   bin     = serialize(date, MODE_PLAIN);
        PofValue pv      = PofValueParser.parse(bin, getPofContext());
        Date     dateGet = pv.getDate();

        assertEquals(date, dateGet);

        try
            {
            pv.getInt();
            fail("Should've thrown RuntimeException.");
            }
        catch (RuntimeException ignore) {}
        }

    /**
    * Test getObjectArray().
    */
    @Test
    public void testGetObjectArray()
            throws IOException
        {
        Object[] ao = new Object[] {"1", Integer.valueOf(1), Character.valueOf('a')};

        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            Binary   bin   = serialize(ao, MODE_PLAIN, fRefEnabled);
            PofValue pv    = PofValueParser.parse(bin, getPofContext(fRefEnabled));
            Object[] aoGet = pv.getObjectArray();

            assertArrayEquals(aoGet, ao);

            try
                {
                pv.getInt();
                fail("Should've thrown RuntimeException.");
                }
            catch (RuntimeException ignore) {}

            if (fRefEnabled)
                {
                break;
                }
            fRefEnabled = true;
            }
        }

    /**
    * Test getCollection().
    */
    @Test
    public void testGetCollection()
            throws IOException
        {
        Collection col = new ArrayList();
        Collections.addAll(col, "1", Integer.valueOf(1),  Character.valueOf('a'));

        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            Binary     bin    = serialize(col, MODE_PLAIN, fRefEnabled);
            PofValue   pv     = PofValueParser.parse(bin, getPofContext(fRefEnabled));
            Collection colGet = pv.getCollection(null);

            assertEquals(colGet, col);

            try
                {
                pv.getInt();
                fail("Should've thrown RuntimeException.");
                }
            catch (RuntimeException ignore) {}

            Collection col2 = new ArrayList();
            col2.add("Append");
            colGet = pv.getCollection(col2);

            assertSame(col2, colGet);
            assertTrue(col2.size() == 4);
            assertTrue(col2.contains("Append"));

            if (fRefEnabled)
                {
                break;
                }
            fRefEnabled = true;
            }
        }

    /**
    * Test getMap().
    */
    @Test
    public void testGetMap()
            throws IOException
        {
        Map map = new HashMap();
        map.put("jedan", "1");
        map.put("dva", Integer.valueOf(2));
        map.put("true", Boolean.TRUE);

        // perform the test twice, once with references disable, once with them enabled
        for (boolean fRefEnabled = false; ; )
            {
            Binary   bin    = serialize(map, MODE_PLAIN, fRefEnabled);
            PofValue pv     = PofValueParser.parse(bin, getPofContext(fRefEnabled));
            Map      mapGet = pv.getMap(null);

            assertEquals(mapGet, map);

            try
                {
                pv.getInt();
                fail("Should've thrown RuntimeException.");
                }
            catch (RuntimeException ignore) {}

            Map map2 = new HashMap();
            map2.put("Append", "Value");
            mapGet = pv.getMap(map2);

            assertSame(map2, mapGet);
            assertTrue(map2.size() == 4);
            assertTrue(map2.containsKey("Append"));

            if (fRefEnabled)
                {
                break;
                }
            fRefEnabled = true;
            }
        }

    /**
    * Test getBoolean().
    */
    @Test
    public void testGetBoolean()
            throws IOException
        {
        ObjectWithAllTypes testObj = new ObjectWithAllTypes();
        testObj.init();

        PofContext ctx   = getPofContext();
        Binary     bin   = serialize(testObj, MODE_FMT_EXT);
        PofValue   pv    = PofValueParser.parse(bin, ctx);

        PofValue boolOne = new SimplePofPath(0).navigate(pv);
        PofValue boolTwo = new SimplePofPath(1).navigate(pv);

        assertEquals(false, boolOne.getValue(Boolean.class));
        assertEquals(true, boolTwo.getValue());
        }

    /**
    * Test object with nested references().
    */
    @Test
    public void testReferencesWithComplexObject()
            throws IOException
        {
        PortablePersonReference ivan  = new PortablePersonReference("Ivan", new Date(78, 4, 25));
        PortablePersonReference goran = new PortablePersonReference("Goran", new Date(82, 3, 3));
        PortablePersonReference anna  = new PortablePersonReference("Anna", new Date(80, 4, 12));
        PortablePerson          tom   = new PortablePerson("Tom", new Date(103, 7, 5));
        PortablePerson          ellen = new PortablePerson("Ellen", new Date(105, 3, 15));

        ivan.setChildren(null);
        goran.setChildren(new PortablePerson[2]);
        goran.getChildren()[0] = tom;
        goran.getChildren()[1] = ellen;
        anna.setChildren(new PortablePerson[2]);
        anna.getChildren()[0] = tom;
        anna.getChildren()[1] = ellen;
        ivan.setSiblings(new PortablePersonReference[1]);
        ivan.getSiblings()[0] = goran;
        goran.setSiblings(new PortablePersonReference[1]);
        goran.getSiblings()[0] = ivan;
        goran.setSpouse(anna);
        anna.setSpouse(goran);

        Map<CompositeKey, PortableObject> mapPerson = new HashMap<CompositeKey, PortableObject>();
        String                            lastName  = "Smith";
        CompositeKey                      key1      = new CompositeKey(lastName, "ivan"),
                                          key2      = new CompositeKey(lastName, "goran");
        mapPerson.put(key1, ivan);
        mapPerson.put(key2, goran);

        Binary   bin       = serialize(mapPerson, MODE_PLAIN, true);
        PofValue pv        = PofValueParser.parse(bin, getPofContext(true));
        Map      mapResult = pv.getMap(null);

        assertEquals(2, mapResult.size());
        PortablePersonReference ivanR  = (PortablePersonReference) mapResult.get(key1);
        PortablePersonReference goranR = (PortablePersonReference) mapResult.get(key2);
        assertEquals(goran.m_sName, goranR.m_sName);
        azzert(ivanR.getSiblings()[0] == goranR);
        azzert(goranR.getSpouse().getChildren()[0] == goranR.getChildren()[0]);

        bin = serialize(ivan, MODE_PLAIN, true);
        pv  = PofValueParser.parse(bin, getPofContext(true));

        ivanR = (PortablePersonReference) pv.getRoot().getValue();
        goranR = ivanR.getSiblings()[0];
        azzert(goranR.getSiblings()[0] == ivanR);

        ivanR = (PortablePersonReference) pv.getValue(PofConstants.T_UNKNOWN);
        goranR = ivanR.getSiblings()[0];
        azzert(goranR.getSiblings()[0] == ivanR);
        }
    }

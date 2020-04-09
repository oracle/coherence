/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator;

import com.tangosol.io.pof.EnumPofSerializer;
import com.tangosol.io.pof.PortableTypeSerializer;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.reflect.PofValue;
import com.tangosol.io.pof.reflect.PofValueParser;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import data.evolvable.Color;
import data.evolvable.DateTypes;

import java.lang.reflect.Constructor;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import static com.tangosol.io.pof.reflect.PofReflectionHelper.getPofNavigator;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author as  2012.05.27
 */
public class PortableTypeGeneratorTest
    {
    private SimplePofContext ctx;
    private ClassLoader loader = new PortableTypeLoader();
    private Constructor allTypesCtor;
    private Constructor dogCtor;
    private Constructor terrierCtor;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception
        {
        Class allTypes = loader.loadClass("data.evolvable.AllTypes");
        allTypesCtor = allTypes.getConstructor();

        Class dog = loader.loadClass("data.evolvable.v3.Dog");
        dogCtor = dog.getConstructor(String.class, Integer.TYPE, String.class, Color.class);

        Class terrier = loader.loadClass("data.evolvable.v3.Terrier");
        terrierCtor = terrier.getConstructor(String.class, Integer.TYPE, Color.class);

        ctx = new SimplePofContext();
        ctx.registerUserType(1, loader.loadClass("data.evolvable.v3.Pet"),
                             new PortableTypeSerializer(1, loader.loadClass("data.evolvable.v3.Pet")));
        ctx.registerUserType(2, dog, new PortableTypeSerializer(2, dog));
        ctx.registerUserType(200, terrier, new PortableTypeSerializer(200, terrier));
        ctx.registerUserType(3, loader.loadClass("data.evolvable.v3.Animal"),
                             new PortableTypeSerializer(3, loader.loadClass("data.evolvable.v3.Animal")));
        ctx.registerUserType(5, Color.class, new EnumPofSerializer());
        ctx.registerUserType(1000, allTypes, new PortableTypeSerializer(1000, allTypes));
        }

    @Test
    public void shouldBeEvolvableWithIgnoredFieldsRemoved() throws Exception
        {
        Class            clzDogV4   = loader.loadClass("data.evolvable.v4.Dog");
        SimplePofContext pofContext = new SimplePofContext();

        pofContext.registerUserType(2, clzDogV4, new PortableTypeSerializer(2, clzDogV4));
        pofContext.registerUserType(5, Color.class, new EnumPofSerializer());
        pofContext.registerUserType(1, loader.loadClass("data.evolvable.v3.Pet"),
                             new PortableTypeSerializer(1, loader.loadClass("data.evolvable.v3.Pet")));
        pofContext.registerUserType(3, loader.loadClass("data.evolvable.v3.Animal"),
                             new PortableTypeSerializer(3, loader.loadClass("data.evolvable.v3.Animal")));


        Object dogV3    = dogCtor.newInstance("Nadia", 10, "Boxer", Color.BRINDLE);
        Binary binDogV3 = ExternalizableHelper.toBinary(dogV3, ctx);
        Object dogV4    = ExternalizableHelper.fromBinary(binDogV3, pofContext);
        Binary binDogV4 = ExternalizableHelper.toBinary(dogV4, pofContext);
        Object dog      = ExternalizableHelper.fromBinary(binDogV4, ctx);

        assertThat(dog, is(dogV3));
        }

    @Test
    public void testAllTypesRoundTrip() throws Exception
        {
        DateTypes expected = (DateTypes) allTypesCtor.newInstance();
        Binary    binObj   = ExternalizableHelper.toBinary(expected, ctx);
        DateTypes actual   = ExternalizableHelper.fromBinary(binObj, ctx);

        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);

        assertEquals(expected, actual);
        assertDateEquals(expected.getDate(), actual.getDate());
        assertTimeEquals(expected.getTime(), actual.getTime());
        assertTimeEquals(expected.getTimeWithZone(), actual.getTimeWithZone());
        }

    @Test
    public void testPofNavigator() throws Exception
        {
        Object   dog = dogCtor.newInstance("Nadia", 10, "Boxer", Color.BRINDLE);
        Binary   binDog = ExternalizableHelper.toBinary(dog, ctx);
        PofValue pofDog = PofValueParser.parse(binDog, ctx);

        assertEquals("Nadia", getPofNavigator(dog.getClass(), "name")
                .navigate(pofDog).getString());
        assertEquals("Boxer", getPofNavigator(dog.getClass(), "breed")
                .navigate(pofDog).getString());
        assertEquals(10, getPofNavigator(dog.getClass(), "age")
                .navigate(pofDog).getInt());
        assertEquals(Color.BRINDLE, getPofNavigator(dog.getClass(), "color")
                .navigate(pofDog).getValue());
        }

//    @Test
//    public void testPofExtractor()
//    throws Exception
//        {
//        Object      dog      = dogCtor.newInstance("Nadia", 10, "Boxer", Color.BRINDLE);
//        Binary      binDog   = ExternalizableHelper.toBinary(dog, ctx);
//        BinaryEntry binEntry = new TestBinaryEntry(null, binDog, ctx);
//
//        assertEquals("Nadia", getPofExtractor(dog.getClass(), "name")
//                .extractFromEntry(binEntry));
//        assertEquals("Boxer", getPofExtractor(dog.getClass(), "breed")
//                .extractFromEntry(binEntry));
//        assertEquals(10, getPofExtractor(dog.getClass(), "age")
//                .extractFromEntry(binEntry));
//        assertEquals(Color.BRINDLE, getPofExtractor(dog.getClass(), "color")
//                .extractFromEntry(binEntry));
//        }

    @Test
    public void testEmptySubClass() throws Exception
        {
        Object terrier = terrierCtor.newInstance("Bullseye", 10, Color.WHITE);
        Binary binDog = ExternalizableHelper.toBinary(terrier, ctx);
        Object result = ExternalizableHelper.fromBinary(binDog, ctx);

        assertThat(result, is(notNullValue()));
        }

    private static void assertDateEquals(Date expected, Date actual)
        {
        assertEquals(expected.getYear(), actual.getYear());
        assertEquals(expected.getMonth(), actual.getMonth());
        assertEquals(expected.getDate(), actual.getDate());
        }

    private static void assertTimeEquals(Date expected, Date actual)
        {
        assertEquals(expected.getHours(), actual.getHours());
        assertEquals(expected.getMinutes(), actual.getMinutes());
        assertEquals(expected.getSeconds(), actual.getSeconds());
        assertEquals(expected.getTime() % 1000, actual.getTime() % 1000);
        assertEquals(expected.getTimezoneOffset(), actual.getTimezoneOffset());
        }
    }

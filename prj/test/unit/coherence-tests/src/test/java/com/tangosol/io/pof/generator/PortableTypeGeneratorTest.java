/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator;

import com.oracle.coherence.common.schema.Schema;

import com.tangosol.io.pof.EnumPofSerializer;
import com.tangosol.io.pof.EvolvableObject;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PortableTypeSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.io.pof.reflect.PofValue;
import com.tangosol.io.pof.reflect.PofValueParser;

import com.tangosol.io.pof.schema.annotation.internal.Instrumented;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.PofExtractorTest;

import data.evolvable.Color;
import data.evolvable.DateTypes;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.tangosol.io.pof.reflect.PofReflectionHelper.getPofNavigator;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author as  2012.05.27
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PortableTypeGeneratorTest
    {
    private static SimplePofContext ctxV3;
    private static SimplePofContext ctxV4;
    private static final ClassLoader loader = new PortableTypeLoader();

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void setup() throws Exception
        {
        Class<?> allTypes = loader.loadClass("data.evolvable.AllTypes");
        Class<?> dogV3    = loader.loadClass("data.evolvable.v3.Dog");
        Class<?> dogV4    = loader.loadClass("data.evolvable.v4.Dog");
        Class<?> terrier  = loader.loadClass("data.evolvable.v3.Terrier");

        ctxV3 = new SimplePofContext();
        ctxV3.registerUserType(1, loader.loadClass("data.evolvable.v3.Pet"),
                               new PortableTypeSerializer(1, loader.loadClass("data.evolvable.v3.Pet")));
        ctxV3.registerUserType(2, dogV3, new PortableTypeSerializer(2, dogV3));
        ctxV3.registerUserType(200, terrier, new PortableTypeSerializer(200, terrier));
        ctxV3.registerUserType(3, loader.loadClass("data.evolvable.v3.Animal"),
                               new PortableTypeSerializer(3, loader.loadClass("data.evolvable.v3.Animal")));
        ctxV3.registerUserType(5, Color.class, new EnumPofSerializer<>());
        ctxV3.registerUserType(10, loader.loadClass("data.evolvable.v3.Address"),
                               new PortableTypeSerializer(10, loader.loadClass("data.evolvable.v3.Address")));
        ctxV3.registerUserType(1000, allTypes, new PortableTypeSerializer(1000, allTypes));

        ctxV4 = new SimplePofContext();
        ctxV4.registerUserType(1, loader.loadClass("data.evolvable.v3.Pet"),
                               new PortableTypeSerializer(1, loader.loadClass("data.evolvable.v3.Pet")));
        ctxV4.registerUserType(2, dogV4, new PortableTypeSerializer(2, dogV4));
        ctxV4.registerUserType(200, terrier, new PortableTypeSerializer(200, terrier));
        ctxV4.registerUserType(3, loader.loadClass("data.evolvable.v3.Animal"),
                               new PortableTypeSerializer(3, loader.loadClass("data.evolvable.v3.Animal")));
        ctxV4.registerUserType(5, Color.class, new EnumPofSerializer<>());
        ctxV4.registerUserType(10, loader.loadClass("data.evolvable.v4.Address"),
                               new PortableTypeSerializer(10, loader.loadClass("data.evolvable.v4.Address")));
        ctxV4.registerUserType(1000, allTypes, new PortableTypeSerializer(1000, allTypes));
        }

    @Test
    public void testRecordRoundTripV3() throws Exception
        {
        Class<?> clz = ctxV3.getClass(10);
        Object address = clz.getDeclaredConstructor(String.class, String.class, String.class).newInstance("100 Main St", "Tampa", "FL");
        Binary binAddress = ExternalizableHelper.toBinary(address, ctxV3);
        assertEquals(address, ExternalizableHelper.fromBinary(binAddress, ctxV3));
        }

    @Test
    public void testRecordRoundTripV4() throws Exception
        {
        Class<?> clz = ctxV4.getClass(10);
        Object address = clz.getDeclaredConstructor(String.class, String.class, String.class, Integer.TYPE, String.class)
                .newInstance("100 Main St", "Tampa", "FL", 33559, "USA");
        Binary binAddress = ExternalizableHelper.toBinary(address, ctxV4);
        assertEquals(address, ExternalizableHelper.fromBinary(binAddress, ctxV4));
        }

    @Test
    public void testRecordEvolution() throws Exception
        {
        Class<?> clz = ctxV4.getClass(10);
        Object addressV4 = clz.getDeclaredConstructor(String.class, String.class, String.class, Integer.TYPE, String.class)
                .newInstance("100 Main St", "Tampa", "FL", 33559, "USA");
        Binary binAddressV4 = ExternalizableHelper.toBinary(addressV4, ctxV4);
        assertEquals(addressV4, ExternalizableHelper.fromBinary(binAddressV4, ctxV4));

        Object addressV3 = ExternalizableHelper.fromBinary(binAddressV4, ctxV3);
        Binary binAddressV3 = ExternalizableHelper.toBinary(addressV3, ctxV3);

        Object address = ExternalizableHelper.fromBinary(binAddressV3, ctxV4);
        assertEquals(addressV4, address);
        }
    
    @Test
    public void shouldBeEvolvableWithIgnoredFieldsRemoved() throws Exception
        {
        Object dogV3    = ctxV3.getClass(2)
                .getConstructor(String.class, Integer.TYPE, String.class, Color.class)
                .newInstance("Nadia", 10, "Boxer", Color.BRINDLE);
        Binary binDogV3 = ExternalizableHelper.toBinary(dogV3, ctxV3);
        Object dogV4    = ExternalizableHelper.fromBinary(binDogV3, ctxV4);
        Binary binDogV4 = ExternalizableHelper.toBinary(dogV4, ctxV4);
        Object dog      = ExternalizableHelper.fromBinary(binDogV4, ctxV3);

        assertThat(dog, is(dogV3));
        }

    @Test
    public void testAllTypesRoundTrip() throws Exception
        {
        DateTypes expected = (DateTypes) ctxV3.getClass(1000).getConstructor().newInstance();
        Binary    binObj   = ExternalizableHelper.toBinary(expected, ctxV3);
        DateTypes actual   = ExternalizableHelper.fromBinary(binObj, ctxV3);

        assertEquals(expected, actual);
        assertDateEquals(expected.getDate(), actual.getDate());
        assertTimeEquals(expected.getTime(), actual.getTime());
        assertTimeEquals(expected.getTimeWithZone(), actual.getTimeWithZone());
        }

    @Test
    public void testPofNavigator() throws Exception
        {
        Object   dog    = ctxV3.getClass(2)
                .getConstructor(String.class, Integer.TYPE, String.class, Color.class)
                .newInstance("Nadia", 10, "Boxer", Color.BRINDLE);
        Binary   binDog = ExternalizableHelper.toBinary(dog, ctxV3);
        PofValue pofDog = PofValueParser.parse(binDog, ctxV3);

        assertEquals("Nadia", getPofNavigator(dog.getClass(), "name")
                .navigate(pofDog).getString());
        assertEquals("Boxer", getPofNavigator(dog.getClass(), "breed")
                .navigate(pofDog).getString());
        assertEquals(10, getPofNavigator(dog.getClass(), "age")
                .navigate(pofDog).getInt());
        assertEquals(Color.BRINDLE, getPofNavigator(dog.getClass(), "color")
                .navigate(pofDog).getValue());
        }

    @Test
    public void testPofExtractor() throws Exception
        {
        Object      dog      = ctxV3.getClass(2)
                .getConstructor(String.class, Integer.TYPE, String.class, Color.class)
                .newInstance("Nadia", 10, "Boxer", Color.BRINDLE);
        Binary      binDog   = ExternalizableHelper.toBinary(dog, ctxV3);
        BinaryEntry binEntry = new PofExtractorTest.TestBinaryEntry(null, binDog, ctxV3);

        assertEquals("Nadia", getPofExtractor(dog.getClass(), "name")
                .extractFromEntry(binEntry));
        assertEquals("Boxer", getPofExtractor(dog.getClass(), "breed")
                .extractFromEntry(binEntry));
        assertEquals(10, getPofExtractor(dog.getClass(), "age")
                .extractFromEntry(binEntry));
        assertEquals(Color.BRINDLE, getPofExtractor(dog.getClass(), "color")
                .extractFromEntry(binEntry));
        }

    private <T> PofExtractor<T, ?> getPofExtractor(Class<? extends T> aClass, String sPropertyName)
        {
        return (PofExtractor<T, ?>) Extractors.fromPof(aClass, sPropertyName);
        }

    @Test
    public void testEmptySubClass() throws Exception
        {
        Object terrier = ctxV3.getClass(200)
                .getConstructor(String.class, Integer.TYPE, Color.class)
                .newInstance("Bullseye", 10, Color.WHITE);
        Binary binDog  = ExternalizableHelper.toBinary(terrier, ctxV3);
        Object result  = ExternalizableHelper.fromBinary(binDog, ctxV3);

        assertThat(result, is(notNullValue()));
        }

    @SuppressWarnings("deprecation")
    private static void assertDateEquals(Date expected, Date actual)
        {
        assertEquals(expected.getYear(), actual.getYear());
        assertEquals(expected.getMonth(), actual.getMonth());
        assertEquals(expected.getDate(), actual.getDate());
        }

    @SuppressWarnings("deprecation")
    private static void assertTimeEquals(Date expected, Date actual)
        {
        assertEquals(expected.getHours(), actual.getHours());
        assertEquals(expected.getMinutes(), actual.getMinutes());
        assertEquals(expected.getSeconds(), actual.getSeconds());
        assertEquals(expected.getTime() % 1000, actual.getTime() % 1000);
        assertEquals(expected.getTimezoneOffset(), actual.getTimezoneOffset());
        }

    @Test
    public void shouldCreateSchemaForPackagedClass() throws Exception
        {
        String         sClassName   = PackagedType.class.getName();
        URL            url          = getClass().getResource("/" + sClassName.replaceAll("\\.", "/") + ".class");
        File           fileClass    = new File(url.toURI());
        Map<String, ?> env          = new HashMap<>();

        Schema schema = PortableTypeGenerator.createSchema(fileClass, env);
        assertThat(schema, is(notNullValue()));
        }

    @Test
    public void shouldInstrumentPackagedClass() throws Exception
        {
        String         sClassName   = PackagedType.class.getName();
        URL            url          = getClass().getResource("/" + sClassName.replaceAll("\\.", "/") + ".class");
        File           fileClass    = new File(url.toURI());
        byte[]         abBytes      = Files.readAllBytes(fileClass.toPath());
        Properties     properties   = new Properties();
        Map<String, ?> env          = new HashMap<>();
        byte[]         instrumented = PortableTypeGenerator.instrumentClass(fileClass, abBytes, 0, abBytes.length, properties, env);

        assertThat(instrumented, is(notNullValue()));

        ByteArrayClassLoader loader = new ByteArrayClassLoader(Collections.singletonMap(sClassName, instrumented));
        Class<?> instrumentedClass = loader.findClass(PackagedType.class.getName());

        assertThat(instrumentedClass.isAnnotationPresent(Instrumented.class), is(true));
        assertThat(PortableObject.class.isAssignableFrom(instrumentedClass), is(true));
        assertThat(EvolvableObject.class.isAssignableFrom(instrumentedClass), is(true));
        }

    @Test
    public void shouldCreateSchemaForPackagelessClass() throws Exception
        {
        String         sClassName   = "NonPackagedType";
        URL            url          = getClass().getResource("/" + sClassName + ".class");
        File           fileClass    = new File(url.toURI());
        Map<String, ?> env          = new HashMap<>();

        Schema schema = PortableTypeGenerator.createSchema(fileClass, env);
        assertThat(schema, is(notNullValue()));
        }

    @Test
    public void shouldInstrumentPackagelessClass() throws Exception
        {
        String         sClassName   = "NonPackagedType";
        URL            url          = getClass().getResource("/" + sClassName + ".class");
        File           fileClass    = new File(url.toURI());
        byte[]         abBytes      = Files.readAllBytes(fileClass.toPath());
        Properties     properties   = new Properties();
        Map<String, ?> env          = new HashMap<>();
        byte[]         instrumented = PortableTypeGenerator.instrumentClass(fileClass, abBytes, 0, abBytes.length, properties, env);

        assertThat(instrumented, is(notNullValue()));

        ByteArrayClassLoader loader = new ByteArrayClassLoader(Collections.singletonMap(sClassName, instrumented));
        Class<?> instrumentedClass = loader.findClass(sClassName);

        assertThat(instrumentedClass.isAnnotationPresent(Instrumented.class), is(true));
        assertThat(PortableObject.class.isAssignableFrom(instrumentedClass), is(true));
        assertThat(EvolvableObject.class.isAssignableFrom(instrumentedClass), is(true));
        }

    // ----- inner class: ByteArrayClassLoader ------------------------------

    public static class ByteArrayClassLoader
            extends URLClassLoader
        {
        public ByteArrayClassLoader(Map<String, byte[]> mapClasses)
            {
            super(new URL[0], Base.getContextClassLoader());
            m_mapClasses = mapClasses;
            }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
            {
            byte[] classBytes = m_mapClasses.get(name);
            if (classBytes != null)
                {
                return defineClass(name, classBytes, 0, classBytes.length);
                }
            return super.findClass(name);
            }

        private final Map<String, byte[]> m_mapClasses;
        }
    }

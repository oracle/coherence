/*
 * Copyright (c) 2012, 2023, Oracle and/or its affiliates.
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
import com.tangosol.io.pof.generator.data.TestClassWithNoId;
import com.tangosol.io.pof.reflect.PofValue;
import com.tangosol.io.pof.reflect.PofValueParser;

import com.tangosol.io.pof.schema.annotation.internal.Instrumented;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import data.evolvable.Color;
import data.evolvable.DateTypes;

import java.io.File;
import java.lang.reflect.Constructor;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.tangosol.io.pof.generator.data.Simple;
import org.hamcrest.MatcherAssert;
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

    @Test
    public void shouldCreateSchemaForPackagedClass() throws Exception
        {
        String         sClassName   = PackagedType.class.getName();
        URL url          = getClass().getResource("/" + sClassName.replaceAll("\\.", "/") + ".class");
        File fileClass    = new File(url.toURI());
        Map<String, ?> env           = new HashMap<>();

        Schema schema = PortableTypeGenerator.createSchema(fileClass, env);
        MatcherAssert.assertThat(schema, is(notNullValue()));
        }

    @Test
    public void shouldInstrumentPackagedClass() throws Exception
        {
        String         sClassName   = PackagedType.class.getName();
        URL            url          = getClass().getResource("/" + sClassName.replaceAll("\\.", "/") + ".class");
        File           fileClass    = new File(url.toURI());
        byte[]         abBytes      = Files.readAllBytes(fileClass.toPath());
        Properties properties   = new Properties();
        Map<String, ?> env          = new HashMap<>();
        byte[]         instrumented = PortableTypeGenerator.instrumentClass(fileClass, abBytes, 0, abBytes.length, properties, env);

        MatcherAssert.assertThat(instrumented, is(notNullValue()));

        ByteArrayClassLoader loader = new ByteArrayClassLoader(Collections.singletonMap(sClassName, instrumented));
        Class<?> instrumentedClass = loader.findClass(PackagedType.class.getName());

        MatcherAssert.assertThat(instrumentedClass.isAnnotationPresent(Instrumented.class), is(true));
        MatcherAssert.assertThat(PortableObject.class.isAssignableFrom(instrumentedClass), is(true));
        MatcherAssert.assertThat(EvolvableObject.class.isAssignableFrom(instrumentedClass), is(true));
        }

    @Test
    public void shouldCreateSchemaForPackagelessClass() throws Exception
        {
        String         sClassName   = "NonPackagedType";
        URL            url          = getClass().getResource("/" + sClassName + ".class");
        File           fileClass    = new File(url.toURI());
        Map<String, ?> env           = new HashMap<>();

        Schema schema = PortableTypeGenerator.createSchema(fileClass, env);
        MatcherAssert.assertThat(schema, is(notNullValue()));
        }

    @Test
    @SuppressWarnings("rawtypes")
    public void shouldInstrumentClassWithNoId() throws Exception
        {
        String         sClassName   = TestClassWithNoId.class.getName();
        URL            url          = getClass().getResource("/" + sClassName.replaceAll("\\.", "/") + ".class");
        File           fileClass    = new File(url.toURI());
        byte[]         abBytes      = Files.readAllBytes(fileClass.toPath());
        Properties     properties   = new Properties();
        Map<String, ?> env          = new HashMap<>();
        byte[]         instrumented = PortableTypeGenerator.instrumentClass(fileClass, abBytes, 0, abBytes.length, properties, env);

        SimplePofContext ctx = new SimplePofContext();

        ByteArrayClassLoader loader = new ByteArrayClassLoader(Collections.singletonMap(sClassName, instrumented));
        Class<?> instrumentedClass = loader.findClass(TestClassWithNoId.class.getName());

        ctx.registerUserType(1, instrumentedClass, new PortableTypeSerializer(1, instrumentedClass));

        Object testClass = instrumentedClass.getDeclaredConstructor(String.class).newInstance("value");

        Binary            binTestClass  = ExternalizableHelper.toBinary(testClass, ctx);
        Object            result        = ExternalizableHelper.fromBinary(binTestClass, ctx);
        MatcherAssert.assertThat(result.equals(testClass), is(true));
        }

    @Test
    @SuppressWarnings("rawtypes")
    public void testBug35038656() throws Exception
        {
        String         sClassName   = Simple.class.getName();
        URL            url          = getClass().getResource("/" + sClassName.replaceAll("\\.", "/") + ".class");
        File           fileClass    = new File(url.toURI());
        byte[]         abBytes      = Files.readAllBytes(fileClass.toPath());
        Properties     properties   = new Properties();
        Map<String, ?> env          = new HashMap<>();
        byte[]         instrumented = PortableTypeGenerator.instrumentClass(fileClass, abBytes, 0, abBytes.length, properties, env);

        SimplePofContext ctx = new SimplePofContext();

        ByteArrayClassLoader loader = new ByteArrayClassLoader(Collections.singletonMap(sClassName, instrumented));
        Class<?> instrumentedClass = loader.findClass(sClassName);

        ctx.registerUserType(1, instrumentedClass, new PortableTypeSerializer(1, instrumentedClass));

        Object testClass = instrumentedClass.getDeclaredConstructor(String.class, Integer.TYPE).newInstance("name", 10);
        Binary            binTestClass  = ExternalizableHelper.toBinary(testClass, ctx);
        Object            result        = ExternalizableHelper.fromBinary(binTestClass, ctx);
        MatcherAssert.assertThat(result.equals(testClass), is(true));
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

        MatcherAssert.assertThat(instrumented, is(notNullValue()));

        ByteArrayClassLoader loader = new ByteArrayClassLoader(Collections.singletonMap(sClassName, instrumented));
        Class<?> instrumentedClass = loader.findClass(sClassName);

        MatcherAssert.assertThat(instrumentedClass.isAnnotationPresent(Instrumented.class), is(true));
        MatcherAssert.assertThat(PortableObject.class.isAssignableFrom(instrumentedClass), is(true));
        MatcherAssert.assertThat(EvolvableObject.class.isAssignableFrom(instrumentedClass), is(true));
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

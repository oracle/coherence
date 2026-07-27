/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.rest.io.JacksonJsonMarshaller;
import com.tangosol.coherence.rest.io.JaxbXmlMarshaller;
import com.tangosol.io.SerializationGeneratedClasses;
import com.tangosol.io.internal.SerializationAllowlist;

import data.pof.Person;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import org.objectweb.asm.util.TraceClassVisitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;


/**
 * @author as  2011.06.29
 */
public class PartialObjectTest
    {
    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        }

    @Test
    public void testClassCreation()
            throws Exception
        {
        PropertySet ps = com.tangosol.coherence.rest.util.PropertySet.fromString("name");
        Class clz = PartialObject.createPartialClass(Person.class, ps);
        assertNotNull(clz.getMethod("getName"));
        }

    @Test
    public void testPropertyAccess()
            throws Exception
        {
        PropertySet ps  = PropertySet.fromString("name,age");
        Class       clz = PartialObject.createPartialClass(Person.class, ps);

        Method      getName = clz.getMethod("getName");
        Method      getAge  = clz.getMethod("getAge");
        Constructor ctor    = clz.getConstructor(Map.class);
        assertNotNull(getName);
        assertNotNull(getAge);
        assertNotNull(ctor);

        Map props = new HashMap();
        props.put("name", "Aleks");
        props.put("age", 36);

        Object o = ctor.newInstance(props);
        assertEquals("Aleks", getName.invoke(o));
        }

    @Test
    public void testJsonSerialization()
            throws Exception
        {
        Person      p  = Person.create();
        PropertySet ps = PropertySet.fromString("name,age,address:(city,state),spouse:(name),children:(name),childrenList:(name)");
        Object      o  = PartialObject.create(p, ps);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JacksonJsonMarshaller(o.getClass()).marshal(o, out, null);

        assertEquals("{\"address\":{\"city\":\"Tampa\",\"state\":\"FL\"},\"age\":36,"
                + "\"children\":[{\"name\":\"Ana Maria Seovic\"},{\"name\":\"Novak Seovic\"}],"
                + "\"name\":\"Aleksandar Seovic\",\"spouse\":{\"name\":\"Marija Seovic\"}}",
                out.toString());
        }

    @Test
    public void testXmlSerialization()
            throws Exception
        {
        Person      p  = Person.create();
        PropertySet ps = PropertySet.fromString("name,age,address:(city,state),spouse:(name),children:(name),childrenList:(name)");
        Object      o  = PartialObject.create(p, ps);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JaxbXmlMarshaller(o.getClass()).marshal(o, out, null);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<person age=\"36\"><address><city>Tampa</city><state>FL</state>"
                + "</address><children><child><name>Ana Maria Seovic</name></child>"
                + "<child><name>Novak Seovic</name></child></children>"
                + "<name>Aleksandar Seovic</name><spouse><name>Marija Seovic</name>"
                + "</spouse></person>",
                out.toString());
        }

    @Test
    public void testGeneratedPartialClassIsSerializationAllowedInHardenedModes()
        {
        Object oPartial = PartialObject.create(Person.create(), PropertySet.fromString("name,age"));

        for (String sMode : new String[] {"dev", "prod"})
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.mode(sMode))
                {
                Class<?> clzPartial = oPartial.getClass();

                assertTrue(clzPartial.getName(), SerializationAllowlist.isAllowlisted(clzPartial));
                assertTrue(clzPartial.getName(), SerializationAllowlist.isAllowed(clzPartial));
                }
            }
        }

    @Test
    public void testGeneratedPartialPredicateDoesNotAllowNormalRestClasses()
        {
        for (String sMode : new String[] {"dev", "prod"})
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.mode(sMode))
                {
                assertFalse(PartialObject.class.getName(),
                        SerializationAllowlist.isAllowlisted(PartialObject.class));
                assertFalse(PartialObject.class.getName(),
                        SerializationAllowlist.isAllowed(PartialObject.class));
                assertFalse(RestHelper.class.getName(),
                        SerializationAllowlist.isAllowlisted(RestHelper.class));
                assertFalse(RestHelper.class.getName(),
                        SerializationAllowlist.isAllowed(RestHelper.class));
                assertFalse(TestPartialObject.class.getName(),
                        SerializationAllowlist.isAllowlisted(TestPartialObject.class));
                assertFalse(TestPartialObject.class.getName(),
                        SerializationAllowlist.isAllowed(TestPartialObject.class));
                }
            }
        }

    @Test
    public void testGeneratedPartialRegistrarRejectsNonRestCaller()
        {
        assertRegistrationRejected(() -> SerializationGeneratedClasses.registerRestGeneratedPartialClass(
                MethodHandles.lookup(), java.io.File.class));

        for (String sMode : new String[] {"dev", "prod"})
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.mode(sMode))
                {
                assertFalse(SerializationAllowlist.isAllowlisted(java.io.File.class));
                assertFalse(SerializationAllowlist.isAllowed(java.io.File.class));
                }
            }
        }

    @Test
    public void testGeneratedPartialRegistrarRejectsDirectInternalBypass()
            throws Exception
        {
        Class<?> clzPartial = PartialObject.create(Person.create(), PropertySet.fromString("name,age")).getClass();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(PartialObject.class, MethodHandles.lookup());

        assertRegistrationRejected(() -> SerializationAllowlist.registerRestGeneratedPartialClass(
                lookup, clzPartial));
        }

    @Test
    public void testGeneratedPartialRegistrarAllowsReflectiveRestCaller()
            throws Exception
        {
        Class<?> clzPartial = PartialObject.create(Person.create(), PropertySet.fromString("name,age")).getClass();
        Method   method     = PartialObject.class.getDeclaredMethod("registerPartialClass", Class.class);

        method.setAccessible(true);
        method.invoke(null, clzPartial);

        for (String sMode : new String[] {"dev", "prod"})
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.mode(sMode))
                {
                assertTrue(clzPartial.getName(), SerializationAllowlist.isAllowed(clzPartial));
                }
            }
        }

    @Test
    public void testGeneratedPartialRegistrarRejectsNonJdkBridgeFrame()
            throws Exception
        {
        Class<?> clzPartial = PartialObject.create(Person.create(), PropertySet.fromString("name,age")).getClass();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(PartialObject.class, MethodHandles.lookup());

        assertEquals(PartialObject.class, lookup.lookupClass());
        assertRegistrationRejected(() -> BridgeRegistrar.register(lookup, clzPartial));
        }

    @Test
    public void testShadowPartialObjectCannotRegisterGeneratedPartial()
            throws Exception
        {
        ShadowPartialFixture fixture = ShadowPartialFixture.create();

        try
            {
            fixture.register();
            fail("shadow PartialObject registration should be rejected");
            }
        catch (InvocationTargetException e)
            {
            assertTrue(e.getCause() instanceof SecurityException);
            }

        for (String sMode : new String[] {"dev", "prod"})
            {
            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.mode(sMode))
                {
                assertFalse(fixture.generatedClass().getName(),
                        SerializationAllowlist.isAllowlisted(fixture.generatedClass()));
                assertFalse(fixture.generatedClass().getName(),
                        SerializationAllowlist.isAllowed(fixture.generatedClass()));
                }
            }
        }

    @Test
    public void testConcurrentPartialRegistrationAndAllowlistChecks()
            throws Exception
        {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch  start    = new CountDownLatch(1);
        List<Future<Void>> listFuture = new ArrayList<>();
        String[] asPropertySets = {"name", "age", "name,age"};

        try
            {
            for (int i = 0; i < 8; i++)
                {
                final int nTask = i;
                listFuture.add(executor.submit((Callable<Void>) () ->
                    {
                    start.await();
                    for (int j = 0; j < 20; j++)
                        {
                        Object oPartial = PartialObject.create(Person.create(),
                                PropertySet.fromString(asPropertySets[(nTask + j) % asPropertySets.length]));
                        for (String sMode : new String[] {"dev", "prod"})
                            {
                            try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.mode(sMode))
                                {
                                assertTrue(oPartial.getClass().getName(),
                                        SerializationAllowlist.isAllowed(oPartial.getClass()));
                                assertFalse(SerializationAllowlist.isAllowlisted(java.io.File.class));
                                }
                            }
                        }
                    return null;
                    }));
                }

            start.countDown();
            for (Future<Void> future : listFuture)
                {
                future.get();
                }
            }
        finally
            {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
            }
        }

    @Test
    public void testPrimitives() throws Exception
        {
        PropertySet pSet = PropertySet.fromString("booleanProp,byteProp,shortProp,intProp,longProp,floatProp,doubleProp,charProp");
        Primitives  p    = new Primitives(true, Byte.MAX_VALUE, Short.MIN_VALUE, 1234, 567890L, 1234.0f, 567890.0d, 'A');

        Object        po        = PartialObject.create(p, pSet);
        Class<?>      clz       = po.getClass();
        Method        isBoolean = clz.getMethod("isBooleanProp");
        Method        getByte   = clz.getMethod("getByteProp");
        Method        getShort  = clz.getMethod("getShortProp");
        Method        getInt    = clz.getMethod("getIntProp");
        Method        getLong   = clz.getMethod("getLongProp");
        Method        getFloat  = clz.getMethod("getFloatProp");
        Method        getDouble = clz.getMethod("getDoubleProp");
        Method        getChar   = clz.getMethod("getCharProp");

        assertTrue((boolean) isBoolean.invoke(po));
        assertEquals(Byte.MAX_VALUE, (byte) getByte.invoke(po));
        assertEquals(Short.MIN_VALUE, (short) getShort.invoke(po));
        assertEquals(1234, (int) getInt.invoke(po));
        assertEquals(567890L, (long) getLong.invoke(po));
        assertEquals(1234.0f, (float) getFloat.invoke(po), 0.01);
        assertEquals(567890.0d, (double) getDouble.invoke(po), 0.01);
        assertEquals('A', (char) getChar.invoke(po));
        }

    //@Test
    public void dump()
            throws Exception
        {
        ClassReader cr = new ClassReader(TestPartialObject.class.getName());
        cr.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
        }

    private static void assertRegistrationRejected(Runnable runnable)
        {
        try
            {
            runnable.run();
            fail("registration should be rejected");
            }
        catch (SecurityException expected)
            {
            }
        }

    private static byte[] partialObjectBytes()
        {
        ClassWriter cw = classWriter(SHADOW_PARTIAL_OBJECT, "java/lang/Object");
        createDefaultConstructor(cw, "java/lang/Object");

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "register",
                "(Ljava/lang/Class;)V", null, null);
        mv.visitCode();
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup",
                "()Ljava/lang/invoke/MethodHandles$Lookup;", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "com/tangosol/io/SerializationGeneratedClasses",
                "registerRestGeneratedPartialClass",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/Class;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
        }

    private static byte[] partialClassLoaderBytes()
        {
        ClassWriter cw = classWriter(SHADOW_PARTIAL_CLASS_LOADER, "java/lang/ClassLoader");
        cw.visitInnerClass(SHADOW_PARTIAL_CLASS_LOADER, SHADOW_PARTIAL_OBJECT, "PartialClassLoader",
                ACC_PUBLIC | ACC_STATIC);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/ClassLoader;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "<init>",
                "(Ljava/lang/ClassLoader;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "defineShadow",
                "(Ljava/lang/String;[B)Ljava/lang/Class;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "defineClass",
                "(Ljava/lang/String;[BII)Ljava/lang/Class;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(5, 3);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
        }

    private static byte[] generatedPartialBytes()
        {
        ClassWriter cw = classWriter(SHADOW_GENERATED_PARTIAL, SHADOW_PARTIAL_OBJECT);
        createDefaultConstructor(cw, SHADOW_PARTIAL_OBJECT);
        cw.visitEnd();
        return cw.toByteArray();
        }

    private static ClassWriter classWriter(String sName, String sSuper)
        {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_8, ACC_PUBLIC, sName, null, sSuper, null);
        return cw;
        }

    private static void createDefaultConstructor(ClassWriter cw, String sSuper)
        {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, sSuper, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        }

    // ----- inner class: Primitives ----------------------------------------

    public static class Primitives
        {
        public Primitives(boolean aBoolean, byte aByte, short aShort, int anInt, long aLong, float aFloat, double aDouble, char aChar)
            {
            m_boolean = aBoolean;
            m_byte = aByte;
            m_short = aShort;
            m_int = anInt;
            m_long = aLong;
            m_float = aFloat;
            m_double = aDouble;
            m_char = aChar;
            }

        public boolean isBooleanProp()
            {
            return m_boolean;
            }

        public byte getByteProp()
            {
            return m_byte;
            }

        public short getShortProp()
            {
            return m_short;
            }

        public int getIntProp()
            {
            return m_int;
            }

        public long getLongProp()
            {
            return m_long;
            }

        public float getFloatProp()
            {
            return m_float;
            }

        public double getDoubleProp()
            {
            return m_double;
            }

        public char getCharProp()
            {
            return m_char;
            }

        private boolean m_boolean;
        private byte m_byte;
        private short m_short;
        private int m_int;
        private long m_long;
        private float m_float;
        private double m_double;
        private char m_char;
        }


    private static class BridgeRegistrar
        {
        static void register(MethodHandles.Lookup lookup, Class<?> clz)
            {
            SerializationGeneratedClasses.registerRestGeneratedPartialClass(lookup, clz);
            }
        }


    private static class ShadowPartialFixture
        {
        private ShadowPartialFixture(Class<?> clzPartialObject, Class<?> clzGenerated)
            {
            f_clzPartialObject = clzPartialObject;
            f_clzGenerated     = clzGenerated;
            }

        static ShadowPartialFixture create()
                throws Exception
            {
            DefiningLoader loaderRoot = new DefiningLoader(PartialObjectTest.class.getClassLoader());
            Class<?> clzPartialObject = loaderRoot.define(SHADOW_PARTIAL_OBJECT_DOT, partialObjectBytes());
            Class<?> clzPartialLoader = loaderRoot.define(SHADOW_PARTIAL_CLASS_LOADER_DOT, partialClassLoaderBytes());
            ClassLoader loaderPartial = (ClassLoader) clzPartialLoader.getConstructor(ClassLoader.class)
                    .newInstance(loaderRoot);
            Class<?> clzGenerated = (Class<?>) clzPartialLoader.getMethod("defineShadow", String.class, byte[].class)
                    .invoke(loaderPartial, SHADOW_GENERATED_PARTIAL_DOT, generatedPartialBytes());
            return new ShadowPartialFixture(clzPartialObject, clzGenerated);
            }

        void register()
                throws Exception
            {
            f_clzPartialObject.getMethod("register", Class.class).invoke(null, f_clzGenerated);
            }

        Class<?> generatedClass()
            {
            return f_clzGenerated;
            }

        private final Class<?> f_clzPartialObject;
        private final Class<?> f_clzGenerated;
        }


    private static class DefiningLoader
            extends ClassLoader
        {
        private DefiningLoader(ClassLoader loaderParent)
            {
            super(loaderParent);
            }

        Class<?> define(String sName, byte[] abClass)
            {
            return defineClass(sName, abClass, 0, abClass.length);
            }

        @Override
        protected Class<?> loadClass(String sName, boolean fResolve)
                throws ClassNotFoundException
            {
            if (sName.startsWith("com.tangosol.coherence.rest.util.PartialObject"))
                {
                Class<?> clz = findLoadedClass(sName);
                if (clz != null)
                    {
                    if (fResolve)
                        {
                        resolveClass(clz);
                        }
                    return clz;
                    }
                }
            return super.loadClass(sName, fResolve);
            }
        }


    private static final String SHADOW_PARTIAL_OBJECT =
            "com/tangosol/coherence/rest/util/PartialObject";

    private static final String SHADOW_PARTIAL_OBJECT_DOT =
            "com.tangosol.coherence.rest.util.PartialObject";

    private static final String SHADOW_PARTIAL_CLASS_LOADER =
            "com/tangosol/coherence/rest/util/PartialObject$PartialClassLoader";

    private static final String SHADOW_PARTIAL_CLASS_LOADER_DOT =
            "com.tangosol.coherence.rest.util.PartialObject$PartialClassLoader";

    private static final String SHADOW_GENERATED_PARTIAL =
            "com/tangosol/coherence/rest/util/gen/partial/Shadow_123";

    private static final String SHADOW_GENERATED_PARTIAL_DOT =
            "com.tangosol.coherence.rest.util.gen.partial.Shadow_123";

    // ----- inner class: TestPartialObject ---------------------------------

    public static class TestPartialObject
            extends PartialObject
        {
        public TestPartialObject()
            {
            }

        public TestPartialObject(Map mapProperties)
            {
            super(mapProperties);
            }

        public String getString()
            {
            return (String) get("string");
            }

        public int getInt()
            {
            return (Integer) get("int");
            }

        public boolean getBoolean()
            {
            return (Boolean) get("boolean");
            }

        public double getDouble()
            {
            return (Double) get("double");
            }

        public int[] getIntArray()
            {
            return (int[]) get("intArray");
            }

        public void setString(String value)
            {
            }

        public void setInt(int value)
            {
            }

        public void setIntArray(int[] value)
            {
            }
        }
    }

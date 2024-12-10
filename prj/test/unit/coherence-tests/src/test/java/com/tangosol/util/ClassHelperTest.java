/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.hasEntry;

import static org.hamcrest.core.AllOf.allOf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;


/**
* Unit test class for the {@link com.tangosol.util.ClassHelper} class.
*
* @author gg
*/
public class ClassHelperTest
        extends Base
    {
    /**
    * Test accessible class methods.
    */
    @Test
    public void testInvoke()
            throws Exception
        {
        Object o;

        o = Factory.newInstance(0);
        ClassHelper.invoke(o, "method0", ClassHelper.VOID);
        ClassHelper.invoke(o, "method1", new Object[] {"p1"});
        ClassHelper.invoke(o, "method1", new Object[] {"p1", new HashMap()});

        o = Factory.newInstance(1);
        ClassHelper.invoke(o, "method0", ClassHelper.VOID);
        ClassHelper.invoke(o, "method1", new Object[] {"p1"});
        ClassHelper.invoke(o, "method1", new Object[] {"p1", new HashMap()});
        ClassHelper.invoke(o, "run", ClassHelper.VOID);

        o = Factory.newInstance(2);
        ClassHelper.invoke(o, "method0", ClassHelper.VOID);
        ClassHelper.invoke(o, "method1", new Object[] {"p1"});
        ClassHelper.invoke(o, "method1", new Object[] {"p1", new HashMap()});
        ClassHelper.invoke(o, "run", ClassHelper.VOID);
        }

    /**
    * Test non-existing class method.
    */
    @Test
    public void testInvokeNonExisting()
            throws Exception
        {
        Object o = Factory.newInstance(0);
        try
            {
            ClassHelper.invoke(o, "nonExisting", ClassHelper.VOID);
            fail("expected exception");
            }
        catch (NoSuchMethodException e)
            {
            // expected
            }
        }

    /**
    * Test non-accesible class method.
    */
    @Test
    public void testInvokeNonAccessible()
            throws Exception
        {
        Object o = Factory.newInstance(1);
        try
            {
            ClassHelper.invoke(o, "methodInaccessible", ClassHelper.VOID);
            fail("expected exception");
            }
        catch (NoSuchMethodException e)
            {
            // expected
            }
        }

    /**
    * Test that findMethod() respects the fStatic flag (COH-3732).
    */
    @Test
    public void testCoh3732()
        {
        Method method;

        method = ClassHelper.findMethod(
            PublicInner0.class, "method0", ClassHelper.VOID_PARAMS, /*fStatic*/ true);
        assertNull(method);

        method = ClassHelper.findMethod(
            PublicInner0.class, "method0", ClassHelper.VOID_PARAMS, /*fStatic*/ false);
        assertNotNull(method);
        assertEquals(method.getName(), "method0");

        method = ClassHelper.findMethod(
            PublicInner0.class, "method1", new Class[] {String.class}, /*fStatic*/ true);
        assertNull(method);

        method = ClassHelper.findMethod(
            PublicInner0.class, "method1", new Class[1], /*fStatic*/ true);
        assertNull(method);

        method = ClassHelper.findMethod(
            PublicInner0.class, "method1", new Class[] {String.class}, /*fStatic*/ false);
        assertNotNull(method);
        assertEquals(method.getName(), "method1");

        method = ClassHelper.findMethod(
            PublicInner0.class, "method1", new Class[1], /*fStatic*/ false);
        assertNotNull(method);
        assertEquals(method.getName(), "method1");
        }

    /**
    * Test the newInstance method with different constructors.
    *
    * @throws InstantiationException if an exception is raised trying
    *         to instantiate the object, whether the exception is a
    *         security, method access, no such method, or instantiation
    *         exception
    * @throws InvocationTargetException if the constructor of the new
    *         object instance raises an exception
    */
    @Test
    public void testNewInstanceConstructors()
        throws InstantiationException, InvocationTargetException
        {
        TestConstructors constrObj = null;
        TestPrimitiveConstructor constrPrimitive = null;

        // test empty constructor
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, ClassHelper.VOID);
        assertNotNull(constrObj);

        // test Boolean and boolean constructors
        Boolean fValue = Boolean.TRUE;
        Object[] aofParam = {fValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aofParam);
        assertEquals(fValue, constrObj.getBooleanValue());

        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aofParam);
        assertEquals(fValue, constrPrimitive.getBooleanValue());

        // test Character and char constructors
        Character chValue = 'a';
        Object[] aochParam = {chValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aochParam);
        assertEquals(chValue, constrObj.getCharacterValue());

        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aochParam);
        assertEquals(chValue.charValue(), constrPrimitive.getCharValue());

        // test Byte and byte constructors
        Byte bValue = (byte) 1;
        Object[] aobParam = {bValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aobParam);
        assertEquals(bValue, constrObj.getByteValue());

        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aobParam);
        assertEquals(bValue.byteValue(), constrPrimitive.getByteValue());

        // test Short and short constructors
        Short shValue = (short) 3;
        Object[] aoshParam = {shValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aoshParam);
        assertEquals(shValue, constrObj.getShortValue());

        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aoshParam);
        assertEquals(shValue.shortValue(), constrPrimitive.getShortValue());

        // test Integer and int constructors
        Integer nValue = 4;
        Object[] aonParam = {nValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aonParam);
        assertEquals(nValue, constrObj.getIntegerValue());

        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aonParam);
        assertEquals(nValue.intValue(), constrPrimitive.getIntValue());

        // test Long and long constructors
        Long lValue = 5L;
        Object[] aolParam = {lValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aolParam);
        assertEquals(lValue, constrObj.getLongValue());

        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aolParam);
        assertEquals(lValue.longValue(), constrPrimitive.getLongValue());

        // test Float and float constructors
        Float flValue = 6F;
        Object[] aoflParam = {flValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aoflParam);
        assertEquals(flValue, constrObj.getFloatValue());

        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aoflParam);
        assertEquals(flValue, constrPrimitive.getFloatValue(), 0);

        // test Double and double constructors
        Double dValue = 7.8;
        Object[] aodParam = {dValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aodParam);
        assertEquals(dValue, constrObj.getDoubleValue());

        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aodParam);
        assertEquals(dValue, constrPrimitive.getDoubleValue(), 0);

        // test multiple argument constructor
        Object[] aoMultipleParam = {fValue, nValue, lValue, dValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aoMultipleParam);
        assertEquals(fValue, constrObj.getBooleanValue());
        assertEquals(nValue, constrObj.getIntegerValue());
        assertEquals(lValue, constrObj.getLongValue());
        assertEquals(dValue, constrObj.getDoubleValue());

        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aoMultipleParam);
        assertEquals(fValue, constrPrimitive.getBooleanValue());
        assertEquals(nValue.intValue(), constrPrimitive.getIntValue());
        assertEquals(lValue.longValue(), constrPrimitive.getLongValue());
        assertEquals(dValue, constrPrimitive.getDoubleValue(), 0);

        // test constructor with mixed primitive and
        // non-primitive arguments
        Integer nIValue = 5;
        Object[] aoMixedParam = {nValue, nIValue};
        constrPrimitive = (TestPrimitiveConstructor) ClassHelper.newInstance(
                TestPrimitiveConstructor.class, aoMixedParam);
        assertEquals(nValue.intValue(), constrPrimitive.getIntValue());
        assertEquals(nIValue, constrPrimitive.getIntegerValue());

        // test String constructor
        String sValue = new String("param1");
        Object[] aosParam = {sValue};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aosParam);
        assertEquals(sValue, constrObj.getStringValue());

        // test valid argument count with trailing null
        Object[] aoTNullParam = {"param3", null };
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aoTNullParam);
        assertEquals("param3", constrObj.getStringValue());
        assertNull(constrObj.getIntegerValue());

        // test valid argument count with leading null
        Object[] aoLNullParam = {null, 3};
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aoLNullParam);
        assertNull(constrObj.getStringValue());
        assertEquals(Integer.valueOf(3), constrObj.getIntegerValue());

        // test multiple null arguments
        Object[] aoMNullParam = new Object[4];
        constrObj = (TestConstructors) ClassHelper.newInstance(
                TestConstructors.class, aoMNullParam);
        assertNull(constrObj.getStringValue());
        assertNull(constrObj.getIntegerValue());
        assertNull(constrObj.getLongValue());
        assertNull(constrObj.getDoubleValue());
        }

    /**
    * Test the newInstance() failure cases.
    */
    @Test
    public void testNewInstanceExceptions()
        {
        // test with correct argument count but wrong type
        Object[] aoParam1 = {"param1", "param2"};
        instantiationExceptionHelper(TestConstructors.class, aoParam1);

        // test with wrong argument count
        Object[] aoParam2 = {"param1", "param2", "param3"};
        instantiationExceptionHelper(TestConstructors.class, aoParam2);

        // test wrong argument count and nulls
        Object[] aoParam3 = {null, null, null};
        instantiationExceptionHelper(TestConstructors.class, aoParam3);

        // test wrong argument type
        Object[] aoParam4 = {new TestConstructors()};
        instantiationExceptionHelper(TestConstructors.class, aoParam4);

        // test wrong argument type mixed
        Object[] aoParam5 = {"param1", new TestConstructors()};
        instantiationExceptionHelper(TestConstructors.class, aoParam5);

        // test primitive types being called with nulls
        Object[] aoParam6 = new Object[4];
        instantiationExceptionHelper(TestPrimitiveConstructor.class, aoParam6);

        // test with no arguments
        instantiationExceptionHelper(TestPrimitiveConstructor.class, null);

        // test abstract class
        instantiationExceptionHelper(TestAbstract.class, null);

        // test abstract class with argument
        Object[] aoParam7 = {"param1"};
        instantiationExceptionHelper(TestAbstract.class, aoParam7);

        // test null class name
        instantiationExceptionHelper(null, null);

        // test constructor throwing RunTimeException
        Object[] aoParam8 = {"RunTimeException"};
        invocationTargetExceptionHelper(TestConstructors.class, aoParam8);
        }

    @Test
    public void testReifyTypes()
        {
        Map<String, Type[]> mapTypes = ClassHelper.getReifiedTypes(SimpleReify.class, Callable.class);
        assertEquals(new Type[] {Integer.class}, mapTypes.get("V"));

        mapTypes = ClassHelper.getReifiedTypes(WildcardReify.class, Callable.class);
        Type[] aTypes = mapTypes.get("V");
        assertEquals(new Type[] {Callable.class}, new Type[] {
                aTypes.length > 0 ? ClassHelper.getClass(aTypes[0]) : null});

        mapTypes = ClassHelper.getReifiedTypes(ReifyTestLevelOne.class, Callable.class);
        assertEquals(new Type[] {String.class}, mapTypes.get("V"));

        mapTypes = ClassHelper.getReifiedTypes(ReifyTestTwoLevelOne.class, Callable.class);
        assertEquals(new Type[] {Number.class}, mapTypes.get("V"));

        mapTypes = ClassHelper.getReifiedTypes(ReifyTestTwoLevelTwo.class, Callable.class);
        assertEquals(new Type[] {Long.class}, mapTypes.get("V"));

        mapTypes = ClassHelper.getReifiedTypes(Foo.class, AbstractFoo.class);
        assertEquals(new Type[] {Integer.class}, mapTypes.get("T"));

        assertThat(ClassHelper.getReifiedTypes(CommonContract.class, Contract.class),
                allOf(hasEntry("A", new Type[]{String.class}),
                        hasEntry("B", new Type[]{String.class}),
                        hasEntry("C", new Type[]{FileInputStream.class}),
                        hasEntry("D", new Type[]{Double.class})));

        assertThat(ClassHelper.getReifiedTypes(QuietContract.class, Contract.class),
                allOf(hasEntry("A", new Type[]{Object.class}),
                      hasEntry("B", new Type[]{Object.class}),
                      hasEntry("C", new Type[]{InputStream.class}),
                      hasEntry("D", new Type[]{Number.class})));

        assertThat(ClassHelper.getReifiedTypes(LevelOneContract.class, Contract.class),
                allOf(hasEntry("A", new Type[]{OutputStream.class}),
                      hasEntry("B", new Type[]{String.class}),
                      hasEntry("C", new Type[]{FilterInputStream.class}),
                      hasEntry("D", new Type[]{Integer.class})));

        assertThat(ClassHelper.getReifiedTypes(LevelTwoContract.class, Contract.class),
                allOf(hasEntry("A", new Type[]{FileOutputStream.class}),
                      hasEntry("B", new Type[]{String.class}),
                      hasEntry("C", new Type[]{DataInputStream.class}),
                      hasEntry("D", new Type[]{Integer.class})));

        assertThat(ClassHelper.getReifiedTypes(QuietLevelTwoContract.class, Contract.class),
                allOf(hasEntry("A", new Type[]{OutputStream.class}),
                      hasEntry("B", new Type[]{String.class}),
                      hasEntry("C", new Type[]{FilterInputStream.class}),
                      hasEntry("D", new Type[]{Integer.class})));

        assertThat(ClassHelper.getReifiedTypes(IntegerContractImpl.class, Contract.class),
                allOf(hasEntry("A", new Type[]{String.class}),
                      hasEntry("B", new Type[]{String.class}),
                      hasEntry("C", new Type[]{FileInputStream.class}),
                      hasEntry("D", new Type[]{Integer.class})));

        assertThat(ClassHelper.getReifiedTypes(FloatContractImpl.class, Contract.class),
                allOf(hasEntry("A", new Type[]{String.class}),
                      hasEntry("B", new Type[]{Float.class}),
                      hasEntry("C", new Type[]{ByteArrayInputStream.class}),
                      hasEntry("D", new Type[]{Integer.class})));
        }

    @Test
    public void testDefaultReflectionFiltering()
        {
        assertThat(ClassHelper.isReflectionAllowed(String.class), is(false));
        assertThat(ClassHelper.isReflectionAllowed(Runtime.getRuntime()), is(false));
        assertThat(ClassHelper.isReflectionAllowed("String"), is(true));
        }

    /**
    * Helper routine for handling instantiationExceptions.
    *
    * @param clz the class to instantiate
    * @param aoParam the constructor parameters
    */
    private void instantiationExceptionHelper(Class clz, Object[] aoParam)
        {
        Object obj = null;

        try
            {
            obj = ClassHelper.newInstance(clz, aoParam);
            fail("expected exception");
            }
        catch (InstantiationException e)
            {
            Base.out(e.toString());
            }
        catch (InvocationTargetException e)
            {
            e.printStackTrace();
            fail();
            }
        catch (Exception e)
            {
            // we would not see these other exception
            e.printStackTrace();
            fail();
            }

        assertNull(obj);
        }

    /**
    * Helper routine for handling InvocationExceptions.
    *
    * @param clz the class to instantiate
    * @param aoParam the constructor parameters
    */
    private void invocationTargetExceptionHelper(Class clz, Object[] aoParam)
        {

        Object obj = null;

        try
            {
            obj = ClassHelper.newInstance(clz, aoParam);
            fail("expected exception");
            }
        catch (InstantiationException e)
            {
            e.printStackTrace();
            fail();
            }
        catch (InvocationTargetException e)
            {
            // expected
            }
        catch (Exception e)
            {
            // we would not see these other exception
            e.printStackTrace();
            fail();
            }
        assertNull(obj);
        }


     // ----- inner classes --------------------------------------------

    public static class PublicInner0
        {
        protected Object newInstance0()
            {
            return new PublicInner0();
            }
        public void method0()
            {
            out("method0");
            }
        public void method1(String s)
            {
            out("method1-1");
            }
        public void method1(String s, Map m)
            {
            out("method1-2");
            }
        private void out(String s)
            {
            Base.out(ClassHelper.getSimpleName(getClass()) +
                " in PublicInner0." + s);
            }
        }

    private static class PrivateInner1
            extends PublicInner0
            implements Runnable
        {
        protected Object newInstance1()
            {
            return new PrivateInner1();
            }
        public void method1(String s)
            {
            out("method1-1");
            }
        public void method1(String s, Map m)
            {
            out("method1-2");
            }
        public void method2()
            {
            out("method2");
            }
        public void run()
            {
            out("run");
            }
        private void methodInaccessible()
            {
            out("methodInaccessible");
            }
        private void out(String s)
            {
            Base.out(ClassHelper.getSimpleName(getClass()) +
                " in PublicInner1." + s);
            }
        }
    private static class PrivateInner2
            extends PrivateInner1
        {
        protected Object newInstance2()
            {
            return new PrivateInner2();
            }
        public void run()
            {
            out("run");
            }
        private void out(String s)
            {
            Base.out(ClassHelper.getSimpleName(getClass()) +
                " in PublicInner2." + s);
            }
        }

    public static class Factory
            extends PrivateInner2
        {
        public static Object newInstance(int i)
            {
            Factory factory = new Factory();
            switch (i)
                {
                default:
                case 0:
                    return factory.newInstance0();
                case 1:
                    return factory.newInstance1();
                case 2:
                    return factory.newInstance2();
                }
            }
        }

    /**
    * Test newInstance instantiation with different constructors.
    */
    public static class TestConstructors
        {
        public TestConstructors()
            {
            m_sValue = null;
            }

        public TestConstructors(String s)
            {
            if (s.equalsIgnoreCase("RunTimeException"))
                {
                throw new RuntimeException("Testing exception handling");
                }
            else
                {
                m_sValue = s;
                }
            }

        public TestConstructors(Boolean f)
            {
            m_fValue = f;
            }

        public TestConstructors(Character ch)
            {
            m_chValue = ch;
            }

        public TestConstructors(Byte b)
            {
            m_bValue = b;
            }

        public TestConstructors(Short sh)
            {
            m_shValue = sh;
            }

        public TestConstructors(Integer n)
            {
            m_nValue = n;
            }

        public TestConstructors(Long l)
            {
            m_lValue = l;
            }

        public TestConstructors(Float fl)
            {
            m_flValue = fl;
            }

        public TestConstructors(Double d)
            {
            m_dValue = d;
            }

        public TestConstructors(String s, Integer n)
            {
            m_sValue = s;;
            m_nValue = n;
            }

        public TestConstructors(String s, Double d)
            {
            m_sValue = s;
            m_dValue = d;
            }

        public TestConstructors(Boolean f, Integer n, Long l, Double d)
            {
            m_fValue = f;
            m_nValue = n;
            m_lValue = l;
            m_dValue = d;
            }

        public String getStringValue()
            {
            return m_sValue;
            }

        public Boolean getBooleanValue()
            {
            return m_fValue;
            }

        public Character getCharacterValue()
            {
            return m_chValue;
            }

        public Byte getByteValue()
            {
            return m_bValue;
            }

        public Short getShortValue()
            {
            return m_shValue;
            }

        public Integer getIntegerValue()
            {
            return m_nValue;
            }

        public Long getLongValue()
            {
            return m_lValue;
            }

        public Float getFloatValue()
            {
            return m_flValue;
            }

        public Double getDoubleValue()
            {
            return m_dValue;
            }

            private String m_sValue;
            private Boolean m_fValue;
            private Character m_chValue;
            private Byte m_bValue;
            private Short m_shValue;
            private Integer m_nValue;
            private Long m_lValue;
            private Float m_flValue;
            private Double m_dValue;
        }

    /**
    * Test newInstance instantiation with constructor with primitive
    * type arguments.
    */
    public static class TestPrimitiveConstructor
        {
        public TestPrimitiveConstructor(boolean f)
            {
            m_fValue = f;
            }

        public TestPrimitiveConstructor(char ch)
            {
            m_chValue = ch;
            }

        public TestPrimitiveConstructor(byte b)
            {
            m_bValue = b;
            }

        public TestPrimitiveConstructor(short sh)
            {
            m_shValue = sh;
            }

        public TestPrimitiveConstructor(int n)
            {
            m_nValue = n;
            }

        public TestPrimitiveConstructor(long l)
            {
            m_lValue = l;
            }

        public TestPrimitiveConstructor(float fl)
            {
            m_flValue = fl;
            }

        public TestPrimitiveConstructor(double d)
            {
            m_dValue = d;
            }

        /**
        * Test mixture of primitive and non primitive arguments.
        */
        public TestPrimitiveConstructor(int n, Integer nI)
            {
            m_nValue = n;
            m_nIValue = nI;
            }

        /**
        * Test multiple arguments.
        */
        public TestPrimitiveConstructor(boolean f, Integer n, Long l, Double d)
            {
            m_fValue = f;
            m_nValue = n;
            m_lValue = l;
            m_dValue = d;
            }

        public boolean getBooleanValue()
            {
            return m_fValue;
            }

        public char getCharValue()
            {
            return m_chValue;
            }

        public byte getByteValue()
            {
            return m_bValue;
            }

        public short getShortValue()
            {
            return m_shValue;
            }

        public int getIntValue()
            {
            return m_nValue;
            }

        public long getLongValue()
            {
            return m_lValue;
            }

        public float getFloatValue()
            {
            return m_flValue;
            }

        public double getDoubleValue()
            {
            return m_dValue;
            }

        public Integer getIntegerValue()
            {
            return m_nIValue;
            }

        private boolean m_fValue;
        private char m_chValue;
        private byte m_bValue;
        private short m_shValue;
        private int m_nValue;
        private long m_lValue;
        private float m_flValue;
        private double m_dValue;
        private Integer m_nIValue;
        }

    /**
    * Test newInstance instantiation abstract class.
    */
    public static abstract class TestAbstract
        {
        public TestAbstract()
            {
            Base.out("Constructor for abstract class TestAbstract()");
            }

        public TestAbstract(String s)
            {
            Base.out("Constructor for abstract class TestAbstract(String s)");
            }
        }

    // ----- inner classes: TestReifyTypes ----------------------------------

    public static class SimpleReify
            implements Callable<Integer>
        {
        public Integer call() throws Exception
            {
            throw new UnsupportedOperationException();
            }
        }

    public static class WildcardReify
            implements Callable<Callable<? extends Enum<?>>>
        {
        public Callable<? extends Enum<?>> call() throws Exception
            {
            throw new UnsupportedOperationException();
            }
        }

    public static class NestedReify<T extends Enum>
            implements Callable<Callable<Callable<T>>>
        {
        public Callable<Callable<T>> call() throws Exception
            {
            throw new UnsupportedOperationException();
            }
        }

    public static class ReifyTestSuper<T>
            implements Callable<T>
        {
        public T call() throws Exception
            {
            throw new UnsupportedOperationException();
            }
        }

    public static class ReifyTestLevelOne
            extends ReifyTestSuper<String>
        {
        }

    public static class ReifyTestTwoLevelOne<T extends Number>
            extends ReifyTestSuper<T>
        {
        }

    public static class ReifyTestTwoLevelTwo
            extends ReifyTestSuper<Long>
        {
        }

    public static abstract class AbstractFoo<T extends Number>
        {
        }

    public static abstract class Foo
            extends AbstractFoo<Integer>
        {
        }

    public static interface Contract<A, B, C extends InputStream, D extends Number>
        {
        }

    public static interface IntegerContract<A, B extends InputStream>
            extends Contract<String, A, B, Integer>
        {
        }

    public static interface FullContract
            extends IntegerContract<Float, ByteArrayInputStream>
        {
        }

    public static class CommonContract
            implements Contract<String, String, FileInputStream, Double>
        {
        }

    public static class QuietContract
            implements Contract
        {
        }

    public static class LevelOneContract<Z extends OutputStream, Y extends String, X extends FilterInputStream, W extends Integer>
            implements Contract<Z, Y, X, W>
        {
        }

    public static class LevelTwoContract
                extends LevelOneContract<FileOutputStream, String, DataInputStream, Integer>
        {
        }

    public static class QuietLevelTwoContract
                extends LevelOneContract
        {
        }

    public static class IntegerContractImpl
                implements IntegerContract<String, FileInputStream>
        {
        }

    public static class FloatContractImpl
                implements FullContract
        {
        }
    }

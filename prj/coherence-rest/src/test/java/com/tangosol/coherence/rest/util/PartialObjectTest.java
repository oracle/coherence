/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.tangosol.coherence.rest.io.JacksonJsonMarshaller;
import com.tangosol.coherence.rest.io.JaxbXmlMarshaller;

import data.pof.Person;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.objectweb.asm.ClassReader;

import org.objectweb.asm.util.TraceClassVisitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author as  2011.06.29
 */
public class PartialObjectTest
    {
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

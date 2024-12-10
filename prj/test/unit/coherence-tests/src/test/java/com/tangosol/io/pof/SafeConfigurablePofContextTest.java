/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.util.UUID;

import data.pof.Address;
import data.pof.EvolvablePortablePerson;
import data.pof.ExternalizablePerson;
import data.pof.Person;
import data.pof.PortablePerson;
import data.pof.SerializablePerson;

import org.junit.Test;

import java.io.IOException;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;


/**
* SafeConfigurablePofContext unit tests.
*
* @author jh  2007.05.03
* @author Gunnar Hillert  2024.05.27
*/
public class SafeConfigurablePofContextTest
        extends AbstractPofTest
    {
    @Test
    public void testGetPofSerializerWithNegativeTypeId()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);

        try
            {
            ctx.getPofSerializer(-1);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetPofSerializerWithKnownTypeId()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertTrue(ctx.getPofSerializer(1) instanceof PortableObjectSerializer);
        }

    @Test
    public void testGetPofSerializerWithUnknownTypeId()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.getPofSerializer(12358);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetTypeWithNegativeTypeId()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.getClass(-1);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetTypeWithKnownTypeId()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertTrue(Throwable.class.equals(ctx.getClass(0)));
        }

    @Test
    public void testGetTypeWithUnknownTypeId()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.getClass(12358);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetUserTypeIdentifierWithNullObject()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.getUserTypeIdentifier((Object) null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetUserTypeIdentifierWithUnknownObject()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertTrue(ctx.getUserTypeIdentifier(new PortablePerson()) ==
            SafeConfigurablePofContext.TYPE_PORTABLE);
        assertTrue(ctx.getUserTypeIdentifier(new ExternalizablePerson()) ==
            SafeConfigurablePofContext.TYPE_SERIALIZABLE);
        assertTrue(ctx.getUserTypeIdentifier(new SerializablePerson()) ==
            SafeConfigurablePofContext.TYPE_SERIALIZABLE);
        }

    @Test
    public void testGetUserTypeIdentifierWithPofObject()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.getUserTypeIdentifier(new Object());
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetUserTypeIdentifierWithNullType()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.getUserTypeIdentifier((Class) null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetUserTypeIdentifierWithUnknownType()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertTrue(ctx.getUserTypeIdentifier(PortablePerson.class) ==
            SafeConfigurablePofContext.TYPE_PORTABLE);
        assertTrue(ctx.getUserTypeIdentifier(ExternalizablePerson.class)
            == SafeConfigurablePofContext.TYPE_SERIALIZABLE);
        assertTrue(ctx.getUserTypeIdentifier(SerializablePerson.class)
            == SafeConfigurablePofContext.TYPE_SERIALIZABLE);
        }

    @Test
    public void testGetUserTypeIdentifierWithPofType()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.getUserTypeIdentifier(Object.class);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetUserTypeIdentifierWithKnownType()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertTrue(ctx.getUserTypeIdentifier(Throwable.class) == 0);
        }

    @Test
    public void testGetUserTypeIdentifierWithNullTypeName()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.getUserTypeIdentifier((String) null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetUserTypeIdentifierWithUnknownTypeName()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertTrue(ctx.getUserTypeIdentifier(PortablePerson.class.getName()) ==
            SafeConfigurablePofContext.TYPE_PORTABLE);
        assertTrue(ctx.getUserTypeIdentifier(ExternalizablePerson.class.getName())
            == SafeConfigurablePofContext.TYPE_SERIALIZABLE);
        assertTrue(ctx.getUserTypeIdentifier(SerializablePerson.class.getName())
            == SafeConfigurablePofContext.TYPE_SERIALIZABLE);
        }

    @Test
    public void testGetUserTypeIdentifierWithPofTypeName()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.getUserTypeIdentifier(Object.class.getName());
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetUserTypeIdentifierWithKnownTypeName()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertTrue(ctx.getUserTypeIdentifier(Throwable.class.getName()) == 0);
        }

    @Test
    public void testIsUserTypeWithNullObject()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.isUserType((Object) null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testIsUserTypeWithUnknownObject()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertFalse(ctx.isUserType(this));
        }

    @Test
    public void testIsUserTypeWithPofObject()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertFalse(ctx.isUserType(new Object()));
        }

    @Test
    public void testIsUserTypeWithKnownObject()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertTrue(ctx.isUserType(new Throwable()));
        }

    @Test
    public void testIsUserTypeWithNullType()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.isUserType((Class) null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testIsUserTypeWithUnknownType()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertFalse(ctx.isUserType(getClass()));
        }

    @Test
    public void testIsUserTypeWithPofType()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertFalse(ctx.isUserType(Object.class));
        }

    @Test
    public void testIsUserTypeWithKnownType()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        assertTrue(ctx.isUserType(Throwable.class));
        }

    @Test
    public void testIsUserTypeWithNullTypeName()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        try
            {
            ctx.isUserType((String) null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testIsUserTypeWithUnknownTypeName()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertFalse(ctx.isUserType(getClass().getName()));
        }

    @Test
    public void testIsUserTypeWithPofTypeName()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertFalse(ctx.isUserType(Object.class.getName()));
        }

    @Test
    public void testIsUserTypeWithKnownTypeName()
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);
        assertTrue(ctx.isUserType(Throwable.class.getName()));
        }

    @Test
    public void testSerialization()
            throws IOException
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);

        UUID uuid = new UUID();

        byte[] ab = new byte[1024];
        ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), uuid);

        Object o = ctx.deserialize(new ByteArrayReadBuffer(ab).getBufferInput());
        assertEquals(o, uuid);

        PortablePerson person = new PortablePerson("Aleksandar Seovic",
                new Date(74, 7, 24));

        Address addr = new Address("208 Myrtle Ridge Rd", "Lutz", "FL", "33549");
        person.setAddress(addr);
        person.setChildren(new Person[]
            {
            new PortablePerson("Aleksandar Seovic JR.", new Date(174, 1, 1))
            });

        ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), person);

        o = ctx.deserialize(new ByteArrayReadBuffer(ab).getBufferInput());
        assertTrue(equals(o, person));

        // COH-5584: repeat the serialization to verify that PortablePerson
        // and Address are now cached in ConfigurablePofContext.
        ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), person);

        o = ctx.deserialize(new ByteArrayReadBuffer(ab).getBufferInput());
        assertTrue(equals(o, person));
        }

    @Test
    public void testMixedSerialization()
            throws IOException
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);

        Exception e = new Exception("this is a test");

        byte[] ab = new byte[0x3 << 11]; // 6k
        ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), e);

        Object o = ctx.deserialize(new ByteArrayReadBuffer(ab).getBufferInput());
        assertTrue(o instanceof PortableException);

        ExternalizablePerson person = new ExternalizablePerson("Aleksandar Seovic",
                new Date(74, 7, 24));

        Address addr = new Address("208 Myrtle Ridge Rd", "Lutz", "FL", "33549");
        person.setAddress(addr);
        person.setChildren(new Person[]
            {
            new PortablePerson("AAAAAleksandar Seovic JR.", new Date(174, 1, 1)),
            new SerializablePerson("BBBBAleksandar Seovic JR. II", new Date(173, 1, 1))
            });

        ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), person);
        o = ctx.deserialize(new ByteArrayReadBuffer(ab).getBufferInput());

        System.out.println("person[0]" + person.getChildren()[0]);
        System.out.println("o[0]     " + ((Person)o).getChildren()[0]);

        System.out.println(o);
        System.out.println(person);
        assertTrue(equals(o, person));
        }

    @Test
    public void testEvolvableSerialization()
            throws IOException
        {
        SafeConfigurablePofContext ctx = new SafeConfigurablePofContext();
        ctx.setEnableAutoTypeDiscovery(false);

        EvolvablePortablePerson person = new EvolvablePortablePerson("Aleksandar Seovic",
                new Date(74, 7, 24));

        Address addr = new Address("208 Myrtle Ridge Rd", "Lutz", "FL", "33549");
        person.setAddress(addr);
        person.setDataVersion(2);

        byte[] ab = new byte[1024];
        ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), person);

        Object o = ctx.deserialize(new ByteArrayReadBuffer(ab).getBufferInput());
        assertTrue(((EvolvablePortablePerson) o).getDataVersion() == 2);
        assertTrue(equals(o, person));
        }

    @Test
    public void testSetSerializer()
            throws IOException
        {
        ConfigurablePofContext ctx = new ConfigurablePofContext("com/tangosol/io/pof/set-serializer-pof-config.xml");
        ctx.setEnableAutoTypeDiscovery(false);

        Set set = new HashSet();
        assertTrue(ctx.isUserType(set));
        assertTrue(ctx.isUserType(set.getClass()));
        assertTrue(ctx.isUserType(set.getClass().getName()));

        ByteArrayWriteBuffer wb  = new ByteArrayWriteBuffer(1024);
        ctx.serialize(wb.getBufferOutput(), set);

        Object o = ctx.deserialize(wb.getReadBuffer().getBufferInput());
        assertTrue(o instanceof Set);

        ctx = new SafeConfigurablePofContext("com/tangosol/io/pof/set-serializer-pof-config.xml");

        assertTrue(ctx.isUserType(set));
        assertTrue(ctx.isUserType(set.getClass()));
        assertTrue(ctx.isUserType(set.getClass().getName()));

        wb  = new ByteArrayWriteBuffer(1024);
        ctx.serialize(wb.getBufferOutput(), new HashSet());

        o = ctx.deserialize(wb.getReadBuffer().getBufferInput());
        assertTrue(o instanceof Set);
        }

    /**
    * Used for debugging only.
    */
    public static void main(String[] asArg)
            throws Exception
        {
        SafeConfigurablePofContextTest test = new SafeConfigurablePofContextTest();
        //test.testSerialization();
        test.testMixedSerialization();
        //test.testEvolvableSerialization();
        }
    }
/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;

import data.pof.Address;
import data.pof.BadPersonLite;
import data.pof.EvolvablePortablePerson;
import data.pof.EvolvablePortablePerson2;
import data.pof.PersonLite;
import data.pof.PortablePerson;
import data.pof.PortablePersonLite;
import data.pof.SkippingPersonLite;

import org.junit.Test;

import java.io.IOException;

import java.util.Date;

import static org.junit.Assert.*;


/**
* SimplePofContext unit tests.
*
* @author gm 2006.12.21
* @author jh 2006.12.27
*/
public class SimplePofContextTest
        extends AbstractPofTest
    {
    @Test
    public void testTypeRegistrationWithNullType()
        {
        SimplePofContext ctx = new SimplePofContext();
        try
            {
            ctx.registerUserType(1, null, null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testTypeRegistrationWithNullSerializer()
        {
        SimplePofContext ctx = new SimplePofContext();
        try
            {
            ctx.registerUserType(1, this.getClass(), null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testTypeRegistration()
        {
        SimplePofContext         ctx        = new SimplePofContext();
        PortableObjectSerializer serializer = new PortableObjectSerializer(1);

        ctx.registerUserType(1, this.getClass(), serializer);
        ctx.registerUserType(2, PortableObject.class, new PortableObjectSerializer(2));

        assertEquals(1, ctx.getUserTypeIdentifier(this));
        assertEquals(1, ctx.getUserTypeIdentifier(this.getClass()));
        assertEquals(1, ctx.getUserTypeIdentifier(this.getClass().getName()));

        assertEquals(this.getClass(), ctx.getClass(1));
        assertEquals(this.getClass().getName(), ctx.getClassName(1));
        assertEquals(serializer, ctx.getPofSerializer(1));

        ctx.unregisterUserType(1);
        ctx.unregisterUserType(2);
        }

    @Test
    public void testGetPofSerializerWithNegativeTypeId()
        {
        SimplePofContext ctx = new SimplePofContext();
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
    public void testGetPofSerializerWithUnknownTypeId()
        {
        SimplePofContext ctx = new SimplePofContext();
        try
            {
            ctx.getPofSerializer(5);
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
        SimplePofContext ctx = new SimplePofContext();
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
    public void testGetTypeWithUnknownTypeId()
        {
        SimplePofContext ctx = new SimplePofContext();
        try
            {
            ctx.getClass(5);
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
        SimplePofContext ctx = new SimplePofContext();
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
    public void testGetUserTypeIdentifierWithNullType()
        {
        SimplePofContext ctx = new SimplePofContext();
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
        SimplePofContext ctx = new SimplePofContext();
        try
            {
            ctx.getUserTypeIdentifier(this.getClass());
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testGetUserTypeIdentifierWithNullTypeName()
        {
        SimplePofContext ctx = new SimplePofContext();
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
        SimplePofContext ctx = new SimplePofContext();
        try
            {
            ctx.getUserTypeIdentifier(this.getClass().getName());
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void testSerialization()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1, PortablePerson.class, new PortableObjectSerializer(1));
        ctx.registerUserType(2, Address.class, new PortableObjectSerializer(2));

        PortablePerson person1 = new PortablePerson("Aleksandar Seovic",
                new Date(74, 7, 24));
        PortablePerson person2 = new PortablePerson("Marija Seovic",
                new Date(78, 1, 20));
        PortablePerson person3 = new PortablePerson("Ana Maria Seovic",
                new Date(104, 7, 14, 7, 43, 0));
        person1.setSpouse(person2);

        Address addr = new Address("208 Myrtle Ridge Rd", "Lutz", "FL", "33549");
        person1.setAddress(addr);
        person2.setAddress(addr);
        person3.setAddress(addr);

        PortablePerson[] aPerson = new PortablePerson[]{person3};
        person1.setChildren(aPerson);
        person2.setChildren(aPerson);

        byte[] ab = new byte[1024];

        ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), person1);

        PortablePerson person4 = (PortablePerson) ctx.deserialize(
                new ByteArrayReadBuffer(ab).getBufferInput());

        assertEquals(person1.m_sName, person4.m_sName);
        assertEquals(person1.m_dtDOB, person4.m_dtDOB);
        assertEquals(person1.getSpouse().m_sName, person4.getSpouse().m_sName);
        assertEquals(person1.getAddress().m_sCity, person4.getAddress().m_sCity);
        assertEquals(1, person4.getChildren().length);
        assertEquals(person3.m_sName, person4.getChildren()[0].m_sName);
        assertEquals(person3.getAddress().m_sStreet,
                person4.getChildren()[0].getAddress().m_sStreet);

        SimplePofContext ctx2 = new SimplePofContext();
        ctx2.registerUserType(1, PortablePersonLite.class,
                new PortableObjectSerializer(1));
        ctx2.registerUserType(2, Address.class,
                new PortableObjectSerializer(2));

        PortablePersonLite person5 = (PortablePersonLite) ctx2.deserialize(
                new ByteArrayReadBuffer(ab).getBufferInput());
        assertEquals(person1.m_sName, person5.m_sName);
        }

    @Test
    public void testSerializationWithNonPortableType()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1, PersonLite.class, new PortableObjectSerializer(1));

        PersonLite person = new PersonLite("Ana Maria Seovic", new Date(106, 7, 14));

        byte[] ab = new byte[1024];
        try
            {
            ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), person);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testSerializationWithBackwardsReadingType()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1, BadPersonLite.class, new PortableObjectSerializer(1));

        BadPersonLite person = new BadPersonLite("Ana Maria Seovic", new Date(106, 7, 14));

        byte[] ab = new byte[1024];
        ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), person);
        try
            {
            ctx.deserialize(new ByteArrayReadBuffer(ab).getBufferInput());
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testSerializationWithSkippingReadingType()
            throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1, SkippingPersonLite.class, new PortableObjectSerializer(1));

        SkippingPersonLite person = new SkippingPersonLite("Ana Maria Seovic",
                new Date(106, 7, 14));

        byte[] ab = new byte[1024];
        ctx.serialize(new ByteArrayWriteBuffer(ab).getBufferOutput(), person);
        try
            {
            ctx.deserialize(new ByteArrayReadBuffer(ab).getBufferInput());
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testEvolvableObjectSerialization()
            throws IOException
        {
        SimplePofContext ctxV1 = new SimplePofContext();
        ctxV1.registerUserType(1, EvolvablePortablePerson.class,
                new PortableObjectSerializer(1));
        ctxV1.registerUserType(2, Address.class,
                new PortableObjectSerializer(2));

        SimplePofContext ctxV2 = new SimplePofContext();
        ctxV2.registerUserType(1, EvolvablePortablePerson2.class,
                new PortableObjectSerializer(1));
        ctxV2.registerUserType(2, Address.class,
                new PortableObjectSerializer(2));

        EvolvablePortablePerson2 person12 = new EvolvablePortablePerson2(
                "Aleksandar Seovic", new Date(74, 7, 24));
        EvolvablePortablePerson2 person22 = new EvolvablePortablePerson2(
                "Ana Maria Seovic", new Date(104, 7, 14, 7, 43, 0));

        Address addr = new Address("208 Myrtle Ridge Rd", "Lutz", "FL", "33549");
        person12.setAddress(addr);
        person22.setAddress(addr);

        person12.m_sNationality = person22.m_sNationality = "Serbian";
        person12.m_addrPOB      = new Address(null, "Belgrade", "Serbia", "11000");
        person22.m_addrPOB      = new Address("128 Asbury Ave, #401", "Evanston", "IL", "60202");

        person12.setChildren(new EvolvablePortablePerson2[]{person22});

        byte[] abV2 = new byte[1024];
        ctxV2.serialize(new ByteArrayWriteBuffer(abV2).getBufferOutput(), person12);

        EvolvablePortablePerson person11 = (EvolvablePortablePerson) ctxV1
                .deserialize(new ByteArrayReadBuffer(abV2).getBufferInput());

        EvolvablePortablePerson person21 = new EvolvablePortablePerson(
                "Marija Seovic", new Date(78, 1, 20));
        person21.setAddress(person11.getAddress());
        person21.setChildren(person11.getChildren());
        person11.setSpouse(person21);

        byte[] abV1 = new byte[1024];
        ctxV1.serialize(new ByteArrayWriteBuffer(abV1).getBufferOutput(), person11);

        EvolvablePortablePerson2 person = (EvolvablePortablePerson2) ctxV2
                .deserialize(new ByteArrayReadBuffer(abV1).getBufferInput());

        assertEquals(person12.m_sName, person.m_sName);
        assertEquals(person12.m_sNationality, person.m_sNationality);
        assertEquals(person12.m_dtDOB, person.m_dtDOB);
        assertEquals(person11.getSpouse().m_sName, person.getSpouse().m_sName);
        assertEquals(person12.getAddress().m_sCity, person.getAddress().m_sCity);
        assertEquals(person12.m_addrPOB.m_sCity, person.m_addrPOB.m_sCity);
        }
    }
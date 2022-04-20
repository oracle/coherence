/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Before;
import org.junit.Test;

import data.evolvable.Color;

import static org.junit.Assert.assertEquals;


public class PortableTypeSerializerTest
    {
    private SimplePofContext v1;
    private SimplePofContext v2;

    @Before
    public void setup()
            throws Exception
        {
        v1 = new SimplePofContext();
        v1.registerUserType(1, data.evolvable.v1.Pet.class,
                            new PortableTypeSerializer(1, data.evolvable.v1.Pet.class));
        v1.registerUserType(2, data.evolvable.v1.Dog.class,
                            new PortableTypeSerializer(2, data.evolvable.v1.Dog.class));

        v2 = new SimplePofContext();
        v2.registerUserType(1, data.evolvable.v2.Pet.class,
                            new PortableTypeSerializer(1, data.evolvable.v2.Pet.class));
        v2.registerUserType(2, data.evolvable.v2.Dog.class,
                            new PortableTypeSerializer(2, data.evolvable.v2.Dog.class));
        v2.registerUserType(3, data.evolvable.v2.Animal.class,
                            new PortableTypeSerializer(3, data.evolvable.v2.Animal.class));
        v2.registerUserType(5, data.evolvable.Color.class, new EnumPofSerializer());
        }

    @Test
    public void testRoundTripV1()
            throws Exception
        {
        data.evolvable.v1.Dog dog = new data.evolvable.v1.Dog("Nadia", "Boxer");
        System.out.println(dog);
        Binary binDog = ExternalizableHelper.toBinary(dog, v1);
        assertEquals(dog, ExternalizableHelper.fromBinary(binDog, v1));
        }

    @Test
    public void testRoundTripV2()
            throws Exception
        {
        data.evolvable.v2.Dog dog = new data.evolvable.v2.Dog("Nadia", 10, "Boxer", Color.BRINDLE);
        System.out.println(dog);
        Binary binDog = ExternalizableHelper.toBinary(dog, v2);
        assertEquals(dog, ExternalizableHelper.fromBinary(binDog, v2));
        }

    @Test
    public void testEvolution()
            throws Exception
        {
        data.evolvable.v2.Dog dogV2 = new data.evolvable.v2.Dog("Nadia", 10, "Boxer", Color.BRINDLE);
        System.out.println(dogV2);
        Binary binDogV2 = ExternalizableHelper.toBinary(dogV2, v2);

        data.evolvable.v1.Dog dogV1 = (data.evolvable.v1.Dog) ExternalizableHelper.fromBinary(binDogV2, v1);
        System.out.println(dogV1);
        Binary binDogV1 = ExternalizableHelper.toBinary(dogV1, v1);

        Object dog = ExternalizableHelper.fromBinary(binDogV1, v2);
        System.out.println(dog);

        assertEquals(dogV2, dog);
        }
    }

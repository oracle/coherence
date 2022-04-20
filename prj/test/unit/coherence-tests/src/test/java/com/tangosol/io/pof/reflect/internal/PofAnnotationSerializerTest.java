/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ReadBuffer.BufferInput;

import com.tangosol.io.pof.PofAnnotationSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;

import com.tangosol.util.Binary;

import org.junit.Test;

import java.io.IOException;

import static com.tangosol.util.ExternalizableHelper.fromBinary;
import static com.tangosol.util.ExternalizableHelper.toBinary;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.junit.Assert.assertThat;

/**
 * {@link PofAnnotationSerializer} tests. 
 *
 * @author hr
 * @since 3.7.1
 */
public class PofAnnotationSerializerTest
    {
    /**
     * Test the ability to serialize and deserialize two annotated types.
     * 
     * @throws IOException
     */
    @Test
    public void testSerialization() throws IOException
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1001, PersonV1.class, new PofAnnotationSerializer<PersonV1>(1001, PersonV1.class, true));
        ctx.registerUserType(1002, Child .class, new PofAnnotationSerializer<Child >(1002, Child .class, true));

        PersonV1 value = new PersonV1("Frank", "Spencer", 57);
        Child  child = new Child ("Betty", "Spencer", 55);

        Binary      binValue = toBinary(child, ctx);
        BufferInput buff     = binValue.getBufferInput();
        int         type     = buff.readUnsignedByte();
        int         typeId   = type == 21 ? buff.readPackedInt() : 1002;

        PersonV1 teleported  = (PersonV1) fromBinary(toBinary(value, ctx), ctx);
        Child    teleported2 = (Child) fromBinary(binValue, ctx);

        assertThat(typeId                 , is(1002));
        assertThat(teleported .m_firstName, is("Frank"));
        assertThat(teleported .m_lastName , is("Spencer"));
        assertThat(teleported .m_age      , is(57));
        assertThat(teleported2.m_firstName, is("Betty"));
        assertThat(teleported2.m_lastName , is("Spencer"));
        assertThat(teleported2.m_age      , is(55));
        }

    /**
     * Test nested objects serialize.
     */
    @Test
    public void testAncestry()
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1002, GrandFather.class, new PofAnnotationSerializer<GrandFather>(1002, GrandFather.class, true));
        ctx.registerUserType(1003, Father     .class, new PofAnnotationSerializer<Father     >(1003, Father     .class, true));
        ctx.registerUserType(1004, Child      .class, new PofAnnotationSerializer<Child      >(1004, Child      .class, true));

        final Child       son = new Child      ("Bart" , "Simpson", 10);
        final Father      dad = new Father     ("Homer", "Simpson", 50, son);
        final GrandFather gf  = new GrandFather("Abe"  , "Simpson", 100, dad);

        GrandFather teleported = (GrandFather) fromBinary(toBinary(gf, ctx), ctx);

        assertThat(teleported.m_firstName                 , is("Abe"    ));
        assertThat(teleported.m_lastName                  , is("Simpson"));
        assertThat(teleported.m_age                       , is(100      ));
        assertThat(teleported.m_father.m_firstName        , is("Homer"  ));
        assertThat(teleported.m_father.m_lastName         , is("Simpson"));
        assertThat(teleported.m_father.m_age              , is(50       ));
        assertThat(teleported.m_father.m_child.m_firstName, is("Bart"   ));
        assertThat(teleported.m_father.m_child.m_lastName , is("Simpson"));
        assertThat(teleported.m_father.m_child.m_age      , is(10       ));
        }

    /**
     * Test inheritance.
     */
    @Test
    public void testInheritance()
        {
        SimplePofContext ctx = new SimplePofContext();
        ctx.registerUserType(1001, PersonV1          .class, new PofAnnotationSerializer<PersonV1        >(1001, PersonV1        .class, true));
        ctx.registerUserType(1005, BewilderedPerson  .class, new PofAnnotationSerializer<BewilderedPerson>(1005, BewilderedPerson.class, true));

        BewilderedPerson value = new BewilderedPerson("Frank", "Spencer", 57, "dizzy");

        BewilderedPerson teleported  = (BewilderedPerson) fromBinary(toBinary(value, ctx), ctx);

        assertThat(teleported .m_firstName, is("Frank"));
        assertThat(teleported .m_lastName , is("Spencer"));
        assertThat(teleported .m_age      , is(57));
        assertThat(teleported .m_state    , is("dizzy"));
        }

    /**
     * Test evolvability support.
     */
    @Test
    public void testEvolvable()
        {
        SimplePofContext ctx1 = new SimplePofContext();
         ctx1.registerUserType(1001, PersonV1.class, new PofAnnotationSerializer<PersonV1>(1001, PersonV1.class, true));
        SimplePofContext ctx2 = new SimplePofContext();
        ctx2.registerUserType(1001, PersonV2.class, new PofAnnotationSerializer<PersonV2>(1001, PersonV2.class, true));

        PersonV1 personV1 = new PersonV1("Frank", "Spencer", 57);

        // verify we can go forward 1 => 2
        PersonV2 teleportedV2 = (PersonV2) fromBinary(toBinary(personV1,  ctx1), ctx2);

        // verify we can go back 2 => 1
        teleportedV2.m_fMale = Boolean.TRUE;
        PersonV1 teleportedV1 = (PersonV1) fromBinary(toBinary(teleportedV2, ctx2),  ctx1);
        PersonV2 teleportedV2FromV1 = (PersonV2) fromBinary(toBinary(teleportedV1,  ctx1), ctx2);

        // v1 => v2
        assertThat(teleportedV2.m_firstName, is(personV1.m_firstName));
        assertThat(teleportedV2.m_lastName , is(personV1.m_lastName));
        assertThat(teleportedV2.m_age      , is(personV1.m_age));
        // v2 => v1
        assertThat(teleportedV1.getFutureData(), notNullValue());
        assertThat(teleportedV1.m_firstName, is(personV1.m_firstName));
        assertThat(teleportedV1.m_lastName , is(personV1.m_lastName));
        assertThat(teleportedV1.m_age      , is(personV1.m_age));
        assertThat(teleportedV2FromV1.m_firstName, is(personV1.m_firstName));
        assertThat(teleportedV2FromV1.m_lastName , is(personV1.m_lastName));
        assertThat(teleportedV2FromV1.m_age      , is(personV1.m_age));
        assertThat(teleportedV2FromV1.m_fMale    , is(Boolean.TRUE));
        }

    /**
     * Test primitive evolability support.
     */
    @Test
    public void testPrimitiveEvolable()
        {
        SimplePofContext ctx1 = new SimplePofContext();
        ctx1.registerUserType(1006, SequenceV1.class, new PofAnnotationSerializer<SequenceV1>(1006, SequenceV1.class, true));
        SimplePofContext ctx2 = new SimplePofContext();
        ctx2.registerUserType(1006, SequenceV2.class, new PofAnnotationSerializer<SequenceV2>(1006, SequenceV2.class, true));

        SequenceV1 sequenceV1 = new SequenceV1(1);
        // verify we can go forward 1 ==> 2
        SequenceV2 sequenceV2 = (SequenceV2) fromBinary(toBinary(sequenceV1, ctx1), ctx2);
        assertThat(sequenceV2.m_a, is(1));
        assertThat(sequenceV2.m_b, is(0));

        // verify we can go back 2 => 1
        sequenceV2.m_b = 2;
        SequenceV1 anotherSequenceV1 = (SequenceV1) fromBinary(toBinary(sequenceV2, ctx2), ctx1);
        assertThat(anotherSequenceV1.m_a, is(1));
        }

    @Portable
    public static class PersonV1 extends AbstractEvolvable
        {
        @PortableProperty(value = 2)
        protected String m_firstName;
        @PortableProperty(value = 0)
        protected String m_lastName;
        @PortableProperty(value = 1)
        protected Integer m_age;

        public PersonV1()
            {
            }

        public PersonV1(String firstName, String lastName, Integer age)
            {
            m_firstName = firstName;
            m_lastName  = lastName;
            m_age       = age;
            }

        @Override
        public int getImplVersion()
            {
            return 0;
            }
        }

    @Portable
    public static class PersonV2  extends AbstractEvolvable
        {
        @PortableProperty(value = 2)
        protected String m_firstName;
        @PortableProperty(value = 0)
        protected String m_lastName;
        @PortableProperty(value = 1)
        protected Integer m_age;
        @PortableProperty(value = 3)
        protected Boolean m_fMale;

        public PersonV2()
            {
            }

        public PersonV2(String firstName, String lastName, Integer age, Boolean fMale)
            {
            m_firstName = firstName;
            m_lastName  = lastName;
            m_age       = age;
            m_fMale     = fMale;
            }

        @Override
        public int getImplVersion()
            {
            return 1;
            }
        }

    @Portable
    public static class BewilderedPerson extends PersonV1
        {
        @PortableProperty
        protected String m_state;

        public BewilderedPerson()
            {
            }

        public BewilderedPerson(String firstName, String lastName, Integer age)
            {
            super(firstName, lastName, age);
            }

        public BewilderedPerson(String firstName, String lastName, Integer age, String state)
            {
            super(firstName, lastName, age);
            m_state = state;
            }
        }

    @Portable
    public static class GrandFather
        {
        @PortableProperty(value = 2)
        protected String m_firstName;
        @PortableProperty(value = 0)
        protected String m_lastName;
        @PortableProperty(value = 1)
        protected Integer m_age;
        @PortableProperty
        protected Father m_father;

        public GrandFather()
            {
            }

        public GrandFather(String firstName, String lastName, Integer age, Father father)
            {
            m_firstName = firstName;
            m_lastName  = lastName;
            m_age       = age;
            m_father    = father;
            }
        }

    @Portable
    public static class Father
        {
        @PortableProperty
        protected String m_firstName;
        @PortableProperty
        protected String m_lastName;
        @PortableProperty
        protected Integer m_age;
        @PortableProperty
        protected Child   m_child;

        public Father()
            {
            }

        public Father(String firstName, String lastName, Integer age, Child child)
            {
            m_firstName = firstName;
            m_lastName  = lastName;
            m_age       = age;
            m_child     = child;
            }
        }

    @Portable
    public static class Child
        {
        @PortableProperty
        protected String m_firstName;
        @PortableProperty
        protected String m_lastName;
        @PortableProperty
        protected Integer m_age;

        public Child()
            {
            }

        public Child(String firstName, String lastName, Integer age)
            {
            m_firstName = firstName;
            m_lastName  = lastName;
            m_age       = age;
            }
        }

    @Portable
    public static class SequenceV1 extends AbstractEvolvable
        {
        @PortableProperty(0)
        protected int m_a;

        public SequenceV1()
            {
            }

        public SequenceV1(int a)
            {
            m_a = a;
            }
        
        @Override
        public int getImplVersion()
            {
            return 0;
            }
        }

    @Portable
    public static class SequenceV2 extends AbstractEvolvable
        {
        @PortableProperty(0)
        protected int m_a;
        @PortableProperty(1)
        protected int m_b;

        public SequenceV2()
            {
            }

        public SequenceV2(int a, int b)
            {
            m_a = a;
            m_b = b;
            }

        @Override
        public int getImplVersion()
            {
            return 1;
            }
        }        
    }

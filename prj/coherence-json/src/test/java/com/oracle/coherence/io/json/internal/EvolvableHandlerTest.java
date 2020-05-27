/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.JsonSerializer;

import com.oracle.coherence.io.json.genson.reflect.EvolvableObject;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import javax.json.bind.annotation.JsonbCreator;

import javax.json.bind.annotation.JsonbProperty;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case for evolvability.
 *
 * @author Aleksandar Seovic  2018.06.06
 * @since 14.1.2
 */
class EvolvableHandlerTest
    {
    // ----- test cases -----------------------------------------------------

    @Test
    @Disabled("need to refactor to use the same class name for different class versions")
    void testDeserialization()
        {
        PersonV2 homer = new PersonV2("Homer", 50);
        homer.setSpouse(new PersonV2("Marge", 40));
        homer.setChildren(Arrays.asList("Bart", "Lisa", "Maggie"));

        Binary   json    = ExternalizableHelper.toBinary(homer, SERIALIZER);
        PersonV1 homerV1 = ExternalizableHelper.fromBinary(json, SERIALIZER, PersonV1.class);

        assertEquals("Homer", homerV1.m_sName);
        assertEquals(50, homerV1.m_nAge);
        assertTrue(homerV1.unknownProperties().get("spouse") instanceof JsonObject);
        assertTrue(((JsonObject) homerV1.unknownProperties().get("spouse")).containsKey("@class"));
        assertTrue(homerV1.unknownProperties().get("children") instanceof JsonArray);
        }

    @Test
    @Disabled("need to refactor to use the same class name for different class versions")
    void testCtorDeserialization()
        {
        PersonV2 homer = new PersonV2("Homer", 50);
        homer.setSpouse(new PersonV2("Marge", 40));
        homer.setChildren(Arrays.asList("Bart", "Lisa", "Maggie"));

        Binary   json    = ExternalizableHelper.toBinary(homer, SERIALIZER);
        PersonV1 homerV1 = ExternalizableHelper.fromBinary(json, SERIALIZER, CtorPersonV1.class);

        assertEquals("Homer", homerV1.m_sName);
        assertEquals(50, homerV1.m_nAge);
        assertTrue(homerV1.unknownProperties().get("spouse") instanceof JsonObject);
        assertTrue(((JsonObject) homerV1.unknownProperties().get("spouse")).containsKey("@class"));
        assertTrue(homerV1.unknownProperties().get("children") instanceof JsonArray);
        }

    @Test
    void testRoundTrip()
        {
        PersonV2 homer = new PersonV2("Homer", 50);
        homer.setSpouse(new PersonV2("Marge", 40));
        homer.setChildren(Arrays.asList("Bart", "Lisa", "Maggie"));

        Binary   json    = ExternalizableHelper.toBinary(homer, SERIALIZER);
        PersonV1 homerV1 = ExternalizableHelper.fromBinary(json, SERIALIZER, PersonV1.class);

        json = ExternalizableHelper.toBinary(homerV1, SERIALIZER);
        PersonV2 homerV2 = ExternalizableHelper.fromBinary(json, SERIALIZER, PersonV2.class);

        assertEquals(homer, homerV2);
        }

    @Test
    void testRoundTripWithMissingClass() throws IOException
        {
        String v3 = "{\n" +
                    "  \"@class\":\"com.oracle.coherence.io.json.internal.EvolvableHandlerTest$PersonV1\",\n" +
                    "  \"age\":50,\n" +
                    "  \"name\":\"Homer\",\n" +
                    "  \"address\":{\n" +
                    "    \"@class\":\"com.missing.Address\",\n" +
                    "    \"city\":\"Springfield\"\n" +
                    "  }\n" +
                    "}";

        JsonValue homerV3   = SERIALIZER.deserialize(new ByteArrayReadBuffer(v3.getBytes()).getBufferInput(),
                                                    JsonValue.class);
        Binary    bin       = ExternalizableHelper.toBinary(homerV3, SERIALIZER);
        PersonV1  homerV1   = ExternalizableHelper.fromBinary(bin, SERIALIZER, PersonV1.class);
        Binary    binActual = ExternalizableHelper.toBinary(homerV1, SERIALIZER);
        System.out.println(new String(bin.toByteArray()));
        System.out.println(new String(binActual.toByteArray()));
        assertEquals(bin, binActual);
        }

    // ----- inner class: PersonV1 ------------------------------------------

    static class PersonV1
            extends EvolvableObject
        {
        // ----- constructors -----------------------------------------------

        public PersonV1()
            {
            }

        public PersonV1(String sName, int nAge)
            {
            this.m_sName = sName;
            this.m_nAge  = nAge;
            }

        // ----- accessors --------------------------------------------------

        public String getName()
            {
            return m_sName;
            }

        public void setName(String name)
            {
            this.m_sName = name;
            }

        public int getAge()
            {
            return m_nAge;
            }

        public void setAge(int age)
            {
            this.m_nAge = age;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            PersonV1 that = (PersonV1) o;
            return m_nAge == that.m_nAge && Objects.equals(m_sName, that.m_sName);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_sName, m_nAge);
            }

        @Override
        public String toString()
            {
            return "PersonV1{" +
                   "name='" + m_sName + '\'' +
                   ", age=" + m_nAge +
                   ", unknownProperties=" + unknownProperties() +
                   '}';
            }

        // ----- data members -----------------------------------------------

        @JsonbProperty("name")
        private String m_sName;

        @JsonbProperty("age")
        private int m_nAge;
        }

    // ----- inner class: PersonV2 ------------------------------------------

    static class PersonV2
            extends PersonV1
        {
        // ----- constructors -----------------------------------------------

        public PersonV2(String sName, int nAge)
            {
            super(sName, nAge);
            }

        // ----- accessors --------------------------------------------------

        public Object getSpouse()
            {
            return m_oSpouse;
            }

        public void setSpouse(Object oSpouse)
            {
            this.m_oSpouse = oSpouse;
            }

        public List<String> getChildren()
            {
            return m_listChildren;
            }

        public void setChildren(List<String> listChildren)
            {
            this.m_listChildren = listChildren;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            if (!super.equals(o))
                {
                return false;
                }
            PersonV2 personV2 = (PersonV2) o;
            return Objects.equals(m_oSpouse, personV2.m_oSpouse) &&
                   Objects.equals(m_listChildren, personV2.m_listChildren);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(super.hashCode(), m_oSpouse, m_listChildren);
            }

        @Override
        public String toString()
            {
            return "PersonV2{" +
                   "name='" + getName() + '\'' +
                   ", age=" + getAge() +
                   ", spouse=" + m_oSpouse +
                   ", children=" + m_listChildren +
                   ", unknownProperties=" + unknownProperties() +
                   '}';
            }

        // ----- data members -----------------------------------------------

        @JsonbProperty("spouse")
        private Object m_oSpouse;

        @JsonbProperty("children")
        private List<String> m_listChildren;
        }

    // ----- inner class: CtorPersonV1 --------------------------------------

    static class CtorPersonV1
            extends PersonV1
        {
        // ----- constructors -----------------------------------------------

        private CtorPersonV1()
            {
            throw new RuntimeException("shouldn't be called");
            }

        @JsonbCreator
        public CtorPersonV1(String name, int age)
            {
            super(name, age);
            }
        }

    // ----- data members ---------------------------------------------------

    protected static final Serializer SERIALIZER = new JsonSerializer();
    }

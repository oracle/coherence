/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Copyright 2011-2014 Genson - Cepoi Eugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.oracle.coherence.io.json.genson.reflect;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.annotation.JsonCreator;
import com.oracle.coherence.io.json.genson.ext.jsr353.JSR353Bundle;

import org.junit.Test;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Aleks Seovic  2018.05.09
*/
public class UnknownPropertyHandlerTest {
    private static final Genson GENSON = new GensonBuilder()
                    .withBundle(new JSR353Bundle())
                    .useClassMetadata(true)
                    .useClassMetadataWithStaticType(false)
                    .useConstructorWithArguments(true)
                    .useUnknownPropertyHandler(new EvolvableHandler())
                    .useIndentation(true)
                    .create();

    @Test
    public void testDeserialization() {
        PersonV2 homer = new PersonV2("Homer", 50);
        homer.setSpouse(new PersonV2("Marge", 40));
        homer.setChildren(Arrays.asList("Bart", "Lisa", "Maggie"));

        String json = GENSON.serialize(homer);
        PersonV1 homerV1 = GENSON.deserialize(json, PersonV1.class);

        assertEquals("Homer", homerV1.name);
        assertEquals(50, homerV1.age);
        assertTrue(homerV1.unknownProperties().get("spouse") instanceof JsonObject);
        assertTrue(((JsonObject) homerV1.unknownProperties().get("spouse")).containsKey("@class"));
        assertTrue(homerV1.unknownProperties().get("children") instanceof JsonArray);
    }

    @Test
    public void testCtorDeserialization() {
        PersonV2 homer = new PersonV2("Homer", 50);
        homer.setSpouse(new PersonV2("Marge", 40));
        homer.setChildren(Arrays.asList("Bart", "Lisa", "Maggie"));

        String json = GENSON.serialize(homer);
        PersonV1 homerV1 = GENSON.deserialize(json, CtorPersonV1.class);

        assertEquals("Homer", homerV1.name);
        assertEquals(50, homerV1.age);
        assertTrue(homerV1.unknownProperties().get("spouse") instanceof JsonObject);
        assertTrue(((JsonObject) homerV1.unknownProperties().get("spouse")).containsKey("@class"));
        assertTrue(homerV1.unknownProperties().get("children") instanceof JsonArray);
    }

    @Test
    public void testRoundTrip() {
        PersonV2 homer = new PersonV2("Homer", 50);
        homer.setSpouse(new PersonV2("Marge", 40));
        homer.setChildren(Arrays.asList("Bart", "Lisa", "Maggie"));

        String json = GENSON.serialize(homer);
        PersonV1 homerV1 = GENSON.deserialize(json, PersonV1.class);

        json = GENSON.serialize(homerV1);
        PersonV2 homerV2 = GENSON.deserialize(json, PersonV2.class);

        assertEquals(homer, homerV2);
    }

    @Test
    public void testRoundTripWithMissingClass() {
        String v3 = "{\n" +
                "  \"age\":50,\n" +
                "  \"name\":\"Homer\",\n" +
                "  \"address\":{\n" +
                "    \"@class\":\"com.missing.Address\",\n" +
                "    \"city\":\"Springfield\"\n" +
                "  }\n" +
                "}";

        PersonV1 homerV1 = GENSON.deserialize(v3, PersonV1.class);
        assertEquals(v3, GENSON.serialize(homerV1));
    }

    static class PersonV1 extends EvolvableObject {
        private String name;
        private int age;

        public PersonV1() {
        }

        public PersonV1(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PersonV1 that = (PersonV1) o;
            return age == that.age && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return "PersonV1{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", unknownProperties=" + unknownProperties() +
                    '}';
        }
    }

    static class PersonV2 extends PersonV1 {
        private Object spouse;
        private List<String> children;

        public PersonV2(String name, int age) {
            super(name, age);
        }

        public Object getSpouse() {
            return spouse;
        }

        public void setSpouse(Object spouse) {
            this.spouse = spouse;
        }

        public List<String> getChildren() {
            return children;
        }

        public void setChildren(List<String> children) {
            this.children = children;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            PersonV2 personV2 = (PersonV2) o;
            return Objects.equals(spouse, personV2.spouse) &&
                    Objects.equals(children, personV2.children);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), spouse, children);
        }

        @Override
        public String toString() {
            return "PersonV2{" +
                    "name='" + getName() + '\'' +
                    ", age=" + getAge() +
                    ", spouse=" + spouse +
                    ", children=" + children +
                    ", unknownProperties=" + unknownProperties() +
                    '}';
        }
    }

    static class CtorPersonV1 extends PersonV1 {
        private CtorPersonV1() {
            throw new RuntimeException("shouldn't be called");
        }

        @JsonCreator
        public CtorPersonV1(String name, int age) {
            super(name, age);
        }
    }
}

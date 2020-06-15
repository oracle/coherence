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

package com.oracle.coherence.io.json.genson.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import com.oracle.coherence.io.json.genson.GensonBuilder;
import org.junit.Test;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.annotation.JsonIgnore;
import com.oracle.coherence.io.json.genson.bean.ComplexObject;
import com.oracle.coherence.io.json.genson.bean.Primitives;
import com.oracle.coherence.io.json.genson.bean.Media.Player;

import static org.junit.Assert.*;

public class JsonSerializationTest {
  Genson genson = new Genson();

  @Test public void testSerializeSpecialFloatingPointNumbers() {
    String json = genson.serialize(new Double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN});
    assertEquals(json, "[\"-Infinity\",\"Infinity\",\"NaN\"]");
  }

  @Test
  public void testJsonPrimitiveObject() {
    Primitives p = createPrimitives();
    String json = genson.serialize(p);
    assertEquals(p.jsonString(), json);
  }

  @Test
  public void testJsonArrayOfPrimitives() {
    String expected = "[\"a\",1,3.2,null,true]";
    Object[] array = new Object[]{"a", 1, 3.2, null, true};
    String json = genson.serialize(array);
    assertEquals(expected, json);
  }

  @Test
  public void testJsonArrayOfMixedContent() {
    Primitives p = createPrimitives();
    p.setIntPrimitive(-88);
    p.setDoubleObject(null);
    String expected = "[\"a\"," + p.jsonString() + ",1,3.2,null,false," + p.jsonString() + "]";
    Object[] array = new Object[]{"a", p, 1, 3.2, null, false, p};
    String json = genson.serialize(array);
    assertEquals(expected, json);
  }

  @Test
  public void testJsonComplexObject() {
    Primitives p = createPrimitives();
    List<Primitives> list = Arrays.asList(p, p, p, p, p);
    ComplexObject co = new ComplexObject(p, list, list.toArray(new Primitives[list.size()]));
    String json = genson.serialize(co);
    assertEquals(co.jsonString(), json);
  }

  /*
   * Serialize all public getXX present and all the public/package fields that don't match an used
   * XX getter.
   */
  @Test
  public void testSerializationMixedFieldsAndGetters() {
    String json = "{\"age\":15,\"name\":\"TOTO\",\"noField\":\"TOTO15\"}";
    ClassWithFieldsAndGetter object = new ClassWithFieldsAndGetter("TOTO", 15);
    String out = genson.serialize(object);
    assertEquals(json, out);
  }

  @Test
  public void testSerializeWithAlias() {
    Genson genson = new GensonBuilder().addAlias("ClassWithFieldsAndGetter",
      ClassWithFieldsAndGetter.class).create();
    String json = genson.serialize(new ClassWithFieldsAndGetter("a", 0));
    assertTrue(json.startsWith("{\"@class\":\"ClassWithFieldsAndGetter\""));
    genson = new GensonBuilder().useClassMetadata(true).create();
    json = genson.serialize(new ClassWithFieldsAndGetter("a", 0));
    assertTrue(json
      .startsWith("{\"@class\":\"com.oracle.coherence.io.json.genson.convert.JsonSerializationTest$ClassWithFieldsAndGetter\""));
  }

  @Test
  public void testSerializeWithPackageAlias() {
    Genson genson = new GensonBuilder().addPackageAlias("test", "com.oracle.coherence.io.json.genson.convert").create();
    String json = genson.serialize(new ClassWithFieldsAndGetter("a", 0));
    assertTrue(json.startsWith("{\"@class\":\"test.JsonSerializationTest$ClassWithFieldsAndGetter\""));
    genson = new GensonBuilder().useClassMetadata(true).create();
    json = genson.serialize(new ClassWithFieldsAndGetter("a", 0));
    assertTrue(json
        .startsWith("{\"@class\":\"com.oracle.coherence.io.json.genson.convert.JsonSerializationTest$ClassWithFieldsAndGetter\""));
  }

  @Test
  public void testSerializeEnum() {
    assertEquals("\"JAVA\"", genson.serialize(Player.JAVA));
  }

  @Test
  public void testSerializeBoxedFloat() {
    assertEquals("2.0", genson.serialize(new Float(2)));
  }

  @Test
  public void testSerializeOptional() {
    assertEquals("{}", genson.serialize(Optional.empty()));
    assertEquals("{}", genson.serialize(Optional.ofNullable(null)));
    assertEquals("{\"value\":\"string\"}", genson.serialize(Optional.of("string")));
    assertEquals("{\"value\":42}", genson.serialize(Optional.of(42L)));
    assertEquals("{\"value\":42.35}", genson.serialize(Optional.of(42.35D)));
    assertEquals("{\"value\":true}", genson.serialize(Optional.of(true)));
    assertEquals("{\"value\":false}", genson.serialize(Optional.of(false)));

    assertEquals("{\"value\":{\"age\":111,\"name\":\"Bilbo Baggins\",\"noField\":\"Bilbo Baggins111\"}}",
                 genson.serialize(Optional.of(new ClassWithFieldsAndGetter("Bilbo Baggins", 111))));
  }

  @Test
  public void testSerializeOptionalInt() {
    assertEquals("{}",
        genson.serialize(OptionalInt.empty()));
    assertEquals("{\"value\":42}",
        genson.serialize(OptionalInt.of(42)));
  }

  @Test
  public void testSerializeOptionalLong() {
    assertEquals("{}",
        genson.serialize(OptionalLong.empty()));
    assertEquals("{\"value\":42}",
        genson.serialize(OptionalLong.of(42L)));
  }

  @Test
  public void testSerializeOptionalDouble() {
    assertEquals("{}",
        genson.serialize(OptionalDouble.empty()));
    assertEquals("{\"value\":42.0}",
        genson.serialize(OptionalDouble.of(42D)));
  }

  @Test
  public void testChar() {
    assertEquals("\"\\u0043\"", genson.serialize('C'));
  }

  @Test
  public void testSerializationCollectionNullElementsWithSkipNullTrue() {
    genson = new GensonBuilder().setSkipNull(true).create();
    List<String> strings = new ArrayList<>();
    strings.add("a");
    strings.add(null);
    strings.add("b");
    assertEquals("[\"a\",null,\"b\"]", genson.serialize(strings));
  }

  private Primitives createPrimitives() {
    return new Primitives(1, new Integer(10), 1.00001, new Double(0.00001), "TEXT ...  HEY!",
      true, new Boolean(false));
  }

  @SuppressWarnings("unused")
  private static class ClassWithFieldsAndGetter {
    private final String name;
    @JsonIgnore
    private String lastName;
    final int age;
    public transient int skipThisField;

    public ClassWithFieldsAndGetter(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public String getName() {
      return name;
    }

    public String getNoField() {
      return name + age;
    }
  }

  @Test
  public void testSerializeNestedPolymorphicType() {
    String expected = "{\"pet\":{\"breed\":\"Boxer\",\"name\":\"Fido\"}}";

    Dog dog = new Dog();
    dog.name = "Fido";
    dog.breed = "Boxer";

    Person p = new Person();
    p.pet = dog;

    assertEquals(expected, genson.serialize(p));
  }

  private static class Pet {
    public String name;
  }

  private static class Dog extends Pet{
    public String breed;
  }

  private static class Person {
    public Pet pet;
  }
}

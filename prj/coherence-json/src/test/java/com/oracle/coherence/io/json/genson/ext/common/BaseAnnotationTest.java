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

package com.oracle.coherence.io.json.genson.ext.common;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.ext.GensonBundle;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public abstract class BaseAnnotationTest {

  protected Genson genson;

  // ---- Test Setup ------------------------------------------------------

  @Before
  public void setUp() {
    genson = new GensonBuilder().withBundle(createTestBundle())
        .useClassMetadata(true)
        .useConstructorWithArguments(true)
        .useIndentation(true)
        .create();
  }
  
  protected abstract GensonBundle createTestBundle();
  
  // ---- Test Methods ------------------------------------------------------

  @Test
  public void testJsonPropertySerializationOnField() {
    final String expected = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnField\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(expected, genson.serialize(new Data.JsonPropertyOnField("Bilbo")));
  }

  @Test
  public void testJsonPropertyDeserializationOnField() {
    final String input = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnField\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(new Data.JsonPropertyOnField("Bilbo"), genson.deserialize(input, Data.JsonPropertyOnField.class));
  }

  @Test
  public void testJsonPropertySerializationOnGetterSetter() {
    final String expected = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnGetterSetter\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(expected, genson.serialize(new Data.JsonPropertyOnGetterSetter("Bilbo")));
  }

  @Test
  public void testJsonPropertyDeserializationOnGetterSetter() {
    final String input = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnGetterSetter\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(new Data.JsonPropertyOnGetterSetter("Bilbo"),
        genson.deserialize(input, Data.JsonPropertyOnGetterSetter.class));
  }

  @Test
  public void testJsonPropertySerializationOnFieldIgnored() {
    final String expected = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnFieldIgnored\"\n" +
        "}";
    assertEquals(expected, genson.serialize(new Data.JsonPropertyOnFieldIgnored("Bilbo")));
  }

  @Test
  public void testJsonPropertyDeserializationOnFieldIgnored() {
    final String input = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnFieldIgnored\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(new Data.JsonPropertyOnFieldIgnored(), genson.deserialize(input, Data.JsonPropertyOnFieldIgnored.class));
  }

  @Test
  public void testJsonPropertySerializationOnGetterIgnoredSetter() {
    final String expected = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnGetterIgnoredSetter\"\n" +
        "}";
    assertEquals(expected, genson.serialize(new Data.JsonPropertyOnGetterIgnoredSetter("Bilbo")));
  }

  @Test
  public void testJsonPropertyDeserializationOnGetterIgnoredSetter() {
    final String input = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnGetterIgnoredSetter\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(new Data.JsonPropertyOnGetterIgnoredSetter("Bilbo"),
        genson.deserialize(input, Data.JsonPropertyOnGetterIgnoredSetter.class));
  }

  @Test
  public void testJsonPropertySerializationOnGetterSetterIgnored() {
    final String expected = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnGetterSetterIgnored\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(expected, genson.serialize(new Data.JsonPropertyOnGetterSetterIgnored("Bilbo")));
  }

  @Test
  public void testJsonPropertyDeserializationOnGetterSetterIgnored() {
    final String input = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnGetterSetterIgnored\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(new Data.JsonPropertyOnGetterSetterIgnored(),
        genson.deserialize(input, Data.JsonPropertyOnGetterSetterIgnored.class));
  }

  @Test
  public void testJsonPropertySerializationOnFieldCustomName() {
    final String expected = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnFieldCustomName\",\n" +
        "  \"n\":\"Bilbo\"\n" +
        "}";
    assertEquals(expected, genson.serialize(new Data.JsonPropertyOnFieldCustomName("Bilbo")));
  }

  @Test
  public void testJsonPropertyDeserializationOnFieldCustomName() {
    final String input = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnFieldCustomName\",\n" +
        "  \"n\":\"Bilbo\"\n" +
        "}";
    assertEquals(new Data.JsonPropertyOnFieldCustomName("Bilbo"), genson.deserialize(input, Data.JsonPropertyOnFieldCustomName.class));
  }

  @Test
  public void testJsonPropertySerializationOnGetterSetterCustomName() {
    final String expected = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnGetterSetterCustomName\",\n" +
        "  \"n\":\"Bilbo\"\n" +
        "}";
    assertEquals(expected, genson.serialize(new Data.JsonPropertyOnGetterSetterCustomName("Bilbo")));
  }

  @Test
  public void testJsonPropertyDeserializationOnGetterSetterCustomName() {
    final String input = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonPropertyOnGetterSetterCustomName\",\n" +
        "  \"n\":\"Bilbo\"\n" +
        "}";
    assertEquals(new Data.JsonPropertyOnGetterSetterCustomName("Bilbo"), genson.deserialize(input, Data.JsonPropertyOnGetterSetterCustomName.class));
  }

  @Test
  public void testJsonCreatorDeserialization() {
    final String input = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonCreatorConstructor\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(new Data.JsonCreatorConstructor("Bilbo"), genson.deserialize(input, Data.JsonCreatorConstructor.class));
  }

  @Test
  public void testJsonCreatorFactoryMethodDeserialization() {
    final String input = "{\n" +
        "  \"@class\":\"com.oracle.coherence.io.json.genson.ext.common.Data$JsonCreatorFactoryMethod\",\n" +
        "  \"name\":\"Bilbo\"\n" +
        "}";
    assertEquals(Data.JsonCreatorFactoryMethod.newInstance("Bilbo"), genson.deserialize(input, Data.JsonCreatorFactoryMethod.class));
  }
}

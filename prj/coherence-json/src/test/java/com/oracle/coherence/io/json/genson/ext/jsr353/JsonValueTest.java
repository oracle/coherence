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

package com.oracle.coherence.io.json.genson.ext.jsr353;

import javax.json.*;

import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.stream.ValueType;

import org.junit.Test;

import static org.junit.Assert.*;

import com.oracle.coherence.io.json.genson.Genson;

import java.util.List;
import java.util.Map;

public class JsonValueTest {
  private final Genson genson = new GensonBuilder().withBundle(new JSR353Bundle()).create();

  @Test
  public void testSerArrayOfLiterals() {
    String json =
      genson.serialize(JSR353Bundle.factory.createArrayBuilder().addNull().add(1.22)
        .add(false).add("str").build());
    assertEquals("[null,1.22,false,\"str\"]", json);
  }

  @Test
  public void testSerObjectAndArray() {
    String json =
      genson.serialize(JSR353Bundle.factory.createObjectBuilder().add("int", 98)
        .addNull("null")
        .add("array", JSR353Bundle.factory.createArrayBuilder().build()).build());
    assertEquals("{\"int\":98,\"null\":null,\"array\":[]}", json);
  }

  @Test
  public void testDeserArrayOfLiterals() {
    JsonArray array = genson.deserialize("[1, 2.2, \"str\", true, null]", JsonArray.class);
    assertEquals(1, array.getInt(0));
    assertEquals(2.2, array.getJsonNumber(1).doubleValue(), 1e-200);
    assertEquals("str", array.getString(2));
    assertEquals(true, array.getBoolean(3));
    assertEquals(true, array.isNull(4));
  }

  @Test
  public void testDeserObject() {
    JsonObject object =
      genson.deserialize("{\"str\":\"a\", \"array\": [1]}", JsonObject.class);
    assertEquals("a", object.getString("str"));
    JsonArray array = (JsonArray) object.get("array");
    assertEquals(1, array.getInt(0));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testJsonValueImmutability() {
    JsonObject object =
      genson.deserialize("{\"str\":\"a\", \"array\": [1]}", JsonObject.class);
    try {
      object.put("str", new GensonJsonString("b"));
      fail("Should've thrown UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e) {
      object.getJsonArray("array").add(new GensonJsonNumber.IntJsonNumber(5));
    }
  }

  @Test
  public void testRoundTripMixBeanAndJsonStructures() {
    JsonArray array = JSR353Bundle.factory.createArrayBuilder().add(1).add(2).build();
    JsonObject object = JSR353Bundle.factory.createObjectBuilder().add("key", "value").addNull("foo").build();
    Bean bean = new Bean();
    bean.setArray(array);
    bean.setObj(object);
    bean.setStr(object.getJsonString("key"));

    String json = genson.serialize(bean);
    Bean actual = genson.deserialize(json, Bean.class);

    assertEquals(array, actual.array);
    assertEquals(object, actual.obj);
    assertEquals(object.getJsonString("key"), actual.str);
  }

  @Test
  public void testPreferJsonpTypes() {
    Genson genson = new GensonBuilder().withBundle(new JSR353Bundle().preferJsonpTypes()).create();
    JsonObject obj = (JsonObject) genson.deserialize("{\n" +
            "  \"array\": [1, 2, 3],\n" +
            "  \"true\": true,\n" +
            "  \"false\": false,\n" +
            "  \"int\": 42,\n" +
            "  \"double\": 98.6,\n" +
            "  \"string\": \"some text\",\n" +
            "  \"obj\": {\"name\": \"Homer\"},\n" +
            "  \"null\": null\n" +
            "}", Object.class);

    assertTrue(obj.get("array") instanceof JsonArray);
    assertEquals(3, obj.getJsonArray("array").size());
    assertTrue(obj.get("int") instanceof JsonNumber);
    assertEquals(42, obj.getJsonNumber("int").intValue());
    assertTrue(obj.get("double") instanceof JsonNumber);
    assertEquals(98.6d, obj.getJsonNumber("double").doubleValue(), 1e-200);
    assertTrue(obj.get("string") instanceof JsonString);
    assertEquals("some text", obj.getJsonString("string").getString());
    assertTrue(obj.get("obj") instanceof JsonObject);
    assertEquals("Homer", obj.getJsonObject("obj").getString("name"));
    assertEquals(JsonValue.TRUE, obj.get("true"));
    assertEquals(JsonValue.FALSE, obj.get("false"));
    assertEquals(JsonValue.NULL, obj.get("null"));
  }


  public static class Bean {
    private JsonString str;
    private JsonObject obj;
    private JsonArray array;

    public JsonString getStr() {
      return str;
    }

    public void setStr(JsonString str) {
      this.str = str;
    }

    public JsonObject getObj() {
      return obj;
    }

    public void setObj(JsonObject obj) {
      this.obj = obj;
    }

    public JsonArray getArray() {
      return array;
    }

    public void setArray(JsonArray array) {
      this.array = array;
    }
  }
}

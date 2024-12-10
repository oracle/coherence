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

import org.junit.Test;

import static org.junit.Assert.*;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.StringReader;

public class JsonReaderTest {
  private GensonJsonReaderFactory factory = new GensonJsonReaderFactory();

  @Test
  public void testReadComplexArray() {
    String json = "[2, \"foo bar\", true, false, 3.8, null, {}, {\"key\": 3}]";
    JsonArray array = factory.createReader(new StringReader(json)).readArray();

    assertComplexArrayEquals(array);
  }

  @Test
  public void testJsonStructureArray() {
    String json = "[]";
    JsonArray array = (JsonArray) factory.createReader(new StringReader(json)).read();

    assertTrue(array.isEmpty());
  }

  @Test
  public void testReadComplexObject() {
    String json = "{\"k1\": 3, \"k2\": 5.9, \"k3\": true, \"k4\": false, \"k5\": null, \"k6\": [5, {\"k\": \"foo bar\"}, null]}";
    JsonObject object = factory.createReader(new StringReader(json)).readObject();

    assertComplexObjectEquals(object);
  }

  @Test
  public void testJsonStructureObject() {
    String json = "{\"k\" : \"foo bar\"}";
    JsonObject object = (JsonObject) factory.createReader(new StringReader(json)).read();

    assertEquals(1, object.size());
    assertEquals("foo bar", object.getString("k"));
  }

  private void assertComplexObjectEquals(JsonObject object) {
    assertEquals(6, object.size());
    assertEquals(3, object.getInt("k1"));
    assertEquals(5.9, object.getJsonNumber("k2").doubleValue(), 1e-2);
    assertEquals(true, object.getBoolean("k3"));
    assertEquals(false, object.getBoolean("k4"));
    assertTrue(object.isNull("k5"));
    assertEquals(3, object.getJsonArray("k6").size());
    assertEquals(5, object.getJsonArray("k6").getInt(0));
    assertEquals("foo bar", object.getJsonArray("k6").getJsonObject(1).getString("k"));
    assertTrue(object.getJsonArray("k6").isNull(2));

  }

  private void assertComplexArrayEquals(JsonArray array) {
    assertEquals(8, array.size());
    assertEquals(2, array.getInt(0));
    assertEquals("foo bar", array.getString(1));
    assertEquals(true, array.getBoolean(2));
    assertEquals(false, array.getBoolean(3));
    assertEquals(3.8, array.getJsonNumber(4).doubleValue(), 1e-2);
    assertTrue(array.isNull(5));
    assertEquals(0, array.getJsonObject(6).size());
    assertEquals(1, array.getJsonObject(7).size());
    assertEquals(3, array.getJsonObject(7).getInt("key"));
  }
}

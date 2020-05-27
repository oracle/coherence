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
import java.math.BigDecimal;

public class JsonBuildersTest {
  private GensonJsonBuilderFactory factory = new GensonJsonBuilderFactory();

  @Test
  public void testComplexArray() {
    JsonArray array = factory.createArrayBuilder().add(1).add("hey").add(
      factory.createArrayBuilder().addNull()
    ).add(true).add(
      factory.createObjectBuilder().add("key", "foo")
    ).build();

    assertEquals(5, array.size());
    assertEquals(1, array.getInt(0));
    assertEquals("hey", array.getString(1));
    assertTrue(array.getBoolean(3));

    assertEquals(1, array.getJsonArray(2).size());
    assertTrue(array.getJsonArray(2).isNull(0));

    assertEquals(1, array.getJsonObject(4).size());
    assertEquals("foo", array.getJsonObject(4).getString("key"));
  }

  @Test
  public void testComplexObject() {
    JsonObject object = factory.createObjectBuilder().addNull("k1").add("k2",
      factory.createArrayBuilder().add(1).add(2).add(3)
    ).add("k3", 2.3).add("k4", new BigDecimal(6.7)).add("k5",
      factory.createObjectBuilder()
    ).build();

    assertEquals(5, object.size());
    assertTrue(object.isNull("k1"));
    assertEquals(3, object.getJsonArray("k2").size());
    assertEquals(1, object.getJsonArray("k2").getInt(0));
    assertEquals(2, object.getJsonArray("k2").getInt(1));
    assertEquals(3, object.getJsonArray("k2").getInt(2));
    assertEquals(2.3, object.getJsonNumber("k3").doubleValue(), 1e-2);
    assertEquals(6.7, object.getJsonNumber("k4").doubleValue(), 1e-2);
    assertEquals(0, object.getJsonObject("k5").size());
  }
}

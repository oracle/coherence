
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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import java.io.StringReader;
import java.io.StringWriter;

public class ImplRoundTripTests {
  @Test
  public void testComplexStructure() {

    JsonObject expected = Json.createObjectBuilder()
      .add("k1", 1)
      .add("k2", false)
      .add("k3", true)
      .add("k4",
        Json.createArrayBuilder()
          .add(3.2e-33)
      ).addNull("k5")
      .add("k6",
        Json.createObjectBuilder()
      ).add("k7",
        Json.createObjectBuilder()
          .add("k1", true)
          .add("k2", "oooo")
          .add("k3", "!")
          .addNull("k4")
      ).add("array",
        Json.createArrayBuilder()
          .add(Json.createObjectBuilder())
          .add(Json.createObjectBuilder())
          .add(Json.createObjectBuilder())
      ).build();

    StringWriter sw = new StringWriter();
    JsonWriter writer = Json.createWriter(sw);
    writer.writeObject(expected);
    writer.close();


    JsonObject actual = Json.createReader(new StringReader(sw.toString())).readObject();

    assertEquals(expected, actual);
  }
}

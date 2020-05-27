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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;

import org.junit.Before;
import org.junit.Test;

import com.oracle.coherence.io.json.genson.stream.JsonWriter;

public class JsonGeneratorTest {
  private JsonGenerator w;
  private StringWriter sw;

  @Before
  public void init() {
    sw = new StringWriter();
    w = new GensonJsonGeneratorFactory().createGenerator(sw);
  }

  @Test(expected = JsonException.class)
  public void testPreventInvalidJsonOutputInObject()
    throws IOException {
    w.writeStartObject().write("must fail");
  }

  @Test(expected = JsonException.class)
  public void testPreventInvalidJsonOutputInArray()
    throws IOException {
    w.writeStartArray().write("key", "must fail");
  }

  @Test
  public void testRootEmptyArray() throws IOException {
    w.writeStartArray().writeEnd().flush();
    assertEquals(sw.toString(), "[]");
  }

  @Test
  public void testRootArrayNumbers() throws IOException {
    w.writeStartArray().write(11).write(0.09).write(0.0009).write(-51.07).writeEnd().flush();
    assertEquals(sw.toString(), "[11,0.09,9.0E-4,-51.07]");
  }

  @Test
  public void testRootArrayStrings() throws IOException {
    w.writeStartArray().write("a").write("b . d").write("\"\\ u").writeEnd().flush();
    String s = "[\"a\",\"b . d\",\"\\\"\\\\ u\"]";
    assertEquals(sw.toString(), s);
  }

  @Test
  public void testRootArrayBooleans() throws IOException {
    w.writeStartArray().write(false).write(true).write(false).writeEnd().flush();
    assertEquals(sw.toString(), "[false,true,false]");
  }

  @Test
  public void testRootObject() throws IOException {
    w.writeStartObject().write("nom", "toto").writeNull("null").write("doub", 10.012)
      .write("int", 7).write("bool", false).writeStartObject("emptyObj").writeEnd()
      .writeStartArray("emptyTab").writeEnd().writeEnd().flush();

    String value =
      "{\"nom\":\"toto\",\"null\":null,\"doub\":10.012,\"int\":7,\"bool\":false,\"emptyObj\":{},\"emptyTab\":[]}";
    assertEquals(sw.toString(), value);
  }

  @Test
  public void testRootObjectWithNested() throws IOException {
    w.writeStartObject().write("nom", "toto").writeNull("null").write("doub", 10.012)
      .write("int", 7).write("bool", false).writeStartObject("nestedObj")
      .write("h1", "fd").write("h2", true).writeStartArray("htab").write(false).write(4)
      .write("s t").writeEnd().writeEnd().writeStartArray("nestedTab").write(8)
      .writeStartArray().write("hey").write(2.29).write("bye").writeEnd()
      .writeStartObject().write("t1", true).write("t2", "kk").writeNull("t3").writeEnd()
      .writeEnd().writeEnd().flush();

    String value =
      "{\"nom\":\"toto\",\"null\":null,\"doub\":10.012,\"int\":7,\"bool\":false,"
        + "\"nestedObj\":{\"h1\":\"fd\",\"h2\":true,\"htab\":[false,4,\"s t\"]},"
        + "\"nestedTab\":[8,[\"hey\",2.29,\"bye\"],{\"t1\":true,\"t2\":\"kk\",\"t3\":null}]}";
    assertEquals(sw.toString(), value);
  }

  @Test(expected = JsonException.class)
  public void testExpectNameInObject() throws IOException {
    w.writeStartObject().writeStartArray();
  }

  @Test
  public void testPrettyPrint() throws IOException {
    String expected = "[\n  2,\n  false,\n  {\n    \"name\":\"toto\",\n    \"uu\":null\n  }\n]";
    StringWriter sw = new StringWriter();
    JsonGenerator writer = new GensonJsonGenerator(new JsonWriter(sw, false, false, true));
    writer.writeStartArray().write(2).write(false).writeStartObject().write("name", "toto")
      .writeNull("uu").writeEnd().writeEnd().flush();
    writer.flush();
    writer.close();
    assertEquals(expected, sw.toString());
  }

  @Test
  public void testEscapedString() throws Exception {
    w.writeStartArray().write("\u0000").writeEnd();
    w.close();

    assertEquals("[\"\\u0000\"]", sw.toString());
  }

  @Test(expected = JsonGenerationException.class)
  public void testGenerationException1()
    throws Exception {
    w.writeStartObject().writeStartObject();
  }

  @Test(expected = JsonGenerationException.class)
  public void testGenerationException2()
    throws Exception {
    w.writeStartObject().writeStartArray();
  }

  @Test
  public void testGeneratorArrayDouble() throws Exception {
    w.writeStartArray();
    try {
      w.write(Double.NaN);
      fail("JsonGenerator.write(Double.NaN) should produce NumberFormatException");
    } catch (NumberFormatException ne) {
      // expected
    }
    try {
      w.write(Double.POSITIVE_INFINITY);
      fail("JsonGenerator.write(Double.POSITIVE_INIFINITY) should produce NumberFormatException");
    } catch (NumberFormatException ne) {
      // expected
    }
    try {
      w.write(Double.NEGATIVE_INFINITY);
      fail("JsonGenerator.write(Double.NEGATIVE_INIFINITY) should produce NumberFormatException");
    } catch (NumberFormatException ne) {
      // expected
    }
    w.writeEnd();
    w.close();
  }

  @Test
  public void testGeneratorWIthJsonValue() {
    JsonArray array =
      Json.createArrayBuilder().add(1)
        .add(Json.createObjectBuilder().add("key", "value").addNull("nullValue"))
        .add(2.2).build();

    w.writeStartObject().write("jsonArray", array).writeEnd().close();
    assertEquals("{\"jsonArray\":[1,{\"key\":\"value\",\"nullValue\":null},2.2]}",
      sw.toString());
  }
}

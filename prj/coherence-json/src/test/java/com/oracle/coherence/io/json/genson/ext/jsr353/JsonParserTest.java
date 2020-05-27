

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.json.*;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import java.io.*;
import java.util.*;

@RunWith(value = org.junit.runners.Parameterized.class)
public class JsonParserTest {
  private boolean strictDoubleParse;

  public JsonParserTest(boolean strictDoubleParse) {
    this.strictDoubleParse = strictDoubleParse;
  }

  // lets run all the tests for strict double parse and custom, but also with metadata and without
  @Parameters
  public static Collection<Boolean[]> data() {
    return Arrays.asList(new Boolean[]{true}, new Boolean[]{false});
  }

  @Test
  public void testRootArrayAndNestedObjects() throws IOException {
    JsonParser parser =
      parserFor("[{},      " + "   []," + "    [\"a a\", -9.9909], " + "false, " + "{"
        + "\"nom\": \"toto\", " + "\"tab\":[5,6,7], "
        + "\"nestedObj\":        {\"prenom\":\"titi\"}" + "}" + "]");

    assertTrue(parser.hasNext());
    assertEquals(Event.START_ARRAY, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.START_OBJECT, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.END_OBJECT, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.START_ARRAY, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.END_ARRAY, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.START_ARRAY, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.VALUE_STRING, parser.next());

    assertTrue(parser.hasNext());
    assertEquals("a a", parser.getString());

    assertTrue(parser.hasNext());
    assertEquals(Event.VALUE_NUMBER, parser.next());

    assertTrue(parser.hasNext());
    assertEquals("-9.9909", parser.getBigDecimal().toString());

    assertTrue(parser.hasNext());
    assertEquals(Event.END_ARRAY, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.VALUE_FALSE, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.START_OBJECT, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.KEY_NAME, parser.next());

    assertTrue(parser.hasNext());
    assertEquals("nom", parser.getString());

    assertTrue(parser.hasNext());
    assertEquals(Event.VALUE_STRING, parser.next());

    assertTrue(parser.hasNext());
    assertEquals("toto", parser.getString());

    assertTrue(parser.hasNext());
    assertEquals(Event.KEY_NAME, parser.next());

    assertTrue(parser.hasNext());
    assertEquals("tab", parser.getString());

    assertTrue(parser.hasNext());
    assertEquals(Event.START_ARRAY, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.VALUE_NUMBER, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(5, parser.getLong());

    assertTrue(parser.hasNext());
    assertEquals(Event.VALUE_NUMBER, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(6, parser.getLong());

    assertTrue(parser.hasNext());
    assertEquals(Event.VALUE_NUMBER, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(7, parser.getLong());

    assertTrue(parser.hasNext());
    assertEquals(Event.END_ARRAY, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.KEY_NAME, parser.next());

    assertTrue(parser.hasNext());
    assertEquals("nestedObj", parser.getString());

    assertTrue(parser.hasNext());
    assertEquals(Event.START_OBJECT, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.KEY_NAME, parser.next());

    assertTrue(parser.hasNext());
    assertEquals("prenom", parser.getString());

    assertTrue(parser.hasNext());
    assertEquals(Event.VALUE_STRING, parser.next());

    assertTrue(parser.hasNext());
    assertEquals("titi", parser.getString());

    assertTrue(parser.hasNext());
    assertEquals(Event.END_OBJECT, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.END_OBJECT, parser.next());

    assertTrue(parser.hasNext());
    assertEquals(Event.END_ARRAY, parser.next());
  }

  @Test(expected = NoSuchElementException.class)
  public void testIllegalOperationCallNext()
    throws IOException {
    JsonParser parser = parserFor("[1,2]");
    assertEquals(Event.START_ARRAY, parser.next());
    assertEquals(Event.VALUE_NUMBER, parser.next());
    assertEquals(1, parser.getInt());
    assertEquals(Event.VALUE_NUMBER, parser.next());
    assertEquals(2, parser.getInt());
    assertEquals(Event.END_ARRAY, parser.next());
    parser.next();
  }

  @Test(expected = JsonException.class)
  public void testIncompleteSource() throws IOException {
    JsonParser parser = parserFor("[1,");
    assertEquals(Event.START_ARRAY, parser.next());
    assertEquals(Event.VALUE_NUMBER, parser.next());
    parser.next();
  }

  private JsonParser parserFor(String json) {
    Map<String, Boolean> config = new HashMap<String, Boolean>();
    config.put(GensonJsonParser.STRICT_DOUBLE_PARSE, strictDoubleParse);
    return new GensonJsonParserFactory(config).createParser(new StringReader(json));
  }
}

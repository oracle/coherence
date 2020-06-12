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

package com.oracle.coherence.io.json.genson.stream;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.coherence.io.json.genson.stream.JsonReader;
import com.oracle.coherence.io.json.genson.stream.ValueType;

import static org.junit.Assert.*;

@RunWith(value = org.junit.runners.Parameterized.class)
public class JsonReaderTest {
  private boolean strictDoubleParse;
  private boolean readMetadata;

  public JsonReaderTest(boolean strictDoubleParse, boolean readMetadata) {
    this.strictDoubleParse = strictDoubleParse;
    this.readMetadata = readMetadata;
  }

  // lets run all the tests for strict double parse and custom, but also with metadata and without
  @Parameters
  public static Collection<Boolean[]> data() {
    return Arrays.asList(new Boolean[]{true, true}, new Boolean[]{true, false},
      new Boolean[]{false, true}, new Boolean[]{false, false});
  }

  @Test public void testReadDoubleAsFloat() {
    assertEquals(-1.1f, createReader("-1.1").valueAsFloat(), 0);
  }

  @Test public void testReadManyValuesNotEnclosedInArrayWithSameReader() {
    StringReader strReader = new StringReader("{\"k1\":1}\n{\"k2\":2}{\"k3\":3}");
    JsonReader reader = new JsonReader(strReader, false, false);
    int i = 1;
    while(reader.hasNext()) {
      reader.next();
      reader.beginObject();
      reader.next();
      assertEquals(reader.name(), "k" + i);
      assertEquals(reader.valueAsInt(), i);
      i++;
      reader.endObject();
    }
    assertEquals(i, 4);
  }

  @Test
  public void testParseLineComment() throws IOException {
    JsonReader reader = new JsonReader("// hey \n" +
      "[ // you, // ! \n " +
      "1 // aaa \n " +
      ", /* ooo */ {\"key\": // you \n " +
      "/*cccc*/ 3 //aaaa\n " +
      "}/**/] //bb/**/");
    reader.beginArray().hasNext();
    reader.next();
    assertEquals(1, reader.valueAsInt());
    assertTrue(reader.hasNext());
    reader.next();
    reader.beginObject();
    reader.next();
    assertEquals("key", reader.name());
    assertEquals(3, reader.valueAsInt());
    assertFalse(reader.hasNext());
    reader.endObject();
    assertFalse(reader.hasNext());
    reader.endArray();
    assertFalse(reader.hasNext());
  }

  @Test
  public void testParsingErrorPositionSameRow() throws IOException {
    @SuppressWarnings("resource")
    JsonReader reader = createReader("[" + "}");
    try {
      reader.beginArray().endArray();
      fail();
    } catch (JsonStreamException e) {
      assertEquals(0, e.getRow());
      assertEquals(1, e.getColumn());
    }
  }

  @Test
  public void testParsingErrorPositionDifferentRow() throws IOException {
    @SuppressWarnings("resource")
    JsonReader reader = createReader("  [\n\n    \n" + "}");
    try {
      reader.beginArray().endArray();
      fail();
    } catch (JsonStreamException e) {
      assertEquals(3, e.getRow());
      assertEquals(1, e.getColumn());
    }
  }

  @Test
  public void testParsingErrorPositionDifferentRowWithContent() throws IOException {
    @SuppressWarnings("resource")
    JsonReader reader = createReader("  [1, 2\n, \"aa vb\",\n4330833    \n}");
    try {
      reader.beginArray().endArray();
      fail();
    } catch (JsonStreamException e) {
      assertEquals(0, e.getRow());
      assertEquals(3, e.getColumn());
    }
  }

  // must produce same result as testParsingErrorPositionDifferentRow
  @Test
  public void testParsingErrorPositionDifferentRowWithContent2() throws IOException {
    @SuppressWarnings("resource")
    JsonReader reader = createReader("  [1, 2\n, \"aa vb\",\n4330833    \n}");
    try {
      reader.beginArray().next();
      reader.next();
      reader.next();
      reader.next();
      reader.endArray();
      fail();
    } catch (JsonStreamException e) {
      assertEquals(3, e.getRow());
      assertEquals(1, e.getColumn());
    }
  }

  @Test
  public void testParsingErrorPositionLargeInput() throws IOException {
    // 2048 is the buffer size, this will allow us to test position
    // information for large input that needs to be buffered
    char[] in = new char[2048 + 7];
    in[0] = '[';
    for (int i = 1; i < 2046; i++) in[i] = '1';
    in[2046] = ',';
    in[2047] = '\n';
    in[2048] = '3';
    in[2049] = '3';
    in[2050] = ',';
    in[2051] = '\n';
    in[2052] = '5';
    in[2053] = 'x';
    in[2054] = ']';
    /* looks like :
		 * [11111.....111,
		 * 3,
		 * 5x]
		 */

    @SuppressWarnings("resource")
    JsonReader reader = new JsonReader(new CharArrayReader(in), strictDoubleParse, readMetadata);
    try {
      for (reader.beginArray(); reader.hasNext(); ) {
        reader.next();
        reader.valueAsDouble();
      }
      reader.endArray();
      fail();
    } catch (JsonStreamException e) {
      assertEquals(2, e.getRow());
      assertEquals(1, e.getColumn());
    }
  }

  @Test
  public void testReader() throws IOException {
    String src = "{\"nom\" : \"toto titi, tu\", \"prenom\" : \"albert  \", \"entier\" : 1322.6}";
    JsonReader reader = createReader(src);
    reader.beginObject();

    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.STRING);
    assertEquals(reader.name(), "nom");
    assertEquals(reader.valueAsString(), "toto titi, tu");
    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.STRING);
    assertEquals(reader.name(), "prenom");
    assertEquals(reader.valueAsString(), "albert  ");
    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.DOUBLE);
    assertEquals(reader.name(), "entier");
    assertEquals(reader.valueAsString(), "1322.6");
    assertFalse(reader.hasNext());

    reader.endObject();
    reader.close();
  }

  @Test
  public void testTokenTypesAndHasNext() throws IOException {
    String src = "[{\"a\": 1, \"b\": \"a\", \"c\":1.1,\"d\":null,\"e\":false, \"f\":[]},[1, 1.1], null, false, true, \"tt\"]";
    JsonReader reader = createReader(src);

    reader.beginArray();

    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.OBJECT);
    assertEquals(reader.beginObject().next(), ValueType.INTEGER);

    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.STRING);

    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.DOUBLE);

    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.NULL);

    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.BOOLEAN);

    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.ARRAY);
    assertFalse(reader.beginArray().hasNext());
    assertTrue(reader.endArray().endObject().hasNext());

    assertEquals(reader.next(), ValueType.ARRAY);
    assertTrue(reader.beginArray().hasNext());
    assertEquals(reader.next(), ValueType.INTEGER);
    assertEquals(reader.next(), ValueType.DOUBLE);
    assertFalse(reader.hasNext());

    assertTrue(reader.endArray().hasNext());
    assertEquals(reader.next(), ValueType.NULL);
    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.BOOLEAN);
    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.BOOLEAN);
    assertTrue(reader.hasNext());
    assertEquals(reader.next(), ValueType.STRING);
    assertFalse(reader.hasNext());

    reader.endArray();
    reader.close();
  }

  @Test
  public void testReadDoubles() throws IOException {
    String src = "[" + String.valueOf(Double.MAX_VALUE) + ","
      + String.valueOf(Double.MIN_VALUE) + "," + Double.MAX_VALUE + "" + 1 + ","
      + -Double.MAX_VALUE + "" + 1 + "]";
    JsonReader reader = createReader(src);
    reader.beginArray();
    reader.next();
    assertTrue(Double.MAX_VALUE == reader.valueAsDouble());
    reader.next();
    assertTrue(Double.MIN_VALUE == reader.valueAsDouble());
    reader.next();
    assertTrue(Double.isInfinite(reader.valueAsDouble()));
    assertTrue(Double.MAX_VALUE < reader.valueAsDouble());
    reader.next();
    assertTrue(Double.isInfinite(reader.valueAsDouble()));
    assertTrue(0 > reader.valueAsDouble());
    reader.endArray();
    reader.close();
  }

  @Test
  public void testNumberConversion() throws IOException {
    JsonReader reader = createReader("[1, 1.1, 1e-32, 12345678901234567890]");
    reader.beginArray();

    reader.next();
    assertEquals(1, reader.valueAsInt());
    assertEquals(1l, reader.valueAsLong());
    assertEquals(1d, reader.valueAsDouble(), 0);
    assertEquals(1f, reader.valueAsFloat(), 0);

    reader.next();
    assertEquals(1, reader.valueAsInt());
    assertEquals(1l, reader.valueAsLong());
    assertEquals(1.1, reader.valueAsDouble(), 0);
    assertEquals(1.1f, reader.valueAsFloat(), 0);

    reader.next();
    assertEquals(0, reader.valueAsInt());
    assertEquals(0l, reader.valueAsLong());
    assertEquals(1e-32, reader.valueAsDouble(), 1e-33);
    assertEquals(1e-32f, reader.valueAsFloat(), 0);

    reader.next();
    try {
      // it must throw an numberformatexception as the integer part value will overflow ints
      // capacity
      reader.valueAsInt();
      fail();
    } catch (NumberFormatException nfe) {
    }
    try {
      // it must throw an numberformatexception as the integer part value will overflow longs
      // capacity
      reader.valueAsLong();
      fail();
    } catch (NumberFormatException nfe) {
    }
    assertEquals(12345678901234567890d, reader.valueAsDouble(), 0);
    assertEquals(12345678901234567890f, reader.valueAsFloat(), 0);
    reader.endArray();
    reader.close();
  }

  // bugs signaled by Jesse Wilson
  @Test
  public void testReadExtremeDoubles() throws IOException {
    String src = "[1111e-4, 1e-4, 0.11111111111111111111, 2.2250738585072014e-308, 1.2345678901234567e22]";
    JsonReader reader = createReader(src);
    reader.beginArray();
    reader.next();
    assertEquals(1111e-4, reader.valueAsDouble(), 0);
    reader.next();
    assertEquals(1e-4, reader.valueAsDouble(), 0);
    reader.next();
    assertEquals(0.11111111111111111111, reader.valueAsDouble(), 0);
    reader.next();
    assertEquals(2.2250738585072014e-308, reader.valueAsDouble(), 0);
    reader.next();
    assertEquals(1.2345678901234567e22, reader.valueAsDouble(), 0);
    reader.endArray();
    reader.close();
  }

  @Test
  public void testReadLong() throws IOException {
    String src = "[" + String.valueOf(Long.MAX_VALUE) + "," + String.valueOf(Long.MIN_VALUE)
      + "," + 999999999999999990l + "]";
    JsonReader reader = createReader(src);
    reader.beginArray();
    reader.next();
    assertTrue(Long.MAX_VALUE == reader.valueAsLong());
    reader.next();
    assertTrue(Long.MIN_VALUE == reader.valueAsLong());
    reader.next();
    assertTrue(999999999999999990l == reader.valueAsLong());
    reader.endArray();
    reader.close();
  }

  @Test
  public void testPrimitivesArray() throws IOException {
    JsonReader reader = createReader("[\"\\u0019\",\"abcde ..u\",null,12222.0101,true,false,9.0E-7]");

    reader.beginArray();

    reader.next();
    assertEquals(reader.valueAsString(), "\u0019");
    reader.next();
    assertEquals(reader.valueAsString(), "abcde ..u");
    reader.next();
    assertEquals(reader.valueAsString(), null);
    reader.next();
    assertEquals(reader.valueAsString(), "12222.0101");
    reader.next();
    assertEquals(reader.valueAsString(), "true");
    reader.next();
    assertEquals(reader.valueAsString(), "false");
    reader.next();
    assertEquals(reader.valueAsString(), "9.0E-7");
    assertTrue(reader.valueAsDouble() == 0.0000009);

    assertFalse(reader.hasNext());

    reader.endArray();
    reader.close();
  }

  @Test
  public void testPrimitivesObject() throws IOException {
    String src = "{\"a\":1.0,\"b\":\"abcde ..u\",\"c\":null,\"d\":12222.0101,\"e\":true,\"f\":false,\"h\":-0.9}";
    JsonReader reader = createReader(src);

    reader.beginObject();

    reader.next();
    assertEquals(reader.name(), "a");
    assertEquals(reader.valueAsString(), "1.0");
    reader.next();
    assertEquals(reader.name(), "b");
    assertEquals(reader.valueAsString(), "abcde ..u");
    reader.next();
    assertEquals(reader.name(), "c");
    assertEquals(reader.valueAsString(), null);
    reader.next();
    assertEquals(reader.name(), "d");
    assertEquals(reader.valueAsString(), "12222.0101");
    reader.next();
    assertEquals(reader.name(), "e");
    assertEquals(reader.valueAsString(), "true");
    reader.next();
    assertEquals(reader.name(), "f");
    assertEquals(reader.valueAsString(), "false");
    reader.next();
    assertEquals(reader.name(), "h");
    assertEquals(reader.valueAsString(), "-0.9");

    reader.endObject();
    reader.close();
  }

  @Test
  public void testEmptyArrayAndObjects() throws IOException {
    JsonReader reader = createReader("[{},[]]");

    assertTrue(reader.beginArray().hasNext());
    assertEquals(reader.next(), ValueType.OBJECT);
    assertFalse(reader.beginObject().hasNext());
    assertTrue(reader.endObject().hasNext());
    assertEquals(reader.next(), ValueType.ARRAY);
    assertFalse(reader.beginArray().hasNext());
    assertFalse(reader.endArray().hasNext());
    reader.endArray();
    reader.close();
  }

  @Test
  public void testRootArrayAndNestedObjects() throws IOException {
    String src = "[{},      " + "	[]," + "	[\"a a\", -9.9909], " + "false, " + "{"
      + "	\"nom\": \"toto\", " + "\"tab\":[5,6,7], "
      + "\"nestedObj\":	   	 {\"prenom\":\"titi\"}" + "}" + "]";
    JsonReader reader = createReader(src);

    assertTrue(reader.beginArray().hasNext());

    reader.next();
    assertFalse(reader.beginObject().hasNext());
    assertTrue(reader.endObject().hasNext());

    reader.next();
    assertFalse(reader.beginArray().hasNext());
    assertTrue(reader.endArray().hasNext());

    reader.next();
    assertTrue(reader.beginArray().hasNext());
    reader.next();
    assertEquals(reader.valueAsString(), "a a");
    assertTrue(reader.hasNext());
    reader.next();
    assertEquals(reader.valueAsString(), "-9.9909");
    assertFalse(reader.hasNext());
    reader.endArray();

    reader.next();
    assertEquals(reader.valueAsString(), "false");
    assertTrue(reader.hasNext());

    reader.next();
    assertTrue(reader.beginObject().hasNext());
    reader.next();
    assertEquals(reader.name(), "nom");
    assertEquals(reader.valueAsString(), "toto");

    assertTrue(reader.hasNext());
    reader.next();
    assertEquals(reader.name(), "tab");
    assertTrue(reader.beginArray().hasNext());
    reader.next();
    assertEquals(reader.valueAsString(), "5");
    assertTrue(reader.hasNext());
    reader.next();
    assertEquals(reader.valueAsString(), "6");
    assertTrue(reader.hasNext());
    reader.next();
    assertEquals(reader.valueAsString(), "7");
    assertFalse(reader.hasNext());
    reader.endArray();

    reader.next();
    assertEquals(reader.name(), "nestedObj");
    assertTrue(reader.beginObject().hasNext());
    reader.next();
    assertEquals(reader.name(), "prenom");
    assertEquals(reader.valueAsString(), "titi");
    reader.endObject();

    reader.endObject();

    reader.endArray();
    reader.close();
  }

  @Test
  public void testSkipValue() throws IOException {
    String src = "{\"a\":[], \"b\":{}, \"c\": [{\"c\":null, \"d\":121212.02}, 4, null], \"e\":1234, \"end\":\"the end\"}";
    JsonReader reader = createReader(src);

    reader.beginObject();

    reader.next();
    reader.skipValue();

    assertTrue(reader.hasNext());
    reader.next();
    assertEquals(reader.name(), "b");
    reader.skipValue();

    assertTrue(reader.hasNext());
    reader.next();
    assertEquals(reader.name(), "c");
    reader.skipValue();

    assertTrue(reader.hasNext());
    reader.next();
    assertEquals(reader.name(), "e");
    reader.skipValue();

    assertTrue(reader.hasNext());
    reader.next();
    assertEquals(reader.name(), "end");

    reader.endObject();
    reader.close();
  }

  @Test
  public void testIllegalReadObjectInstedOfArray() throws IOException {
    JsonReader reader = createReader("[1,2]");
    try {
      reader.beginObject();
      fail();
    } catch (JsonStreamException ise) {
    }
    reader.close();
  }

  @Test
  public void testIllegalOperationCallNext() throws IOException {
    JsonReader reader = createReader("[1,2]");
    try {
      reader.beginArray();
      reader.next();
      reader.next();
      reader.next();
      fail();
    } catch (JsonStreamException ise) {
    }
    reader.close();
  }

  @Test
  public void testIncompleteSource() throws IOException {
    JsonReader reader = createReader("[1,");
    try {
      reader.beginArray();
      reader.next();
      reader.next();
      fail();
    } catch (JsonStreamException ioe) {
    }
    reader.close();
  }

  @Test
  public void testMetadata() throws IOException {
    String src = "{\"@class\"	: \"theclass\"" + ",     \"@author\":\"me\""
      + ", \"@comment\":\"no comment\"" + ", \"obj\" :      	"
      + "			{\"@class\":\"anotherclass\"}}";
    JsonReader reader = new JsonReader(new StringReader(src), false, true);
    assertTrue(reader.beginObject().hasNext());
    assertEquals("theclass", reader.metadata("class"));
    assertEquals("me", reader.metadata("author"));
    assertEquals("no comment", reader.metadata("comment"));
    reader.next();
    reader.beginObject();
    assertNull(reader.metadata("author"));
    assertEquals("anotherclass", reader.metadata("class"));
    assertFalse(reader.hasNext());
    reader.endObject().endObject();
    reader.close();
  }

  @Test
  public void testMultipleCallsToNextObjectMetadata() throws IOException {
    String src = "{\"@class\"	: \"theclass\"" + ",     \"@author\":\"me\""
      + ", \"@comment\":\"no comment\"}";
    JsonReader reader = new JsonReader(new StringReader(src), false, true);
    assertEquals("theclass", reader.nextObjectMetadata().nextObjectMetadata().metadata("class"));
    assertEquals("theclass", reader.nextObjectMetadata().metadata("class"));
    assertEquals("no comment", reader.metadata("comment"));
    assertEquals("no comment", reader.nextObjectMetadata().metadata("comment"));
    assertEquals("me", reader.beginObject().metadata("author"));
    reader.endObject();
    reader.close();
  }

  @Test
  public void testReadMalformedJson() throws IOException {
    JsonReader reader = createReader("");
    try {
      reader.beginObject();
      fail();
    } catch (JsonStreamException ise) {
    }
    reader.close();
  }

  // TODO this test fails for the moment as we do not handle values that overflow the buffer
  // capacity
  public void testReadOverflowingExtremlyLongNumber() throws IOException {
    char[] array = new char[4100];
    array[0] = '[';
    array[array.length - 1] = ']';
    for (int i = 1; i < array.length - 1; i++) {
      array[i] = (char) (i % 10 + 48);
    }
    JsonReader reader = createReader(new String(array));
    reader.beginArray();
    reader.next();
    reader.endArray();
    reader.close();
  }

  @Test
  public void testSkipValueDeepObject() throws IOException {
    JsonReader reader = new JsonReader(new StringReader("{\"a\":{}}"), strictDoubleParse, readMetadata);
    reader.skipValue();
    reader.close();
  }

  private JsonReader createReader(String json) {
    return new JsonReader(new StringReader(json), strictDoubleParse, readMetadata);
  }
}

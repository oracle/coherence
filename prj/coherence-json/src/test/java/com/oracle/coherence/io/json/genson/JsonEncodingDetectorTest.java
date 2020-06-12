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

package com.oracle.coherence.io.json.genson;

import com.oracle.coherence.io.json.genson.stream.JsonReader;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class JsonEncodingDetectorTest {
  private final List<String> encodings = Arrays.asList("UTF-8", "UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE");

  @Test public void shouldDetectCorrectEncodingUsingJsonSpec() throws IOException {
    for (String expectedEncoding : encodings) {
      checkCorrectRoundTrip(makeReader("[92]".getBytes(expectedEncoding)));
    }
  }

  @Test public void shouldDetectCorrectEncodingFromBOM() throws IOException {
    for (String expectedEncoding : encodings) {
      checkCorrectRoundTrip(makeReader("\uFEFF[92]".getBytes(expectedEncoding)));
    }
  }

  @Test public void shouldNotFailOnEmptyStream() throws IOException {
    for (String expectedEncoding : encodings) {
      JsonReader reader = makeReader("".getBytes(expectedEncoding));
      assertFalse(reader.hasNext());
    }
  }

  @Test public void shouldNotFailOnEmptyStreamWithBOM() throws IOException {
    for (String expectedEncoding : encodings) {
      JsonReader reader = makeReader("\uFEFF".getBytes(expectedEncoding));
      assertFalse(reader.hasNext());
    }
  }


  private void checkCorrectRoundTrip(JsonReader reader) {
    reader.beginArray();
    reader.next();
    assertEquals(92, reader.valueAsLong());
    reader.endArray();
  }

  private JsonReader makeReader(byte[] bytes) {
    try {
      return new JsonReader(
          new EncodingAwareReaderFactory().createReader(new ByteArrayInputStream(bytes)), false, false
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

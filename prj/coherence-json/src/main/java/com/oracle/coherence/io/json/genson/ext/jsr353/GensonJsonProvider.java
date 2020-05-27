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
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

public class GensonJsonProvider extends JsonProvider {
  private final GensonJsonGeneratorFactory generatorFactory = new GensonJsonGeneratorFactory();
  private final GensonJsonParserFactory parserFactory = new GensonJsonParserFactory();
  private final GensonJsonReaderFactory readerFactory = new GensonJsonReaderFactory(
    Collections.singletonMap(GensonJsonParser.STRICT_DOUBLE_PARSE, true)
  );
  private final GensonJsonWriterFactory writerFactory = new GensonJsonWriterFactory();
  private final GensonJsonBuilderFactory builderFactory = new GensonJsonBuilderFactory();

  @Override
  public JsonParser createParser(Reader reader) {
    return parserFactory.createParser(reader);
  }

  @Override
  public JsonParser createParser(InputStream in) {
    return parserFactory.createParser(in);
  }

  @Override
  public JsonParserFactory createParserFactory(Map<String, ?> config) {
    return new GensonJsonParserFactory(config);
  }

  @Override
  public JsonGenerator createGenerator(Writer writer) {
    return generatorFactory.createGenerator(writer);
  }

  @Override
  public JsonGenerator createGenerator(OutputStream out) {
    return generatorFactory.createGenerator(out);
  }

  @Override
  public JsonGeneratorFactory createGeneratorFactory(Map<String, ?> config) {
    return new GensonJsonGeneratorFactory(config);
  }

  @Override
  public JsonReader createReader(Reader reader) {
    return readerFactory.createReader(reader);
  }

  @Override
  public JsonReader createReader(InputStream in) {
    return readerFactory.createReader(in);
  }

  @Override
  public JsonWriter createWriter(Writer writer) {
    return writerFactory.createWriter(writer);
  }

  @Override
  public JsonWriter createWriter(OutputStream out) {
    return writerFactory.createWriter(out);
  }

  @Override
  public JsonWriterFactory createWriterFactory(Map<String, ?> config) {
    return new GensonJsonWriterFactory(config);
  }

  @Override
  public JsonReaderFactory createReaderFactory(Map<String, ?> config) {
    return new GensonJsonReaderFactory(config);
  }

  @Override
  public JsonObjectBuilder createObjectBuilder() {
    return builderFactory.createObjectBuilder();
  }

  @Override
  public JsonArrayBuilder createArrayBuilder() {
    return builderFactory.createArrayBuilder();
  }

  @Override
  public JsonBuilderFactory createBuilderFactory(Map<String, ?> config) {
    return builderFactory;
  }
}

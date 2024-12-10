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


import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

import com.oracle.coherence.io.json.genson.EncodingAwareReaderFactory;
import com.oracle.coherence.io.json.genson.stream.JsonReader;
import com.oracle.coherence.io.json.genson.stream.JsonWriter;

public class GensonJsonParserFactory implements JsonParserFactory {
  private final boolean strictDoubleParse;
  private final EncodingAwareReaderFactory encodingAwareReaderFactory = new EncodingAwareReaderFactory();

  public GensonJsonParserFactory() {
    strictDoubleParse = false;
  }

  public GensonJsonParserFactory(Map<String, ?> config) {
    strictDoubleParse = JSR353Bundle.toBoolean(config, GensonJsonParser.STRICT_DOUBLE_PARSE);
  }

  @Override
  public JsonParser createParser(Reader reader) {
    return new GensonJsonParser(new JsonReader(reader, strictDoubleParse, false));
  }

  @Override
  public JsonParser createParser(InputStream in) {

    try {
      return new GensonJsonParser(
          new JsonReader(encodingAwareReaderFactory.createReader(in), strictDoubleParse, false)
      );
    } catch (IOException e) {
      throw new JsonException("Failed to detect encoding");
    }
  }

  @Override
  public JsonParser createParser(InputStream in, Charset charset) {
    return new GensonJsonParser(new JsonReader(new InputStreamReader(in, charset), strictDoubleParse, false));
  }

  @Override
  public JsonParser createParser(JsonObject obj) {
    return parserForJsonStructure(obj);
  }

  @Override
  public JsonParser createParser(JsonArray array) {
    return parserForJsonStructure(array);
  }

  // TODO: OK I know pretty slow to do that, but what a pain to also implement this...will do it latter
  private JsonParser parserForJsonStructure(JsonStructure jsonStructure) {
    StringWriter sw = new StringWriter();
    GensonJsonGenerator generator = new GensonJsonGenerator(new JsonWriter(sw));
    generator.write(jsonStructure);
    generator.flush();

    return createParser(new StringReader(sw.toString()));
  }

  @Override
  public Map<String, ?> getConfigInUse() {
    Map<String, Boolean> config = new HashMap<String, Boolean>();
    config.put(GensonJsonParser.STRICT_DOUBLE_PARSE, strictDoubleParse);
    return config;
  }

}

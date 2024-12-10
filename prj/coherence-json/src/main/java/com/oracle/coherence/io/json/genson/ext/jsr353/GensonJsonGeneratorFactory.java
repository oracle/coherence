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


import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonException;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import com.oracle.coherence.io.json.genson.stream.JsonWriter;

public class GensonJsonGeneratorFactory implements JsonGeneratorFactory {
  private final boolean prettyPrint;
  private final boolean htmlSafe;
  private final boolean skipNull;

  public GensonJsonGeneratorFactory() {
    prettyPrint = false;
    htmlSafe = false;
    skipNull = false;
  }

  public GensonJsonGeneratorFactory(Map<String, ?> config) {
    prettyPrint = JSR353Bundle.toBoolean(config, JsonGenerator.PRETTY_PRINTING);
    htmlSafe = JSR353Bundle.toBoolean(config, GensonJsonGenerator.HTML_SAFE);
    skipNull = JSR353Bundle.toBoolean(config, GensonJsonGenerator.SKIP_NULL);
  }

  @Override
  public JsonGenerator createGenerator(Writer writer) {
    return new GensonJsonGenerator(new JsonWriter(writer, skipNull, htmlSafe, prettyPrint));
  }

  @Override
  public JsonGenerator createGenerator(OutputStream out) {
    try {
      return new GensonJsonGenerator(new JsonWriter(new OutputStreamWriter(out, "UTF-8"),
        skipNull, htmlSafe, prettyPrint));
    } catch (UnsupportedEncodingException e) {
      throw new JsonException("Charset UTF-8 is not supported.", e);
    }
  }

  @Override
  public JsonGenerator createGenerator(OutputStream out, Charset charset) {
    return new GensonJsonGenerator(new JsonWriter(new OutputStreamWriter(out), skipNull,
      htmlSafe, prettyPrint));
  }

  @Override
  public Map<String, ?> getConfigInUse() {
    Map<String, Boolean> config = new HashMap<String, Boolean>();
    config.put(JsonGenerator.PRETTY_PRINTING, prettyPrint);
    config.put(GensonJsonGenerator.HTML_SAFE, htmlSafe);
    config.put(GensonJsonGenerator.SKIP_NULL, skipNull);
    return config;
  }
}

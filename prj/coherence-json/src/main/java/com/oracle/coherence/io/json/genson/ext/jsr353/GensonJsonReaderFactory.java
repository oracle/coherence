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


import com.oracle.coherence.io.json.genson.EncodingAwareReaderFactory;

import javax.json.*;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import static javax.json.stream.JsonParser.Event.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

public class GensonJsonReaderFactory implements javax.json.JsonReaderFactory {
  private final GensonJsonParserFactory parserFactory;
  private final GensonJsonBuilderFactory builderFactory;
  private final EncodingAwareReaderFactory encodingAwareReaderFactory = new EncodingAwareReaderFactory();

  public GensonJsonReaderFactory() {
    this(Collections.<String, Object>emptyMap());
  }

  public GensonJsonReaderFactory(Map<String, ?> config) {
    parserFactory = new GensonJsonParserFactory(config);
    builderFactory = new GensonJsonBuilderFactory();
  }

  @Override
  public JsonReader createReader(final Reader reader) {
    return new JsonReader() {
      private final JsonParser parser = parserFactory.createParser(reader);
      private boolean readed = false;

      @Override
      public JsonStructure read() {
        checkNotReadedAndRead();

        if (parser.hasNext()) {
          Event evt = parser.next();
          if (START_OBJECT == evt) {
            return read(builderFactory.createObjectBuilder()).build();
          } else if (START_ARRAY == evt) {
            return read(builderFactory.createArrayBuilder()).build();
          } else throw new JsonException("Expected START_OBJECT or START_ARRAY but got " + evt);
        }

        throw new JsonException("Empty stream");
      }

      @Override
      public JsonObject readObject() {
        checkNotReadedAndRead();

        if (parser.hasNext()) {
          Event evt = parser.next();
          if (START_OBJECT == evt) {
            return read(builderFactory.createObjectBuilder()).build();
          } else throw new JsonException("Expected " + START_OBJECT + " but got " + evt);
        }

        throw new JsonException("Empty stream");
      }

      @Override
      public JsonArray readArray() {
        checkNotReadedAndRead();

        if (parser.hasNext()) {
          Event evt = parser.next();
          if (START_ARRAY == evt) {
            return read(builderFactory.createArrayBuilder()).build();
          } else throw new JsonException("Expected " + START_ARRAY + " but got " + evt);
        }

        throw new JsonException("Empty stream");
      }

      private JsonArrayBuilder read(JsonArrayBuilder arrayBuilder) {
        while (parser.hasNext()) {
          Event evt = parser.next();
          switch (evt) {
            case VALUE_STRING:
              arrayBuilder.add(parser.getString());
              break;
            case VALUE_NUMBER:
              if (parser.isIntegralNumber()) arrayBuilder.add(parser.getLong());
              else arrayBuilder.add(parser.getBigDecimal());
              break;
            case VALUE_NULL:
              arrayBuilder.addNull();
              break;
            case VALUE_FALSE:
              arrayBuilder.add(JsonValue.FALSE);
              break;
            case VALUE_TRUE:
              arrayBuilder.add(JsonValue.TRUE);
              break;
            case START_OBJECT:
              arrayBuilder.add(
                read(builderFactory.createObjectBuilder())
              );
              break;
            case START_ARRAY:
              arrayBuilder.add(
                read(builderFactory.createArrayBuilder())
              );
              break;
            case END_ARRAY:
              return arrayBuilder;
            default:
              throw new JsonException("Unexpected event " + evt);
          }
        }

        throw new IllegalStateException();
      }

      private JsonObjectBuilder read(JsonObjectBuilder objectBuilder) {
        String name = null;

        while (parser.hasNext()) {
          Event evt = parser.next();
          switch (evt) {
            case KEY_NAME:
              name = parser.getString();
              break;
            case VALUE_STRING:
              objectBuilder.add(name, parser.getString());
              break;
            case VALUE_NUMBER:
              if (parser.isIntegralNumber()) objectBuilder.add(name, parser.getLong());
              else objectBuilder.add(name, parser.getBigDecimal());
              break;
            case VALUE_NULL:
              objectBuilder.addNull(name);
              break;
            case VALUE_FALSE:
              objectBuilder.add(name, JsonValue.FALSE);
              break;
            case VALUE_TRUE:
              objectBuilder.add(name, JsonValue.TRUE);
              break;
            case START_OBJECT:
              objectBuilder.add(
                name, read(builderFactory.createObjectBuilder())
              );
              break;
            case START_ARRAY:
              objectBuilder.add(
                name, read(builderFactory.createArrayBuilder())
              );
              break;
            case END_OBJECT:
              return objectBuilder;
            default:
              throw new JsonException("Unknown Event " + evt);
          }
        }

        throw new IllegalStateException();
      }

      @Override
      public void close() {
        parser.close();
      }

      private void checkNotReadedAndRead() {
        if (readed) throw new IllegalStateException();
        readed = true;
      }
    };
  }

  @Override
  public JsonReader createReader(InputStream in) {
    try {
      return createReader(encodingAwareReaderFactory.createReader(in));
    } catch (IOException e) {
      throw new JsonException("Failed to detect encoding.", e);
    }
  }

  @Override
  public JsonReader createReader(InputStream in, Charset charset) {
    return createReader(new InputStreamReader(in, charset));
  }

  @Override
  public Map<String, ?> getConfigInUse() {
    return parserFactory.getConfigInUse();
  }
}

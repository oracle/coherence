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


import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.spi.JsonProvider;

import com.oracle.coherence.io.json.genson.*;
import com.oracle.coherence.io.json.genson.ext.GensonBundle;
import com.oracle.coherence.io.json.genson.stream.JsonWriter;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

public class JSR353Bundle extends GensonBundle {
  static final JsonBuilderFactory factory = new GensonJsonProvider().createBuilderFactory(
    new HashMap<String, String>());

  private boolean preferJsonpTypes = false;

  @Override
  public void configure(GensonBuilder builder) {
    builder.withConverterFactory(new Factory<Converter<JsonValue>>() {
      @Override
      public Converter<JsonValue> create(Type type, Genson genson) {
        return new JsonValueConverter();
      }
    });

    if (preferJsonpTypes) {
      builder.setDefaultType(com.oracle.coherence.io.json.genson.stream.ValueType.ARRAY, JsonArray.class)
             .setDefaultType(com.oracle.coherence.io.json.genson.stream.ValueType.BOOLEAN, JsonValue.class)
             .setDefaultType(com.oracle.coherence.io.json.genson.stream.ValueType.DOUBLE, JsonNumber.class)
             .setDefaultType(com.oracle.coherence.io.json.genson.stream.ValueType.INTEGER, JsonNumber.class)
             .setDefaultType(com.oracle.coherence.io.json.genson.stream.ValueType.NULL, JsonValue.class)
             .setDefaultType(com.oracle.coherence.io.json.genson.stream.ValueType.OBJECT, JsonObject.class)
             .setDefaultType(com.oracle.coherence.io.json.genson.stream.ValueType.STRING, JsonString.class);
    }
  }

  public JSR353Bundle preferJsonpTypes() {
    preferJsonpTypes = true;
    return this;
  }

  public static class JsonValueConverter implements Converter<JsonValue> {

    @Override
    public void serialize(JsonValue value, ObjectWriter writer, Context ctx) {
      ValueType type = value.getValueType();
      if (ValueType.STRING == type) writer.writeValue(((JsonString) value).getString());
      else if (ValueType.ARRAY == type) writeArray((JsonArray) value, writer, ctx);
      else if (ValueType.OBJECT == type) writeObject((JsonObject) value, writer, ctx);
      else if (ValueType.NULL == type) writer.writeNull();
      else if (ValueType.NUMBER == type) {
        JsonNumber num = (JsonNumber) value;
        if (num.isIntegral()) writer.writeValue(num.longValue());
        else writer.writeValue(num.bigDecimalValue());
      } else if (ValueType.FALSE == type) writer.writeValue(false);
      else if (ValueType.TRUE == type) writer.writeValue(true);
      else {
        throw new IllegalStateException("Unknown ValueType " + type);
      }
    }

    private void writeArray(JsonArray array, ObjectWriter writer, Context ctx) {
      writer.beginArray();
      for (JsonValue value : array)
        serialize(value, writer, ctx);
      writer.endArray();
    }

    private void writeObject(JsonObject object, ObjectWriter writer, Context ctx) {
      writer.beginObject();
      for (Entry<String, JsonValue> e : object.entrySet()) {
        writer.writeName(e.getKey());
        serialize(e.getValue(), writer, ctx);
      }
      writer.endObject();
    }

    @Override
    public JsonValue deserialize(ObjectReader reader, Context ctx) {
      com.oracle.coherence.io.json.genson.stream.ValueType type = reader.getValueType();
      if (com.oracle.coherence.io.json.genson.stream.ValueType.OBJECT == type) {
        return deserObject(reader, ctx);
      } else if (com.oracle.coherence.io.json.genson.stream.ValueType.ARRAY == type) {
        return deserArray(reader, ctx);
      } else {
        // let's allow using literal JsonValues outside of JsonArray or JsonObject
        // thus we need this dummy builder to not by pass the creation mechanism
        if (com.oracle.coherence.io.json.genson.stream.ValueType.STRING == type) {
          return factory.createArrayBuilder().add(reader.valueAsString()).build().get(0);
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.BOOLEAN == type) {
          return reader.valueAsBoolean() ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.NULL == type) {
          return JsonValue.NULL;
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.INTEGER == type) {
          return factory.createArrayBuilder().add(reader.valueAsLong()).build().get(0);
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.DOUBLE == type) {
          return factory.createArrayBuilder().add(reader.valueAsDouble()).build().get(0);
        }
      }

      throw new IllegalStateException("Unsupported ValueType " + type);
    }

    public JsonValue deserObject(ObjectReader reader, Context ctx) {
      JsonObjectBuilder builder = factory.createObjectBuilder();

      // copy metadata first
      Map<String, Object> metadata = reader.metadata();
      for (String key : metadata.keySet()) {
        Object value = metadata.get(key);
        if (value instanceof String) {
          builder.add('@' + key, (String) value);
        } else if (value instanceof Number) {
          builder.add('@' + key, ((Number) value).longValue());
        } else {
          builder.add('@' + key, (Boolean) value);
        }
      }

      while (reader.hasNext()) {
        com.oracle.coherence.io.json.genson.stream.ValueType type = reader.next();
        String name = reader.name();
        if (com.oracle.coherence.io.json.genson.stream.ValueType.STRING == type) {
          builder.add(name, reader.valueAsString());
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.BOOLEAN == type) {
          builder.add(name, reader.valueAsBoolean());
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.NULL == type) {
          builder.addNull(name);
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.INTEGER == type) {
          builder.add(name, reader.valueAsLong());
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.DOUBLE == type) {
          builder.add(name, reader.valueAsDouble());
        } else builder.add(name, deserialize(reader, ctx));
      }

      reader.endObject();
      return builder.build();
    }

    public JsonValue deserArray(ObjectReader reader, Context ctx) {
      JsonArrayBuilder builder = factory.createArrayBuilder();
      reader.beginArray();

      while (reader.hasNext()) {
        com.oracle.coherence.io.json.genson.stream.ValueType type = reader.next();
        if (com.oracle.coherence.io.json.genson.stream.ValueType.STRING == type) {
          builder.add(reader.valueAsString());
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.BOOLEAN == type) {
          builder.add(reader.valueAsBoolean());
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.NULL == type) {
          builder.addNull();
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.INTEGER == type) {
          builder.add(reader.valueAsLong());
        } else if (com.oracle.coherence.io.json.genson.stream.ValueType.DOUBLE == type) {
          builder.add(reader.valueAsDouble());
        } else builder.add(deserialize(reader, ctx));
      }

      reader.endArray();
      return builder.build();
    }
  }

  static String toString(JsonValue value) {
    StringWriter sw = new StringWriter();
    com.oracle.coherence.io.json.genson.stream.JsonWriter writer = new JsonWriter(sw);
    GensonJsonGenerator generator = new GensonJsonGenerator(writer);
    generator.write(value);
    generator.close();
    return sw.toString();
  }

  static boolean toBoolean(Map<String, ?> config, String key) {
    if (config == null) return false;

    if (config.containsKey(key)) {
      Object value = config.get(key);
      if (value instanceof Boolean) {
        return (Boolean) value;
      } else if (value instanceof String) {
        return Boolean.parseBoolean((String) value);
      } else return false;
    } else return false;
  }
}

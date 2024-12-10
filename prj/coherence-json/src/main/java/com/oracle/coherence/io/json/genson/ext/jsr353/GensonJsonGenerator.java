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


import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map.Entry;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;

import com.oracle.coherence.io.json.genson.stream.JsonStreamException;
import com.oracle.coherence.io.json.genson.stream.JsonType;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

public class GensonJsonGenerator implements JsonGenerator {
  public static final String SKIP_NULL = "GensonJsonGenerator.skipNull";
  public static final String HTML_SAFE = "GensonJsonGenerator.htmlSafe";

  private final ObjectWriter writer;
  private final Deque<JsonType> _ctx = new ArrayDeque<JsonType>();

  public GensonJsonGenerator(ObjectWriter writer) {
    this.writer = writer;
  }

  @Override
  public JsonGenerator writeStartObject() {
    try {
      writer.beginObject();
      _ctx.push(JsonType.OBJECT);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator writeStartObject(String name) {
    try {
      writer.writeName(name).beginObject();
      _ctx.push(JsonType.OBJECT);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator writeStartArray() {
    try {
      writer.beginArray();
      _ctx.push(JsonType.ARRAY);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator writeStartArray(String name) {
    try {
      writer.writeName(name).beginArray();
      _ctx.push(JsonType.ARRAY);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(String name, JsonValue value) {
    try {
      writer.writeName(name);
    } catch (JsonStreamException e) {
      throw new JsonException(e.getMessage(), e.getCause());
    }
    return write(value);
  }

  @Override
  public JsonGenerator write(String name, String value) {
    try {
      writer.writeName(name).writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(String name, BigInteger value) {
    try {
      writer.writeName(name).writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(String name, BigDecimal value) {
    try {
      writer.writeName(name).writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(String name, int value) {
    try {
      writer.writeName(name).writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(String name, long value) {
    try {
      writer.writeName(name).writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(String name, double value) {
    try {
      writer.writeName(name).writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(String name, boolean value) {
    try {
      writer.writeName(name).writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator writeNull(String name) {
    try {
      writer.writeName(name).writeNull();
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator writeEnd() {
    JsonType type = _ctx.pop();
    try {
      if (JsonType.OBJECT == type) writer.endObject();
      else if (JsonType.ARRAY == type) writer.endArray();
      else throw new JsonGenerationException(
          "Must call writeStartObject or writeStartArray before calling writeEnd.");
    } catch (JsonStreamException jse) {
      throw new JsonGenerationException(jse.getMessage(), jse.getCause());
    }
    return this;
  }

  @Override
  public JsonGenerator write(JsonValue value) {
    javax.json.JsonValue.ValueType type = value.getValueType();
    if (javax.json.JsonValue.ValueType.ARRAY == type) {
      writeStartArray();
      JsonArray array = (JsonArray) value;
      for (JsonValue elem : array)
        write(elem);
      writeEnd();
    } else if (javax.json.JsonValue.ValueType.OBJECT == type) {
      writeStartObject();
      JsonObject object = (JsonObject) value;
      for (Entry<String, JsonValue> entry : object.entrySet())
        write(entry.getKey(), entry.getValue());
      writeEnd();
    } else if (javax.json.JsonValue.ValueType.FALSE == type) {
      write(false);
    } else if (javax.json.JsonValue.ValueType.TRUE == type) {
      write(true);
    } else if (javax.json.JsonValue.ValueType.NULL == type) {
      writeNull();
    } else if (javax.json.JsonValue.ValueType.STRING == type) {
      write(((JsonString) value).getString());
    } else if (javax.json.JsonValue.ValueType.NUMBER == type) {
      JsonNumber num = (JsonNumber) value;
      if (num.isIntegral()) write(num.bigIntegerValueExact());
      else write(num.bigDecimalValue());
    }
    return this;
  }

  @Override
  public JsonGenerator write(String value) {
    try {
      writer.writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(BigDecimal value) {
    try {
      writer.writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(BigInteger value) {
    try {
      writer.writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(int value) {
    try {
      writer.writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(long value) {
    try {
      writer.writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(double value) {
    try {
      writer.writeValue(value);
    } catch (NumberFormatException e) {
      throw e;
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator write(boolean value) {
    try {
      writer.writeValue(value);
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public JsonGenerator writeNull() {
    try {
      writer.writeNull();
    } catch (Exception e) {
      _wrapAndThrow(e);
    }
    return this;
  }

  @Override
  public void close() {
    flush();
    writer.close();
  }

  @Override
  public void flush() {
    writer.flush();
  }

  private void _wrapAndThrow(Exception e) {
    JsonException newException = null;
    if (e instanceof JsonStreamException) {
      newException = new JsonGenerationException(e.getMessage(), e);
    } else newException = new JsonException(e.getMessage(), e);

    throw JsonStreamException.niceTrace(newException);
  }

  public JsonGenerator writeKey(String s) {
    try {
      writer.writeName(s);
    } catch (Exception e) {
    _wrapAndThrow(e);
    }
    return this;
  }
}

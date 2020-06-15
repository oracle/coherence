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
import java.util.NoSuchElementException;

import javax.json.JsonException;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

import com.oracle.coherence.io.json.genson.stream.JsonStreamException;
import com.oracle.coherence.io.json.genson.stream.JsonType;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ValueType;

import static com.oracle.coherence.io.json.genson.stream.ValueType.*;

public class GensonJsonParser implements JsonParser {
  public static final String STRICT_DOUBLE_PARSE = "GensonJsonParser.strictDoubleParse";

  private boolean parseKey = false;

  private final ObjectReader reader;

  public GensonJsonParser(ObjectReader reader) {
    this.reader = reader;
  }

  @Override
  public boolean hasNext() {
    try {
      return reader.hasNext() || reader.enclosingType() != JsonType.EMPTY;
    } catch (JsonStreamException e) {
      throw _wrapException(e);
    }
  }

  private Event currentValue(ValueType type) {
    if (type == ARRAY) {
      reader.beginArray();
      return Event.START_ARRAY;
    } else if (type == OBJECT) {
      reader.beginObject();
      return Event.START_OBJECT;
    } else if (type == STRING) {
      return Event.VALUE_STRING;
    } else if (type == NULL) {
      return Event.VALUE_NULL;
    } else if (type == BOOLEAN) {
      return reader.valueAsBoolean() ? Event.VALUE_TRUE : Event.VALUE_FALSE;
    } else if (type == INTEGER || type == DOUBLE) {
      return Event.VALUE_NUMBER;
    }

    throw new JsonException("Unknown ValueType " + type);
  }

  @Override
  public Event next() {
    if (!hasNext()) throw new NoSuchElementException();

    try {
      JsonType enclosingType = reader.enclosingType();

      // read the value of an object key/value pair
      if (parseKey) {
        parseKey = false;
        return currentValue(reader.getValueType());
      } else if (reader.hasNext()) {
        ValueType valueType = reader.next();

        // we are in an object make the pair and keep value evt for next call to next()
        if (enclosingType == JsonType.OBJECT) {
          parseKey = true;
          return Event.KEY_NAME;
        } else {
          // this means it is an array, then just read current value and dont care about
          // the next evt
          parseKey = false;
          return currentValue(valueType);
        }
      } else {
        parseKey = false;
        if (enclosingType == JsonType.OBJECT) {
          reader.endObject();
          return Event.END_OBJECT;
        } else if (enclosingType == JsonType.ARRAY) {
          reader.endArray();
          return Event.END_ARRAY;
        }
        throw new JsonException("Reached end of stream, next should not be called.");
      }
    } catch (JsonStreamException e) {
      throw _wrapException(e);
    }
  }

  @Override
  public String getString() {
    try {
      if (parseKey) return reader.name();
      else return reader.valueAsString();
    } catch (JsonStreamException e) {
      throw _wrapException(e);
    }
  }

  @Override
  public boolean isIntegralNumber() {
    return reader.getValueType() == INTEGER;
  }

  @Override
  public int getInt() {
    try {
      return reader.valueAsInt();
    } catch (JsonStreamException e) {
      throw _wrapException(e);
    }
  }

  @Override
  public long getLong() {
    try {
      return reader.valueAsLong();
    } catch (JsonStreamException e) {
      throw _wrapException(e);
    }
  }

  @Override
  public BigDecimal getBigDecimal() {
    // TODO
    try {
      return new BigDecimal(reader.valueAsString());
    } catch (JsonStreamException e) {
      throw _wrapException(e);
    }
  }

  @Override
  public JsonLocation getLocation() {
    return new Location(reader.row(), reader.column());
  }

  @Override
  public void close() {
    try {
      reader.close();
    } catch (IOException e) {
      throw _wrapException(e);
    }
  }

  private JsonException _wrapException(Exception e) {
    JsonException newException = null;
    if (e instanceof JsonStreamException) {
      JsonStreamException jse = (JsonStreamException) e;
      newException =
        new JsonParsingException(e.getMessage(), e, new Location(jse.getRow(),
          jse.getColumn()));
    } else newException = new JsonException(e.getMessage(), e);

    return JsonStreamException.niceTrace(newException);
  }

  static class Location implements JsonLocation {
    final long lineNumber;
    final long columnNumber;

    public Location(long lineNumber, long columnNumber) {
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }

    @Override
    public long getStreamOffset() {
      return -1;
    }

    @Override
    public long getLineNumber() {
      return lineNumber;
    }

    @Override
    public long getColumnNumber() {
      return columnNumber;
    }
  }
}

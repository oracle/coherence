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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class GensonJsonBuilderFactory implements JsonBuilderFactory {

  @Override
  public JsonObjectBuilder createObjectBuilder() {
    return new JsonObjectBuilder() {
      private final Map<String, JsonValue> values = new LinkedHashMap<String, JsonValue>();

      @Override
      public JsonObjectBuilder add(String name, JsonValue value) {
        if (value == null) addNull(name);
        else values.put(name, value);

        return this;
      }

      @Override
      public JsonObjectBuilder add(String name, String value) {
        if (value == null) return addNull(name);
        else return add(name, new GensonJsonString(value));
      }

      @Override
      public JsonObjectBuilder add(String name, BigInteger value) {
        if (value == null) return addNull(name);
        else return add(name, new GensonJsonNumber.IntJsonNumber(value));
      }

      @Override
      public JsonObjectBuilder add(String name, BigDecimal value) {
        if (value == null) return addNull(name);
        else return add(name, new GensonJsonNumber.DoubleJsonNumber(value));
      }

      @Override
      public JsonObjectBuilder add(String name, int value) {
        return add(name, new GensonJsonNumber.IntJsonNumber(value));
      }

      @Override
      public JsonObjectBuilder add(String name, long value) {
        return add(name, new GensonJsonNumber.IntJsonNumber(value));
      }

      @Override
      public JsonObjectBuilder add(String name, double value) {
        return add(name, new GensonJsonNumber.DoubleJsonNumber(value));
      }

      @Override
      public JsonObjectBuilder add(String name, boolean value) {
        return add(name, value ? JsonValue.TRUE : JsonValue.FALSE);
      }

      @Override
      public JsonObjectBuilder addNull(String name) {
        return add(name, JsonValue.NULL);
      }

      @Override
      public JsonObjectBuilder add(String name, JsonObjectBuilder builder) {
        if (builder == null) return addNull(name);
        else return add(name, builder.build());
      }

      @Override
      public JsonObjectBuilder add(String name, JsonArrayBuilder builder) {
        if (builder == null) return addNull(name);
        else return add(name, builder.build());
      }

      @Override
      public JsonObject build() {
        return new GensonJsonObject(Collections.unmodifiableMap(values));
      }
    };
  }

  @Override
  public JsonArrayBuilder createArrayBuilder() {
    return new JsonArrayBuilder() {
      private final List<JsonValue> values = new ArrayList<JsonValue>();

      @Override
      public JsonArrayBuilder add(JsonValue value) {
        if (value == null) addNull();
        else values.add(value);

        return this;
      }

      @Override
      public JsonArrayBuilder add(String value) {
        if (value == null) return addNull();
        else return add(new GensonJsonString(value));
      }

      @Override
      public JsonArrayBuilder add(BigDecimal value) {
        if (value == null) return addNull();
        else return add(new GensonJsonNumber.DoubleJsonNumber(value));
      }

      @Override
      public JsonArrayBuilder add(BigInteger value) {
        if (value == null) return addNull();
        else return add(new GensonJsonNumber.IntJsonNumber(value));
      }

      @Override
      public JsonArrayBuilder add(int value) {
        return add(new GensonJsonNumber.IntJsonNumber(value));
      }

      @Override
      public JsonArrayBuilder add(long value) {
        return add(new GensonJsonNumber.IntJsonNumber(value));
      }

      @Override
      public JsonArrayBuilder add(double value) {
        return add(new GensonJsonNumber.DoubleJsonNumber(value));
      }

      @Override
      public JsonArrayBuilder add(boolean value) {
        return add(value ? JsonValue.TRUE : JsonValue.FALSE);
      }

      @Override
      public JsonArrayBuilder addNull() {
        return add(JsonValue.NULL);
      }

      @Override
      public JsonArrayBuilder add(JsonObjectBuilder builder) {
        if (builder == null) return addNull();
        else return add(builder.build());
      }

      @Override
      public JsonArrayBuilder add(JsonArrayBuilder builder) {
        if (builder == null) return addNull();
        else return add(builder.build());
      }

      @Override
      public JsonArray build() {
        return new GensonJsonArray(Collections.unmodifiableList(values));
      }
    };
  }

  @Override
  public Map<String, ?> getConfigInUse() {
    return Collections.emptyMap();
  }
}

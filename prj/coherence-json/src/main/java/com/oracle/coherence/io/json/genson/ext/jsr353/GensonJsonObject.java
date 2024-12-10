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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class GensonJsonObject implements JsonObject {
  private final Map<String, JsonValue> values;

  GensonJsonObject(Map<String, JsonValue> values) {
    this.values = values;
  }

  @Override
  public JsonArray getJsonArray(String name) {
    return JsonArray.class.cast(values.get(name));
  }

  @Override
  public JsonObject getJsonObject(String name) {
    return JsonObject.class.cast(values.get(name));
  }

  @Override
  public JsonNumber getJsonNumber(String name) {
    return JsonNumber.class.cast(values.get(name));
  }

  @Override
  public JsonString getJsonString(String name) {
    return JsonString.class.cast(values.get(name));
  }

  @Override
  public String getString(String name) {
    return getJsonString(name).getString();
  }

  @Override
  public String getString(String name, String defaultValue) {
    return isNull(name) ? defaultValue : getString(name);
  }

  @Override
  public int getInt(String name) {
    return getJsonNumber(name).intValue();
  }

  @Override
  public int getInt(String name, int defaultValue) {
    return isNull(name) ? defaultValue : getInt(name);
  }

  @Override
  public boolean getBoolean(String name) {
    JsonValue value = values.get(name);
    if (JsonValue.TRUE.equals(value)) return true;
    if (JsonValue.FALSE.equals(value)) return false;
    throw new ClassCastException();
  }

  @Override
  public boolean getBoolean(String name, boolean defaultValue) {
    return isNull(name) ? defaultValue : getBoolean(name);
  }

  @Override
  public boolean isNull(String name) {
    JsonValue value = values.get(name);
    return (JsonValue.NULL.equals(value) || value == null);
  }

  @Override
  public ValueType getValueType() {
    return ValueType.OBJECT;
  }

  @Override
  public int size() {
    return values.size();
  }

  @Override
  public boolean isEmpty() {
    return values.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return values.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return values.containsValue(value);
  }

  @Override
  public JsonValue get(Object key) {
    return values.get(key);
  }

  @Override
  public JsonValue put(String key, JsonValue value) {
    return values.put(key, value);
  }

  @Override
  public JsonValue remove(Object key) {
    return values.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends JsonValue> m) {
    values.putAll(m);
  }

  @Override
  public void clear() {
    values.clear();
  }

  @Override
  public Set<String> keySet() {
    return values.keySet();
  }

  @Override
  public Collection<JsonValue> values() {
    return values.values();
  }

  @Override
  public Set<Entry<String, JsonValue>> entrySet() {
    return values.entrySet();
  }

  @Override
  public JsonValue getOrDefault(Object key, JsonValue defaultValue) {
    return values.getOrDefault(key, defaultValue);
  }

  @Override
  public void forEach(BiConsumer<? super String, ? super JsonValue> action) {
    values.forEach(action);
  }

  @Override
  public void replaceAll(BiFunction<? super String, ? super JsonValue, ? extends JsonValue> function) {
    values.replaceAll(function);
  }

  @Override
  public JsonValue putIfAbsent(String key, JsonValue value) {
    return values.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return values.remove(key, value);
  }

  @Override
  public boolean replace(String key, JsonValue oldValue, JsonValue newValue) {
    return values.replace(key, oldValue, newValue);
  }

  @Override
  public JsonValue replace(String key, JsonValue value) {
    return values.replace(key, value);
  }

  @Override
  public JsonValue computeIfAbsent(String key, Function<? super String, ? extends JsonValue> mappingFunction) {
    return values.computeIfAbsent(key, mappingFunction);
  }

  @Override
  public JsonValue computeIfPresent(String key, BiFunction<? super String, ? super JsonValue, ? extends JsonValue> remappingFunction) {
    return values.computeIfPresent(key, remappingFunction);
  }

  @Override
  public JsonValue compute(String key, BiFunction<? super String, ? super JsonValue, ? extends JsonValue> remappingFunction) {
    return values.compute(key, remappingFunction);
  }

  @Override
  public JsonValue merge(String key, JsonValue value, BiFunction<? super JsonValue, ? super JsonValue, ? extends JsonValue> remappingFunction) {
    return values.merge(key, value, remappingFunction);
  }

  @Override
  public boolean equals(Object o) {
    return values.equals(o);
  }

  @Override
  public int hashCode() {
    return values.hashCode();
  }

  @Override
  public String toString() {
    return JSR353Bundle.toString(this);
  }
}

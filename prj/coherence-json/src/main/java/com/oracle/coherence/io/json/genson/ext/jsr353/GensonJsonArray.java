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
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

class GensonJsonArray implements JsonArray {
  private final List<JsonValue> values;

  GensonJsonArray(List<JsonValue> values) {
    this.values = values;
  }

  @Override
  public JsonObject getJsonObject(int index) {
    return JsonObject.class.cast(values.get(index));
  }

  @Override
  public JsonArray getJsonArray(int index) {
    return JsonArray.class.cast(values.get(index));
  }

  @Override
  public JsonNumber getJsonNumber(int index) {
    return JsonNumber.class.cast(values.get(index));
  }

  @Override
  public JsonString getJsonString(int index) {
    return JsonString.class.cast(values.get(index));
  }

  @Override
  public <T extends JsonValue> List<T> getValuesAs(Class<T> clazz) {
    return (List<T>) values;
  }

  @Override
  public String getString(int index) {
    return getJsonString(index).getString();
  }

  @Override
  public String getString(int index, String defaultValue) {
    if (isNull(index)) return defaultValue;
    return getString(index);
  }

  @Override
  public int getInt(int index) {
    return getJsonNumber(index).intValue();
  }

  @Override
  public int getInt(int index, int defaultValue) {
    if (isNull(index)) return defaultValue;
    return getInt(index);
  }

  @Override
  public boolean getBoolean(int index) {
    JsonValue value = values.get(index);
    if (JsonValue.TRUE.equals(value)) return true;
    if (JsonValue.FALSE.equals(value)) return false;
    throw new ClassCastException();
  }

  @Override
  public boolean getBoolean(int index, boolean defaultValue) {
    if (isNull(index)) return defaultValue;
    return getBoolean(index);
  }

  @Override
  public boolean isNull(int index) {
    return JsonValue.NULL.equals(values.get(index));
  }

  @Override
  public ValueType getValueType() {
    return ValueType.ARRAY;
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
  public boolean contains(Object o) {
    return values.contains(o);
  }

  @Override
  public Iterator<JsonValue> iterator() {
    return values.iterator();
  }

  @Override
  public Object[] toArray() {
    return values.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return values.toArray(a);
  }

  @Override
  public boolean add(JsonValue jsonValue) {
    return values.add(jsonValue);
  }

  @Override
  public boolean remove(Object o) {
    return values.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return values.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends JsonValue> c) {
    return values.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends JsonValue> c) {
    return values.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return values.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return values.retainAll(c);
  }

  @Override
  public void replaceAll(UnaryOperator<JsonValue> operator) {
    values.replaceAll(operator);
  }

  @Override
  public void sort(Comparator<? super JsonValue> c) {
    values.sort(c);
  }

  @Override
  public void clear() {
    values.clear();
  }

  @Override
  public JsonValue get(int index) {
    return values.get(index);
  }

  @Override
  public JsonValue set(int index, JsonValue element) {
    return values.set(index, element);
  }

  @Override
  public void add(int index, JsonValue element) {
    values.add(index, element);
  }

  @Override
  public JsonValue remove(int index) {
    return values.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return values.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return values.lastIndexOf(o);
  }

  @Override
  public ListIterator<JsonValue> listIterator() {
    return values.listIterator();
  }

  @Override
  public ListIterator<JsonValue> listIterator(int index) {
    return values.listIterator(index);
  }

  @Override
  public List<JsonValue> subList(int fromIndex, int toIndex) {
    return values.subList(fromIndex, toIndex);
  }

  @Override
  public Spliterator<JsonValue> spliterator() {
    return values.spliterator();
  }

  @Override
  public boolean removeIf(Predicate<? super JsonValue> filter) {
    return values.removeIf(filter);
  }

  @Override
  public Stream<JsonValue> stream() {
    return values.stream();
  }

  @Override
  public Stream<JsonValue> parallelStream() {
    return values.parallelStream();
  }

  @Override
  public void forEach(Consumer<? super JsonValue> action) {
    values.forEach(action);
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

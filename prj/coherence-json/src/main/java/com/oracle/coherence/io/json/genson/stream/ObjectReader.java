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

package com.oracle.coherence.io.json.genson.stream;


import java.io.Closeable;
import java.util.Map;

/**
 * ObjectReader is part of the streaming api, it's implementations allow you to read data from the
 * stream. The root of the input should always be a object, an array or a literal. There may be some
 * differences between implementations if they try to be compliant with their format specification.
 * <p/>
 * <ul>
 * <li>To read an array call {@link #beginArray()} then use {@link #hasNext()} to check if there is
 * a next element and then call {@link #next()} to advance. When you call next in an array it will
 * read the next value and return its type {@link ValueType}. Use it to check values type and to
 * retrieve its value (if it is a literal) use one of valueAsXXX methods, otherwise it is an array
 * or object. When hasNext returns false terminate the array by calling {@link #endArray()}.
 * <li>To read a object call {@link #beginObject()} then use {@link #hasNext()} to check if there is
 * a next property and then call {@link #next()} to read the name/value pair and {@link #name()} to
 * retrieve its name. If the value is a literal retrieve its value with valueAsXXX methods,
 * otherwise it is an array or object. When you finished reading all properties call
 * {@link #endObject()}.
 * <li>Objects can also contain metadata as their first properties. To read object metadata you have
 * two options:
 * <ol>
 * <li>Just begin your object with beginObject and then retrieve the metadata that you want with
 * {@link #metadata(String) metadata(nameOfTheMetadataProperty)}.
 * <li>Use {@link #nextObjectMetadata()} to read the next objects metadata without calling
 * beginObject. This is useful when you want to handle some metadata in a converter and then
 * delegate the rest to another converter (that will call beginObject or again nextObjectMetadata,
 * so for him it will be transparent that you retrieved already some metadata and he will still be
 * able to retrieve the same data).
 * </ol>
 * <li>To read a literal use valueAsXXX methods. Actual implementation allows literals as root and
 * is relatively tolerant to wrong types, for example if the stream contains the string "123" but
 * you want to retrieve it as a int, {@link JsonReader#valueAsInt()} will parse it and return 123.
 * It does also conversion between numeric types (double <-> int etc).
 * <li>To skip a value use {@link #skipValue()}. If the value is an object or an array it will skip
 * all its content.
 * </ul>
 * <p/>
 * Here is an example if you want to use directly the streaming api instead of the databind api (or
 * if you write a custom converter or deserializer).
 * <p/>
 * <pre>
 * public static void main(String[] args) {
 * 	// we will read from json to Person
 * 	Person.read(new JsonReader(&quot;{\&quot;name\&quot;:\&quot;eugen\&quot;,\&quot;age\&quot;:26, \&quot;childrenYearOfBirth\&quot;:[]}&quot;));
 * }
 *
 * class Person {
 * 	String name;
 * 	int age;
 * 	List&lt;Integer&gt; childrenYearOfBirth;
 *
 * 	public static Person read(ObjectReader reader) {
 * 		Person p = new Person();
 * 		for (; reader.hasNext();) {
 * 			if (&quot;name&quot;.equals(reader.name()))
 * 				p.name = reader.valueAsString();
 * 			else if (&quot;age&quot;.equals(reader.name()))
 * 				p.age = reader.valueAsInt();
 * 			else if (&quot;childrenYearOfBirth&quot;.equals(reader.name())) {
 * 				if (reader.getValueType() == TypeValue.NULL)
 * 					p.childrenYearOfBirth = null;
 * 				else {
 * 					reader.beginArray();
 * 					p.childrenYearOfBirth = new ArrayList&lt;Integer&gt;();
 * 					for (int i = 0; reader.hasNext(); i++)
 * 						p.childrenYearOfBirth.add(reader.valueAsInt());
 * 					reader.endArray();
 *        }
 *      }
 *    }
 * 		return p;
 *  }
 * }
 * </pre>
 *
 * @author eugen
 * @see ValueType
 * @see JsonReader
 * @see ObjectWriter
 * @see JsonWriter
 */
public interface ObjectReader extends Closeable {
  /**
   * Starts reading a object. Objects contain name/value pairs. Call {@link #endObject()} when the
   * objects contains no more properties.
   *
   * @return a reference to the reader.
   * @throws JsonStreamException
   */
  ObjectReader beginObject();

  /**
   * Ends the object. If you were not in an object or the object contains more data, an exception
   * will be thrown.
   *
   * @return a reference to the reader.
   * @throws JsonStreamException
   */
  ObjectReader endObject();

  /**
   * Starts reading an array. Arrays contain only values. Call {@link #endArray()} when the array
   * contains no more values.
   *
   * @return a reference to the reader.
   * @throws JsonStreamException
   */
  ObjectReader beginArray();

  /**
   * Ends the array. If you were not in an array or the array contains more data, an exception
   * will be thrown.
   *
   * @return a reference to the reader.
   * @throws JsonStreamException
   */
  ObjectReader endArray();

  /**
   * Will read nexts object metadata. You can call this method as many times as you want, with the
   * condition that you use only {@link #metadata(String)} method. For example if you call
   * {@link #beginObject()} you wont be able to do it anymore (however you still can retrieve the
   * metadata!).
   *
   * @return a reference to the reader.
   * @throws JsonStreamException
   */
  ObjectReader nextObjectMetadata();

  /**
   * If we are in a object it will read the next name/value pair and if we are in an array it will
   * read the next value (except if value is of complex type, in that case after the call to
   * next() you must use one of beginXXX methods).
   *
   * @return the type of the value, see {@link ValueType} for possible types.
   * @throws JsonStreamException
   */
  ValueType next();

  /**
   * @return true if there is a next property or value, false otherwise.
   * @throws JsonStreamException
   */
  boolean hasNext();

  /**
   * If the value is of complex type it will skip its content.
   *
   * @return a reference to the reader.
   * @throws JsonStreamException
   */
  ObjectReader skipValue();

  /**
   * @return The type of current value.
   * @see ValueType
   */
  ValueType getValueType();

  /**
   * Return the map containing all metadata.
   *
   * @return the map containing all metadata key/value pairs.
   * @throws JsonStreamException
   */
  Map<String, Object> metadata();

  /**
   * The value of a specified metadata attribute.
   *
   * @param name the name of the metadata attribute to retrieve.
   * @return the value of metadata with name as key or null if there
   *         is no such metadata attribute.
   * @throws JsonStreamException
   */
  String metadata(String name);

  /**
   * @see #metadata(String)
   */
  Long metadataAsLong(String name);

  /**
   * @see #metadata(String)
   */
  Boolean metadataAsBoolean(String name);

  /**
   * @return the name of current property, valid only if we are in a object and you called
   * {@link #next()} before.
   * @throws JsonStreamException
   */
  String name();

  /**
   * @return the current value as a String. It will try to convert the actual value to String if
   * its not of that type.
   * @throws JsonStreamException
   */
  String valueAsString();

  /**
   * @throws JsonStreamException
   * @throws NumberFormatException
   * @see #valueAsString()
   */
  int valueAsInt();

  /**
   * @throws JsonStreamException
   * @throws NumberFormatException
   * @see #valueAsString()
   */
  long valueAsLong();

  /**
   * @throws JsonStreamException
   * @throws NumberFormatException
   * @see #valueAsString()
   */
  double valueAsDouble();

  /**
   * @throws JsonStreamException
   * @throws NumberFormatException
   * @see #valueAsString()
   */
  short valueAsShort();

  /**
   * @throws JsonStreamException
   * @throws NumberFormatException
   * @see #valueAsString()
   */
  float valueAsFloat();

  /**
   * @throws JsonStreamException
   * @see #valueAsString()
   */
  boolean valueAsBoolean();

  /**
   * @return the incoming base64 string converted to a byte array.
   * @throws JsonStreamException
   */
  byte[] valueAsByteArray();

  JsonType enclosingType();

  int column();

  int row();
}

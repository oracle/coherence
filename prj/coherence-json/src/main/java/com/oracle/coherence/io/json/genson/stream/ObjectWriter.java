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
import java.io.Flushable;
import java.io.IOException;

/**
 * ObjectWriter defines the api allowing to write data to different format and the contract for
 * classes that implement ObjectWriter to provide different formats support. Implementations are
 * extremely efficient as they use low level stream operations and optimizations. If you want
 * optimal performance you can directly use the streaming api (ObjectWriter and {@link ObjectReader}
 * ) without the databind support that comes with the converters. This will be very close in terms
 * of performance as writing manually formated data to the stream.
 * <p/>
 * If you want to write the array new int[1, 2, 3] to the stream with ObjectWriter:
 * <p/>
 * <pre>
 * writer.beginArray().writeValue(1).writeValue(2).writeValue(3).endArray();
 * </pre>
 * <p/>
 * And to write Person (we simplify, in practice you must handle null values):
 * <p/>
 * <pre>
 * class Person {
 * 	public static void main(String[] args) {
 * 		// we will write from Person to json string
 * 		Person p = new Person();
 * 		p.name = &quot;eugen&quot;;
 * 		p.age = 26;
 * 		p.childrenYearOfBirth = new ArrayList&lt;Integer&gt;();
 * 		StringWriter sw = new StringWriter();
 * 		ObjectWriter writer = new JsonWriter(sw);
 * 		p.write(writer);
 * 		writer.flush();
 * 		writer.close();
 * 		// will write {&quot;name&quot;:&quot;eugen&quot;,&quot;age&quot;:26,&quot;childrenYearOfBirth&quot;:[]}
 * 		System.out.println(sw.toString());
 *  }
 *
 * 	String name;
 * 	int age;
 * 	List&lt;Integer&gt; childrenYearOfBirth;
 *
 * 	public void write(ObjectWriter writer) {
 * 		writer.beginObject().writeName(&quot;name&quot;);
 * 		if (name == null) writer.writeNull();
 * 		else writer.writeValue(name)
 * 		writer.writeName(&quot;age&quot;).writeAge(age)
 * 				.writeName(&quot;childrenYearOfBirth&quot;);
 * 		if (childrenYearOfBirth == null) writer.writeNull();
 * 		else {
 * 			writer.beginArray();
 * 			for (Integer year : childrenYearOfBirth)
 * 				writer.writeValue(year);
 * 			writer.endArray()
 *    }
 * 		writer.endObject();
 *  }
 * }
 * </pre>
 * <p/>
 * Be careful if you instantiate ObjectWriter your self you are responsible of flushing and closing
 * the stream.
 *
 * @author eugen
 * @see JsonWriter
 * @see ObjectReader
 * @see JsonReader
 */
public interface ObjectWriter {

  /**
   * Starts to write an array (use it also for collections). An array is a suite of values that
   * may be literals, arrays or objects. When you finished writing the values don't forget to call
   * endArray().
   *
   * @return a reference to this allowing to chain method calls.
   * @throws JsonStreamException if trying to produce invalid json
   */
  public ObjectWriter beginArray();

  /**
   * Ends the array, if beginArray was not called, implementations should throw an exception.
   *
   * @return a reference to this allowing to chain method calls.
   * @throws JsonStreamException if trying to produce invalid json
   */
  public ObjectWriter endArray();

  /**
   * Starts a object, objects are a suite of name/value pairs, values may be literals, arrays or
   * objects. Don't forget to call endObject.
   *
   * @return a reference to this allowing to chain method calls.
   * @throws JsonStreamException if trying to produce invalid json
   */
  public ObjectWriter beginObject();

  /**
   * Ends the object being written, if beginObject was not called an exception will be throwed.
   *
   * @return a reference to this allowing to chain method calls.
   * @throws JsonStreamException if trying to produce invalid json
   */
  public ObjectWriter endObject();

  /**
   * Writes the name of a property. Names can be written only in objects and must be called before
   * writing the properties value.
   *
   * @param name a non null String
   * @return a reference to this, allowing to chain method calls.
   * @throws JsonStreamException if trying to produce invalid json
   */
  public ObjectWriter writeName(String name);

  /**
   * Will write the name without escaping special characters, assuming it has been done by the caller or the string
   * doesn't contain any character needing to be escaped.
   * @param name a non null escaped String
   * @return a reference to this, allowing to chain method calls.
   * @throws JsonStreamException if trying to produce invalid json
   */
  public ObjectWriter writeEscapedName(char[] name);

  /**
   * Writes a value to the stream. Values can be written in arrays and in objects (after writing
   * the name).
   *
   * @param value to write.
   * @return a reference to this, allowing to chain method calls.
   * @throws JsonStreamException if trying to produce invalid json
   */
  public ObjectWriter writeValue(int value);

  /**
   * See {@link #writeValue(int)}.
   *
   * @throws JsonStreamException if trying to produce invalid json
   * @see #writeValue(int)
   */
  public ObjectWriter writeValue(double value);

  /**
   * See {@link #writeValue(int)}.
   *
   * @throws JsonStreamException if trying to produce invalid json
   * @see #writeValue(int)
   */
  public ObjectWriter writeValue(long value);

  /**
   * See {@link #writeValue(int)}.
   *
   * @throws JsonStreamException if trying to produce invalid json
   * @see #writeValue(int)
   */
  public ObjectWriter writeValue(short value);

  /**
   * @see #writeValue(int)
   */
  public ObjectWriter writeValue(float value);

  /**
   * See {@link #writeValue(int)}.
   *
   * @throws JsonStreamException if trying to produce invalid json
   * @see #writeValue(int)
   */
  public ObjectWriter writeValue(boolean value);

  /**
   * @see #writeString(String)
   */
  public ObjectWriter writeBoolean(Boolean value);

  /**
   * See {@link #writeValue(int)}.
   *
   * @throws JsonStreamException if trying to produce invalid json
   * @see #writeValue(int)
   */
  public ObjectWriter writeValue(Number value);

  /**
   * @see #writeString(String)
   */
  public ObjectWriter writeNumber(Number value);

  /**
   * See {@link #writeValue(int)}.
   *
   * @throws JsonStreamException if trying to produce invalid json
   * @see #writeValue(int)
   */
  public ObjectWriter writeValue(String value);

  /**
   * Similar to writeValue(String) but is null safe, meaning that if the value is null,
   * then the write will call writeNull for you.
   */
  public ObjectWriter writeString(String value);

  /**
   * Writes an array of bytes as a base64 encoded string. See {@link #writeValue(int)}.
   *
   * @throws JsonStreamException if trying to produce invalid json
   * @see #writeValue(int)
   */
  public ObjectWriter writeValue(byte[] value);

  /**
   * @see #writeString(String)
   */
  public ObjectWriter writeBytes(byte[] value);

  /**
   * Writes value as is without any pre-processing, it's faster than {@link #writeValue(String)}
   * but should be used only if you know that it is safe.
   *
   * @throws JsonStreamException if trying to produce invalid json
   * @see #writeValue(int)
   */
  public ObjectWriter writeUnsafeValue(String value);

  /**
   * Must be called when a null value is encountered. Implementations will deal with the null
   * representation (just skip it or write null, etc).
   *
   * @return a reference to this allowing to chain method calls.
   * @throws JsonStreamException if trying to produce invalid json
   * @see #writeValue(int)
   */
  public ObjectWriter writeNull();

  /**
   * This method is a kind of cheat as it allows us to start writing metadata and then still be
   * able to call beginObject. This is mainly intended to be used in wrapped converters that want
   * to handle a part of the serialization and then let the chain continue. Have a look at <a
   * href=
   * "http://code.google.com/p/genson/source/browse/src/main/java/com/owlike/genson/convert/ClassMetadataConverter.java"
   * >ClassMetadataConverter</a>
   *
   * @return a reference to this allowing to chain method calls.
   * @throws JsonStreamException
   * @see #writeMetadata(String, String)
   */
  public ObjectWriter beginNextObjectMetadata();

  /**
   * Metadata is a suite of name/value pairs, names will be prepended with '@' (handled by the
   * library). Metadata feature is experimental for the moment so things may change a bit. The
   * signature will not, but the way it is implemented could... Actually the contract is that
   * metadata must be written first and only in objects. If it does not respect these conditions
   * ObjectReader won't be able to detect it as metadata. Here is an example of two ways to write
   * object metadata.
   * <p/>
   * <pre>
   * // here it is transparent for the library if you write metadata or something else, you must call
   * // beginObject before being able to start writing its metadata.
   * writer.beginObject().writeMetadata(&quot;doc&quot;, &quot;Object documentation bla bla...&quot;).writeName(&quot;name&quot;)
   * 		.writeNull().endObject().flush();
   *
   * // previous example works fine, but if you want to write some metadata and still be able to call
   * // beginObject (for example in a converter you want to write some metadata and have all the existing
   * // continue to work with calling beginObject) you must use beginNextObjectMetadata.
   *
   * // written from a first converter
   * writer.beginNextObjectMetadata().writeMetadata(&quot;dataFromConverter1&quot;, &quot;writtenFromConverter1&quot;);
   * // written from a second converter after first one
   * writer.beginNextObjectMetadata().writeMetadata(&quot;dataFromConverter2&quot;, &quot;writtenFromConverter2&quot;);
   * // finally concrete properties will be written from a custom converter
   * writer.beginObject().writeName(&quot;name&quot;).writeNull().endObject().flush();
   * </pre>
   *
   * @param name  of the metadata property, should not start with '@', the library will add it.
   * @param value of the metadata property.
   * @return a reference to this allowing to chain method calls.
   * @throws JsonStreamException
   * @see #beginNextObjectMetadata()
   */
  public ObjectWriter writeMetadata(String name, String value);

  /**
   * @see #writeMetadata(String, String)
   */
  public ObjectWriter writeMetadata(String name, Long value);

  /**
   * @see #writeMetadata(String, String)
   */
  public ObjectWriter writeMetadata(String name, Boolean value);

  /**
   * @see #writeString(String, String)
   */
  public ObjectWriter writeBoolean(String name, Boolean value);

  /**
   * @see #writeString(String, String)
   */
  public ObjectWriter writeNumber(String name, Number value);

  /**
   * Will write the name and the value, it is just a shortcut for writer.writeName("key").writeString(value).
   * Note if the value is null, writeNull is used.
   */
  public ObjectWriter writeString(String name, String value);

  /**
   * @see #writeString(String, String)
   */
  public ObjectWriter writeBytes(String name, byte[] value);

  public void flush();

  public void close();

  public JsonType enclosingType();
}

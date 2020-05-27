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

package com.oracle.coherence.io.json.genson;


import java.io.IOException;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;

/**
 * Converter interface is a shorthand for classes who want to implement both serialization and
 * deserialization. You should always privilege Converter instead of low level Serializer and
 * Deseriliazer as they will be wrapped into a converter and all the ChainedFactory mechanism is
 * designed for converters. Here is an example of a Converter of URLs.
 * <p/>
 * <pre>
 * Genson genson = new Genson.Builder().with(new Converter&lt;URL&gt;() {
 *
 * 	&#064;Override
 * 	public void serialize(URL url, ObjectWriter writer, Context ctx) {
 * 		// you don't have to worry about null objects, as the library will handle them.
 * 		writer.writeValue(obj.toExternalForm());
 *  }
 *
 * 	&#064;Override
 * 	public URL deserialize(ObjectReader reader, Context ctx) {
 * 		return new URL(reader.valueAsString());
 *  }
 *
 * }).create();
 *
 * String serializedUrl = genson.serialize(new URL(&quot;http://www.google.com&quot;));
 * URL url = genson.deserialize(serializedUrl, URL.class);
 * </pre>
 * <p/>
 * As you can see it is quite straightforward to create and register new Converters. Here is an
 * example dealing with more complex objects.
 *
 * @param <T> type of objects handled by this converter.
 * @author eugen
 */
public interface Converter<T> extends Serializer<T>, Deserializer<T> {
  @Override
  public void serialize(T object, com.oracle.coherence.io.json.genson.stream.ObjectWriter writer, Context ctx) throws Exception;

  @Override
  public T deserialize(ObjectReader reader, Context ctx) throws Exception;
}

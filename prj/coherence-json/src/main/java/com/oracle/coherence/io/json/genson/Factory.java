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


import java.lang.reflect.Type;

/**
 * Factory interface must be implemented by classes who want to act as factories and create
 * instances of Converter/Serializer/Deserializer. Implementations will be used as Converter,
 * Serializer and Deserializer factories. So the type T will be something like
 * Converter&lt;Integer&gt; but the type argument of method create will correspond to Integer <u>or
 * a subclass of Integer</u>.
 * <p/>
 * As an example you can have a look at factories from {@link com.oracle.coherence.io.json.genson.convert.DefaultConverters
 * DefaultConverters}. Here is an example with a custom converter and factory for enums.
 * <p/>
 * <pre>
 * public static class EnumConverter&lt;T extends Enum&lt;T&gt;&gt; implements Converter&lt;T&gt; {
 * 	private final Class&lt;T&gt; eClass;
 *
 * 	public EnumConverter(Class&lt;T&gt; eClass) {
 * 		this.eClass = eClass;
 *  }
 *
 * 	&#064;Override
 * 	public void serialize(T obj, ObjectWriter writer, Context ctx) {
 * 		writer.writeUnsafeValue(obj.name());
 *  }
 *
 * 	&#064;Override
 * 	public T deserialize(ObjectReader reader, Context ctx) {
 * 		return Enum.valueOf(eClass, reader.valueAsString());
 *  }
 * }
 *
 * public final static class EnumConverterFactory implements Factory&lt;Converter&lt;? extends Enum&lt;?&gt;&gt;&gt; {
 * 	public final static EnumConverterFactory instance = new EnumConverterFactory();
 *
 * 	private EnumConverterFactory() {
 *  }
 *
 * 	&#064;SuppressWarnings({ &quot;rawtypes&quot;, &quot;unchecked&quot; })
 * 	&#064;Override
 * 	public Converter&lt;Enum&lt;?&gt;&gt; create(Type type, Genson genson) {
 * 		Class&lt;?&gt; rawClass = TypeUtil.getRawClass(type);
 * 		return rawClass.isEnum() || Enum.class.isAssignableFrom(rawClass) ? new EnumConverter(
 * 				rawClass) : null;
 *  }
 * };
 * </pre>
 * <p/>
 * Note the use of {@link com.oracle.coherence.io.json.genson.reflect.TypeUtil TypeUtil} class that provides operations to
 * work with generic types. However this class might change in the future, in order to provide a better API.
 *
 * @param <T> the base type of the objects this factory can create. T can be of type Converter,
 *            Serializer or Deserializer.
 * @author Eugen Cepoi
* @see com.oracle.coherence.io.json.genson.Converter
 * @see com.oracle.coherence.io.json.genson.convert.ChainedFactory ChainedFactory
 * @see com.oracle.coherence.io.json.genson.Serializer
 * @see com.oracle.coherence.io.json.genson.Deserializer
 */
public interface Factory<T> {
  /**
   * Implementations of this method must try to create an instance of type T based on the
   * parameter "type". If this factory can not create an object of type T for parameter type then
   * it must return null.
   *
   * @param type used to build an instance of T.
   * @return null if it doesn't support this type or an instance of T (or a subclass).
   */
  public T create(Type type, Genson genson);
}

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

import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

/**
 * Serializers handle serialization by writing a java object of type T to a stream using
 * {@link com.oracle.coherence.io.json.genson.stream.ObjectWriter ObjectWriter}. Genson Serializers work like classic
 * serializers from other libraries. Here is an example of a custom serializer that will delegate
 * the serialization of Author type to the library:
 * <p/>
 * <pre>
 * class Book {
 * 	String title;
 * 	int totalPages;
 * 	Author author;
 * }
 *
 * class Author {
 * 	String name;
 * }
 *
 * static class BookSerializer implements Serializer&lt;Book&gt; {
 * 	private final Serializer&lt;Author&gt; authorSerializer;
 *
 * 	// a reference to a delegated author serializer
 * 	BookSerializer(Serializer&lt;Author&gt; authorSerializer) {
 * 		this.authorSerializer = authorSerializer;
 *  }
 *
 * 	public void serialize(Book book, ObjectWriter writer, Context ctx) {
 * 		// we don't have to worry if book is null by default it is handled by the library.
 * 		writer.beginObject().writeName(&quot;title&quot;).writeValue(book.title).writeName(&quot;totalPages&quot;)
 * 				.writeValue(book.totalPages).writeName(&quot;author&quot;);
 *
 * 		// again no need to check if author is null the library will handle it
 * 		authorSerializer.serialize(book.author, writer, ctx);
 * 		writer.endObject();
 *  }
 *
 * 	 public final static Factory&lt;Serializer&lt;Book&gt;&gt; bookFactory = new Factory&lt;Serializer&lt;Book&gt;&gt;() {
 *
 * 		public Serializer&lt;Book&gt; create(Type type, Genson genson) {
 * 			Serializer&lt;Author&gt; authorSerializer = genson.provideConverter(Author.class);
 * 			return new GoodBookSerializer(authorSerializer);
 *    }
 *  }
 * }
 * </pre>
 * <p/>
 * As you see it involves very few lines of code and is quite powerful.
 *
 * @param <T> the type of objects this Serializer can serialize.
 * @author Eugen Cepoi
* @see Converter
 * @see com.oracle.coherence.io.json.genson.Factory Factory
 */
public interface Serializer<T> {
  /**
   * @param object we want to serialize. The object is of type T or a subclass (if this serializer
   *               has been registered for subclasses).
   * @param writer to use to write data to the output stream.
   * @param ctx    the current context.
   * @throws com.oracle.coherence.io.json.genson.JsonBindingException
   * @throws com.oracle.coherence.io.json.genson.stream.JsonStreamException
   */
  public void serialize(T object, ObjectWriter writer, Context ctx) throws Exception;
}

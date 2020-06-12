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
 * Deserializers handle deserialization by reading data form {@link com.oracle.coherence.io.json.genson.stream.ObjectReader
 * ObjectReader} and constructing java objects of type T. Genson Deserializers work like classic
 * deserializers from other libraries.
 *
 * @param <T> the type of objects this deserializer can deserialize.
 * @author eugen
 * @see Converter
 */
public interface Deserializer<T> {
  /**
   * @param reader used to read data from.
   * @param ctx    the current context.
   * @return an instance of T or a subclass of T.
   * @throws com.oracle.coherence.io.json.genson.JsonBindingException
   * @throws com.oracle.coherence.io.json.genson.stream.JsonStreamException
   */
  public T deserialize(ObjectReader reader, Context ctx) throws Exception;
}

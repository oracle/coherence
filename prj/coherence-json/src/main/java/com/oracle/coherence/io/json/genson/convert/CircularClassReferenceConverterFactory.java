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

package com.oracle.coherence.io.json.genson.convert;


import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.Wrapper;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

/**
 * ChainedFactory that handles circular class references.
 *
 * @author Eugen Cepoi
*/
public class CircularClassReferenceConverterFactory extends ChainedFactory {
  private final static class CircularConverter<T> extends Wrapper<Converter<T>> implements Converter<T> {

    private CountDownLatch initLatch = new CountDownLatch(1);      

    protected CircularConverter() {
      super();
    }

    public void serialize(T obj, ObjectWriter writer, Context ctx) throws Exception {
      if(wrapped == null) {
        initLatch.await();
      }
      wrapped.serialize(obj, writer, ctx);
    }

    public T deserialize(ObjectReader reader, Context ctx) throws Exception {
      if(wrapped == null) {
        initLatch.await();
      }
      return wrapped.deserialize(reader, ctx);
    }

    void setDelegateConverter(Converter<T> delegate) {
      decorate(delegate);
    }
  }

  private final ThreadLocal<Map<Type, CircularConverter<?>>> _circularConverters = new ThreadLocal<Map<Type, CircularConverter<?>>>();

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Converter<?> create(Type type, Genson genson) {
    Map<Type, CircularConverter<?>> map = _circularConverters.get();
    if (map == null) {
      map = new HashMap<Type, CircularConverter<?>>();
      _circularConverters.set(map);
    }

    if (_circularConverters.get().containsKey(type)) {
      return _circularConverters.get().get(type);
    } else {
      try {
        CircularConverter circularConverter = new CircularConverter();
        try {
          _circularConverters.get().put(type, circularConverter);
          Converter converter = next().create(type, genson);
          circularConverter.setDelegateConverter(converter);
          return converter;
        } finally {
          circularConverter.initLatch.countDown();
        }
      } finally {
        _circularConverters.get().remove(type);
      }
    }
  }

  @Override
  protected Converter<?> create(Type type, Genson genson, Converter<?> nextConverter) {
    throw new UnsupportedOperationException();
  }
}

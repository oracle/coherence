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

import com.oracle.coherence.io.json.genson.convert.*;
import org.junit.Test;

import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import java.lang.reflect.Type;

import static org.junit.Assert.*;

public class GensonBuilderTest {
  @Test
  public void testCustomConverterRegistration() {
    final Converter<Number> dummyConverter = new Converter<Number>() {
      public void serialize(Number object, ObjectWriter writer, Context ctx) {
      }

      public Number deserialize(ObjectReader reader, Context ctx) {
        return null;
      }
    };
    Genson genson = new GensonBuilder() {
      @Override
      protected Factory<Converter<?>> createConverterFactory() {
        assertEquals(dummyConverter, getSerializersMap().get(Number.class));
        assertEquals(dummyConverter, getSerializersMap().get(Long.class));
        assertEquals(dummyConverter, getSerializersMap().get(Double.class));
        assertEquals(dummyConverter, getDeserializersMap().get(Number.class));
        assertEquals(dummyConverter, getDeserializersMap().get(Long.class));
        assertEquals(dummyConverter, getDeserializersMap().get(Double.class));
        return new BasicConvertersFactory(getSerializersMap(), getDeserializersMap(), getFactories(), getBeanDescriptorProvider());
      }
    }.withConverters(dummyConverter).withConverter(dummyConverter, Long.class).withConverter(dummyConverter, new GenericType<Double>() {
    }).create();

    assertEquals(dummyConverter, genson.provideConverter(Number.class));
    assertEquals(dummyConverter, genson.provideConverter(Long.class));
    assertEquals(dummyConverter, genson.provideConverter(Double.class));
  }

  @Test
  public void testChainFactoryVisitor() {
    Genson genson = new GensonBuilder()
            .withConverterFactory(factory ->
                    factory.find(NullConverterFactory.class)
                            .withNext(new CapitalizingConverter.Factory()))
            .create();

    String json = genson.serialize("test");
    assertEquals("\"TEST\"", json);
    assertEquals("test", genson.deserialize(json, String.class));
  }

  private static class CapitalizingConverter<T>
          extends Wrapper<Converter<T>>
          implements Converter<T> {

    public CapitalizingConverter(Converter<T> wrappedObject) {
      super(wrappedObject);
    }

    @Override
    public void serialize(T object, ObjectWriter writer, Context ctx) throws Exception {
      if (object instanceof String) {
        object = (T) ((String) object).toUpperCase();
      }
      wrapped.serialize(object, writer, ctx);
    }

    @Override
    public T deserialize(ObjectReader reader, Context ctx) throws Exception {
      T obj = wrapped.deserialize(reader, ctx);
      if (obj instanceof String) {
        obj = (T) ((String) obj).toLowerCase();
      }
      return obj;
    }

    static class Factory extends ChainedFactory {
      @Override
      protected Converter<?> create(Type type, Genson genson, Converter<?> nextConverter) {
        return new CapitalizingConverter<>(nextConverter);
      }
    }
  }
}

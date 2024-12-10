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

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.JsonBindingException;
import com.oracle.coherence.io.json.genson.Serializer;
import com.oracle.coherence.io.json.genson.Wrapper;
import com.oracle.coherence.io.json.genson.annotation.HandleNull;
import com.oracle.coherence.io.json.genson.reflect.TypeUtil;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;
import com.oracle.coherence.io.json.genson.stream.ValueType;

/**
 * The default implementation handles null values by returning the predefined default value if any or null during
 * deserialization and by calling writer.writeNull() during serialization.
 * <p/>
 *
 * @author Eugen Cepoi
*/
public class NullConverterFactory extends ChainedFactory {
  private final boolean failOnNullPrimitive;

  public NullConverterFactory(boolean failOnNullPrimitive) {
    this.failOnNullPrimitive = failOnNullPrimitive;
  }

  private class FailIfNullConverter<T> extends Wrapper<Converter<T>> implements Converter<T> {
    public FailIfNullConverter(Converter<T> delegate) {
      super(delegate);
    }

    @Override
    public void serialize(T object, ObjectWriter writer, Context ctx) throws Exception {
      if (object == null) {
        throw new JsonBindingException("Serialization of null primitives is forbidden");
      } else {
        wrapped.serialize(object, writer, ctx);
      }
    }

    @Override
    public T deserialize(ObjectReader reader, Context ctx) throws Exception {
      if (ValueType.NULL == reader.getValueType()) {
        throw new JsonBindingException("Can not deserialize null to a primitive type");
      } else {
        return wrapped.deserialize(reader, ctx);
      }
    }
  }

  // TODO check if making the delegate instance final would improve perfs
  private class NullConverterWrapper<T> extends Wrapper<Converter<T>> implements
    Converter<T> {
    private final T defaultValue;

    public NullConverterWrapper(T defaultValue, Converter<T> converter) {
      super(converter);
      this.defaultValue = defaultValue;
    }

    public void serialize(T obj, ObjectWriter writer, Context ctx) throws Exception {
      if (obj == null) {
        writer.writeNull();
      } else {
        wrapped.serialize(obj, writer, ctx);
      }
    }

    public T deserialize(ObjectReader reader, Context ctx) throws Exception {
      if (ValueType.NULL == reader.getValueType()) {
        return defaultValue;
      } else {
        return wrapped.deserialize(reader, ctx);
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  protected Converter<?> create(Type type, Genson genson, Converter<?> nextConverter) {
    if (Wrapper.toAnnotatedElement(nextConverter).isAnnotationPresent(HandleNull.class)) {
      return nextConverter;
    } else {
      Class<?> rawClass = TypeUtil.getRawClass(type);
      if (failOnNullPrimitive && rawClass.isPrimitive()) {
        return new FailIfNullConverter(nextConverter);
      } else {
        return new NullConverterWrapper(genson.defaultValue(rawClass), nextConverter);
      }
    }
  }
}

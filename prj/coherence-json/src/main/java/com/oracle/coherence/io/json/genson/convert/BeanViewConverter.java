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
import java.util.List;

import com.oracle.coherence.io.json.genson.BeanView;
import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.Wrapper;
import com.oracle.coherence.io.json.genson.annotation.HandleBeanView;
import com.oracle.coherence.io.json.genson.reflect.BeanDescriptor;
import com.oracle.coherence.io.json.genson.reflect.BeanViewDescriptorProvider;
import com.oracle.coherence.io.json.genson.reflect.TypeUtil;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

/**
 * Converter responsible of applying the BeanView mechanism.
 *
 * @param <T> type of objects this BeanViewConverter can handle.
 * @author Eugen Cepoi
* @see com.oracle.coherence.io.json.genson.reflect.BeanViewDescriptorProvider BeanViewDescriptorProvider
 * @see com.oracle.coherence.io.json.genson.BeanView BeanView
 */
public class BeanViewConverter<T> extends Wrapper<Converter<T>> implements Converter<T> {

  public static class BeanViewConverterFactory extends ChainedFactory {
    private final BeanViewDescriptorProvider provider;

    public BeanViewConverterFactory(BeanViewDescriptorProvider provider) {
      this.provider = provider;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected Converter<?> create(Type type, Genson genson, Converter<?> nextConverter) {
      if (!Wrapper.toAnnotatedElement(nextConverter).isAnnotationPresent(
        HandleBeanView.class))
        // TODO as we link an instance to a type, we may optimize things, but for the moment it is okay
        // lets see if this feature is used
        return new BeanViewConverter(type, provider, nextConverter);
      return nextConverter;
    }
  }

  private final BeanViewDescriptorProvider provider;
  private final Type type;

  public BeanViewConverter(Type type, BeanViewDescriptorProvider provider, Converter<T> next) {
    super(next);
    this.provider = provider;
    this.type = type;
  }

  @SuppressWarnings("unchecked")
  protected Class<? extends BeanView<T>> findViewFor(Type type,
                                                     List<Class<? extends BeanView<?>>> views) {
    for (Class<? extends BeanView<?>> v : views) {
      Type searchedType = TypeUtil.lookupGenericType(BeanView.class, v);
      searchedType = TypeUtil.expandType(searchedType, v);
      searchedType = TypeUtil.typeOf(0, searchedType);
      if (TypeUtil.match(type, searchedType, false)) {
        return (Class<? extends BeanView<T>>) v;
      }
    }
    return null;
  }

  public void serialize(T obj, ObjectWriter writer, Context ctx) throws Exception {
    boolean handled = false;
    List<Class<? extends BeanView<?>>> views = ctx.views();
    if (views != null && views.size() > 0) {
      Class<? extends BeanView<T>> viewClass = findViewFor(type, views);
      if (viewClass != null) {
        Type viewForType = TypeUtil.expandType(BeanView.class.getTypeParameters()[0],
          viewClass);
        @SuppressWarnings("unchecked")
        Class<T> viewForClass = (Class<T>) TypeUtil.getRawClass(viewForType);
        BeanDescriptor<T> descriptor = provider
          .provide(viewForClass, viewClass, ctx.genson);
        descriptor.serialize(obj, writer, ctx);
        handled = true;
      }
    }
    if (!handled) wrapped.serialize(obj, writer, ctx);
  }

  public T deserialize(ObjectReader reader, Context ctx) throws Exception {
    if (ctx.hasViews()) {
      Class<? extends BeanView<T>> viewClass = findViewFor(type, ctx.views());
      if (viewClass != null) {
        Type viewForType = TypeUtil.expandType(BeanView.class.getTypeParameters()[0],
          viewClass);
        @SuppressWarnings("unchecked")
        Class<T> viewForClass = (Class<T>) TypeUtil.getRawClass(viewForType);
        BeanDescriptor<T> descriptor = provider
          .provide(viewForClass, viewClass, ctx.genson);
        return descriptor.deserialize(reader, ctx);
      }
    }
    return wrapped.deserialize(reader, ctx);
  }
}

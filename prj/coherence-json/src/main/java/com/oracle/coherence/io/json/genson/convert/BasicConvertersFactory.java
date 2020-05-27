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


import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Deserializer;
import com.oracle.coherence.io.json.genson.Factory;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.Operations;
import com.oracle.coherence.io.json.genson.Serializer;
import com.oracle.coherence.io.json.genson.Wrapper;
import com.oracle.coherence.io.json.genson.reflect.BeanDescriptorProvider;
import com.oracle.coherence.io.json.genson.reflect.TypeUtil;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

/**
 * This is the base factory that will create converters based on the default ones and on custom
 * Serializer, Deserializer and Converter. But it also uses factories (default and custom) and
 * {@link com.oracle.coherence.io.json.genson.reflect.BeanDescriptorProvider BeanDescriptorProvider} that is
 * responsible of creating bean converters.
 * <p/>
 * When you ask for a Converter it will
 * <ul>
 * <ol>
 * <li>Lookup in the registered Serializers for one that is parameterized with the current type, if
 * found we finished (it takes the first one, so the order matters).</li>
 * <li>Else we will try the factories by searching the ones that can create and instance of
 * Serializer&lt;CurrentType&gt; (again the order is very important). We continue while they return
 * null.</li>
 * <li>If no factory could create an instance we will use BeanDescriptorProvider.</li>
 * </ol>
 * </li>
 * <li>We apply all the same logic a second time for Deserializer.</li>
 * <li>If they are both an instance of Converter then we return one of them</li>
 * <li>Otherwise we will wrap both into a Converter.</li>
 * </ul>
 * <p/>
 * Note that the create method from the registered factories will only be called if the type with
 * which they are parameterized is assignable from the current type. For example, if we look for a
 * serializer of Integer then Factory&lt;Converter&lt;Integer>> and Factory&lt;Serializer&lt;Object>> match
 * both, the first registered will be used.
 *
 * @author eugen
 */
public class BasicConvertersFactory implements Factory<Converter<?>> {
  private final Map<Type, Serializer<?>> serializersMap;
  private final Map<Type, Deserializer<?>> deserializersMap;
  private final List<Factory<?>> factories;
  private final BeanDescriptorProvider beanDescriptorProvider;

  public BasicConvertersFactory(Map<Type, Serializer<?>> serializersMap,
                                Map<Type, Deserializer<?>> deserializersMap, List<Factory<?>> factories,
                                BeanDescriptorProvider beanDescriptorProvider) {
    this.serializersMap = serializersMap;
    this.deserializersMap = deserializersMap;
    this.factories = factories;
    this.beanDescriptorProvider = beanDescriptorProvider;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Converter<?> create(Type type, Genson genson) {
    Converter<?> converter;
    Serializer<?> serializer = provide(Serializer.class, type, serializersMap, genson);
    Deserializer<?> deserializer = provide(Deserializer.class, type, deserializersMap, genson);
    if (serializer instanceof Converter && deserializer instanceof Converter) {
      converter = (Converter<?>) deserializer;
    } else {
      converter = new DelegatedConverter(serializer, deserializer);
    }
    return converter;
  }

  @SuppressWarnings("unchecked")
  protected <T> T provide(Class<T> forClass, Type withParameterType,
                          Map<Type, ? extends T> fromTypeMap, Genson genson) {
    if (fromTypeMap.containsKey(withParameterType)) return fromTypeMap.get(withParameterType);

    Type wrappedParameterType = withParameterType;
    if (withParameterType instanceof Class<?> && ((Class<?>) withParameterType).isPrimitive())
      wrappedParameterType = TypeUtil.wrap((Class<?>) withParameterType);

    for (Iterator<Factory<?>> it = factories.iterator(); it.hasNext(); ) {
      Factory<?> factory = it.next();
      Object object;
      Type factoryType = TypeUtil.lookupGenericType(Factory.class, factory.getClass());
      factoryType = TypeUtil.expandType(factoryType, factory.getClass());
      // it is a parameterized type and we want the parameter corresponding to Serializer from
      // Factory<Serializer<?>>
      factoryType = TypeUtil.typeOf(0, factoryType);
      Type factoryParameter = TypeUtil.typeOf(0, factoryType);
      if (forClass.isAssignableFrom(TypeUtil.getRawClass(factoryType))
        && TypeUtil.match(wrappedParameterType, factoryParameter, false)
        && (object = factory.create(withParameterType, genson)) != null) {
        return forClass.cast(object);
      }
    }

    return (T) beanDescriptorProvider.provide(TypeUtil.getRawClass(withParameterType),
      withParameterType, genson);
  }

  private class DelegatedConverter<T> extends Wrapper<Converter<T>> implements Converter<T> {
    private final Serializer<T> serializer;
    private final Deserializer<T> deserializer;

    public DelegatedConverter(Serializer<T> serializer, Deserializer<T> deserializer) {
      this.serializer = serializer;
      this.deserializer = deserializer;
    }

    public void serialize(T obj, ObjectWriter writer, Context ctx) throws Exception {
      serializer.serialize(obj, writer, ctx);
    }

    public T deserialize(ObjectReader reader, Context ctx) throws Exception {
      return deserializer.deserialize(reader, ctx);
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> aClass) {
      A a = null;
      if (serializer != null) a = toAnnotatedElement(serializer).getAnnotation(aClass);
      if (deserializer != null && a == null)
        a = toAnnotatedElement(deserializer).getAnnotation(aClass);
      return a;
    }

    @Override
    public Annotation[] getAnnotations() {
      if (serializer != null && deserializer != null)
        return Operations.union(Annotation[].class, toAnnotatedElement(serializer)
          .getAnnotations(), toAnnotatedElement(deserializer).getAnnotations());
      if (serializer != null) return toAnnotatedElement(serializer).getAnnotations();
      if (deserializer != null) return toAnnotatedElement(deserializer).getAnnotations();

      return new Annotation[0];
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
      if (serializer != null && deserializer != null)
        return Operations.union(Annotation[].class, toAnnotatedElement(serializer)
          .getDeclaredAnnotations(), toAnnotatedElement(deserializer)
          .getDeclaredAnnotations());
      if (serializer != null) return toAnnotatedElement(serializer).getDeclaredAnnotations();
      if (deserializer != null)
        return toAnnotatedElement(deserializer).getDeclaredAnnotations();

      return new Annotation[0];
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
      if (serializer != null)
        return toAnnotatedElement(serializer).isAnnotationPresent(annotationClass);
      if (deserializer != null)
        return toAnnotatedElement(deserializer).isAnnotationPresent(annotationClass);
      return false;
    }
  }
}

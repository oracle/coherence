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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.coherence.io.json.genson.*;
import org.junit.Before;
import org.junit.Test;

import com.oracle.coherence.io.json.genson.annotation.HandleClassMetadata;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import static org.junit.Assert.*;

public class FactoryTest {
  private BasicConvertersFactory factory;
  private Genson genson;

  @SuppressWarnings("serial")
  public static class ParameterizedSuperType extends HashMap<Object, String> {
  }

  @Test
  public void testConstructionForTypeWithParameters() {
    assertNotNull(new Genson().provideConverter(ParameterizedSuperType.class));
  }

  @Before
  public void setUp() {

    genson = new GensonBuilder() {
      @Override
      protected Factory<Converter<?>> createConverterFactory() {
        factory = new BasicConvertersFactory(getSerializersMap(), getDeserializersMap(),
          getFactories(), getBeanDescriptorProvider());

        ChainedFactory chain = new NullConverterFactory(false);
        chain.withNext(
          new BeanViewConverter.BeanViewConverterFactory(
            getBeanViewDescriptorProvider())).withNext(factory);

        return chain;
      }
    }.create();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBasicConvertersFactory() {
    Converter<Integer> ci = (Converter<Integer>) factory.create(Integer.class, genson);
    assertEquals(DefaultConverters.IntegerConverter.class, ci.getClass());

    Converter<Boolean> cb = (Converter<Boolean>) factory.create(Boolean.class, genson);
    assertEquals(DefaultConverters.BooleanConverter.class, cb.getClass());

    assertNotNull(factory.create(int.class, genson));

    Converter<Long[]> cal = (Converter<Long[]>) factory.create(Long[].class, genson);
    assertEquals(DefaultConverters.ArrayConverter.class, cal.getClass());

    Converter<Object[]> cao = (Converter<Object[]>) factory.create(Object[].class, genson);
    assertEquals(DefaultConverters.ArrayConverter.class, cao.getClass());

    Converter<List<?>> converter = (Converter<List<?>>) factory.create(List.class, genson);
    assertEquals(DefaultConverters.CollectionConverter.class, converter.getClass());

    Converter<Map<String, Object>> cm = (Converter<Map<String, Object>>) factory.create(
      new GenericType<Map<String, Object>>() {
      }.getType(), genson);
    assertEquals(DefaultConverters.HashMapConverter.class, cm.getClass());
  }

  @Test
  public void testCircularReferencingClasses() {
    Genson genson = new Genson();
    Converter<A> converter = genson.provideConverter(A.class);
    assertNotNull(converter);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testUnwrapAnnotations() throws Exception {
    Genson genson = new GensonBuilder().withConverters(new ClassMetadataConverter()).create();

    Wrapper<Converter<A>> wrapper = (Wrapper) genson.provideConverter(A.class);
    Converter<A> converter = wrapper.unwrap();
    while (converter instanceof Wrapper) {
      converter = (Converter<A>) ((Wrapper) converter).unwrap();
    }

    assertTrue(converter instanceof ClassMetadataConverter);
    assertFalse(ClassMetadataConverter.used);
    converter.serialize(new A(), null, null);
    assertTrue(ClassMetadataConverter.used);
  }

  @HandleClassMetadata
  static class ClassMetadataConverter implements Converter<A> {
    static boolean used = false;

    public void serialize(A obj, ObjectWriter writer, Context ctx) {
      used = true;
    }

    public A deserialize(ObjectReader reader, Context ctx) {
      return null;
    }
  }

  static class A {
    A a;
    B b;
    C c;
  }

  static class B {
    B b;
    A a;
  }

  static class C {
    B b;
  }
}

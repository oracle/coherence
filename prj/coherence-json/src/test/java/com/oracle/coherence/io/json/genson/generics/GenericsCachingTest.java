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

package com.oracle.coherence.io.json.genson.generics;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.annotation.JsonProperty;
import com.oracle.coherence.io.json.genson.reflect.TypeUtil;

import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * artifactId: genson
 * <p/>
 * version: 0.94 & 0.95
 * Issue 4, Reported by jesper.hammarback
 * http://code.google.com/p/genson/issues/detail?id=4
 */
public class GenericsCachingTest {

  @Test
  public void testTypeExpansionCaching() throws SecurityException, NoSuchFieldException {
    Type typeOfFoosField = FooContainer.class.getDeclaredField("foos").getGenericType();
    Type typeOfBarsField = BarContainer.class.getDeclaredField("bars").getGenericType();
    Type typeOfDataField = FooBarContainer.class.getDeclaredField("data").getGenericType();

    ParameterizedType expandedTypeOfFoosDataField = (ParameterizedType) TypeUtil.expandType(typeOfDataField,
      typeOfFoosField);
    ParameterizedType expandedTypeOfBarsDataField = (ParameterizedType) TypeUtil.expandType(typeOfDataField,
      typeOfBarsField);

    assertEquals(List.class, expandedTypeOfFoosDataField.getRawType());
    assertEquals(Foo.class, expandedTypeOfFoosDataField.getActualTypeArguments()[0]);

    // testing that those expanded types are considered as distinct in the cache
    assertEquals(List.class, expandedTypeOfBarsDataField.getRawType());
    assertEquals(Bar.class, expandedTypeOfBarsDataField.getActualTypeArguments()[0]);
  }

  @Test
  public void testDistinctExpandedTypeNotMixed() throws Exception {
    Genson genson = new GensonBuilder().setSkipNull(true).create();

    FooContainer fooContainer = new FooContainer(new FooBarContainer<Foo>(Arrays.asList(new Foo("foo"))));
    BarContainer barContainer = new BarContainer(new FooBarContainer<Bar>(Arrays.asList(new Bar("bar"))));

    String fooContainerJson = genson.serialize(fooContainer);
    String barContainerJson = genson.serialize(barContainer);

    assertThat(fooContainerJson, is("{\"foos\":{\"data\":[{\"fooId\":\"foo\"}]}}"));
    assertThat(barContainerJson, is("{\"bars\":{\"data\":[{\"barId\":\"bar\"}]}}"));

    // lets also make sure deser works fine and types dont get mixed...
    assertEquals(fooContainer, genson.deserialize(fooContainerJson, FooContainer.class));
    assertEquals(barContainer, genson.deserialize(barContainerJson, BarContainer.class));
  }

  public static class FooContainer {
    public final FooBarContainer<Foo> foos;

    public FooContainer(@JsonProperty("foos") FooBarContainer<Foo> foos) {
      this.foos = foos;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((foos == null) ? 0 : foos.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      FooContainer other = (FooContainer) obj;
      if (foos == null) {
        if (other.foos != null) return false;
      } else if (!foos.equals(other.foos)) return false;
      return true;
    }
  }

  public static class BarContainer {
    public final FooBarContainer<Bar> bars;

    public BarContainer(@JsonProperty("bars") FooBarContainer<Bar> bars) {
      this.bars = bars;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((bars == null) ? 0 : bars.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      BarContainer other = (BarContainer) obj;
      if (bars == null) {
        if (other.bars != null) return false;
      } else if (!bars.equals(other.bars)) return false;
      return true;
    }
  }

  public static class FooBarContainer<T> {
    public final List<T> data;

    public FooBarContainer(@JsonProperty("data") List<T> data) {
      this.data = data;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((data == null) ? 0 : data.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      @SuppressWarnings("rawtypes") FooBarContainer other = (FooBarContainer) obj;
      if (data == null) {
        if (other.data != null) return false;
      } else if (!data.equals(other.data)) return false;
      return true;
    }
  }

  public static class Foo {
    public final String fooId;

    public Foo(@JsonProperty("fooId") String fooId) {
      this.fooId = fooId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fooId == null) ? 0 : fooId.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Foo other = (Foo) obj;
      if (fooId == null) {
        if (other.fooId != null) return false;
      } else if (!fooId.equals(other.fooId)) return false;
      return true;
    }
  }

  public static class Bar {
    public final String barId;

    public Bar(@JsonProperty("barId") String barId) {
      this.barId = barId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((barId == null) ? 0 : barId.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Bar other = (Bar) obj;
      if (barId == null) {
        if (other.barId != null) return false;
      } else if (!barId.equals(other.barId)) return false;
      return true;
    }
  }

}

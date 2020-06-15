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

package com.oracle.coherence.io.json.genson.reflect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

import com.oracle.coherence.io.json.genson.Trilean;

import static com.oracle.coherence.io.json.genson.reflect.BeanMutatorAccessorResolver.*;

public class BeanMutatorAccessorResolverTest {

  @Test
  public void testCustomResolver() throws SecurityException, NoSuchFieldException {
    List<BeanMutatorAccessorResolver> resolvers = new ArrayList<BeanMutatorAccessorResolver>();
    resolvers.add(new PropertyBaseResolver() {
      @Override
      public Trilean isAccessor(Field field, Class<?> fromClass) {
        return MyProxy.class.equals(field.getType()) ? Trilean.FALSE : Trilean.UNKNOWN;
      }
    });
    resolvers.add(new StandardMutaAccessorResolver());

    CompositeResolver composite = new CompositeResolver(resolvers);

    assertEquals(Trilean.FALSE, composite.isAccessor(MyPojo.class.getDeclaredField("proxy"), MyPojo.class));
    assertEquals(Trilean.TRUE, composite.isAccessor(MyPojo.class.getDeclaredField("aString"), MyPojo.class));
  }

  private class MyPojo {
    @SuppressWarnings("unused")
    private MyProxy proxy;
    @SuppressWarnings("unused")
    public String aString;
  }

  private class MyProxy {
  }
}

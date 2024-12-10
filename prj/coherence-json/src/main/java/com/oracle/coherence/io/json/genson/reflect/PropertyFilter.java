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


import com.oracle.coherence.io.json.genson.Trilean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PropertyFilter extends BeanMutatorAccessorResolver.PropertyBaseResolver {
  private final boolean exclude;
  private final String field;
  private final Class<?> declaringClass;
  private final Class<?> ofType;

  public PropertyFilter(boolean exclude, String field, Class<?> declaringClass, Class<?> ofType) {
    this.exclude = exclude;
    this.field = field;
    this.declaringClass = declaringClass;
    this.ofType = ofType;
  }

  @Override
  public Trilean isAccessor(Field field, Class<?> fromClass) {
    return filter(field.getName(), fromClass, field.getType(), exclude);
  }

  @Override
  public Trilean isMutator(Field field, Class<?> fromClass) {
    return filter(field.getName(), fromClass, field.getType(), exclude);
  }

  @Override
  public Trilean isAccessor(Method method, Class<?> fromClass) {
    String name = method.getName();
    if (name.startsWith("is") && name.length() > 2) {
      return filter(name.substring(2), method.getDeclaringClass(),
        method.getReturnType(), exclude);
    }
    if (name.length() > 3) {
      if (name.startsWith("get"))
        return filter(name.substring(3), method.getDeclaringClass(),
          method.getReturnType(), exclude);
    }
    return Trilean.UNKNOWN;
  }

  @Override
  public Trilean isMutator(Method method, Class<?> fromClass) {
    String name = method.getName();
    if (name.length() > 3 && method.getParameterTypes().length == 1) {
      if (name.startsWith("set"))
        return filter(name.substring(3), method.getDeclaringClass(),
          method.getParameterTypes()[0], exclude);
    }
    return Trilean.UNKNOWN;
  }

  private Trilean filter(String actualName, Class<?> fromClass,
                         Class<?> propertyType, boolean exclude) {
    if ((field == null || actualName.equalsIgnoreCase(field))
      && (declaringClass == null || declaringClass.isAssignableFrom(fromClass))
      && (ofType == null || ofType.isAssignableFrom(TypeUtil.wrap(propertyType))))
      return exclude ? Trilean.FALSE : Trilean.TRUE;
    return Trilean.UNKNOWN;
  }
}

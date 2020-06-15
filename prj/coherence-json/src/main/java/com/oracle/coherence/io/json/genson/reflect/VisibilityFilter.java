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


import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * This class is used as filter for properties discovery. It uses java modifiers to check if a
 * property (can be a method, field, constructor or any class that implements Member interface) is
 * visible.
 * <p/>
 * The filter acts by excluding the properties with specified modifiers. Here are some examples :
 * <p/>
 * <pre>
 * // will filter nothing :
 * new VisibilityFilter();
 *
 * // exclude only private and transient:
 * new VisibilityFilter(Modifier.TRANSIENT, Modifier.PRIVATE);
 *
 * // exclude only public!! and allow all the rest
 * new VisibilityFilter(Modifier.public);
 * </pre>
 * <p/>
 * So the idea is to pass to the constructor all the Modifier.XXX modifiers that you want to be
 * filtered.
 *
 * @author Eugen Cepoi
* @see BeanMutatorAccessorResolver.StandardMutaAccessorResolver
 */
public final class VisibilityFilter {

  private final static int JAVA_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED
    | Modifier.PRIVATE | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL
    | Modifier.TRANSIENT | Modifier.VOLATILE | Modifier.SYNCHRONIZED | Modifier.NATIVE
    | Modifier.STRICT | Modifier.INTERFACE;

  public final static VisibilityFilter ABSTRACT = new VisibilityFilter(Modifier.ABSTRACT);
  public final static VisibilityFilter PRIVATE = new VisibilityFilter(Modifier.TRANSIENT,
    Modifier.NATIVE, Modifier.STATIC);
  public final static VisibilityFilter ALL = new VisibilityFilter();
  public final static VisibilityFilter NONE = new VisibilityFilter(JAVA_MODIFIERS);
  public final static VisibilityFilter PROTECTED = new VisibilityFilter(Modifier.TRANSIENT,
    Modifier.NATIVE, Modifier.STATIC, Modifier.PRIVATE);
  public final static VisibilityFilter PACKAGE_PUBLIC = new VisibilityFilter(Modifier.TRANSIENT,
    Modifier.NATIVE, Modifier.STATIC, Modifier.PRIVATE, Modifier.PROTECTED);

  private int filter;

  /**
   * Creates a new VisibilityFilter with specified modifiers. You must use existing values from
   * Modifier class otherwise an exception will be thrown.
   *
   * @param modifier all the modifiers you want to exclude.
   */
  public VisibilityFilter(int... modifier) {
    filter = 0;
    for (int m : modifier) {

      if ((m & JAVA_MODIFIERS) == 0)
        throw new IllegalArgumentException(
          "One of the modifiers is not a standard java modifier.");
      filter = filter | m;
    }
  }

  /**
   * Checks whether this member is visible or not according to this filter.
   *
   * @param member
   * @return true if this member is visible according to this filter.
   */
  public final boolean isVisible(Member member) {
    return isVisible(member.getModifiers());
  }

  public final boolean isVisible(int modifiers) {
    return (modifiers & filter) == 0;
  }
}

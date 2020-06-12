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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.oracle.coherence.io.json.genson.Operations.checkNotNull;

/**
 * The context class is intended to be a statefull class shared across a single execution. Its main
 * purpose is to hold local data and to pass it to others contributors of the
 * serialization/deserialization chain.
 * <p/>
 * For example if you have computed in a first serializer a value and you need it later in another
 * one, you can put it in the context by using {@link Context#store(String, Object)} method and
 * later retrieve it with {@link #get(String, Class)} or remove it with {@link #remove(String, Class)}.
 * <p/>
 * You can achieve the same thing with {@link ThreadLocalHolder} that stores the data in a thread
 * local map but it is cleaner to use this Context class as you wont have to worry about removing
 * values from it. Indeed java web servers reuse created threads, so if you store data in a thread
 * local variable and don't remove it, it will still be present when another request comes in and is
 * bound to that thread! This can also lead to memory leaks! Don't forget the
 * try-store-finally-remove block if you stick with ThreadLocalHolder.
 * <p/>
 * <p/>
 * This class stores also the views present in the current context, those views will be applied to
 * the matching objects during serialization and deserialization.
 *
 * @author Eugen Cepoi
* @see com.oracle.coherence.io.json.genson.BeanView BeanView
 * @see com.oracle.coherence.io.json.genson.convert.BeanViewConverter BeanViewConverter
 * @see com.oracle.coherence.io.json.genson.ThreadLocalHolder ThreadLocalHolder
 */
public class Context {
  public final Genson genson;
  private List<Class<? extends BeanView<?>>> views;
  private Map<String, Object> _ctxData = new HashMap<String, Object>();

  public Context(Genson genson) {
    this(genson, null);
  }

  public Context(Genson genson, List<Class<? extends BeanView<?>>> views) {
    checkNotNull(genson);
    this.genson = genson;
    this.views = views;
  }

  public boolean hasViews() {
    return views != null && !views.isEmpty();
  }

  public Context withView(Class<? extends BeanView<?>> view) {
    if (views == null) views = new ArrayList<Class<? extends BeanView<?>>>();
    views.add(view);
    return this;
  }

  public List<Class<? extends BeanView<?>>> views() {
    return views;
  }

  /**
   * Puts the object o in the current context indexed by key.
   *
   * @param key must be not null
   * @param o
   * @return the old object associated with that key or null.
   */
  public Object store(String key, Object o) {
    checkNotNull(key);
    Object old = _ctxData.get(key);
    _ctxData.put(key, o);
    return old;
  }

  /**
   * Returns the value mapped to key in this context or null. If the value is not of type
   * valueType then an exception is thrown.
   *
   * @param key       must be not null
   * @param valueType the type of the value, null not allowed
   * @return the mapping for key or null
   * @throws ClassCastException if the value mapped to key is not of type valueType.
   */
  public <T> T get(String key, Class<T> valueType) {
    checkNotNull(key, valueType);
    return valueType.cast(_ctxData.get(key));
  }

  /**
   * Removes the mapping for this key from the context. If there is no mapping for that key, null
   * is returned. If the value mapped to key is not of type valueType an ClassCastException is
   * thrown and the mapping is not removed.
   *
   * @param key       must be not null
   * @param valueType the type of the value, null not allowed
   * @return the value associated to this key
   * @throws ClassCastException if the value mapped to key is not of type valueType.
   * @see com.oracle.coherence.io.json.genson.Context#get(String, Class)
   */
  public <T> T remove(String key, Class<T> valueType) {
    checkNotNull(key, valueType);
    T value = valueType.cast(_ctxData.get(key));
    _ctxData.remove(key);
    return value;
  }
}

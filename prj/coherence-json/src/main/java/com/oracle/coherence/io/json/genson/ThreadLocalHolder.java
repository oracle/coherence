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


import static com.oracle.coherence.io.json.genson.Operations.checkNotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Just another data holder that stores data in a threadlocal map.
 * If you only want to share data across serializers and deserializers prefer using {@link Context}.
 * Internally Genson uses it for the spring webmvc integration, so it can pass method signatures and
 * extract its annotations, etc.
 *
 * @author Eugen Cepoi
* @see Context
 * @see com.oracle.coherence.io.json.genson.ext.spring.ExtendedReqRespBodyMethodProcessor ExtendedReqRespBodyMethodProcessor
 * @see com.oracle.coherence.io.json.genson.ext.spring.GensonMessageConverter GensonMessageConverter
 */
public final class ThreadLocalHolder {
  private final static ThreadLocal<Map<String, Object>> _data = new ThreadLocal<Map<String, Object>>();

  public static Object store(String key, Object parameter) {
    checkNotNull(key);
    return getPutIfMissing().put(key, parameter);
  }

  public static <T> T remove(String key, Class<T> valueType) {
    checkNotNull(key, valueType);
    Map<String, Object> map = getPutIfMissing();
    T value = valueType.cast(map.get(key));
    map.remove(key);
    return value;
  }

  public static <T> T get(String key, Class<T> valueType) {
    checkNotNull(key, valueType);
    return valueType.cast(getPutIfMissing().get(key));
  }

  private static Map<String, Object> getPutIfMissing() {
    Map<String, Object> map = _data.get();
    if (map == null) {
      map = new HashMap<String, Object>();
      _data.set(map);
    }
    return map;
  }
}

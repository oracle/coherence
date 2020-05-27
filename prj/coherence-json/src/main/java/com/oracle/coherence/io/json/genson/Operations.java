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


import java.lang.reflect.Array;

public final class Operations {
  public static <T> T[] union(Class<T[]> tClass, T[]... values) {
    int size = 0;
    for (T[] value : values)
      size += value.length;
    T[] arr = tClass.cast(Array.newInstance(tClass.getComponentType(), size));
    for (int i = 0, len = 0; i < values.length; len += values[i].length, i++)
      System.arraycopy(values[i], 0, arr, len, values[i].length);
    return arr;
  }

  public static byte[] expandArray(byte[] array, int idx, double factor) {
    if (idx >= array.length) {
      byte[] tmpArray = new byte[(int) (array.length * factor)];
      System.arraycopy(array, 0, tmpArray, 0, array.length);
      return tmpArray;
    } else return array;
  }

  public static byte[] truncateArray(byte[] array, int size) {
    if (size < array.length) {
      byte[] tmpArray = new byte[size];
      System.arraycopy(array, 0, tmpArray, 0, size);
      return tmpArray;
    } else return array;
  }

  public static void checkNotNull(Object... values) {
    for (Object value : values)
      if (value == null) throw new IllegalArgumentException("Null not allowed!");
  }
}

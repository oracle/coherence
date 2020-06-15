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

package com.oracle.coherence.io.json.genson.bean;

public class ReprUtil {
  public static String repr(String s) {
    if (s == null) return "null";
    return '"' + s + '"';
  }

  public static String repr(Iterable<String> it) {
    StringBuilder buf = new StringBuilder();
    buf.append('[');
    String sep = "";
    for (String s : it) {
      buf.append(sep);
      sep = ", ";
      buf.append(repr(s));
    }
    buf.append(']');
    return buf.toString();
  }
}

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

package com.oracle.coherence.io.json.genson.stream;


import java.util.List;
import java.util.Map;

public enum ValueType {
  ARRAY(List.class),
  OBJECT(Map.class),
  STRING(String.class),
  CHAR(Character.class),
  INTEGER(Long.class),
  DOUBLE(Double.class),
  BOOLEAN(Boolean.class),
  NULL(null);

  private Class<?> clazz;

  ValueType(Class<?> clazz) {
    this.clazz = clazz;
  }

  public void setDefaultClass(Class<?> clazz) {
    this.clazz = clazz;
  }

  public Class<?> toClass() {
    return clazz;
  }
}

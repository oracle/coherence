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


/**
 * A boolean with 3 states : true, false and unknown.
 *
 * @author eugen
 */
public enum Trilean {
  TRUE() {
    @Override
    public boolean booleanValue() {
      return true;
    }
  },
  FALSE {
    @Override
    public boolean booleanValue() {
      return false;
    }
  },
  UNKNOWN {
    @Override
    public boolean booleanValue() {
      throw new IllegalStateException(
        "Unknown state can not be converter to a boolean, only TRUE AND FALSE can!");
    }
  };

  public static Trilean valueOf(boolean value) {
    return value ? TRUE : FALSE;
  }

  public abstract boolean booleanValue();
}

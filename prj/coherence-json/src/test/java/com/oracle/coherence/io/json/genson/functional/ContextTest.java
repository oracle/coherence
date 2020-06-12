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

package com.oracle.coherence.io.json.genson.functional;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Genson;

public class ContextTest {
  private final Context ctx = new Context(new Genson());

  @Test
  public void testStore() {
    assertNull(ctx.store("key", 1));
    assertEquals(1, ctx.store("key", 2));
    assertNull(ctx.store("key2", 1));
  }

  @Test
  public void testGet() {
    ctx.store("key", new String[]{"value"});
    assertArrayEquals(new String[]{"value"}, ctx.get("key", String[].class));
    try {
      ctx.get("key", List.class);
      fail();
    } catch (ClassCastException cce) {
    }
  }

  @Test
  public void testRemove() {
    ctx.store("key", new String[]{"value"});
    assertNull(ctx.remove("key2", String.class));
    try {
      ctx.remove("key", Integer[].class);
      fail();
    } catch (ClassCastException cce) {

    }

    assertArrayEquals(new String[]{"value"}, ctx.remove("key", String[].class));
  }
}

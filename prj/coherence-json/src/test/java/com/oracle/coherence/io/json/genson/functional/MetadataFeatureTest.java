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

import java.util.Date;

import com.oracle.coherence.io.json.genson.GensonBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.oracle.coherence.io.json.genson.Genson;

public class MetadataFeatureTest {
  private Genson genson;

  @Before
  public void setUp() {
    genson = new GensonBuilder().useClassMetadata(true).addAlias("bean", Bean.class).create();
  }

  @Test
  public void testSerializeUnknownType() {
    Bean bean = new Bean();
    bean.value = new Date();
    assertEquals("{\"@class\":\"bean\",\"value\":" + ((Date) bean.value).getTime() + "}", genson.serialize(bean));
  }

  @Test
  public void testDeserializeToUnknownType() {
    Bean bean = (Bean) genson.deserialize("{\"@class\":\"bean\",\"value\":{\"@class\":\"bean\"}}", Object.class);
    assertTrue(bean.value instanceof Bean);

    bean = genson.deserialize("{\"@class\":\"bean\",\"value\":{\"@class\":\"bean\"}}", Bean.class);
    assertTrue(bean.value instanceof Bean);
  }

  @Test public void testClassMetadataShouldNotBeSerializedForStaticTypes() {
    Genson genson = new GensonBuilder().useClassMetadata(true).useClassMetadataWithStaticType(false).create();

    Bean bean = new Bean();

    assertEquals("{\"value\":null}", genson.serialize(bean));
  }

  @Test public void testClassMetadataShouldBeSerializedOnceWhenUsingUntypedConverter() {
    Bean bean = new Bean();
    bean.value = new Bean();
    assertEquals("{\"@class\":\"bean\",\"value\":{\"@class\":\"bean\",\"value\":null}}", genson.serialize(bean));
  }

  static class Bean {
    Object value;
  }
}

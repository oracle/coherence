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

import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.annotation.JsonCreator;
import org.junit.Test;

import com.oracle.coherence.io.json.genson.BeanView;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.annotation.JsonProperty;

import static org.junit.Assert.*;

public class BeanViewTest {
  Genson genson = new GensonBuilder()
    .useBeanViews(true)
    .useConstructorWithArguments(true)
    .set(new BeanMutatorAccessorResolver.StandardMutaAccessorResolver(VisibilityFilter.ALL,
      VisibilityFilter.PACKAGE_PUBLIC, VisibilityFilter.PACKAGE_PUBLIC)).create();

  @Test
  public void testSerializeWithInheritedView() {
    MyClass c = new MyClass();
    c.name = "toto";

    String json = genson.serialize(c, ExtendedView.class);
    assertEquals("{\"forName\":\"his name is : " + c.name + "\",\"value\":1}", json);

    json = genson.serialize(c, ExtendedBeanView2Class.class);
    assertEquals(json, "{\"value\":2}");

    json = genson.serialize(c, ConcreteView.class);
    assertEquals(json, "{\"value\":3}");
  }

  @Test
  public void testDeserializeWithInheritedView() {
    String json = "{\"forName\": \"titi\", \"value\": 123}";
    MyClass mc = genson.deserialize(json, MyClass.class, ExtendedView.class);
    assertTrue(ExtendedView.usedCtr);
    assertFalse(ExtendedView.usedForNameMethod);
    assertEquals(ExtendedView.val, 123);
    assertEquals("titi", mc.name);
  }

  public static class MyClass {
    public String name;
  }

  public static class MyClassView implements BeanView<MyClass> {
    static boolean usedCtr = false;
    static boolean usedForNameMethod = false;
    static int val;

    @JsonCreator
    public static MyClass create(String forName, @JsonProperty(value = "value") Integer theValue) {
      usedCtr = true;
      MyClass mc = new MyClass();
      mc.name = forName;
      val = theValue;
      return mc;
    }

    public void setForName(String name, MyClass target) {
      target.name = name;
      usedForNameMethod = true;
    }

    @JsonProperty(value = "forName")
    public String getHisName(MyClass b) {
      return "his name is : " + b.name;
    }
  }

  public static class ExtendedView extends MyClassView {
    public int getValue(MyClass b) {
      return 1;
    }
  }

  public static interface ExtendedBeanView2<T extends MyClass> extends BeanView<T> {
  }

  public static class ExtendedBeanView2Class implements ExtendedBeanView2<MyClass> {
    public int getValue(MyClass t) {
      return 2;
    }
  }

  public static class AbstractView<T> implements BeanView<T> {
    public int getValue(T t) {
      return 3;
    }
  }

  public static class ConcreteView extends AbstractView<MyClass> {

  }
}

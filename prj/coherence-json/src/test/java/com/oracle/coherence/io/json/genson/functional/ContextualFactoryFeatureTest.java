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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.convert.ContextualFactory;
import com.oracle.coherence.io.json.genson.reflect.BeanProperty;
import com.oracle.coherence.io.json.genson.reflect.VisibilityFilter;
import org.junit.Test;

import static org.junit.Assert.*;

import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.annotation.JsonConverter;
import com.oracle.coherence.io.json.genson.annotation.JsonDateFormat;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

public class ContextualFactoryFeatureTest {
  private final Genson genson = new Genson();

  @Test
  public void testJsonDateFormat() {
    ABean bean = new ABean();
    bean.milis = new Date();
    bean.date = new Date();

    String json = genson.serialize(bean);

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/mm/yyyy");
    assertEquals(
      "{\"date\":\"" + dateFormat.format(bean.date) + "\",\"milis\":"
        + bean.milis.getTime() + "}", json);

    ABean bean2 = genson.deserialize(json, ABean.class);
    assertEquals(bean.milis, bean2.milis);
    assertEquals(dateFormat.format(bean.date), dateFormat.format(bean2.date));
  }

  @Test
  public void testPropertyConverter() {
    assertEquals("{\"value\":1}", genson.serialize(new BBean()));
    assertEquals("1", genson.deserialize("{\"value\":1}", BBean.class).value);
  }

  @Test(expected = ClassCastException.class)
  public void testExceptionWhenPropertyTypeDoesNotMatch() {
    genson.serialize(new ExceptionBean());
  }

  @Test public void testFieldSerializationShouldUseContextualConverter() {
    User user = new User("hey");

    Genson genson = new GensonBuilder()
      .useFields(true, VisibilityFilter.PRIVATE)
      .useRuntimeType(true)
      .withContextualFactory(new ContextualFactory<String>() {
      @Override
      public Converter<String> create(BeanProperty property, Genson genson) {
        Censor ann = property.getAnnotation(Censor.class);
        if (ann != null) {
          return new Converter<String>() {
            @Override
            public void serialize(String object, ObjectWriter writer, Context ctx) throws Exception {
              writer.writeUnsafeValue("***");
            }

            @Override
            public String deserialize(ObjectReader reader, Context ctx) throws Exception {
              return null;
            }
          };
        }
        return null;
      }
    }).create();

    assertEquals("{\"password\":\"***\"}", genson.serialize(user));
  }

  static class ABean {
    @JsonDateFormat(asTimeInMillis = true)
    public Date milis;
    @JsonDateFormat("dd/mm/yyyy")
    public Date date;
  }

  static class ExceptionBean {
    @JsonConverter(DummyConverter.class)
    Object value;
  }

  static class BBean {
    @JsonConverter(DummyConverter.class)
    String value = "foo";
  }

  public static class DummyConverter implements Converter<String> {
    @Override
    public void serialize(String object, ObjectWriter writer, Context ctx) {
      writer.writeValue(1);
    }

    @Override
    public String deserialize(ObjectReader reader, Context ctx) {
      return reader.valueAsString();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(value = {ElementType.FIELD, ElementType.METHOD})
  public @interface Censor {}

  public class User {
    @Censor
    private String password;

    public User(String password) {
      this.password = password;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}

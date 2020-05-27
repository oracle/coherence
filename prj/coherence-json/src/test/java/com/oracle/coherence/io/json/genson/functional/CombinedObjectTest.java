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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Timestamp;

import com.oracle.coherence.io.json.genson.*;
import org.junit.Test;

import static org.junit.Assert.*;

import com.oracle.coherence.io.json.genson.annotation.JsonProperty;
import com.oracle.coherence.io.json.genson.reflect.BeanDescriptor;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;

public class CombinedObjectTest {
  @Test
  public void combineMultipleJsonObjectIntoSingleObject() {
    String json = "{\"Person\":{\"id\":\"2\"},\"Dog\":{\"dateOfBirth\":\"2012-08-20 00:00:00\",\"price\" : \"10.00\"}}";
    Genson genson = new GensonBuilder().withDeserializerFactory(new MyClassConverterFactory())
      .create();
    MyClass myClass = genson.deserialize(json, MyClass.class);
    assertEquals(Timestamp.valueOf("2012-08-20 00:00:00"), myClass.dogsDateOfBirth);
    assertEquals("2", myClass.personsId);
    assertEquals(new BigDecimal("10.00"), myClass.dogsPrice);
  }

  public static class MyClassConverterFactory implements Factory<Deserializer<MyClass>> {
    @Override
    public Deserializer<MyClass> create(Type type, Genson genson) {
      BeanDescriptor<MyClass> myClassDescriptor = (BeanDescriptor<MyClass>) genson
        .getBeanDescriptorProvider().provide(MyClass.class, MyClass.class, genson);
      return new MyClassConverter(myClassDescriptor);
    }
  }

  public static class MyClassConverter implements Deserializer<MyClass> {
    BeanDescriptor<MyClass> myClassDescriptor;

    public MyClassConverter(BeanDescriptor<MyClass> myClassDescriptor) {
      this.myClassDescriptor = myClassDescriptor;
    }

    @Override
    public MyClass deserialize(ObjectReader reader, Context ctx) {
      reader.beginObject();
      MyClass myClass = new MyClass();
      for (; reader.hasNext(); ) {
        reader.next();
        if ("Person".equals(reader.name())) {
          myClassDescriptor.deserialize(myClass, reader, ctx);
        } else if ("Dog".equals(reader.name())) {
          myClassDescriptor.deserialize(myClass, reader, ctx);
        }
      }
      reader.endObject();
      return myClass;
    }
  }

  public static class MyClass {
    @JsonProperty("id")
    private String personsId;
    @JsonProperty("dateOfBirth")
    private Timestamp dogsDateOfBirth;
    @JsonProperty("price")
    private BigDecimal dogsPrice;

    public MyClass() {
    }
  }
}

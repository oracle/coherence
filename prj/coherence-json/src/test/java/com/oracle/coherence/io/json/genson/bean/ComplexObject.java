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

import static org.junit.Assert.*;

import java.util.List;

public class ComplexObject {
  private Primitives primitives;
  private List<Primitives> listOfPrimitives;
  private Primitives[] arrayOfPrimitives;

  public ComplexObject() {
  }

  public ComplexObject(Primitives primitives,
                       List<Primitives> listOfPrimitives, Primitives[] arrayOfPrimitives) {
    super();
    this.primitives = primitives;
    this.listOfPrimitives = listOfPrimitives;
    this.arrayOfPrimitives = arrayOfPrimitives;
  }

  public static void assertCompareComplexObjects(ComplexObject co, ComplexObject coo) {
    Primitives.assertComparePrimitives(co.getPrimitives(), coo.getPrimitives());

    assertTrue(coo.getArrayOfPrimitives().length == co.getArrayOfPrimitives().length);
    assertTrue(coo.getListOfPrimitives().size() == co.getListOfPrimitives().size());

    for (int i = 0; i < coo.getArrayOfPrimitives().length; i++) {
      Primitives.assertComparePrimitives(coo.getArrayOfPrimitives()[i], co.getArrayOfPrimitives()[i]);
    }

    for (int i = 0; i < coo.getListOfPrimitives().size(); i++) {
      Primitives.assertComparePrimitives(coo.getListOfPrimitives().get(i), co.getListOfPrimitives().get(i));
    }
  }

  public String jsonString() {
    StringBuilder sb = new StringBuilder();

    sb.append("{\"arrayOfPrimitives\":[");

    if (arrayOfPrimitives != null && arrayOfPrimitives.length > 0) {
      for (int i = 0; i < arrayOfPrimitives.length - 1; i++) {
        sb.append(arrayOfPrimitives[i].jsonString()).append(',');
      }
      sb.append(arrayOfPrimitives[arrayOfPrimitives.length - 1].jsonString());
    }

    sb.append("],\"listOfPrimitives\":[");
    if (listOfPrimitives != null && listOfPrimitives.size() > 0) {
      for (int i = 0; i < listOfPrimitives.size() - 1; i++) {
        sb.append(listOfPrimitives.get(i).jsonString()).append(',');
      }

      sb.append(listOfPrimitives.get(listOfPrimitives.size() - 1).jsonString());
    }

    sb.append("],\"primitives\":");
    if (primitives != null) sb.append(primitives.jsonString());
    else sb.append("null");


    sb.append("}");
    return sb.toString();
  }

  public Primitives getPrimitives() {
    return primitives;
  }

  public void setPrimitives(Primitives primitives) {
    this.primitives = primitives;
  }

  public List<Primitives> getListOfPrimitives() {
    return listOfPrimitives;
  }

  public void setListOfPrimitives(List<Primitives> listOfPrimitives) {
    this.listOfPrimitives = listOfPrimitives;
  }

  public Primitives[] getArrayOfPrimitives() {
    return arrayOfPrimitives;
  }

  public void setArrayOfPrimitives(Primitives[] arrayOfPrimitives) {
    this.arrayOfPrimitives = arrayOfPrimitives;
  }

}

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

public class Primitives {
  private int intPrimitive;
  private Integer integerObject;
  private double doublePrimitive;
  private Double doubleObject;
  private String text;
  private boolean booleanPrimitive;
  private boolean booleanObject;

  public Primitives() {
  }

  public Primitives(int intPrimitive, Integer integerObject, double doublePrimitive,
                    Double doubleObject, String text, boolean booleanPrimitive, boolean booleanObject) {
    super();
    this.intPrimitive = intPrimitive;
    this.integerObject = integerObject;
    this.doublePrimitive = doublePrimitive;
    this.doubleObject = doubleObject;
    this.text = text;
    this.booleanPrimitive = booleanPrimitive;
    this.booleanObject = booleanObject;
  }

  public static void assertComparePrimitives(Primitives p1, Primitives p2) {
    assertEquals(p1.getIntPrimitive(), p2.getIntPrimitive());
    assertEquals(p1.getIntegerObject(), p2.getIntegerObject());
    assertEquals(p1.getDoubleObject(), p2.getDoubleObject());
    assertEquals(p1.getDoublePrimitive(), p2.getDoublePrimitive(), 0);
    assertEquals(p1.getText(), p2.getText());
    assertEquals(p1.isBooleanPrimitive(), p2.isBooleanPrimitive());
    assertEquals(p1.isBooleanObject(), p2.isBooleanObject());
  }

  public String jsonString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"booleanObject\":").append(booleanObject).append(",\"booleanPrimitive\":")
      .append(booleanPrimitive).append(",\"doubleObject\":").append(doubleObject)
      .append(",\"doublePrimitive\":").append(doublePrimitive)
      .append(",\"integerObject\":").append(integerObject).append(",\"intPrimitive\":")
      .append(intPrimitive).append(",\"text\":");
    if (text != null)
      sb.append("\"" + text + "\"");
    else
      sb.append("null");
    sb.append('}');
    return sb.toString();
  }

  public int getIntPrimitive() {
    return intPrimitive;
  }

  public void setIntPrimitive(int intPrimitive) {
    this.intPrimitive = intPrimitive;
  }

  public Integer getIntegerObject() {
    return integerObject;
  }

  public void setIntegerObject(Integer integerObject) {
    this.integerObject = integerObject;
  }

  public double getDoublePrimitive() {
    return doublePrimitive;
  }

  public void setDoublePrimitive(double doublePrimitive) {
    this.doublePrimitive = doublePrimitive;
  }

  public Double getDoubleObject() {
    return doubleObject;
  }

  public void setDoubleObject(Double doubleObject) {
    this.doubleObject = doubleObject;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public boolean isBooleanPrimitive() {
    return booleanPrimitive;
  }

  public void setBooleanPrimitive(boolean booleanPrimitive) {
    this.booleanPrimitive = booleanPrimitive;
  }

  public boolean isBooleanObject() {
    return booleanObject;
  }

  public void setBooleanObject(boolean booleanObject) {
    this.booleanObject = booleanObject;
  }
}

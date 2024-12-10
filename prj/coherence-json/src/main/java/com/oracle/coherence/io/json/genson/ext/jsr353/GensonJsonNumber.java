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

package com.oracle.coherence.io.json.genson.ext.jsr353;


import javax.json.JsonNumber;
import java.math.BigDecimal;
import java.math.BigInteger;

abstract class GensonJsonNumber implements JsonNumber {

  static class IntJsonNumber extends GensonJsonNumber {
    private final long value;
    private BigInteger exactValue;

    protected IntJsonNumber(long value) {
      this.value = value;
    }

    IntJsonNumber(BigInteger exactValue) {
      this.exactValue = exactValue;
      this.value = exactValue.longValue();
    }

    @Override
    public boolean isIntegral() {
      return true;
    }

    @Override
    public int intValue() {
      return (int) value;
    }

    @Override
    public int intValueExact() {
      return (int) value;
    }

    @Override
    public long longValue() {
      return value;
    }

    @Override
    public long longValueExact() {
      return value;
    }

    @Override
    public BigInteger bigIntegerValue() {
      return BigInteger.valueOf(value);
    }

    @Override
    public BigInteger bigIntegerValueExact() {
      return BigInteger.valueOf(value);
    }

    @Override
    public double doubleValue() {
      return (double) value;
    }

    @Override
    public BigDecimal bigDecimalValue() {
      return BigDecimal.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      IntJsonNumber that = (IntJsonNumber) o;

      return bigIntegerValue().equals(that.bigIntegerValue());
    }

    @Override
    public int hashCode() {
      return bigIntegerValue().hashCode();
    }

    @Override
    public String toString() {
      return Long.toString(value);
    }
  }

  static class DoubleJsonNumber extends GensonJsonNumber {
    private final double value;
    private BigDecimal exactValue;

    protected DoubleJsonNumber(double value) {
      this.value = value;
    }

    protected DoubleJsonNumber(BigDecimal value) {
      this.exactValue = value;
      this.value = exactValue.doubleValue();
    }

    @Override
    public boolean isIntegral() {
      return false;
    }

    @Override
    public int intValue() {
      return (int) value;
    }

    @Override
    public int intValueExact() {
      return (int) value;
    }

    @Override
    public long longValue() {
      return (long) value;
    }

    @Override
    public long longValueExact() {
      return (long) value;
    }

    @Override
    public BigInteger bigIntegerValue() {
      return bigDecimalValue().toBigInteger();
    }

    @Override
    public BigInteger bigIntegerValueExact() {
      return bigDecimalValue().toBigIntegerExact();
    }

    @Override
    public double doubleValue() {
      return value;
    }

    @Override
    public BigDecimal bigDecimalValue() {
      if (exactValue == null) {
        exactValue = BigDecimal.valueOf(value);
      }
      return exactValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      DoubleJsonNumber that = (DoubleJsonNumber) o;

      return bigDecimalValue().equals(that.bigDecimalValue());
    }

    @Override
    public int hashCode() {
      return bigDecimalValue().hashCode();
    }

    @Override
    public String toString() {
      return Double.toString(value);
    }
  }

  @Override
  public ValueType getValueType() {
    return ValueType.NUMBER;
  }

}

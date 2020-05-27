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

import java.io.IOException;

public class DoubleParseAlgorithmTest {
  public static void main(String[] args) throws IOException {
    for (int p = 1; p < 23; p++) {
      alllong(p, new char[27], 0);
      System.out.println("passed " + p);
    }
  }

  static void alllong(int pow, char[] arr, int pos) throws IOException {
    if (pos > 20) return;

    for (int i = 0; i < 10; i++) {
      arr[pos] = (char) (i + 48);

      arr[pos + 1] = 'E';
      arr[pos + 2] = '-';
      String ps = "" + pow;
      for (int k = 0; k < ps.length(); k++)
        arr[pos + k + 3] = ps.charAt(k);

      String s = new String(arr, 0, pos + 3 + ps.length());
      try {
        double d = Double.parseDouble(s);
        JsonReader reader = new JsonReader(s);
        double d2 = reader.valueAsDouble();
        reader.close();
        if (d != d2) System.out.println("d=" + d + ", d2=" + d2 + ", str=" + s);
      } catch (NumberFormatException nfe) {
        nfe.printStackTrace();
        System.out.println("ex for str=" + s);
      }

      alllong(pow, arr, pos + 1);
    }
  }
}

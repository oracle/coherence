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

public class Timer {
  private long start;
  private long end;
  private long sum;
  private int cnt;
  private boolean paused;

  public Timer start() {
    end = 0;
    sum = 0;
    cnt = 0;
    paused = false;
    start = System.currentTimeMillis();
    return this;
  }

  public Timer stop() {
    end = System.currentTimeMillis();

    return this;
  }

  public Timer pause() {
    paused = true;
    end = System.currentTimeMillis();
    return this;
  }

  public Timer unpause() {
    if (paused) {
      long t = System.currentTimeMillis();
      start = t - (end - start);
      paused = false;
    }
    return this;
  }

  public Timer cumulate() {
    cnt++;
    end = System.currentTimeMillis();
    sum += end - start;
    start = end;

    return this;
  }

  public String printMS() {
    if (cnt > 0)
      return ((double) sum / cnt) + " ms";

    return end - start + " ms";
  }

  public String printS() {
    if (cnt > 0)
      return ((double) sum / (cnt * 1000)) + " ms";

    return ((double) (end - start) / 1000) + " s";
  }
}
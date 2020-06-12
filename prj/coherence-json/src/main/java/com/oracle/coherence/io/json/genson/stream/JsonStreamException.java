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


/**
 * JsonStreamException are thrown by ObjectWriter and ObjectReader implementations. They indicate
 * that there was a syntax error or a state error (calling endObject when it should be endArray
 * etc).
 *
 * @author eugen
 */
public final class JsonStreamException extends RuntimeException {
  private static final long serialVersionUID = 8033784054415043293L;

  private final int column;
  private final int row;

  public JsonStreamException(String message, Throwable cause) {
    this(message, cause, -1, -1);
  }

  public JsonStreamException(String message) {
    this(message, null);
  }

  public JsonStreamException(Throwable cause) {
    this(null, cause);
  }

  // package visibility, api users are not supposed to use it
  JsonStreamException(String message, Throwable cause, int row, int col) {
    super(message, cause);
    this.column = col;
    this.row = row;
  }

  public int getColumn() {
    return column;
  }

  public int getRow() {
    return row;
  }

  public static <T extends Exception> T niceTrace(T exception) {
    final StackTraceElement[] stackTrace = exception.getStackTrace();
    final StackTraceElement[] newStackTrace = new StackTraceElement[stackTrace.length - 1];

    System.arraycopy(stackTrace, 1, newStackTrace, 0, stackTrace.length - 1);
    exception.setStackTrace(newStackTrace);
    return exception;
  }

  public JsonStreamException niceTrace() {
    return niceTrace(this);
  }

  static class Builder {
    private int col;
    private int row;
    private String message;
    private Throwable cause;

    public JsonStreamException create() {
      return new JsonStreamException(message, cause, row, col);
    }

    Builder locate(int row, int col) {
      this.row = row;
      this.col = col;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder cause(Throwable th) {
      this.cause = th;
      return this;
    }
  }
}

/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package logging.impl;

import java.util.Map;

import org.slf4j.spi.MDCAdapter;

/**
 * This adapter is an empty implementation of the {@link MDCAdapter} interface.
 * It is used for all logging systems which do not support mapped
 * diagnostic contexts such as JDK14, simple, NOP, and debug.
 * 
 * @author Jason Howes
 * 
 * @since 1.4.1
 */
public class DebugMDCAdapter implements MDCAdapter {
  public void clear() {
  }

  public String get(String key) {
    return null;
  }

  public void put(String key, String val) {
  }

  public void remove(String key) {
  }

  public Map getCopyOfContextMap() {
    return null;
  }

  public void setContextMap(Map contextMap) {
  }
}

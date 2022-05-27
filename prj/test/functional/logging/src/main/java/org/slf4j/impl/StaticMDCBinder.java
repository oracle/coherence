/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package org.slf4j.impl;

import logging.impl.DebugMDCAdapter;

import org.slf4j.spi.MDCAdapter;

/**
 * This implementation is bound to {@link DebugMDCAdapter}.
 *
 * @author Jason Howes
 */
public class StaticMDCBinder {
  /**
   * The unique instance of this class.
   */
  public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

  private StaticMDCBinder() {
  }
  
  /**
   * Currently this method always returns an instance of 
   * {@link StaticMDCBinder}.
   */
  public MDCAdapter getMDCA() {
     return new DebugMDCAdapter();
  }
  
  public String  getMDCAdapterClassStr() {
    return DebugMDCAdapter.class.getName();
  }
}

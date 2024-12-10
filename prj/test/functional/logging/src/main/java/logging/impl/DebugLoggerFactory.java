/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package logging.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * DebugLoggerFactory is an trivial implementation of {@link
 * ILoggerFactory} which returns instances of DebugLogger.
 * 
 * @author Jason Howes
 */
public class DebugLoggerFactory implements ILoggerFactory {
  public DebugLoggerFactory() {
  }
  
  public Logger getLogger(String name) {
    Logger logger = m_mapLogger.get(name);
    if (logger == null) {
      m_mapLogger.putIfAbsent(name, new DebugLogger(name));
      logger = m_mapLogger.get(name);
    }
    return logger;
  }
    
  private static final ConcurrentMap<String, Logger> m_mapLogger = new ConcurrentHashMap<String, Logger>();
}

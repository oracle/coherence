/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package logging.impl;

import org.slf4j.Logger;

import org.slf4j.helpers.MarkerIgnoringBase;

/**
 * A direct implementation of {@link Logger} that can be used for debugging
 * the SLF4J framework.
 *
 * @author Jason Howes
 */
public class DebugLogger extends MarkerIgnoringBase {
  private static final long serialVersionUID = -517220405410904473L;

  DebugLogger(String name) {
    m_sName = name;
  }

  public String getName() {
    return m_sName;
  }

  /**
   * Always returns true.
   * @return always true
   */
  final public boolean isTraceEnabled() {
    return true;
  }

  /** A Debug implementation. */
  final public void trace(String msg) {
    log(TRACE_STRING, msg);
  }

  /** A Debug implementation.  */
  final public void trace(String format, Object arg1) {
    log(TRACE_STRING, String.format(format, arg1));
  }

  /** A Debug implementation.  */
  public final void trace(String format, Object arg1, Object arg2) {
    log(TRACE_STRING, String.format(format, arg1, arg2));
  }

  /** A Debug implementation.  */
  public final void trace(String format, Object[] argArray) {
    log(TRACE_STRING, String.format(format, argArray));
  }
  
  /** A Debug implementation. */
  final public void trace(String msg, Throwable t) {
    log(TRACE_STRING, msg, t);
  }

  /**
   * Always returns true.
   * @return always true
   */
  final public boolean isDebugEnabled() {
    return true;
  }

  /** A Debug implementation. */
  final public void debug(String msg) {
    log(DEBUG_STRING, msg);
  }

  /** A Debug implementation.  */
  final public void debug(String format, Object arg1) {
    log(DEBUG_STRING, String.format(format, arg1));
  }

  /** A Debug implementation.  */
  public final void debug(String format, Object arg1, Object arg2) {
    log(DEBUG_STRING, String.format(format, arg1, arg2));
  }

  /** A Debug implementation.  */
  public final void debug(String format, Object[] argArray) {
    log(DEBUG_STRING, String.format(format, argArray));
  }
  
  /** A Debug implementation. */
  final public void debug(String msg, Throwable t) {
    log(DEBUG_STRING, msg, t);
  }

  /**
   * Always returns true.
   * @return always true
   */
  final public boolean isInfoEnabled() {
    // Debug
    return true;
  }

  /** A Debug implementation. */
  final public void info(String msg) {
    log(INFO_STRING, msg);
  }

  /** A Debug implementation. */
  final  public void info(String format, Object arg1) {
    log(INFO_STRING, String.format(format, arg1));
  }

  /** A Debug implementation. */
  final public void info(String format, Object arg1, Object arg2) {
    log(INFO_STRING, String.format(format, arg1, arg2));
  }
  
  /** A Debug implementation.  */
  public final void info(String format, Object[] argArray) {
    log(INFO_STRING, String.format(format, argArray));
  }

  /** A Debug implementation. */
  final public void info(String msg, Throwable t) {
    log(INFO_STRING, msg, t);
  }

  /**
   * Always returns true.
   * @return always true
   */
  final public boolean isWarnEnabled() {
    return true;
  }

  /** A Debug implementation. */
  final public void warn(String msg) {
    log(WARN_STRING, msg);
  }

  /** A Debug implementation. */
  final public void warn(String format, Object arg1) {
    log(WARN_STRING, String.format(format, arg1));
  }

  /** A Debug implementation. */
  final public void warn(String format, Object arg1, Object arg2) {
    log(WARN_STRING, String.format(format, arg1, arg2));
  }
  
  /** A Debug implementation.  */
  public final void warn(String format, Object[] argArray) {
    log(WARN_STRING, String.format(format, argArray));
  }

  /** A Debug implementation. */
  final public void warn(String msg, Throwable t) {
    log(WARN_STRING, msg, t);
  }

  /** A Debug implementation. */
  final public boolean isErrorEnabled() {
    return true;
  }

  /** A Debug implementation. */
  final public void error(String msg) {
    log(ERROR_STRING, msg);
  }

  /** A Debug implementation. */
  final public void error(String format, Object arg1) {
    log(ERROR_STRING, String.format(format, arg1));
  }

  /** A Debug implementation. */
  final public void error(String format, Object arg1, Object arg2) {
    log(ERROR_STRING, String.format(format, arg1, arg2));
  }
  
  /** A Debug implementation.  */
  public final void error(String format, Object[] argArray) {
    log(ERROR_STRING, String.format(format, argArray));
  }

  /** A Debug implementation. */
  final public void error(String msg, Throwable t) {
    log(ERROR_STRING, msg, t);
  }

  public void log(String sPrefix, String sMsg) {
    boolean fCapture = false;
    if (sMsg.startsWith(CAPTURE_PREFIX)) {
      sMsg = sMsg.substring(CAPTURE_PREFIX.length());
      fCapture = true;
    }
    String sLog = sPrefix + " " + getName() + " - " + sMsg + "\n";
    System.err.print(sLog);
    if (fCapture) {
      m_sbOutput.append(sLog);
    }
  }

  public void log(String sPrefix, String sMsg, Throwable t) {
    log(sPrefix, sMsg + " - " + t);
  }

  public String getLogOutput() {
    return m_sbOutput.toString();       
  }
    
  public void resetLogOutput() {
    m_sbOutput.setLength(0);    
  }
    
  public static final String ERROR_STRING = "Error";
  public static final String WARN_STRING  = "Warn";
  public static final String INFO_STRING  = "Info";
  public static final String DEBUG_STRING = "Debug";
  public static final String TRACE_STRING = "Trace";

  public static final String CAPTURE_PREFIX = "<DebugLogger>";

  private final String m_sName;

  private final StringBuffer m_sbOutput = new StringBuffer();
}

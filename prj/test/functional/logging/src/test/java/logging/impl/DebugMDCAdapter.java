/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package logging.impl;

import java.util.Deque;
import java.util.Map;

import org.slf4j.spi.MDCAdapter;

/**
 * This adapter is an empty implementation of the {@link MDCAdapter} interface.
 * It is used for all logging systems which do not support mapped
 * diagnostic contexts such as JDK14, simple, NOP, and debug.
 *
 * @author Jason Howes
 * @since 1.4.1
 */
public class DebugMDCAdapter
        implements MDCAdapter
    {
    @Override
    public void clear()
        {
        }

    @Override
    public String get(String key)
        {
        return null;
        }

    @Override
    public void put(String key, String val)
        {
        }

    @Override
    public void remove(String key)
        {
        }

    @Override
    public Map getCopyOfContextMap()
        {
        return null;
        }

    @Override
    public void setContextMap(Map contextMap)
        {
        }

    @Override
    public void pushByKey(String s, String s1)
        {
        }

    @Override
    public String popByKey(String s)
        {
        return "";
        }

    @Override
    public Deque<String> getCopyOfDequeByKey(String s)
        {
        return null;
        }

    @Override
    public void clearDequeByKey(String s)
        {
        }
    }

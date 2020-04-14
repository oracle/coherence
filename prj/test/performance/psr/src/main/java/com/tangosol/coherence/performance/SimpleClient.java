/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.tangosol.net.CacheFactory;

/**
 * @author jk 2016.03.08
 */
public class SimpleClient
    {

    public static void main(String[] args)
            throws Exception
        {
        System.out.println("Staring " + SimpleClient.class.getSimpleName());

        System.out.println("System Properties: >>>>>>>>>>>>>>>");

        System.getProperties().stringPropertyNames().stream()
                .sorted()
                .forEach((sName) -> System.out.println(sName + "=" + System.getProperty(sName)));

        System.out.println("<<<<<<<<<<<<<<< System Properties");

        CacheFactory.getConfigurableCacheFactory();

        synchronized (MONITOR)
            {
            MONITOR.wait();
            }
        }

    public static final Object MONITOR = new Object();
    }

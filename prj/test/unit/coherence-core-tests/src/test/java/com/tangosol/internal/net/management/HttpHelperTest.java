/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;


import com.tangosol.net.Cluster;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.when;

public class HttpHelperTest
    {
    // regression test for Bug 33303922 that threw NPE when missing coherence-management dependencies
    @Test
    public void testTestServiceHandlingOfIllegalStateException()
            throws ClassNotFoundException
        {
        Cluster     cluster = mock(Cluster.class);
        ClassLoader loader  = mock(ClassLoader.class);
        when(cluster.getContextClassLoader()).thenReturn(loader);

        Class clz = String.class;
        when(loader.loadClass(anyString())).thenReturn(clz);
        when(loader.getResource(anyString())).thenThrow(new IllegalStateException("test configuration failure"));

        HttpHelper.startService(cluster);
        }

    // regression test for Bug 33303922 that threw NPE when missing coherence-management dependencies
    @Test
    public void testTestServiceHandlingOfNoClassDefFoundException()
            throws ClassNotFoundException
        {
        Cluster     cluster = mock(Cluster.class);
        ClassLoader loader  = mock(ClassLoader.class);
        when(cluster.getContextClassLoader()).thenReturn(loader);

        Class clz = String.class;
        when(loader.loadClass(anyString())).thenReturn(clz);
        when(loader.getResource(anyString())).thenThrow(new NoClassDefFoundError("jersey-resource-class-missing"));

        HttpHelper.startService(cluster);
        }
    }
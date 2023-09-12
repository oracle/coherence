/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class VirtualThreadsTest
    {
    @AfterEach
    public void reset()
        {
        System.setProperty(PROPERTY_ENABLED, "true");
        System.setProperty(PROPERTY_SERVICE_ENABLED, "true");
        }

    @Test
    public void ensureThreadIsVirtual()
        {
        Thread t = VirtualThreads.makeThread(null, () -> {}, "MyThread");

        assertThat(t.isVirtual(), is(true));
        assertThat(t.getName(), is("MyThread"));
        }

    @Test
    public void ensureVirtualThreadsAreEnabledByDefault()
        {
        assertThat(VirtualThreads.isEnabled(), is(true));
        assertThat(VirtualThreads.isEnabled(SERVICE_NAME), is(true));
        }

    @Test
    public void ensureVirtualThreadsCanBeDisabledGlobally()
        {
        System.setProperty(PROPERTY_ENABLED, "false");
        assertThat(VirtualThreads.isEnabled(), is(false));
        assertThat(VirtualThreads.isEnabled(SERVICE_NAME), is(false));
        }

    @Test
    public void ensureVirtualThreadsCanBeEnabledForService()
        {
        System.setProperty(PROPERTY_ENABLED, "false");
        System.setProperty(PROPERTY_SERVICE_ENABLED, "true");
        assertThat(VirtualThreads.isEnabled(), is(false));
        assertThat(VirtualThreads.isEnabled(SERVICE_NAME), is(true));
        }

    @Test
    public void ensureVirtualThreadsCanBeDisabledForService()
        {
        System.setProperty(PROPERTY_ENABLED, "true");
        System.setProperty(PROPERTY_SERVICE_ENABLED, "false");
        assertThat(VirtualThreads.isEnabled(), is(true));
        assertThat(VirtualThreads.isEnabled(SERVICE_NAME), is(false));
        }

    private static final String SERVICE_NAME = "Foo";
    private static final String PROPERTY_ENABLED = VirtualThreads.PROPERTY_ENABLED;
    private static final String PROPERTY_SERVICE_ENABLED = VirtualThreads.PROPERTY_SERVICE_ENABLED.apply(SERVICE_NAME);
    }

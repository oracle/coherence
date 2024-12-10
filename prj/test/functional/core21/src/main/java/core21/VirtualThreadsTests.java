/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package core21;


import com.tangosol.internal.util.VirtualThreads;
import com.tangosol.net.CacheFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class VirtualThreadsTests
    {
    @BeforeEach
    public void reset()
        {
        System.clearProperty(PROPERTY_ENABLED);
        System.clearProperty(PROPERTY_SERVICE_ENABLED);
        CacheFactory.shutdown();
        }

    @Test
    public void ensureThreadIsVirtual()
        {
        Thread t = VirtualThreads.makeThread(null, () -> {}, "MyThread");

        assertThat(t.isVirtual(), is(true));
        assertThat(t.getName(), is("MyThread"));
        }

    @Test
    public void ensureVirtualThreadsAreDisabledByDefault()
        {
        assertThat(VirtualThreads.isEnabled(), is(false));
        assertThat(VirtualThreads.isEnabled(SERVICE_NAME), is(false));
        }

    @Test
    public void ensureVirtualThreadsCanBeEnabledGlobally()
        {
        System.setProperty(PROPERTY_ENABLED, "true");
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

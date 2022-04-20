/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.events.application.EventDispatcher;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.application.LifecycleEvent.Type;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.util.ImmutableArrayList;

import org.junit.Test;

import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test the implementation of {@link ConfigurableCacheFactoryDispatcher}.
 *
 * @author hr 2012.07.24
 * @since Coherence 12.1.2
 */
public class LifecycleDispatcherTest
    {

    /**
     * Test the dispatching of the supported lifecycle events.
     */
    @Test
    public void testEventDispatching()
        {
        ConfigurableCacheFactoryDispatcher dispatcher = new ConfigurableCacheFactoryDispatcher();

        EventInterceptor         interceptor = mock(EventInterceptor.class);
        ConfigurableCacheFactory laccf       = mock(ConfigurableCacheFactory.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        dispatcher.addEventInterceptor("LACCF", interceptor, new ImmutableArrayList(LifecycleEvent.Type.values()), false);

        dispatcher.dispatchActivating(laccf);

        verify(interceptor).onEvent(captor.capture());
        assertThat(captor.getValue().getType(), is((Enum) Type.ACTIVATING));

        dispatcher.dispatchActivated(laccf);

        verify(interceptor, times(2)).onEvent(captor.capture());
        assertThat(captor.getValue().getType(), is((Enum) Type.ACTIVATED));

        dispatcher.dispatchDisposing(laccf);

        verify(interceptor, times(3)).onEvent(captor.capture());
        assertThat(captor.getValue().getType(), is((Enum) Type.DISPOSING));
        }
    }
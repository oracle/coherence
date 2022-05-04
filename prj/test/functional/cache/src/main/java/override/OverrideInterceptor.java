/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package override;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.util.Base;

/**
 * A test class that implements {@link EventInterceptor}.
 */
public class OverrideInterceptor
        implements EventInterceptor<LifecycleEvent>
    {
    // ----- EventInterceptor interface -------------------------------------

    @Override
    public void onEvent(LifecycleEvent event)
        {
        Logger.info(String.format("OverrideInterceptor received event with type: %s", event.getType()));

        if (event.getType() == LifecycleEvent.Type.ACTIVATED)
            {
            ConfigurableCacheFactory configurableCacheFactory = event.getConfigurableCacheFactory();

            NamedCache<String, String> cache = configurableCacheFactory.ensureCache("my-cache-interceptor", null);

            if (cache == null || !cache.isActive())
                {
                Base.err("Failed to activate cache: my-cache-interceptor");
                }
            else
                {
                cache.put("interceptor-name", this.getClass().getName());
                }
            }
        }
    }